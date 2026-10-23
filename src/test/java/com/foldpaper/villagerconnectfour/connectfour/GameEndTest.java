package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How games end. Every move sequence here is written 1-based, as {@link MoveSequence} does, and
 * the assertions check the resulting shape rather than trusting the sequence - so a typo shows
 * up as a failure rather than as a test that quietly proves nothing.
 */
class GameEndTest {

	@Test
	void aFreshBoardIsOngoing() {
		assertEquals(GameStatus.ONGOING, Board.startpos().status());
		assertFalse(GameStatus.ONGOING.isOver());
	}

	@Test
	void fourInAColumnWinsForYellow() {
		// Yellow stacks column 4 while red answers alongside.
		Board board = MoveSequence.parse("4546474");
		assertEquals(GameStatus.WIN_YELLOW, board.status());
		assertTrue(board.status().isOver());
		for (int row = 0; row < 4; row++) assertEquals(Board.YELLOW, board.discAt(3, row));
	}

	@Test
	void fourAlongTheBottomWinsForYellow() {
		// Yellow walks the bottom row; red stacks harmlessly on top of it.
		Board board = MoveSequence.parse("1122334");
		assertEquals(GameStatus.WIN_YELLOW, board.status());
		for (int col = 0; col < 4; col++) assertEquals(Board.YELLOW, board.discAt(col, 0));
	}

	@Test
	void redCanWinToo() {
		// Yellow piles into column 1 and then steps aside; red takes the bottom row behind it.
		Board board = MoveSequence.parse("12131475");
		assertEquals(GameStatus.WIN_RED, board.status());
		for (int col = 1; col <= 4; col++) assertEquals(Board.RED, board.discAt(col, 0));
	}

	@Test
	void aDiagonalWins() {
		// Yellow climbs (0,0) (1,1) (2,2) (3,3); the spare discs go into column 7.
		Board board = MoveSequence.parse("1223733474744");
		assertEquals(GameStatus.WIN_YELLOW, board.status());
		for (int i = 0; i < 4; i++) assertEquals(Board.YELLOW, board.discAt(i, i));
	}

	@Test
	void winningLineNamesTheFourCells() {
		Board board = MoveSequence.parse("1122334");
		int[][] line = board.winningLine();
		assertNotNull(line);
		assertEquals(4, line.length);
		for (int[] cell : line) {
			assertEquals(Board.YELLOW, board.discAt(cell[0], cell[1]));
			assertEquals(0, cell[1], "this win runs along the bottom row");
		}
	}

	@Test
	void thereIsNoWinningLineWithoutAWin() {
		assertNull(Board.startpos().winningLine());
		assertNull(MoveSequence.parse("44").winningLine());
	}

	@Test
	void aFullBoardWithNoLineIsADraw() {
		Board board = MoveSequence.parse(FULL_BOARD_DRAW);
		assertEquals(42, board.moves());
		assertEquals(GameStatus.DRAW_FULL, board.status());
		assertArrayEquals(new int[0], board.legalMoves());
		for (int col = 0; col < 7; col++) {
			for (int row = 0; row < 6; row++) {
				assertTrue(board.discAt(col, row) != Board.EMPTY, "cell " + col + "," + row);
			}
		}
	}

	/**
	 * A complete 42-move game that fills the board with nobody connecting four. Each pair of
	 * columns is filled three discs at a time and then revisited in the opposite order, which
	 * leaves every row alternating and every diagonal broken. The test verifies the claim rather
	 * than taking it on trust, so the sequence needs no external provenance.
	 */
	private static final String FULL_BOARD_DRAW =
			"121212" + "343434" + "565656" + "777777" + "212121" + "434343" + "656565";

	@Test
	void noMoveIsLegalOnceTheGameIsWon() {
		Board board = MoveSequence.parse("1122334");
		assertEquals(GameStatus.WIN_YELLOW, board.status());
		assertArrayEquals(new int[0], board.legalMoves());
		assertThrows(IllegalStateException.class, () -> board.play(6));
	}

	@Test
	void takingTheWinningMoveBackReopensTheGame() {
		Board board = MoveSequence.parse("1122334");
		GameStatus won = board.status();
		board.unplay();
		assertEquals(GameStatus.ONGOING, board.status());
		assertTrue(board.legalMoves().length > 0);
		board.play(3);
		assertEquals(won, board.status());
	}
}
