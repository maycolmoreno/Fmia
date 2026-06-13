using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using System.Security.Cryptography;
using System.Text;

namespace Farmamia.Agent.Aplicacion.CasosUso;

public sealed class PrepararActualizacionCasoUso
{
    private const string TipoInstruccionActualizarPos = "UPDATE_POS";

    private readonly IClienteOperacionesFarmamia clienteOperaciones;
    private readonly IAlmacenamientoPaquetes almacenamientoPaquetes;
    private readonly IInventarioEquipo inventarioEquipo;
    private readonly IRespaldoPos respaldoPos;
    private readonly IActualizadorPos actualizadorPos;
    private readonly IProcesoPos procesoPos;
    private readonly IRelojSistema relojSistema;
    private readonly IAvisadorUsuario avisadorUsuario;
    private readonly IEstadoAvisosActualizacion estadoAvisosActualizacion;
    private readonly IEstadoLocalAgente estadoLocalAgente;
    private readonly IBloqueoActualizacion bloqueoActualizacion;
    private readonly ILogger<PrepararActualizacionCasoUso> logger;
    private readonly OpcionesAgente opciones;

    public PrepararActualizacionCasoUso(
        IClienteOperacionesFarmamia clienteOperaciones,
        IAlmacenamientoPaquetes almacenamientoPaquetes,
        IInventarioEquipo inventarioEquipo,
        IRespaldoPos respaldoPos,
        IActualizadorPos actualizadorPos,
        IProcesoPos procesoPos,
        IRelojSistema relojSistema,
        IAvisadorUsuario avisadorUsuario,
        IEstadoAvisosActualizacion estadoAvisosActualizacion,
        IEstadoLocalAgente estadoLocalAgente,
        IBloqueoActualizacion bloqueoActualizacion,
        ILogger<PrepararActualizacionCasoUso> logger,
        IOptions<OpcionesAgente> opciones
    )
    {
        this.clienteOperaciones = clienteOperaciones;
        this.almacenamientoPaquetes = almacenamientoPaquetes;
        this.inventarioEquipo = inventarioEquipo;
        this.respaldoPos = respaldoPos;
        this.actualizadorPos = actualizadorPos;
        this.procesoPos = procesoPos;
        this.relojSistema = relojSistema;
        this.avisadorUsuario = avisadorUsuario;
        this.estadoAvisosActualizacion = estadoAvisosActualizacion;
        this.estadoLocalAgente = estadoLocalAgente;
        this.bloqueoActualizacion = bloqueoActualizacion;
        this.logger = logger;
        this.opciones = opciones.Value;
    }

    public async Task EjecutarAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
    {
        InstruccionActualizacion? instruccion = await clienteOperaciones.ConsultarInstruccionAsync(
            credenciales,
            cancellationToken
        );

        if (instruccion is null || !instruccion.TieneInstruccion)
        {
            return;
        }

        ValidarInstruccion(instruccion);
        DatosInventarioEquipo inventario = inventarioEquipo.ObtenerInventario();

        if (!EsHoraOficialAlcanzada(instruccion))
        {
            return;
        }

        if (inventario.ProcesoPosEjecutandose && !EsHoraForzadaAlcanzada(instruccion))
        {
            await EnviarAvisosPendientesAsync(credenciales, instruccion, inventario, cancellationToken);
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "POS_ACTIVITY_DETECTED", "POS activo; actualizacion diferida hasta hora forzada", inventario, null),
                cancellationToken
            );
            return;
        }

        using IDisposable? bloqueo = bloqueoActualizacion.IntentarAdquirir();
        if (bloqueo is null)
        {
            await ReportarFalloSinRollbackAsync(
                credenciales,
                instruccion,
                inventario,
                "UPDATE_FAILED",
                "Otra actualizacion POS ya esta en ejecucion en este equipo",
                cancellationToken
            );
            return;
        }

        ArchivoPaqueteLocal archivo;
        try
        {
            archivo = await DescargarConReintentosAsync(credenciales, instruccion, inventario, cancellationToken);
        }
        catch (Exception ex)
        {
            await ReportarFalloSinRollbackAsync(credenciales, instruccion, inventario, "UPDATE_FAILED", ex.Message, cancellationToken);
            throw;
        }

        if (!string.Equals(archivo.ChecksumSha256, instruccion.ChecksumSha256, StringComparison.OrdinalIgnoreCase))
        {
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "VALIDATION_FAILED", "Checksum SHA-256 invalido", inventario, archivo),
                cancellationToken
            );
            await ReportarResultadoFallidoAsync(
                credenciales,
                instruccion,
                inventario,
                "FAILED",
                "Checksum SHA-256 invalido para el paquete descargado",
                cancellationToken
            );
            await GuardarEstadoAsync("VALIDATION_FAILED", instruccion, inventario, "FAILED", "Checksum SHA-256 invalido", cancellationToken);
            throw new InvalidOperationException("Checksum SHA-256 invalido para el paquete descargado");
        }

        RespaldoPos? respaldo = null;
        bool posEstabaEjecutandose = inventario.ProcesoPosEjecutandose;

        await clienteOperaciones.ReportarEventoAsync(
            credenciales,
            Evento(instruccion, "CHECKSUM_VALIDATED", "Checksum SHA-256 validado", inventario, archivo),
            cancellationToken
        );

        if (!FirmaValida(instruccion))
        {
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "SIGNATURE_VALIDATION_FAILED", "Firma digital de paquete invalida", inventario, archivo),
                cancellationToken
            );
            await ReportarResultadoFallidoAsync(
                credenciales,
                instruccion,
                inventario,
                "FAILED",
                "Firma digital de paquete invalida",
                cancellationToken
            );
            await GuardarEstadoAsync("SIGNATURE_VALIDATION_FAILED", instruccion, inventario, "FAILED", "Firma digital invalida", cancellationToken);
            throw new InvalidOperationException("Firma digital de paquete invalida");
        }

        await clienteOperaciones.ReportarEventoAsync(
            credenciales,
            Evento(instruccion, "SIGNATURE_VALIDATED", "Firma digital de paquete validada", inventario, archivo),
            cancellationToken
        );

        try
        {
            if (posEstabaEjecutandose)
            {
                await procesoPos.CerrarSiEjecutandoseAsync(cancellationToken);
                await clienteOperaciones.ReportarEventoAsync(
                    credenciales,
                    Evento(instruccion, "POS_CLOSED", "Proceso POS cerrado para actualizacion", inventario, archivo),
                    cancellationToken
                );
            }

            respaldo = await respaldoPos.CrearAsync(inventario.RutaPos, inventario.VersionPos, cancellationToken);
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "BACKUP_CREATED", "Respaldo POS creado", inventario, archivo, respaldo),
                cancellationToken
            );

            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "UPDATE_STARTED", "Aplicando paquete POS", inventario, archivo, respaldo),
                cancellationToken
            );
            await actualizadorPos.AplicarAsync(archivo, inventario.RutaPos, cancellationToken);

            if (!actualizadorPos.Validar(inventario.RutaPos))
            {
                await clienteOperaciones.ReportarEventoAsync(
                    credenciales,
                    Evento(instruccion, "VALIDATION_FAILED", "No se encontro Zabyca.Pos.Desktop.exe despues de actualizar", inventario, archivo, respaldo),
                    cancellationToken
                );
                throw new InvalidOperationException("Validacion POS fallida despues de actualizar");
            }

            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "VALIDATION_OK", "Validacion POS correcta", inventario, archivo, respaldo),
                cancellationToken
            );

            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "UPDATE_COMPLETED", "Actualizacion POS completada", inventario, archivo, respaldo),
                cancellationToken
            );

            await clienteOperaciones.ReportarResultadoAsync(
                credenciales,
                new ResultadoActualizacionAgente(
                    instruccion.IdObjetivoDespliegue!.Value,
                    IdempotencyKeyResultado(instruccion, "COMPLETED"),
                    "COMPLETED",
                    inventario.VersionPos,
                    instruccion.Version,
                    "Actualizacion POS completada"
                ),
                cancellationToken
            );
            await GuardarEstadoAsync("UPDATE_COMPLETED", instruccion, inventario, "COMPLETED", null, cancellationToken);

            if (posEstabaEjecutandose)
            {
                await procesoPos.IniciarAsync(inventario.RutaPos, cancellationToken);
            }
        }
        catch (Exception ex) when (respaldo is not null)
        {
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "UPDATE_FAILED", "Fallo despues de modificar POS: " + ex.Message, inventario, archivo, respaldo),
                cancellationToken
            );
            await EjecutarRollbackAsync(
                credenciales,
                instruccion,
                inventario,
                archivo,
                respaldo,
                posEstabaEjecutandose,
                ex,
                cancellationToken
            );
        }
        catch (Exception ex)
        {
            await ReportarFalloSinRollbackAsync(
                credenciales,
                instruccion,
                inventario,
                "UPDATE_FAILED",
                "Fallo antes de modificar POS: " + ex.Message,
                cancellationToken
            );
            throw;
        }
    }

    private static void ValidarInstruccion(InstruccionActualizacion instruccion)
    {
        if (!string.Equals(instruccion.TipoInstruccion, TipoInstruccionActualizarPos, StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Tipo de instruccion no soportado: " + instruccion.TipoInstruccion);
        }

        if (instruccion.IdObjetivoDespliegue is null || instruccion.IdPaquete is null)
        {
            throw new InvalidOperationException("La instruccion no contiene identificadores de despliegue completos");
        }

        if (string.IsNullOrWhiteSpace(instruccion.Version)
            || string.IsNullOrWhiteSpace(instruccion.UrlDescarga)
            || string.IsNullOrWhiteSpace(instruccion.ChecksumSha256))
        {
            throw new InvalidOperationException("La instruccion no contiene datos suficientes para descargar el paquete");
        }
    }

    private bool EsHoraOficialAlcanzada(InstruccionActualizacion instruccion)
    {
        if (instruccion.HoraOficialActualizacion is null)
        {
            return true;
        }

        TimeOnly ahora = TimeOnly.FromDateTime(relojSistema.Ahora().LocalDateTime);
        if (instruccion.HoraForzadaActualizacion is not null
            && instruccion.HoraForzadaActualizacion < instruccion.HoraOficialActualizacion)
        {
            return ahora >= instruccion.HoraOficialActualizacion || ahora <= instruccion.HoraForzadaActualizacion;
        }

        return ahora >= instruccion.HoraOficialActualizacion;
    }

    private bool EsHoraForzadaAlcanzada(InstruccionActualizacion instruccion)
    {
        if (instruccion.HoraForzadaActualizacion is null)
        {
            return false;
        }

        TimeOnly ahora = TimeOnly.FromDateTime(relojSistema.Ahora().LocalDateTime);
        if (instruccion.HoraOficialActualizacion is not null
            && instruccion.HoraForzadaActualizacion < instruccion.HoraOficialActualizacion)
        {
            return ahora >= instruccion.HoraForzadaActualizacion && ahora < instruccion.HoraOficialActualizacion;
        }

        return ahora >= instruccion.HoraForzadaActualizacion;
    }

    private async Task EnviarAvisosPendientesAsync(
        CredencialesAgente credenciales,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        CancellationToken cancellationToken
    )
    {
        if (instruccion.IdObjetivoDespliegue is null
            || string.IsNullOrWhiteSpace(instruccion.Version)
            || instruccion.Avisos.Count == 0)
        {
            return;
        }

        foreach (TimeOnly horaAviso in instruccion.Avisos.Where(hora => EsHoraAvisoAlcanzada(instruccion, hora)))
        {
            bool enviado = await estadoAvisosActualizacion.FueEnviadoAsync(
                instruccion.IdObjetivoDespliegue.Value,
                horaAviso,
                cancellationToken
            );

            if (enviado)
            {
                continue;
            }

            var aviso = new AvisoUsuario(
                instruccion.IdObjetivoDespliegue.Value,
                instruccion.Version,
                horaAviso,
                instruccion.HoraForzadaActualizacion,
                "El POS tiene una actualizacion pendiente. Se cerrara automaticamente en la hora forzada si continua activo."
            );

            await avisadorUsuario.EnviarAsync(aviso, cancellationToken);
            await estadoAvisosActualizacion.RegistrarEnviadoAsync(
                instruccion.IdObjetivoDespliegue.Value,
                horaAviso,
                cancellationToken
            );

            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                EventoAviso(instruccion, horaAviso, inventario),
                cancellationToken
            );
        }
    }

    private bool EsHoraAvisoAlcanzada(InstruccionActualizacion instruccion, TimeOnly horaAviso)
    {
        TimeOnly ahora = TimeOnly.FromDateTime(relojSistema.Ahora().LocalDateTime);

        if (instruccion.HoraOficialActualizacion is not null
            && horaAviso < instruccion.HoraOficialActualizacion)
        {
            return ahora >= horaAviso && ahora < instruccion.HoraOficialActualizacion;
        }

        return ahora >= horaAviso;
    }

    private static EventoAgente Evento(
        InstruccionActualizacion instruccion,
        string tipoEvento,
        string mensaje,
        DatosInventarioEquipo inventario,
        ArchivoPaqueteLocal? archivo,
        RespaldoPos? respaldo = null
    )
    {
        Dictionary<string, object?> metadatos = new()
        {
            ["packageId"] = instruccion.IdPaquete,
            ["downloadUrl"] = instruccion.UrlDescarga
        };

        if (archivo is not null)
        {
            metadatos["localPath"] = archivo.RutaArchivo;
            metadatos["sizeBytes"] = archivo.TamanoBytes;
            metadatos["sha256"] = archivo.ChecksumSha256;
            metadatos["signatureStatus"] = string.IsNullOrWhiteSpace(instruccion.Firma) ? "UNSIGNED" : "VALIDATED";
            metadatos["signingKeyId"] = instruccion.IdClaveFirma;
        }

        if (respaldo is not null)
        {
            metadatos["backupPath"] = respaldo.RutaRespaldo;
        }

        return new EventoAgente(
            instruccion.IdObjetivoDespliegue,
            IdempotencyKeyEvento(instruccion, tipoEvento),
            tipoEvento,
            mensaje,
            inventario.VersionPos,
            instruccion.Version,
            metadatos
        );
    }

    private async Task<ArchivoPaqueteLocal> DescargarConReintentosAsync(
        CredencialesAgente credenciales,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        CancellationToken cancellationToken
    )
    {
        Exception? ultimoError = null;
        int intentos = Math.Max(1, opciones.MaxIntentosDescarga);

        for (int intento = 1; intento <= intentos; intento++)
        {
            try
            {
                await clienteOperaciones.ReportarEventoAsync(
                    credenciales,
                    Evento(instruccion, "DOWNLOAD_STARTED", $"Descarga de paquete iniciada. Intento {intento}/{intentos}", inventario, null),
                    cancellationToken
                );

                await using Stream paquete = await clienteOperaciones.DescargarPaqueteAsync(
                    credenciales,
                    instruccion.UrlDescarga!,
                    cancellationToken
                );
                ArchivoPaqueteLocal archivo = await almacenamientoPaquetes.GuardarAsync(
                    instruccion.IdPaquete!.Value,
                    instruccion.Version!,
                    paquete,
                    cancellationToken
                );

                await clienteOperaciones.ReportarEventoAsync(
                    credenciales,
                    Evento(instruccion, "DOWNLOAD_COMPLETED", "Descarga de paquete completada", inventario, archivo),
                    cancellationToken
                );
                return archivo;
            }
            catch (Exception ex) when (intento < intentos)
            {
                ultimoError = ex;
                logger.LogWarning(ex, "Fallo descarga paquete {IdPaquete}. Intento {Intento}/{Intentos}", instruccion.IdPaquete, intento, intentos);
                await Task.Delay(Backoff(intento), cancellationToken);
            }
            catch (Exception ex)
            {
                ultimoError = ex;
            }
        }

        throw new InvalidOperationException("No se pudo descargar el paquete despues de reintentos: " + ultimoError?.Message, ultimoError);
    }

    private TimeSpan Backoff(int intento)
    {
        int segundos = Math.Min(
            opciones.BackoffMaximoSegundos,
            opciones.BackoffInicialSegundos * (int)Math.Pow(2, Math.Max(0, intento - 1))
        );
        return TimeSpan.FromSeconds(Math.Max(1, segundos));
    }

    private static bool FirmaValida(InstruccionActualizacion instruccion)
    {
        if (string.IsNullOrWhiteSpace(instruccion.Firma)
            || string.IsNullOrWhiteSpace(instruccion.ClavePublicaFirmaPem))
        {
            return true;
        }

        if (!string.Equals(instruccion.AlgoritmoFirma, "SHA256withRSA", StringComparison.OrdinalIgnoreCase)
            || string.IsNullOrWhiteSpace(instruccion.ChecksumSha256))
        {
            return false;
        }

        try
        {
            using RSA rsa = RSA.Create();
            rsa.ImportFromPem(instruccion.ClavePublicaFirmaPem);
            byte[] datos = Encoding.UTF8.GetBytes(instruccion.ChecksumSha256);
            byte[] firma = Convert.FromBase64String(instruccion.Firma);
            return rsa.VerifyData(datos, firma, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
        }
        catch
        {
            return false;
        }
    }

    private async Task ReportarFalloSinRollbackAsync(
        CredencialesAgente credenciales,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        string tipoEvento,
        string mensaje,
        CancellationToken cancellationToken
    )
    {
        await clienteOperaciones.ReportarEventoAsync(
            credenciales,
            Evento(instruccion, tipoEvento, mensaje, inventario, null),
            cancellationToken
        );
        await ReportarResultadoFallidoAsync(credenciales, instruccion, inventario, "FAILED", mensaje, cancellationToken);
        await GuardarEstadoAsync(tipoEvento, instruccion, inventario, "FAILED", mensaje, cancellationToken);
    }

    private Task ReportarResultadoFallidoAsync(
        CredencialesAgente credenciales,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        string estado,
        string mensaje,
        CancellationToken cancellationToken
    )
    {
        return clienteOperaciones.ReportarResultadoAsync(
            credenciales,
            new ResultadoActualizacionAgente(
                instruccion.IdObjetivoDespliegue!.Value,
                IdempotencyKeyResultado(instruccion, estado),
                estado,
                inventario.VersionPos,
                instruccion.Version,
                mensaje
            ),
            cancellationToken
        );
    }

    private Task GuardarEstadoAsync(
        string accion,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        string? resultado,
        string? error,
        CancellationToken cancellationToken
    )
    {
        return estadoLocalAgente.GuardarAsync(new EstadoLocalAgente(
            accion,
            instruccion.Version ?? inventario.VersionPos,
            resultado,
            error,
            instruccion.IdObjetivoDespliegue,
            instruccion.IdPaquete,
            DateTimeOffset.Now
        ), cancellationToken);
    }

    private static EventoAgente EventoAviso(
        InstruccionActualizacion instruccion,
        TimeOnly horaAviso,
        DatosInventarioEquipo inventario
    )
    {
        Dictionary<string, object?> metadatos = new()
        {
            ["packageId"] = instruccion.IdPaquete,
            ["warningTime"] = horaAviso.ToString("HH:mm:ss"),
            ["forceUpdateTime"] = instruccion.HoraForzadaActualizacion?.ToString("HH:mm:ss")
        };

        return new EventoAgente(
            instruccion.IdObjetivoDespliegue,
            IdempotencyKeyEvento(instruccion, "USER_WARNING_SENT:" + horaAviso.ToString("HHmmss")),
            "USER_WARNING_SENT",
            "Aviso de actualizacion enviado al usuario",
            inventario.VersionPos,
            instruccion.Version,
            metadatos
        );
    }

    private async Task EjecutarRollbackAsync(
        CredencialesAgente credenciales,
        InstruccionActualizacion instruccion,
        DatosInventarioEquipo inventario,
        ArchivoPaqueteLocal archivo,
        RespaldoPos respaldo,
        bool reiniciarPos,
        Exception error,
        CancellationToken cancellationToken
    )
    {
        await clienteOperaciones.ReportarEventoAsync(
            credenciales,
            Evento(instruccion, "ROLLBACK_STARTED", "Iniciando rollback POS", inventario, archivo, respaldo),
            cancellationToken
        );

        try
        {
            await respaldoPos.RestaurarAsync(respaldo, inventario.RutaPos, cancellationToken);
            if (reiniciarPos)
            {
                await procesoPos.IniciarAsync(inventario.RutaPos, cancellationToken);
            }

            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "ROLLBACK_COMPLETED", "Rollback POS completado", inventario, archivo, respaldo),
                cancellationToken
            );
            await clienteOperaciones.ReportarResultadoAsync(
                credenciales,
                new ResultadoActualizacionAgente(
                    instruccion.IdObjetivoDespliegue!.Value,
                    IdempotencyKeyResultado(instruccion, "ROLLBACK_COMPLETED"),
                    "ROLLBACK_COMPLETED",
                    inventario.VersionPos,
                    instruccion.Version,
                    "Fallo actualizacion, rollback completado: " + error.Message
                ),
                cancellationToken
            );
            await GuardarEstadoAsync("ROLLBACK_COMPLETED", instruccion, inventario, "ROLLBACK_COMPLETED", error.Message, cancellationToken);
        }
        catch (Exception rollbackError)
        {
            await clienteOperaciones.ReportarEventoAsync(
                credenciales,
                Evento(instruccion, "ROLLBACK_FAILED", "Rollback POS fallido: " + rollbackError.Message, inventario, archivo, respaldo),
                cancellationToken
            );
            await clienteOperaciones.ReportarResultadoAsync(
                credenciales,
                new ResultadoActualizacionAgente(
                    instruccion.IdObjetivoDespliegue!.Value,
                    IdempotencyKeyResultado(instruccion, "ROLLBACK_FAILED"),
                    "ROLLBACK_FAILED",
                    inventario.VersionPos,
                    instruccion.Version,
                    "Fallo rollback: " + rollbackError.Message
                ),
                cancellationToken
            );
            await GuardarEstadoAsync("ROLLBACK_FAILED", instruccion, inventario, "ROLLBACK_FAILED", rollbackError.Message, cancellationToken);
        }
    }

    private static string IdempotencyKeyEvento(InstruccionActualizacion instruccion, string tipoEvento)
    {
        return $"{instruccion.IdObjetivoDespliegue}:event:{tipoEvento}";
    }

    private static string IdempotencyKeyResultado(InstruccionActualizacion instruccion, string estado)
    {
        return $"{instruccion.IdObjetivoDespliegue}:result:{estado}";
    }
}
