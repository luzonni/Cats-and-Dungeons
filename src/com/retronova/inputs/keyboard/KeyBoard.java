package com.retronova.inputs.keyboard;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyBoard implements KeyListener {

    //Instances
    private static boolean BlockWithWriting;
    private static boolean pressKeyboard;
    private static boolean PressedChar;
    private static char LastKeyChar;
    public static Key[] Keys = {
            new Key("E", KeyEvent.VK_E, true),
            new Key("F", KeyEvent.VK_F, true),
            new Key("Q", KeyEvent.VK_Q, true),
            new Key("W", KeyEvent.VK_W, true),
            new Key("A", KeyEvent.VK_A, true),
            new Key("S", KeyEvent.VK_S, true),
            new Key("D", KeyEvent.VK_D, true),
            new Key("T", KeyEvent.VK_T, true),
            new Key("K", KeyEvent.VK_K, true),
            new Key("Shift", KeyEvent.VK_SHIFT, false),
            new Key("Ctrl", KeyEvent.VK_CONTROL, false),
            new Key("Escape", KeyEvent.VK_ESCAPE, false),
            new Key("Space", KeyEvent.VK_SPACE, false),
            new Key("Enter", KeyEvent.VK_ENTER, false),
            new Key("Back_Space", KeyEvent.VK_BACK_SPACE, false),
            new Key("Up", KeyEvent.VK_UP, true),
            new Key("Down", KeyEvent.VK_DOWN, true),
            new Key("Left", KeyEvent.VK_LEFT, true),
            new Key("Right", KeyEvent.VK_RIGHT, true),
            new Key("0", KeyEvent.VK_0, true),
            new Key("1", KeyEvent.VK_1, true),
            new Key("2", KeyEvent.VK_2, true),
            new Key("3", KeyEvent.VK_3, true),
            new Key("4", KeyEvent.VK_4, true),
            new Key("5", KeyEvent.VK_5, true),
            new Key("6", KeyEvent.VK_6, true),
            new Key("7", KeyEvent.VK_7, true),
            new Key("8", KeyEvent.VK_8, true),
            new Key("9", KeyEvent.VK_9, true)
    };
    private static char[] Caracteres = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            '1','2','3','4','5','6','7','8','9','0','-','+','=','.','[',']','(',')','{','}',';','!','\'','@','_','\"'};

    public static final char NONE = '∎';

    public static void Writing(boolean writing) {
        BlockWithWriting = writing;
    }

    public static boolean KeyPressing(String key) {
        for(int i = 0; i < Keys.length; i++) {
            if(Keys[i].getName().equalsIgnoreCase(key)) {
                return Keys[i].Actived();
            }
        }
        return false;
    }

    public static boolean KeyPressed(String key) {
        for(int i = 0; i < Keys.length; i++) {
            if(Keys[i].getName().equalsIgnoreCase(key) && Keys[i].Actived()) {
                Keys[i].setActive(false);
                return true;
            }
        }
        return false;
    }

    public static char getKeyChar() {
        if(PressedChar) {
            for(int i = 0; i < Caracteres.length; i++) {
                if(Caracteres[i] == LastKeyChar) {
                    PressedChar = false;
                    return LastKeyChar;
                }
            }
        }
        return NONE;
    }

    public static char getKeyChar(char[] Caracteres) {
        if(PressedChar) {
            for(int i = 0; i < Caracteres.length; i++) {
                if(Caracteres[i] == LastKeyChar) {
                    PressedChar = false;
                    return LastKeyChar;
                }
            }
        }
        return NONE;
    }

    public static boolean keyboardPressed() {
        return PressedChar;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // == System ==
        for(int i = 0; i < Keys.length; i++) {
            if(Keys[i].getKey() == e.getKeyCode()) {
                Keys[i].setActive(true);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //System
        for(int i = 0; i < Keys.length; i++) {
            if(Keys[i].getKey() == e.getKeyCode()) {
                Keys[i].setActive(false);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        PressedChar = true;
        LastKeyChar = e.getKeyChar();
    }

    public static class Key {

        protected String Name;
        protected int Key;
        protected boolean Actived;
        private boolean lockable;

        public Key(String name, int key, boolean lockable) {
            this.Name = name;
            this.Key = key;
            this.lockable = lockable;
        }

        public Key setActive(boolean bool) {
            this.Actived = bool;
            return this;
        }

        public int getKey() {
            return this.Key;
        }

        public boolean Actived() {
            if(lockable)
                if(BlockWithWriting)
                    return false;
            return this.Actived;
        }

        public String getName() {
            return this.Name;
        }

    }

}