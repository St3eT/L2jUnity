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

import org.l2junity.Config;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author -Wooden-, Sdw
 */
public class ExPledgeEmblem implements IGameServerPacket
{
	private final int _crestId;
	private final int _clanId;
	private final byte[] _data;
	private final int _chunkId;
	private static final int TOTAL_SIZE = 65664;
	
	public ExPledgeEmblem(int crestId, byte[] chunkedData, int clanId, int chunkId)
	{
		_crestId = crestId;
		_data = chunkedData;
		_clanId = clanId;
		_chunkId = chunkId;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_PLEDGE_EMBLEM.writeId(packet);
		
		packet.writeD(Config.SERVER_ID);
		packet.writeD(_clanId);
		packet.writeD(_crestId);
		packet.writeD(_chunkId);
		packet.writeD(TOTAL_SIZE);
		if (_data != null)
		{
			packet.writeD(_data.length);
			packet.writeB(_data);
		}
		else
		{
			packet.writeD(0);
		}
		return true;
	}
}