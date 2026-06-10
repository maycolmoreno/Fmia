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
    default:
        Console.WriteLine("Comandos: estado | version | buscar | instalar-ahora | diagnostico");
        break;
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
        new ActualizadorPosZip(),
        new ProcesoPosWindows(),
        new RelojSistema(),
        new AvisadorUsuarioArchivo(opcionesMonitor),
        new EstadoAvisosActualizacionLocal(opcionesMonitor),
        new EstadoLocalAgenteArchivo(opcionesMonitor),
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
