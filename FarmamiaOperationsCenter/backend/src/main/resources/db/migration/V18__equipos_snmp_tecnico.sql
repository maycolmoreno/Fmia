ALTER TABLE devices
    ADD COLUMN pdv_code VARCHAR(20),
    ADD COLUMN snmp_community VARCHAR(120);

CREATE UNIQUE INDEX ux_devices_pdv_code
    ON devices(pdv_code)
    WHERE pdv_code IS NOT NULL;
