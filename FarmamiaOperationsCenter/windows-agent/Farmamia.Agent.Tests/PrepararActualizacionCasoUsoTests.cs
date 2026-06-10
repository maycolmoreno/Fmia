using System.IO.Compression;
using System.Security.Cryptography;
using Farmamia.Agent.Aplicacion.CasosUso;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Actualizacion;
using Farmamia.Agent.Infraestructura.Almacenamiento;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Xunit;

namespace Farmamia.Agent.Tests;

public sealed class PrepararActualizacionCasoUsoTests : IDisposable
{
    private readonly string raiz = Path.Combine(Path.GetTempPath(), "farmamia-agent-usecase-tests", Guid.NewGuid().ToString("N"));

    [Fact]
    public async Task Zip_valido_reporta_update_completed_y_resultado_completed()
    {
        string rutaPos = CrearPosOriginal();
        (byte[] zip, string checksum) = CrearZipBytes(("Zabyca.Pos.Desktop.exe", "version-nueva"), ("version.txt", "2026.06.2-success"));
        var cliente = ClienteConInstruccion(zip, checksum);
        var estadoLocal = new FakeEstadoLocalAgente();

        PrepararActualizacionCasoUso casoUso = CrearCasoUso(cliente, rutaPos, estadoLocal: estadoLocal);

        await casoUso.EjecutarAsync(new CredencialesAgente(Guid.NewGuid(), "token"), CancellationToken.None);

        Assert.Equal("version-nueva", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "UPDATE_STARTED");
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "UPDATE_COMPLETED");
        ResultadoActualizacionAgente resultado = Assert.Single(cliente.Resultados);
        Assert.Equal("COMPLETED", resultado.Estado);
        Assert.Equal("1.0.0", resultado.VersionAnterior);
        Assert.Equal("2026.06.2-success", resultado.VersionNueva);
        Assert.Equal("UPDATE_COMPLETED", estadoLocal.Estado?.UltimaAccion);
    }

    [Fact]
    public async Task Checksum_incorrecto_reporta_validation_failed_y_no_modifica_pos()
    {
        string rutaPos = CrearPosOriginal();
        (byte[] zip, _) = CrearZipBytes(("Zabyca.Pos.Desktop.exe", "version-nueva"));
        var cliente = ClienteConInstruccion(zip, "checksum-incorrecto");

        PrepararActualizacionCasoUso casoUso = CrearCasoUso(cliente, rutaPos);

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            casoUso.EjecutarAsync(new CredencialesAgente(Guid.NewGuid(), "token"), CancellationToken.None)
        );

        Assert.Equal("version-original", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "VALIDATION_FAILED");
        Assert.DoesNotContain(cliente.Eventos, evento => evento.TipoEvento == "UPDATE_STARTED");
        ResultadoActualizacionAgente resultado = Assert.Single(cliente.Resultados);
        Assert.Equal("FAILED", resultado.Estado);
        Assert.Equal("1.0.0", resultado.VersionAnterior);
    }

    [Fact]
    public async Task Descarga_falla_y_luego_reintenta_hasta_completar()
    {
        string rutaPos = CrearPosOriginal();
        (byte[] zip, string checksum) = CrearZipBytes(("Zabyca.Pos.Desktop.exe", "version-nueva"));
        var cliente = ClienteConInstruccion(zip, checksum);
        cliente.FallosDescargaAntesDeExito = 2;

        PrepararActualizacionCasoUso casoUso = CrearCasoUso(cliente, rutaPos, maxIntentosDescarga: 3);

        await casoUso.EjecutarAsync(new CredencialesAgente(Guid.NewGuid(), "token"), CancellationToken.None);

        Assert.Equal(3, cliente.IntentosDescarga);
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "DOWNLOAD_COMPLETED");
        Assert.Equal("COMPLETED", Assert.Single(cliente.Resultados).Estado);
    }

    [Fact]
    public async Task Falla_despues_de_modificar_pos_ejecuta_rollback_y_no_deja_version_rota()
    {
        string rutaPos = CrearPosOriginal();
        (byte[] zip, string checksum) = CrearZipBytes(("Zabyca.Pos.Desktop.exe", "version-nueva"));
        var cliente = ClienteConInstruccion(zip, checksum);
        var estadoLocal = new FakeEstadoLocalAgente();

        PrepararActualizacionCasoUso casoUso = CrearCasoUso(
            cliente,
            rutaPos,
            actualizadorPos: new FakeActualizadorPosQueFallaDespuesDeModificar(),
            estadoLocal: estadoLocal
        );

        await casoUso.EjecutarAsync(new CredencialesAgente(Guid.NewGuid(), "token"), CancellationToken.None);

        Assert.Equal("version-original", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "UPDATE_FAILED");
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "ROLLBACK_STARTED");
        Assert.Contains(cliente.Eventos, evento => evento.TipoEvento == "ROLLBACK_COMPLETED");
        ResultadoActualizacionAgente resultado = Assert.Single(cliente.Resultados);
        Assert.Equal("ROLLBACK_COMPLETED", resultado.Estado);
        Assert.Equal("1.0.0", resultado.VersionAnterior);
        Assert.Equal("2026.06.2-success", resultado.VersionNueva);
        Assert.Equal("ROLLBACK_COMPLETED", estadoLocal.Estado?.UltimaAccion);
    }

    public void Dispose()
    {
        if (Directory.Exists(raiz))
        {
            Directory.Delete(raiz, recursive: true);
        }
    }

    private PrepararActualizacionCasoUso CrearCasoUso(
        FakeClienteOperaciones cliente,
        string rutaPos,
        int maxIntentosDescarga = 1,
        IActualizadorPos? actualizadorPos = null,
        FakeEstadoLocalAgente? estadoLocal = null
    )
    {
        OpcionesAgente opciones = new()
        {
            RutaAgente = raiz,
            RutaPos = rutaPos,
            MaxIntentosDescarga = maxIntentosDescarga,
            BackoffInicialSegundos = 1,
            BackoffMaximoSegundos = 1
        };

        var inventario = new FakeInventario(new DatosInventarioEquipo(
            "POS-DEMO-001",
            "127.0.0.1",
            "00-00-00-00-00-01",
            "Windows",
            "1.0.0",
            "1.0.0",
            rutaPos,
            1024,
            2048,
            false
        ));

        using ILoggerFactory loggerFactory = LoggerFactory.Create(_ => { });
        return new PrepararActualizacionCasoUso(
            cliente,
            new AlmacenamientoPaquetesLocal(Options.Create(opciones)),
            inventario,
            new RespaldoPosLocal(Options.Create(opciones)),
            actualizadorPos ?? new ActualizadorPosZip(),
            new FakeProcesoPos(),
            new FakeRelojSistema(),
            new FakeAvisadorUsuario(),
            new FakeEstadoAvisosActualizacion(),
            estadoLocal ?? new FakeEstadoLocalAgente(),
            loggerFactory.CreateLogger<PrepararActualizacionCasoUso>(),
            Options.Create(opciones)
        );
    }

    private FakeClienteOperaciones ClienteConInstruccion(byte[] zip, string checksum)
    {
        return new FakeClienteOperaciones
        {
            Paquete = zip,
            Instruccion = new InstruccionActualizacion(
                true,
                "UPDATE_POS",
                Guid.NewGuid(),
                Guid.NewGuid(),
                "2026.06.2-success",
                "/api/packages/demo/download",
                checksum,
                null,
                null,
                []
            )
        };
    }

    private string CrearPosOriginal()
    {
        string rutaPos = Path.Combine(raiz, "POS");
        Directory.CreateDirectory(rutaPos);
        File.WriteAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "version-original");
        return rutaPos;
    }

    private static (byte[] Zip, string Checksum) CrearZipBytes(params (string Ruta, string Contenido)[] archivos)
    {
        using MemoryStream memoria = new();
        using (ZipArchive zip = new(memoria, ZipArchiveMode.Create, leaveOpen: true))
        {
            foreach ((string ruta, string contenido) in archivos)
            {
                ZipArchiveEntry entrada = zip.CreateEntry(ruta);
                using StreamWriter writer = new(entrada.Open());
                writer.Write(contenido);
            }
        }

        byte[] bytes = memoria.ToArray();
        string checksum = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();
        return (bytes, checksum);
    }

    private sealed class FakeClienteOperaciones : IClienteOperacionesFarmamia
    {
        public required InstruccionActualizacion Instruccion { get; init; }

        public required byte[] Paquete { get; init; }

        public int FallosDescargaAntesDeExito { get; set; }

        public int IntentosDescarga { get; private set; }

        public List<EventoAgente> Eventos { get; } = [];

        public List<ResultadoActualizacionAgente> Resultados { get; } = [];

        public Task<CredencialesAgente> RegistrarAsync(string codigoSucursal, DatosInventarioEquipo inventario, CancellationToken cancellationToken)
        {
            return Task.FromResult(new CredencialesAgente(Guid.NewGuid(), "token"));
        }

        public Task EnviarLatidoAsync(CredencialesAgente credenciales, LatidoAgente latido, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }

        public Task<InstruccionActualizacion?> ConsultarInstruccionAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
        {
            return Task.FromResult<InstruccionActualizacion?>(Instruccion);
        }

        public Task<Stream> DescargarPaqueteAsync(CredencialesAgente credenciales, string urlDescarga, CancellationToken cancellationToken)
        {
            IntentosDescarga++;
            if (IntentosDescarga <= FallosDescargaAntesDeExito)
            {
                throw new HttpRequestException("API de descarga no disponible");
            }

            return Task.FromResult<Stream>(new MemoryStream(Paquete));
        }

        public Task ReportarEventoAsync(CredencialesAgente credenciales, EventoAgente evento, CancellationToken cancellationToken)
        {
            Eventos.Add(evento);
            return Task.CompletedTask;
        }

        public Task ReportarResultadoAsync(CredencialesAgente credenciales, ResultadoActualizacionAgente resultado, CancellationToken cancellationToken)
        {
            Resultados.Add(resultado);
            return Task.CompletedTask;
        }
    }

    private sealed class FakeInventario : IInventarioEquipo
    {
        private readonly DatosInventarioEquipo inventario;

        public FakeInventario(DatosInventarioEquipo inventario)
        {
            this.inventario = inventario;
        }

        public DatosInventarioEquipo ObtenerInventario()
        {
            return inventario;
        }
    }

    private sealed class FakeProcesoPos : IProcesoPos
    {
        public Task<bool> CerrarSiEjecutandoseAsync(CancellationToken cancellationToken)
        {
            return Task.FromResult(true);
        }

        public Task IniciarAsync(string rutaPos, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }

    private sealed class FakeActualizadorPosQueFallaDespuesDeModificar : IActualizadorPos
    {
        public Task AplicarAsync(ArchivoPaqueteLocal paquete, string rutaPos, CancellationToken cancellationToken)
        {
            File.WriteAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "version-rota");
            return Task.CompletedTask;
        }

        public bool Validar(string rutaPos)
        {
            return false;
        }
    }

    private sealed class FakeRelojSistema : IRelojSistema
    {
        public DateTimeOffset Ahora()
        {
            return DateTimeOffset.Now;
        }
    }

    private sealed class FakeAvisadorUsuario : IAvisadorUsuario
    {
        public Task EnviarAsync(AvisoUsuario aviso, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }

    private sealed class FakeEstadoAvisosActualizacion : IEstadoAvisosActualizacion
    {
        public Task<bool> FueEnviadoAsync(Guid idObjetivoDespliegue, TimeOnly horaAviso, CancellationToken cancellationToken)
        {
            return Task.FromResult(false);
        }

        public Task RegistrarEnviadoAsync(Guid idObjetivoDespliegue, TimeOnly horaAviso, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }

    private sealed class FakeEstadoLocalAgente : IEstadoLocalAgente
    {
        public EstadoLocalAgente? Estado { get; private set; }

        public Task<EstadoLocalAgente?> LeerAsync(CancellationToken cancellationToken)
        {
            return Task.FromResult(Estado);
        }

        public Task GuardarAsync(EstadoLocalAgente estado, CancellationToken cancellationToken)
        {
            Estado = estado;
            return Task.CompletedTask;
        }
    }
}
