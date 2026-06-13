using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Configuracion;

public sealed class ConfiguracionLocalAgente : IConfiguracionLocalAgente
{
    private const string ProteccionDpapiUsuario = "DPAPI_CURRENT_USER";
    private const string ProteccionBase64SinDpapi = "BASE64_UNPROTECTED";
    private static readonly byte[] EntropiaDpapi = Encoding.UTF8.GetBytes("FarmamiaOpsAgentCredentialsV1");

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
        CredencialesPersistidas? persistidas = await JsonSerializer.DeserializeAsync<CredencialesPersistidas>(stream, JsonOptions, cancellationToken);
        if (persistidas is null)
        {
            return null;
        }

        if (string.IsNullOrWhiteSpace(persistidas.Proteccion))
        {
            return new CredencialesAgente(persistidas.IdEquipo, persistidas.TokenAgente);
        }

        return new CredencialesAgente(
            persistidas.IdEquipo,
            DesprotegerToken(persistidas.TokenAgente, persistidas.Proteccion)
        );
    }

    public async Task GuardarCredencialesAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
    {
        string ruta = RutaCredenciales();
        await using FileStream stream = File.Create(ruta);
        (string tokenProtegido, string proteccion) = ProtegerToken(credenciales.TokenAgente);
        var persistidas = new CredencialesPersistidas(credenciales.IdEquipo, tokenProtegido, proteccion);
        await JsonSerializer.SerializeAsync(stream, persistidas, JsonOptions, cancellationToken);
    }

    private string RutaCredenciales()
    {
        return Path.Combine(opciones.RutaAgente, "State", "credenciales.json");
    }

    private static (string Token, string Proteccion) ProtegerToken(string token)
    {
        byte[] bytes = Encoding.UTF8.GetBytes(token);
        if (OperatingSystem.IsWindows())
        {
            byte[] protegidos = Dpapi.Proteger(bytes, EntropiaDpapi);
            return (Convert.ToBase64String(protegidos), ProteccionDpapiUsuario);
        }

        return (Convert.ToBase64String(bytes), ProteccionBase64SinDpapi);
    }

    private static string DesprotegerToken(string token, string proteccion)
    {
        byte[] bytes = Convert.FromBase64String(token);
        if (string.Equals(proteccion, ProteccionDpapiUsuario, StringComparison.Ordinal))
        {
            if (!OperatingSystem.IsWindows())
            {
                throw new PlatformNotSupportedException("Las credenciales fueron protegidas con DPAPI de Windows.");
            }

            byte[] desprotegidos = Dpapi.Desproteger(bytes, EntropiaDpapi);
            return Encoding.UTF8.GetString(desprotegidos);
        }

        if (string.Equals(proteccion, ProteccionBase64SinDpapi, StringComparison.Ordinal))
        {
            return Encoding.UTF8.GetString(bytes);
        }

        throw new InvalidOperationException("Proteccion de credenciales no soportada: " + proteccion);
    }

    private sealed record CredencialesPersistidas(Guid IdEquipo, string TokenAgente, string? Proteccion);

    private static class Dpapi
    {
        private const int CryptprotectUiForbidden = 0x1;

        public static byte[] Proteger(byte[] datos, byte[] entropia)
        {
            return Ejecutar(datos, entropia, proteger: true);
        }

        public static byte[] Desproteger(byte[] datos, byte[] entropia)
        {
            return Ejecutar(datos, entropia, proteger: false);
        }

        private static byte[] Ejecutar(byte[] datos, byte[] entropia, bool proteger)
        {
            using Blob entrada = Blob.From(datos);
            using Blob entropiaBlob = Blob.From(entropia);
            DATA_BLOB salida = default;

            bool ok = proteger
                ? CryptProtectData(
                    ref entrada.Data,
                    null,
                    ref entropiaBlob.Data,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    CryptprotectUiForbidden,
                    ref salida
                )
                : CryptUnprotectData(
                    ref entrada.Data,
                    IntPtr.Zero,
                    ref entropiaBlob.Data,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    CryptprotectUiForbidden,
                    ref salida
                );

            if (!ok)
            {
                throw new InvalidOperationException("No se pudo procesar credenciales con DPAPI.", new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error()));
            }

            try
            {
                byte[] resultado = new byte[salida.cbData];
                Marshal.Copy(salida.pbData, resultado, 0, resultado.Length);
                return resultado;
            }
            finally
            {
                if (salida.pbData != IntPtr.Zero)
                {
                    LocalFree(salida.pbData);
                }
            }
        }

        [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool CryptProtectData(
            ref DATA_BLOB pDataIn,
            string? szDataDescr,
            ref DATA_BLOB pOptionalEntropy,
            IntPtr pvReserved,
            IntPtr pPromptStruct,
            int dwFlags,
            ref DATA_BLOB pDataOut
        );

        [DllImport("crypt32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool CryptUnprotectData(
            ref DATA_BLOB pDataIn,
            IntPtr ppszDataDescr,
            ref DATA_BLOB pOptionalEntropy,
            IntPtr pvReserved,
            IntPtr pPromptStruct,
            int dwFlags,
            ref DATA_BLOB pDataOut
        );

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern IntPtr LocalFree(IntPtr hMem);

        [StructLayout(LayoutKind.Sequential)]
        private struct DATA_BLOB
        {
            public int cbData;
            public IntPtr pbData;
        }

        private sealed class Blob : IDisposable
        {
            public DATA_BLOB Data;

            private Blob(byte[] datos)
            {
                Data.cbData = datos.Length;
                Data.pbData = Marshal.AllocHGlobal(datos.Length);
                Marshal.Copy(datos, 0, Data.pbData, datos.Length);
            }

            public static Blob From(byte[] datos)
            {
                return new Blob(datos);
            }

            public void Dispose()
            {
                if (Data.pbData != IntPtr.Zero)
                {
                    Marshal.FreeHGlobal(Data.pbData);
                    Data.pbData = IntPtr.Zero;
                    Data.cbData = 0;
                }
            }
        }
    }
}
