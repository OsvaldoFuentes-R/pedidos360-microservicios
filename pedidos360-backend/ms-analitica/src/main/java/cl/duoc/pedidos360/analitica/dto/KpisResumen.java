package cl.duoc.pedidos360.analitica.dto;

import java.math.BigDecimal;

public record KpisResumen(long pedidos, BigDecimal ingresos, BigDecimal ticketPromedio, long unidadesVendidas) {
}
