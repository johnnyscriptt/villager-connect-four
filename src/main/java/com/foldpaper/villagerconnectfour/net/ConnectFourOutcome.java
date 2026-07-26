package com.foldpaper.villagerconnectfour.net;

/**
 * How a Connect Four game against a villager stands or ended, from the player's point of view.
 *
 * <p>There is one draw rather than the Checkers sibling's several: nothing ever leaves the board,
 * so no position can repeat and no quiet-move counter is needed. A game is drawn when the board
 * fills up, and not before.
 *
 * <p><b>The order of these constants is part of the network protocol</b> — the ordinal is what
 * travels — so new outcomes go on the end and existing ones never move.
 */
public enum ConnectFourOutcome {
	ONGOING,
	PLAYER_WON,
	VILLAGER_WON,
	DRAW_FULL,
	PLAYER_RESIGNED,
	ABANDONED;

	private static final ConnectFourOutcome[] VALUES = values();

	public static ConnectFourOutcome byId(int id) {
		return id >= 0 && id < VALUES.length ? VALUES[id] : ABANDONED;
	}

	public boolean isOver() {
		return this != ONGOING;
	}
}
