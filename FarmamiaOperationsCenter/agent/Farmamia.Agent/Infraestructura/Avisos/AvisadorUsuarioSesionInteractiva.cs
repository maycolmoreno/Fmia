using System.Runtime.InteropServices;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Avisos;

// El agente corre como Windows Service en la Sesion 0, aislada del escritorio interactivo del
// usuario (ver docs/auditoria: escribir un archivo no lo hace visible para el cajero). WTSSendMessage
// es el mecanismo soportado por Windows para que un servicio muestre un mensaje real en la sesion
// interactiva activa, sin necesitar un proceso auxiliar corriendo en esa sesion.
public sealed class AvisadorUsuarioSesionInteractiva : IAvisadorUsuario
{
    private const uint SesionConsolaInvalida = 0xFFFFFFFF;
    private const int MB_OK = 0x00000000;
    private const int MB_ICONWARNING = 0x00000030;
    private const int MB_SETFOREGROUND = 0x00010000;
    private const int MB_TOPMOST = 0x00040000;
    private const int TimeoutSegundosMensaje = 60;

    private readonly OpcionesAgente opciones;
    private readonly ILogger<AvisadorUsuarioSesionInteractiva> logger;

    public AvisadorUsuarioSesionInteractiva(
        IOptions<OpcionesAgente> opciones,
        ILogger<AvisadorUsuarioSesionInteractiva> logger
    )
    {
        this.opciones = opciones.Value;
        this.logger = logger;
    }

    public async Task EnviarAsync(AvisoUsuario aviso, CancellationToken cancellationToken)
    {
        await RegistrarEnArchivoAsync(aviso, cancellationToken);
        MostrarEnSesionInteractiva(aviso);
    }

    private void MostrarEnSesionInteractiva(AvisoUsuario aviso)
    {
        uint idSesion = WTSGetActiveConsoleSessionId();
        if (idSesion == SesionConsolaInvalida)
        {
            logger.LogWarning(
                "No hay una sesion de usuario interactiva activa; el aviso de actualizacion (objetivo {IdObjetivoDespliegue}) solo quedo registrado en archivo",
                aviso.IdObjetivoDespliegue
            );
            return;
        }

        string titulo = "Actualizacion de POS pendiente";
        List<string> lineas =
        [
            aviso.Mensaje,
            "Version nueva: " + aviso.VersionNueva
        ];
        if (aviso.HoraForzadaActualizacion is not null)
        {
            lineas.Add($"Se cerrara automaticamente a las {aviso.HoraForzadaActualizacion:HH:mm} si continua abierto.");
        }

        string mensaje = string.Join(Environment.NewLine, lineas);

        bool enviado = WTSSendMessage(
            IntPtr.Zero,
            (int)idSesion,
            titulo,
            titulo.Length,
            mensaje,
            mensaje.Length,
            MB_OK | MB_ICONWARNING | MB_SETFOREGROUND | MB_TOPMOST,
            TimeoutSegundosMensaje,
            out _,
            bWait: false
        );

        if (!enviado)
        {
            logger.LogWarning(
                "No se pudo mostrar el aviso de actualizacion en la sesion interactiva {IdSesion} (error Win32 {Error})",
                idSesion,
                Marshal.GetLastWin32Error()
            );
        }
    }

    private async Task RegistrarEnArchivoAsync(AvisoUsuario aviso, CancellationToken cancellationToken)
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

    [DllImport("kernel32.dll")]
    private static extern uint WTSGetActiveConsoleSessionId();

    [DllImport("wtsapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool WTSSendMessage(
        IntPtr hServer,
        int sessionId,
        string pTitle,
        int titleLength,
        string pMessage,
        int messageLength,
        int style,
        int timeoutSeconds,
        out int response,
        bool bWait
    );
}
