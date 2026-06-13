namespace Farmamia.Agent.Dominio.Puertos;

public interface IBloqueoActualizacion
{
    IDisposable? IntentarAdquirir();
}
