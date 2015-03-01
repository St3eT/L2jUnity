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

import org.l2junity.Config;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author Sdw
 */
public class ExVitalityEffectInfo implements IGameServerPacket
{
	private final int _vitalityBonus;
	private final int _vitalityItemsRemaining;
	private final int _points;
	
	public ExVitalityEffectInfo(PlayerInstance cha)
	{
		_points = cha.getVitalityPoints();
		_vitalityBonus = (int) cha.getStat().getVitalityExpBonus() * 100;
		_vitalityItemsRemaining = cha.getVitalityItemsUsed() - Config.VITALITY_MAX_ITEMS_ALLOWED;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_VITALITY_EFFECT_INFO.writeId(packet);
		
		packet.writeD(_points);
		packet.writeD(_vitalityBonus); // Vitality Bonus
		packet.writeH(_vitalityItemsRemaining); // How much vitality items remaining for use
		packet.writeH(Config.VITALITY_MAX_ITEMS_ALLOWED); // Max number of items for use
		return true;
	}
}