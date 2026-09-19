package com.retronova.game;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do save de corrida.
 *
 * DUAS REGRAS SAO GUARDADAS AQUI, e as duas sustentam a permadeath.
 *
 * A PRIMEIRA E O APAGAR AO LER. Um save que sobrevive ao carregamento vira um
 * "desfazer" — morreu, fecha o jogo, abre de novo, tenta outra vez — e a morte
 * permanente deixa de existir sem que ninguem tenha tocado na regra dela.
 *
 * A SEGUNDA E A INDEPENDENCIA DOS ESPACOS. Os tres guardam corridas DIFERENTES,
 * e nao pontos diferentes da mesma. Se um dia uma partida passar a escrever em
 * mais de um espaco, o save-scum volta inteiro pela porta da frente: bastaria
 * guardar uma copia antes de uma sala perigosa e recarrega-la ao morrer.
 *
 * As duas se quebram sem barulho, porque quebrar so torna o jogo mais
 * permissivo: nada trava, nada da erro, e o defeito so aparece quando alguem
 * repara que perder deixou de custar. Por isso estao escritas em teste.
 *
 * Nada aqui depende de partida carregada — e leitura e escrita de arquivo.
 */
class CorridaTest {

    private static Path arquivo(int espaco) {
        return Paths.get("saves", "corrida-" + espaco + ".json");
    }

    private static final Path LEGADO = Paths.get("corrida.json");

    /**
     * Onde os saves de verdade ficam guardados enquanto os testes rodam.
     *
     * O caminho e relativo ao diretorio de trabalho, que na hora do teste e a raiz
     * do projeto — a mesma em que o jogo grava. Sem este cuidado, rodar a suite
     * apagaria as corridas de quem estivesse jogando na maquina.
     */
    private final List<Path> afastados = new ArrayList<>();

    private void afastar(Path original) throws IOException {
        if (!Files.exists(original)) {
            return;
        }
        Path guardado = original.resolveSibling(original.getFileName() + ".teste-guardado");
        Files.move(original, guardado, StandardCopyOption.REPLACE_EXISTING);
        afastados.add(original);
    }

    @BeforeEach
    void afastarOsSavesReais() throws IOException {
        afastar(LEGADO);
        for (int i = 1; i <= Corrida.ESPACOS; i++) {
            afastar(arquivo(i));
        }
    }

    @AfterEach
    void devolverOsSavesReais() throws IOException {
        Files.deleteIfExists(LEGADO);
        for (int i = 1; i <= Corrida.ESPACOS; i++) {
            Files.deleteIfExists(arquivo(i));
        }
        for (Path original : afastados) {
            Path guardado = original.resolveSibling(original.getFileName() + ".teste-guardado");
            if (Files.exists(guardado)) {
                Files.createDirectories(original.getParent() == null
                        ? Paths.get(".") : original.getParent());
                Files.move(guardado, original, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        afastados.clear();
    }

    private static void escrever(Path onde, String json) throws IOException {
        if (onde.getParent() != null) {
            Files.createDirectories(onde.getParent());
        }
        Files.writeString(onde, json, StandardCharsets.UTF_8);
    }

    /** Um save valido, com o nivel servindo de marca para saber de qual espaco veio. */
    private static String valido(int nivel) {
        return "{"
                + "\"versao\":1,"
                + "\"gato\":2,"
                + "\"nivel\":" + nivel + ","
                + "\"dificuldade\":1,"
                + "\"segundos\":423,"
                + "\"vida\":18.5,"
                + "\"xp\":140.25,"
                + "\"nivelDoGato\":3,"
                + "\"dinheiro\":260,"
                + "\"hotbar\":[[0,1,1],[2,5,3]],"
                + "\"mochila\":[[4,9,1]],"
                + "\"passivos\":[]"
                + "}";
    }

    // ------------------------------------------------------------ o basico

    @Test
    @DisplayName("sem arquivo, nao ha o que continuar")
    void semArquivo() {
        assertFalse(Corrida.existeAlguma());
        for (int i = 1; i <= Corrida.ESPACOS; i++) {
            assertFalse(Corrida.existe(i));
            assertNull(Corrida.consumir(i));
        }
    }

    @Test
    @DisplayName("le todos os campos de volta")
    void leDeVolta() throws IOException {
        escrever(arquivo(2), valido(7));
        assertTrue(Corrida.existe(2));

        Corrida corrida = Corrida.consumir(2);
        assertNotNull(corrida);
        assertEquals(2, corrida.gato());
        assertEquals(7, corrida.nivel());
        assertEquals(1, corrida.dificuldade());
        assertEquals(423, corrida.segundos());
    }

    @Test
    @DisplayName("ler apaga o arquivo — e o que impede desfazer a morte")
    void lerApaga() throws IOException {
        escrever(arquivo(1), valido(7));
        assertNotNull(Corrida.consumir(1));

        assertFalse(Files.exists(arquivo(1)), "o save tem de sumir ao ser lido");
        assertFalse(Corrida.existe(1));
        assertNull(Corrida.consumir(1), "nao da para retomar duas vezes o mesmo ponto");
    }

    // --------------------------------------------- os espacos nao se misturam

    @Test
    @DisplayName("cada espaco guarda a sua corrida")
    void espacosSaoIndependentes() throws IOException {
        escrever(arquivo(1), valido(1));
        escrever(arquivo(2), valido(2));
        escrever(arquivo(3), valido(3));

        assertEquals(1, Corrida.espiar(1).nivel());
        assertEquals(2, Corrida.espiar(2).nivel());
        assertEquals(3, Corrida.espiar(3).nivel());
    }

    @Test
    @DisplayName("retomar um espaco nao toca nos outros")
    void consumirNaoVazaEntreEspacos() throws IOException {
        escrever(arquivo(1), valido(1));
        escrever(arquivo(2), valido(2));
        escrever(arquivo(3), valido(3));

        assertEquals(2, Corrida.consumir(2).nivel());

        assertFalse(Corrida.existe(2));
        assertTrue(Corrida.existe(1), "comecar ou retomar uma corrida nao custa as outras");
        assertTrue(Corrida.existe(3));
        assertEquals(1, Corrida.espiar(1).nivel());
        assertEquals(3, Corrida.espiar(3).nivel());
    }

    @Test
    @DisplayName("apagar um espaco nao toca nos outros")
    void apagarNaoVazaEntreEspacos() throws IOException {
        escrever(arquivo(1), valido(1));
        escrever(arquivo(3), valido(3));

        Corrida.apagar(1);

        assertFalse(Corrida.existe(1));
        assertTrue(Corrida.existe(3));
    }

    @Test
    @DisplayName("espaco fora da faixa nao cria nem apaga nada")
    void espacoInvalido() throws IOException {
        escrever(arquivo(1), valido(1));

        assertFalse(Corrida.existe(0));
        assertFalse(Corrida.existe(Corrida.ESPACOS + 1));
        assertNull(Corrida.espiar(0));
        assertNull(Corrida.consumir(99));
        Corrida.apagar(-3);

        assertTrue(Corrida.existe(1), "um indice invalido nao pode respingar num espaco valido");
    }

    @Test
    @DisplayName("primeiroLivre acha o buraco, e -1 quando esta tudo cheio")
    void primeiroLivre() throws IOException {
        assertEquals(1, Corrida.primeiroLivre());

        escrever(arquivo(1), valido(1));
        assertEquals(2, Corrida.primeiroLivre());

        escrever(arquivo(2), valido(2));
        escrever(arquivo(3), valido(3));
        assertEquals(-1, Corrida.primeiroLivre());
        assertTrue(Corrida.existeAlguma());
    }

    // ------------------------------------------------------------ o legado

    @Test
    @DisplayName("o save da versao de um espaco so vira o espaco 1")
    void migraOSaveAntigo() throws IOException {
        escrever(LEGADO, valido(5));

        assertTrue(Corrida.existe(1), "quem estava jogando nao pode perder a corrida na mudanca");
        assertEquals(5, Corrida.espiar(1).nivel());
        assertFalse(Files.exists(LEGADO), "migrado uma vez, o arquivo antigo nao fica para tras");
    }

    @Test
    @DisplayName("o save antigo nao atropela um espaco 1 ja ocupado")
    void migracaoNaoAtropela() throws IOException {
        escrever(arquivo(1), valido(9));
        escrever(LEGADO, valido(5));

        assertEquals(9, Corrida.espiar(1).nivel(), "o espaco 1 ja tinha dono");
        assertFalse(Files.exists(LEGADO));
    }

    // ------------------------------------------------------- ler sem consumir

    @Test
    @DisplayName("espiar NAO apaga — o dialogo pode ser cancelado")
    void espiarNaoApaga() throws IOException {
        escrever(arquivo(2), valido(7));

        Corrida espiada = Corrida.espiar(2);
        assertNotNull(espiada);
        assertEquals(7, espiada.nivel());
        assertTrue(Files.exists(arquivo(2)),
                "espiar so descreve o save; quem responde 'no' fica com a corrida intacta");

        Corrida consumida = Corrida.consumir(2);
        assertNotNull(consumida);
        assertEquals(7, consumida.nivel());
        assertFalse(Files.exists(arquivo(2)));
    }

    @Test
    @DisplayName("espiar um arquivo quebrado devolve nulo sem apagar")
    void espiarQuebrado() throws IOException {
        escrever(arquivo(1), "{nao e json");

        assertNull(Corrida.espiar(1));
        assertTrue(Files.exists(arquivo(1)), "quem apaga e o consumir, e so ele");
    }

    // ------------------------------------------------------- arquivo estragado

    @Test
    @DisplayName("formato desconhecido e recusado, e o arquivo some junto")
    void versaoDeOutroFormato() throws IOException {
        escrever(arquivo(1), valido(7).replace("\"versao\":1", "\"versao\":99"));

        assertNull(Corrida.consumir(1), "save de outro formato nao pode ser aplicado");
        assertFalse(Files.exists(arquivo(1)),
                "recusar sem apagar deixaria o espaco quebrado para sempre");
    }

    @Test
    @DisplayName("arquivo corrompido nao derruba o jogo")
    void arquivoQuebrado() throws IOException {
        escrever(arquivo(3), "{isto nao e json, o processo morreu no meio da gravacao");

        assertNull(Corrida.consumir(3));
        assertFalse(Files.exists(arquivo(3)));
    }

    @Test
    @DisplayName("o BOM do Bloco de Notas nao invalida o save")
    void comBom() throws IOException {
        escrever(arquivo(1), "﻿" + valido(7));

        Corrida corrida = Corrida.consumir(1);
        assertNotNull(corrida, "o caractere invisivel do UTF-8 com BOM nao pode barrar a leitura");
        assertEquals(7, corrida.nivel());
    }

    @Test
    @DisplayName("campo faltando cai no padrao em vez de quebrar")
    void campoAusente() throws IOException {
        escrever(arquivo(1), "{\"versao\":1,\"gato\":1,\"nivel\":4}");

        Corrida corrida = Corrida.consumir(1);
        assertNotNull(corrida);
        assertEquals(1, corrida.gato());
        assertEquals(4, corrida.nivel());
        assertEquals(0, corrida.dificuldade(), "sem o campo, a dificuldade volta ao padrao");
    }

    @Test
    @DisplayName("apagar funciona mesmo sem arquivo")
    void apagarSemArquivo() {
        Corrida.apagar(1);
        assertFalse(Corrida.existe(1));
    }
}
