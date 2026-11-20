package cl.duoc.pedidos360.clientes.service;

import cl.duoc.pedidos360.clientes.error.ConflictoException;
import cl.duoc.pedidos360.clientes.error.RecursoNoEncontradoException;
import cl.duoc.pedidos360.clientes.model.Cliente;
import cl.duoc.pedidos360.clientes.repository.ClienteRepository;
import cl.duoc.pedidos360.clientes.web.ClienteRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente " + id + " no existe"));
    }

    @Transactional
    public Cliente crear(ClienteRequest datos) {
        if (repositorio.existsByEmail(datos.email())) {
            throw new ConflictoException("Ya existe un cliente con el email " + datos.email());
        }
        return repositorio.save(new Cliente(datos.nombre(), datos.email(), datos.telefono(), datos.direccion()));
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest datos) {
        Cliente cliente = obtener(id);
        if (repositorio.existsByEmailAndIdNot(datos.email(), id)) {
            throw new ConflictoException("Ya existe otro cliente con el email " + datos.email());
        }
        cliente.setNombre(datos.nombre());
        cliente.setEmail(datos.email());
        cliente.setTelefono(datos.telefono());
        cliente.setDireccion(datos.direccion());
        return repositorio.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException("El cliente " + id + " no existe");
        }
        repositorio.deleteById(id);
    }
}
