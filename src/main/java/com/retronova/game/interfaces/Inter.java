package com.retronova.game.interfaces;

import com.retronova.engine.graphics.Palette;
import com.retronova.engine.Activity;
import com.retronova.engine.Configs;
import com.retronova.engine.Engine;
import com.retronova.engine.graphics.FontHandler;
import com.retronova.engine.inputs.gamepad.Gamepad;
import com.retronova.engine.inputs.keyboard.KeyBoard;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Inter implements Activity {


    private Rectangle[] tabs;
    private final Map<String, Activity> inter;
    private final List<String> temp;
    private String index;

    public Inter() {
        this.inter = new TreeMap<>();
        this.temp = new ArrayList<>();
        this.index = "";
    }

    public void open() {
        if(this.inter.isEmpty()) {
            return;
        }
        if(index.isBlank()) {
            this.index = keys()[0];
        }
        Engine.pause(this);
    }

    public void open(String index) {
        this.entrada = null;
        this.index = index;
        Engine.pause(this);
    }

    /**
     * Abre deslizando de lado (ver EntradaLateral). Enquanto a tela entra, nenhum
     * aperto conta: o Enter que encerrou a conversa do vendedor não compra nada.
     */
    public void open(String index, com.retronova.menus.shared.EntradaLateral entrada) {
        open(index);
        this.entrada = entrada;
    }

    private com.retronova.menus.shared.EntradaLateral entrada;

    public void close() {
        Engine.pause(null);
        closeTemps();
        for(String key : keys()) {
            this.inter.get(key).dispose();
        }
    }

    private void closeTemps() {
        for(int i = 0; i < this.temp.size(); i++) {
            String tempKey = this.temp.get(i);
            this.remove(tempKey);
            this.temp.remove(this.temp.get(i));
            this.index = "";
        }
    }

    private String[] keys() {
        return inter.keySet().toArray(new String[0]);
    }

    public void put(String interName, Activity inter, boolean temp) {
        if(!this.inter.containsKey(interName)) {
            this.inter.put(interName, inter);
            this.tabs = tabs();
            if(temp && !this.temp.contains(interName)) {
                this.temp.add(interName);
            }
        }
    }

    public void remove(String interName) {
        if(index.equals(interName)) {
            this.index = keys()[0];
        }
        this.inter.get(interName).dispose();
        this.inter.remove(interName);
        this.tabs = tabs();
        refreshPositions();
    }

    private void refreshPositions() {
        // Sem inventário e status fixos (desativados, ver Game), a Inter fica vazia
        // quando a loja fecha: não há aba para posicionar.
        if (this.tabs == null || this.tabs.length == 0) {
            return;
        }
        String[] keys = keys();
        Rectangle rec = this.tabs[0];
        int width = rec.width * keys.length;
        int x = Engine.window.largura()/2 - width/2;
        int y = Configs.Margin();
        for(int i = 0; i < keys.length; i++) {
            tabs[i].setLocation(x + (rec.width+Configs.HudScale()*2)*i, y);
        }
    }

    private Rectangle[] tabs() {
        String[] keys = keys();
        int scale = Configs.HudScale();
        Rectangle rec = new Rectangle(48*scale, 12*scale);
        Rectangle[] tabs = new Rectangle[keys.length];
        for(int i = 0; i < keys.length; i++) {
            tabs[i] = new Rectangle(rec.width, rec.height);
        }
        return tabs;
    }
    
    @Override
    public void tick() {
        if (entrada != null && entrada.ativa()) {
            entrada.tick();
            refreshPositions();
            // Descarta o que for apertado no deslize: o clique do mouse fica preso
            // ate ser lido, e compraria um item assim que a loja assentasse.
            Mouse.click(Mouse_Button.LEFT);
            KeyBoard.KeyPressed("Enter");
            return;
        }
        if(KeyBoard.KeyPressed("E") || KeyBoard.KeyPressed("Escape") ) {
            close();
        }
        refreshPositions();
        trocarAbaPeloControle();
        for(int i = 0; i < tabs.length; i++) {
            Rectangle rec = tabs[i];
            // As abas — inventario, status, loja — sao o primeiro lugar onde o
            // jogador tenta clicar, e eram justamente as que nao avisavam que
            // eram clicaveis. A patinha de ponteiro cobre o mouse aqui.
            if(Mouse.on(rec)) {
                Engine.window.pointing();
            }
            if(Mouse.clickOn(Mouse_Button.LEFT, rec)) {
                this.index = keys()[i];
            }
        }
        if(inter.containsKey(index))
            inter.get(index).tick();
    }

    /** LB/RB passam de aba — inventario, status, loja —, como nas Opcoes. */
    private void trocarAbaPeloControle() {
        int passo = Gamepad.pressionou(Gamepad.Botao.RB) ? 1
                : Gamepad.pressionou(Gamepad.Botao.LB) ? -1 : 0;
        if (passo == 0) {
            return;
        }
        String[] keys = keys();
        int atual = Math.max(0, Arrays.asList(keys).indexOf(index));
        this.index = keys[(atual + passo + keys.length) % keys.length];
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(VEU);
        g.fillRect(0, 0, Engine.window.largura(), Engine.window.altura());
        Graphics2D c = g;
        if (entrada != null && entrada.ativa()) {
            c = (Graphics2D) g.create();
            entrada.aplicar(c, Engine.alpha());
        }
        if(inter.containsKey(index))
            inter.get(index).render(c);
        renderTabs(c);
        if (c != g) {
            c.dispose();
        }
    }

    private static final Color VEU = new Color(0, 0, 0, 80);

    /**
     * As abas no mesmo desenho das abas das Opções (ver menus.shared.Abas): a
     * ativa é preenchida e tem o friso claro em cima; a de baixo do mouse ganha o
     * realce comum (ver Realce). Antes a ativa era marcada por uma borda grossa,
     * que era o mesmo contorno que o jogo usava para hover em outros lugares.
     */
    private void renderTabs(Graphics2D g) {
        int s = Configs.HudScale();
        Font font = FontHandler.font(FontHandler.Game, s * 6);
        g.setFont(font);
        for(int i = 0; i < tabs.length; i++) {
            Rectangle rec = tabs[i];
            String name = keys()[i];
            boolean ativa = index.equals(name);
            boolean sob = !ativa && com.retronova.engine.inputs.Entrada.porMouse() && Mouse.on(rec);
            if (sob) {
                com.retronova.menus.shared.Realce.brilho(g, rec, 1f, s);
            }
            g.setColor(Palette.OUTLINE);
            g.fillRect(rec.x, rec.y, rec.width, rec.height);
            g.setColor(ativa ? Palette.MAIN : Palette.DEEP);
            g.fillRect(rec.x + s, rec.y + s, rec.width - 2 * s, rec.height - 2 * s);
            if (ativa) {
                g.setColor(Palette.LIGHT);
                g.fillRect(rec.x + s, rec.y + s, rec.width - 2 * s, s);
            }
            if (sob) {
                com.retronova.menus.shared.Realce.clarear(g, rec, 1f);
            }
            int wf = FontHandler.getWidth(name, font);
            int hf = FontHandler.getHeight(name, font);
            g.setColor(Palette.OUTLINE);
            g.drawString(name, rec.x + rec.width/2 - wf/2 + s, rec.y + rec.height/2 + hf/2 + s);
            g.setColor(ativa ? Palette.TEXT : Palette.LIGHT);
            g.drawString(name, rec.x + rec.width/2 - wf/2, rec.y + rec.height/2 + hf/2);
        }
    }


    @Override
    public void dispose() {
        this.inter.clear();
    }

}
