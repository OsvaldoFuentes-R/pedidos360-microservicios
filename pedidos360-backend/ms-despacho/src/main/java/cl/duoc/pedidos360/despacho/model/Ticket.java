package cl.duoc.pedidos360.despacho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Ticket de despacho de un pedido (uno por pedido). */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pedidoId;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String clienteNombre;

    @Column(nullable = false)
    private String direccionEntrega;

    private int totalItems;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTicket estado;

    @Column(nullable = false)
    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaActualizacion;

    protected Ticket() {
        // requerido por JPA
    }

    public Ticket(Long pedidoId, String codigo, String clienteNombre, String direccionEntrega, int totalItems) {
        this.pedidoId = pedidoId;
        this.codigo = codigo;
        this.clienteNombre = clienteNombre;
        this.direccionEntrega = direccionEntrega;
        this.totalItems = totalItems;
        this.estado = EstadoTicket.GENERADO;
        this.fechaGeneracion = LocalDateTime.now();
    }

    public void cambiarEstado(EstadoTicket nuevoEstado) {
        this.estado = nuevoEstado;
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public String getCodigo() { return codigo; }
    public String getClienteNombre() { return clienteNombre; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public int getTotalItems() { return totalItems; }
    public EstadoTicket getEstado() { return estado; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
