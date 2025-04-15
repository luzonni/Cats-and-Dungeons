package com.retronova.game.objects.entities.furniture;

import com.retronova.engine.exceptions.TrapDoorCommandException;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.map.arena.Arena;
import com.retronova.game.map.room.Room;
import com.retronova.game.objects.GameObject;
import com.retronova.game.objects.entities.Entity;
import com.retronova.game.objects.entities.Player;

public class TrapDoor extends Furniture {

    private int indexSprite;
    private int count;

    private final String command;

    public TrapDoor(int ID, double x, double y, String command) {
        super(ID, x, y);
        loadSprites("trapdoor");
        this.command = command;
    }

    @Override
    public void tick() {
        Player player = Game.getPlayer();
        if(player.getDistance(this) <= GameObject.SIZE()*1.5) {
            count++;
            if(count > 8) {
                count = 0;
                if (indexSprite < getSheet().size()-1) {
                    indexSprite++;
                }
            }
            if(Mouse.clickOnMap(Mouse_Button.LEFT, this.getBounds(), Game.C)) {
                if(!command.equals("None")) {
                    getCommand();
                }else {
                    System.err.println("A trapdoor foi instanciada sem a referencia do próximo mapa!");
                }
            }
        }else {
            count++;
            if(count > 8) {
                count = 0;
                if (indexSprite > 0) {
                    indexSprite--;
                }
            }
        }
        getSheet().setIndex(indexSprite);
    }

    private void getCommand() {
        // LOAD ["ARENA", "ROOM] SPACE_NAME => Para carregar o espaço referente ao mapa.
        // NEXT ["ARENA", "ROOM] => Para passar para a proxima arena/room
        String[] commands = this.command.split(" ");
        if(commands[0].equalsIgnoreCase("LOAD") && commands.length == 3) {
            String name = commands[2];
            if(commands[1].equalsIgnoreCase("ARENA")) {
                String[] types = {"EASY", "NORMAL", "HARD"};
                Arena arena = null;
                for(int i = 0; i < types.length; i++) {
                    if(types[i].equalsIgnoreCase(name)) {
                        arena = new Arena(i);
                        Game.getGame().setDifficult(i);
                        break;
                    }
                }
                if(arena != null) {
                    Game.getGame().changeMap(arena);
                }else {
                    throw new TrapDoorCommandException("Arena type not found!");
                }
            }else if(commands[1].equalsIgnoreCase("ROOM")) {
                Room room = new Room(name);
                Game.getGame().changeMap(room);
            }
        }
        if(commands[0].equalsIgnoreCase("NEXT") && commands.length == 2) {
            if(commands[1].equalsIgnoreCase("ARENA")) {
                Game.getGame().plusLevel();
                int difficult = Game.getGame().getDifficult();
                Game.getGame().changeMap(new Arena(difficult));
            }
        }
    }

}