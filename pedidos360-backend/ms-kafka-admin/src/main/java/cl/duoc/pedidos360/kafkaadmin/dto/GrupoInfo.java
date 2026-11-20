package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record GrupoInfo(
        String grupo,
        String estado,
        int miembros,
        long lagTotal,
        List<Offset> offsets) {

    /** lag = mensajes que el grupo todavía no ha leído en esa partición. */
    public record Offset(String topico, int particion, long offsetConsumido, long offsetFinal, long lag) {
    }
}
