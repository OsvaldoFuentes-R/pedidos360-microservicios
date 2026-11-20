package cl.duoc.pedidos360.kafkaadmin.service;

import cl.duoc.pedidos360.kafkaadmin.dto.GrupoInfo;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Consulta y elimina consumer groups; calcula el lag (mensajes pendientes) de cada partición. */
@Service
public class GrupoService {

    private static final Logger log = LoggerFactory.getLogger(GrupoService.class);

    private final AdminClient admin;
    private final KafkaAdminOperaciones kafka;

    public GrupoService(AdminClient admin, KafkaAdminOperaciones kafka) {
        this.admin = admin;
        this.kafka = kafka;
    }

    public List<String> listar() {
        return kafka.esperar(admin.listConsumerGroups().all(), "listar consumer groups").stream()
                .map(ConsumerGroupListing::groupId)
                .sorted()
                .toList();
    }

    public GrupoInfo describir(String grupo) {
        ConsumerGroupDescription descripcion = kafka.esperar(
                admin.describeConsumerGroups(List.of(grupo)).describedGroups().get(grupo), "grupo " + grupo);
        Map<TopicPartition, OffsetAndMetadata> consumidos = kafka.esperar(
                admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata(), "offsets del grupo " + grupo);

        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        consumidos.keySet().forEach(tp -> pedido.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales = pedido.isEmpty()
                ? Map.of()
                : kafka.esperar(admin.listOffsets(pedido).all(), "offsets finales de " + grupo);

        List<GrupoInfo.Offset> offsets = new ArrayList<>();
        long lagTotal = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : consumidos.entrySet()) {
            long consumido = e.getValue().offset();
            long fin = finales.get(e.getKey()).offset();
            long lag = Math.max(0, fin - consumido);
            lagTotal += lag;
            offsets.add(new GrupoInfo.Offset(e.getKey().topic(), e.getKey().partition(), consumido, fin, lag));
        }
        offsets.sort(Comparator.comparing(GrupoInfo.Offset::topico).thenComparingInt(GrupoInfo.Offset::particion));
        return new GrupoInfo(grupo, descripcion.state().toString(), descripcion.members().size(), lagTotal, offsets);
    }

    /** Solo se puede eliminar un grupo sin miembros activos (si no, Kafka responde GroupNotEmpty). */
    public void eliminar(String grupo) {
        kafka.esperar(admin.deleteConsumerGroups(List.of(grupo)).all(), "eliminar el grupo " + grupo);
        log.warn("Consumer group eliminado: {}", grupo);
    }
}
