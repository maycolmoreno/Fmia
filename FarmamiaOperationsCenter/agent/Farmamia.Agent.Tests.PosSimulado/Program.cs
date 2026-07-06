// Ejecutable de prueba que imita el comportamiento de Zabyca.Pos.Desktop.exe frente a
// "--smoke-test", para poder probar ActualizadorPosZip.ValidarAsync contra un proceso real
// en vez de un archivo de texto (que no es un binario Win32 valido y no se puede ejecutar).
//
// El comportamiento se configura via un archivo "simulado.config" en el mismo folder del
// ejecutable (no variables de entorno: UseShellExecute=true no garantiza heredar el entorno
// del proceso que lo lanzo). Formato: tres lineas -> exitCode;ignoraSmokeTest;vidaSegundos
string rutaConfig = Path.Combine(AppContext.BaseDirectory, "simulado.config");
string[] partes = File.Exists(rutaConfig)
    ? File.ReadAllText(rutaConfig).Split(';')
    : ["0", "false", "60"];

int exitCodeSmokeTest = int.Parse(partes[0]);
bool ignoraSmokeTest = bool.Parse(partes[1]);
int vidaSegundos = int.Parse(partes[2]);

bool pideSmokeTest = args.Contains("--smoke-test");

if (pideSmokeTest)
{
    if (!ignoraSmokeTest)
    {
        return exitCodeSmokeTest;
    }

    // Simula una version que no reconoce el flag: se queda "abierta" como una GUI normal
    // en vez de autoevaluarse y salir, forzando a que el llamador detecte el timeout.
    Thread.Sleep(TimeSpan.FromMinutes(2));
    return 0;
}

// Lanzamiento normal (sin --smoke-test): permanece vivo "vidaSegundos" y luego termina.
Thread.Sleep(TimeSpan.FromSeconds(vidaSegundos));
return 0;
