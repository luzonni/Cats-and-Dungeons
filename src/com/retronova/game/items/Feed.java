package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class Feed extends Consumable {


    Feed(int id) {
        super(id, "Feed", "feed");
        addSpecifications("Add 10 points of life", "Less 10% os luck", "10 seconds of regeneration");

        if(Engine.RAND.nextBoolean())
            plusIndexSprite();
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Life, 10);
        player.addModifier(Modifiers.Luck, 0.1);
        player.addEffect("regeneration", e -> {
            if(e.getLife() < e.getLifeSize())
                e.setLife(e.getLife()+0.1d);
        }, 10);
    }

    @Override
    public void render(Graphics2D g) {

    }
}
