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
package org.l2junity.gameserver.model;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2junity.Config;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.events.EventDispatcher;
import org.l2junity.gameserver.model.events.impl.clan.OnClanWarStart;
import org.l2junity.gameserver.network.client.send.SurrenderPledgeWar;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;

/**
 * @author Sdw
 */
public final class ClanWar
{
	public static final long TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR = TimeUnit.DAYS.toMillis(7);
	public static final long TIME_TO_DELETION_AFTER_CANCELLATION = TimeUnit.DAYS.toMillis(5);
	public static final long TIME_TO_DELETION_AFTER_DEFEAT = TimeUnit.DAYS.toMillis(21);
	private final int _attackerClanId;
	private final int _attackedClanId;
	private int _winnerClanId = 0;
	private ClanWarState _state;
	private Future<?> _cancelTask;
	private final long _startTime;
	private long _endTime = 0;
	
	private final AtomicInteger _attackerKillCount = new AtomicInteger();
	private final AtomicInteger _attackedKillCount = new AtomicInteger();
	
	public ClanWar(L2Clan attacker, L2Clan attacked)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = System.currentTimeMillis();
		_state = ClanWarState.BLOOD_DECLARATION;
		
		_cancelTask = ThreadPoolManager.getInstance().scheduleGeneral(() ->
		{
			clanWarTimeout();
		}, (_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis());
		
		attacker.addWar(attacked.getId(), this);
		attacked.addWar(attacker.getId(), this);
		
		EventDispatcher.getInstance().notifyEventAsync(new OnClanWarStart(attacker, attacked));
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_DECLARED_A_CLAN_WAR_WITH_S1);
		sm.addString(attacked.getName());
		attacker.broadcastToOnlineMembers(sm);
		
		sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_DECLARED_A_CLAN_WAR_THE_WAR_WILL_AUTOMATICALLY_START_IF_YOU_KILL_S1_CLAN_MEMBERS_5_TIMES_WITHIN_A_WEEK);
		sm.addString(attacker.getName());
		attacked.broadcastToOnlineMembers(sm);
	}
	
	public ClanWar(L2Clan attacker, L2Clan attacked, int attackerKillCount, int attackedKillCount, int winnerClan, long startTime, long endTime, ClanWarState state)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = startTime;
		_endTime = endTime;
		_state = state;
		_attackerKillCount.set(attackerKillCount);
		_attackedKillCount.set(attackedKillCount);
		_winnerClanId = winnerClan;
		
		if ((_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) > System.currentTimeMillis())
		{
			_cancelTask = ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				clanWarTimeout();
			}, (_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis());
		}
		
		if (_endTime > 0)
		{
			final long endTimePeriod = _endTime + (_state == ClanWarState.TIE ? TIME_TO_DELETION_AFTER_CANCELLATION : TIME_TO_DELETION_AFTER_DEFEAT);
			
			if (endTimePeriod > System.currentTimeMillis())
			{
				ClanTable.getInstance().deleteclanswars(_attackerClanId, _attackedClanId);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleGeneral(() ->
				{
					ClanTable.getInstance().deleteclanswars(_attackerClanId, _attackedClanId);
				}, endTimePeriod);
			}
		}
	}
	
	public void onKill(PlayerInstance killer, PlayerInstance victim)
	{
		final L2Clan victimClan = victim.getClan();
		final L2Clan killerClan = killer.getClan();
		
		// Reputation increase by killing an enemy (over level 4) in a clan war under the condition of mutual war declaration
		if ((victim.getLevel() > 4) && (_state == ClanWarState.MUTUAL))
		{
			// however, when the other side reputation score is 0 or below, your clan cannot acquire any reputation points from them.
			if (victimClan.getReputationScore() > 0)
			{
				victimClan.takeReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
				killerClan.addReputationScore(Config.REPUTATION_SCORE_PER_KILL, false);
			}
			
			// System Message notification to clan members
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.BECAUSE_C1_WAS_KILLED_BY_A_CLAN_MEMBER_OF_S2_CLAN_REPUTATION_DECREASED_BY_1);
			sm.addPcName(victim);
			sm.addString(killerClan.getName());
			victimClan.broadcastToOtherOnlineMembers(sm, victim);
			
			sm = SystemMessage.getSystemMessage(SystemMessageId.BECAUSE_A_CLAN_MEMBER_OF_S1_WAS_KILLED_BY_C2_CLAN_REPUTATION_INCREASED_BY_1);
			sm.addString(victimClan.getName());
			sm.addPcName(killer);
			killerClan.broadcastToOtherOnlineMembers(sm, killer);
			
			if (killerClan.getId() == _attackerClanId)
			{
				_attackerKillCount.incrementAndGet();
			}
			else
			{
				_attackedKillCount.incrementAndGet();
			}
		}
		else if ((_state == ClanWarState.BLOOD_DECLARATION) && (victimClan.getId() == _attackerClanId))
		{
			final int killCount = _attackedKillCount.incrementAndGet();
			
			if (killCount >= 5)
			{
				_state = ClanWarState.MUTUAL;
				
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_5_000_CLAN_REPUTATION_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_THE_HUNTING_GROUND);
				sm.addString(victimClan.getName());
				killerClan.broadcastToOnlineMembers(sm);
				
				sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_5_000_CLAN_REPUTATION_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_THE_HUNTING_GROUND);
				sm.addString(killerClan.getName());
				victimClan.broadcastToOnlineMembers(sm);
				
				if (_cancelTask != null)
				{
					_cancelTask.cancel(true);
					_cancelTask = null;
				}
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_MEMBER_OF_S1_WAS_KILLED_BY_YOUR_CLAN_MEMBER_IF_YOUR_CLAN_KILLS_S2_MEMBERS_OF_CLAN_S1_A_CLAN_WAR_WITH_CLAN_S1_WILL_START);
				sm.addString(victimClan.getName());
				sm.addInt(5 - killCount);
				killerClan.broadcastToOnlineMembers(sm);
			}
		}
	}
	
	public void cancel(PlayerInstance player, L2Clan cancelor)
	{
		final L2Clan winnerClan = cancelor.getId() == _attackerClanId ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
		
		if (cancelor.getReputationScore() > 5000)
		{
			cancelor.takeReputationScore(5000, true);
			
			player.sendPacket(new SurrenderPledgeWar(cancelor.getName(), player.getName()));
			
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_WAR_ENDED_BY_YOUR_DEFEAT_DECLARATION_WITH_THE_S1_CLAN);
			sm.addString(winnerClan.getName());
			cancelor.broadcastToOnlineMembers(sm);
			
			sm = SystemMessage.getSystemMessage(SystemMessageId.THE_WAR_ENDED_BY_THE_S1_CLAN_S_DEFEAT_DECLARATION_YOU_HAVE_WON_THE_CLAN_WAR_OVER_THE_S1_CLAN);
			sm.addString(cancelor.getName());
			winnerClan.broadcastToOnlineMembers(sm);
			
			_winnerClanId = winnerClan.getId();
			_endTime = System.currentTimeMillis();
			
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				ClanTable.getInstance().deleteclanswars(cancelor.getId(), winnerClan.getId());
			}, (_endTime + TIME_TO_DELETION_AFTER_DEFEAT) - System.currentTimeMillis());
		}
	}
	
	public void clanWarTimeout()
	{
		final L2Clan attackerClan = ClanTable.getInstance().getClan(_attackerClanId);
		final L2Clan attackedClan = ClanTable.getInstance().getClan(_attackedClanId);
		
		if ((attackerClan != null) && (attackedClan != null))
		{
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_WAR_DECLARED_BY_CLAN_S1_WAS_CANCELLED);
			sm.addString(attackerClan.getName());
			attackedClan.broadcastToOnlineMembers(sm);
			
			sm = SystemMessage.getSystemMessage(SystemMessageId.BECAUSE_CLAN_S1_DID_NOT_FIGHT_BACK_FOR_1_WEEK_THE_CLAN_WAR_WAS_CANCELLED);
			sm.addString(attackedClan.getName());
			attackerClan.broadcastToOnlineMembers(sm);
			
			_state = ClanWarState.TIE;
			_endTime = System.currentTimeMillis();
			
			ThreadPoolManager.getInstance().scheduleGeneral(() ->
			{
				ClanTable.getInstance().deleteclanswars(attackerClan.getId(), attackedClan.getId());
			}, (_endTime + TIME_TO_DELETION_AFTER_CANCELLATION) - System.currentTimeMillis());
		}
	}
	
	public void mutualClanWarAccepted(L2Clan attacker, L2Clan attacked)
	{
		_state = ClanWarState.MUTUAL;
		
		SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_5_000_CLAN_REPUTATION_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_THE_HUNTING_GROUND);
		sm.addString(attacker.getName());
		attacked.broadcastToOnlineMembers(sm);
		
		sm = SystemMessage.getSystemMessage(SystemMessageId.A_CLAN_WAR_WITH_CLAN_S1_HAS_STARTED_THE_CLAN_THAT_CANCELS_THE_WAR_FIRST_WILL_LOSE_5_000_CLAN_REPUTATION_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_GETS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_THE_HUNTING_GROUND);
		sm.addString(attacked.getName());
		attacker.broadcastToOnlineMembers(sm);
		
		if (_cancelTask != null)
		{
			_cancelTask.cancel(true);
			_cancelTask = null;
		}
	}
	
	public int getKillDifference(L2Clan clan)
	{
		return _attackerClanId == clan.getId() ? _attackerKillCount.get() - _attackedKillCount.get() : _attackedKillCount.get() - _attackerKillCount.get();
	}
	
	public ClanWarState getClanWarState(L2Clan clan)
	{
		if (_winnerClanId > 0)
		{
			return _winnerClanId == clan.getId() ? ClanWarState.WIN : ClanWarState.LOSS;
		}
		return _state;
	}
	
	public int getAttackerClanId()
	{
		return _attackerClanId;
	}
	
	public int getAttackedClanId()
	{
		return _attackedClanId;
	}
	
	public int getAttackerKillCount()
	{
		return _attackerKillCount.get();
	}
	
	public int getAttackedKillCount()
	{
		return _attackedKillCount.get();
	}
	
	public int getWinnerClanId()
	{
		return _winnerClanId;
	}
	
	public long getStartTime()
	{
		return _startTime;
	}
	
	public long getEndTime()
	{
		return _endTime;
	}
	
	public ClanWarState getState()
	{
		return _state;
	}
	
	public int getKillToStart()
	{
		return _state == ClanWarState.BLOOD_DECLARATION ? 5 - _attackedKillCount.get() : 0;
	}
	
	public int getRemainingTime()
	{
		return (int) TimeUnit.SECONDS.convert(_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR, TimeUnit.MILLISECONDS);
	}
	
	public L2Clan getOpposingClan(L2Clan clan)
	{
		return _attackerClanId == clan.getId() ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
	}
	
	public enum ClanWarState
	{
		DECLARATION,
		BLOOD_DECLARATION,
		MUTUAL,
		WIN,
		LOSS,
		TIE
	}
}
