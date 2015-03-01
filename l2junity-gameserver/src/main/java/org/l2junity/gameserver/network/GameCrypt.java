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
package org.l2junity.gameserver.network;

import io.netty.buffer.ByteBuf;

import org.l2junity.network.ICrypt;

/**
 * @author KenM
 */
public class GameCrypt implements ICrypt
{
	private final byte[] _inKey = new byte[16];
	private final byte[] _outKey = new byte[16];
	private boolean _isEnabled;
	
	public void setKey(byte[] key)
	{
		System.arraycopy(key, 0, _inKey, 0, 16);
		System.arraycopy(key, 0, _outKey, 0, 16);
	}
	
	@Override
	public void encrypt(ByteBuf buf)
	{
		if (!_isEnabled)
		{
			_isEnabled = true;
			return;
		}
		
		buf.readerIndex(2);
		int a = 0;
		while (buf.isReadable())
		{
			final int b = buf.readByte() & 0xFF;
			a = b ^ _outKey[(buf.readerIndex() - 3) & 15] ^ a;
			buf.setByte(buf.readerIndex() - 1, a);
		}
		
		shifKey(_outKey, buf.writerIndex());
	}
	
	@Override
	public void decrypt(ByteBuf buf)
	{
		if (!_isEnabled)
		{
			return;
		}
		
		buf.readerIndex(2);
		int a = 0;
		while (buf.isReadable())
		{
			final int b = buf.readByte() & 0xFF;
			buf.setByte(buf.readerIndex() - 1, b ^ _inKey[(buf.readerIndex() - 3) & 15] ^ a);
			a = b;
		}
		
		shifKey(_inKey, buf.writerIndex());
	}
	
	private void shifKey(byte[] key, int size)
	{
		int old = key[8] & 0xff;
		old |= (key[9] << 8) & 0xff00;
		old |= (key[10] << 0x10) & 0xff0000;
		old |= (key[11] << 0x18) & 0xff000000;
		
		old += size;
		
		key[8] = (byte) (old & 0xff);
		key[9] = (byte) ((old >> 0x08) & 0xff);
		key[10] = (byte) ((old >> 0x10) & 0xff);
		key[11] = (byte) ((old >> 0x18) & 0xff);
	}
}
