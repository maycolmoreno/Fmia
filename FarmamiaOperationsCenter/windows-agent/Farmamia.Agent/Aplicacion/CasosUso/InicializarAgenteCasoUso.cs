using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Aplicacion.CasosUso;

public sealed class InicializarAgenteCasoUso
{
    private readonly IConfiguracionLocalAgente configuracionLocal;
    private readonly IInventarioEquipo inventarioEquipo;
    private readonly IClienteOperacionesFarmamia clienteOperaciones;
    private readonly OpcionesAgente opciones;

    public InicializarAgenteCasoUso(
        IConfiguracionLocalAgente configuracionLocal,
        IInventarioEquipo inventarioEquipo,
        IClienteOperacionesFarmamia clienteOperaciones,
        IOptions<OpcionesAgente> opciones
    )
    {
        this.configuracionLocal = configuracionLocal;
        this.inventarioEquipo = inventarioEquipo;
        this.clienteOperaciones = clienteOperaciones;
        this.opciones = opciones.Value;
    }

    public async Task<CredencialesAgente> EjecutarAsync(CancellationToken cancellationToken)
    {
        await configuracionLocal.PrepararEstructuraAsync(cancellationToken);

        CredencialesAgente? existentes = await configuracionLocal.LeerCredencialesAsync(cancellationToken);
        if (existentes is not null)
        {
            return existentes;
        }

        DatosInventarioEquipo inventario = inventarioEquipo.ObtenerInventario();
        CredencialesAgente nuevas = await clienteOperaciones.RegistrarAsync(
            opciones.CodigoSucursal,
            inventario,
            cancellationToken
        );

        await configuracionLocal.GuardarCredencialesAsync(nuevas, cancellationToken);
        return nuevas;
    }
}
