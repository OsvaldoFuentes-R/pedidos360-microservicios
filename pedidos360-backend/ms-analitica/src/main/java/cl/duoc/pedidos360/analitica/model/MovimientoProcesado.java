package cl.duoc.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Registro de los eventIds de inventario ya procesados (idempotencia). */
@Entity
@Table(name = "movimientos_procesados")
public class MovimientoProcesado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    protected MovimientoProcesado() {
        // requerido por JPA
    }

    public MovimientoProcesado(String eventId) {
        this.eventId = eventId;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
}
