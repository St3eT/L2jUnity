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

import org.l2junity.gameserver.ai.CtrlEvent;
import org.l2junity.gameserver.model.StatsSet;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.EffectFlag;
import org.l2junity.gameserver.model.effects.L2EffectType;
import org.l2junity.gameserver.model.skills.BuffInfo;

/**
 * Block Actions effect implementation.
 * @author mkizub
 */
public final class BlockActions extends AbstractEffect
{
	public BlockActions(Condition attachCond, Condition applyCond, StatsSet set, StatsSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.BLOCK_ACTIONS.getMask();
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.BLOCK_ACTIONS;
	}

	@Override
	public void onStart(BuffInfo info)
	{
		// Aborts any attacks/casts if stunned
		info.getEffected().startParalyze();
	}

	@Override
	public void onExit(BuffInfo info)
	{
		final Creature effected = info.getEffected();
		if (!effected.isPlayer())
		{
			effected.getAI().notifyEvent(CtrlEvent.EVT_THINK);
		}
	}
}