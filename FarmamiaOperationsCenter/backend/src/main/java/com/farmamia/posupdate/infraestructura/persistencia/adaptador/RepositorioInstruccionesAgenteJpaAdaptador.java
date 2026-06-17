package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.dominio.modelo.InstruccionAgente;
import com.farmamia.posupdate.dominio.puerto.RepositorioCampanaGruposTrx;
import com.farmamia.posupdate.dominio.puerto.RepositorioInstruccionesAgente;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.OleadaDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RepositorioInstruccionesAgenteJpaAdaptador implements RepositorioInstruccionesAgente {

    private static final String ESTADO_AUTORIZADO = "AUTHORIZED";
    private static final String TIPO_INSTRUCCION_ACTUALIZAR_POS = "UPDATE_POS";

    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;
    private final RepositorioCampanaGruposTrx repositorioCampanaGruposTrx;
    private final int minutosLeaseInstruccion;
    private final Counter instruccionesEmitidas;
    private final Counter bloqueosEstadoObjetivo;
    private final Counter bloqueosEstadoDespliegue;
    private final Counter bloqueosPaquete;
    private final Counter bloqueosLeaseActivo;
    private final Counter bloqueosEstadoOleada;
    private final Counter bloqueosVentana;
    private final Counter bloqueosParalelismo;
    private final Counter bloqueosGrupoTrxCampana;

    public RepositorioInstruccionesAgenteJpaAdaptador(
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        RepositorioCampanaGruposTrx repositorioCampanaGruposTrx,
        @Value("${farmamia.orchestration.instruction-lease-minutes:15}") int minutosLeaseInstruccion,
        MeterRegistry meterRegistry
    ) {
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.repositorioCampanaGruposTrx = repositorioCampanaGruposTrx;
        this.minutosLeaseInstruccion = Math.max(1, minutosLeaseInstruccion);
        this.instruccionesEmitidas = Counter.builder("farmamia.orchestration.instructions.issued.total")
            .description("Instrucciones de actualizacion emitidas a agentes")
            .register(meterRegistry);
        this.bloqueosEstadoObjetivo = contadorBloqueo(meterRegistry, "target_status");
        this.bloqueosEstadoDespliegue = contadorBloqueo(meterRegistry, "deployment_status");
        this.bloqueosPaquete = contadorBloqueo(meterRegistry, "package_status");
        this.bloqueosLeaseActivo = contadorBloqueo(meterRegistry, "active_lease");
        this.bloqueosEstadoOleada = contadorBloqueo(meterRegistry, "wave_status");
        this.bloqueosVentana = contadorBloqueo(meterRegistry, "maintenance_window");
        this.bloqueosParalelismo = contadorBloqueo(meterRegistry, "parallelism_limit");
        this.bloqueosGrupoTrxCampana = contadorBloqueo(meterRegistry, "campaign_trx_status");
    }

    @Override
    @Transactional
    public Optional<InstruccionAgente> buscarSiguienteParaEquipo(UUID idEquipo) {
        return objetivoDespliegueRepositorioJpa
            .findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(idEquipo, ESTADO_AUTORIZADO)
            .filter(this::esEntregable)
            .map(this::registrarLeaseYConvertir);
    }

    private boolean esEntregable(ObjetivoDespliegueEntidad objetivo) {
        DespliegueEntidad despliegue = objetivo.getDespliegue();
        PaquetePosEntidad paquete = despliegue.getPaquete();

        OffsetDateTime ahora = OffsetDateTime.now();
        if (!objetivo.estaAutorizado()) {
            bloqueosEstadoObjetivo.increment();
            return false;
        }
        if (!despliegue.puedeEntregarInstrucciones()) {
            bloqueosEstadoDespliegue.increment();
            return false;
        }
        if (!paquete.estaAprobado()) {
            bloqueosPaquete.increment();
            return false;
        }
        if (objetivo.tieneLeaseActivo(ahora)) {
            bloqueosLeaseActivo.increment();
            return false;
        }
        if (grupoTrxCampanaBloquea(objetivo)) {
            bloqueosGrupoTrxCampana.increment();
            return false;
        }
        return oleadaPermiteEntrega(objetivo.getOleada(), ahora);
    }

    private boolean grupoTrxCampanaBloquea(ObjetivoDespliegueEntidad objetivo) {
        UUID idGrupoTrx = objetivo.getGrupoTrx() == null ? null : objetivo.getGrupoTrx().getId();
        return repositorioCampanaGruposTrx.instruccionBloqueada(
            objetivo.getDespliegue().getId(),
            idGrupoTrx,
            objetivo.getGrupoObjetivo()
        );
    }

    private boolean oleadaPermiteEntrega(OleadaDespliegueEntidad oleada, OffsetDateTime ahora) {
        if (oleada == null) {
            return true;
        }
        if (!"RUNNING".equals(oleada.getEstado())) {
            bloqueosEstadoOleada.increment();
            return false;
        }
        if (!dentroDeVentana(oleada, ahora)) {
            bloqueosVentana.increment();
            return false;
        }
        if (leasesActivos(oleada, ahora) >= oleada.getMaximoEquiposParalelos()) {
            bloqueosParalelismo.increment();
            return false;
        }
        return true;
    }

    private long leasesActivos(OleadaDespliegueEntidad oleada, OffsetDateTime ahora) {
        return objetivoDespliegueRepositorioJpa.countByOleada_IdAndLeaseInstruccionHastaAfter(oleada.getId(), ahora);
    }

    private boolean dentroDeVentana(OleadaDespliegueEntidad oleada, OffsetDateTime ahoraFecha) {
        if (oleada.getVentanaInicio() == null || oleada.getVentanaFin() == null) {
            return true;
        }
        LocalTime ahora = ahoraFecha.toLocalTime();
        LocalTime inicio = oleada.getVentanaInicio();
        LocalTime fin = oleada.getVentanaFin();
        if (inicio.equals(fin)) {
            return true;
        }
        if (inicio.isBefore(fin)) {
            return !ahora.isBefore(inicio) && !ahora.isAfter(fin);
        }
        return !ahora.isBefore(inicio) || !ahora.isAfter(fin);
    }

    private InstruccionAgente registrarLeaseYConvertir(ObjetivoDespliegueEntidad objetivo) {
        objetivo.registrarLeaseInstruccion(OffsetDateTime.now(), minutosLeaseInstruccion);
        instruccionesEmitidas.increment();
        return aDominio(objetivoDespliegueRepositorioJpa.save(objetivo));
    }

    private Counter contadorBloqueo(MeterRegistry meterRegistry, String motivo) {
        return Counter.builder("farmamia.orchestration.instructions.blocked.total")
            .tag("reason", motivo)
            .description("Intentos de entrega de instruccion bloqueados por regla operacional")
            .register(meterRegistry);
    }

    private InstruccionAgente aDominio(ObjetivoDespliegueEntidad objetivo) {
        DespliegueEntidad despliegue = objetivo.getDespliegue();
        PaquetePosEntidad paquete = despliegue.getPaquete();

        return new InstruccionAgente(
            true,
            TIPO_INSTRUCCION_ACTUALIZAR_POS,
            objetivo.getId(),
            paquete.getId(),
            paquete.getVersion(),
            "/api/packages/" + paquete.getId() + "/download",
            paquete.getChecksumSha256(),
            paquete.getFirma(),
            paquete.getAlgoritmoFirma(),
            paquete.getIdClaveFirma(),
            paquete.getClavePublicaFirmaPem(),
            despliegue.getHoraOficialActualizacion(),
            despliegue.getHoraForzadaActualizacion(),
            List.of(LocalTime.of(0, 50), LocalTime.of(0, 55))
        );
    }
}
