package cl.duoc.pedidos360.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Reenvía al microservicio ms-pedidos360-kafka las rutas /api/kafka/**, igual que con orders y catalog.
 *
 * - /api/kafka/streaming/** (KPIs, historial, inventario, DLT): lectura para Admin y Operator.
 * - /api/kafka/admin/** y /api/kafka/publish/** (tópicos, consumer groups, offsets, eventos de prueba):
 *   solo Admin, porque son operaciones que cambian la infraestructura.
 *
 * Los códigos de error del microservicio (404, 409, 400...) se devuelven tal cual al cliente.
 */
@RestController
public class KafkaProxyController {

    private final RestClient restClient;
    private final String kafkaServiceUrl;

    public KafkaProxyController(@Value("${kafka.service.url}") String kafkaServiceUrl) {
        this.kafkaServiceUrl = kafkaServiceUrl;
        this.restClient = RestClient.builder().build();
    }

    @GetMapping("/api/kafka/streaming/**")
    @PreAuthorize("hasAnyRole('Admin', 'Operator')")
    public ResponseEntity<String> streaming(HttpServletRequest request) {
        return forward(HttpMethod.GET, request, null);
    }

    @RequestMapping(
            value = {"/api/kafka/admin/**", "/api/kafka/publish/**"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> admin(HttpServletRequest request,
                                        @RequestBody(required = false) String body) {
        return forward(HttpMethod.valueOf(request.getMethod()), request, body);
    }

    private ResponseEntity<String> forward(HttpMethod method, HttpServletRequest request, String body) {
        String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        URI target = URI.create(kafkaServiceUrl + request.getRequestURI() + query);

        RestClient.RequestBodySpec spec = restClient.method(method).uri(target);
        if (body != null && !body.isBlank()) {
            spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }

        ResponseEntity<String> response = spec.retrieve()
                .onStatus(status -> true, (req, res) -> { })   
                .toEntity(String.class);

        return ResponseEntity.status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }
}