package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.dto.ClusterInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ClusterService {

    private final AdminClient admin;
    private final KafkaAdminOperaciones kafka;

    public ClusterService(AdminClient admin, KafkaAdminOperaciones kafka) {
        this.admin = admin;
        this.kafka = kafka;
    }

    public ClusterInfo describir() {
        DescribeClusterResult resultado = admin.describeCluster();
        String id = kafka.esperar(resultado.clusterId(), "id del cluster");
        Node controlador = kafka.esperar(resultado.controller(), "controlador del cluster");
        List<ClusterInfo.Broker> brokers = kafka.esperar(resultado.nodes(), "brokers").stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new ClusterInfo.Broker(n.id(), n.host(), n.port()))
                .toList();
        return new ClusterInfo(id, controlador == null ? -1 : controlador.id(), brokers);
    }
}
