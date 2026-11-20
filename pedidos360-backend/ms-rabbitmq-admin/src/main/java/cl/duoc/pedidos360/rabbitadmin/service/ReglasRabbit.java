package cl.duoc.pedidos360.rabbitadmin.service;

import java.util.Collection;
import java.util.regex.Pattern;

/** Reglas puras (sin RabbitMQ) para nombres y elementos protegidos. */
public final class ReglasRabbit {

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("^[a-zA-Z0-9._:-]{1,255}$");

    private ReglasRabbit() {
    }

    public static void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException(
                    "Nombre inválido: solo letras, números, '.', '_', '-' y ':' (máximo 255)");
        }
        if (nombre.startsWith("amq.")) {
            throw new IllegalArgumentException("Los nombres que empiezan con 'amq.' están reservados por RabbitMQ");
        }
    }

    public static boolean colaProtegida(String nombre, Collection<String> prefijos) {
        return prefijos != null && prefijos.stream().anyMatch(nombre::startsWith);
    }

    public static boolean exchangeProtegido(String nombre, Collection<String> protegidos) {
        return protegidos != null && protegidos.contains(nombre);
    }

    /** La DLQ necesita las dos mitades del par (exchange y routing key) o ninguna. */
    public static void validarDeadLetter(String exchange, String routingKey) {
        boolean tieneExchange = exchange != null && !exchange.isBlank();
        boolean tieneClave = routingKey != null && !routingKey.isBlank();
        if (tieneExchange != tieneClave) {
            throw new IllegalArgumentException(
                    "deadLetterExchange y deadLetterRoutingKey deben indicarse juntos");
        }
    }
}
