using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Aplicacion.CasosUso;

public sealed class EnviarLatidoCasoUso
{
    private readonly IInventarioEquipo inventarioEquipo;
    private readonly IClienteOperacionesFarmamia clienteOperaciones;

    public EnviarLatidoCasoUso(
        IInventarioEquipo inventarioEquipo,
        IClienteOperacionesFarmamia clienteOperaciones
    )
    {
        this.inventarioEquipo = inventarioEquipo;
        this.clienteOperaciones = clienteOperaciones;
    }

    public async Task EjecutarAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
    {
        DatosInventarioEquipo inventario = inventarioEquipo.ObtenerInventario();
        var latido = new LatidoAgente(
            credenciales.IdEquipo,
            inventario.VersionPos,
            inventario.DiscoLibreMb,
            inventario.DiscoTotalMb,
            inventario.ProcesoPosEjecutandose,
            LatenciaMs: 0,
            PorcentajePerdidaPaquetes: 0
        );

        await clienteOperaciones.EnviarLatidoAsync(credenciales, latido, cancellationToken);
    }
}
