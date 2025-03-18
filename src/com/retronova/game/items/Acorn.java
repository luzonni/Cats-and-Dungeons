package com.retronova.game.items;

import com.retronova.engine.Engine;
import com.retronova.engine.sound.Sound;
import com.retronova.engine.sound.Sounds;
import com.retronova.game.Game;
import com.retronova.game.objects.entities.Modifiers;
import com.retronova.game.objects.entities.Player;

public class Acorn extends Consumable {

    Acorn(int id) {
        super(id, "Acorn", "acorn");
        addSpecifications("Gets 1 min of small regeneration");
    }

    @Override
    public void consume() {
        Player player = Game.getPlayer();
        player.addEffect("Acorn regeneration", (e) -> {
            if(e.getLife() < e.getLifeSize() && Engine.RAND.nextDouble() <= player.getLuck())
                e.setLife(e.getLife()+0.15d);
        }, 60);
        Game.getPlayer().addModifier(Modifiers.Dodge, 0.4);
        Sound.play(Sounds.Crack);
    }
}
