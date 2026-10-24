package com.retronova.engine;

import com.retronova.game.Corrida;
import com.retronova.game.Game;
import com.retronova.game.espelho.Espelho;
import com.retronova.game.items.*;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.arena.SubirDeNivel;
import com.retronova.game.objects.entities.*;
import com.retronova.game.save.Progresso;
import com.retronova.menus.*;
import com.retronova.menus.shared.TelaDeMenu;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Bancada automatizada com objetos reais; Foto isola saves, configuração, áudio e foco. */
public final class VerificarProgressao {
    private static void igual(double esperado, double atual, String motivo) {
        if (Math.abs(esperado - atual) > 0.000001) {
            throw new AssertionError(motivo + ": " + esperado + " != " + atual);
        }
    }

    private static void foto(TelaDeMenu tela, String nome) throws Exception {
        TelaDeMenu.abrir(tela);
        for (int i = 0; i < 70; i++) tela.tick();
        BufferedImage img = new BufferedImage(Engine.window.largura(), Engine.window.altura(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Engine.desenharTudo(g);
        g.dispose();
        ImageIO.write(img, "png", Path.of(nome + ".png").toFile());
        Engine.pause(null);
    }

    private static void verificar() throws Exception {
        Game jogo = Game.getGame();
        Player gato = Game.getPlayer();
        double vidaBase = Player.TEMPLATES[0].getLifeSize();
        double danoBase = Player.TEMPLATES[0].getDamage();
        Progresso.creditar("saldo-de-teste", "Muffin", 10000);
        igual(10000, com.retronova.game.dialogos.Dialogos.contexto().moedas(), "dialogo usa saldo permanente");
        for (var m : Espelho.tabela()) {
            if (Espelho.comprar(gato, m) != Espelho.Compra.FEITA) throw new AssertionError(m.id());
        }
        igual(vidaBase + 6, gato.getLifeSize(), "vida do espelho");
        igual(danoBase + 1, gato.getDamage(), "dano do espelho");
        igual(vidaBase, Player.TEMPLATES[0].getLifeSize(), "template imutavel");
        igual(0, Progresso.moedas("Finn"), "saldo por gato");
        igual(1, Espelho.cartasExtras(gato), "opcao extra");
        igual(1.1, Espelho.ganancia("Muffin"), "ganho extra");
        int trocasComEspelho = SubirDeNivel.trocas();
        gato.getMelhorias().aplicar(gato, Melhoria.FURIA, Raridade.COMUM);
        igual(danoBase + 4, gato.getDamage(), "base + permanente + carta");
        foto(new TelaDoEspelho(gato), "espelho");
        foto(new TelaDeStatus(), "status");

        // Salva/retoma em espaço isolado e verifica a composição real dos bônus.
        var espaco = Game.class.getDeclaredField("espaco");
        espaco.setAccessible(true);
        espaco.setInt(jogo, 1);
        Corrida.gravar();
        Corrida corrida = Corrida.espiar(1);
        if (corrida == null) throw new AssertionError("checkpoint ausente");
        Player retomado = Player.newPlayer(0);
        Bonus.aplicarNoInicio(retomado);
        corrida.aplicarNoGato(retomado);
        igual(danoBase + 4, retomado.getDamage(), "retomada sem duplicacao");
        igual(vidaBase + 6, retomado.getLifeSize(), "vida retomada");

        Espelho.redistribuir(gato);
        igual(danoBase + 3, gato.getDamage(), "reembolso preserva carta");
        igual(vidaBase, gato.getLifeSize(), "reembolso remove so espelho");
        igual(10000, Progresso.moedas("Muffin"), "reembolso integral");
        if (SubirDeNivel.trocas() >= trocasComEspelho) throw new AssertionError("reroll nao removido");
        Espelho.comprar(gato, Espelho.porId("rerolls"));
        Bonus.aplicarNoInicio(Player.newPlayer(2));
        igual(trocasComEspelho - 1, SubirDeNivel.trocas(), "troca de gato limpa reroll");

        for (var a : ArmaExtra.values()) gato.getMelhorias().subirArma(a);
        for (int i = 0; i < 2000; i++) {
            if (RecompensaDoBau.sortear(gato.getMelhorias(), 0).arma() != null) throw new AssertionError("arma em standby oferecida");
        }
        foto(new TelaDoEspelho(gato), "espelho-reembolso");
        int antesDaMorte = Progresso.moedas("Muffin");
        gato.die();
        for (int i = 0; i < 190 && Engine.getACTIVITY() == jogo; i++) jogo.tick();
        if (!(Engine.getACTIVITY() instanceof GameOver fim)) throw new AssertionError("sem Run Over");
        for (int i = 0; i < 75; i++) fim.tick();
        BufferedImage img = new BufferedImage(Engine.window.largura(), Engine.window.altura(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics(); Engine.desenharTudo(g); g.dispose();
        ImageIO.write(img, "png", Path.of("run-over.png").toFile());
        if (Progresso.moedas("Muffin") < antesDaMorte) throw new AssertionError("morte perdeu moedas");
        if (Corrida.espiar(1) != null) throw new AssertionError("morte deixou run retomavel");
        // A conversa de verdade entrega a mesma loja; pular não reativa a vitrine antiga.
        Game hub = new Game(0, new com.retronova.game.map.room.Room("beginning"), 0);
        Engine.heapActivity(hub);
        var lojista = Game.getMap().getEntities(com.retronova.game.objects.entities.NPCs.Seller.class).getFirst();
        if (!com.retronova.game.dialogos.Dialogos.conversar("vendedor", lojista)) throw new AssertionError("sem conversa");
        var conversa = Engine.sobreposta();
        for (int i = 0; i < 35; i++) conversa.tick();
        BufferedImage dialogo = new BufferedImage(Engine.window.largura(), Engine.window.altura(), BufferedImage.TYPE_INT_RGB);
        Graphics2D dg = dialogo.createGraphics(); Engine.desenharTudo(dg); dg.dispose();
        ImageIO.write(dialogo, "png", Path.of("dialogo-vendedor.png").toFile());
        com.retronova.engine.inputs.keyboard.KeyBoard.simular("Escape", true);
        conversa.tick();
        com.retronova.engine.inputs.keyboard.KeyBoard.simular("Escape", false);
        for (int i = 0; i < 30 && Engine.sobreposta() == conversa; i++) conversa.tick();
        if (!(Engine.sobreposta() instanceof TelaDoEspelho)) throw new AssertionError("dialogo nao abriu espelho");
        for (int i = 0; i < 35; i++) Engine.sobreposta().tick();
        System.out.println("[ok] conversa do vendedor -> espelho; saldo permanente e entrada protegida");
        System.out.println("[ok] atributos reais, templates, compra, refund, gatos, rerolls, retomada, standby e morte");
    }

    public static void main(String[] args) {
        if (Files.exists(Path.of("saves"))) throw new IllegalStateException("Use uma pasta temporaria vazia");
        System.setProperty("cd.foto", "capturas");
        System.setProperty("cd.foto.roteiro", "espera:9000");
        System.setProperty("cd.foto.tamanho", args.length == 0 ? "1920x1080" : args[0]);
        System.setProperty("cd.bench", "true");
        System.setProperty("cd.gato", "0");
        System.setProperty("sun.java2d.uiScale", "1.0");
        SwingUtilities.invokeLater(() -> {
            new Engine();
            Game.depoisDeUmSegundo = () -> {
                try { verificar(); System.exit(0); }
                catch (Throwable falha) { falha.printStackTrace(); System.exit(1); }
            };
        });
    }
}
