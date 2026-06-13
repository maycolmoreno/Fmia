package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.FirmaPaquetePos;

public interface FirmadorPaquetesPos {

    FirmaPaquetePos firmarChecksum(String checksumSha256);
}
