package com.retronova.inputs.mouse;

import com.retronova.game.Camera;

import java.awt.*;
import java.awt.event.*;

public class Mouse implements MouseListener, MouseMotionListener, MouseWheelListener {

    protected static int xMouse, yMouse;
    protected static int xClicked, yClicked;

    protected static boolean pressingLeft, pressingRight, pressingScrool;
    protected static boolean clickLeftOn, clickRightOn, clickScroolOn;
    protected static boolean clickLeft, clickRight, clickScrool;

    protected static int scroolSide = 0;
    protected static boolean scrollChanged = false;

    public static boolean On_Mouse(Rectangle rec) {
        if(rec.contains(xMouse, yMouse)) {
            return true;
        }
        return false;
    }

    public static boolean On_Mouse(int x, int y, int width, int height) {
        if((Mouse.xMouse >= x && Mouse.xMouse < x + width) &&
                Mouse.yMouse >= y && Mouse.yMouse < y + height) {
            return true;
        }
        return false;
    }

    public static synchronized boolean clickOn(Mouse_Button button, Rectangle rec) {
        if(button.equals(Mouse_Button.LEFT))
            if(rec.contains(xClicked, yClicked) && clickLeftOn) {
                clickLeftOn = false;
                return true;
            }
        if(button.equals(Mouse_Button.RIGHT))
            if(rec.contains(xClicked, yClicked) && clickRightOn)  {
                clickRightOn = false;
                return true;
            }
        if(button.equals(Mouse_Button.SCROOL))
            if(rec.contains(xClicked, yClicked) && clickScroolOn) {
                clickScroolOn = false;
                return true;
            }
        return false;
    }

    public static boolean pressingOn(Mouse_Button button, Rectangle rec) {
        if(button.equals(Mouse_Button.LEFT))
            if(rec.contains(xMouse, yMouse) && pressingLeft) {
                return true;
            }
        if(button.equals(Mouse_Button.RIGHT))
            if(rec.contains(xMouse, yMouse) && pressingRight)  {
                return true;
            }
        if(button.equals(Mouse_Button.SCROOL))
            if(rec.contains(xMouse, yMouse) && pressingScrool) {
                return true;
            }
        return false;
    }

    public static boolean clickOnMap(Mouse_Button button, Rectangle rec, Camera cam) {
        if(button.equals(Mouse_Button.LEFT))
            if(rec.contains(xClicked + cam.getX(), yClicked + cam.getY()) && clickLeftOn) {
                clickLeftOn = false;
                return true;
            }
        if(button.equals(Mouse_Button.RIGHT))
            if(rec.contains(xClicked + cam.getX(), yClicked + cam.getY()) && clickRightOn)  {
                clickRightOn = false;
                return true;
            }
        if(button.equals(Mouse_Button.SCROOL))
            if(rec.contains(xClicked + cam.getX(), yClicked + cam.getY()) && clickScroolOn) {
                clickScroolOn = false;
                return true;
            }
        return false;
    }

    public static boolean pressingOnMap(Mouse_Button button, Rectangle rec, Camera cam) {
        if(button.equals(Mouse_Button.LEFT))
            if(rec.contains(xMouse + cam.getX(), yMouse + cam.getY()) && pressingLeft) {
                return true;
            }
        if(button.equals(Mouse_Button.RIGHT))
            if(rec.contains(xMouse + cam.getX(), yMouse + cam.getY()) && pressingRight)  {
                return true;
            }
        if(button.equals(Mouse_Button.SCROOL))
            if(rec.contains(xMouse + cam.getX(), yMouse + cam.getY()) && pressingScrool) {
                return true;
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
        if(button.equals(Mouse_Button.SCROOL) && clickScrool) {
            clickScrool = false;
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
            return pressingScrool;
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
            return scroolSide;
        }
        return 0;
    }

    //Interface Methods

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        //Default
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
            clickScrool = true;
            clickScroolOn = true;
            pressingScrool = true;
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
            clickScrool = false;
            clickScroolOn = false;
            pressingScrool = false;
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
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //Set's
        Mouse.xMouse = e.getX();
        Mouse.yMouse = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        //Set's
        scrollChanged = true;
        scroolSide = e.getWheelRotation();
    }

}
