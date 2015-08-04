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
package org.l2junity.gameserver.model.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.l2junity.Config;
import org.l2junity.gameserver.GameTimeController;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.model.CharEffectList;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Summon;
import org.l2junity.gameserver.model.effects.AbstractEffect;
import org.l2junity.gameserver.model.effects.EffectTaskInfo;
import org.l2junity.gameserver.model.effects.EffectTickTask;
import org.l2junity.gameserver.model.stats.Formulas;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Buff Info.<br>
 * Complex DTO that holds all the information for a given buff (or debuff or dance/song) set of effects issued by an skill.
 * @author Zoey76
 */
public final class BuffInfo
{
	private static final Logger _log = LoggerFactory.getLogger(BuffInfo.class);
	
	// Data
	/** Data. */
	private final Creature _effector;
	private final Creature _effected;
	private final Skill _skill;
	/** The effects. */
	private final List<AbstractEffect> _effects = new ArrayList<>(1);
	// Tasks
	/** Effect tasks for ticks. */
	private volatile Map<AbstractEffect, EffectTaskInfo> _tasks;
	/** Scheduled future. */
	private ScheduledFuture<?> _scheduledFutureTimeTask;
	// Time and ticks
	/** Abnormal time. */
	private int _abnormalTime;
	/** The game ticks at the start of this effect. */
	private int _periodStartTicks;
	// Misc
	/** If {@code true} then this effect has been cancelled. */
	private boolean _isRemoved = false;
	/** If {@code true} then this effect is in use (or has been stop because an Herb took place). */
	private boolean _isInUse = true;
	private boolean _hideStartMessage;
	
	/**
	 * Buff Info constructor.
	 * @param effector the effector
	 * @param effected the effected
	 * @param skill the skill
	 * @param hideStartMessage  hides start message
	 */
	public BuffInfo(Creature effector, Creature effected, Skill skill, boolean hideStartMessage)
	{
		_effector = effector;
		_effected = effected;
		_skill = skill;
		_abnormalTime = Formulas.calcEffectAbnormalTime(effector, effected, skill);
		_periodStartTicks = GameTimeController.getInstance().getGameTicks();
		_hideStartMessage = hideStartMessage;
	}
	
	/**
	 * Gets the effects on this buff info.
	 * @return the effects
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Adds an effect to this buff info.
	 * @param effect the effect to add
	 */
	public void addEffect(AbstractEffect effect)
	{
		_effects.add(effect);
	}
	
	/**
	 * Adds an effect task to this buff info.<br>
	 * Uses double-checked locking to initialize the map if it's necessary.
	 * @param effect the effect that owns the task
	 * @param effectTaskInfo the task info
	 */
	private void addTask(AbstractEffect effect, EffectTaskInfo effectTaskInfo)
	{
		if (_tasks == null)
		{
			synchronized (this)
			{
				if (_tasks == null)
				{
					_tasks = new ConcurrentHashMap<>();
				}
			}
		}
		_tasks.put(effect, effectTaskInfo);
	}
	
	/**
	 * Gets the task for the given effect.
	 * @param effect the effect
	 * @return the task
	 */
	private EffectTaskInfo getEffectTask(AbstractEffect effect)
	{
		return (_tasks == null) ? null : _tasks.get(effect);
	}
	
	/**
	 * Gets the skill that created this buff info.
	 * @return the skill
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Gets the calculated abnormal time.
	 * @return the abnormal time
	 */
	public int getAbnormalTime()
	{
		return _abnormalTime;
	}
	
	/**
	 * Sets the abnormal time.
	 * @param abnormalTime the abnormal time to set
	 */
	public void setAbnormalTime(int abnormalTime)
	{
		_abnormalTime = abnormalTime;
	}
	
	/**
	 * Gets the period start ticks.
	 * @return the period start
	 */
	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}
	
	/**
	 * Get the remaining time in seconds for this buff info.
	 * @return the elapsed time
	 */
	public int getTime()
	{
		return _abnormalTime - ((GameTimeController.getInstance().getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND);
	}
	
	/**
	 * Verify if this buff info has been cancelled.
	 * @return {@code true} if this buff info has been cancelled, {@code false} otherwise
	 */
	public boolean isRemoved()
	{
		return _isRemoved;
	}
	
	/**
	 * Set the buff info to removed.
	 * @param val the value to set
	 */
	public void setRemoved(boolean val)
	{
		_isRemoved = val;
	}
	
	/**
	 * Verify if this buff info is in use.
	 * @return {@code true} if this buff info is in use, {@code false} otherwise
	 */
	public boolean isInUse()
	{
		return _isInUse;
	}
	
	/**
	 * Set the buff info to in use.
	 * @param val the value to set
	 */
	public void setInUse(boolean val)
	{
		_isInUse = val;
	}
	
	/**
	 * Gets the character that launched the buff.
	 * @return the effector
	 */
	public Creature getEffector()
	{
		return _effector;
	}
	
	/**
	 * Gets the target of the skill.
	 * @return the effected
	 */
	public Creature getEffected()
	{
		return _effected;
	}
	
	/**
	 * Stops all the effects for this buff info.<br>
	 * Removes effects stats.<br>
	 * <b>It will not remove the buff info from the effect list</b>.<br>
	 * Instead call {@link CharEffectList#stopSkillEffects(boolean, Skill)}
	 * @param removed if {@code true} the skill will be handled as removed
	 */
	public void stopAllEffects(boolean removed)
	{
		setRemoved(removed);
		// Cancels the task that will end this buff info
		if ((_scheduledFutureTimeTask != null) && !_scheduledFutureTimeTask.isCancelled())
		{
			_scheduledFutureTimeTask.cancel(true);
		}
		finishEffects();
	}
	
	public void initializeEffects()
	{
		if ((_effected == null) || (_skill == null))
		{
			return;
		}
		
		// When effects are initialized, the successfully landed.
		if (!_hideStartMessage && _effected.isPlayer() && !_skill.isPassive() && !_skill.isAura())
		{
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S_EFFECT_CAN_BE_FELT);
			sm.addSkillName(_skill);
			_effected.sendPacket(sm);
		}
		
		// Creates a task that will stop all the effects.
		if (_abnormalTime > 0)
		{
			_scheduledFutureTimeTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new BuffTimeTask(this), 0, 1000L);
		}
		
		// Reset abnormal visual effects.
		resetAbnormalVisualEffects();
		
		for (AbstractEffect effect : _effects)
		{
			if (effect.isInstant() || (_effected.isDead() && !_skill.isPassive()))
			{
				continue;
			}
			
			// Call on start.
			effect.onStart(getEffector(), getEffected(), getSkill());
			effect.onStart(this);

			// If it's a continuous effect, if has ticks schedule a task with period, otherwise schedule a simple task to end it.
			if (effect.getTicks() > 0)
			{
				// The task for the effect ticks.
				final EffectTickTask effectTask = new EffectTickTask(this, effect);
				final ScheduledFuture<?> scheduledFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(effectTask, effect.getTicks() * Config.EFFECT_TICK_RATIO, effect.getTicks() * Config.EFFECT_TICK_RATIO);
				// Adds the task for ticking.
				addTask(effect, new EffectTaskInfo(effectTask, scheduledFuture));
			}
			
			// Add stats.
			_effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill));
		}
	}
	
	/**
	 * Resets the effects' duration.
	 * @return true if the debuff duration is reset and a visual update is needed.
	 */
	public boolean resetAbnormalTime()
	{
		if ((_effected == null) || (_skill == null))
		{
			return false;
		}
		
		// Check if the effect is continuous and has an active timer.
		if ((_abnormalTime > 0) && (_scheduledFutureTimeTask != null))
		{
			try
			{
				// In retail Reset skill doesn't exactly reset to the bare milisecond. If a second is ticking, its counted.
				// Thats why here its the same way. It just sets the elapsed time to 0, but including the seconds' tick time before it was reset.
				((BuffTimeTask) _scheduledFutureTimeTask.get()).setElapsedTime(0);
				_periodStartTicks = GameTimeController.getInstance().getGameTicks();
				return true;
			}
			catch (InterruptedException | ExecutionException e)
			{
				_log.warn("Task interrupted while resetting the abnormal time. ", e);
			}
		}
		
		return false;
	}
	
	/**
	 * Called on each tick.<br>
	 * Verify if the effect should end and the effect task should be cancelled.
	 * @param effect the effect that is ticking
	 * @param tickCount the tick count
	 */
	public void onTick(AbstractEffect effect, int tickCount)
	{
		boolean continueForever = false;
		// If the effect is in use, allow it to affect the effected.
		if (_isInUse)
		{
			// Callback for on action time event.
			continueForever = effect.onActionTime(this);
		}
		
		if (!continueForever && _skill.isToggle())
		{
			final EffectTaskInfo task = getEffectTask(effect);
			if (task != null)
			{
				task.getScheduledFuture().cancel(true); // Don't allow to finish current run.
				_effected.getEffectList().stopSkillEffects(true, getSkill()); // Remove the buff from the effect list.
			}
		}
	}
	
	public void finishEffects()
	{
		// Cancels the ticking task.
		if (_tasks != null)
		{
			for (EffectTaskInfo effectTask : _tasks.values())
			{
				effectTask.getScheduledFuture().cancel(true); // Don't allow to finish current run.
			}
		}
		// Remove stats
		removeStats();
		// Notify on exit.
		for (AbstractEffect effect : _effects)
		{
			// Instant effects shouldn't call onExit(..).
			if (!effect.isInstant())
			{
				effect.onExit(this);
			}
		}
		// Remove abnormal visual effects.
		resetAbnormalVisualEffects();
		// Set the proper system message.
		if (!(_effected.isSummon() && !((Summon) _effected).getOwner().hasSummon()) && !_skill.isAura())
		{
			SystemMessageId smId = null;
			if (_skill.isToggle())
			{
				smId = SystemMessageId.S1_HAS_BEEN_ABORTED;
			}
			else if (isRemoved())
			{
				smId = SystemMessageId.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED;
			}
			else if (!_skill.isPassive())
			{
				smId = SystemMessageId.S1_HAS_WORN_OFF;
			}
			
			if (smId != null)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(smId);
				sm.addSkillName(_skill);
				_effected.sendPacket(sm);
			}
		}
		// Remove short buff.
		if (this == _effected.getEffectList().getShortBuff())
		{
			_effected.getEffectList().shortBuffStatusUpdate(null);
		}
	}
	
	/**
	 * Applies all the abnormal visual effects to the effected.<br>
	 * Prevents multiple updates.
	 */
	private void resetAbnormalVisualEffects()
	{
		if (_skill.hasAbnormalVisualEffects())
		{
			_effected.resetCurrentAbnormalVisualEffects();
		}
	}
	
	/**
	 * Adds the buff stats.
	 */
	public void addStats()
	{
		_effects.forEach(effect -> _effected.addStatFuncs(effect.getStatFuncs(_effector, _effected, _skill)));
	}
	
	/**
	 * Removes the buff stats.
	 */
	public void removeStats()
	{
		_effects.forEach(_effected::removeStatsOwner);
		_effected.removeStatsOwner(_skill);
	}
	
	/**
	 * Gets the effect tick count.
	 * @param effect the effect
	 * @return the current tick count
	 */
	public int getTickCount(AbstractEffect effect)
	{
		if (_tasks != null)
		{
			final EffectTaskInfo effectTaskInfo = _tasks.get(effect);
			if (effectTaskInfo != null)
			{
				return effectTaskInfo.getEffectTask().getTickCount();
			}
		}
		return 0;
	}
}
