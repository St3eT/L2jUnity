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
package org.l2junity.gameserver.model.entity.clanhall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2junity.Config;
import org.l2junity.DatabaseFactory;
import org.l2junity.gameserver.ThreadPoolManager;
import org.l2junity.gameserver.data.sql.impl.ClanTable;
import org.l2junity.gameserver.enums.ChatType;
import org.l2junity.gameserver.enums.SiegeClanType;
import org.l2junity.gameserver.instancemanager.CHSiegeManager;
import org.l2junity.gameserver.instancemanager.MapRegionManager;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.L2Spawn;
import org.l2junity.gameserver.model.Location;
import org.l2junity.gameserver.model.SiegeClan;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.actor.Npc;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.entity.Siegable;
import org.l2junity.gameserver.model.quest.Quest;
import org.l2junity.gameserver.network.client.send.NpcSay;
import org.l2junity.gameserver.network.client.send.SystemMessage;
import org.l2junity.gameserver.network.client.send.string.NpcStringId;
import org.l2junity.gameserver.network.client.send.string.SystemMessageId;
import org.l2junity.gameserver.util.Broadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author BiggBoss
 */
public abstract class ClanHallSiegeEngine extends Quest implements Siegable
{
	private static final String SQL_LOAD_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
	private static final String SQL_SAVE_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
	private static final String SQL_LOAD_GUARDS = "SELECT * FROM clanhall_siege_guards WHERE clanHallId = ?";
	
	public static final int FORTRESS_RESSISTANCE = 21;
	public static final int DEVASTATED_CASTLE = 34;
	public static final int BANDIT_STRONGHOLD = 35;
	public static final int RAINBOW_SPRINGS = 62;
	public static final int BEAST_FARM = 63;
	public static final int FORTRESS_OF_DEAD = 64;
	
	protected final Logger _log = LoggerFactory.getLogger(getClass());
	
	private final Map<Integer, SiegeClan> _attackers = new ConcurrentHashMap<>();
	private Collection<L2Spawn> _guards;
	
	public SiegableHall _hall;
	public ScheduledFuture<?> _siegeTask;
	public boolean _missionAccomplished = false;
	
	public ClanHallSiegeEngine(String name, String descr, final int hallId)
	{
		super(-1, name, descr);

		_hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
		_hall.setSiege(this);
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		_log.info(_hall.getName() + " siege scheduled for: " + getSiegeDate().getTime());
		loadAttackers();
	}
	
	// XXX Load methods -------------------------------
	
	public void loadAttackers()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_LOAD_ATTACKERS))
		{
			statement.setInt(1, _hall.getId());
			try (ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("attacker_id");
					SiegeClan clan = new SiegeClan(id, SiegeClanType.ATTACKER);
					_attackers.put(id, clan);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn(getName() + ": Could not load siege attackers!:");
		}
	}
	
	public final void saveAttackers()
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement delStatement = con.prepareStatement("DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?"))
		{
			delStatement.setInt(1, _hall.getId());
			delStatement.execute();
			
			if (getAttackers().size() > 0)
			{
				try (PreparedStatement insert = con.prepareStatement(SQL_SAVE_ATTACKERS))
				{
					for (SiegeClan clan : getAttackers().values())
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.execute();
						insert.clearParameters();
					}
				}
			}
			_log.info(getName() + ": Sucessfully saved attackers down to database!");
		}
		catch (Exception e)
		{
			_log.warn(getName() + ": Couldnt save attacker list!");
		}
	}
	
	public final void loadGuards()
	{
		if (_guards == null)
		{
			_guards = new LinkedList<>();
			try (Connection con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(SQL_LOAD_GUARDS))
			{
				statement.setInt(1, _hall.getId());
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						final L2Spawn spawn = new L2Spawn(rset.getInt("npcId"));
						spawn.setX(rset.getInt("x"));
						spawn.setY(rset.getInt("y"));
						spawn.setZ(rset.getInt("z"));
						spawn.setHeading(rset.getInt("heading"));
						spawn.setRespawnDelay(rset.getInt("respawnDelay"));
						spawn.setAmount(1);
						_guards.add(spawn);
					}
				}
			}
			catch (Exception e)
			{
				_log.warn(getName() + ": Couldnt load siege guards!:");
			}
		}
	}
	
	// XXX Npc Management methods ----------------------------
	
	private void spawnSiegeGuards()
	{
		for (L2Spawn guard : _guards)
		{
			if (guard != null)
			{
				guard.init();
			}
		}
	}
	
	private void unSpawnSiegeGuards()
	{
		if ((_guards != null) && (_guards.size() > 0))
		{
			for (L2Spawn guard : _guards)
			{
				if (guard != null)
				{
					guard.stopRespawn();
					if (guard.getLastSpawn() != null)
					{
						guard.getLastSpawn().deleteMe();
					}
				}
			}
		}
	}
	
	@Override
	public Set<Npc> getFlag(L2Clan clan)
	{
		Set<Npc> result = null;
		SiegeClan sClan = getAttackerClan(clan);
		if (sClan != null)
		{
			result = sClan.getFlag();
		}
		return result;
	}
	
	// XXX Attacker clans management -----------------------------
	
	public final Map<Integer, SiegeClan> getAttackers()
	{
		return _attackers;
	}
	
	@Override
	public boolean checkIsAttacker(L2Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		
		return _attackers.containsKey(clan.getId());
	}
	
	@Override
	public boolean checkIsDefender(L2Clan clan)
	{
		return false;
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		return _attackers.get(clanId);
	}
	
	@Override
	public SiegeClan getAttackerClan(L2Clan clan)
	{
		return getAttackerClan(clan.getId());
	}
	
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		return Collections.unmodifiableCollection(_attackers.values());
	}
	
	@Override
	public List<PlayerInstance> getAttackersInZone()
	{
		final Collection<PlayerInstance> list = _hall.getSiegeZone().getPlayersInside();
		final List<PlayerInstance> attackers = new LinkedList<>();
		for (PlayerInstance pc : list)
		{
			final L2Clan clan = pc.getClan();
			if ((clan != null) && getAttackers().containsKey(clan.getId()))
			{
				attackers.add(pc);
			}
		}
		
		return attackers;
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}
	
	@Override
	public SiegeClan getDefenderClan(L2Clan clan)
	{
		return null;
	}
	
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		return null;
	}
	
	// XXX Siege execution --------------------------
	
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			final SiegeClan clan = new SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
			getAttackers().put(clan.getClanId(), new SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
		}
		
		_hall.free();
		_hall.banishForeigners();
		SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
		msg.addString(getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if ((getAttackers().size() < 1) && (_hall.getId() != 21)) // Fortress of resistance dont have attacker list
		{
			onSiegeEnds();
			getAttackers().clear();
			_hall.updateNextSiege();
			_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getSiegeDate().getTimeInMillis());
			_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		_hall.spawnDoor();
		loadGuards();
		spawnSiegeGuards();
		_hall.updateSiegeZone(true);
		
		final byte state = 1;
		for (SiegeClan sClan : getAttackerClans())
		{
			final L2Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (PlayerInstance pc : clan.getOnlineMembers(0))
			{
				if (pc != null)
				{
					pc.setSiegeState(state);
					pc.broadcastUserInfo();
					pc.setIsInHideoutSiege(true);
				}
			}
		}
		
		_hall.updateSiegeStatus(SiegeStatus.RUNNING);
		onSiegeStarts();
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEnds(), _hall.getSiegeLenght());
	}
	
	@Override
	public void endSiege()
	{
		SystemMessage end = SystemMessage.getSystemMessage(SystemMessageId.THE_S1_SIEGE_HAS_FINISHED);
		end.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(end);
		
		L2Clan winner = getWinner();
		SystemMessage finalMsg = null;
		if (_missionAccomplished && (winner != null))
		{
			_hall.setOwner(winner);
			winner.setHideoutId(_hall.getId());
			finalMsg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
			finalMsg.addString(winner.getName());
			finalMsg.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(finalMsg);
		}
		else
		{
			finalMsg = SystemMessage.getSystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
			finalMsg.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(finalMsg);
		}
		_missionAccomplished = false;
		
		_hall.updateSiegeZone(false);
		_hall.updateNextSiege();
		_hall.spawnDoor(false);
		_hall.banishForeigners();
		
		final byte state = 0;
		for (SiegeClan sClan : getAttackerClans())
		{
			final L2Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (PlayerInstance player : clan.getOnlineMembers(0))
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
				player.setIsInHideoutSiege(false);
			}
		}
		
		// Update pvp flag for winners when siege zone becomes inactive
		for (Creature chr : _hall.getSiegeZone().getCharactersInside())
		{
			if ((chr != null) && chr.isPlayer())
			{
				chr.getActingPlayer().startPvPFlag();
			}
		}
		
		getAttackers().clear();
		
		onSiegeEnds();
		
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		_log.info("Siege of " + _hall.getName() + " scheduled for: " + _hall.getSiegeDate().getTime());
		
		_hall.updateSiegeStatus(SiegeStatus.REGISTERING);
		unSpawnSiegeGuards();
	}
	
	@Override
	public void updateSiege()
	{
		cancelSiegeTask();
		_siegeTask = ThreadPoolManager.getInstance().scheduleGeneral(new PrepareOwner(), _hall.getNextSiegeTime() - 3600000);
		_log.info(_hall.getName() + " siege scheduled for: " + _hall.getSiegeDate().getTime().toString());
	}
	
	public void cancelSiegeTask()
	{
		if (_siegeTask != null)
		{
			_siegeTask.cancel(false);
		}
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return _hall.getSiegeDate();
	}
	
	// XXX Fame settings ---------------------------
	
	@Override
	public boolean giveFame()
	{
		return Config.CHS_ENABLE_FAME;
	}
	
	@Override
	public int getFameAmount()
	{
		return Config.CHS_FAME_AMOUNT;
	}
	
	// XXX Misc methods -----------------------------
	
	@Override
	public int getFameFrequency()
	{
		return Config.CHS_FAME_FREQUENCY;
	}
	
	public final void broadcastNpcSay(final Npc npc, final ChatType type, final NpcStringId messageId)
	{
		final NpcSay npcSay = new NpcSay(npc, type, messageId);
		final int sourceRegion = MapRegionManager.getInstance().getMapRegionLocId(npc);
		for (PlayerInstance pc : World.getInstance().getPlayers())
		{
			if ((pc != null) && (MapRegionManager.getInstance().getMapRegionLocId(pc) == sourceRegion))
			{
				pc.sendPacket(npcSay);
			}
		}
	}
	
	// XXX Siege task and abstract methods -------------------
	public Location getInnerSpawnLoc(PlayerInstance player)
	{
		return null;
	}
	
	public boolean canPlantFlag()
	{
		return true;
	}
	
	public boolean doorIsAutoAttackable()
	{
		return true;
	}
	
	public void onSiegeStarts()
	{
	}
	
	public void onSiegeEnds()
	{
	}
	
	public abstract L2Clan getWinner();
	
	public class PrepareOwner implements Runnable
	{
		@Override
		public void run()
		{
			prepareOwner();
		}
	}
	
	public class SiegeStarts implements Runnable
	{
		@Override
		public void run()
		{
			startSiege();
		}
	}
	
	public class SiegeEnds implements Runnable
	{
		@Override
		public void run()
		{
			endSiege();
		}
	}
}
