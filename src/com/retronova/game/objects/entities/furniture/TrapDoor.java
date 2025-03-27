package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.GameMap;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.entities.Entity;

public class TrapDoor extends Furniture {

    private int indexSprite;
    private int count;

    private final String placeName;

    public TrapDoor(int ID, double x, double y, String place) {
        super(ID, x, y);
        loadSprites("trapdoor");
        this.placeName = place;
    }

    @Override
    public void tick() {
        Entity nearest = getNearest(2);
        if(nearest != null) {
            count++;
            if(count > 7 && indexSprite < 11) {
                indexSprite++;
            }
        }else {
            count++;
            if(count > 0 && indexSprite > 0) {
                indexSprite--;
            }
        }
        if(indexSprite == 11) {
            if(Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                if(!placeName.equals("None")) {
                    loadPlace();
                }else {
                    System.err.println("A trapdoor foi instanciada sem a referencia do próximo mapa!");
                }
            }
        }
        getSheet().setIndex(indexSprite);
    }

    private void loadPlace() {
        int level = Game.getGame().getLevel();
        String[] result = placeName.split("_");
        String type = result[0];
        String name = result[1];

        if(type.equals("room")) {
            Room room = new Room(name);
            Game.getGame().changeMap(room);
        }else if(type.equals("arena")) {
            //TODO consertar sistema de dificuldade
            Arena arena = new Arena(name, 1, level);
            Game.getGame().changeMap(arena);
        }
    }

}