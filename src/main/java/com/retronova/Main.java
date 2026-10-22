package com.retronova;

import com.retronova.engine.Engine;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // RESOLUÇÃO EM PIXELS DA TELA, EM TODO SISTEMA. Desde o Java 9 o Java2D
        // aplica sozinho a escala de DPI do sistema (JEP 263, Windows e Linux): a
        // 125%, uma janela "1280" ocuparia 1600 pixels, e o pixel art seria
        // reamostrado. Com 1.0 a resolução escolhida é a da tela, e a escala é
        // inteira e do jogo. Era só no Windows; o Linux com GDK_SCALE tinha o mesmo
        // problema. O macOS desenha sempre na escala da tela e ignora a opção.
        // Quem quiser outra escala passa -Dsun.java2d.uiScale na linha de comando.
        if (System.getProperty("sun.java2d.uiScale") == null) {
            System.setProperty("sun.java2d.uiScale", "1.0");
        }
        // OPENGL AQUI, ANTES DE QUALQUER COISA DO AWT. A Window ligava esta mesma
        // propriedade, mas depois de o Java2D ja ter escolhido o pipeline — e ele
        // escolhe uma vez so. Resultado: o jogo inteiro desenhava em software.
        // Medido nesta maquina, ampliar a tela com suavizacao custava 30 ms em
        // software e 0,3 ms em OpenGL. Quem precisar desligar (driver ruim) passa
        // -Dsun.java2d.opengl=false na linha de comando, que tem precedencia.
        if (System.getProperty("sun.java2d.opengl") == null) {
            System.setProperty("sun.java2d.opengl", "true");
        }
        SwingUtilities.invokeLater(Engine::new);
    }
}
