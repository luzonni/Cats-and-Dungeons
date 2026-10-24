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

    @org.junit.jupiter.api.io.TempDir
    Path pasta;

    private Path arquivo(int espaco) {
        return pasta.resolve("corrida-" + espaco + ".json");
    }

    private Path LEGADO;

    @BeforeEach
    void isolarSaves() {
        Corrida.usarPasta(pasta);
        LEGADO = pasta.resolve("corrida.json");
    }

    @AfterEach
    void restaurarPasta() {
        Corrida.usarPasta(Paths.get(com.retronova.engine.Foto.pastaDeSaves()));
    }
    private static void escrever(Path onde, String json) throws IOException {
        if (onde.getParent() != null) {
            Files.createDirectories(onde.getParent());
        }
        Files.writeString(onde, json, StandardCharsets.UTF_8);
    }

    @Test
    void recuperaBackupDaCorridaSemRessuscitarDepoisDeConsumir() throws Exception {
        escrever(arquivo(1), "{quebrado");
        escrever(arquivo(1).resolveSibling("corrida-1.json.bak"), valido(9));
        Corrida recuperada = Corrida.consumir(1);
        assertNotNull(recuperada);
        assertEquals(9, recuperada.nivelDoGato());
        assertNull(Corrida.espiar(1));
        assertTrue(Files.exists(arquivo(1).resolveSibling("corrida-1.json.corrompido")));
    }

    @Test
    void idLegadoPermaneceIgualEmLeiturasSeparadas() throws Exception {
        escrever(arquivo(1), valido(4));
        assertEquals(Corrida.espiar(1).id(), Corrida.espiar(1).id());
    }

    /** Um save valido, com o nivel do gato servindo de marca para saber de qual espaco veio. */
    private static String valido(int nivelDoGato) {
        return "{"
                + "\"versao\":1,"
                + "\"gato\":2,"
                + "\"segundos\":423,"
                + "\"vida\":18.5,"
                + "\"xp\":140.25,"
                + "\"nivelDoGato\":" + nivelDoGato + ","
                + "\"dinheiro\":260,"
                + "\"hotbar\":[[0,1,1],[2,5,3]],"
                + "\"mochila\":[[4,9,1]],"
                + "\"passivos\":[],"
                + "\"andamento\":{"
                + "\"ticks\":72000,\"abates\":1840,\"proximoChefe\":5,\"reiNasceu\":false,"
                + "\"ateOCerco\":900,"
                + "\"chefes\":[{\"tipo\":\"CatToyBoss\",\"vida\":0.4},{\"tipo\":\"NaoExiste\",\"vida\":1}],"
                + "\"niveisPendentes\":1,\"trocas\":1,\"pulos\":0,\"furia\":0.5,"
                + "\"baus\":6,\"maiorHorda\":120,\"venceu\":false,"
                + "\"danoPorFonte\":{\"Hand weapon\":5000.5}"
                + "}"
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
        assertEquals(7, corrida.nivelDoGato());
        assertEquals(423, corrida.segundos());
    }

    // ------------------------------------------------- o andamento da corrida

    @Test
    @DisplayName("o relogio da corrida volta de onde parou")
    void leOAndamento() throws IOException {
        escrever(arquivo(1), valido(7));

        Corrida.Andamento a = Corrida.consumir(1).andamento();
        assertEquals(72000, a.relogio().ticks(), "sem o relogio a corrida recomecaria do minuto zero");
        assertEquals(1840, a.relogio().abates());
        assertEquals(5, a.relogio().proximoChefe(), "chefe que ja nasceu nao pode nascer de novo");
        assertFalse(a.relogio().reiNasceu());
        assertEquals(900, a.relogio().ateOCerco());
        assertEquals(1, a.niveisPendentes());
        assertEquals(1, a.trocas());
        assertEquals(0, a.pulos());
        assertEquals(0.5, a.furia(), 1e-9);
        assertEquals(6, a.baus());
        assertEquals(120, a.maiorHorda());
        assertEquals(5000.5, a.danoPorFonte().get("Hand weapon"), 1e-9);
    }

    @Test
    @DisplayName("o chefe vivo volta pelo nome, e nome desconhecido e ignorado")
    void leOsChefes() throws IOException {
        escrever(arquivo(1), valido(7));

        var chefes = Corrida.consumir(1).andamento().chefes();
        assertEquals(1, chefes.size(), "um tipo que saiu do jogo nao pode derrubar a retomada");
        assertEquals(com.retronova.game.objects.entities.EntityIDs.CatToyBoss, chefes.getFirst().tipo());
        assertEquals(0.4, chefes.getFirst().fracaoDeVida(), 1e-9);
    }

    @Test
    @DisplayName("o tempo mostrado e o do relogio da corrida")
    void tempoDoRelogio() throws IOException {
        escrever(arquivo(1), valido(7));

        assertEquals("20:00", Corrida.espiar(1).tempo(), "72000 ticks sao vinte minutos");
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

        assertEquals(1, Corrida.espiar(1).nivelDoGato());
        assertEquals(2, Corrida.espiar(2).nivelDoGato());
        assertEquals(3, Corrida.espiar(3).nivelDoGato());
    }

    @Test
    @DisplayName("retomar um espaco nao toca nos outros")
    void consumirNaoVazaEntreEspacos() throws IOException {
        escrever(arquivo(1), valido(1));
        escrever(arquivo(2), valido(2));
        escrever(arquivo(3), valido(3));

        assertEquals(2, Corrida.consumir(2).nivelDoGato());

        assertFalse(Corrida.existe(2));
        assertTrue(Corrida.existe(1), "comecar ou retomar uma corrida nao custa as outras");
        assertTrue(Corrida.existe(3));
        assertEquals(1, Corrida.espiar(1).nivelDoGato());
        assertEquals(3, Corrida.espiar(3).nivelDoGato());
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
        assertEquals(5, Corrida.espiar(1).nivelDoGato());
        assertFalse(Files.exists(LEGADO), "migrado uma vez, o arquivo antigo nao fica para tras");
    }

    @Test
    @DisplayName("o save antigo nao atropela um espaco 1 ja ocupado")
    void migracaoNaoAtropela() throws IOException {
        escrever(arquivo(1), valido(9));
        escrever(LEGADO, valido(5));

        assertEquals(9, Corrida.espiar(1).nivelDoGato(), "o espaco 1 ja tinha dono");
        assertFalse(Files.exists(LEGADO));
    }

    // ------------------------------------------------------- ler sem consumir

    @Test
    @DisplayName("espiar NAO apaga — o dialogo pode ser cancelado")
    void espiarNaoApaga() throws IOException {
        escrever(arquivo(2), valido(7));

        Corrida espiada = Corrida.espiar(2);
        assertNotNull(espiada);
        assertEquals(7, espiada.nivelDoGato());
        assertTrue(Files.exists(arquivo(2)),
                "espiar so descreve o save; quem responde 'no' fica com a corrida intacta");

        Corrida consumida = Corrida.consumir(2);
        assertNotNull(consumida);
        assertEquals(7, consumida.nivelDoGato());
        assertFalse(Files.exists(arquivo(2)));
    }

    @Test
    @DisplayName("espiar um arquivo quebrado devolve nulo sem apagar")
    void espiarQuebrado() throws IOException {
        escrever(arquivo(1), "{nao e json");

        assertNull(Corrida.espiar(1));
        assertTrue(Files.exists(arquivo(1).resolveSibling("corrida-1.json.corrompido")));
    }

    // ------------------------------------------------------- arquivo estragado

    @Test
    @DisplayName("formato desconhecido e recusado, e o arquivo some junto")
    void versaoDeOutroFormato() throws IOException {
        escrever(arquivo(1), valido(7).replace("\"versao\":1", "\"versao\":99"));

        org.junit.jupiter.api.Assertions.assertThrows(com.retronova.game.save.ArquivoSeguro.VersaoIncompativel.class, () -> Corrida.consumir(1));
        assertTrue(Files.exists(arquivo(1)),
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
        assertEquals(7, corrida.nivelDoGato());
    }

    @Test
    @DisplayName("campo faltando cai no padrao em vez de quebrar")
    void campoAusente() throws IOException {
        // O save antigo, do tempo das salas: "nivel" e "dificuldade" sao ignorados,
        // e sem "andamento" a corrida volta como recem-comecada.
        escrever(arquivo(1), "{\"versao\":1,\"gato\":1,\"nivel\":4,\"dificuldade\":2,\"nivelDoGato\":4}");

        Corrida corrida = Corrida.consumir(1);
        assertNotNull(corrida);
        assertEquals(1, corrida.gato());
        assertEquals(4, corrida.nivelDoGato());
        assertEquals(0, corrida.andamento().relogio().ticks(), "sem andamento, o relogio comeca do zero");
        assertEquals(1, corrida.andamento().relogio().proximoChefe());
        assertTrue(corrida.andamento().chefes().isEmpty());
        assertEquals(-1, corrida.andamento().trocas(), "ausente diz 'nao sei', e nao 'nenhuma'");
    }

    @Test
    @DisplayName("apagar funciona mesmo sem arquivo")
    void apagarSemArquivo() {
        Corrida.apagar(1);
        assertFalse(Corrida.existe(1));
    }
}
