using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Infraestructura.Actualizacion;

public sealed class BloqueoActualizacionMutex : IBloqueoActualizacion
{
    private const string NombreMutex = @"Global\FarmamiaOpsAgent.UpdateLock";

    public IDisposable? IntentarAdquirir()
    {
        var mutex = new Mutex(initiallyOwned: false, NombreMutex);
        try
        {
            if (!mutex.WaitOne(TimeSpan.Zero))
            {
                mutex.Dispose();
                return null;
            }

            return new Liberador(mutex);
        }
        catch (AbandonedMutexException)
        {
            return new Liberador(mutex);
        }
        catch
        {
            mutex.Dispose();
            throw;
        }
    }

    private sealed class Liberador : IDisposable
    {
        private readonly Mutex mutex;
        private bool liberado;

        public Liberador(Mutex mutex)
        {
            this.mutex = mutex;
        }

        public void Dispose()
        {
            if (liberado)
            {
                return;
            }

            liberado = true;
            mutex.ReleaseMutex();
            mutex.Dispose();
        }
    }
}
