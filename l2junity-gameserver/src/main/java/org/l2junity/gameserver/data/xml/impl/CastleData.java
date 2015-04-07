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
package org.l2junity.gameserver.data.xml.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.l2junity.gameserver.data.xml.IXmlReader;
import org.l2junity.gameserver.enums.CastleSide;
import org.l2junity.gameserver.enums.SiegeGuardType;
import org.l2junity.gameserver.model.holders.CastleSpawnHolder;
import org.l2junity.gameserver.model.holders.SiegeGuardHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author St3eT
 */
public final class CastleData implements IXmlReader
{
	private final Map<Integer, List<CastleSpawnHolder>> _castles = new HashMap<>();
	private static final Map<Integer, List<SiegeGuardHolder>> _siegeGuards = new HashMap<>();
	
	protected CastleData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_castles.clear();
		_siegeGuards.clear();
		parseDatapackDirectory("data/castles", true);
	}
	
	@Override
	public void parseDocument(Document doc)
	{
		for (Node listNode = doc.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
		{
			if ("list".equals(listNode.getNodeName()))
			{
				for (Node castleNode = listNode.getFirstChild(); castleNode != null; castleNode = castleNode.getNextSibling())
				{
					if ("castle".equals(castleNode.getNodeName()))
					{
						final int castleId = parseInteger(castleNode.getAttributes(), "id");
						for (Node tpNode = castleNode.getFirstChild(); tpNode != null; tpNode = tpNode.getNextSibling())
						{
							final List<CastleSpawnHolder> spawns = new ArrayList<>();
							
							if ("spawn".equals(tpNode.getNodeName()))
							{
								final CastleSide side = parseEnum(tpNode.getAttributes(), CastleSide.class, "castleSide", CastleSide.NEUTRAL);
								for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
								{
									if ("npc".equals(npcNode.getNodeName()))
									{
										final NamedNodeMap np = npcNode.getAttributes();
										final int npcId = parseInteger(np, "id");
										final int x = parseInteger(np, "x");
										final int y = parseInteger(np, "y");
										final int z = parseInteger(np, "z");
										final int heading = parseInteger(np, "heading");
										
										spawns.add(new CastleSpawnHolder(npcId, side, x, y, z, heading));
									}
								}
								_castles.put(castleId, spawns);
							}
							else if ("siegeGuards".equals(tpNode.getNodeName()))
							{
								final List<SiegeGuardHolder> guards = new ArrayList<>();
								
								for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
								{
									if ("guard".equals(npcNode.getNodeName()))
									{
										final NamedNodeMap np = npcNode.getAttributes();
										final int itemId = parseInteger(np, "itemId");
										final SiegeGuardType type = parseEnum(tpNode.getAttributes(), SiegeGuardType.class, "type");
										final boolean stationary = parseBoolean(np, "stationary", false);
										final int npcId = parseInteger(np, "npcId");
										final int npcMaxAmount = parseInteger(np, "npcMaxAmount");
										
										guards.add(new SiegeGuardHolder(castleId, itemId, type, stationary, npcId, npcMaxAmount));
									}
								}
								getSiegeGuards().put(castleId, guards);
							}
						}
					}
				}
			}
		}
	}
	
	public final List<CastleSpawnHolder> getSpawnsForSide(int castleId, CastleSide side)
	{
		return _castles.getOrDefault(castleId, Collections.emptyList()).stream().filter(s -> s.getSide() == side).collect(Collectors.toList());
	}
	
	public final List<SiegeGuardHolder> getSiegeGuardsForCastle(int castleId)
	{
		return _siegeGuards.getOrDefault(castleId, Collections.emptyList());
	}
	
	public final Map<Integer, List<SiegeGuardHolder>> getSiegeGuards()
	{
		return _siegeGuards;
	}
	
	/**
	 * Gets the single instance of TeleportersData.
	 * @return single instance of TeleportersData
	 */
	public static CastleData getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleData _instance = new CastleData();
	}
}
