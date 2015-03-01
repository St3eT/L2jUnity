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

import org.l2junity.gameserver.instancemanager.CastleManager;
import org.l2junity.gameserver.model.entity.Castle;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.serverpackets.SiegeDefenderList;
import org.l2junity.network.PacketReader;

/**
 * This class ...
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestSiegeDefenderList implements IGameClientPacket
{
	private int _castleId;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_castleId = packet.readD();
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle == null)
		{
			return;
		}
		client.sendPacket(new SiegeDefenderList(castle));
	}
}
