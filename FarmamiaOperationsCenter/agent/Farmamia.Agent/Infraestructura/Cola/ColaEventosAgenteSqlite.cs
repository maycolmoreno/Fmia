using System.Security.Cryptography;
using System.Text;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Data.Sqlite;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Cola;

public sealed class ColaEventosAgenteSqlite : IColaEventosAgente
{
    private readonly string rutaBase;
    private readonly string rutaDb;

    public ColaEventosAgenteSqlite(IOptions<OpcionesAgente> opciones)
    {
        rutaBase = Path.Combine(opciones.Value.RutaAgente, "State");
        rutaDb = Path.Combine(rutaBase, "agent-queue.db");
    }

    public async Task InicializarAsync(CancellationToken cancellationToken)
    {
        Directory.CreateDirectory(rutaBase);
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await EjecutarAsync(conexion, """
            CREATE TABLE IF NOT EXISTS outbox_events (
                id TEXT PRIMARY KEY,
                event_type TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                idempotency_key TEXT NOT NULL,
                status TEXT NOT NULL,
                attempt_count INTEGER NOT NULL,
                next_attempt_at TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                last_error TEXT,
                checksum TEXT NOT NULL
            );
            """, cancellationToken);
        await EjecutarAsync(conexion, """
            CREATE TABLE IF NOT EXISTS agent_state (
                key TEXT PRIMARY KEY,
                value_json TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
            """, cancellationToken);
        await EjecutarAsync(conexion, """
            CREATE INDEX IF NOT EXISTS ix_outbox_status_next_attempt
            ON outbox_events(status, next_attempt_at);
            """, cancellationToken);
    }

    public async Task RecuperarEnviosInterrumpidosAsync(CancellationToken cancellationToken)
    {
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = """
            UPDATE outbox_events
            SET status = 'PENDING', updated_at = $updatedAt
            WHERE status = 'SENDING';
            """;
        comando.Parameters.AddWithValue("$updatedAt", SerializarFecha(DateTimeOffset.UtcNow));
        await comando.ExecuteNonQueryAsync(cancellationToken);
    }

    public async Task<EventoPendienteAgente> EncolarAsync(
        string tipoEvento,
        string payloadJson,
        string? idempotencyKey,
        CancellationToken cancellationToken
    )
    {
        DateTimeOffset ahora = DateTimeOffset.UtcNow;
        Guid id = Guid.NewGuid();
        string key = string.IsNullOrWhiteSpace(idempotencyKey) ? id.ToString("N") : idempotencyKey.Trim();
        string checksum = CalcularChecksum(payloadJson);

        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = """
            INSERT INTO outbox_events (
                id, event_type, payload_json, idempotency_key, status, attempt_count,
                next_attempt_at, created_at, updated_at, last_error, checksum
            )
            VALUES (
                $id, $eventType, $payloadJson, $idempotencyKey, 'PENDING', 0,
                $nextAttemptAt, $createdAt, $updatedAt, NULL, $checksum
            );
            """;
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.Parameters.AddWithValue("$eventType", tipoEvento);
        comando.Parameters.AddWithValue("$payloadJson", payloadJson);
        comando.Parameters.AddWithValue("$idempotencyKey", key);
        comando.Parameters.AddWithValue("$nextAttemptAt", SerializarFecha(ahora));
        comando.Parameters.AddWithValue("$createdAt", SerializarFecha(ahora));
        comando.Parameters.AddWithValue("$updatedAt", SerializarFecha(ahora));
        comando.Parameters.AddWithValue("$checksum", checksum);
        await comando.ExecuteNonQueryAsync(cancellationToken);

        return new EventoPendienteAgente(id, tipoEvento, payloadJson, key, "PENDING", 0, ahora, ahora, ahora, null, checksum);
    }

    public async Task<IReadOnlyList<EventoPendienteAgente>> ObtenerPendientesAsync(
        int limite,
        DateTimeOffset ahora,
        CancellationToken cancellationToken
    )
    {
        var eventos = new List<EventoPendienteAgente>();
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = """
            SELECT id, event_type, payload_json, idempotency_key, status, attempt_count,
                   next_attempt_at, created_at, updated_at, last_error, checksum
            FROM outbox_events
            WHERE status IN ('PENDING', 'FAILED') AND next_attempt_at <= $now
            ORDER BY created_at
            LIMIT $limit;
            """;
        comando.Parameters.AddWithValue("$now", SerializarFecha(ahora));
        comando.Parameters.AddWithValue("$limit", Math.Max(1, limite));

        await using SqliteDataReader reader = await comando.ExecuteReaderAsync(cancellationToken);
        while (await reader.ReadAsync(cancellationToken))
        {
            var evento = LeerEvento(reader);
            if (!string.Equals(evento.Checksum, CalcularChecksum(evento.PayloadJson), StringComparison.OrdinalIgnoreCase))
            {
                await MarcarDeadLetterAsync(evento.Id, "Checksum de payload local invalido", cancellationToken);
                continue;
            }

            eventos.Add(evento);
        }

        return eventos;
    }

    public async Task<DiagnosticoColaEventosAgente> ObtenerDiagnosticoAsync(CancellationToken cancellationToken)
    {
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        var conteos = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase)
        {
            ["PENDING"] = 0,
            ["SENDING"] = 0,
            ["SENT"] = 0,
            ["FAILED"] = 0,
            ["DEAD_LETTER"] = 0
        };

        await using (SqliteCommand comando = conexion.CreateCommand())
        {
            comando.CommandText = """
                SELECT status, COUNT(*)
                FROM outbox_events
                GROUP BY status;
                """;
            await using SqliteDataReader reader = await comando.ExecuteReaderAsync(cancellationToken);
            while (await reader.ReadAsync(cancellationToken))
            {
                conteos[reader.GetString(0)] = reader.GetInt32(1);
            }
        }

        DateTimeOffset? eventoMasAntiguoPendiente = null;
        await using (SqliteCommand comando = conexion.CreateCommand())
        {
            comando.CommandText = """
                SELECT MIN(created_at)
                FROM outbox_events
                WHERE status IN ('PENDING', 'FAILED');
                """;
            object? valor = await comando.ExecuteScalarAsync(cancellationToken);
            if (valor is string fecha && !string.IsNullOrWhiteSpace(fecha))
            {
                eventoMasAntiguoPendiente = DateTimeOffset.Parse(fecha);
            }
        }

        DateTimeOffset? ultimoDeadLetterEn = null;
        string? ultimoErrorDeadLetter = null;
        await using (SqliteCommand comando = conexion.CreateCommand())
        {
            comando.CommandText = """
                SELECT updated_at, last_error
                FROM outbox_events
                WHERE status = 'DEAD_LETTER'
                ORDER BY updated_at DESC
                LIMIT 1;
                """;
            await using SqliteDataReader reader = await comando.ExecuteReaderAsync(cancellationToken);
            if (await reader.ReadAsync(cancellationToken))
            {
                ultimoDeadLetterEn = DateTimeOffset.Parse(reader.GetString(0));
                ultimoErrorDeadLetter = reader.IsDBNull(1) ? null : reader.GetString(1);
            }
        }

        return new DiagnosticoColaEventosAgente(
            conteos["PENDING"],
            conteos["SENDING"],
            conteos["SENT"],
            conteos["FAILED"],
            conteos["DEAD_LETTER"],
            eventoMasAntiguoPendiente,
            ultimoDeadLetterEn,
            ultimoErrorDeadLetter
        );
    }

    public Task MarcarEnviandoAsync(Guid id, CancellationToken cancellationToken)
    {
        return ActualizarEstadoAsync(id, "SENDING", null, null, cancellationToken);
    }

    public Task MarcarEnviadoAsync(Guid id, CancellationToken cancellationToken)
    {
        return ActualizarEstadoAsync(id, "SENT", null, null, cancellationToken);
    }

    public async Task MarcarFallidoAsync(
        Guid id,
        string error,
        int maxIntentos,
        DateTimeOffset proximoIntentoEn,
        CancellationToken cancellationToken
    )
    {
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = """
            UPDATE outbox_events
            SET status = CASE WHEN attempt_count + 1 >= $maxAttempts THEN 'DEAD_LETTER' ELSE 'FAILED' END,
                attempt_count = attempt_count + 1,
                next_attempt_at = $nextAttemptAt,
                updated_at = $updatedAt,
                last_error = $error
            WHERE id = $id;
            """;
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.Parameters.AddWithValue("$maxAttempts", maxIntentos);
        comando.Parameters.AddWithValue("$nextAttemptAt", SerializarFecha(proximoIntentoEn));
        comando.Parameters.AddWithValue("$updatedAt", SerializarFecha(DateTimeOffset.UtcNow));
        comando.Parameters.AddWithValue("$error", error);
        await comando.ExecuteNonQueryAsync(cancellationToken);
    }

    private async Task MarcarDeadLetterAsync(Guid id, string error, CancellationToken cancellationToken)
    {
        await ActualizarEstadoAsync(id, "DEAD_LETTER", error, null, cancellationToken);
    }

    private async Task ActualizarEstadoAsync(
        Guid id,
        string estado,
        string? error,
        DateTimeOffset? proximoIntentoEn,
        CancellationToken cancellationToken
    )
    {
        await using SqliteConnection conexion = await AbrirAsync(cancellationToken);
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = """
            UPDATE outbox_events
            SET status = $status,
                next_attempt_at = COALESCE($nextAttemptAt, next_attempt_at),
                updated_at = $updatedAt,
                last_error = $error
            WHERE id = $id;
            """;
        comando.Parameters.AddWithValue("$id", id.ToString());
        comando.Parameters.AddWithValue("$status", estado);
        comando.Parameters.AddWithValue("$nextAttemptAt", proximoIntentoEn is null ? DBNull.Value : SerializarFecha(proximoIntentoEn.Value));
        comando.Parameters.AddWithValue("$updatedAt", SerializarFecha(DateTimeOffset.UtcNow));
        comando.Parameters.AddWithValue("$error", error is null ? DBNull.Value : error);
        await comando.ExecuteNonQueryAsync(cancellationToken);
    }

    private async Task<SqliteConnection> AbrirAsync(CancellationToken cancellationToken)
    {
        var conexion = new SqliteConnection($"Data Source={rutaDb};Pooling=False");
        await conexion.OpenAsync(cancellationToken);
        return conexion;
    }

    private static async Task EjecutarAsync(SqliteConnection conexion, string sql, CancellationToken cancellationToken)
    {
        await using SqliteCommand comando = conexion.CreateCommand();
        comando.CommandText = sql;
        await comando.ExecuteNonQueryAsync(cancellationToken);
    }

    private static EventoPendienteAgente LeerEvento(SqliteDataReader reader)
    {
        return new EventoPendienteAgente(
            Guid.Parse(reader.GetString(0)),
            reader.GetString(1),
            reader.GetString(2),
            reader.GetString(3),
            reader.GetString(4),
            reader.GetInt32(5),
            DateTimeOffset.Parse(reader.GetString(6)),
            DateTimeOffset.Parse(reader.GetString(7)),
            DateTimeOffset.Parse(reader.GetString(8)),
            reader.IsDBNull(9) ? null : reader.GetString(9),
            reader.GetString(10)
        );
    }

    private static string CalcularChecksum(string payloadJson)
    {
        byte[] hash = SHA256.HashData(Encoding.UTF8.GetBytes(payloadJson));
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private static string SerializarFecha(DateTimeOffset fecha)
    {
        return fecha.UtcDateTime.ToString("O");
    }
}
