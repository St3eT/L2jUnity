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
package org.l2junity.gameserver.network.serverpackets.adenadistribution;

import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.serverpackets.IGameServerPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author Sdw
 */
public class ExDivideAdenaCancel implements IGameServerPacket
{
	public static final ExDivideAdenaCancel STATIC_PACKET = new ExDivideAdenaCancel();
	
	private ExDivideAdenaCancel()
	{
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_DIVIDE_ADENA_CANCEL.writeId(packet);
		
		packet.writeC(0x00); // TODO: Find me
		return true;
	}
}
