package com.retronova.engine.sound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sound.sampled.AudioSystem;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica os WAV reais: silêncio inicial e caudas longas não aparecem em teste de chamada. */
class SonsDasArmasTest {
    private static final String AUDIO = "/com/retronova/resources/audio/";

    @Test
    void todoEfeitoRegistradoTemArquivoDecodificavel() throws Exception {
        for (Sounds som : Sounds.values()) {
            var arquivo = getClass().getResource(AUDIO + som.resource() + ".wav");
            assertNotNull(arquivo, som.name());
            try (var in = AudioSystem.getAudioInputStream(arquivo)) {
                assertTrue(in.getFrameLength() > 0, som.name());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"hit_fire", "hit_ice", "hit_water", "hit_wind", "hit_earth", "hit_thunder",
            "weapon_blade", "weapon_magic", "weapon_throw", "weapon_impact", "weapon_aura", "weapon_explosion",
            "spell_arcane"})
    void golpeTemAtaqueImediatoECaudaCurta(String nome) throws Exception {
        try (var in = AudioSystem.getAudioInputStream(getClass().getResource(AUDIO + nome + ".wav"))) {
            var formato = in.getFormat();
            assertEquals(16, formato.getSampleSizeInBits());
            assertEquals(1, formato.getChannels());
            assertFalse(formato.isBigEndian());
            byte[] bytes = in.readAllBytes();
            double[] amostras = new double[bytes.length / 2];
            double pico = 0, energia = 0;
            for (int i = 0; i < amostras.length; i++) {
                double v = (short) ((bytes[2 * i] & 0xFF) | (bytes[2 * i + 1] << 8)) / 32768d;
                amostras[i] = Math.abs(v);
                pico = Math.max(pico, amostras[i]);
                energia += v * v;
            }
            int inicio = 0;
            while (inicio < amostras.length && amostras[inicio] < pico * 0.1) {
                inicio++;
            }
            assertTrue(inicio / formato.getSampleRate() <= 0.010, "Ataque atrasado: " + nome);
            assertTrue(amostras.length / formato.getSampleRate() <= 0.351, "Cauda longa: " + nome);
            assertEquals(-28, 20 * Math.log10(Math.sqrt(energia / amostras.length)), 0.2,
                    "Fora da camada de rotina: " + nome);
            assertTrue(pico < 0.85, "Sem folga para a mixagem: " + nome);
        }
    }
}
