-- Índice de interfaz SNMP configurable por equipo de red.
-- Permite apuntar al OID correcto según la interfaz WAN del MikroTik
-- (ej: ether3_Telconet = índice 3, bridge1 = índice 1).
-- Valor por defecto 2 cubre la mayoría de routers MikroTik donde
-- ether1 es WAN (índice 2 en ifTable tras bridge1).
ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS snmp_if_index INTEGER NOT NULL DEFAULT 2;

COMMENT ON COLUMN devices.snmp_if_index IS
    'Índice de interfaz SNMP (ifTable) para métricas de tráfico WAN. '
    'Consultar con: snmpwalk -v2c -c <community> <ip> 1.3.6.1.2.1.2.2.1.2';
