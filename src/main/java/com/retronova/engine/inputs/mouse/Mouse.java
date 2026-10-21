package com.retronova.engine.inputs.mouse;

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

    /** O mouse de verdade se mexeu desde a última consulta. Ver Gamepad. */
    private static volatile boolean movimentoReal;

    /**
     * Posiciona o cursor virtual do controle.
     *
     * Escreve nos mesmos campos que o mouse de verdade, e é por isso que loja,
     * inventário e objetos do mapa funcionam com o controle sem mudar uma linha.
     */
    public static void moverVirtual(int x, int y) {
        xMouse = x;
        yMouse = y;
    }

    /** Aperta ou solta um botão pelo cursor virtual, igual a mousePressed/mouseReleased. */
    public static synchronized void botaoVirtual(Mouse_Button button, boolean apertado) {
        if (apertado) {
            if (button == Mouse_Button.LEFT) {
                clickLeft = clickLeftOn = pressingLeft = true;
            } else if (button == Mouse_Button.RIGHT) {
                clickRight = clickRightOn = pressingRight = true;
            }
            xClicked = xMouse;
            yClicked = yMouse;
        } else if (button == Mouse_Button.LEFT) {
            clickLeft = clickLeftOn = pressingLeft = false;
        } else if (button == Mouse_Button.RIGHT) {
            clickRight = clickRightOn = pressingRight = false;
        }
    }

    /** Diz se o mouse de verdade se mexeu, e zera o aviso. */
    public static boolean consumirMovimentoReal() {
        boolean moveu = movimentoReal;
        movimentoReal = false;
        return moveu;
    }

    public static boolean on(Rectangle rec) {
        if(rec.contains(xMouse, yMouse)) {
            return true;
        }
        return false;
    }

    public static boolean on(int x, int y, int width, int height) {
        if((Mouse.xMouse >= x && Mouse.xMouse < x + width) &&
                Mouse.yMouse >= y && Mouse.yMouse < y + height) {
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
            return pressingLeft && rec.contains(xMouse, yMouse);
        }
        if (button.equals(Mouse_Button.RIGHT)) {
            return pressingRight && rec.contains(xMouse, yMouse);
        }
        if (button.equals(Mouse_Button.SCROOL)) {
            return pressingScroll && rec.contains(xMouse, yMouse);
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
        return xMouse;
    }

    public static int getY() {
        return yMouse;
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
        movimentoReal = true;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //Set's
        Mouse.xMouse = e.getX();
        Mouse.yMouse = e.getY();
        movimentoReal = true;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        //Set's
        scrollChanged = true;
        scrollSide = e.getWheelRotation();
    }

}
