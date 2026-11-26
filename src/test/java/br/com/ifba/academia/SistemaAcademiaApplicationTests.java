package br.com.ifba.academia;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class AcademiaApplicationTests {
    @Test
    void entrypointDaAplicacaoEstaDisponivel() {
        assertNotNull(AcademiaApplication.class);
    }
}
