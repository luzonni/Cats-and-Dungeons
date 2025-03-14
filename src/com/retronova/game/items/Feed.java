package com.retronova.game.items;

import com.retronova.engine.inputs.mouse.Mouse;
import com.retronova.engine.inputs.mouse.Mouse_Button;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

import java.awt.*;

public class Feed extends Consumable {


    Feed(int id) {
        super(id, "Feed", "feed");
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addModifier(Modifiers.Life, 10);
        player.addModifier(Modifiers.Luck, -0.1);
    }

    @Override
    public void render(Graphics2D g) {

    }
}
