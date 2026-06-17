package com.farmamia.posupdate.infraestructura.almacenamiento;

import java.io.OutputStream;

final class OutputStreamNulo extends OutputStream {

    static final OutputStreamNulo INSTANCE = new OutputStreamNulo();

    private OutputStreamNulo() {
    }

    @Override
    public void write(int b) {
        // Descarta bytes mientras DigestInputStream calcula el hash.
    }
}
