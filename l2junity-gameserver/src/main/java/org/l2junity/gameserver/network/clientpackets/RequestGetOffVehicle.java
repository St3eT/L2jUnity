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
package org.l2junity.gameserver.network.clientpackets;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.serverpackets.ActionFailed;
import org.l2junity.gameserver.network.serverpackets.GetOffVehicle;
import org.l2junity.gameserver.network.serverpackets.StopMoveInVehicle;
import org.l2junity.network.PacketReader;

/**
 * @author Maktakien
 */
public final class RequestGetOffVehicle implements IGameClientPacket
{
	private int _boatId, _x, _y, _z;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_boatId = packet.readD();
		_x = packet.readD();
		_y = packet.readD();
		_z = packet.readD();
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
		if (!activeChar.isInBoat() || (activeChar.getBoat().getObjectId() != _boatId) || activeChar.getBoat().isMoving() || !activeChar.isInsideRadius(_x, _y, _z, 1000, true, false))
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		activeChar.broadcastPacket(new StopMoveInVehicle(activeChar, _boatId));
		activeChar.setVehicle(null);
		activeChar.setInVehiclePosition(null);
		client.sendPacket(ActionFailed.STATIC_PACKET);
		activeChar.broadcastPacket(new GetOffVehicle(activeChar.getObjectId(), _boatId, _x, _y, _z));
		activeChar.setXYZ(_x, _y, _z);
		activeChar.setInsideZone(ZoneId.PEACE, false);
		activeChar.revalidateZone(true);
	}
}
