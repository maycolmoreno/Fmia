using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Estado;

public sealed class EstadoAvisosActualizacionLocal : IEstadoAvisosActualizacion
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web)
    {
        WriteIndented = true
    };

    private readonly OpcionesAgente opciones;

    public EstadoAvisosActualizacionLocal(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public async Task<bool> FueEnviadoAsync(
        Guid idObjetivoDespliegue,
        TimeOnly horaAviso,
        CancellationToken cancellationToken
    )
    {
        HashSet<string> enviados = await LeerAsync(cancellationToken);
        return enviados.Contains(Clave(idObjetivoDespliegue, horaAviso));
    }

    public async Task RegistrarEnviadoAsync(
        Guid idObjetivoDespliegue,
        TimeOnly horaAviso,
        CancellationToken cancellationToken
    )
    {
        HashSet<string> enviados = await LeerAsync(cancellationToken);
        enviados.Add(Clave(idObjetivoDespliegue, horaAviso));
        string rutaEstado = RutaEstado();
        Directory.CreateDirectory(Path.GetDirectoryName(rutaEstado)!);

        await using FileStream stream = File.Create(rutaEstado);
        await JsonSerializer.SerializeAsync(stream, enviados.Order().ToArray(), JsonOptions, cancellationToken);
    }

    private async Task<HashSet<string>> LeerAsync(CancellationToken cancellationToken)
    {
        string rutaEstado = RutaEstado();
        if (!File.Exists(rutaEstado))
        {
            return [];
        }

        await using FileStream stream = File.OpenRead(rutaEstado);
        string[]? enviados = await JsonSerializer.DeserializeAsync<string[]>(stream, JsonOptions, cancellationToken);
        return enviados is null ? [] : enviados.ToHashSet(StringComparer.Ordinal);
    }

    private static string Clave(Guid idObjetivoDespliegue, TimeOnly horaAviso)
    {
        return $"{idObjetivoDespliegue:N}|{horaAviso:HH:mm:ss}";
    }

    private string RutaEstado()
    {
        return Path.Combine(opciones.RutaAgente, "State", "avisos-enviados.json");
    }
}
