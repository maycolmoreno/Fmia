using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Estado;

public sealed class EstadoLocalAgenteArchivo : IEstadoLocalAgente
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = true
    };

    private readonly OpcionesAgente opciones;

    public EstadoLocalAgenteArchivo(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public async Task<EstadoLocalAgente?> LeerAsync(CancellationToken cancellationToken)
    {
        string ruta = RutaEstado();
        if (!File.Exists(ruta))
        {
            return null;
        }

        await using FileStream stream = File.OpenRead(ruta);
        return await JsonSerializer.DeserializeAsync<EstadoLocalAgente>(stream, JsonOptions, cancellationToken);
    }

    public async Task GuardarAsync(EstadoLocalAgente estado, CancellationToken cancellationToken)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(RutaEstado())!);
        await using FileStream stream = File.Create(RutaEstado());
        await JsonSerializer.SerializeAsync(stream, estado, JsonOptions, cancellationToken);
    }

    private string RutaEstado()
    {
        return Path.Combine(opciones.RutaAgente, "State", "estado-agente.json");
    }
}
