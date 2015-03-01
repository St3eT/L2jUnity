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
package org.l2junity.gameserver.network.serverpackets.appearance;

import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.serverpackets.IGameServerPacket;
import org.l2junity.network.PacketWriter;

/**
 * @author UnAfraid
 */
public class ExPutShapeShiftingTargetItemResult implements IGameServerPacket
{
	public static int RESULT_FAILED = 0x00;
	public static int RESULT_SUCCESS = 0x01;
	
	public static ExPutShapeShiftingTargetItemResult FAILED = new ExPutShapeShiftingTargetItemResult(RESULT_FAILED, 0L);
	
	private final int _resultId;
	private final long _price;
	
	public ExPutShapeShiftingTargetItemResult(int resultId, long price)
	{
		_resultId = resultId;
		_price = price;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_PUT_SHAPE_SHIFTING_TARGET_ITEM_RESULT.writeId(packet);
		
		packet.writeD(_resultId);
		packet.writeQ(_price);
		return true;
	}
}