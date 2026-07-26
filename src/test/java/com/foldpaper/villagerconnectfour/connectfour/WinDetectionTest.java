package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Win detection, checked against an oracle the test builds itself rather than against any
 * published number: every set of four collinear cells must be a win, and a plain scan over a
 * grid must agree with the bitboard on random real positions.
 *
 * <p>The two named anti-tests are the whole reason {@link Geometry} wastes a bit per column. A
 * layout without the sentinel row would report four in a row for discs that merely sit either
 * side of a column boundary, and the bug would look like an engine bug for days.
 */
class WinDetectionTest {
	private static final Geometry G = Geometry.STANDARD;

	private static long bit(int col, int row) {
		return 1L << G.bitOf(col, row);
	}

	@Test
	void everyCollinearFourIsAWin() {
		int lines = 0;
		for (int[] dir : new int[][] {{1, 0}, {0, 1}, {1, 1}, {1, -1}}) {
			for (int col = 0; col < G.width(); col++) {
				for (int row = 0; row < G.height(); row++) {
					int endCol = col + 3 * dir[0], endRow = row + 3 * dir[1];
					if (endCol < 0 || endCol >= G.width() || endRow < 0 || endRow >= G.height()) continue;
					long discs = 0;
					for (int i = 0; i < 4; i++) discs |= bit(col + i * dir[0], row + i * dir[1]);
					assertTrue(G.isWin(discs),
							"missed a line from (" + col + "," + row + ") direction " + dir[0] + "," + dir[1]);
					lines++;
				}
			}
		}
		// 24 horizontal + 21 vertical + 12 of each diagonal.
		assertEquals(69, lines, "the 7x6 board has 69 four-in-a-row lines");
	}

	@Test
	void threeAtopAColumnPlusOneBesideItIsNotAWin() {
		// Rows 3,4,5 of column 0 and row 0 of column 1. Contiguous in a layout with no sentinel
		// row, and so a phantom vertical win; here the sentinel keeps them apart.
		long discs = bit(0, 3) | bit(0, 4) | bit(0, 5) | bit(1, 0);
		assertFalse(G.isWin(discs));
	}

	@Test
	void aRunThatWouldWrapTheRightEdgeIsNotAWin() {
		// The last two cells of the top row and the first two of the bottom row: adjacent only
		// if the board is treated as one flat array.
		long discs = bit(5, 5) | bit(6, 5) | bit(0, 0) | bit(1, 0);
		assertFalse(G.isWin(discs));
	}

	@Test
	void threeInARowIsNotAWin() {
		assertFalse(G.isWin(bit(0, 0) | bit(1, 0) | bit(2, 0)));
		assertFalse(G.isWin(bit(3, 0) | bit(3, 1) | bit(3, 2)));
		assertFalse(G.isWin(bit(0, 0) | bit(1, 1) | bit(2, 2)));
	}

	@Test
	void aGapBreaksALine() {
		// X X . X across the bottom row.
		assertFalse(G.isWin(bit(0, 0) | bit(1, 0) | bit(3, 0)));
	}

	@Test
	void bitboardAgreesWithAPlainScanOnRealPositions() {
		Random random = new Random(20260726L);
		int checked = 0, wins = 0;
		for (int game = 0; game < 3000; game++) {
			Board board = new Board(G);
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
				// The side that just moved is the one that could have completed a line.
				int mover = -board.sideToMove();
				assertEquals(scanForWin(board, mover), G.isWin(board.opponent()),
						"disagreement after " + MoveSequence.render(board));
				checked++;
			}
			if (board.status() != GameStatus.DRAW_FULL) wins++;
		}
		assertTrue(checked > 50_000, "expected a decent sample, got " + checked);
		assertTrue(wins > 0, "random play should produce some wins");
	}

	/** Deliberately naive: walk every cell in every direction and count. */
	private static boolean scanForWin(Board board, int colour) {
		for (int col = 0; col < G.width(); col++) {
			for (int row = 0; row < G.height(); row++) {
				if (board.discAt(col, row) != colour) continue;
				for (int[] dir : new int[][] {{1, 0}, {0, 1}, {1, 1}, {1, -1}}) {
					int run = 0;
					for (int i = 0; i < 4; i++) {
						int c = col + i * dir[0], r = row + i * dir[1];
						if (c < 0 || c >= G.width() || r < 0 || r >= G.height()) break;
						if (board.discAt(c, r) != colour) break;
						run++;
					}
					if (run == 4) return true;
				}
			}
		}
		return false;
	}
}
