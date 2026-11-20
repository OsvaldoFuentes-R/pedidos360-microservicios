package cl.duoc.pedidos360.clientes.config;

import cl.duoc.pedidos360.clientes.model.Cliente;
import cl.duoc.pedidos360.clientes.repository.ClienteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Carga clientes de ejemplo al arrancar (la base H2 en memoria parte vacía). */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ClienteRepository repositorio;

    public DatosIniciales(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(String... args) {
        if (repositorio.count() > 0) {
            return;
        }
        repositorio.saveAll(List.of(
                new Cliente("Camila Rojas", "camila.rojas@example.com", "+56 9 1111 1111",
                        "Av. Providencia 1234, Santiago"),
                new Cliente("Diego Fuentes", "diego.fuentes@example.com", "+56 9 2222 2222",
                        "Calle O'Higgins 567, Valparaíso"),
                new Cliente("Valentina Soto", "valentina.soto@example.com", "+56 9 3333 3333",
                        "Pasaje Los Aromos 89, Concepción")));
    }
}
