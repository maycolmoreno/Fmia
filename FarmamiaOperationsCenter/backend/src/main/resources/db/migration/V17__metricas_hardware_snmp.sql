ALTER TABLE device_metrics
    ADD COLUMN cpu_usage_percent INTEGER,
    ADD COLUMN ram_usage_percent INTEGER,
    ADD COLUMN response_time_ms INTEGER,
    ADD COLUMN inbound_traffic_kbps NUMERIC(12,2),
    ADD COLUMN outbound_traffic_kbps NUMERIC(12,2),
    ADD COLUMN router_uptime_ticks BIGINT,
    ADD COLUMN router_sys_desc TEXT,
    ADD CONSTRAINT ck_device_metrics_cpu_usage CHECK (
        cpu_usage_percent IS NULL OR (cpu_usage_percent >= 0 AND cpu_usage_percent <= 100)
    ),
    ADD CONSTRAINT ck_device_metrics_ram_usage CHECK (
        ram_usage_percent IS NULL OR (ram_usage_percent >= 0 AND ram_usage_percent <= 100)
    ),
    ADD CONSTRAINT ck_device_metrics_response_time CHECK (
        response_time_ms IS NULL OR response_time_ms >= 0
    ),
    ADD CONSTRAINT ck_device_metrics_inbound_traffic CHECK (
        inbound_traffic_kbps IS NULL OR inbound_traffic_kbps >= 0
    ),
    ADD CONSTRAINT ck_device_metrics_outbound_traffic CHECK (
        outbound_traffic_kbps IS NULL OR outbound_traffic_kbps >= 0
    );
