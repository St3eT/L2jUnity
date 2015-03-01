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
package org.l2junity.gameserver.network.clientpackets.mentoring;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.clientpackets.IGameClientPacket;
import org.l2junity.gameserver.network.serverpackets.mentoring.ListMenteeWaiting;
import org.l2junity.network.PacketReader;

/**
 * @author UnAfraid
 */
public class RequestMenteeWaitingList implements IGameClientPacket
{
	private int _page;
	private int _minLevel;
	private int _maxLevel;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_page = packet.readD();
		_minLevel = packet.readD();
		_maxLevel = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		client.sendPacket(new ListMenteeWaiting(_page, _minLevel, _maxLevel));
	}
}
