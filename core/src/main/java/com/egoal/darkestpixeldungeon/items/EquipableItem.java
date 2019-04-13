/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.egoal.darkestpixeldungeon.items;

import com.egoal.darkestpixeldungeon.Assets;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.actors.hero.Hero;
import com.egoal.darkestpixeldungeon.effects.particles.ShadowParticle;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public abstract class EquipableItem extends Item {

  public static final String AC_EQUIP = "EQUIP";
  public static final String AC_UNEQUIP = "UNEQUIP";

  {
    bones = true;
  }

  @Override
  public ArrayList<String> actions(Hero hero) {
    ArrayList<String> actions = super.actions(hero);
    actions.add(isEquipped(hero) ? AC_UNEQUIP : AC_EQUIP);
    return actions;
  }

  @Override
  public void execute(Hero hero, String action) {

    super.execute(hero, action);

    if (action.equals(AC_EQUIP)) {
      //In addition to equipping itself, item reassigns itself to the quickslot
      //This is a special case as the item is being removed from inventory, 
      // but is staying with the hero.
      int slot = Dungeon.quickslot.getSlot(this);
      doEquip(hero);
      if (slot != -1) {
        Dungeon.quickslot.setSlot(slot, this);
        updateQuickslot();
      }
    } else if (action.equals(AC_UNEQUIP)) {
      doUnequip(hero, true);
    }
  }

  @Override
  public void doDrop(Hero hero) {
    if (!isEquipped(hero) || doUnequip(hero, false, false)) {
      super.doDrop(hero);
    }
  }

  @Override
  public void cast(final Hero user, int dst) {

    if (isEquipped(user)) {
      if (quantity == 1 && !this.doUnequip(user, false, false)) {
        return;
      }
    }

    super.cast(user, dst);
  }

  // called when equip cursed things, 
  public static void equipCursed(Hero hero) {
    hero.sprite.emitter().burst(ShadowParticle.CURSE, 6);
    Sample.INSTANCE.play(Assets.SND_CURSED);

    hero.takeDamage(new Damage(Random.Int(4, 10), Char.Nobody.INSTANCE, hero).type(Damage
            .Type.MENTAL));
  }

  protected float time2equip(Hero hero) {
    return 1;
  }

  public abstract boolean doEquip(Hero hero);

  public boolean doUnequip(Hero hero, boolean collect, boolean single) {

    if (cursed) {
      GLog.w(Messages.get(EquipableItem.class, "unequip_cursed"));
      return false;
    }

    if (single) {
      hero.spendAndNext(time2equip(hero));
    } else {
      hero.spend(time2equip(hero));
    }

    if (!collect || !collect(hero.belongings.backpack)) {
      onDetach();
      Dungeon.quickslot.clearItem(this);
      updateQuickslot();
      if (collect) Dungeon.level.drop(this, hero.pos);
    }

    return true;
  }

  final public boolean doUnequip(Hero hero, boolean collect) {
    return doUnequip(hero, collect, true);
  }

  public void activate(Char ch) {
  }
}
