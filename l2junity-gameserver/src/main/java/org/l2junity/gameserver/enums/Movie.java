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
package org.l2junity.gameserver.enums;

/**
 * @author St3eT
 */
public enum Movie
{
	SC_LINDVIOR(1),
	SC_ECHMUS_OPENING(2),
	SC_ECHMUS_SUCCESS(3),
	SC_ECHMUS_FAIL(4),
	SC_BOSS_TIAT_OPENING(5),
	SC_BOSS_TIAT_ENDING_SUCCES(6),
	SC_BOSS_TIAT_ENDING_FAIL(7),
	SSQ_SUSPICIOUS_DEATHS(8),
	SSQ_DYING_MASSAGE(9),
	SSQ_CONTRACT_OF_MAMMON(10),
	SSQ_RITUAL_OF_PRIEST(11),
	SSQ_SEALING_EMPEROR_1ST(12),
	SSQ_SEALING_EMPEROR_2ND(13),
	SSQ_EMBRYO(14),
	SC_BOSS_FREYA_OPENING(15),
	SC_BOSS_FREYA_PHASECH_A(16),
	SC_BOSS_FREYA_PHASECH_B(17),
	SC_BOSS_KEGOR_INTRUSION(18),
	SC_BOSS_FREYA_ENDING_A(19),
	SC_BOSS_FREYA_ENDING_B(20),
	SC_BOSS_FREYA_FORCED_DEFEAT(21),
	SC_BOSS_FREYA_DEFEAT(22),
	SC_ICE_HEAVYKNIGHT_SPAWN(23),
	SSQ2_HOLY_BURIAL_GROUND_OPENING(24),
	SSQ2_HOLY_BURIAL_GROUND_CLOSING(25),
	SSQ2_SOLINA_TOMB_OPENING(26),
	SSQ2_SOLINA_TOMB_CLOSING(27),
	SSQ2_ELYSS_NARRATION(28),
	SSQ2_BOSS_OPENING(29),
	SSQ2_BOSS_CLOSING(30),
	SC_ISTINA_OPENING(31),
	SC_ISTINA_ENDING_A(32),
	SC_ISTINA_ENDING_B(33),
	SC_ISTINA_BRIDGE(34),
	SC_OCTABIS_OPENING(35),
	SC_OCTABIS_PHASECH_A(36),
	SC_OCTABIS_PHASECH_B(37),
	SC_OCTABIS_ENDING(38),
	SC_GD1_PROLOGUE(42),
	SC_TALKING_ISLAND_BOSS_OPENING(43),
	SC_TALKING_ISLAND_BOSS_ENDING(44),
	SC_AWAKENING_OPENING(45),
	SC_AWAKENING_BOSS_OPENING(46),
	SC_AWAKENING_BOSS_ENDING_A(47),
	SC_AWAKENING_BOSS_ENDING_B(48),
	SC_EARTHWORM_ENDING(49),
	SC_SPACIA_OPENING(50),
	SC_SPACIA_A(51),
	SC_SPACIA_B(52),
	SC_SPACIA_C(53),
	SC_SPACIA_ENDING(54),
	SC_AWAKENING_VIEW(55),
	SC_AWAKENING_OPENING_C(56),
	SC_AWAKENING_OPENING_D(57),
	SC_AWAKENING_OPENING_E(58),
	SC_AWAKENING_OPENING_F(59),
	SC_TAUTI_OPENING_B(69),
	SC_TAUTI_OPENING(70),
	SC_TAUTI_PHASE(71),
	SC_TAUTI_ENDING(72),
	SC_SOULISLAND_QUEST(73),
	SC_METUCELLAR_OPENING(74),
	SC_SUB_QUEST(75),
	SC_LIND_OPENING(76),
	SC_KATACOMB(77),
	SC_NECRO(78),
	SC_HELLBOUND(79),
	SC_NOBLE_OPENING(99),
	SC_NOBLE_ENDING(100),
	SI_ILLUSION_01_QUE(101),
	SI_ILLUSION_02_QUE(102),
	SI_ILLUSION_03_QUE(103),
	SI_ARKAN_ENTER(104),
	SI_BARLOG_OPENING(105),
	SI_BARLOG_STORY(106),
	SI_ILLUSION_04_QUE(107),
	SI_ILLUSION_05_QUE(108),
	SC_BLOODVEIN_OPENING(109),
	ERT_QUEST_A(110),
	ERT_QUEST_B(111),
	LAND_KSERTH_A(1000),
	LAND_KSERTH_B(1001),
	LAND_UNDEAD_A(1002),
	LAND_DISTRUCTION_A(1003),
	LAND_ANNIHILATION_A(1004),
	G_CARTIA_1_SIN(2001),
	G_CARTIA_2_SIN(2002);
	
	private final int _clientId;
	
	private Movie(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * @return the client id.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	/**
	 * Finds the {@code Movie} by its clientId
	 * @param clientId the clientId
	 * @return the {@code Movie} if its found, {@code null} otherwise.
	 */
	public static Movie findByClientId(int clientId)
	{
		for (Movie movie : values())
		{
			if (movie.getClientId() == clientId)
			{
				return movie;
			}
		}
		return null;
	}
}