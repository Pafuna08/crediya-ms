package co.com.crediya.autenticacion;

import co.com.crediya.model.cliente.Cliente;
import co.com.crediya.model.cliente.gateways.ClienteGateway;
import co.com.crediya.model.exceptions.ServicioExternoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adaptador de salida: obtiene los datos de los clientes llamando al microservicio de autenticación
 * (GET /api/v1/usuarios?documentos=...). Reenvía el token de quien hizo la petición original,
 * así autenticación aplica sus propios permisos.
 */
@Slf4j
@Component
public class AutenticacionClienteAdapter implements ClienteGateway {

    static final String RUTA_USUARIOS = "/api/v1/usuarios";
    /** Autenticación acepta como máximo 100 documentos por petición. */
    static final int DOCUMENTOS_POR_PETICION = 100;

    private final RestClient restClient;

    @Autowired
    public AutenticacionClienteAdapter(@Value("${autenticacion.url}") String urlBase,
                                       @Value("${autenticacion.timeout-ms:5000}") int timeoutMs) {
        this(RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabricaConTiempoLimite(timeoutMs))
                .build());
    }

    AutenticacionClienteAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Cliente> findByDocumentos(List<String> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return List.of();
        }
        String token = tokenDeLaPeticionActual();
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < documentos.size(); i += DOCUMENTOS_POR_PETICION) {
            List<String> grupo = documentos.subList(i, Math.min(i + DOCUMENTOS_POR_PETICION, documentos.size()));
            clientes.addAll(consultar(grupo, token));
        }
        log.info("Autenticación devolvió {} de {} clientes consultados", clientes.size(), documentos.size());
        return clientes;
    }

    private List<Cliente> consultar(List<String> documentos, String token) {
        log.debug("Consultando {} clientes en autenticación", documentos.size());
        try {
            UsuarioAutenticacion[] respuesta = restClient.get()
                    .uri(uri -> uri.path(RUTA_USUARIOS).queryParam("documentos", String.join(",", documentos)).build())
                    .headers(h -> {
                        if (token != null) {
                            h.setBearerAuth(token);
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(UsuarioAutenticacion[].class);
            return respuesta == null ? List.of()
                    : Arrays.stream(respuesta).map(UsuarioAutenticacion::toDomain).toList();
        } catch (RestClientException e) {
            log.error("Falló la consulta de clientes al microservicio de autenticación: {}", e.getMessage());
            throw new ServicioExternoException("No fue posible consultar la información de los clientes", e);
        }
    }

    private static String tokenDeLaPeticionActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof AbstractOAuth2TokenAuthenticationToken<?> conToken) {
            return conToken.getToken().getTokenValue();
        }
        log.warn("No hay token en la petición actual; se consultará autenticación sin credenciales");
        return null;
    }

    private static SimpleClientHttpRequestFactory fabricaConTiempoLimite(int timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(timeoutMs);
        fabrica.setReadTimeout(timeoutMs);
        return fabrica;
    }
}
