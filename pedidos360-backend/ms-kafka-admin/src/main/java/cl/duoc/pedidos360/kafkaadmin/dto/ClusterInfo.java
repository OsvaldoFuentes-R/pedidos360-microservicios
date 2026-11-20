package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ClusterInfo(String clusterId, int controlador, List<Broker> brokers) {

    public record Broker(int id, String host, int puerto) {
    }
}
