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
package org.l2junity.gameserver.network.clientpackets.adenadistribution;

import java.util.Objects;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.request.AdenaDistributionRequest;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.clientpackets.IGameClientPacket;
import org.l2junity.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaCancel;
import org.l2junity.network.PacketReader;

/**
 * @author Sdw
 */
public class RequestDivideAdenaCancel implements IGameClientPacket
{
	private boolean _cancel;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_cancel = packet.readC() == 0;
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		final PlayerInstance player = client.getActiveChar();
		if (player == null)
		{
			return;
		}
		
		if (_cancel)
		{
			final AdenaDistributionRequest request = player.getRequest(AdenaDistributionRequest.class);
			request.getPlayers().stream().filter(Objects::nonNull).forEach(p ->
			{
				p.sendPacket(SystemMessageId.ADENA_DISTRIBUTION_HAS_BEEN_CANCELLED);
				p.sendPacket(ExDivideAdenaCancel.STATIC_PACKET);
				p.removeRequest(AdenaDistributionRequest.class);
			});
		}
	}
}
