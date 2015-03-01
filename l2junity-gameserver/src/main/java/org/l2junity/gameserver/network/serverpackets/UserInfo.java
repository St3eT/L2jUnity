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

import org.l2junity.gameserver.data.xml.impl.ExperienceData;
import org.l2junity.gameserver.enums.UserInfoType;
import org.l2junity.gameserver.instancemanager.RaidBossPointsManager;
import org.l2junity.gameserver.model.Elementals;
import org.l2junity.gameserver.model.L2Clan;
import org.l2junity.gameserver.model.Party;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

/**
 * @author Sdw, UnAfraid
 */
public class UserInfo extends AbstractMaskPacket<UserInfoType>
{
	private final PlayerInstance _activeChar;
	
	private final int _relation;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flRunSpd = 0;
	private final int _flWalkSpd = 0;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private int _enchantLevel = 0;
	private int _armorEnchant = 0;
	
	private final byte[] _masks = new byte[]
	{
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00
	};
	
	private int _initSize = 5;
	
	public UserInfo(PlayerInstance cha)
	{
		this(cha, true);
	}
	
	public UserInfo(PlayerInstance cha, boolean addAll)
	{
		_activeChar = cha;
		
		_relation = calculateRelation(cha);
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(cha.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(cha.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(cha.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = cha.isFlying() ? _runSpd : 0;
		_flyWalkSpd = cha.isFlying() ? _walkSpd : 0;
		_enchantLevel = cha.getInventory().getWeaponEnchant();
		_armorEnchant = cha.getInventory().getArmorMinEnchant();
		
		if (addAll)
		{
			addComponentType(UserInfoType.values());
		}
	}
	
	@Override
	protected byte[] getMasks()
	{
		return _masks;
	}
	
	@Override
	protected void onNewMaskAdded(UserInfoType component)
	{
		calcBlockSize(component);
	}
	
	private void calcBlockSize(UserInfoType type)
	{
		switch (type)
		{
			case BASIC_INFO:
			{
				_initSize += type.getBlockLength() + (_activeChar.getName().length() * 2);
				break;
			}
			case CLAN:
			{
				_initSize += type.getBlockLength() + (_activeChar.getTitle().length() * 2);
				break;
			}
			default:
			{
				_initSize += type.getBlockLength();
				break;
			}
		}
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.USER_INFO.writeId(packet);
		
		packet.writeD(_activeChar.getObjectId());
		packet.writeD(_initSize);
		packet.writeH(23);
		packet.writeB(_masks);
		
		if (containsMask(UserInfoType.RELATION))
		{
			packet.writeD(_relation);
		}
		
		if (containsMask(UserInfoType.BASIC_INFO))
		{
			packet.writeH(16 + (_activeChar.getName().length() * 2));
			packet.writeString(_activeChar.getName());
			packet.writeC(_activeChar.isGM() ? 0x01 : 0x00);
			packet.writeC(_activeChar.getRace().ordinal());
			packet.writeC(_activeChar.getAppearance().getSex() ? 0x01 : 0x00);
			packet.writeD(_activeChar.getBaseClass());
			packet.writeD(_activeChar.getClassId().getId());
			packet.writeC(_activeChar.getLevel());
		}
		
		if (containsMask(UserInfoType.BASE_STATS))
		{
			packet.writeH(18);
			packet.writeH(_activeChar.getSTR());
			packet.writeH(_activeChar.getDEX());
			packet.writeH(_activeChar.getCON());
			packet.writeH(_activeChar.getINT());
			packet.writeH(_activeChar.getWIT());
			packet.writeH(_activeChar.getMEN());
			packet.writeH(_activeChar.getLUC());
			packet.writeH(_activeChar.getCHA());
		}
		
		if (containsMask(UserInfoType.MAX_HPCPMP))
		{
			packet.writeH(14);
			packet.writeD(_activeChar.getMaxHp());
			packet.writeD(_activeChar.getMaxMp());
			packet.writeD(_activeChar.getMaxCp());
		}
		
		if (containsMask(UserInfoType.CURRENT_HPMPCP_EXP_SP))
		{
			packet.writeH(38);
			packet.writeD((int) Math.round(_activeChar.getCurrentHp()));
			packet.writeD((int) Math.round(_activeChar.getCurrentMp()));
			packet.writeD((int) Math.round(_activeChar.getCurrentCp()));
			packet.writeQ(_activeChar.getSp());
			packet.writeQ(_activeChar.getExp());
			packet.writeF((float) (_activeChar.getExp() - ExperienceData.getInstance().getExpForLevel(_activeChar.getLevel())) / (ExperienceData.getInstance().getExpForLevel(_activeChar.getLevel() + 1) - ExperienceData.getInstance().getExpForLevel(_activeChar.getLevel())));
		}
		
		if (containsMask(UserInfoType.ENCHANTLEVEL))
		{
			packet.writeH(4);
			packet.writeC(_enchantLevel);
			packet.writeC(_armorEnchant);
		}
		
		if (containsMask(UserInfoType.APPAREANCE))
		{
			packet.writeH(15);
			packet.writeD(_activeChar.getVisualHair());
			packet.writeD(_activeChar.getVisualHairColor());
			packet.writeD(_activeChar.getVisualFace());
			packet.writeC(_activeChar.isHairAccessoryEnabled() ? 0x01 : 0x00);
		}
		
		if (containsMask(UserInfoType.STATUS))
		{
			packet.writeH(6);
			packet.writeC(_activeChar.getMountType().ordinal());
			packet.writeC(_activeChar.getPrivateStoreType().getId());
			packet.writeC(_activeChar.hasDwarvenCraft() ? 1 : 0);
			packet.writeC(_activeChar.getAbilityPointsUsed());
		}
		
		if (containsMask(UserInfoType.STATS))
		{
			packet.writeH(56);
			packet.writeH(_activeChar.getActiveWeaponItem() != null ? 40 : 20);
			packet.writeD(_activeChar.getPAtk(null));
			packet.writeD(_activeChar.getPAtkSpd());
			packet.writeD(_activeChar.getPDef(null));
			packet.writeD(_activeChar.getEvasionRate(null));
			packet.writeD(_activeChar.getAccuracy());
			packet.writeD(_activeChar.getCriticalHit(null, null));
			packet.writeD(_activeChar.getMAtk(null, null));
			packet.writeD(_activeChar.getMAtkSpd());
			packet.writeD(_activeChar.getPAtkSpd()); // Seems like atk speed - 1
			packet.writeD(_activeChar.getMagicEvasionRate(null));
			packet.writeD(_activeChar.getMDef(null, null));
			packet.writeD(_activeChar.getMagicAccuracy());
			packet.writeD(_activeChar.getMCriticalHit(null, null));
		}
		
		if (containsMask(UserInfoType.ELEMENTALS))
		{
			packet.writeH(14);
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.FIRE));
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.WATER));
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.WIND));
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.EARTH));
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.HOLY));
			packet.writeH(_activeChar.getDefenseElementValue(Elementals.DARK));
		}
		
		if (containsMask(UserInfoType.POSITION))
		{
			packet.writeH(18);
			packet.writeD(_activeChar.getX());
			packet.writeD(_activeChar.getY());
			packet.writeD(_activeChar.getZ());
			packet.writeD(_activeChar.getHeading());
		}
		
		if (containsMask(UserInfoType.SPEED))
		{
			packet.writeH(18);
			packet.writeH(_runSpd);
			packet.writeH(_walkSpd);
			packet.writeH(_swimRunSpd);
			packet.writeH(_swimWalkSpd);
			packet.writeH(_flRunSpd);
			packet.writeH(_flWalkSpd);
			packet.writeH(_flyRunSpd);
			packet.writeH(_flyWalkSpd);
		}
		
		if (containsMask(UserInfoType.MULTIPLIER))
		{
			packet.writeH(18);
			packet.writeF(_moveMultiplier);
			packet.writeF(_activeChar.getAttackSpeedMultiplier());
		}
		
		if (containsMask(UserInfoType.COL_RADIUS_HEIGHT))
		{
			packet.writeH(18);
			packet.writeF(_activeChar.getCollisionRadius());
			packet.writeF(_activeChar.getCollisionHeight());
		}
		
		if (containsMask(UserInfoType.ATK_ELEMENTAL))
		{
			packet.writeH(5);
			byte attackAttribute = _activeChar.getAttackElement();
			packet.writeC(attackAttribute);
			packet.writeH(_activeChar.getAttackElementValue(attackAttribute));
		}
		
		if (containsMask(UserInfoType.CLAN))
		{
			packet.writeH(32 + (_activeChar.getTitle().length() * 2));
			packet.writeString(_activeChar.getTitle());
			packet.writeH(_activeChar.getPledgeType());
			packet.writeD(_activeChar.getClanId());
			packet.writeD(_activeChar.getClanCrestLargeId());
			packet.writeD(_activeChar.getClanCrestId());
			packet.writeC(_activeChar.isClanLeader() ? -1 : 0x00);
			packet.writeD(_activeChar.getClanPrivileges().getBitmask());
			packet.writeD(_activeChar.getAllyId());
			packet.writeD(_activeChar.getAllyCrestId());
			packet.writeC(_activeChar.isInMatchingRoom() ? 0x01 : 0x00);
		}
		
		if (containsMask(UserInfoType.SOCIAL))
		{
			packet.writeH(22);
			packet.writeC(_activeChar.getPvpFlag());
			packet.writeD(_activeChar.getKarma()); // Reputation
			packet.writeC(_activeChar.isNoble() ? 0x01 : 0x00);
			packet.writeC(_activeChar.isHero() ? 0x01 : 0x00);
			packet.writeC(_activeChar.getPledgeClass());
			packet.writeD(_activeChar.getPkKills());
			packet.writeD(_activeChar.getPvpKills());
			packet.writeD(_activeChar.getRecomLeft());
		}
		
		if (containsMask(UserInfoType.VITA_FAME))
		{
			packet.writeH(15);
			packet.writeD(_activeChar.getVitalityPoints());
			packet.writeC(0x00); // Vita Bonus
			packet.writeD(_activeChar.getFame());
			packet.writeD(RaidBossPointsManager.getInstance().getPointsByOwnerId(_activeChar.getObjectId()));
		}
		
		if (containsMask(UserInfoType.SLOTS))
		{
			packet.writeH(9);
			packet.writeC(_activeChar.getInventory().getTalismanSlots()); // Confirmed
			packet.writeC(_activeChar.getInventory().getBroochJewelSlots()); // Confirmed
			packet.writeC(_activeChar.getTeam().getId()); // Confirmed
			packet.writeC(0x00); // (1 = Red, 2 = White, 3 = White Pink) dotted ring on the floor
			packet.writeC(0x00);
			packet.writeC(0x00);
			packet.writeC(0x00);
		}
		
		if (containsMask(UserInfoType.MOVEMENTS))
		{
			packet.writeH(4);
			packet.writeC(_activeChar.isInsideZone(ZoneId.WATER) ? 1 : _activeChar.isFlyingMounted() ? 2 : 0);
			packet.writeC(_activeChar.isRunning() ? 0x01 : 0x00);
		}
		
		if (containsMask(UserInfoType.COLOR))
		{
			packet.writeH(10);
			packet.writeD(_activeChar.getAppearance().getNameColor());
			packet.writeD(_activeChar.getAppearance().getTitleColor());
		}
		
		if (containsMask(UserInfoType.INVENTORY_LIMIT))
		{
			packet.writeH(9);
			packet.writeH(0x00);
			packet.writeH(0x00);
			packet.writeH(_activeChar.getInventoryLimit());
			packet.writeC(0x00); // if greater than 1 show the attack cursor when interacting
		}
		
		if (containsMask(UserInfoType.UNK_3))
		{
			packet.writeH(9);
			packet.writeC(0x00);
			packet.writeH(0x00);
			packet.writeD(0x00);
		}
		
		return true;
	}
	
	private int calculateRelation(PlayerInstance activeChar)
	{
		int relation = 0;
		final Party party = activeChar.getParty();
		final L2Clan clan = activeChar.getClan();
		
		if (party != null)
		{
			relation |= 0x08; // Party member
			if (party.getLeader() == _activeChar)
			{
				relation |= 0x10; // Party leader
			}
		}
		
		if (clan != null)
		{
			relation |= 0x20; // Clan member
			if (clan.getLeaderId() == activeChar.getObjectId())
			{
				relation |= 0x40; // Clan leader
			}
		}
		
		if (activeChar.isInSiege())
		{
			relation |= 0x80; // In siege
		}
		
		return relation;
	}
}
