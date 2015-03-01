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
package org.l2junity.gameserver.network.clientpackets.compound;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.actor.request.CompoundRequest;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.clientpackets.IGameClientPacket;
import org.l2junity.gameserver.network.serverpackets.compound.ExEnchantOneFail;
import org.l2junity.gameserver.network.serverpackets.compound.ExEnchantTwoRemoveFail;
import org.l2junity.gameserver.network.serverpackets.compound.ExEnchantTwoRemoveOK;
import org.l2junity.network.PacketReader;

/**
 * @author UnAfraid
 */
public class RequestNewEnchantRemoveTwo implements IGameClientPacket
{
	private int _objectId;
	
	@Override
	public boolean read(PacketReader packet)
	{
		_objectId = packet.readD();
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
		else if (activeChar.isInStoreMode())
		{
			client.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_IN_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			client.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		else if (activeChar.isProcessingTransaction() || activeChar.isProcessingRequest())
		{
			client.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			client.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		final CompoundRequest request = activeChar.getRequest(CompoundRequest.class);
		if ((request == null) || request.isProcessing())
		{
			client.sendPacket(ExEnchantTwoRemoveFail.STATIC_PACKET);
			return;
		}
		
		final ItemInstance item = request.getItemTwo();
		if ((item == null) || (item.getObjectId() != _objectId))
		{
			client.sendPacket(ExEnchantTwoRemoveFail.STATIC_PACKET);
			return;
		}
		request.setItemTwo(0);
		
		client.sendPacket(ExEnchantTwoRemoveOK.STATIC_PACKET);
	}
}
