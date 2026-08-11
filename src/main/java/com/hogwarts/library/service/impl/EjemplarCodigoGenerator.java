package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.SecuenciaCodigo;
import com.hogwarts.library.exception.ResourceNotFoundException;
import com.hogwarts.library.repository.SecuenciaCodigoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EjemplarCodigoGenerator {

    private static final String SECUENCIA_EJEMPLAR = "EJEMPLAR";
    private static final String PREFIJO = "HOG";

    private final SecuenciaCodigoRepository secuenciaCodigoRepository;

    public EjemplarCodigoGenerator(
            SecuenciaCodigoRepository secuenciaCodigoRepository
    ) {
        this.secuenciaCodigoRepository = secuenciaCodigoRepository;
    }

    @Transactional
    public String generarSiguienteCodigo() {

        SecuenciaCodigo secuencia =
                secuenciaCodigoRepository
                        .findByNombreForUpdate(SECUENCIA_EJEMPLAR)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No existe la secuencia de cÃƒÂ³digo para ejemplares"
                                )
                        );

        long siguienteValor = secuencia.getUltimoValor() + 1;
        secuencia.setUltimoValor(siguienteValor);

        secuenciaCodigoRepository.save(secuencia);

        return formatear(siguienteValor);
    }

    public String formatear(long valor) {
        return "%s-%06d".formatted(PREFIJO, valor);
    }
}
