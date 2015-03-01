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

import java.util.Set;

import org.l2junity.Config;
import org.l2junity.gameserver.instancemanager.CursedWeaponsManager;
import org.l2junity.gameserver.model.PcCondOverride;
import org.l2junity.gameserver.model.actor.Decoy;
import org.l2junity.gameserver.model.actor.instance.PlayerInstance;
import org.l2junity.gameserver.model.itemcontainer.Inventory;
import org.l2junity.gameserver.model.skills.AbnormalVisualEffect;
import org.l2junity.gameserver.model.zone.ZoneId;
import org.l2junity.gameserver.network.OutgoingPackets;
import org.l2junity.network.PacketWriter;

public class CharInfo implements IGameServerPacket
{
	private final PlayerInstance _activeChar;
	private int _objId;
	private int _x, _y, _z, _heading;
	private final int _mAtkSpd, _pAtkSpd;
	
	private final int _runSpd, _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private final float _attackSpeedMultiplier;
	private int _enchantLevel = 0;
	private int _armorEnchant = 0;
	
	private int _vehicleId = 0;
	
	private static final int[] PAPERDOLL_ORDER = new int[]
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2
	};
	
	public CharInfo(PlayerInstance cha)
	{
		_activeChar = cha;
		_objId = cha.getObjectId();
		if ((_activeChar.getVehicle() != null) && (_activeChar.getInVehiclePosition() != null))
		{
			_x = _activeChar.getInVehiclePosition().getX();
			_y = _activeChar.getInVehiclePosition().getY();
			_z = _activeChar.getInVehiclePosition().getZ();
			_vehicleId = _activeChar.getVehicle().getObjectId();
		}
		else
		{
			_x = _activeChar.getX();
			_y = _activeChar.getY();
			_z = _activeChar.getZ();
		}
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_moveMultiplier = cha.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(cha.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(cha.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(cha.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(cha.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = cha.isFlying() ? _runSpd : 0;
		_flyWalkSpd = cha.isFlying() ? _walkSpd : 0;
		_enchantLevel = cha.getInventory().getWeaponEnchant();
		_armorEnchant = cha.getInventory().getArmorMinEnchant();
	}
	
	public CharInfo(Decoy decoy)
	{
		this(decoy.getActingPlayer()); // init
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		final boolean gmSeeInvis = _activeChar.isInvisible() && _activeChar.canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS);
		
		OutgoingPackets.CHAR_INFO.writeId(packet);
		
		packet.writeD(_x); // Confirmed
		packet.writeD(_y); // Confirmed
		packet.writeD(_z); // Confirmed
		packet.writeD(_vehicleId); // Confirmed
		packet.writeD(_objId); // Confirmed
		packet.writeS(_activeChar.getAppearance().getVisibleName()); // Confirmed
		packet.writeH(_activeChar.getRace().ordinal()); // Confirmed
		packet.writeC(_activeChar.getAppearance().getSex() ? 0x01 : 0x00); // Confirmed
		packet.writeD(_activeChar.getBaseClass()); // Confirmed
		
		for (int slot : getPaperdollOrder())
		{
			packet.writeD(_activeChar.getInventory().getPaperdollItemDisplayId(slot)); // Confirmed
		}
		
		for (int slot : getPaperdollOrderAugument())
		{
			packet.writeD(_activeChar.getInventory().getPaperdollAugmentationId(slot)); // Confirmed
		}
		
		packet.writeC(_armorEnchant);
		
		for (int slot : getPaperdollOrderVisualId())
		{
			packet.writeD(_activeChar.getInventory().getPaperdollItemVisualId(slot));
		}
		
		packet.writeC(_activeChar.getPvpFlag());
		packet.writeD(_activeChar.getKarma());
		
		packet.writeD(_mAtkSpd);
		packet.writeD(_pAtkSpd);
		
		packet.writeH(_runSpd);
		packet.writeH(_walkSpd);
		packet.writeH(_swimRunSpd);
		packet.writeH(_swimWalkSpd);
		packet.writeH(_flyRunSpd);
		packet.writeH(_flyWalkSpd);
		packet.writeH(_flyRunSpd);
		packet.writeH(_flyWalkSpd);
		packet.writeF(_moveMultiplier);
		packet.writeF(_attackSpeedMultiplier);
		
		packet.writeF(_activeChar.getCollisionRadius());
		packet.writeF(_activeChar.getCollisionHeight());
		
		packet.writeD(_activeChar.getVisualHair());
		packet.writeD(_activeChar.getVisualHairColor());
		packet.writeD(_activeChar.getVisualFace());
		
		packet.writeS(gmSeeInvis ? "Invisible" : _activeChar.getAppearance().getVisibleTitle());
		
		if (!_activeChar.isCursedWeaponEquipped())
		{
			packet.writeD(_activeChar.getClanId());
			packet.writeD(_activeChar.getClanCrestId());
			packet.writeD(_activeChar.getAllyId());
			packet.writeD(_activeChar.getAllyCrestId());
		}
		else
		{
			packet.writeD(0x00);
			packet.writeD(0x00);
			packet.writeD(0x00);
			packet.writeD(0x00);
		}
		
		packet.writeC(_activeChar.isSitting() ? 0x00 : 0x01); // Confirmed
		packet.writeC(_activeChar.isRunning() ? 0x01 : 0x00); // Confirmed
		packet.writeC(_activeChar.isInCombat() ? 0x01 : 0x00); // Confirmed
		
		packet.writeC(!_activeChar.isInOlympiadMode() && _activeChar.isAlikeDead() ? 0x01 : 0x00); // Confirmed
		
		packet.writeC(!gmSeeInvis /* && isInvisible() */? 0x01 : 0x00); // TODO: Find me!
		
		packet.writeC(_activeChar.getMountType().ordinal()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		packet.writeC(_activeChar.getPrivateStoreType().getId()); // Confirmed
		
		packet.writeH(_activeChar.getCubics().size()); // Confirmed
		_activeChar.getCubics().keySet().forEach(packet::writeH);
		
		packet.writeC(_activeChar.isInMatchingRoom() ? 0x01 : 0x00); // Confirmed
		
		packet.writeC(_activeChar.isInsideZone(ZoneId.WATER) ? 1 : _activeChar.isFlyingMounted() ? 2 : 0);
		packet.writeH(_activeChar.getRecomHave()); // Confirmed
		packet.writeD(_activeChar.getMountNpcId() == 0 ? 0 : _activeChar.getMountNpcId() + 1000000);
		
		packet.writeD(_activeChar.getClassId().getId()); // Confirmed
		packet.writeD(0x00); // TODO: Find me!
		packet.writeC(_activeChar.isMounted() ? 0 : _enchantLevel); // Confirmed
		
		packet.writeC(_activeChar.getTeam().getId()); // Confirmed
		
		packet.writeD(_activeChar.getClanCrestLargeId());
		packet.writeC(_activeChar.isNoble() ? 1 : 0); // Confirmed
		packet.writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // Confirmed
		
		packet.writeC(_activeChar.isFishing() ? 1 : 0); // Confirmed
		packet.writeD(_activeChar.getFishx()); // Confirmed
		packet.writeD(_activeChar.getFishy()); // Confirmed
		packet.writeD(_activeChar.getFishz()); // Confirmed
		
		packet.writeD(_activeChar.getAppearance().getNameColor()); // Confirmed
		
		packet.writeD(_heading); // Confirmed
		
		packet.writeC(_activeChar.getPledgeClass());
		packet.writeH(_activeChar.getPledgeType());
		
		packet.writeD(_activeChar.getAppearance().getTitleColor()); // Confirmed
		
		packet.writeC(_activeChar.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquippedId()) : 0); // TODO: Find me!
		
		packet.writeD(_activeChar.getClanId() > 0 ? _activeChar.getClan().getReputationScore() : 0);
		packet.writeD(_activeChar.getTransformationDisplayId()); // Confirmed
		packet.writeD(_activeChar.getAgathionId()); // Confirmed
		
		packet.writeC(0x00); // TODO: Find me!
		
		packet.writeD((int) Math.round(_activeChar.getCurrentCp())); // Confirmed
		packet.writeD(_activeChar.getMaxHp()); // Confirmed
		packet.writeD((int) Math.round(_activeChar.getCurrentHp())); // Confirmed
		packet.writeD(_activeChar.getMaxMp()); // Confirmed
		packet.writeD((int) Math.round(_activeChar.getCurrentMp())); // Confirmed
		
		packet.writeC(0x00); // TODO: Find me!
		final Set<AbnormalVisualEffect> abnormalVisualEffects = _activeChar.getCurrentAbnormalVisualEffects();
		packet.writeD(abnormalVisualEffects.size()); // Confirmed
		for (AbnormalVisualEffect abnormalVisualEffect : abnormalVisualEffects)
		{
			packet.writeH(abnormalVisualEffect.getClientId()); // Confirmed
		}
		packet.writeC(0x00); // TODO: Find me!
		packet.writeC(_activeChar.isHairAccessoryEnabled() ? 0x01 : 0x00); // Hair accessory
		packet.writeC(_activeChar.getAbilityPointsUsed()); // Used Ability Points
		return true;
	}
	
	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
