package cl.duoc.pedidos360.pedidos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pedido registrado. Guarda una copia del nombre del cliente para no depender de ms-clientes al leerlo. */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private String clienteNombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private List<ItemPedido> items = new ArrayList<>();

    protected Pedido() {
        // requerido por JPA
    }

    public Pedido(Long clienteId, String clienteNombre, BigDecimal total, List<ItemPedido> items) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.total = total;
        this.items = new ArrayList<>(items);
        this.estado = EstadoPedido.REGISTRADO;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public BigDecimal getTotal() { return total; }
    public EstadoPedido getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
    public List<ItemPedido> getItems() { return items; }
}
