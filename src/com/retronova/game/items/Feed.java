package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class Feed extends Consumable {


    Feed(int id) {
        super(id, "Feed", "feed");
        addSpecifications("(Consumable) Press F", "Add 10 points of life", "Less 10% os luck", "plus +1 bag slot", "plus +1 hotbar slotf");
        if(Engine.RAND.nextBoolean())
            plusIndexSprite();
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Life, 10);
        player.addModifier(Modifiers.Luck, -0.1);
        player.getInventory().plusBag(1);
        player.getInventory().plusHotbar(1);
        player.addPassive(this);
        Sound.play(Sounds.Crack);
    }

}
