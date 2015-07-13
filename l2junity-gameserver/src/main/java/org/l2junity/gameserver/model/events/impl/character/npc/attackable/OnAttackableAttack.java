/*
 * Copyright (C) 2004-2015 L2J Unity
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
package org.l2junity.gameserver.model.events.impl.character.npc.attackable;

import org.l2junity.gameserver.model.actor.Attackable;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventType;
import org.l2junity.gameserver.model.events.impl.IBaseEvent;
import org.l2junity.gameserver.model.skills.Skill;

/**
 * An instantly executed event when L2Attackable is attacked by L2PcInstance.
 * @author UnAfraid
 */
public class OnAttackableAttack implements IBaseEvent
{
	private final PlayerInstance _attacker;
	private final Attackable _target;
	private final int _damage;
	private final Skill _skill;
	private final boolean _isSummon;
	
	public OnAttackableAttack(PlayerInstance attacker, Attackable target, int damage, Skill skill, boolean isSummon)
	{
		_attacker = attacker;
		_target = target;
		_damage = damage;
		_skill = skill;
		_isSummon = isSummon;
	}
	
	public final PlayerInstance getAttacker()
	{
		return _attacker;
	}
	
	public final Attackable getTarget()
	{
		return _target;
	}
	
	public int getDamage()
	{
		return _damage;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_ATTACK;
	}
}