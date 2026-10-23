package com.retronova.engine;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.ptr.PointerByReference;

import javax.swing.JFrame;

/**
 * TELA CHEIA PARA O SHELL, além do tamanho da janela. A detecção automática do
 * Windows pode deixar a barra de tarefas acima de um JFrame sem bordas, mesmo
 * com o tamanho e o foco corretos. ITaskbarList2.MarkFullscreenWindow informa
 * explicitamente o modo; o Shell mantém a barra atrás só enquanto o jogo está
 * ativo, sem esconder a barra do sistema nem prender o jogo sempre por cima.
 *
 * A marca se perde quando a janela é escondida, por isso Window a reaplica ao
 * mostrar e ativar o frame. COM é adquirido e liberado na própria EDT.
 * Referência: Microsoft Learn, ITaskbarList2::MarkFullscreenWindow.
 */
final class TelaCheiaDoWindows {
    private static final int RPC_E_CHANGED_MODE = 0x80010106;

    private TelaCheiaDoWindows() { }

    static void marcar(JFrame frame, boolean telaCheia) {
        if (!Platform.isWindows() || !frame.isDisplayable()) {
            return;
        }
        boolean desinicializar = false;
        Taskbar barra = null;
        try {
            HRESULT inicio = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
            desinicializar = COMUtils.SUCCEEDED(inicio);
            // A EDT pode já ter COM em outro modo. Nesse caso, usamos a
            // inicialização existente sem adquirir nem liberar a referência dela.
            if (!desinicializar && inicio.intValue() != RPC_E_CHANGED_MODE) {
                COMUtils.checkRC(inicio);
            }
            PointerByReference resultado = new PointerByReference();
            COMUtils.checkRC(Ole32.INSTANCE.CoCreateInstance(
                    new Guid.GUID("{56FDF344-FD6D-11D0-958A-006097C9A090}"), null,
                    WTypes.CLSCTX_INPROC_SERVER,
                    new Guid.GUID("{602D4995-B13A-429B-A66E-1935E44F4317}"), resultado));
            barra = new Taskbar(resultado.getValue());
            barra.iniciar();
            HWND janela = new HWND(Native.getComponentPointer(frame));
            barra.marcar(janela, telaCheia);
            if (Video.LIGADO) {
                System.out.println("[video] Windows: tela cheia=" + telaCheia + " (MarkFullscreenWindow OK)");
            }
        } catch (RuntimeException | LinkageError falha) {
            System.err.println("Vídeo: não consegui informar tela cheia ao Windows: " + falha);
        } finally {
            if (barra != null) {
                barra.Release();
            }
            if (desinicializar) {
                Ole32.INSTANCE.CoUninitialize();
            }
        }
    }

    /** ITaskbarList2: IUnknown (0–2), ITaskbarList (3–7), MarkFullscreenWindow (8). */
    private static final class Taskbar extends Unknown {
        Taskbar(Pointer ponteiro) {
            super(ponteiro);
        }

        void iniciar() {
            COMUtils.checkRC((HRESULT) _invokeNativeObject(3, new Object[]{getPointer()}, HRESULT.class));
        }

        void marcar(HWND janela, boolean telaCheia) {
            COMUtils.checkRC((HRESULT) _invokeNativeObject(8,
                    new Object[]{getPointer(), janela, telaCheia ? 1 : 0}, HRESULT.class));
        }
    }
}
