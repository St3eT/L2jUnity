/*
 * Copyright (C) 2004-2015 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import java.util.Collection;

import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.holders.ItemHolder;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * Sweeper effect implementation.
 * @author Zoey76
 */
public final class Sweeper extends AbstractEffect
{
	public Sweeper(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, ItemInstance item)
	{
		if (!effector.isPlayer() || !effected.isAttackable())
		{
			return;
		}
		
		final PlayerInstance player = effector.getActingPlayer();
		final Attackable monster = (Attackable) effected;
		if (!monster.checkSpoilOwner(player, false))
		{
			return;
		}
		
		if (!player.getInventory().checkInventorySlotsAndWeight(monster.getSpoilLootItems(), false, false))
		{
			return;
		}
		
		final Collection<ItemHolder> items = monster.takeSweep();
		if (items != null)
		{
			for (ItemHolder sweepedItem : items)
			{
				Party party = player.getParty();
				if (party != null)
				{
					party.distributeItem(player, sweepedItem, true, monster);
				}
				else
				{
					player.addItem("Sweeper", sweepedItem, effected, true);
				}
			}
		}
	}
}
