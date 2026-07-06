using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Infraestructura.Avisos;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Xunit;

namespace Farmamia.Agent.Tests;

public sealed class AvisadorUsuarioSesionInteractivaTests : IDisposable
{
    private readonly string raiz = Path.Combine(Path.GetTempPath(), "farmamia-agent-avisos-tests", Guid.NewGuid().ToString("N"));

    [Fact]
    public async Task EnviarAsync_registra_el_aviso_en_archivo_y_no_lanza_sin_sesion_interactiva()
    {
        // En un runner de CI no hay sesion de consola interactiva activa, asi que este test
        // ejercita la ruta de degradacion elegante (WTSGetActiveConsoleSessionId devuelve
        // "sin sesion") y confirma que el aviso no se pierde: sigue quedando en archivo.
        using ILoggerFactory loggerFactory = LoggerFactory.Create(_ => { });
        var opciones = Options.Create(new OpcionesAgente { RutaAgente = raiz });
        var avisador = new AvisadorUsuarioSesionInteractiva(opciones, loggerFactory.CreateLogger<AvisadorUsuarioSesionInteractiva>());

        Guid idObjetivo = Guid.NewGuid();
        var aviso = new AvisoUsuario(
            idObjetivo,
            "2026.06.2-success",
            new TimeOnly(21, 0),
            new TimeOnly(22, 0),
            "El POS tiene una actualizacion pendiente."
        );

        await avisador.EnviarAsync(aviso, CancellationToken.None);

        string carpetaAvisos = Path.Combine(raiz, "State", "Avisos");
        string[] archivos = Directory.GetFiles(carpetaAvisos);
        string archivo = Assert.Single(archivos);
        Assert.Contains(idObjetivo.ToString("N"), archivo);
        string contenido = await File.ReadAllTextAsync(archivo);
        Assert.Contains("El POS tiene una actualizacion pendiente.", contenido);
        Assert.Contains("2026.06.2-success", contenido);
    }

    public void Dispose()
    {
        if (Directory.Exists(raiz))
        {
            Directory.Delete(raiz, recursive: true);
        }
    }
}
