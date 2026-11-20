package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record TopicoInfo(
        String nombre,
        int particiones,
        int replicas,
        boolean interno,
        List<ParticionInfo> detalle,
        Map<String, String> configuracion) {

    public record ParticionInfo(int particion, int lider, List<Integer> replicas, List<Integer> isr) {
    }
}
