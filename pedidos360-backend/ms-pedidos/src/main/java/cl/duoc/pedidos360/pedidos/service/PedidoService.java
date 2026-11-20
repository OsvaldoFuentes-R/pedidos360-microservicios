package cl.duoc.pedidos360.pedidos.service;

import cl.duoc.pedidos360.pedidos.client.ClientesClient;
import cl.duoc.pedidos360.pedidos.client.ProductosClient;
import cl.duoc.pedidos360.pedidos.dto.ClienteDto;
import cl.duoc.pedidos360.pedidos.dto.EventoNotificacion;
import cl.duoc.pedidos360.pedidos.dto.EventoTicket;
import cl.duoc.pedidos360.pedidos.dto.ProductoDto;
import cl.duoc.pedidos360.pedidos.dto.SolicitudPedido;
import cl.duoc.pedidos360.pedidos.dto.SolicitudPedido.ItemSolicitud;
import cl.duoc.pedidos360.pedidos.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.pedidos.error.SolicitudInvalidaException;
import cl.duoc.pedidos360.pedidos.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.pedidos.messaging.PedidoKafkaPublisher;
import cl.duoc.pedidos360.pedidos.model.ItemPedido;
import cl.duoc.pedidos360.pedidos.model.Pedido;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio de los pedidos.
 *
 * <p>Registrar un pedido: (1) valida la solicitud, el cliente y el stock sin modificar nada,
 * (2) reserva el stock en ms-productos y guarda el pedido (si algo falla, devuelve lo reservado),
 * (3) publica los eventos de notificación y de despacho en RabbitMQ y (4) el evento de analítica en Kafka.</p>
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository repositorio;
    private final ClientesClient clientes;
    private final ProductosClient productos;
    private final PedidoEventPublisher publicador;
    private final PedidoKafkaPublisher kafkaPublicador;

    public PedidoService(PedidoRepository repositorio, ClientesClient clientes,
                         ProductosClient productos, PedidoEventPublisher publicador,
                         PedidoKafkaPublisher kafkaPublicador) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.productos = productos;
        this.publicador = publicador;
        this.kafkaPublicador = kafkaPublicador;
    }

    public Pedido registrar(SolicitudPedido solicitud) {
        validar(solicitud);

        ClienteDto cliente = clientes.obtener(solicitud.clienteId());
        if (cliente == null) {
            throw new SolicitudInvalidaException("El cliente " + solicitud.clienteId() + " no existe");
        }

        // 1) Validar productos y stock sin tocar nada todavía
        List<ItemPedido> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemSolicitud pedido : solicitud.items()) {
            ProductoDto producto = productos.obtener(pedido.productoId());
            if (producto == null) {
                throw new SolicitudInvalidaException("El producto " + pedido.productoId() + " no existe");
            }
            if (producto.stock() < pedido.cantidad()) {
                throw new SolicitudInvalidaException("Stock insuficiente de '" + producto.nombre()
                        + "': disponible " + producto.stock() + ", solicitado " + pedido.cantidad());
            }
            items.add(new ItemPedido(producto.id(), producto.nombre(), pedido.cantidad(), producto.precio()));
            total = total.add(producto.precio().multiply(BigDecimal.valueOf(pedido.cantidad())));
        }

        // 2) Reservar stock y guardar. Si algo falla, se devuelve lo ya reservado.
        List<ItemPedido> reservados = new ArrayList<>();
        Pedido guardado;
        try {
            for (ItemPedido item : items) {
                productos.reservar(item.getProductoId(), item.getCantidad());
                reservados.add(item);
            }
            guardado = repositorio.save(new Pedido(cliente.id(), cliente.nombre(), total, items));
        } catch (RuntimeException e) {
            compensar(reservados);
            throw e;
        }

        // 3) Avisar a los demás servicios. El pedido ya está guardado: si RabbitMQ falla ahora,
        //    se deja registrado en el log en vez de perder el pedido.
        try {
            publicarEventos(guardado, cliente);
        } catch (AmqpException e) {
            log.error("Pedido {} guardado, pero no se pudieron publicar sus eventos: {}",
                    guardado.getId(), e.getMessage());
        }
        // 4) Evento para analítica en Kafka. Es independiente de RabbitMQ: si Kafka falla no se pierde el pedido.
        try {
            kafkaPublicador.publicarPedidoRegistrado(guardado);
        } catch (RuntimeException e) {
            log.error("Pedido {} guardado, pero no se pudo publicar en Kafka: {}", guardado.getId(), e.getMessage());
        }
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repositorio.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Pedido obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El pedido " + id + " no existe"));
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorCliente(Long clienteId) {
        return repositorio.findByClienteIdOrderByIdDesc(clienteId);
    }

    private void validar(SolicitudPedido solicitud) {
        if (solicitud == null || solicitud.clienteId() == null) {
            throw new SolicitudInvalidaException("La solicitud no indica el clienteId");
        }
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new SolicitudInvalidaException("La solicitud no tiene productos");
        }
        for (ItemSolicitud item : solicitud.items()) {
            if (item == null || item.productoId() == null) {
                throw new SolicitudInvalidaException("Hay un producto sin productoId en la solicitud");
            }
            if (item.cantidad() <= 0) {
                throw new SolicitudInvalidaException("La cantidad del producto " + item.productoId()
                        + " debe ser mayor que cero");
            }
        }
    }

    private void compensar(List<ItemPedido> reservados) {
        for (ItemPedido item : reservados) {
            try {
                productos.liberar(item.getProductoId(), item.getCantidad());
                log.info("Stock devuelto del producto {} (x{}) por un pedido fallido",
                        item.getProductoId(), item.getCantidad());
            } catch (RuntimeException e) {
                log.error("No se pudo devolver el stock del producto {} (x{}): {}",
                        item.getProductoId(), item.getCantidad(), e.getMessage());
            }
        }
    }

    private void publicarEventos(Pedido pedido, ClienteDto cliente) {
        int totalItems = pedido.getItems().stream().mapToInt(ItemPedido::getCantidad).sum();
        String mensaje = "Tu pedido #" + pedido.getId() + " fue registrado. Total: $" + pedido.getTotal();

        publicador.publicarNotificacion(new EventoNotificacion(pedido.getId(), cliente.id(),
                cliente.nombre(), cliente.email(), pedido.getTotal(), mensaje));
        publicador.publicarTicket(new EventoTicket(pedido.getId(), cliente.nombre(),
                cliente.direccion(), totalItems));
    }
}
