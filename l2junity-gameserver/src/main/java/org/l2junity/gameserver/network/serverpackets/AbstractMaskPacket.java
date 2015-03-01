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

import org.l2junity.gameserver.model.interfaces.IUpdateTypeComponent;

/**
 * @author UnAfraid
 * @param <T>
 */
public abstract class AbstractMaskPacket<T extends IUpdateTypeComponent> implements IGameServerPacket
{
	protected static final byte[] DEFAULT_FLAG_ARRAY =
	{
		(byte) 0x80,
		0x40,
		0x20,
		0x10,
		0x08,
		0x04,
		0x02,
		0x01
	};
	
	protected abstract byte[] getMasks();
	
	protected abstract void onNewMaskAdded(T component);
	
	@SuppressWarnings("unchecked")
	public void addComponentType(T... updateComponents)
	{
		for (T component : updateComponents)
		{
			if (!containsMask(component))
			{
				getMasks()[component.getMask() >> 3] |= DEFAULT_FLAG_ARRAY[component.getMask() & 7];
				onNewMaskAdded(component);
			}
		}
	}
	
	public boolean containsMask(T component)
	{
		return containsMask(component.getMask());
	}
	
	public boolean containsMask(int mask)
	{
		return (getMasks()[mask >> 3] & DEFAULT_FLAG_ARRAY[mask & 7]) != 0;
	}
	
	/**
	 * @param masks
	 * @param type
	 * @return {@code true} if the mask contains the current update component type
	 */
	public boolean containsMask(int masks, T type)
	{
		return (masks & type.getMask()) == type.getMask();
	}
}
