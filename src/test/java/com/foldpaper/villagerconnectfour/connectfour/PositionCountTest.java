package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The nearest honest analogue of the Checkers sibling's perft test.
 *
 * <p>Perft proper is vacuous here: there are never more than seven moves and the count from any
 * position is just the number of columns with room, so a perft assertion would restate the code
 * rather than check it. What is worth testing is that the two things the search relies on —
 * {@link Board#key()} being unique per position, and {@link Board#discAt} agreeing with the
 * bitboards — hold across a large slice of the game tree.
 *
 * <p>So the tree is walked once and every reachable position counted <em>twice</em>: once keyed
 * by {@code key()}, once keyed by a grid written out cell by cell. Two independent encodings
 * that agree at every ply is the oracle; no external table is needed, and the low plies are
 * hand-derivable besides.
 */
class PositionCountTest {

	/** Distinct positions at each ply, counted both ways. Index 0 is the empty board. */
	private static long[][] countBothWays(Geometry geometry, int maxPly) {
		long[] byKey = new long[maxPly + 1];
		long[] byGrid = new long[maxPly + 1];

		List<Board> frontier = new ArrayList<>();
		frontier.add(new Board(geometry));
		byKey[0] = 1;
		byGrid[0] = 1;

		for (int ply = 1; ply <= maxPly; ply++) {
			Map<Long, Board> nextByKey = new HashMap<>();
			Set<String> nextByGrid = new HashSet<>();
			for (Board board : frontier) {
				for (int col : board.legalMoves()) {
					board.play(col);
					if (!nextByKey.containsKey(board.key())) nextByKey.put(board.key(), board.copy());
					nextByGrid.add(grid(board));
					board.unplay();
				}
			}
			byKey[ply] = nextByKey.size();
			byGrid[ply] = nextByGrid.size();
			frontier = new ArrayList<>(nextByKey.values());
		}
		return new long[][] {byKey, byGrid};
	}

	/** Every cell written out — an encoding that shares no code with {@link Board#key()}. */
	private static String grid(Board board) {
		StringBuilder out = new StringBuilder();
		for (int col = 0; col < board.geometry().width(); col++) {
			for (int row = 0; row < board.geometry().height(); row++) {
				int disc = board.discAt(col, row);
				out.append(disc == Board.EMPTY ? '.' : disc == Board.YELLOW ? 'y' : 'r');
			}
		}
		return out.toString();
	}

	@Test
	void theTwoEncodingsAgreeAtEveryPly() {
		long[][] counts = countBothWays(Geometry.STANDARD, 8);
		for (int ply = 0; ply < counts[0].length; ply++) {
			assertEquals(counts[1][ply], counts[0][ply],
					"key() and the grid encoding disagree at ply " + ply);
		}
		assertTrue(counts[0][8] > 100_000, "expected the tree to open up, got " + counts[0][8]);
	}

	@Test
	void theFirstFewPliesAreHandDerivable() {
		long[] byKey = countBothWays(Geometry.STANDARD, 3)[0];
		// One empty board; seven first moves; 49 ordered pairs, all distinct because the two
		// colours land in different columns. No line can exist before ply 7, so nothing is
		// pruned this early.
		assertEquals(1, byKey[0]);
		assertEquals(7, byKey[1]);
		assertEquals(49, byKey[2]);
		// Ply 3 is below 343 only because different move orders transpose to the same position.
		assertTrue(byKey[3] < 343, "transpositions should collapse some of the 343 sequences");
	}

	@Test
	void countsAgreeOnASmallBoardToo() {
		long[][] counts = countBothWays(new Geometry(5, 4), 8);
		for (int ply = 0; ply < counts[0].length; ply++) {
			assertEquals(counts[1][ply], counts[0][ply], "disagreement at ply " + ply);
		}
		assertEquals(1, counts[0][0]);
		assertEquals(5, counts[0][1]);
		assertEquals(25, counts[0][2]);
	}

	@Test
	void aFinishedGameStopsBranching() {
		// legalMoves() is empty once someone has won, so won positions are leaves in the walk.
		Board won = MoveSequence.parse("1122334");
		assertEquals(GameStatus.WIN_YELLOW, won.status());
		assertEquals(0, won.legalMoves().length);
	}
}
