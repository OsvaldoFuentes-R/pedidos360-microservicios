package cl.duoc.pedidos360.analitica.service;

import cl.duoc.pedidos360.analitica.dto.MovimientoInventario;
import cl.duoc.pedidos360.analitica.dto.StockCritico;
import cl.duoc.pedidos360.analitica.model.MovimientoProcesado;
import cl.duoc.pedidos360.analitica.model.StockProducto;
import cl.duoc.pedidos360.analitica.repository.MovimientoProcesadoRepository;
import cl.duoc.pedidos360.analitica.repository.StockProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Mantiene el último stock conocido de cada producto y detecta stock crítico. */
@Service
public class AnaliticaInventarioService {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaInventarioService.class);

    private final StockProductoRepository stock;
    private final MovimientoProcesadoRepository procesados;

    public AnaliticaInventarioService(StockProductoRepository stock, MovimientoProcesadoRepository procesados) {
        this.stock = stock;
        this.procesados = procesados;
    }

    /** @return true si el movimiento se aplicó; false si ya se había procesado. */
    @Transactional
    public boolean procesar(MovimientoInventario movimiento) {
        if (procesados.existsByEventId(movimiento.eventId())) {
            log.info("[STOCK] Movimiento {} repetido; se ignora", movimiento.eventId());
            return false;
        }
        StockProducto actual = stock.findById(movimiento.productoId())
                .orElseGet(() -> new StockProducto(movimiento.productoId(), movimiento.nombreProducto()));
        actual.actualizar(movimiento.nombreProducto(), movimiento.stockResultante());
        stock.save(actual);
        procesados.save(new MovimientoProcesado(movimiento.eventId()));
        log.info("[STOCK] {} de {} unidades de '{}': stock actual {}", movimiento.tipo(), movimiento.cantidad(),
                movimiento.nombreProducto(), movimiento.stockResultante());
        return true;
    }

    @Transactional(readOnly = true)
    public List<StockCritico> stockCritico(int umbral) {
        return stock.findByStockActualLessThanEqualOrderByStockActualAsc(Math.max(umbral, 0)).stream()
                .map(s -> new StockCritico(s.getProductoId(), s.getNombre(), s.getStockActual()))
                .toList();
    }
}
