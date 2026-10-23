package com.retronova.engine;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.platform.win32.WinUser.KEYBDINPUT;

import javax.swing.JFrame;
import java.awt.event.KeyEvent;

/**
 * ATIVAÇÃO NATIVA NA ABERTURA. Elevar o JFrame não garante que ele seja a janela
 * ativa do Windows: a barra continua acima até o primeiro clique. O Java pode
 * inclusive informar foco enquanto GetForegroundWindow aponta para outro app.
 *
 * Primeiro pede SetForegroundWindow. Se o Windows recusar, usa uma única
 * passagem de Alt pressionado/solto para liberar a ativação, como a biblioteca
 * Tao faz na criação da janela (windows/window.rs, force_window_active).
 * Não envia combinações nem altera configurações globais do sistema; se um
 * modificador já estiver pressionado, não simula Alt. Nunca repetir ao perder
 * foco: Alt+Tab depois da abertura é uma escolha do jogador.
 */
final class FocoDoWindows {
    private static final int VK_LWIN = 0x5B, VK_RWIN = 0x5C;

    private FocoDoWindows() { }

    static void ativarAoIniciar(JFrame frame) {
        if (!Platform.isWindows() || !frame.isShowing() || !frame.isFocusableWindow()) {
            return;
        }
        try {
            HWND janela = new HWND(Native.getComponentPointer(frame));
            if (janela.equals(User32.INSTANCE.GetForegroundWindow())) {
                return;
            }
            boolean aceitou = User32.INSTANCE.SetForegroundWindow(janela);
            boolean usouAlt = false;
            if (!aceitou && !modificadorPressionado()) {
                INPUT[] eventos = (INPUT[]) new INPUT().toArray(2);
                for (int i = 0; i < eventos.length; i++) {
                    eventos[i].type = new DWORD(INPUT.INPUT_KEYBOARD);
                    eventos[i].input.setType(KEYBDINPUT.class);
                    eventos[i].input.ki.wVk = new WORD(KeyEvent.VK_ALT);
                    eventos[i].input.ki.dwFlags = new DWORD(i == 0 ? 0 : KEYBDINPUT.KEYEVENTF_KEYUP);
                }
                int enviados = User32.INSTANCE.SendInput(new DWORD(eventos.length), eventos, eventos[0].size()).intValue();
                if (enviados == eventos.length) {
                    usouAlt = true;
                    aceitou = User32.INSTANCE.SetForegroundWindow(janela);
                } else if (enviados == 1) {
                    // Se só o pressionamento entrou, garante a tentativa de soltar.
                    User32.INSTANCE.SendInput(new DWORD(1), new INPUT[]{eventos[1]}, eventos[1].size());
                }
            }
            if (Video.LIGADO) {
                System.out.println("[video] ativação inicial: pedido aceito=" + aceitou + ", desbloqueio Alt=" + usouAlt);
            }
        } catch (RuntimeException | LinkageError falha) {
            System.err.println("Janela: falha ao pedir ativação no Windows: " + falha);
        }
    }

    private static boolean modificadorPressionado() {
        for (int tecla : new int[]{KeyEvent.VK_ALT, KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, VK_LWIN, VK_RWIN}) {
            if (User32.INSTANCE.GetAsyncKeyState(tecla) < 0) {
                return true;
            }
        }
        return false;
    }

    static boolean emPrimeiroPlano(JFrame frame) {
        return Platform.isWindows() && frame.isDisplayable()
                && new HWND(Native.getComponentPointer(frame)).equals(User32.INSTANCE.GetForegroundWindow());
    }
}
