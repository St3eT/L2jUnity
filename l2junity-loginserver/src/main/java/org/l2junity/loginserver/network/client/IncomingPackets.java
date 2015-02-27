/*
 * Copyright (C) 2004-2014 L2J Server
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
package org.l2junity.loginserver.network.client;

import java.util.Arrays;
import java.util.function.Supplier;

import org.l2junity.loginserver.network.client.receive.RequestAuthLogin;
import org.l2junity.loginserver.network.client.receive.RequestSCCheck;
import org.l2junity.loginserver.network.client.receive.RequestServerList;
import org.l2junity.loginserver.network.client.receive.RequestServerLogin;
import org.l2junity.loginserver.network.client.receive.ResponseAuthGameGuard;
import org.l2junity.network.IConnectionState;
import org.l2junity.network.IIncomingPacket;
import org.l2junity.network.IIncomingPackets;

/**
 * @author Nos
 */
enum IncomingPackets implements IIncomingPackets<IIncomingPacket<ClientHandler>>
{
	REQUEST_AUTH_LOGIN(0x00, RequestAuthLogin::new, ConnectionState.CONNECTED),
	REQUEST_SERVER_LOGIN(0x02, RequestServerLogin::new, ConnectionState.AUTHED_GG),
	REQUEST_SERVER_LIST(0x05, RequestServerList::new, ConnectionState.AUTHED_SERVER_LIST),
	REQUEST_SC_CHECK(0x06, RequestSCCheck::new, ConnectionState.AUTHED_GG),
	RESPONSE_AUTH_GAMEGUARD(0x07, ResponseAuthGameGuard::new, ConnectionState.CONNECTED);
	
	public static final IncomingPackets[] PACKET_ARRAY;
	
	static
	{
		final short maxPacketId = (short) Arrays.stream(values()).mapToInt(IIncomingPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new IncomingPackets[maxPacketId + 1];
		for (IncomingPackets incomingPacket : values())
		{
			PACKET_ARRAY[incomingPacket.getPacketId()] = incomingPacket;
		}
	}
	
	private short _packetId;
	private Supplier<IIncomingPacket<ClientHandler>> _incomingPacketFactory;
	private IConnectionState _connectionState;
	
	private IncomingPackets(int packetId, Supplier<IIncomingPacket<ClientHandler>> incomingPacketFactory, IConnectionState connectionState)
	{
		// packetId is an unsigned byte
		if (packetId > 0xFF)
		{
			throw new IllegalArgumentException("packetId must not be bigger than 0xFF");
		}
		
		_packetId = (short) packetId;
		_incomingPacketFactory = incomingPacketFactory;
		_connectionState = connectionState;
	}
	
	@Override
	public int getPacketId()
	{
		return _packetId;
	}
	
	@Override
	public IConnectionState getState()
	{
		return _connectionState;
	}
	
	@Override
	public IIncomingPacket<ClientHandler> newIncomingPacket()
	{
		return _incomingPacketFactory.get();
	}
}
