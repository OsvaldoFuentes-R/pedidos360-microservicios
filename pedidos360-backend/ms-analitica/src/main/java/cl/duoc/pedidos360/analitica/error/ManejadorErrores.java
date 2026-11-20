package cl.duoc.pedidos360.analitica.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> error(RuntimeException e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", 500);
        cuerpo.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        cuerpo.put("mensaje", e.getMessage());
        cuerpo.put("fecha", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo);
    }
}
