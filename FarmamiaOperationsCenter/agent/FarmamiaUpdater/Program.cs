using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Farmamia.Agent.Aplicacion.CasosUso;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Infraestructura.Actualizacion;
using Farmamia.Agent.Infraestructura.Almacenamiento;
using Farmamia.Agent.Infraestructura.Api;
using Farmamia.Agent.Infraestructura.Avisos;
using Farmamia.Agent.Infraestructura.Configuracion;
using Farmamia.Agent.Infraestructura.Estado;
using Farmamia.Agent.Infraestructura.Inventario;
using Farmamia.Agent.Infraestructura.Procesos;
using Farmamia.Agent.Infraestructura.Tiempo;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

string rutaAgente = args.FirstOrDefault(arg => arg.StartsWith("--agent-root=", StringComparison.OrdinalIgnoreCase))?.Split('=', 2)[1]
    ?? @"C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent";
string comando = args.FirstOrDefault(arg => !arg.StartsWith("--", StringComparison.OrdinalIgnoreCase)) ?? "estado";

string rutaConfig = Path.Combine(rutaAgente, "config.json");
string rutaCredenciales = Path.Combine(rutaAgente, "State", "credenciales.json");
string rutaEstado = Path.Combine(rutaAgente, "State", "estado-agente.json");

OpcionesAgente opciones = LeerOpciones(rutaConfig, rutaAgente);
CredencialesAgente? credenciales = File.Exists(rutaCredenciales)
    ? JsonSerializer.Deserialize<CredencialesAgente>(File.ReadAllText(rutaCredenciales), JsonOptions())
    : null;

switch (comando.ToLowerInvariant())
{
    case "estado":
        Console.WriteLine(File.Exists(rutaEstado) ? File.ReadAllText(rutaEstado) : "Sin estado local registrado.");
        break;
    case "version":
        Console.WriteLine("Ruta POS: " + opciones.RutaPos);
        Console.WriteLine("Version local estimada: " + LeerVersionPos(opciones.RutaPos));
        break;
    case "buscar":
        await BuscarActualizacionAsync(opciones, credenciales);
        break;
    case "instalar-ahora":
        await InstalarAhoraAsync(opciones, credenciales);
        break;
    case "diagnostico":
        await EnviarDiagnosticoAsync(opciones, credenciales, rutaEstado);
        break;
    case "probar-sesion":
        await ProbarSesionInteractivaAsync(opciones);
        break;
    default:
        Console.WriteLine("Comandos: estado | version | buscar | instalar-ahora | diagnostico | probar-sesion");
        break;
}

// Diagnostico manual para el riesgo de Sesion 0: ejecuta este comando dentro de una tarea
// programada como SYSTEM (ver herramientas/laboratorio-pos/probar-sesion0-real.ps1) para
// reproducir el mismo aislamiento de sesion del servicio real, sin depender del backend.
static async Task ProbarSesionInteractivaAsync(OpcionesAgente opciones)
{
    using ILoggerFactory loggerFactory = LoggerFactory.Create(_ => { });
    IOptions<OpcionesAgente> opcionesMonitor = Options.Create(opciones);
    var avisador = new AvisadorUsuarioSesionInteractiva(opcionesMonitor, loggerFactory.CreateLogger<AvisadorUsuarioSesionInteractiva>());
    var procesoPos = new ProcesoPosWindows();

    Console.WriteLine("[1/3] Enviando aviso de prueba via WTSSendMessage...");
    await avisador.EnviarAsync(
        new AvisoUsuario(
            Guid.NewGuid(),
            "9.9.9-prueba-sesion",
            TimeOnly.FromDateTime(DateTime.Now),
            TimeOnly.FromDateTime(DateTime.Now.AddMinutes(5)),
            "PRUEBA DE DIAGNOSTICO: si ve este mensaje, WTSSendMessage funciona desde este contexto. Puede ignorarlo."
        ),
        CancellationToken.None
    );
    Console.WriteLine("[1/3] Aviso enviado.");

    Console.WriteLine("[2/3] Intentando cerrar Zabyca.Pos.Desktop.exe si esta en ejecucion...");
    bool huboProcesoQueCerrar = await procesoPos.CerrarSiEjecutandoseAsync(CancellationToken.None);
    Console.WriteLine(huboProcesoQueCerrar
        ? "[2/3] Se encontro y se intento cerrar el proceso POS."
        : "[2/3] No se encontro ningun proceso POS en ejecucion.");

    Console.WriteLine("[3/3] Reabriendo POS en " + opciones.RutaPos + "...");
    await procesoPos.IniciarAsync(opciones.RutaPos, CancellationToken.None);
    Console.WriteLine("[3/3] Comando de apertura enviado.");
    Console.WriteLine("Diagnostico de sesion completado.");
}

static async Task BuscarActualizacionAsync(OpcionesAgente opciones, CredencialesAgente? credenciales)
{
    if (credenciales is null)
    {
        Console.WriteLine("Sin credenciales locales. El servicio debe registrarse primero.");
        return;
    }

    using HttpClient httpClient = Cliente(opciones, credenciales);
    var cliente = new ClienteOperacionesFarmamia(httpClient);
    InstruccionActualizacion? instruccion = await cliente.ConsultarInstruccionAsync(credenciales, CancellationToken.None);
    if (instruccion is null || !instruccion.TieneInstruccion)
    {
        Console.WriteLine("No hay actualizacion autorizada.");
        return;
    }

    Console.WriteLine($"Actualizacion autorizada: {instruccion.Version} paquete={instruccion.IdPaquete}");
}

static async Task InstalarAhoraAsync(OpcionesAgente opciones, CredencialesAgente? credenciales)
{
    if (credenciales is null)
    {
        Console.WriteLine("Sin credenciales locales. El servicio debe registrarse primero.");
        return;
    }

    IOptions<OpcionesAgente> opcionesMonitor = Options.Create(opciones);
    await new ConfiguracionLocalAgente(opcionesMonitor).PrepararEstructuraAsync(CancellationToken.None);

    using HttpClient httpClient = Cliente(opciones, credenciales);
    using ILoggerFactory loggerFactory = LoggerFactory.Create(_ => { });
    var cliente = new ClienteOperacionesFarmamia(httpClient);
    var casoUso = new PrepararActualizacionCasoUso(
        cliente,
        new AlmacenamientoPaquetesLocal(opcionesMonitor),
        new InventarioWindows(opcionesMonitor),
        new RespaldoPosLocal(opcionesMonitor),
        new ActualizadorPosZip(opcionesMonitor),
        new ProcesoPosWindows(),
        new RelojSistema(),
        new AvisadorUsuarioSesionInteractiva(opcionesMonitor, loggerFactory.CreateLogger<AvisadorUsuarioSesionInteractiva>()),
        new EstadoAvisosActualizacionLocal(opcionesMonitor),
        new EstadoLocalAgenteArchivo(opcionesMonitor),
        new BloqueoActualizacionMutex(),
        loggerFactory.CreateLogger<PrepararActualizacionCasoUso>(),
        opcionesMonitor
    );

    await casoUso.EjecutarAsync(credenciales, CancellationToken.None);
    Console.WriteLine("Proceso manual finalizado. Revise State\\estado-agente.json y Logs para el detalle.");
}

static async Task EnviarDiagnosticoAsync(OpcionesAgente opciones, CredencialesAgente? credenciales, string rutaEstado)
{
    if (credenciales is null)
    {
        Console.WriteLine("Sin credenciales locales. No se puede enviar diagnostico.");
        return;
    }

    string estado = File.Exists(rutaEstado) ? File.ReadAllText(rutaEstado) : "Sin estado local.";
    using HttpClient cliente = Cliente(opciones, credenciales);
    using HttpResponseMessage respuesta = await cliente.PostAsJsonAsync($"/api/agent/{credenciales.IdEquipo}/events", new
    {
        eventType = "AGENT_DIAGNOSTIC",
        eventMessage = estado,
        oldVersion = LeerVersionPos(opciones.RutaPos),
        newVersion = LeerVersionPos(opciones.RutaPos),
        metadata = new Dictionary<string, object?>
        {
            ["agentRoot"] = opciones.RutaAgente,
            ["posPath"] = opciones.RutaPos
        }
    });
    respuesta.EnsureSuccessStatusCode();
    Console.WriteLine("Diagnostico enviado.");
}

static HttpClient Cliente(OpcionesAgente opciones, CredencialesAgente credenciales)
{
    var cliente = new HttpClient
    {
        BaseAddress = new Uri(opciones.UrlApiCentral),
        Timeout = TimeSpan.FromSeconds(opciones.TimeoutSegundos)
    };
    cliente.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);
    return cliente;
}

static string LeerVersionPos(string rutaPos)
{
    string archivoVersion = Path.Combine(rutaPos, "version.txt");
    return File.Exists(archivoVersion) ? File.ReadAllText(archivoVersion).Trim() : "N/D";
}

static JsonSerializerOptions JsonOptions() => new(JsonSerializerDefaults.Web);

static OpcionesAgente LeerOpciones(string rutaConfig, string rutaAgente)
{
    if (!File.Exists(rutaConfig))
    {
        return new OpcionesAgente { RutaAgente = rutaAgente };
    }

    using JsonDocument documento = JsonDocument.Parse(File.ReadAllText(rutaConfig));
    JsonElement raiz = documento.RootElement;
    if (raiz.TryGetProperty("AgenteFarmamia", out JsonElement seccion))
    {
        return seccion.Deserialize<OpcionesAgente>(JsonOptions()) ?? new OpcionesAgente { RutaAgente = rutaAgente };
    }

    return raiz.Deserialize<OpcionesAgente>(JsonOptions()) ?? new OpcionesAgente { RutaAgente = rutaAgente };
}
