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
package org.l2junity.gameserver.network.clientpackets.friend;

import org.l2junity.gameserver.model.BlockList;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.network.L2GameClient;
import org.l2junity.gameserver.network.SystemMessageId;
import org.l2junity.gameserver.network.clientpackets.IGameClientPacket;
import org.l2junity.gameserver.network.serverpackets.SystemMessage;
import org.l2junity.gameserver.network.serverpackets.friend.FriendAddRequest;
import org.l2junity.network.PacketReader;

public final class RequestFriendInvite implements IGameClientPacket
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
		final PlayerInstance activeChar = client.getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		final PlayerInstance friend = World.getInstance().getPlayer(_name);
		
		// Target is not found in the game.
		if ((friend == null) || !friend.isOnline() || friend.isInvisible())
		{
			activeChar.sendPacket(SystemMessageId.THE_USER_WHO_REQUESTED_TO_BECOME_FRIENDS_IS_NOT_FOUND_IN_THE_GAME);
			return;
		}
		// You cannot add yourself to your own friend list.
		if (friend == activeChar)
		{
			activeChar.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
			return;
		}
		// Target is in olympiad.
		if (activeChar.isInOlympiadMode() || friend.isInOlympiadMode())
		{
			activeChar.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}
		// Target blocked active player.
		if (BlockList.isBlocked(friend, activeChar))
		{
			activeChar.sendMessage("You are in target's block list.");
			return;
		}
		SystemMessage sm;
		// Target is blocked.
		if (BlockList.isBlocked(activeChar, friend))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_BLOCKED_C1);
			sm.addCharName(friend);
			activeChar.sendPacket(sm);
			return;
		}
		
		// Target already in friend list.
		if (activeChar.getFriendList().contains(friend.getObjectId()))
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.THIS_PLAYER_IS_ALREADY_REGISTERED_ON_YOUR_FRIENDS_LIST);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		// Target is busy.
		if (friend.isProcessingRequest())
		{
			sm = SystemMessage.getSystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(_name);
			activeChar.sendPacket(sm);
			return;
		}
		// Friend request sent.
		activeChar.onTransactionRequest(friend);
		friend.sendPacket(new FriendAddRequest(activeChar.getName()));
		sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_VE_REQUESTED_C1_TO_BE_ON_YOUR_FRIENDS_LIST);
		sm.addString(_name);
		activeChar.sendPacket(sm);
	}
}
