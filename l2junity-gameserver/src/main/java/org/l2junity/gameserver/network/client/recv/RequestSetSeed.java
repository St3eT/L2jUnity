/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.network.client.recv;

import java.util.ArrayList;
import java.util.List;

import org.l2junity.Config;
import org.l2junity.gameserver.instancemanager.CastleManorManager;
import org.l2junity.gameserver.model.ClanPrivilege;
import org.l2junity.gameserver.model.L2Seed;
import org.l2junity.gameserver.model.SeedProduction;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.client.L2GameClient;
import org.l2junity.gameserver.network.client.send.ActionFailed;
import org.l2junity.network.PacketReader;

/**
 * @author l3x
 */
public class RequestSetSeed implements IClientIncomingPacket
{
	private static final int BATCH_LENGTH = 20; // length of the one item
	
	private int _manorId;
	private List<SeedProduction> _items;
	
	@Override
	public boolean read(L2GameClient client, PacketReader packet)
	{
		_manorId = packet.readD();
		final int count = packet.readD();
		if ((count <= 0) || (count > Config.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != packet.getReadableBytes()))
		{
			return false;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = packet.readD();
			final long sales = packet.readQ();
			final long price = packet.readQ();
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return false;
			}
			
			if (sales > 0)
			{
				_items.add(new SeedProduction(itemId, sales, price, sales));
			}
		}
		return true;
	}
	
	@Override
	public void run(L2GameClient client)
	{
		if (_items.isEmpty())
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (!manor.isModifiablePeriod())
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check player privileges
		final PlayerInstance player = client.getActiveChar();
		if ((player == null) || (player.getClan() == null) || (player.getClan().getCastleId() != _manorId) || !player.hasClanPrivilege(ClanPrivilege.CS_MANOR_ADMIN) || !player.getLastFolkNPC().canInteract(player))
		{
			client.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Filter seeds with start amount lower than 0 and incorrect price
		final List<SeedProduction> list = new ArrayList<>(_items.size());
		for (SeedProduction sp : _items)
		{
			final L2Seed s = manor.getSeed(sp.getId());
			if ((s != null) && (sp.getStartAmount() <= s.getSeedLimit()) && (sp.getPrice() >= s.getSeedMinPrice()) && (sp.getPrice() <= s.getSeedMaxPrice()))
			{
				list.add(sp);
			}
		}
		
		// Save new list
		manor.setNextSeedProduction(list, _manorId);
	}
	
}