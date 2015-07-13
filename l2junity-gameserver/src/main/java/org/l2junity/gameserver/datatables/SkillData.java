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
package org.l2junity.gameserver.datatables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2junity.Config;
import org.l2junity.gameserver.data.xml.impl.SkillTreesData;
import org.l2junity.gameserver.engines.DocumentEngine;
import org.l2junity.gameserver.model.skills.CommonSkill;
import org.l2junity.gameserver.model.skills.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skill data.
 */
public final class SkillData
{
	private static Logger LOGGER = LoggerFactory.getLogger(SkillData.class);
	
	private final Map<Integer, Skill> _skills = new HashMap<>();
	private final Map<Integer, Integer> _skillMaxLevel = new HashMap<>();
	private final Set<Integer> _enchantable = new HashSet<>();
	
	protected SkillData()
	{
		load();
	}
	
	public void reload()
	{
		load();
		// Reload Skill Tree as well.
		SkillTreesData.getInstance().load();
	}
	
	private void load()
	{
		final Map<Integer, Skill> _temp = new HashMap<>();
		DocumentEngine.getInstance().loadAllSkills(_temp);
		
		_skills.clear();
		_skills.putAll(_temp);
		
		_skillMaxLevel.clear();
		_enchantable.clear();
		for (Skill skill : _skills.values())
		{
			final int skillId = skill.getId();
			final int skillLvl = skill.getLevel();
			if (skillLvl > 99)
			{
				if (!_enchantable.contains(skillId))
				{
					_enchantable.add(skillId);
				}
				continue;
			}
			
			// only non-enchanted skills
			final int maxLvl = getMaxLevel(skillId);
			if (skillLvl > maxLvl)
			{
				_skillMaxLevel.put(skillId, skillLvl);
			}
		}
	}
	
	/**
	 * Provides the skill hash
	 * @param skill The L2Skill to be hashed
	 * @return getSkillHashCode(skill.getId(), skill.getLevel())
	 */
	public static int getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel());
	}
	
	/**
	 * Centralized method for easier change of the hashing sys
	 * @param skillId The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillHashCode(int skillId, int skillLevel)
	{
		return (skillId * 1021) + skillLevel;
	}
	
	public Skill getSkill(int skillId, int level)
	{
		final Skill result = _skills.get(getSkillHashCode(skillId, level));
		if (result != null)
		{
			return result;
		}
		
		// skill/level not found, fix for transformation scripts
		final int maxLvl = getMaxLevel(skillId);
		// requested level too high
		if ((maxLvl > 0) && (level > maxLvl))
		{
			if (Config.DEBUG)
			{
				LOGGER.warn("call to unexisting skill level id: {} requested level: {} max level: {}", skillId, level, maxLvl, new Throwable());
			}
			return _skills.get(getSkillHashCode(skillId, maxLvl));
		}
		
		LOGGER.warn("No skill info found for skill id {} and skill level {}", skillId, level);
		return null;
	}
	
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _skillMaxLevel.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}
	
	/**
	 * Verifies if the given skill ID correspond to an enchantable skill.
	 * @param skillId the skill ID
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise
	 */
	public boolean isEnchantable(int skillId)
	{
		return _enchantable.contains(skillId);
	}
	
	/**
	 * @param addNoble
	 * @param hasCastle
	 * @return an array with siege skills. If addNoble == true, will add also Advanced headquarters.
	 */
	public List<Skill> getSiegeSkills(boolean addNoble, boolean hasCastle)
	{
		final List<Skill> temp = new LinkedList<>();
		
		temp.add(_skills.get(SkillData.getSkillHashCode(CommonSkill.IMPRIT_OF_LIGHT.getId(), 1)));
		temp.add(_skills.get(SkillData.getSkillHashCode(CommonSkill.IMPRIT_OF_DARKNESS.getId(), 1)));
		
		temp.add(_skills.get(SkillData.getSkillHashCode(247, 1))); // Build Headquarters
		
		if (addNoble)
		{
			temp.add(_skills.get(SkillData.getSkillHashCode(326, 1))); // Build Advanced Headquarters
		}
		if (hasCastle)
		{
			temp.add(_skills.get(SkillData.getSkillHashCode(844, 1))); // Outpost Construction
			temp.add(_skills.get(SkillData.getSkillHashCode(845, 1))); // Outpost Demolition
		}
		return temp;
	}
	
	public static SkillData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData _instance = new SkillData();
	}
}
