package cl.duoc.pedidos360.kafkaadmin.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Reglas de validación puras (sin Kafka) para nombres y configuraciones de tópicos. */
public final class ReglasTopico {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._-]{1,249}$");

    /** Configuraciones que este servicio permite cambiar. Cualquier otra se rechaza. */
    public static final Set<String> CONFIGURACIONES_PERMITIDAS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "segment.bytes", "segment.ms", "max.message.bytes", "compression.type");

    private static final Set<String> POLITICAS_VALIDAS = Set.of("delete", "compact", "compact,delete", "delete,compact");

    private ReglasTopico() {
    }

    public static void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches() || nombre.equals(".") || nombre.equals("..")) {
            throw new IllegalArgumentException("Nombre de tópico inválido: solo letras, números, '.', '_' y '-' (máximo 249)");
        }
    }

    public static void validarConfiguracion(Map<String, String> configuracion) {
        if (configuracion == null) {
            return;
        }
        for (Map.Entry<String, String> entrada : configuracion.entrySet()) {
            if (!CONFIGURACIONES_PERMITIDAS.contains(entrada.getKey())) {
                throw new IllegalArgumentException("La configuración '" + entrada.getKey()
                        + "' no está permitida. Permitidas: " + CONFIGURACIONES_PERMITIDAS);
            }
            if (entrada.getValue() == null || entrada.getValue().isBlank()) {
                throw new IllegalArgumentException("La configuración '" + entrada.getKey() + "' no tiene valor");
            }
        }
        String politica = configuracion.get("cleanup.policy");
        if (politica != null && !POLITICAS_VALIDAS.contains(politica)) {
            throw new IllegalArgumentException("cleanup.policy debe ser delete, compact o compact,delete");
        }
    }

    /** Las réplicas no pueden superar la cantidad de brokers vivos. */
    public static void validarReplicas(int replicas, int brokers) {
        if (replicas < 1) {
            throw new IllegalArgumentException("Las réplicas deben ser al menos 1");
        }
        if (replicas > brokers) {
            throw new IllegalArgumentException("Hay " + brokers + " brokers disponibles: no se pueden pedir " + replicas + " réplicas");
        }
    }

    public static boolean esProtegido(String nombre, Collection<String> protegidos) {
        return protegidos != null && protegidos.contains(nombre);
    }
}
