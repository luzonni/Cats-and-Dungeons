package com.retronova.engine.inputs.gamepad;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

/**
 * Ponte para a XInput, a API de controle do Xbox no Windows.
 *
 * Chamada pelo JNA, que já estava no projeto por causa do OSHI — então o suporte
 * a controle não trouxe dependência nova nem engine. Fora do Windows, ou se a DLL
 * não carregar, {@link #API} fica nulo e o jogo segue só com teclado e mouse.
 */
public final class XInput {

    public static final int ERROR_SUCCESS = 0;

    interface Api extends Library {
        int XInputGetState(int dwUserIndex, Estado pState);
        int XInputSetState(int dwUserIndex, Vibracao pVibration);
    }

    /** Nulo quando a XInput não está disponível. */
    static final Api API = carregar();

    private XInput() {
    }

    /**
     * xinput1_4 vem no Windows 8 em diante; xinput9_1_0 existe desde o Vista e é
     * o recurso para o resto. As duas têm as duas funções usadas aqui.
     */
    private static Api carregar() {
        if (!System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
            return null;
        }
        for (String dll : new String[] {"xinput1_4", "xinput9_1_0"}) {
            try {
                return Native.load(dll, Api.class);
            } catch (LinkageError ignore) {
                // tenta a próxima
            }
        }
        System.err.println("XInput indisponível: controle desligado.");
        return null;
    }

    /** XINPUT_GAMEPAD. */
    @Structure.FieldOrder({"wButtons", "bLeftTrigger", "bRightTrigger",
            "sThumbLX", "sThumbLY", "sThumbRX", "sThumbRY"})
    public static class Controle extends Structure {
        public short wButtons;
        public byte bLeftTrigger;
        public byte bRightTrigger;
        public short sThumbLX;
        public short sThumbLY;
        public short sThumbRX;
        public short sThumbRY;
    }

    /** XINPUT_STATE. */
    @Structure.FieldOrder({"dwPacketNumber", "controle"})
    public static class Estado extends Structure {
        public int dwPacketNumber;
        public Controle controle;
    }

    /** XINPUT_VIBRATION. Os dois motores vão de 0 a 65535, lidos como WORD. */
    @Structure.FieldOrder({"wLeftMotorSpeed", "wRightMotorSpeed"})
    public static class Vibracao extends Structure {
        public short wLeftMotorSpeed;
        public short wRightMotorSpeed;
    }
}
