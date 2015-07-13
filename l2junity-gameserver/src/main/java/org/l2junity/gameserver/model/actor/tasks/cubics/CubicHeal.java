/*
 * Copyright (C) 2004-2014 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.actor.tasks.cubics;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2junity.commons.util.Rnd;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.instance.L2CubicInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.network.client.send.MagicSkillUse;

/**
 * Cubic heal task.
 * @author Zoey76
 */
public class CubicHeal implements Runnable
{
	private static final Logger _log = Logger.getLogger(CubicHeal.class.getName());
	private final L2CubicInstance _cubic;
	
	public CubicHeal(L2CubicInstance cubic)
	{
		_cubic = cubic;
	}
	
	@Override
	public void run()
	{
		if (_cubic == null)
		{
			return;
		}
		
		if (_cubic.getOwner().isDead() || !_cubic.getOwner().isOnline())
		{
			_cubic.stopAction();
			_cubic.getOwner().getCubics().remove(_cubic.getId());
			_cubic.getOwner().broadcastUserInfo();
			_cubic.cancelDisappear();
			return;
		}
		try
		{
			Skill skill = null;
			final int chance = Rnd.get(100);
			OUTTER: for (Skill sk : _cubic.getSkills())
			{
				switch (sk.getId())
				{
					case L2CubicInstance.SKILL_CUBIC_HEAL:
					case L2CubicInstance.SKILL_CUBIC_HEALER:
					{
						skill = sk;
						break OUTTER;
					}
					case L2CubicInstance.SKILL_POEM_CUBIC_HEAL:
					case L2CubicInstance.SKILL_MENTAL_CUBIC_RECHARGE:
					{
						if (chance < 90)
						{
							skill = sk;
							break OUTTER;
						}
						break;
					}
					case L2CubicInstance.SKILL_POEM_CUBIC_GREAT_HEAL:
					case L2CubicInstance.SKILL_MENTAL_CUBIC_GREAT_RECHARGE:
					{
						if (chance < 10)
						{
							skill = sk;
							break OUTTER;
						}
						break;
					}
				}
			}
			
			if (skill != null)
			{
				_cubic.cubicTargetForHeal();
				final Creature target = _cubic.getTarget();
				if ((target != null) && !target.isDead())
				{
					if ((target.getMaxHp() - target.getCurrentHp()) > 10)
					{
						Creature[] targets =
						{
							target
						};
						
						skill.activateSkill(_cubic.getOwner(), targets);
						
						_cubic.getOwner().broadcastPacket(new MagicSkillUse(_cubic.getOwner(), target, skill.getId(), skill.getLevel(), 0, 0));
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.SEVERE, "", e);
		}
	}
}