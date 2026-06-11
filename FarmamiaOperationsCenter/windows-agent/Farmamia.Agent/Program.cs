using Farmamia.Agent.Aplicacion.CasosUso;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Actualizacion;
using Farmamia.Agent.Infraestructura.Api;
using Farmamia.Agent.Infraestructura.Almacenamiento;
using Farmamia.Agent.Infraestructura.Avisos;
using Farmamia.Agent.Infraestructura.Configuracion;
using Farmamia.Agent.Infraestructura.Cola;
using Farmamia.Agent.Infraestructura.Estado;
using Farmamia.Agent.Infraestructura.Inventario;
using Farmamia.Agent.Infraestructura.Logging;
using Farmamia.Agent.Infraestructura.Procesos;
using Farmamia.Agent.Infraestructura.Tiempo;
using Farmamia.Agent.Servicio;
using Microsoft.Extensions.Options;

var builder = Host.CreateApplicationBuilder(args);

builder.Services.AddWindowsService(options =>
{
    options.ServiceName = "FarmamiaOpsAgent";
});

builder.Configuration.AddJsonFile("config.json", optional: true, reloadOnChange: true);

builder.Services.Configure<OpcionesAgente>(builder.Configuration.GetSection("AgenteFarmamia"));

builder.Services.AddSingleton<IRelojSistema, RelojSistema>();
builder.Services.AddSingleton<IConfiguracionLocalAgente, ConfiguracionLocalAgente>();
builder.Services.AddSingleton<IInventarioEquipo, InventarioWindows>();
builder.Services.AddSingleton<IAlmacenamientoPaquetes, AlmacenamientoPaquetesLocal>();
builder.Services.AddSingleton<IRespaldoPos, RespaldoPosLocal>();
builder.Services.AddSingleton<IActualizadorPos, ActualizadorPosZip>();
builder.Services.AddSingleton<IProcesoPos, ProcesoPosWindows>();
builder.Services.AddSingleton<IAvisadorUsuario, AvisadorUsuarioArchivo>();
builder.Services.AddSingleton<IEstadoAvisosActualizacion, EstadoAvisosActualizacionLocal>();
builder.Services.AddSingleton<IEstadoLocalAgente, EstadoLocalAgenteArchivo>();
builder.Services.AddSingleton<IColaEventosAgente, ColaEventosAgenteSqlite>();
builder.Services.AddSingleton<ILoggerProvider, ArchivoLoggerProvider>();
builder.Services.AddSingleton<InicializarAgenteCasoUso>();
builder.Services.AddSingleton<EnviarLatidoCasoUso>();
builder.Services.AddSingleton<PrepararActualizacionCasoUso>();
builder.Services.AddSingleton<IClienteOperacionesFarmamia, ClienteOperacionesFarmamiaConCola>();
builder.Services.AddHostedService<ServicioAgente>();
builder.Services.AddHostedService<DespachadorColaEventosServicio>();

builder.Services.AddHttpClient<ClienteOperacionesFarmamia>((servicios, cliente) =>
{
    var opciones = servicios.GetRequiredService<IOptions<OpcionesAgente>>().Value;
    cliente.BaseAddress = new Uri(opciones.UrlApiCentral);
    cliente.Timeout = TimeSpan.FromSeconds(opciones.TimeoutSegundos);
});

var host = builder.Build();
host.Run();
