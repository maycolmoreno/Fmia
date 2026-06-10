using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Configuracion;

public sealed class ConfiguracionLocalAgente : IConfiguracionLocalAgente
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = true
    };

    private readonly OpcionesAgente opciones;

    public ConfiguracionLocalAgente(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public async Task PrepararEstructuraAsync(CancellationToken cancellationToken)
    {
        string[] carpetas =
        [
            "Downloads",
            "Backups",
            "Logs",
            "Temp",
            "State"
        ];

        foreach (string carpeta in carpetas)
        {
            cancellationToken.ThrowIfCancellationRequested();
            Directory.CreateDirectory(Path.Combine(opciones.RutaAgente, carpeta));
        }

        string config = Path.Combine(opciones.RutaAgente, "config.json");
        if (!File.Exists(config))
        {
            await File.WriteAllTextAsync(
                config,
                JsonSerializer.Serialize(new Dictionary<string, OpcionesAgente>
                {
                    ["AgenteFarmamia"] = opciones
                }, JsonOptions),
                cancellationToken
            );
        }

        await Task.CompletedTask;
    }

    public async Task<CredencialesAgente?> LeerCredencialesAsync(CancellationToken cancellationToken)
    {
        string ruta = RutaCredenciales();
        if (!File.Exists(ruta))
        {
            return null;
        }

        await using FileStream stream = File.OpenRead(ruta);
        return await JsonSerializer.DeserializeAsync<CredencialesAgente>(stream, JsonOptions, cancellationToken);
    }

    public async Task GuardarCredencialesAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
    {
        string ruta = RutaCredenciales();
        await using FileStream stream = File.Create(ruta);
        await JsonSerializer.SerializeAsync(stream, credenciales, JsonOptions, cancellationToken);
    }

    private string RutaCredenciales()
    {
        return Path.Combine(opciones.RutaAgente, "State", "credenciales.json");
    }
}
