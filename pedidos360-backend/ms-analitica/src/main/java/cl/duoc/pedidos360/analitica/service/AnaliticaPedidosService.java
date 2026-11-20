package cl.duoc.pedidos360.analitica.service;

import cl.duoc.pedidos360.analitica.dto.KpisResumen;
import cl.duoc.pedidos360.analitica.dto.PedidoEvento;
import cl.duoc.pedidos360.analitica.dto.ProductoTop;
import cl.duoc.pedidos360.analitica.model.PedidoAnalitica;
import cl.duoc.pedidos360.analitica.model.VentaProducto;
import cl.duoc.pedidos360.analitica.repository.PedidoAnaliticaRepository;
import cl.duoc.pedidos360.analitica.repository.VentaProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Calcula los KPIs de ventas a partir de los eventos de pedidos. */
@Service
public class AnaliticaPedidosService {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaPedidosService.class);

    private final PedidoAnaliticaRepository pedidos;
    private final VentaProductoRepository ventas;

    public AnaliticaPedidosService(PedidoAnaliticaRepository pedidos, VentaProductoRepository ventas) {
        this.pedidos = pedidos;
        this.ventas = ventas;
    }

    /** @return true si el evento se contabilizó; false si ya estaba (Kafka entrega "al menos una vez"). */
    @Transactional
    public boolean procesar(PedidoEvento evento) {
        if (pedidos.existsByEventId(evento.eventId())) {
            log.info("[KPI] Evento {} repetido; se ignora", evento.eventId());
            return false;
        }
        int unidades = 0;
        for (PedidoEvento.ItemEvento item : evento.items()) {
            unidades += item.cantidad();
            VentaProducto venta = ventas.findById(item.productoId())
                    .orElseGet(() -> new VentaProducto(item.productoId(), item.nombreProducto()));
            venta.sumar(item.cantidad());
            ventas.save(venta);
        }
        pedidos.save(new PedidoAnalitica(evento.eventId(), evento.pedidoId(), evento.clienteNombre(),
                evento.total(), unidades));
        log.info("[KPI] Pedido {} contabilizado: total {} y {} unidades", evento.pedidoId(), evento.total(), unidades);
        return true;
    }

    @Transactional(readOnly = true)
    public KpisResumen kpis() {
        long cantidad = pedidos.count();
        BigDecimal ingresos = pedidos.sumaIngresos();
        BigDecimal promedio = cantidad == 0 ? BigDecimal.ZERO
                : ingresos.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        return new KpisResumen(cantidad, ingresos, promedio, pedidos.sumaUnidades());
    }

    @Transactional(readOnly = true)
    public List<ProductoTop> masVendidos(int limite) {
        int tope = Math.max(1, Math.min(limite, 50));
        return ventas.masVendidos(PageRequest.of(0, tope)).stream()
                .map(v -> new ProductoTop(v.getProductoId(), v.getNombre(), v.getUnidades()))
                .toList();
    }
}
