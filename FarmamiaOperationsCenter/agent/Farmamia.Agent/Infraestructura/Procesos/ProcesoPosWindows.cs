using System.Diagnostics;
using System.Runtime.InteropServices;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace Farmamia.Agent.Infraestructura.Procesos;

public sealed class ProcesoPosWindows : IProcesoPos
{
    private const string NombreProcesoPos = "Zabyca.Pos.Desktop";
    private const string EjecutablePos = "Zabyca.Pos.Desktop.exe";
    private const uint SesionConsolaInvalida = 0xFFFFFFFF;
    private const uint MaximumAllowed = 0x02000000;
    private const uint CreateUnicodeEnvironment = 0x00000400;
    private const uint CreateNewConsole = 0x00000010;

    private readonly ILogger<ProcesoPosWindows> logger;

    public ProcesoPosWindows(ILogger<ProcesoPosWindows>? logger = null)
    {
        this.logger = logger ?? NullLogger<ProcesoPosWindows>.Instance;
    }

    public async Task<bool> CerrarSiEjecutandoseAsync(CancellationToken cancellationToken)
    {
        Process[] procesos = Process.GetProcessesByName(NombreProcesoPos);
        if (procesos.Length == 0)
        {
            return false;
        }

        foreach (Process proceso in procesos)
        {
            cancellationToken.ThrowIfCancellationRequested();

            using (proceso)
            {
                if (proceso.CloseMainWindow())
                {
                    Task espera = proceso.WaitForExitAsync(cancellationToken);
                    Task timeout = Task.Delay(TimeSpan.FromSeconds(20), cancellationToken);
                    await Task.WhenAny(espera, timeout);
                }

                if (!proceso.HasExited)
                {
                    proceso.Kill(entireProcessTree: true);
                    await proceso.WaitForExitAsync(cancellationToken);
                }
            }
        }

        return true;
    }

    public Task IniciarAsync(string rutaPos, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        string ejecutable = Path.Combine(rutaPos, EjecutablePos);
        if (!File.Exists(ejecutable))
        {
            throw new FileNotFoundException("Ejecutable POS no encontrado", ejecutable);
        }

        if (!IniciarEnSesionInteractiva(ejecutable, rutaPos))
        {
            // Cuando el agente corre como Windows Service (Sesion 0), Process.Start crea el proceso
            // en la propia Sesion 0 del servicio, invisible para el cajero (confirmado empiricamente
            // en el ensayo de Fase 0: el proceso reabierto asi queda con SessionId=0 aunque exista una
            // sesion interactiva activa). Este fallback solo se usa cuando no hay sesion interactiva
            // detectable o CreateProcessAsUser fallo -- preferible a no reabrir el POS en absoluto,
            // pero el cajero no lo vera hasta que inicie sesion o el soporte revise la maquina.
            logger.LogWarning(
                "No se pudo relanzar {Ejecutable} en la sesion interactiva; se usara Process.Start (puede quedar en Sesion 0, invisible para el usuario)",
                EjecutablePos
            );
            Process.Start(new ProcessStartInfo
            {
                FileName = ejecutable,
                WorkingDirectory = rutaPos,
                UseShellExecute = true
            });
        }

        return Task.CompletedTask;
    }

    private bool IniciarEnSesionInteractiva(string ejecutable, string directorioTrabajo)
    {
        uint idSesion = WTSGetActiveConsoleSessionId();
        if (idSesion == SesionConsolaInvalida)
        {
            logger.LogWarning("No hay una sesion de usuario interactiva activa para relanzar {Ejecutable}", EjecutablePos);
            return false;
        }

        IntPtr tokenUsuario = IntPtr.Zero;
        IntPtr tokenPrimario = IntPtr.Zero;
        IntPtr bloqueEntorno = IntPtr.Zero;

        try
        {
            if (!WTSQueryUserToken(idSesion, out tokenUsuario))
            {
                logger.LogWarning(
                    "WTSQueryUserToken fallo para la sesion {IdSesion} (error Win32 {Error})",
                    idSesion,
                    Marshal.GetLastWin32Error()
                );
                return false;
            }

            if (!DuplicateTokenEx(
                    tokenUsuario,
                    MaximumAllowed,
                    IntPtr.Zero,
                    SecurityImpersonationLevel.SecurityIdentification,
                    TokenType.TokenPrimary,
                    out tokenPrimario))
            {
                logger.LogWarning(
                    "DuplicateTokenEx fallo para la sesion {IdSesion} (error Win32 {Error})",
                    idSesion,
                    Marshal.GetLastWin32Error()
                );
                return false;
            }

            if (!CreateEnvironmentBlock(out bloqueEntorno, tokenPrimario, false))
            {
                bloqueEntorno = IntPtr.Zero;
            }

            var startupInfo = new StartupInfo();
            startupInfo.cb = Marshal.SizeOf<StartupInfo>();
            startupInfo.lpDesktop = "winsta0\\default";

            bool creado = CreateProcessAsUser(
                tokenPrimario,
                ejecutable,
                null,
                IntPtr.Zero,
                IntPtr.Zero,
                false,
                CreateUnicodeEnvironment | CreateNewConsole,
                bloqueEntorno,
                directorioTrabajo,
                ref startupInfo,
                out ProcessInformation procesoInfo
            );

            if (!creado)
            {
                logger.LogWarning(
                    "CreateProcessAsUser fallo para {Ejecutable} en la sesion {IdSesion} (error Win32 {Error})",
                    EjecutablePos,
                    idSesion,
                    Marshal.GetLastWin32Error()
                );
                return false;
            }

            CloseHandle(procesoInfo.hProcess);
            CloseHandle(procesoInfo.hThread);
            return true;
        }
        finally
        {
            if (bloqueEntorno != IntPtr.Zero)
            {
                DestroyEnvironmentBlock(bloqueEntorno);
            }

            if (tokenPrimario != IntPtr.Zero)
            {
                CloseHandle(tokenPrimario);
            }

            if (tokenUsuario != IntPtr.Zero)
            {
                CloseHandle(tokenUsuario);
            }
        }
    }

    private enum SecurityImpersonationLevel
    {
        SecurityAnonymous,
        SecurityIdentification,
        SecurityImpersonation,
        SecurityDelegation
    }

    private enum TokenType
    {
        TokenPrimary = 1,
        TokenImpersonation
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct StartupInfo
    {
        public int cb;
        public string? lpReserved;
        public string? lpDesktop;
        public string? lpTitle;
        public int dwX;
        public int dwY;
        public int dwXSize;
        public int dwYSize;
        public int dwXCountChars;
        public int dwYCountChars;
        public int dwFillAttribute;
        public int dwFlags;
        public short wShowWindow;
        public short cbReserved2;
        public IntPtr lpReserved2;
        public IntPtr hStdInput;
        public IntPtr hStdOutput;
        public IntPtr hStdError;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct ProcessInformation
    {
        public IntPtr hProcess;
        public IntPtr hThread;
        public int dwProcessId;
        public int dwThreadId;
    }

    [DllImport("kernel32.dll")]
    private static extern uint WTSGetActiveConsoleSessionId();

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CloseHandle(IntPtr hObject);

    [DllImport("wtsapi32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool WTSQueryUserToken(uint sessionId, out IntPtr phToken);

    [DllImport("advapi32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool DuplicateTokenEx(
        IntPtr hExistingToken,
        uint dwDesiredAccess,
        IntPtr lpTokenAttributes,
        SecurityImpersonationLevel impersonationLevel,
        TokenType tokenType,
        out IntPtr phNewToken
    );

    [DllImport("userenv.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CreateEnvironmentBlock(out IntPtr lpEnvironment, IntPtr hToken, bool bInherit);

    [DllImport("userenv.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool DestroyEnvironmentBlock(IntPtr lpEnvironment);

    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool CreateProcessAsUser(
        IntPtr hToken,
        string lpApplicationName,
        string? lpCommandLine,
        IntPtr lpProcessAttributes,
        IntPtr lpThreadAttributes,
        bool bInheritHandles,
        uint dwCreationFlags,
        IntPtr lpEnvironment,
        string lpCurrentDirectory,
        ref StartupInfo lpStartupInfo,
        out ProcessInformation lpProcessInformation
    );
}
