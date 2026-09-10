package com.splashhelper;

/**
 * A single hub entry parsed out of the SkyBlock/Dungeon Hub Selector GUI.
 */
public class Hub {
	public final String colour;     // "a" (green) or "c" (red) - taken from the item name's formatting code
	public final String area;       // "SkyBlock" or "Dungeon"
	public final String number;     // hub number, as text (e.g. "7")
	public final String serverName; // e.g. "mega12A"
	public final int players;
	public final int maxPlayers;
	public final int freeSlots;
	public final int slotIndex;     // index into the menu's slot list

	public Hub(String colour, String area, String number, String serverName,
	           int players, int maxPlayers, int slotIndex) {
		this.colour = colour;
		this.area = area;
		this.number = number;
		this.serverName = serverName;
		this.players = players;
		this.maxPlayers = maxPlayers;
		this.freeSlots = maxPlayers - players;
		this.slotIndex = slotIndex;
	}

	public boolean isDungeon() {
		return "Dungeon".equals(area);
	}

	public boolean isRestarting() {
		return players == 0 && maxPlayers == 0;
	}
}
