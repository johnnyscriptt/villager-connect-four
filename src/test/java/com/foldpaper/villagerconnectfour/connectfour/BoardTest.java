package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

	@Test
	void discsStackFromTheBottom() {
		Board board = Board.startpos();
		assertEquals(0, board.landingRow(3));
		board.play(3);
		assertEquals(1, board.landingRow(3));
		board.play(3);
		assertEquals(2, board.landingRow(3));

		assertEquals(Board.YELLOW, board.discAt(3, 0));
		assertEquals(Board.RED, board.discAt(3, 1));
		assertEquals(Board.EMPTY, board.discAt(3, 2));
	}

	@Test
	void yellowMovesFirstAndSidesAlternate() {
		Board board = Board.startpos();
		assertEquals(Board.YELLOW, board.sideToMove());
		board.play(0);
		assertEquals(Board.RED, board.sideToMove());
		board.play(1);
		assertEquals(Board.YELLOW, board.sideToMove());
	}

	@Test
	void aColumnFillsUpAndThenRefuses() {
		Board board = Board.startpos();
		for (int i = 0; i < board.geometry().height(); i++) {
			assertTrue(board.canPlay(2));
			board.play(2);
		}
		assertFalse(board.canPlay(2));
		assertThrows(IllegalArgumentException.class, () -> board.play(2));
		assertEquals(-1, board.landingRow(2));
	}

	@Test
	void offBoardColumnsAreNotPlayable() {
		Board board = Board.startpos();
		assertFalse(board.canPlay(-1));
		assertFalse(board.canPlay(7));
	}

	@Test
	void playAndUnplayRestoreTheExactPosition() {
		Random random = new Random(7L);
		for (int game = 0; game < 200; game++) {
			Board board = Board.startpos();
			int plies = random.nextInt(20);
			for (int i = 0; i < plies && board.status() == GameStatus.ONGOING; i++) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
			if (board.status() != GameStatus.ONGOING) continue;

			String before = MoveSequence.render(board);
			long position = board.position(), mask = board.mask(), key = board.key();
			int moves = board.moves();
			for (int col : board.legalMoves()) {
				board.play(col);
				board.unplay();
				assertEquals(position, board.position(), "position after play/unplay of " + col);
				assertEquals(mask, board.mask(), "mask after play/unplay of " + col);
				assertEquals(key, board.key(), "key after play/unplay of " + col);
				assertEquals(moves, board.moves());
				assertEquals(before, MoveSequence.render(board));
			}
		}
	}

	@Test
	void unplayOnAnEmptyBoardThrows() {
		assertThrows(IllegalStateException.class, () -> Board.startpos().unplay());
	}

	@Test
	void copyIsIndependent() {
		Board board = MoveSequence.parse("4433");
		Board copy = board.copy();
		copy.play(5);
		assertEquals("4433", MoveSequence.render(board));
		assertEquals("44336", MoveSequence.render(copy), "column index 5 is written 6");
		assertEquals(4, board.moves());
	}

	@Test
	void historyRecordsTheColumnsPlayed() {
		Board board = MoveSequence.parse("4433");
		assertArrayEquals(new int[] {3, 3, 2, 2}, board.history());
	}

	@Test
	void keysAreUniqueAcrossDistinctPositions() {
		// Walk the tree to a modest depth and check no two different boards share a key.
		Set<Long> keys = new HashSet<>();
		Set<String> grids = new HashSet<>();
		walk(Board.startpos(), 6, keys, grids);
		assertEquals(grids.size(), keys.size(), "key() collided between two different positions");
		assertTrue(keys.size() > 1000, "expected a decent sample, got " + keys.size());
	}

	private static void walk(Board board, int depth, Set<Long> keys, Set<String> grids) {
		keys.add(board.key());
		grids.add(grid(board));
		if (depth == 0) return;
		for (int col : board.legalMoves()) {
			board.play(col);
			walk(board, depth - 1, keys, grids);
			board.unplay();
		}
	}

	/** A position written out cell by cell - an encoding independent of the bitboards. */
	private static String grid(Board board) {
		StringBuilder out = new StringBuilder();
		for (int col = 0; col < board.geometry().width(); col++) {
			for (int row = 0; row < board.geometry().height(); row++) {
				out.append(board.discAt(col, row) + 1);
			}
		}
		return out.toString();
	}

	@Test
	void isWinningMoveAgreesWithActuallyPlayingIt() {
		Random random = new Random(99L);
		int found = 0;
		for (int game = 0; game < 500; game++) {
			Board board = Board.startpos();
			while (board.status() == GameStatus.ONGOING) {
				for (int col : board.legalMoves()) {
					boolean predicted = board.isWinningMove(col);
					int mover = board.sideToMove();
					board.play(col);
					GameStatus status = board.status();
					boolean actual = status == (mover == Board.YELLOW
							? GameStatus.WIN_YELLOW : GameStatus.WIN_RED);
					board.unplay();
					assertEquals(predicted, actual,
							"isWinningMove(" + col + ") after " + MoveSequence.render(board));
					if (predicted) found++;
				}
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
		assertTrue(found > 100, "expected to encounter winning moves, saw " + found);
	}
}
