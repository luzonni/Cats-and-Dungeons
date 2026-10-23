package com.retronova.engine.sound;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.sound.sampled.AudioSystem;
import static org.junit.jupiter.api.Assertions.*;

class SonsDaMorteTest {
    @ParameterizedTest
    @ValueSource(strings = {"death_muffin", "death_azrael", "death_finn", "run_over"})
    void despedidaTemAtaqueImediatoENivelDeEvento(String nome) throws Exception {
        var url = getClass().getResource("/com/retronova/resources/audio/" + nome + ".wav");
        assertNotNull(url);
        try (var in = AudioSystem.getAudioInputStream(url)) {
            byte[] bytes = in.readAllBytes();
            double energia = 0, pico = 0;
            int primeiro = -1;
            for (int i = 0; i < bytes.length / 2; i++) {
                double v = (short) ((bytes[i * 2] & 255) | bytes[i * 2 + 1] << 8) / 32768.0;
                energia += v * v;
                pico = Math.max(pico, Math.abs(v));
                if (primeiro < 0 && Math.abs(v) > 0.01) primeiro = i;
            }
            double duracao = bytes.length / 2.0 / in.getFormat().getSampleRate();
            assertTrue(duracao > 0.30 && duracao <= 1.21);
            assertTrue(primeiro >= 0 && primeiro / in.getFormat().getSampleRate() < 0.015);
            assertTrue(pico < 0.85);
            assertEquals(nome.equals("run_over") ? -24 : -20,
                    20 * Math.log10(Math.sqrt(energia / (bytes.length / 2))), 0.3);
        }
    }
}
