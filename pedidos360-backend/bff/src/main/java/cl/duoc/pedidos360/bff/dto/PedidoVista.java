package cl.duoc.pedidos360.bff.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que recibe el frontend: id, producto y estado (campos que ya usa la pantalla)
 * más datos adicionales que salen de combinar ms-pedidos y ms-despacho.
 */
public record PedidoVista(
        Long id,
        String producto,
        String estado,
        String cliente,
        BigDecimal total,
        LocalDateTime fecha,
        String ticket,
        List<Linea> items) {

    public record Linea(String producto, int cantidad, BigDecimal precioUnitario) {
    }
}
