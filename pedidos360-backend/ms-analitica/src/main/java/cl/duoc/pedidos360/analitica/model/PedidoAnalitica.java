package cl.duoc.pedidos360.analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pedido ya contabilizado en los KPIs. eventId único: un evento repetido no se cuenta dos veces. */
@Entity
@Table(name = "pedidos_analitica")
public class PedidoAnalitica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private Long pedidoId;

    private String clienteNombre;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    private int unidades;

    @Column(nullable = false)
    private LocalDateTime procesadoEn;

    protected PedidoAnalitica() {
        // requerido por JPA
    }

    public PedidoAnalitica(String eventId, Long pedidoId, String clienteNombre, BigDecimal total, int unidades) {
        this.eventId = eventId;
        this.pedidoId = pedidoId;
        this.clienteNombre = clienteNombre;
        this.total = total;
        this.unidades = unidades;
        this.procesadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getPedidoId() { return pedidoId; }
    public String getClienteNombre() { return clienteNombre; }
    public BigDecimal getTotal() { return total; }
    public int getUnidades() { return unidades; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
}
