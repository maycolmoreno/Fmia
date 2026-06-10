using System.IO.Compression;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Infraestructura.Actualizacion;
using Farmamia.Agent.Infraestructura.Configuracion;
using Microsoft.Extensions.Options;
using Xunit;

namespace Farmamia.Agent.Tests;

public sealed class InfraestructuraArchivosTests : IDisposable
{
    private readonly string raiz = Path.Combine(Path.GetTempPath(), "farmamia-agent-tests", Guid.NewGuid().ToString("N"));

    [Fact]
    public async Task Configuracion_persiste_credenciales_y_las_recupera_despues_de_reinicio()
    {
        OpcionesAgente opciones = Opciones();
        var configuracion = new ConfiguracionLocalAgente(Options.Create(opciones));
        CredencialesAgente credenciales = new(Guid.NewGuid(), "token-tecnico");

        await configuracion.PrepararEstructuraAsync(CancellationToken.None);
        await configuracion.GuardarCredencialesAsync(credenciales, CancellationToken.None);

        var configuracionReiniciada = new ConfiguracionLocalAgente(Options.Create(opciones));
        CredencialesAgente? recuperadas = await configuracionReiniciada.LeerCredencialesAsync(CancellationToken.None);

        Assert.Equal(credenciales, recuperadas);
        Assert.True(File.Exists(Path.Combine(raiz, "config.json")));
        Assert.True(Directory.Exists(Path.Combine(raiz, "Downloads")));
        Assert.True(Directory.Exists(Path.Combine(raiz, "Backups")));
        Assert.True(Directory.Exists(Path.Combine(raiz, "Logs")));
        Assert.True(Directory.Exists(Path.Combine(raiz, "Temp")));
        Assert.True(Directory.Exists(Path.Combine(raiz, "State")));
    }

    [Fact]
    public async Task Respaldo_crea_copia_y_rollback_restaurar_archivos_originales()
    {
        string rutaPos = Path.Combine(raiz, "POS");
        Directory.CreateDirectory(rutaPos);
        File.WriteAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "version-original");
        File.WriteAllText(Path.Combine(rutaPos, "config.ini"), "original");

        var respaldo = new RespaldoPosLocal(Options.Create(Opciones()));

        RespaldoPos copia = await respaldo.CrearAsync(rutaPos, "2026.06.1", CancellationToken.None);
        File.WriteAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "version-nueva-rota");
        File.Delete(Path.Combine(rutaPos, "config.ini"));

        await respaldo.RestaurarAsync(copia, rutaPos, CancellationToken.None);

        Assert.Equal("version-original", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Equal("original", File.ReadAllText(Path.Combine(rutaPos, "config.ini")));
    }

    [Fact]
    public async Task Actualizador_zip_valido_copia_archivos_y_valida_ejecutable_principal()
    {
        string zip = Path.Combine(raiz, "paquete-valido.zip");
        string rutaPos = Path.Combine(raiz, "POS");
        CrearZip(zip, ("Zabyca.Pos.Desktop.exe", "version-nueva"), ("version.txt", "2026.06.2-success"));

        var actualizador = new ActualizadorPosZip();
        var paquete = new ArchivoPaqueteLocal(zip, new FileInfo(zip).Length, "checksum-no-relevante");

        await actualizador.AplicarAsync(paquete, rutaPos, CancellationToken.None);

        Assert.True(actualizador.Validar(rutaPos));
        Assert.Equal("version-nueva", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Equal("2026.06.2-success", File.ReadAllText(Path.Combine(rutaPos, "version.txt")));
    }

    [Fact]
    public async Task Actualizador_zip_sin_ejecutable_no_supera_validacion_final()
    {
        string zip = Path.Combine(raiz, "paquete-invalido.zip");
        string rutaPos = Path.Combine(raiz, "POS");
        CrearZip(zip, ("version.txt", "2026.06.2-fail"));

        var actualizador = new ActualizadorPosZip();
        var paquete = new ArchivoPaqueteLocal(zip, new FileInfo(zip).Length, "checksum-no-relevante");

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            actualizador.AplicarAsync(paquete, rutaPos, CancellationToken.None)
        );
        Assert.False(actualizador.Validar(rutaPos));
    }

    public void Dispose()
    {
        if (Directory.Exists(raiz))
        {
            Directory.Delete(raiz, recursive: true);
        }
    }

    private OpcionesAgente Opciones()
    {
        return new OpcionesAgente
        {
            RutaAgente = raiz,
            RutaPos = Path.Combine(raiz, "POS")
        };
    }

    private static void CrearZip(string rutaZip, params (string Ruta, string Contenido)[] archivos)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(rutaZip)!);
        using ZipArchive zip = ZipFile.Open(rutaZip, ZipArchiveMode.Create);
        foreach ((string ruta, string contenido) in archivos)
        {
            ZipArchiveEntry entrada = zip.CreateEntry(ruta);
            using StreamWriter writer = new(entrada.Open());
            writer.Write(contenido);
        }
    }
}
