package com.foldpaper.villagerconnectfour.connectfour;

/**
 * Terminal state of a Connect Four position.
 *
 * <p>There is only one kind of draw. Every move adds a disc and nothing ever moves or is
 * removed, so a position can never repeat and no quiet-move counter is needed — a reader
 * arriving from the Checkers sibling will look for its threefold and forty-move rules and
 * should know they have no analogue here. The game is drawn when the board fills up, and not
 * before.
 */
public enum GameStatus {
	ONGOING,
	/** The side that dropped first completed a line. */
	WIN_YELLOW,
	/** The side that dropped second completed a line. */
	WIN_RED,
	/** Every cell is full with no line. */
	DRAW_FULL;

	public boolean isOver() {
		return this != ONGOING;
	}
}
