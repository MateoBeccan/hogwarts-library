package com.hogwarts.library.service.impl;

import com.hogwarts.library.domain.SecuenciaCodigo;
import com.hogwarts.library.repository.SecuenciaCodigoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjemplarCodigoGeneratorTest {

    @Mock
    private SecuenciaCodigoRepository secuenciaCodigoRepository;

    private EjemplarCodigoGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new EjemplarCodigoGenerator(secuenciaCodigoRepository);
    }

    @Test
    void generarSiguienteCodigo_deberiaIncrementarSecuenciaBloqueadaYFormatearCodigo() {

        SecuenciaCodigo secuencia = new SecuenciaCodigo("EJEMPLAR", 3L);

        when(secuenciaCodigoRepository.findByNombreForUpdate("EJEMPLAR"))
                .thenReturn(Optional.of(secuencia));

        String codigo = generator.generarSiguienteCodigo();

        assertEquals("HOG-000004", codigo);
        assertEquals(4L, secuencia.getUltimoValor());
        verify(secuenciaCodigoRepository).findByNombreForUpdate("EJEMPLAR");
        verify(secuenciaCodigoRepository).save(secuencia);
    }

    @Test
    void formatear_deberiaUsarPrefijoHogYSeisDigitos() {
        assertEquals("HOG-000001", generator.formatear(1));
        assertEquals("HOG-000027", generator.formatear(27));
        assertEquals("HOG-001058", generator.formatear(1058));
    }
}
