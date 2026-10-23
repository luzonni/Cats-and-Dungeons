package com.retronova.engine.inputs;

import com.retronova.engine.inputs.gamepad.Comando;
import com.retronova.engine.inputs.gamepad.Gamepad;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O catálogo de ações contra o código: a tela de Controles não pode ficar para
 * trás do que o jogo realmente lê.
 */
class AcaoTest {

    /**
     * Teclas lidas no código que não aparecem na tela, e por quê. O Shift é o
     * Shift+clique do inventário, que perdeu a porta de entrada (ver Game: o E
     * abre o Status) e não é alcançável numa corrida.
     */
    private static final Set<String> FORA_DA_TELA = Set.of("shift");

    private static final Pattern LEITURA = Pattern.compile("KeyPress(?:ed|ing)\\(\"([^\"]+)\"\\)");
    private static final Pattern BOTAO_DIRETO = Pattern.compile("Gamepad\\.Botao\\.([A-Z0-9_]+)");

    private static Set<String> teclasDoCatalogo() {
        Set<String> todas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Acao a : Acao.values()) {
            todas.addAll(a.teclas());
        }
        return todas;
    }

    private static List<String> fontes() throws IOException {
        List<String> textos = new ArrayList<>();
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main/java"))) {
            for (Path p : arquivos.filter(f -> f.toString().endsWith(".java")).toList()) {
                textos.add(Files.readString(p));
            }
        }
        return textos;
    }

    @Test
    @DisplayName("toda tecla que o código lê por nome está em alguma ação")
    void codigoCobertoPeloCatalogo() throws IOException {
        Set<String> catalogo = teclasDoCatalogo();
        Set<String> faltando = new TreeSet<>();
        for (String fonte : fontes()) {
            Matcher m = LEITURA.matcher(fonte);
            while (m.find()) {
                String tecla = m.group(1);
                if (!catalogo.contains(tecla) && !FORA_DA_TELA.contains(tecla.toLowerCase())) {
                    faltando.add(tecla);
                }
            }
        }
        assertTrue(faltando.isEmpty(), "teclas lidas pelo jogo e ausentes da tela de Controles (Acao): " + faltando);
    }

    @Test
    @DisplayName("todo botão que o código lê direto do controle está em alguma ação")
    void botoesDiretosCobertos() throws IOException {
        Set<Comando> doCatalogo = EnumSet.noneOf(Comando.class);
        for (Acao a : Acao.values()) {
            doCatalogo.addAll(a.comandos());
        }
        Set<String> faltando = new TreeSet<>();
        for (String fonte : fontes()) {
            Matcher m = BOTAO_DIRETO.matcher(fonte);
            while (m.find()) {
                Comando c = Comando.de(Gamepad.Botao.valueOf(m.group(1)));
                if (!doCatalogo.contains(c)) {
                    faltando.add(m.group(1));
                }
            }
        }
        assertTrue(faltando.isEmpty(), "botões lidos direto e ausentes da tela de Controles: " + faltando);
    }

    @Test
    @DisplayName("toda tecla das ações existe no teclado")
    void teclasExistem() {
        for (Acao a : Acao.values()) {
            for (String t : a.teclas()) {
                boolean existe = false;
                for (KeyBoard.Key k : KeyBoard.Keys) {
                    existe |= k.getName().equalsIgnoreCase(t);
                }
                assertTrue(existe, a + " lê \"" + t + "\", que não está em KeyBoard.Keys");
            }
        }
    }

    @Test
    @DisplayName("toda tecla que o controle aperta é lida por alguma ação")
    void controleCoberto() {
        Set<String> catalogo = teclasDoCatalogo();
        for (String t : Gamepad.teclasDoControle()) {
            assertTrue(catalogo.contains(t), "o controle aperta \"" + t + "\", que nenhuma ação lê");
        }
    }

    @Test
    @DisplayName("os botões saem do mapa do controle, com o B dependendo de onde se está")
    void derivacaoDosBotoes() {
        assertEquals(EnumSet.of(Comando.Y), Acao.STATUS.comandos());
        assertEquals(EnumSet.of(Comando.X), Acao.USAR_ITEM.comandos());
        assertEquals(EnumSet.of(Comando.VIEW), Acao.LARGAR_ITEM.comandos());
        assertEquals(EnumSet.of(Comando.LT, Comando.RT), Acao.DASH.comandos());
        assertEquals(EnumSet.of(Comando.MENU), Acao.PAUSAR.comandos(), "em jogo, o B não pausa");
        assertEquals(EnumSet.of(Comando.B), Acao.INTERAGIR.comandos(), "em jogo, o B interage");
        assertEquals(EnumSet.of(Comando.B, Comando.MENU), Acao.VOLTAR.comandos(), "nas telas, o B volta");
        assertEquals(EnumSet.of(Comando.A), Acao.SELECIONAR.comandos());
        assertEquals(EnumSet.of(Comando.DPAD, Comando.ANALOGICO_ESQ), Acao.MOVER.comandos());
        assertEquals(EnumSet.of(Comando.LB, Comando.RB), Acao.ABAS.comandos(), "as abas leem LB/RB direto");
        assertTrue(Acao.ESCOLHER_CARTA.comandos().isEmpty(), "as cartas por número são só do teclado");
    }

    @Test
    @DisplayName("ações só de um aparelho: olhar só no controle, cartas por número só no teclado")
    void soDeUmAparelho() {
        assertTrue(!Acao.OLHAR.noTeclado());
        assertEquals(EnumSet.of(Comando.ANALOGICO_DIR), Acao.OLHAR.comandos());
        assertTrue(Acao.ESCOLHER_CARTA.noTeclado());
        assertTrue(Acao.INTERAGIR.codigosDeTecla().isEmpty(), "no teclado, interagir é o clique");
        assertEquals(List.of(Acao.Mouse.ESQUERDO), Acao.INTERAGIR.mouse());
    }
}
