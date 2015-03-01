/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.serverpackets;

import java.util.List;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author mrTJO
 */
public class ExCubeGameTeamList implements IGameServerPacket
{
	// Players Lists
	private final List<PlayerInstance> _bluePlayers;
	private final List<PlayerInstance> _redPlayers;
	
	// Common Values
	private final int _roomNumber;
	
	/**
	 * Show Minigame Waiting List to Player
	 * @param redPlayers Red Players List
	 * @param bluePlayers Blue Players List
	 * @param roomNumber Arena/Room ID
	 */
	public ExCubeGameTeamList(List<PlayerInstance> redPlayers, List<PlayerInstance> bluePlayers, int roomNumber)
	{
		_redPlayers = redPlayers;
		_bluePlayers = bluePlayers;
		_roomNumber = roomNumber - 1;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_BLOCK_UP_SET_LIST.writeId(packet);
		
		packet.writeD(0x00);
		
		packet.writeD(_roomNumber);
		packet.writeD(0xffffffff);
		
		packet.writeD(_bluePlayers.size());
		for (PlayerInstance player : _bluePlayers)
		{
			packet.writeD(player.getObjectId());
			packet.writeS(player.getName());
		}
		packet.writeD(_redPlayers.size());
		for (PlayerInstance player : _redPlayers)
		{
			packet.writeD(player.getObjectId());
			packet.writeS(player.getName());
		}
		return true;
	}
}
