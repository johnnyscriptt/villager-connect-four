package com.foldpaper.villagerconnectfour.connectfour.engine;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.GameStatus;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;
import com.foldpaper.villagerconnectfour.connectfour.MoveSequence;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real correctness oracle for the solver.
 *
 * <p>Nothing about the bitboards, the transposition table or the non-losing-move pruning is
 * obviously right, and a subtly wrong solver still plays plausible moves. So this test writes a
 * second, deliberately naive solver - a plain grid, a plain four-direction scan, no pruning
 * beyond memoisation - and demands the two agree on the exact score of every position it can
 * reach on a board small enough to settle completely.
 *
 * <p>Two independent implementations agreeing is a genuine oracle, and it depends on no
 * published number. The small-board results it reproduces along the way (4x4 and 5x4 are both
 * draws) are noted only as corroboration; the assertions are against the brute force.
 */
class SmallBoardOracleTest {

	// ------------------------------------------------------- the naive solver

	/** A position as a plain grid, sharing no code at all with {@link Board}. */
	private static final class Grid {
		final int width, height;
		final int[][] cell;     // [col][row], 0 empty, 1 yellow, -1 red
		final int[] filled;
		int moves;
		int side = 1;           // yellow moves first

		Grid(int width, int height) {
			this.width = width;
			this.height = height;
			this.cell = new int[width][height];
			this.filled = new int[width];
		}

		boolean canDrop(int col) {
			return filled[col] < height;
		}

		void drop(int col) {
			cell[col][filled[col]++] = side;
			side = -side;
			moves++;
		}

		void undrop(int col) {
			side = -side;
			cell[col][--filled[col]] = 0;
			moves--;
		}

		String key() {
			StringBuilder out = new StringBuilder();
			for (int col = 0; col < width; col++) {
				for (int row = 0; row < height; row++) out.append(cell[col][row] + 1);
			}
			return out.toString();
		}

		/** Four in a row for {@code colour}, counted the slow obvious way. */
		boolean wins(int colour) {
			for (int col = 0; col < width; col++) {
				for (int row = 0; row < height; row++) {
					if (cell[col][row] != colour) continue;
					for (int[] dir : new int[][] {{1, 0}, {0, 1}, {1, 1}, {1, -1}}) {
						int run = 0;
						for (int i = 0; i < 4; i++) {
							int c = col + i * dir[0], r = row + i * dir[1];
							if (c < 0 || c >= width || r < 0 || r >= height) break;
							if (cell[c][r] != colour) break;
							run++;
						}
						if (run == 4) return true;
					}
				}
			}
			return false;
		}
	}

	private final Map<String, Integer> memo = new HashMap<>();

	/** Exact score from the side to move's point of view, in the same units as the engine. */
	private int brute(Grid grid, int ply) {
		if (grid.wins(-grid.side)) return -(Engine.MATE - ply);
		if (grid.moves == grid.width * grid.height) return 0;

		String key = grid.key();
		Integer cached = memo.get(key);
		if (cached != null) return cached;

		int best = Integer.MIN_VALUE;
		for (int col = 0; col < grid.width; col++) {
			if (!grid.canDrop(col)) continue;
			grid.drop(col);
			best = Math.max(best, -brute(grid, ply + 1));
			grid.undrop(col);
		}
		memo.put(key, best);
		return best;
	}

	// ------------------------------------------------------------- the checks

	private static Grid gridOf(Board board) {
		Geometry geometry = board.geometry();
		Grid grid = new Grid(geometry.width(), geometry.height());
		for (int col : board.history()) grid.drop(col);
		return grid;
	}

	/**
	 * One table for the whole test. Reusing it is not just an optimisation - a proven score is
	 * true regardless of which search found it, so carrying the table between positions is the
	 * same thing the Master does between moves in a real game.
	 */
	private final TranspositionTable table = new TranspositionTable(18);

	private int solved(Board board) {
		return Engine.solveExact(board, table, () -> false);
	}

	@Test
	void theSolverMatchesBruteForceOnEveryEarlyFourByFourPosition() {
		Geometry geometry = new Geometry(4, 4);
		int[] checked = {0};
		walk(new Board(geometry), 6, checked);
		assertTrue(checked[0] > 200, "expected a decent corpus, checked " + checked[0]);
	}

	private void walk(Board board, int depth, int[] checked) {
		if (board.status() != GameStatus.ONGOING) return;
		int expected = brute(gridOf(board), board.moves());
		int actual = solved(board);
		assertEquals(expected, actual,
				"disagreement on \"" + MoveSequence.render(board) + "\" ("
						+ board.geometry() + ")");
		checked[0]++;
		if (depth == 0) return;
		for (int col : board.legalMoves()) {
			board.play(col);
			walk(board, depth - 1, checked);
			board.unplay();
		}
	}

	@Test
	void fourByFourIsADrawFromTheStart() {
		Board board = new Board(new Geometry(4, 4));
		int expected = brute(gridOf(board), 0);
		assertEquals(expected, solved(board));
		// Corroboration only: the small boards where neither side can force a win are known,
		// and 4x4 is one of them. The assertion above is what actually holds the test up.
		assertEquals(0, expected, "4x4 Connect Four is a draw with perfect play");
	}

	@Test
	void fiveByFourIsADrawFromTheStart() {
		Board board = new Board(new Geometry(5, 4));
		int expected = brute(gridOf(board), 0);
		assertEquals(expected, solved(board));
		assertEquals(0, expected, "5x4 Connect Four is a draw with perfect play");
	}

	@Test
	void theSolverMatchesBruteForceOnEveryEarlyFiveByFourPosition() {
		Geometry geometry = new Geometry(5, 4);
		int[] checked = {0};
		walk(new Board(geometry), 4, checked);
		assertTrue(checked[0] > 200, "expected a decent corpus, checked " + checked[0]);
	}

	/**
	 * Wider boards are deliberately not brute-forced here. The naive solver memoises on a string
	 * per position, and at 7x4 that is millions of entries - it would dominate the build for no
	 * extra confidence. The 4x4 and 5x4 corpora already exercise every mechanism in the search.
	 */
	@Test
	void aProvenWinCanActuallyBeConverted() {
		// A different kind of check, and one that scales: take the solver's word that a position
		// is won, then play it out against every defence and confirm it really does win. This
		// tests the score and the move choice together, which comparing numbers alone does not.
		Geometry geometry = new Geometry(5, 4);
		Board board = new Board(geometry);
		board.play(2);
		board.play(0);
		int score = solved(board);
		if (score <= 0) return;   // not a won position; nothing to convert
		assertTrue(playsItOut(board, board.sideToMove()), "the solver could not convert its own win");
	}

	/** Follows the solver for {@code winner} and every reply for the other side. */
	private boolean playsItOut(Board board, int winner) {
		GameStatus status = board.status();
		if (status.isOver()) {
			return status == (winner == Board.YELLOW ? GameStatus.WIN_YELLOW : GameStatus.WIN_RED);
		}
		if (board.sideToMove() == winner) {
			int best = -1, bestScore = Integer.MIN_VALUE;
			for (int col : board.legalMoves()) {
				board.play(col);
				int score = -solved(board);
				board.unplay();
				if (score > bestScore) {
					bestScore = score;
					best = col;
				}
			}
			board.play(best);
			boolean won = playsItOut(board, winner);
			board.unplay();
			return won;
		}
		for (int col : board.legalMoves()) {
			board.play(col);
			boolean won = playsItOut(board, winner);
			board.unplay();
			if (!won) return false;
		}
		return true;
	}
}
