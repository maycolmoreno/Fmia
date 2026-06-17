namespace Farmamia.Agent.Infraestructura.Sse;

/// <summary>
/// Reads an SSE stream and yields named events as they arrive.
/// Only yields events whose name matches the requested eventName.
/// </summary>
internal static class LectorSseLineas
{
    public static async IAsyncEnumerable<string> LeerEventosAsync(
        Stream stream,
        string nombreEvento,
        [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken
    )
    {
        using var reader = new StreamReader(stream);
        string? eventoActual = null;

        while (!cancellationToken.IsCancellationRequested)
        {
            string? linea = await reader.ReadLineAsync(cancellationToken);

            if (linea is null)
                yield break;

            if (linea.StartsWith("event:", StringComparison.Ordinal))
            {
                eventoActual = linea[6..].Trim();
                continue;
            }

            if (linea.StartsWith("data:", StringComparison.Ordinal))
            {
                if (string.Equals(eventoActual, nombreEvento, StringComparison.Ordinal))
                {
                    yield return linea[5..].Trim();
                }
                eventoActual = null;
                continue;
            }

            if (linea.Length == 0)
            {
                eventoActual = null;
            }
        }
    }
}
