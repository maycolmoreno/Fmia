namespace Farmamia.Agent.Dominio.Modelos;

public sealed record ResultadoValidacionPos(bool Exitoso, string Metodo, string? Causa)
{
    public static ResultadoValidacionPos Exito(string metodo) => new(true, metodo, null);

    public static ResultadoValidacionPos Fallo(string metodo, string causa) => new(false, metodo, causa);
}
