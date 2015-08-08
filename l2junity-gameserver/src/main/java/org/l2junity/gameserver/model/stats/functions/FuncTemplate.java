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
package org.l2junity.gameserver.model.stats.functions;

import java.lang.reflect.Constructor;

import org.l2junity.gameserver.enums.StatFunction;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.conditions.Condition;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.skills.Skill;
import org.l2junity.gameserver.model.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function template.
 * @author mkizub, Zoey76
 */
public final class FuncTemplate
{
	private static final Logger LOG = LoggerFactory.getLogger(FuncTemplate.class);
	
	private final Class<?> _functionClass;
	private final Condition _attachCond;
	private final Condition _applayCond;
	private final Constructor<?> _constructor;
	private final Stats _stat;
	private final int _order;
	private final double _value;
	
	public FuncTemplate(Condition attachCond, Condition applayCond, String functionName, int order, Stats stat, double value)
	{
		final StatFunction function = StatFunction.valueOf(functionName.toUpperCase());
		if (order >= 0)
		{
			_order = order;
		}
		else
		{
			_order = function.getOrder();
		}
		
		_attachCond = attachCond;
		_applayCond = applayCond;
		_stat = stat;
		_value = value;
		
		try
		{
			_functionClass = Class.forName("org.l2junity.gameserver.model.stats.functions.Func" + function.getName());
			_constructor = _functionClass.getConstructor(Stats.class, // Stats to update
			Integer.TYPE, // Order of execution
			Object.class, // Owner
			Double.TYPE, // Value for function
			Condition.class // Condition
			);
		}
		catch (ClassNotFoundException | NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public Class<?> getFunctionClass()
	{
		return _functionClass;
	}
	
	/**
	 * Gets the function stat.
	 * @return the stat.
	 */
	public Stats getStat()
	{
		return _stat;
	}
	
	/**
	 * Gets the function priority order.
	 * @return the order
	 */
	public int getOrder()
	{
		return _order;
	}
	
	/**
	 * Gets the function value.
	 * @return the value
	 */
	public double getValue()
	{
		return _value;
	}
	
	/**
	 * Gets the functions for skills.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @param owner the owner
	 * @return the function if conditions are met, {@code null} otherwise
	 */
	public AbstractFunction getFunc(Creature caster, Creature target, Skill skill, Object owner)
	{
		return getFunc(caster, target, skill, null, owner);
	}
	
	/**
	 * Gets the functions for items.
	 * @param caster the caster
	 * @param target the target
	 * @param item the item
	 * @param owner the owner
	 * @return the function if conditions are met, {@code null} otherwise
	 */
	public AbstractFunction getFunc(Creature caster, Creature target, ItemInstance item, Object owner)
	{
		return getFunc(caster, target, null, item, owner);
	}
	
	/**
	 * Gets the functions for skills and items.
	 * @param caster the caster
	 * @param target the target
	 * @param skill the skill
	 * @param item the item
	 * @param owner the owner
	 * @return the function if conditions are met, {@code null} otherwise
	 */
	private AbstractFunction getFunc(Creature caster, Creature target, Skill skill, ItemInstance item, Object owner)
	{
		if ((_attachCond != null) && !_attachCond.test(caster, target, skill))
		{
			return null;
		}
		try
		{
			return (AbstractFunction) _constructor.newInstance(_stat, _order, owner, _value, _applayCond);
		}
		catch (Exception e)
		{
			LOG.warn(FuncTemplate.class.getSimpleName() + ": " + e.getMessage());
		}
		return null;
	}
}
