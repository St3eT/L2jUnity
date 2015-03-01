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

import java.util.List;

import org.l2junity.gameserver.enums.MailType;
import org.l2junity.gameserver.instancemanager.MailManager;
import org.l2junity.gameserver.model.entity.Message;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.network.PacketWriter;

/**
 * @author Migi, DS
 */
public class ExShowReceivedPostList implements IGameServerPacket
{
	private final List<Message> _inbox;
	
	public ExShowReceivedPostList(int objectId)
	{
		_inbox = MailManager.getInstance().getInbox(objectId);
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_SHOW_RECEIVED_POST_LIST.writeId(packet);
		
		packet.writeD((int) (System.currentTimeMillis() / 1000));
		if ((_inbox != null) && (_inbox.size() > 0))
		{
			packet.writeD(_inbox.size());
			for (Message msg : _inbox)
			{
				packet.writeD(msg.getMailType().ordinal());
				if (msg.getMailType() == MailType.COMMISSION_ITEM_SOLD)
				{
					packet.writeD(SystemMessageId.THE_ITEM_YOU_REGISTERED_HAS_BEEN_SOLD.getId());
				}
				else if (msg.getMailType() == MailType.COMMISSION_ITEM_RETURNED)
				{
					packet.writeD(SystemMessageId.THE_REGISTRATION_PERIOD_FOR_THE_ITEM_YOU_REGISTERED_HAS_EXPIRED.getId());
				}
				packet.writeD(msg.getId());
				packet.writeS(msg.getSubject());
				packet.writeS(msg.getSenderName());
				packet.writeD(msg.isLocked() ? 0x01 : 0x00);
				packet.writeD(msg.getExpirationSeconds());
				packet.writeD(msg.isUnread() ? 0x01 : 0x00);
				packet.writeD(((msg.getMailType() == MailType.COMMISSION_ITEM_SOLD) || (msg.getMailType() == MailType.COMMISSION_ITEM_RETURNED)) ? 0 : 1);
				packet.writeD(msg.hasAttachments() ? 0x01 : 0x00);
				packet.writeD(msg.isReturned() ? 0x01 : 0x00);
				packet.writeD(0x00); // SysString in some case it seems
			}
		}
		else
		{
			packet.writeD(0x00);
		}
		packet.writeD(100); // TODO: Find me
		packet.writeD(1000); // TODO: Find me
		return true;
	}
}
