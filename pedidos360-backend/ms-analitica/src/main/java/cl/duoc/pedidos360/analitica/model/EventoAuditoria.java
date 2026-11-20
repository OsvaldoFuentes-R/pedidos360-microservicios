package cl.duoc.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Línea de tiempo de auditoría: copia de cada evento recibido, con su partición y offset de Kafka. */
@Entity
@Table(name = "eventos_auditoria",
        uniqueConstraints = @UniqueConstraint(columnNames = {"topico", "particion", "offset_kafka"}))
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topico;

    @Column(nullable = false)
    private int particion;

    @Column(name = "offset_kafka", nullable = false)
    private long offsetKafka;

    private String clave;

    private String tipo;

    @Column(length = 4000)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime recibidoEn;

    protected EventoAuditoria() {
        // requerido por JPA
    }

    public EventoAuditoria(String topico, int particion, long offsetKafka, String clave, String tipo, String payload) {
        this.topico = topico;
        this.particion = particion;
        this.offsetKafka = offsetKafka;
        this.clave = clave;
        this.tipo = tipo;
        this.payload = payload != null && payload.length() > 4000 ? payload.substring(0, 4000) : payload;
        this.recibidoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTopico() { return topico; }
    public int getParticion() { return particion; }
    public long getOffsetKafka() { return offsetKafka; }
    public String getClave() { return clave; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public LocalDateTime getRecibidoEn() { return recibidoEn; }
}
