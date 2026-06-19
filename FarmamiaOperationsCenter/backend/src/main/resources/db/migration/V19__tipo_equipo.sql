ALTER TABLE devices
    ADD COLUMN equipment_type VARCHAR(40) NOT NULL DEFAULT 'POS_TERMINAL',
    ADD CONSTRAINT ck_devices_equipment_type CHECK (equipment_type IN ('POS_TERMINAL', 'NETWORK_LINK'));

UPDATE devices
SET equipment_type = 'NETWORK_LINK'
WHERE pdv_code IS NOT NULL AND ip_address IS NOT NULL;
