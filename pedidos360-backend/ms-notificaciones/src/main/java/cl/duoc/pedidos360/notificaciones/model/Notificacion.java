package cl.duoc.pedidos360.notificaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Notificación enviada a un cliente (el envío del correo es simulado: se guarda y se registra en el log). */
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    private Long clienteId;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String asunto;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false)
    private String canal;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Notificacion() {
        // requerido por JPA
    }

    public Notificacion(Long pedidoId, Long clienteId, String destinatario, String asunto,
                        String mensaje, String canal) {
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.canal = canal;
        this.estado = "ENVIADA";
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getPedidoId() { return pedidoId; }
    public Long getClienteId() { return clienteId; }
    public String getDestinatario() { return destinatario; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getCanal() { return canal; }
    public String getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
}
