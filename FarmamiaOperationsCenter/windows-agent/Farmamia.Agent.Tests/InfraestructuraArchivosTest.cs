using System.IO.Compression;
using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Infraestructura.Actualizacion;
using Farmamia.Agent.Infraestructura.Configuracion;
using Microsoft.Extensions.Options;
using Xunit;

namespace Farmamia.Agent.Tests;

public sealed class InfraestructuraArchivosTest
{
    [Fact]
    public async Task ConfiguracionPersisteCredencialesDespuesDeReinicio()
    {
        using DirectorioTemporal temporal = new();
        var opciones = Options.Create(new OpcionesAgente { RutaAgente = temporal.Ruta });
        var configuracion = new ConfiguracionLocalAgente(opciones);
        var credenciales = new CredencialesAgente(Guid.NewGuid(), "token-tecnico");

        await configuracion.PrepararEstructuraAsync(CancellationToken.None);
        await configuracion.GuardarCredencialesAsync(credenciales, CancellationToken.None);

        var otraInstancia = new ConfiguracionLocalAgente(opciones);
        CredencialesAgente? recuperadas = await otraInstancia.LeerCredencialesAsync(CancellationToken.None);

        Assert.Equal(credenciales, recuperadas);
        string contenido = await File.ReadAllTextAsync(Path.Combine(temporal.Ruta, "State", "credenciales.json"));
        Assert.DoesNotContain("token-tecnico", contenido);
        Assert.Contains("proteccion", contenido);
    }

    [Fact]
    public async Task ConfiguracionLeeCredencialesLegacySinProteccion()
    {
        using DirectorioTemporal temporal = new();
        var opciones = Options.Create(new OpcionesAgente { RutaAgente = temporal.Ruta });
        var configuracion = new ConfiguracionLocalAgente(opciones);
        var credenciales = new CredencialesAgente(Guid.NewGuid(), "token-legacy");

        await configuracion.PrepararEstructuraAsync(CancellationToken.None);
        await File.WriteAllTextAsync(
            Path.Combine(temporal.Ruta, "State", "credenciales.json"),
            JsonSerializer.Serialize(credenciales, new JsonSerializerOptions(JsonSerializerDefaults.Web)),
            CancellationToken.None
        );

        CredencialesAgente? recuperadas = await configuracion.LeerCredencialesAsync(CancellationToken.None);

        Assert.Equal(credenciales, recuperadas);
    }

    [Fact]
    public async Task BackupYRollbackRestauranArchivosOriginales()
    {
        using DirectorioTemporal temporal = new();
        string rutaPos = Path.Combine(temporal.Ruta, "POS");
        Directory.CreateDirectory(rutaPos);
        await File.WriteAllTextAsync(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "original");
        await File.WriteAllTextAsync(Path.Combine(rutaPos, "version.txt"), "2026.06.1");
        var respaldo = new RespaldoPosLocal(Options.Create(new OpcionesAgente { RutaAgente = temporal.Ruta }));

        RespaldoPos copia = await respaldo.CrearAsync(rutaPos, "2026.06.1", CancellationToken.None);
        await File.WriteAllTextAsync(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe"), "modificado");

        await respaldo.RestaurarAsync(copia, rutaPos, CancellationToken.None);

        Assert.Equal("original", await File.ReadAllTextAsync(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
    }

    [Fact]
    public async Task ActualizadorValidaEjecutablePrincipal()
    {
        using DirectorioTemporal temporal = new();
        string rutaPos = Path.Combine(temporal.Ruta, "POS");
        string zipValido = Path.Combine(temporal.Ruta, "valido.zip");
        CrearZip(zipValido, ("Zabyca.Pos.Desktop.exe", "exe"));
        var actualizador = new ActualizadorPosZip();

        await actualizador.AplicarAsync(new ArchivoPaqueteLocal(zipValido, new FileInfo(zipValido).Length, "sha"), rutaPos, CancellationToken.None);

        Assert.True(actualizador.Validar(rutaPos));
    }

    [Fact]
    public async Task ActualizadorDetectaZipSinEjecutable()
    {
        using DirectorioTemporal temporal = new();
        string rutaPos = Path.Combine(temporal.Ruta, "POS");
        string zipInvalido = Path.Combine(temporal.Ruta, "invalido.zip");
        CrearZip(zipInvalido, ("version.txt", "2026.06.2"));
        var actualizador = new ActualizadorPosZip();

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            actualizador.AplicarAsync(new ArchivoPaqueteLocal(zipInvalido, new FileInfo(zipInvalido).Length, "sha"), rutaPos, CancellationToken.None)
        );
        Assert.False(actualizador.Validar(rutaPos));
    }

    private static void CrearZip(string ruta, params (string Nombre, string Contenido)[] archivos)
    {
        using ZipArchive zip = ZipFile.Open(ruta, ZipArchiveMode.Create);
        foreach ((string nombre, string contenido) in archivos)
        {
            ZipArchiveEntry entrada = zip.CreateEntry(nombre);
            using StreamWriter writer = new(entrada.Open());
            writer.Write(contenido);
        }
    }

    private sealed class DirectorioTemporal : IDisposable
    {
        public string Ruta { get; } = Path.Combine(Path.GetTempPath(), "FarmamiaAgentTests", Guid.NewGuid().ToString("N"));

        public DirectorioTemporal()
        {
            Directory.CreateDirectory(Ruta);
        }

        public void Dispose()
        {
            if (Directory.Exists(Ruta))
            {
                Directory.Delete(Ruta, recursive: true);
            }
        }
    }
}
