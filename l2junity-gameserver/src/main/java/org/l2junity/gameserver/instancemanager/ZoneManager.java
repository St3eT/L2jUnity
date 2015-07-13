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
package org.l2junity.gameserver.instancemanager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.l2junity.gameserver.data.xml.IGameXmlReader;
import org.l2junity.gameserver.model.World;
import org.l2junity.gameserver.model.WorldObject;
import org.l2junity.gameserver.model.actor.Creature;
import org.l2junity.gameserver.model.interfaces.ILocational;
import org.l2junity.gameserver.model.items.instance.ItemInstance;
import org.l2junity.gameserver.model.zone.AbstractZoneSettings;
import org.l2junity.gameserver.model.zone.L2ZoneForm;
import org.l2junity.gameserver.model.zone.L2ZoneRespawn;
import org.l2junity.gameserver.model.zone.ZoneRegion;
import org.l2junity.gameserver.model.zone.ZoneType;
import org.l2junity.gameserver.model.zone.form.ZoneCuboid;
import org.l2junity.gameserver.model.zone.form.ZoneCylinder;
import org.l2junity.gameserver.model.zone.form.ZoneNPoly;
import org.l2junity.gameserver.model.zone.type.ArenaZone;
import org.l2junity.gameserver.model.zone.type.NpcSpawnTerritory;
import org.l2junity.gameserver.model.zone.type.OlympiadStadiumZone;
import org.l2junity.gameserver.model.zone.type.RespawnZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * This class manages the zones
 * @author durgus
 */
public final class ZoneManager implements IGameXmlReader
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ZoneManager.class);
	
	private static final Map<String, AbstractZoneSettings> _settings = new HashMap<>();
	
	public static final int SHIFT_BY = 15;
	public static final int OFFSET_X = Math.abs(World.MAP_MIN_X >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(World.MAP_MIN_Y >> SHIFT_BY);
	
	private final Map<Class<? extends ZoneType>, Map<Integer, ? extends ZoneType>> _classZones = new HashMap<>();
	private final Map<String, NpcSpawnTerritory> _spawnTerritories = new HashMap<>();
	private int _lastDynamicId = 300000;
	private List<ItemInstance> _debugItems;
	
	private final ZoneRegion[][] _zoneRegions = new ZoneRegion[(World.MAP_MAX_X >> SHIFT_BY) + OFFSET_X + 1][(World.MAP_MAX_Y >> SHIFT_BY) + OFFSET_Y + 1];
	
	/**
	 * Instantiates a new zone manager.
	 */
	protected ZoneManager()
	{
		for (int x = 0; x < _zoneRegions.length; x++)
		{
			for (int y = 0; y < _zoneRegions[x].length; y++)
			{
				_zoneRegions[x][y] = new ZoneRegion(x, y);
			}
		}
		LOGGER.info("{} by {} Zone Region Grid set up.", _zoneRegions.length, _zoneRegions[0].length);
		
		load();
	}
	
	/**
	 * Reload.
	 */
	public void reload()
	{
		// Get the world regions
		int count = 0;
		
		// Backup old zone settings
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (ZoneType zone : map.values())
			{
				if (zone.getSettings() != null)
				{
					_settings.put(zone.getName(), zone.getSettings());
				}
			}
		}
		
		// Clear zones
		for (ZoneRegion[] zoneRegions : _zoneRegions)
		{
			for (ZoneRegion zoneRegion : zoneRegions)
			{
				zoneRegion.getZones().clear();
				count++;
			}
		}
		LOGGER.info("Removed zones in {} regions.", count);
		
		// Load the zones
		load();
		
		// Re-validate all characters in zones
		for (WorldObject obj : World.getInstance().getVisibleObjects())
		{
			if (obj instanceof Creature)
			{
				((Creature) obj).revalidateZone(true);
			}
		}
		_settings.clear();
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		NamedNodeMap attrs;
		Node attribute;
		String zoneName;
		int[][] coords;
		int zoneId, minZ, maxZ;
		String zoneType, zoneShape;
		final List<int[]> rs = new ArrayList<>();
		
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				attribute = attrs.getNamedItem("enabled");
				if ((attribute != null) && !Boolean.parseBoolean(attribute.getNodeValue()))
				{
					continue;
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						
						attribute = attrs.getNamedItem("type");
						if (attribute != null)
						{
							zoneType = attribute.getNodeValue();
						}
						else
						{
							LOGGER.warn("ZoneData: Missing type for zone in file: {}", f.getName());
							continue;
						}
						
						attribute = attrs.getNamedItem("id");
						if (attribute != null)
						{
							zoneId = Integer.parseInt(attribute.getNodeValue());
						}
						else
						{
							zoneId = zoneType.equalsIgnoreCase("NpcSpawnTerritory") ? 0 : _lastDynamicId++;
						}
						
						attribute = attrs.getNamedItem("name");
						if (attribute != null)
						{
							zoneName = attribute.getNodeValue();
						}
						else
						{
							zoneName = null;
						}
						
						// Check zone name for NpcSpawnTerritory. Must exist and to be unique
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							if (zoneName == null)
							{
								LOGGER.warn("ZoneData: Missing name for NpcSpawnTerritory in file: {}, skipping zone", f.getName());
								continue;
							}
							else if (_spawnTerritories.containsKey(zoneName))
							{
								LOGGER.warn("ZoneData: Name {} already used for another zone, check file: {}. Skipping zone", zoneName, f.getName());
								continue;
							}
						}
						
						minZ = parseInteger(attrs, "minZ");
						maxZ = parseInteger(attrs, "maxZ");
						
						zoneType = parseString(attrs, "type");
						zoneShape = parseString(attrs, "shape");
						
						// Get the zone shape from xml
						L2ZoneForm zoneForm = null;
						try
						{
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("node".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									int[] point = new int[2];
									point[0] = parseInteger(attrs, "X");
									point[1] = parseInteger(attrs, "Y");
									rs.add(point);
								}
							}
							
							coords = rs.toArray(new int[rs.size()][2]);
							rs.clear();
							
							if ((coords == null) || (coords.length == 0))
							{
								LOGGER.warn("ZoneData: missing data for zone: {} XML file: {}", zoneId, f.getName());
								continue;
							}
							
							// Create this zone. Parsing for cuboids is a
							// bit different than for other polygons
							// cuboids need exactly 2 points to be defined.
							// Other polygons need at least 3 (one per
							// vertex)
							if (zoneShape.equalsIgnoreCase("Cuboid"))
							{
								if (coords.length == 2)
								{
									zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
								}
								else
								{
									LOGGER.warn("ZoneData: Missing cuboid vertex data for zone: {} in file: {}", zoneId, f.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("NPoly"))
							{
								// nPoly needs to have at least 3 vertices
								if (coords.length > 2)
								{
									final int[] aX = new int[coords.length];
									final int[] aY = new int[coords.length];
									for (int i = 0; i < coords.length; i++)
									{
										aX[i] = coords[i][0];
										aY[i] = coords[i][1];
									}
									zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
								}
								else
								{
									LOGGER.warn("ZoneData: Bad data for zone: {} in file: {}", zoneId, f.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("Cylinder"))
							{
								// A Cylinder zone requires a center point
								// at x,y and a radius
								attrs = d.getAttributes();
								final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
								if ((coords.length == 1) && (zoneRad > 0))
								{
									zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
								}
								else
								{
									LOGGER.warn("ZoneData: Bad data for zone: {} in file: {}", zoneId, f.getName());
									continue;
								}
							}
							else
							{
								LOGGER.warn("ZoneData: Unknown shape: \"{}\"  for zone: {} in file: {}", zoneShape, zoneId, f.getName());
								continue;
							}
						}
						catch (Exception e)
						{
							LOGGER.warn("ZoneData: Failed to load zone {} coordinates: ", zoneId, e);
						}
						
						// No further parameters needed, if NpcSpawnTerritory is loading
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							_spawnTerritories.put(zoneName, new NpcSpawnTerritory(zoneName, zoneForm));
							continue;
						}
						
						// Create the zone
						Class<?> newZone = null;
						Constructor<?> zoneConstructor = null;
						ZoneType temp;
						try
						{
							newZone = Class.forName("org.l2junity.gameserver.model.zone.type." + zoneType);
							zoneConstructor = newZone.getConstructor(int.class);
							temp = (ZoneType) zoneConstructor.newInstance(zoneId);
							temp.setZone(zoneForm);
						}
						catch (Exception e)
						{
							LOGGER.warn("ZoneData: No such zone type: {} in file: {}", zoneType, f.getName());
							continue;
						}
						
						// Check for additional parameters
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								String name = attrs.getNamedItem("name").getNodeValue();
								String val = attrs.getNamedItem("val").getNodeValue();
								
								temp.setParameter(name, val);
							}
							else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof L2ZoneRespawn))
							{
								attrs = cd.getAttributes();
								int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
								int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
								int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
								Node val = attrs.getNamedItem("type");
								((L2ZoneRespawn) temp).parseLoc(spawnX, spawnY, spawnZ, val == null ? null : val.getNodeValue());
							}
							else if ("race".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof RespawnZone))
							{
								attrs = cd.getAttributes();
								String race = attrs.getNamedItem("name").getNodeValue();
								String point = attrs.getNamedItem("point").getNodeValue();
								
								((RespawnZone) temp).addRaceRespawnPoint(race, point);
							}
						}
						if (checkId(zoneId))
						{
							LOGGER.info("Caution: Zone ({}) from file: {} overrides previos definition.", zoneId, f.getName());
						}
						
						if ((zoneName != null) && !zoneName.isEmpty())
						{
							temp.setName(zoneName);
						}
						
						addZone(zoneId, temp);
						
						// Register the zone into any world region it
						// intersects with...
						// currently 11136 test for each zone :>
						for (int x = 0; x < _zoneRegions.length; x++)
						{
							for (int y = 0; y < _zoneRegions[x].length; y++)
							{
								
								int ax = (x - OFFSET_X) << SHIFT_BY;
								int bx = ((x + 1) - OFFSET_X) << SHIFT_BY;
								int ay = (y - OFFSET_Y) << SHIFT_BY;
								int by = ((y + 1) - OFFSET_Y) << SHIFT_BY;
								
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									_zoneRegions[x][y].getZones().put(temp.getId(), temp);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public final void load()
	{
		_classZones.clear();
		_spawnTerritories.clear();
		parseDatapackDirectory("data/zones", false);
		parseDatapackDirectory("data/zones/npcSpawnTerritories", false);
		LOGGER.info("Loaded {} zone classes and {} zones.", _classZones.size(), getSize());
		LOGGER.info("Loaded {} NPC spawn territoriers.", _spawnTerritories.size());
	}
	
	/**
	 * Gets the size.
	 * @return the size
	 */
	public int getSize()
	{
		int i = 0;
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		return i;
	}
	
	/**
	 * Check id.
	 * @param id the id
	 * @return true, if successful
	 */
	public boolean checkId(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Add new zone.
	 * @param <T> the generic type
	 * @param id the id
	 * @param zone the zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> void addZone(Integer id, T zone)
	{
		Map<Integer, T> map = (Map<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			map = new HashMap<>();
			map.put(id, zone);
			_classZones.put(zone.getClass(), map);
		}
		else
		{
			map.put(id, zone);
		}
	}
	
	/**
	 * Return all zones by class type.
	 * @param <T> the generic type
	 * @param zoneType Zone class
	 * @return Collection of zones
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	/**
	 * Get zone by ID.
	 * @param id the id
	 * @return the zone by id
	 * @see #getZoneById(int, Class)
	 */
	public ZoneType getZoneById(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return map.get(id);
			}
		}
		return null;
	}
	
	/**
	 * Get zone by ID and zone class.
	 * @param <T> the generic type
	 * @param id the id
	 * @param zoneType the zone type
	 * @return zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneById(int id, Class<T> zoneType)
	{
		return (T) _classZones.get(zoneType).get(id);
	}
	
	/**
	 * Returns all zones from where the object is located.
	 * @param locational the locational
	 * @return zones
	 */
	public List<ZoneType> getZones(ILocational locational)
	{
		return getZones(locational.getX(), locational.getY(), locational.getZ());
	}
	
	/**
	 * Gets the zone.
	 * @param <T> the generic type
	 * @param locational the locational
	 * @param type the type
	 * @return zone from where the object is located by type
	 */
	public <T extends ZoneType> T getZone(ILocational locational, Class<T> type)
	{
		if (locational == null)
		{
			return null;
		}
		return getZone(locational.getX(), locational.getY(), locational.getZ(), type);
	}
	
	/**
	 * Returns all zones from given coordinates (plane).
	 * @param x the x
	 * @param y the y
	 * @return zones
	 */
	public List<ZoneType> getZones(int x, int y)
	{
		final List<ZoneType> temp = new ArrayList<>();
		for (ZoneType zone : getRegion(x, y).getZones().values())
		{
			if (zone.isInsideZone(x, y))
			{
				temp.add(zone);
			}
		}
		return temp;
	}
	
	/**
	 * Returns all zones from given coordinates.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return zones
	 */
	public List<ZoneType> getZones(int x, int y, int z)
	{
		final List<ZoneType> temp = new ArrayList<>();
		for (ZoneType zone : getRegion(x, y).getZones().values())
		{
			if (zone.isInsideZone(x, y, z))
			{
				temp.add(zone);
			}
		}
		return temp;
	}
	
	/**
	 * Gets the zone.
	 * @param <T> the generic type
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param type the type
	 * @return zone from given coordinates
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		for (ZoneType zone : getRegion(x, y).getZones().values())
		{
			if (zone.isInsideZone(x, y, z) && type.isInstance(zone))
			{
				return (T) zone;
			}
		}
		return null;
	}
	
	/**
	 * Get spawm territory by name
	 * @param name name of territory to search
	 * @return link to zone form
	 */
	public NpcSpawnTerritory getSpawnTerritory(String name)
	{
		return _spawnTerritories.containsKey(name) ? _spawnTerritories.get(name) : null;
	}
	
	/**
	 * Returns all spawm territories from where the object is located
	 * @param object
	 * @return zones
	 */
	public List<NpcSpawnTerritory> getSpawnTerritories(WorldObject object)
	{
		List<NpcSpawnTerritory> temp = new ArrayList<>();
		for (NpcSpawnTerritory territory : _spawnTerritories.values())
		{
			if (territory.isInsideZone(object.getX(), object.getY(), object.getZ()))
			{
				temp.add(territory);
			}
		}
		
		return temp;
	}
	
	/**
	 * Gets the arena.
	 * @param character the character
	 * @return the arena
	 */
	public final ArenaZone getArena(Creature character)
	{
		if (character == null)
		{
			return null;
		}
		
		for (ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if ((temp instanceof ArenaZone) && temp.isCharacterInZone(character))
			{
				return ((ArenaZone) temp);
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the olympiad stadium.
	 * @param character the character
	 * @return the olympiad stadium
	 */
	public final OlympiadStadiumZone getOlympiadStadium(Creature character)
	{
		if (character == null)
		{
			return null;
		}
		
		for (ZoneType temp : ZoneManager.getInstance().getZones(character.getX(), character.getY(), character.getZ()))
		{
			if ((temp instanceof OlympiadStadiumZone) && temp.isCharacterInZone(character))
			{
				return ((OlympiadStadiumZone) temp);
			}
		}
		return null;
	}
	
	/**
	 * For testing purposes only.
	 * @param <T> the generic type
	 * @param obj the obj
	 * @param type the type
	 * @return the closest zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getClosestZone(WorldObject obj, Class<T> type)
	{
		T zone = getZone(obj, type);
		if (zone == null)
		{
			double closestdis = Double.MAX_VALUE;
			for (T temp : (Collection<T>) _classZones.get(type).values())
			{
				double distance = temp.getDistanceToZone(obj);
				if (distance < closestdis)
				{
					closestdis = distance;
					zone = temp;
				}
			}
		}
		return zone;
	}
	
	/**
	 * General storage for debug items used for visualizing zones.
	 * @return list of items
	 */
	public List<ItemInstance> getDebugItems()
	{
		if (_debugItems == null)
		{
			_debugItems = new ArrayList<>();
		}
		return _debugItems;
	}
	
	/**
	 * Remove all debug items from l2world.
	 */
	public void clearDebugItems()
	{
		if (_debugItems != null)
		{
			final Iterator<ItemInstance> it = _debugItems.iterator();
			while (it.hasNext())
			{
				final ItemInstance item = it.next();
				if (item != null)
				{
					item.decayMe();
				}
				it.remove();
			}
		}
	}
	
	public ZoneRegion getRegion(int x, int y)
	{
		return _zoneRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
	}
	
	public ZoneRegion getRegion(ILocational point)
	{
		return getRegion(point.getX(), point.getY());
	}
	
	/**
	 * Gets the settings.
	 * @param name the name
	 * @return the settings
	 */
	public static AbstractZoneSettings getSettings(String name)
	{
		return _settings.get(name);
	}
	
	/**
	 * Gets the single instance of ZoneManager.
	 * @return single instance of ZoneManager
	 */
	public static ZoneManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneManager _instance = new ZoneManager();
	}
}
