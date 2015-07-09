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
package org.l2junity.gameserver.handler;

import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IUserCommandHandler
{
	Logger _log = LoggerFactory.getLogger(IUserCommandHandler.class);
	
	/**
	 * this is the worker method that is called when someone uses an admin command.
	 * @param id
	 * @param activeChar
	 * @return command success
	 */
	boolean useUserCommand(int id, PlayerInstance activeChar);
	
	/**
	 * this method is called at initialization to register all the item ids automatically
	 * @return all known itemIds
	 */
	int[] getUserCommandList();
}
