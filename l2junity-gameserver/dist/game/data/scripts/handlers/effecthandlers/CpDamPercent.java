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

import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Formulas;

/**
 * CP Damage Percent effect implementation.
 * @author Zoey76, Adry_85
 */
public final class CpDamPercent extends AbstractEffect
{
	private final double _power;
	
	public CpDamPercent(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, ItemInstance item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		if (effected.getActingPlayer().isFakeDeath())
		{
			effected.stopFakeDeath(true);
		}
		
		int damage = (int) ((effected.getCurrentCp() * _power) / 100);
		// Manage attack or cast break of the target (calculating rate, sending message)
		if (Formulas.calcAtkBreak(effected, damage))
		{
			effected.breakAttack();
			effected.breakCast();
		}
		
		if (damage > 0)
		{
			effected.setCurrentCp(effected.getCurrentCp() - damage);
		}
	}
}