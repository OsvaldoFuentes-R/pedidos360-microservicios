package cl.duoc.pedidos360.bff.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Pedido tal como lo devuelve ms-pedidos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoRemoto(
        Long id,
        Long clienteId,
        String clienteNombre,
        BigDecimal total,
        String estado,
        LocalDateTime fecha,
        List<ItemRemoto> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemRemoto(Long productoId, String nombreProducto, int cantidad, BigDecimal precioUnitario) {
    }
}
