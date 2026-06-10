using Microsoft.Extensions.Options;
using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Infraestructura.Logging;

public sealed class ArchivoLoggerProvider : ILoggerProvider
{
    private readonly OpcionesAgente opciones;

    public ArchivoLoggerProvider(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public ILogger CreateLogger(string categoryName)
    {
        return new ArchivoLogger(opciones, categoryName);
    }

    public void Dispose()
    {
    }

    private sealed class ArchivoLogger : ILogger
    {
        private readonly OpcionesAgente opciones;
        private readonly string categoria;

        public ArchivoLogger(OpcionesAgente opciones, string categoria)
        {
            this.opciones = opciones;
            this.categoria = categoria;
        }

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => logLevel >= LogLevel.Information;

        public void Log<TState>(
            LogLevel logLevel,
            EventId eventId,
            TState state,
            Exception? exception,
            Func<TState, Exception?, string> formatter
        )
        {
            if (!IsEnabled(logLevel))
            {
                return;
            }

            string carpeta = Path.Combine(opciones.RutaAgente, "Logs");
            Directory.CreateDirectory(carpeta);
            string archivo = Path.Combine(carpeta, $"agent-{DateTimeOffset.Now:yyyyMMdd}.log");
            string linea = string.Join(
                " | ",
                DateTimeOffset.Now.ToString("O"),
                logLevel,
                categoria,
                formatter(state, exception),
                exception?.ToString()
            );
            File.AppendAllText(archivo, linea + Environment.NewLine);
        }
    }
}
