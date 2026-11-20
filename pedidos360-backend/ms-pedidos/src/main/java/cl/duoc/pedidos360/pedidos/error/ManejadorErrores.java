package cl.duoc.pedidos360.pedidos.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Respuestas JSON de error para la API REST de consulta de pedidos. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", HttpStatus.NOT_FOUND.value());
        cuerpo.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        cuerpo.put("mensaje", e.getMessage());
        cuerpo.put("fecha", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
    }
}
