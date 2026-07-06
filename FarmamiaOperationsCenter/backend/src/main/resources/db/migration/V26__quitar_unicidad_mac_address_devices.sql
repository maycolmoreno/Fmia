-- POST /api/agent/register fallaba con 500 (uq_devices_mac_address) para cualquier segundo equipo
-- detras de FortiClient VPN: el agente toma el MAC del primer adaptador "up" sin filtrar adaptadores
-- virtuales, y FortiClient asigna el mismo MAC fijo (00:09:0F OUI) en todas las maquinas. La identidad
-- real y unica del equipo ya es hostname (uq_devices_hostname); mac_address es dato informativo/SNMP,
-- no debe ser unico.
ALTER TABLE devices DROP CONSTRAINT uq_devices_mac_address;
