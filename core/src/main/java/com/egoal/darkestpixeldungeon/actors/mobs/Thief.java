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
package com.egoal.darkestpixeldungeon.actors.mobs;

import com.egoal.darkestpixeldungeon.PropertyConfiger;
import com.egoal.darkestpixeldungeon.actors.Char;
import com.egoal.darkestpixeldungeon.actors.Damage;
import com.egoal.darkestpixeldungeon.actors.buffs.Corruption;
import com.egoal.darkestpixeldungeon.actors.buffs.Roots;
import com.egoal.darkestpixeldungeon.actors.buffs.Terror;
import com.egoal.darkestpixeldungeon.actors.hero.Hero;
import com.egoal.darkestpixeldungeon.effects.CellEmitter;
import com.egoal.darkestpixeldungeon.effects.Speck;
import com.egoal.darkestpixeldungeon.items.artifacts.MasterThievesArmband;
import com.egoal.darkestpixeldungeon.items.unclassified.Gold;
import com.egoal.darkestpixeldungeon.items.unclassified.Honeypot;
import com.egoal.darkestpixeldungeon.sprites.CharSprite;
import com.egoal.darkestpixeldungeon.utils.GLog;
import com.egoal.darkestpixeldungeon.Dungeon;
import com.egoal.darkestpixeldungeon.items.Item;
import com.egoal.darkestpixeldungeon.messages.Messages;
import com.egoal.darkestpixeldungeon.sprites.ThiefSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.HashSet;

public class Thief extends Mob {

  public Item item;

  {
    PropertyConfiger.INSTANCE.set(this, "Thief");

    spriteClass = ThiefSprite.class;

     loot = new MasterThievesArmband().identify();

    FLEEING = new Fleeing();
  }

  private static final String ITEM = "item";

  @Override
  public void storeInBundle(Bundle bundle) {
    super.storeInBundle(bundle);
    bundle.put(ITEM, item);
  }

  @Override
  public void restoreFromBundle(Bundle bundle) {
    super.restoreFromBundle(bundle);
    item = (Item) bundle.get(ITEM);
  }

  @Override
  public float speed() {
    if (item != null) return (5 * super.speed()) / 6;
    else return super.speed();
  }

  @Override
  protected float attackDelay() {
    return 0.5f;
  }

  @Override
  public void die(Object cause) {

    super.die(cause);

    if (item != null) {
      Dungeon.level.drop(item, pos).getSprite().drop();
      //updates position
      if (item instanceof Honeypot.ShatteredPot)
        ((Honeypot.ShatteredPot) item).setHolder(this);
    }
  }

  @Override
  protected Item createLoot() {
    if (!Dungeon.limitedDrops.armband.dropped()) {
      Dungeon.limitedDrops.armband.drop();
      return super.createLoot();
    } else
      return new Gold(Random.NormalIntRange(80, 200));
  }

  @Override
  public Damage attackProc(Damage dmg) {
    Char enemy = (Char) dmg.to;
    if (item == null && enemy instanceof Hero && steal((Hero) enemy)) {
      enemy.takeDamage(new Damage(Random.IntRange(1, 5), this, enemy).type(Damage.Type.MENTAL));
      state = FLEEING;
    }

    return dmg;
  }

  @Override
  public Damage defenseProc(Damage damage) {
    if (state == FLEEING) {
      Dungeon.level.drop(new Gold(), pos).getSprite().drop();
    }

    return super.defenseProc(damage);
  }

  protected boolean steal(Hero hero) {

    Item item = hero.getBelongings().randomUnequipped();

    if (item != null && !item.unique && item.level() < 1) {

      GLog.w(Messages.get(Thief.class, "stole", item.name()));
      Dungeon.quickslot.clearItem(item);
      item.updateQuickslot();

      // process on the honey pot
      if (item instanceof Honeypot) {
        this.item = ((Honeypot) item).shatter(this, this.pos);
        item.detach(hero.getBelongings().backpack);
      } else {
        this.item = item.detach(hero.getBelongings().backpack);
        if (item instanceof Honeypot.ShatteredPot)
          ((Honeypot.ShatteredPot) item).setHolder(this);
      }

      return true;
    } else {
      return false;
    }
  }

  @Override
  public String description() {
    String desc = super.description();

    if (item != null) {
      desc += Messages.get(this, "carries", item.name());
    }

    return desc;
  }

  private static final HashSet<Class<?>> IMMUS = new HashSet<>();

  public HashSet<Class<?>> immunizedBuffs() {
    IMMUS.add(Roots.class);
    return IMMUS;
  }
  
  private class Fleeing extends Mob.Fleeing {
    @Override
    protected void nowhereToRun() {
      if (buff(Terror.class) == null && buff(Corruption.class) == null) {
        if (enemySeen) {
          sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Mob.class,
                  "rage"));
          state = HUNTING;
        } else {

          int count = 32;
          int newPos;
          do {
            newPos = Dungeon.level.randomRespawnCell();
            if (count-- <= 0) {
              break;
            }
          }
          while (newPos == -1 || Dungeon.visible[newPos] || Dungeon.level
                  .distance(newPos, pos) < (count / 3));

          if (newPos != -1) {

            if (Dungeon.visible[pos])
              CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
            pos = newPos;
            sprite.place(pos);
            sprite.visible = Dungeon.visible[pos];
            if (Dungeon.visible[pos])
              CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);

          }

          if (item != null) {
            GLog.n(Messages.get(Thief.class, "escapes", item.name()));
            if(Dungeon.hero.isAlive())
              Dungeon.hero.takeDamage(new Damage(Random.IntRange(2, 6), this, Dungeon.hero).
                    type(Damage.Type.MENTAL).addFeature(Damage.Feature.PURE));
          }
          item = null;
          state = WANDERING;
        }
      } else {
        super.nowhereToRun();
      }
    }
  }
}
