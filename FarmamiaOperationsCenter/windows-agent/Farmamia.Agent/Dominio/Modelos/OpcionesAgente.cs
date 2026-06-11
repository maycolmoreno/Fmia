namespace Farmamia.Agent.Dominio.Modelos;

public sealed class OpcionesAgente
{
    public string UrlApiCentral { get; init; } = "http://localhost:8080";

    public string CodigoSucursal { get; init; } = "FM001";

    public string VersionAgente { get; init; } = "1.0.0";

    public string RutaPos { get; init; } = @"C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Cliente";

    public string RutaAgente { get; init; } = @"C:\Program Files (x86)\Farmamia Cia Ltda - Elipsys\Agent";

    public int IntervaloHeartbeatSegundos { get; init; } = 60;

    public int TimeoutSegundos { get; init; } = 30;

    public int MaxIntentosDescarga { get; init; } = 3;

    public int BackoffInicialSegundos { get; init; } = 5;

    public int BackoffMaximoSegundos { get; init; } = 300;

    public int MaxIntentosColaEventos { get; init; } = 8;
}
