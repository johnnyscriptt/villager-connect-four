package com.foldpaper.villagerconnectfour.connectfour.engine;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.GameStatus;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;
import com.foldpaper.villagerconnectfour.connectfour.MoveSequence;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Properties the solver must satisfy against itself. These catch the class of bug that the
 * brute-force oracle in {@link SmallBoardOracleTest} is too slow to reach - above all a broken
 * transposition key, which produces scores that are wrong only sometimes and only on positions
 * that happen to collide.
 */
class SolverSelfConsistencyTest {
	private static final Geometry SMALL = new Geometry(5, 4);

	private static int solve(Board board, TranspositionTable table) {
		return Engine.solveExact(board, table, () -> false);
	}

	@Test
	void theTableNeverChangesAnAnswer() {
		// The sharpest test of the key: identical scores with the table on and off.
		Random random = new Random(1234L);
		TranspositionTable table = new TranspositionTable(18);
		int checked = 0;
		for (int game = 0; game < 60; game++) {
			Board board = new Board(SMALL);
			while (board.status() == GameStatus.ONGOING) {
				assertEquals(solve(board, null), solve(board, table),
						"table changed the score of \"" + MoveSequence.render(board) + "\"");
				checked++;
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
		assertTrue(checked > 300, "expected a decent sample, got " + checked);
	}

	@Test
	void aScoreIsTheNegationOfTheBestReply() {
		// The defining recurrence of negamax, checked from the outside.
		Random random = new Random(99L);
		TranspositionTable table = new TranspositionTable(18);
		for (int game = 0; game < 40; game++) {
			Board board = new Board(SMALL);
			while (board.status() == GameStatus.ONGOING) {
				int score = solve(board, table);
				int best = Integer.MIN_VALUE;
				for (int col : board.legalMoves()) {
					board.play(col);
					best = Math.max(best, -solve(board, table));
					board.unplay();
				}
				assertEquals(best, score, "at \"" + MoveSequence.render(board) + "\"");
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
	}

	@Test
	void aWonPositionStaysWonForTheSameSide() {
		// Following the best move must never flip the sign of the result.
		TranspositionTable table = new TranspositionTable(18);
		Board board = new Board(SMALL);
		while (board.status() == GameStatus.ONGOING) {
			int before = solve(board, table);
			int best = Integer.MIN_VALUE, chosen = -1;
			for (int col : board.legalMoves()) {
				board.play(col);
				int score = -solve(board, table);
				board.unplay();
				if (score > best) {
					best = score;
					chosen = col;
				}
			}
			board.play(chosen);
			if (board.status() == GameStatus.ONGOING) {
				assertEquals(-before, solve(board, table),
						"best play changed the value at \"" + MoveSequence.render(board) + "\"");
			}
		}
		// Both sides played perfectly throughout, so a drawn game is what should come out.
		assertEquals(GameStatus.DRAW_FULL, board.status(), "5x4 is a draw with perfect play");
	}

	@Test
	void aTerminalPositionScoresAsALoss() {
		Board won = MoveSequence.parse("1122334");
		assertEquals(GameStatus.WIN_YELLOW, won.status());
		assertEquals(Board.RED, won.sideToMove());
		// Red is to move and has already lost. The margin is measured in discs on the board, not
		// in plies from wherever a search happened to start, so it is 7 here however it is asked.
		assertEquals(7, won.moves());
		assertEquals(-(Engine.MATE - 7), solve(won, null));
	}

	@Test
	void aScoreDoesNotDependOnWhereTheSearchStarted() {
		// The property the whole transposition scheme rests on: reaching a position as the root
		// of one search and as a child of another must produce the same number.
		Board root = new Board(SMALL);
		root.play(2);
		root.play(0);
		int asRoot = solve(root, null);

		Board start = new Board(SMALL);
		int fromStart = Integer.MIN_VALUE;
		for (int col : start.legalMoves()) {
			if (col != 2) continue;
			start.play(col);
			start.play(0);
			fromStart = solve(start, null);
		}
		assertEquals(asRoot, fromStart);
	}

	@Test
	void mirroredPositionsScoreTheSame() {
		Random random = new Random(555L);
		TranspositionTable table = new TranspositionTable(18);
		for (int game = 0; game < 40; game++) {
			Board board = new Board(SMALL);
			while (board.status() == GameStatus.ONGOING) {
				String moves = MoveSequence.render(board);
				StringBuilder mirrored = new StringBuilder();
				for (int i = 0; i < moves.length(); i++) {
					mirrored.append((char) ('1' + SMALL.width() - 1 - (moves.charAt(i) - '1')));
				}
				assertEquals(solve(board, table),
						solve(MoveSequence.parse(mirrored.toString(), SMALL), table), moves);
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
	}
}
