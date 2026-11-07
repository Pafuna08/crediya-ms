package co.com.crediya.usecase.consultarusuarios;

import co.com.crediya.model.exceptions.DatosInvalidosException;
import co.com.crediya.model.user.User;
import co.com.crediya.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: consultar los datos de varios usuarios a partir de sus documentos de identidad.
 * Lo usa el microservicio de solicitudes para mostrar nombre, correo y salario en el listado de revisión (HU4).
 */
@Log
@RequiredArgsConstructor
public class ConsultarUsuariosUseCase {

    static final int MAXIMO_DOCUMENTOS = 100;

    private final UserRepository userRepository;

    public List<User> buscarPorDocumentos(List<String> documentos) {
        List<String> limpios = documentos == null ? List.of() : documentos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(d -> !d.isEmpty())
                .distinct()
                .toList();

        if (limpios.isEmpty()) {
            throw new DatosInvalidosException(List.of("Debe indicar al menos un documento de identidad"));
        }
        if (limpios.size() > MAXIMO_DOCUMENTOS) {
            throw new DatosInvalidosException(List.of(
                    "No se pueden consultar más de " + MAXIMO_DOCUMENTOS + " documentos por petición"));
        }

        List<User> usuarios = userRepository.findByDocumentosIdentidad(limpios);
        log.info("Consulta de usuarios: " + limpios.size() + " documentos pedidos, " + usuarios.size() + " encontrados");
        return usuarios;
    }
}
