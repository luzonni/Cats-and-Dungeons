package com.retronova.menus;

import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.menus.shared.Button;
import com.retronova.menus.shared.TelaDeMenu;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/** FALHA RECUPERÁVEL: manter a operação e permitir tentar novamente após liberar o disco. */
public final class FalhaDeSave extends TelaDeMenu {
    private static final int LARGURA = 180, ALTURA = 28;
    private final Button repetir;

    public static void tentar(Runnable operacao) {
        try {
            operacao.run();
        } catch (IllegalStateException | java.io.UncheckedIOException falha) {
            System.err.println("[save] operacao pendente: " + falha);
            abrir(new FalhaDeSave(operacao));
        }
    }

    private FalhaDeSave(Runnable operacao) {
        super("Could not save - check disk access");
        repetir = new Button(0, 0, 0, 0, "Retry saving", b -> {
            voltar();
            tentar(operacao);
        }).primary();
    }

    @Override protected boolean podeVoltar() { return false; }

    @Override protected void posicionar() {
        Rectangle area = areaDoConteudo();
        int s = com.retronova.engine.Configs.UiScale();
        repetir.setBounds(new Rectangle(area.x + (area.width - LARGURA * s) / 2,
                area.y + (area.height - ALTURA * s) / 2, LARGURA * s, ALTURA * s));
    }

    @Override protected void atualizar() {
        repetir.setFocused(true);
        repetir.tick();
        if (KeyBoard.KeyPressed("Enter")) repetir.activate();
    }

    @Override protected void desenharConteudo(Graphics2D g) { repetir.render(g); }
}
