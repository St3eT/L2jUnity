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

import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;
import org.l2junity.network.PacketReader;

/**
 * Format: (ch) S
 * @author -Wooden-, Tryskell
 */
public class RequestAskJoinPartyRoom implements IGameClientPacket
{
	private String _name;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_name = packet.readS();
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
		
		// Send PartyRoom invite request (with activeChar) name to the target
		final PlayerInstance target = World.getInstance().getPlayer(_name);
		if (target != null)
		{
			if (!target.isProcessingRequest())
			{
				player.onTransactionRequest(target);
				target.sendPacket(new ExAskJoinPartyRoom(player));
			}
			else
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER).addPcName(target));
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
		}
	}
}
