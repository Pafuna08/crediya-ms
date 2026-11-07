package co.com.crediya.autenticacion;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.exceptions.ServicioExternoException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba el adaptador contra un servidor HTTP real (el que trae Java) que imita al microservicio de autenticación.
 */
class AutenticacionClienteAdapterTest {

    private HttpServer servidor;
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();
    private final AtomicReference<String> autorizacionRecibida = new AtomicReference<>();
    private volatile int estadoRespuesta = 200;
    private volatile String cuerpoRespuesta = "[]";

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().toString());
            autorizacionRecibida.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            byte[] cuerpo = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estadoRespuesta, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();

        Jwt jwt = Jwt.withTokenValue("token-del-asesor").header("alg", "HS256").claim("rol", "ASESOR")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
        SecurityContextHolder.clearContext();
    }

    private AutenticacionClienteAdapter adapter() {
        return new AutenticacionClienteAdapter("http://localhost:" + servidor.getAddress().getPort(), 2000);
    }

    @Test
    void consultaLosClientesReenviandoElTokenDelAsesor() {
        cuerpoRespuesta = """
                [{"id":3,"nombres":"Ana María","apellidos":"Gómez","documento_identidad":"4000000000",
                  "correo_electronico":"ana@correo.com","salario_base":3000000,"rol":"CLIENTE"}]
                """;

        List<Cliente> clientes = adapter().findByDocumentos(List.of("3000000000", "4000000000"));

        assertEquals("/api/v1/usuarios?documentos=3000000000,4000000000", rutaRecibida.get());
        assertEquals("Bearer token-del-asesor", autorizacionRecibida.get());
        assertEquals(1, clientes.size());
        Cliente ana = clientes.getFirst();
        assertEquals("4000000000", ana.getDocumentoIdentidad());
        assertEquals("Ana María Gómez", ana.nombreCompleto());
        assertEquals("ana@correo.com", ana.getCorreoElectronico());
        assertEquals(new BigDecimal("3000000"), ana.getSalarioBase());
    }

    @Test
    void siAutenticacionRespondeConErrorLanzaUnaExcepcionControlada() {
        estadoRespuesta = 500;
        cuerpoRespuesta = "{\"mensajes\":[\"falla\"]}";

        ServicioExternoException ex = assertThrows(ServicioExternoException.class,
                () -> adapter().findByDocumentos(List.of("3000000000")));

        assertEquals("No fue posible consultar la información de los clientes", ex.getMessage());
    }

    @Test
    void siAutenticacionNoEstaDisponibleLanzaUnaExcepcionControlada() {
        AutenticacionClienteAdapter apagado = new AutenticacionClienteAdapter("http://localhost:1", 500);

        assertThrows(ServicioExternoException.class, () -> apagado.findByDocumentos(List.of("3000000000")));
    }

    @Test
    void sinDocumentosNoLlamaAAutenticacion() {
        assertTrue(adapter().findByDocumentos(List.of()).isEmpty());
        assertTrue(adapter().findByDocumentos(null).isEmpty());
        assertEquals(null, rutaRecibida.get());
    }
}
