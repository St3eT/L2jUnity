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

import java.util.Objects;

import org.l2junity.gameserver.model.CommandChannel;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author chris_00
 */
public class ExMultiPartyCommandChannelInfo implements IGameServerPacket
{
	private final CommandChannel _channel;
	
	public ExMultiPartyCommandChannelInfo(CommandChannel channel)
	{
		Objects.requireNonNull(channel);
		_channel = channel;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_MULTI_PARTY_COMMAND_CHANNEL_INFO.writeId(packet);
		
		packet.writeS(_channel.getLeader().getName());
		packet.writeD(0x00); // Channel loot 0 or 1
		packet.writeD(_channel.getMemberCount());
		
		packet.writeD(_channel.getPartys().size());
		for (Party p : _channel.getPartys())
		{
			packet.writeS(p.getLeader().getName());
			packet.writeD(p.getLeaderObjectId());
			packet.writeD(p.getMemberCount());
		}
		return true;
	}
}
