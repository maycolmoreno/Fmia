using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Avisos;

public sealed class AvisadorUsuarioArchivo : IAvisadorUsuario
{
    private readonly OpcionesAgente opciones;

    public AvisadorUsuarioArchivo(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public async Task EnviarAsync(AvisoUsuario aviso, CancellationToken cancellationToken)
    {
        string carpetaAvisos = Path.Combine(opciones.RutaAgente, "State", "Avisos");
        Directory.CreateDirectory(carpetaAvisos);

        string archivo = Path.Combine(
            carpetaAvisos,
            $"{aviso.IdObjetivoDespliegue:N}-{aviso.HoraAviso:HHmmss}.txt"
        );

        string contenido = string.Join(
            Environment.NewLine,
            "Farmamia Operations Agent",
            aviso.Mensaje,
            "Version nueva: " + aviso.VersionNueva,
            "Hora aviso: " + aviso.HoraAviso,
            "Hora forzada: " + (aviso.HoraForzadaActualizacion?.ToString("HH:mm:ss") ?? "N/D"),
            "Generado: " + DateTimeOffset.Now
        );

        await File.WriteAllTextAsync(archivo, contenido, cancellationToken);
    }
}
