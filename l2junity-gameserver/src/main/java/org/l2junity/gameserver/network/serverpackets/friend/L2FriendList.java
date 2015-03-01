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
package org.l2junity.gameserver.network.serverpackets.friend;

import java.util.List;

import javolution.util.FastList;

import org.l2junity.gameserver.data.sql.impl.CharNameTable;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.serverpackets.IGameServerPacket;
import org.l2junity.network.PacketWriter;

/**
 * Support for "Chat with Friends" dialog. <br />
 * This packet is sent only at login.
 * @author Tempy
 */
public class L2FriendList implements IGameServerPacket
{
	private final List<FriendInfo> _info;
	
	private static class FriendInfo
	{
		int _objId;
		String _name;
		int _level;
		int _classId;
		boolean _online;
		
		public FriendInfo(int objId, String name, boolean online, int level, int classId)
		{
			_objId = objId;
			_name = name;
			_online = online;
			_level = level;
			_classId = classId;
		}
	}
	
	public L2FriendList(PlayerInstance player)
	{
		_info = new FastList<>(player.getFriendList().size());
		for (int objId : player.getFriendList())
		{
			String name = CharNameTable.getInstance().getNameById(objId);
			PlayerInstance player1 = World.getInstance().getPlayer(objId);
			boolean online = false;
			int level = 0;
			int classId = 0;
			
			if (player1 != null)
			{
				online = true;
				level = player1.getLevel();
				classId = player1.getClassId().getId();
			}
			else
			{
				level = CharNameTable.getInstance().getLevelById(objId);
				classId = CharNameTable.getInstance().getClassIdById(objId);
			}
			_info.add(new FriendInfo(objId, name, online, level, classId));
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.L2_FRIEND_LIST.writeId(packet);
		
		packet.writeD(_info.size());
		for (FriendInfo info : _info)
		{
			packet.writeD(info._objId); // character id
			packet.writeS(info._name);
			packet.writeD(info._online ? 0x01 : 0x00); // online
			packet.writeD(info._online ? info._objId : 0x00); // object id if online
			packet.writeD(info._level);
			packet.writeD(info._classId);
			packet.writeH(0x00);
		}
		return true;
	}
}