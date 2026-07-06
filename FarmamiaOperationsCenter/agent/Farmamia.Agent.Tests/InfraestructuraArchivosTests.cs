using System.IO.Compression;
using System.Text.Json;
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

        string contenido = await File.ReadAllTextAsync(Path.Combine(raiz, "State", "credenciales.json"));
        Assert.DoesNotContain("token-tecnico", contenido);
        Assert.Contains("proteccion", contenido);
    }

    [Fact]
    public async Task Configuracion_lee_credenciales_legacy_sin_proteccion()
    {
        OpcionesAgente opciones = Opciones();
        var configuracion = new ConfiguracionLocalAgente(Options.Create(opciones));
        CredencialesAgente credenciales = new(Guid.NewGuid(), "token-legacy");

        await configuracion.PrepararEstructuraAsync(CancellationToken.None);
        await File.WriteAllTextAsync(
            Path.Combine(raiz, "State", "credenciales.json"),
            JsonSerializer.Serialize(credenciales, new JsonSerializerOptions(JsonSerializerDefaults.Web)),
            CancellationToken.None
        );

        CredencialesAgente? recuperadas = await configuracion.LeerCredencialesAsync(CancellationToken.None);

        Assert.Equal(credenciales, recuperadas);
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
    public async Task Actualizador_zip_valido_copia_archivos_y_ejecutable_queda_presente()
    {
        string zip = Path.Combine(raiz, "paquete-valido.zip");
        string rutaPos = Path.Combine(raiz, "POS");
        CrearZip(zip, ("Zabyca.Pos.Desktop.exe", "version-nueva"), ("version.txt", "2026.06.2-success"));

        var actualizador = new ActualizadorPosZip(Options.Create(Opciones()));
        var paquete = new ArchivoPaqueteLocal(zip, new FileInfo(zip).Length, "checksum-no-relevante");

        await actualizador.AplicarAsync(paquete, rutaPos, CancellationToken.None);

        Assert.Equal("version-nueva", File.ReadAllText(Path.Combine(rutaPos, "Zabyca.Pos.Desktop.exe")));
        Assert.Equal("2026.06.2-success", File.ReadAllText(Path.Combine(rutaPos, "version.txt")));
    }

    [Fact]
    public async Task Actualizador_zip_sin_ejecutable_no_supera_validacion_final()
    {
        string zip = Path.Combine(raiz, "paquete-invalido.zip");
        string rutaPos = Path.Combine(raiz, "POS");
        CrearZip(zip, ("version.txt", "2026.06.2-fail"));

        var actualizador = new ActualizadorPosZip(Options.Create(Opciones()));
        var paquete = new ArchivoPaqueteLocal(zip, new FileInfo(zip).Length, "checksum-no-relevante");

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            actualizador.AplicarAsync(paquete, rutaPos, CancellationToken.None)
        );

        ResultadoValidacionPos resultado = await actualizador.ValidarAsync(rutaPos, CancellationToken.None);
        Assert.False(resultado.Exitoso);
        Assert.Equal("EJECUTABLE_NO_ENCONTRADO", resultado.Metodo);
    }

    [Fact]
    public async Task Validar_usa_smoke_test_y_reporta_exito_si_el_codigo_de_salida_es_cero()
    {
        string rutaPos = PrepararPosSimulado(exitCodeSmokeTest: 0);
        var actualizador = new ActualizadorPosZip(Options.Create(OpcionesConTimeoutsCortos()));

        ResultadoValidacionPos resultado = await actualizador.ValidarAsync(rutaPos, CancellationToken.None);

        Assert.True(resultado.Exitoso);
        Assert.Equal("SMOKE_TEST", resultado.Metodo);
    }

    [Fact]
    public async Task Validar_usa_smoke_test_y_reporta_fallo_si_el_codigo_de_salida_no_es_cero()
    {
        string rutaPos = PrepararPosSimulado(exitCodeSmokeTest: 1);
        var actualizador = new ActualizadorPosZip(Options.Create(OpcionesConTimeoutsCortos()));

        ResultadoValidacionPos resultado = await actualizador.ValidarAsync(rutaPos, CancellationToken.None);

        Assert.False(resultado.Exitoso);
        Assert.Equal("SMOKE_TEST", resultado.Metodo);
    }

    [Fact]
    public async Task Validar_cae_a_proceso_vivo_si_la_version_no_soporta_smoke_test_y_reporta_exito()
    {
        string rutaPos = PrepararPosSimulado(ignoraSmokeTest: true, vidaSegundos: 30);
        var actualizador = new ActualizadorPosZip(Options.Create(OpcionesConTimeoutsCortos()));

        ResultadoValidacionPos resultado = await actualizador.ValidarAsync(rutaPos, CancellationToken.None);

        Assert.True(resultado.Exitoso);
        Assert.Equal("PROCESO_VIVO_20S", resultado.Metodo);
    }

    [Fact]
    public async Task Validar_cae_a_proceso_vivo_y_reporta_fallo_si_el_proceso_muere_antes_de_tiempo()
    {
        string rutaPos = PrepararPosSimulado(ignoraSmokeTest: true, vidaSegundos: 0);
        var actualizador = new ActualizadorPosZip(Options.Create(OpcionesConTimeoutsCortos()));

        ResultadoValidacionPos resultado = await actualizador.ValidarAsync(rutaPos, CancellationToken.None);

        Assert.False(resultado.Exitoso);
        Assert.Equal("PROCESO_VIVO_20S", resultado.Metodo);
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

    private OpcionesAgente OpcionesConTimeoutsCortos()
    {
        return new OpcionesAgente
        {
            RutaAgente = raiz,
            RutaPos = Path.Combine(raiz, "POS"),
            SmokeTestTimeoutSegundos = 3,
            ValidacionProcesoVivoSegundos = 3
        };
    }

    private string PrepararPosSimulado(int exitCodeSmokeTest = 0, bool ignoraSmokeTest = false, int vidaSegundos = 60)
    {
        string rutaPos = Path.Combine(raiz, "POS");
        Directory.CreateDirectory(rutaPos);

        // El apphost tiene el nombre de su .dll administrada embebido en el binario desde el build
        // (no lo deriva en tiempo de ejecucion reemplazando la extension), asi que renombrar los
        // archivos despues de compilados no funciona: el proyecto Farmamia.Agent.Tests.PosSimulado
        // ya se compila con AssemblyName=Zabyca.Pos.Desktop para que esto sea una copia simple.
        const string nombreEjecutable = "Zabyca.Pos.Desktop";
        foreach (string extension in new[] { ".exe", ".dll", ".runtimeconfig.json", ".deps.json" })
        {
            string origen = Path.Combine(AppContext.BaseDirectory, nombreEjecutable + extension);
            if (File.Exists(origen))
            {
                File.Copy(origen, Path.Combine(rutaPos, nombreEjecutable + extension), overwrite: true);
            }
        }

        File.WriteAllText(
            Path.Combine(rutaPos, "simulado.config"),
            $"{exitCodeSmokeTest};{ignoraSmokeTest.ToString().ToLowerInvariant()};{vidaSegundos}"
        );
        return rutaPos;
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
