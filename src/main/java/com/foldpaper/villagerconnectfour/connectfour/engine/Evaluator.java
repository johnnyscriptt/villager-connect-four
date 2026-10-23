package com.foldpaper.villagerconnectfour.connectfour.engine;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;

/**
 * Static evaluation, scored from {@link Board#YELLOW}'s point of view and negated for red by the
 * caller. Weights live in one block so the whole personality is tunable in one place.
 *
 * <h2>Parity - the mechanism that makes an Expert</h2>
 *
 * <p>This is the one genuinely Connect-Four-specific idea in the engine, and it is what
 * separates rung 4 from rung 3.
 *
 * <p>Every column holds an even number of cells, and the board holds an even number in total. In
 * a pure filling race - both sides simply dropping discs with nothing else going on - yellow
 * takes the 1st, 3rd and 5th cell of a column and red takes the 2nd, 4th and 6th. Counting rows
 * from zero at the bottom, that means the cells at rows 0, 2 and 4 fall to <b>yellow</b>, the
 * side that moved first, and rows 1, 3 and 5 fall to <b>red</b>.
 *
 * <p>So a threat is only worth much if its row falls to the player who owns it. A yellow threat
 * on row 3 is nearly worthless: red controls when that cell gets filled and will simply never
 * fill it, or will fill it themselves. The same threat on row 2 is close to winning, because
 * yellow can wait and red eventually has to hand it over.
 *
 * <p>An engine that counts threats without weighing their parity - the Journeyman - builds the
 * wrong ones and hands its opponent the right ones. That is a completely different mistake from
 * simply not searching deep enough, which is exactly what the ladder is supposed to be made of.
 *
 * <p>This is why {@link Board} ties colour to move order rather than to a player: the whole
 * argument above rests on yellow being the side that moved first, so it stays a fixed invariant
 * rather than something that flips with a config key.
 */
final class Evaluator {
	/** A threat whose row falls to its owner in the filling race. */
	private static final int THREAT_RIGHT_PARITY = 40;
	/** A threat whose row falls to the opponent - real, but they decide when it is reached. */
	private static final int THREAT_WRONG_PARITY = 4;
	/** Threat weight when the profile does not understand parity at all. */
	private static final int THREAT_FLAT = 18;
	/** A threat sitting on a cell that can be played right now is worth more than a distant one. */
	private static final int THREAT_IMMEDIATE_BONUS = 12;
	/** Per disc, scaled by how central its column is. */
	private static final int CENTRE_COLUMN = 6;

	private Evaluator() {}

	/** Score in threat-hundredths from yellow's perspective. */
	static int evaluate(Board board, DifficultyProfile profile) {
		long yellow = board.sideToMove() == Board.YELLOW ? board.position() : board.opponent();
		long red = yellow ^ board.mask();

		int score = centre(board.geometry(), yellow) - centre(board.geometry(), red);
		score += threats(board, yellow, Board.YELLOW, profile);
		score -= threats(board, red, Board.RED, profile);
		return score;
	}

	/** Discs weighted by how close their column is to the middle. */
	private static int centre(Geometry geometry, long discs) {
		int score = 0;
		int middle = geometry.width() / 2;
		for (int col = 0; col < geometry.width(); col++) {
			int weight = middle + 1 - Math.abs(col - middle);
			score += CENTRE_COLUMN * weight * Long.bitCount(discs & geometry.columnMask(col));
		}
		return score;
	}

	/** The value of every cell that would complete a line for {@code discs}. */
	private static int threats(Board board, long discs, int owner, DifficultyProfile profile) {
		Geometry geometry = board.geometry();
		long spots = geometry.winningSpots(discs, board.mask());
		long playable = board.possibleMoves();

		int score = 0;
		for (long rest = spots; rest != 0; rest &= rest - 1) {
			int bit = Long.numberOfTrailingZeros(rest);
			int row = geometry.rowOf(bit);
			if (profile.parityAware()) {
				// Rows 0, 2, 4 fall to yellow in the filling race; rows 1, 3, 5 fall to red.
				boolean rowFallsToYellow = (row & 1) == 0;
				boolean rightParity = rowFallsToYellow == (owner == Board.YELLOW);
				score += rightParity ? THREAT_RIGHT_PARITY : THREAT_WRONG_PARITY;
			} else {
				score += THREAT_FLAT;
			}
			if ((playable & (1L << bit)) != 0) score += THREAT_IMMEDIATE_BONUS;
		}
		return score;
	}
}
