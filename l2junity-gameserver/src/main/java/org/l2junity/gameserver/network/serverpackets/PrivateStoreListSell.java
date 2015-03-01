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

import org.l2junity.gameserver.model.TradeItem;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public class PrivateStoreListSell extends AbstractItemPacket
{
	private final int _objId;
	private final long _playerAdena;
	private final boolean _packageSale;
	private final TradeItem[] _items;
	
	public PrivateStoreListSell(PlayerInstance player, PlayerInstance storePlayer)
	{
		_objId = storePlayer.getObjectId();
		_playerAdena = player.getAdena();
		_items = storePlayer.getSellList().getItems();
		_packageSale = storePlayer.getSellList().isPackaged();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.PRIVATE_STORE_LIST.writeId(packet);
		
		packet.writeD(_objId);
		packet.writeD(_packageSale ? 1 : 0);
		packet.writeQ(_playerAdena);
		packet.writeD(0x00);
		packet.writeD(_items.length);
		for (TradeItem item : _items)
		{
			writeItem(packet, item);
			packet.writeQ(item.getPrice());
			packet.writeQ(item.getItem().getReferencePrice() * 2);
		}
		return true;
	}
}
