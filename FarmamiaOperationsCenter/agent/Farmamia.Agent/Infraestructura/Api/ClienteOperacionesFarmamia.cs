using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json.Serialization;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Infraestructura.Api;

public sealed class ClienteOperacionesFarmamia : IClienteOperacionesFarmamia
{
    private readonly HttpClient httpClient;

    public ClienteOperacionesFarmamia(HttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public async Task<CredencialesAgente> RegistrarAsync(
        string codigoSucursal,
        DatosInventarioEquipo inventario,
        CancellationToken cancellationToken
    )
    {
        var solicitud = new SolicitudRegistroAgente(
            codigoSucursal,
            inventario.NombreEquipo,
            inventario.DireccionIp,
            inventario.DireccionMac,
            inventario.VersionWindows,
            inventario.VersionAgente,
            inventario.VersionPos,
            inventario.RutaPos
        );

        using HttpResponseMessage respuesta = await httpClient.PostAsJsonAsync(
            "/api/agent/register",
            solicitud,
            cancellationToken
        );
        respuesta.EnsureSuccessStatusCode();

        RespuestaRegistroAgente? registro = await respuesta.Content.ReadFromJsonAsync<RespuestaRegistroAgente>(
            cancellationToken: cancellationToken
        );

        if (registro is null)
        {
            throw new InvalidOperationException("La API no devolvio credenciales de agente");
        }

        return new CredencialesAgente(registro.DeviceId, registro.AgentToken);
    }

    public async Task EnviarLatidoAsync(
        CredencialesAgente credenciales,
        LatidoAgente latido,
        CancellationToken cancellationToken
    )
    {
        using var solicitud = new HttpRequestMessage(HttpMethod.Post, "/api/agent/heartbeat")
        {
            Content = JsonContent.Create(new SolicitudLatido(
                latido.IdEquipo,
                latido.VersionPos,
                latido.DiscoLibreMb,
                latido.DiscoTotalMb,
                latido.ProcesoPosEjecutandose,
                latido.LatenciaMs,
                latido.PorcentajePerdidaPaquetes
            ))
        };
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);

        using HttpResponseMessage respuesta = await httpClient.SendAsync(solicitud, cancellationToken);
        respuesta.EnsureSuccessStatusCode();
    }

    public async Task<InstruccionActualizacion?> ConsultarInstruccionAsync(
        CredencialesAgente credenciales,
        CancellationToken cancellationToken
    )
    {
        using var solicitud = new HttpRequestMessage(
            HttpMethod.Get,
            $"/api/agent/{credenciales.IdEquipo}/instructions"
        );
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);

        using HttpResponseMessage respuesta = await httpClient.SendAsync(solicitud, cancellationToken);
        respuesta.EnsureSuccessStatusCode();

        RespuestaInstruccion? instruccion = await respuesta.Content.ReadFromJsonAsync<RespuestaInstruccion>(
            cancellationToken: cancellationToken
        );

        if (instruccion is null || !instruccion.HasInstruction)
        {
            return null;
        }

        return new InstruccionActualizacion(
            instruccion.HasInstruction,
            instruccion.InstructionType,
            instruccion.DeploymentTargetId,
            instruccion.PackageId,
            instruccion.Version,
            instruccion.DownloadUrl,
            instruccion.Sha256Checksum,
            instruccion.Signature,
            instruccion.SignatureAlgorithm,
            instruccion.SigningKeyId,
            instruccion.SigningPublicKeyPem,
            instruccion.OfficialUpdateTime,
            instruccion.ForceUpdateTime,
            instruccion.Warnings ?? []
        );
    }

    public async Task<Stream> DescargarPaqueteAsync(
        CredencialesAgente credenciales,
        string urlDescarga,
        Guid idObjetivoDespliegue,
        CancellationToken cancellationToken
    )
    {
        using var solicitud = new HttpRequestMessage(HttpMethod.Get, urlDescarga);
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);

        using HttpResponseMessage respuesta = await httpClient.SendAsync(
            solicitud,
            HttpCompletionOption.ResponseHeadersRead,
            cancellationToken
        );
        respuesta.EnsureSuccessStatusCode();

        long totalBytes = respuesta.Content.Headers.ContentLength ?? -1;
        await using Stream remoto = await respuesta.Content.ReadAsStreamAsync(cancellationToken);
        var memoria = new MemoryStream(totalBytes > 0 ? (int)totalBytes : 0);

        var buffer = new byte[81_920];
        long bytesLeidos = 0;
        int umbralSiguiente = 10; // reportar en 10%, 20%, ..., 100%
        int leido;

        while ((leido = await remoto.ReadAsync(buffer, cancellationToken)) > 0)
        {
            await memoria.WriteAsync(buffer.AsMemory(0, leido), cancellationToken);
            bytesLeidos += leido;

            if (totalBytes > 0)
            {
                double porcentaje = (double)bytesLeidos / totalBytes * 100.0;
                if (porcentaje >= umbralSiguiente)
                {
                    umbralSiguiente = (int)(Math.Floor(porcentaje / 10) + 1) * 10;
                    // fire-and-forget: el progreso es best-effort, no bloquea la descarga
                    _ = ReportarProgresoDescargaInternoAsync(
                        credenciales, idObjetivoDespliegue,
                        Math.Min(porcentaje, 100.0), cancellationToken);
                }
            }
        }

        memoria.Position = 0;
        return memoria;
    }

    private async Task ReportarProgresoDescargaInternoAsync(
        CredencialesAgente credenciales,
        Guid idObjetivoDespliegue,
        double porcentaje,
        CancellationToken cancellationToken
    )
    {
        try
        {
            using var solicitud = new HttpRequestMessage(
                HttpMethod.Post,
                $"/api/agent/{credenciales.IdEquipo}/download-progress"
            )
            {
                Content = JsonContent.Create(new
                {
                    idObjetivoDespliegue,
                    progreso = Math.Round(porcentaje, 2)
                })
            };
            solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);
            using HttpResponseMessage _ = await httpClient.SendAsync(solicitud, cancellationToken);
        }
        catch
        {
            // El progreso es informativo; si falla el POST no debe interrumpir la descarga
        }
    }

    public async Task ReportarEventoAsync(
        CredencialesAgente credenciales,
        EventoAgente evento,
        CancellationToken cancellationToken
    )
    {
        using var solicitud = new HttpRequestMessage(
            HttpMethod.Post,
            $"/api/agent/{credenciales.IdEquipo}/events"
        )
        {
            Content = JsonContent.Create(new SolicitudEventoAgente(
                evento.IdObjetivoDespliegue,
                evento.IdempotencyKey,
                evento.TipoEvento,
                evento.MensajeEvento,
                evento.VersionAnterior,
                evento.VersionNueva,
                evento.Metadatos
            ))
        };
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);

        using HttpResponseMessage respuesta = await httpClient.SendAsync(solicitud, cancellationToken);
        respuesta.EnsureSuccessStatusCode();
    }

    public async Task ReportarResultadoAsync(
        CredencialesAgente credenciales,
        ResultadoActualizacionAgente resultado,
        CancellationToken cancellationToken
    )
    {
        using var solicitud = new HttpRequestMessage(
            HttpMethod.Post,
            $"/api/agent/{credenciales.IdEquipo}/update-result"
        )
        {
            Content = JsonContent.Create(new SolicitudResultadoActualizacion(
                resultado.IdObjetivoDespliegue,
                resultado.IdempotencyKey,
                resultado.Estado,
                resultado.VersionAnterior,
                resultado.VersionNueva,
                resultado.Mensaje
            ))
        };
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);

        using HttpResponseMessage respuesta = await httpClient.SendAsync(solicitud, cancellationToken);
        respuesta.EnsureSuccessStatusCode();
    }

    private sealed record SolicitudRegistroAgente(
        string BranchCode,
        string Hostname,
        string IpAddress,
        string MacAddress,
        string WindowsVersion,
        string AgentVersion,
        string PosVersion,
        string PosPath
    );

    private sealed record RespuestaRegistroAgente(Guid DeviceId, string AgentToken, DateTimeOffset ServerTime);

    private sealed record SolicitudLatido(
        Guid DeviceId,
        string PosVersion,
        long DiskFreeMb,
        long DiskTotalMb,
        bool PosProcessRunning,
        int LatencyMs,
        decimal PacketLossPercent
    );

    private sealed record RespuestaInstruccion(
        bool HasInstruction,
        string? InstructionType,
        Guid? DeploymentTargetId,
        Guid? PackageId,
        string? Version,
        string? DownloadUrl,
        [property: JsonPropertyName("sha256Checksum")] string? Sha256Checksum,
        string? Signature,
        string? SignatureAlgorithm,
        string? SigningKeyId,
        string? SigningPublicKeyPem,
        TimeOnly? OfficialUpdateTime,
        TimeOnly? ForceUpdateTime,
        IReadOnlyList<TimeOnly>? Warnings
    );

    private sealed record SolicitudEventoAgente(
        Guid? DeploymentTargetId,
        string IdempotencyKey,
        string EventType,
        string? EventMessage,
        string? OldVersion,
        string? NewVersion,
        IReadOnlyDictionary<string, object?> Metadata
    );

    private sealed record SolicitudResultadoActualizacion(
        Guid DeploymentTargetId,
        string IdempotencyKey,
        string Status,
        string? OldVersion,
        string? NewVersion,
        string? Message
    );
}
