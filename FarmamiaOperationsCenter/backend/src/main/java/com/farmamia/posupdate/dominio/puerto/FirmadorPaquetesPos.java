package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.FirmaPaquetePos;

public interface FirmadorPaquetesPos {

    FirmaPaquetePos firmarChecksum(String checksumSha256);
}
