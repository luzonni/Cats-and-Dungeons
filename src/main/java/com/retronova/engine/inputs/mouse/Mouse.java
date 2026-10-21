package com.retronova.engine.inputs.mouse;

import com.retronova.engine.inputs.Entrada;

import java.awt.*;
import java.awt.event.*;

public class Mouse implements MouseListener, MouseMotionListener, MouseWheelListener {

    protected static int xMouse, yMouse;
    protected static int xClicked, yClicked;

    protected static boolean pressingLeft, pressingRight, pressingScroll;
    protected static boolean clickLeftOn, clickRightOn, clickScrollOn;
    protected static boolean clickLeft, clickRight, clickScroll;

    protected static int scrollSide = 0;
    protected static boolean scrollChanged = false;

    /** Onde o ponteiro "está" no modo controle: longe de qualquer coisa na tela. */
    private static final int FORA_DA_TELA = -100_000;

    /**
     * NO MODO CONTROLE O PONTEIRO NAO EXISTE.
     *
     * As telas perguntam a cada tick se o mouse esta sobre um botao ou uma carta,
     * e dao o foco a quem estiver embaixo dele. Um mouse PARADO em cima de uma
     * carta respondia "sim" para sempre e tomava de volta o foco que o D-pad
     * acabara de mudar. Responder daqui que o ponteiro esta fora da tela resolve
     * isso em todas as telas de uma vez, e mexer o mouse o traz de volta.
     */
    private static int x() {
        return Entrada.usandoControle() ? FORA_DA_TELA : xMouse;
    }

    private static int y() {
        return Entrada.usandoControle() ? FORA_DA_TELA : yMouse;
    }

    public static boolean on(Rectangle rec) {
        if(rec.contains(x(), y())) {
            return true;
        }
        return false;
    }

    public static boolean on(int x, int y, int width, int height) {
        if((x() >= x && x() < x + width) &&
                y() >= y && y() < y + height) {
            return true;
        }
        return false;
    }

    public static synchronized boolean clickOn(Mouse_Button button, Rectangle rec) {
        if (button.equals(Mouse_Button.LEFT))
            if (rec.contains(xClicked, yClicked) && clickLeftOn) {
                clickLeftOn = false;
                return true;
            }
        if (button.equals(Mouse_Button.RIGHT))
            if (rec.contains(xClicked, yClicked) && clickRightOn) {
                clickRightOn = false;
                return true;
            }
        if (button.equals(Mouse_Button.SCROOL))
            if (rec.contains(xClicked, yClicked) && clickScrollOn) {
                clickScrollOn = false;
                return true;
            }
        return false;
    }

    public static boolean isPressed(Mouse_Button button, Rectangle rec) {
        if (button.equals(Mouse_Button.LEFT)) {
            return pressingLeft && rec.contains(x(), y());
        }
        if (button.equals(Mouse_Button.RIGHT)) {
            return pressingRight && rec.contains(x(), y());
        }
        if (button.equals(Mouse_Button.SCROOL)) {
            return pressingScroll && rec.contains(x(), y());
        }
        return false;
    }

    public static boolean click(Mouse_Button button) {
        if(button.equals(Mouse_Button.LEFT) && clickLeft) {
            clickLeft = false;
            return true;
        }
        if(button.equals(Mouse_Button.RIGHT) && clickRight) {
            clickRight = false;
            return true;
        }
        if(button.equals(Mouse_Button.SCROOL) && clickScroll) {
            clickScroll = false;
            return true;
        }
        return false;
    }

    public static boolean pressing(Mouse_Button button) {
        if(button.equals(Mouse_Button.LEFT))
            return pressingLeft;
        if(button.equals(Mouse_Button.RIGHT))
            return pressingRight;
        if(button.equals(Mouse_Button.SCROOL))
            return pressingScroll;
        return false;
    }

    public static int getX() {
        return x();
    }

    public static int getY() {
        return y();
    }

    public static int getClickedX() {
        return xClicked;
    }

    public static int getClickedY() {
        return yClicked;
    }

    public static int Scroll() {
        if(scrollChanged) {
            scrollChanged = false;
            return scrollSide;
        }
        return 0;
    }

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            clickLeft = true;
            clickLeftOn = true;
            pressingLeft = true;
        }
        if(e.getButton() == MouseEvent.BUTTON3) {
            clickRight = true;
            clickRightOn = true;
            pressingRight = true;
        }
        if(e.getButton() == MouseEvent.BUTTON2) {
            clickScroll = true;
            clickScrollOn = true;
            pressingScroll = true;
        }
        //Sets
        xClicked = e.getX();
        yClicked = e.getY();
        Entrada.mouse();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        //DefaultSystem
        if(e.getButton() == MouseEvent.BUTTON1) {
            clickLeft = false;
            clickLeftOn = false;
            pressingLeft = false;
        }
        if(e.getButton() == MouseEvent.BUTTON3) {
            clickRight = false;
            clickRightOn = false;
            pressingRight = false;
        }
        if(e.getButton() == MouseEvent.BUTTON2) {
            clickScroll = false;
            clickScrollOn = false;
            pressingScroll = false;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        //Set's
        Mouse.xMouse = e.getX();
        Mouse.yMouse = e.getY();
        Entrada.mouse();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //Set's
        Mouse.xMouse = e.getX();
        Mouse.yMouse = e.getY();
        Entrada.mouse();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        //Set's
        scrollChanged = true;
        scrollSide = e.getWheelRotation();
    }

}
