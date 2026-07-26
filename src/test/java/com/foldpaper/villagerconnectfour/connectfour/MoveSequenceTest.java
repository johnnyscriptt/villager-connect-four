package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveSequenceTest {

	@Test
	void theEmptyStringIsTheStartPosition() {
		Board board = MoveSequence.parse(MoveSequence.START);
		assertEquals(0, board.moves());
		assertEquals(GameStatus.ONGOING, board.status());
		assertEquals("", MoveSequence.render(board));
	}

	@Test
	void columnsAreWrittenOneBased() {
		Board board = MoveSequence.parse("1");
		assertEquals(Board.YELLOW, board.discAt(0, 0));
		assertEquals("1", MoveSequence.render(board));
		assertEquals("4", MoveSequence.column(3));
	}

	@Test
	void everyPositionRoundTrips() {
		Random random = new Random(31337L);
		for (int game = 0; game < 500; game++) {
			Board board = Board.startpos();
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);

				String rendered = MoveSequence.render(board);
				Board reparsed = MoveSequence.parse(rendered);
				assertEquals(rendered, MoveSequence.render(reparsed));
				assertEquals(board.key(), reparsed.key(), rendered);
				assertEquals(board.moves(), reparsed.moves());
				assertEquals(board.status(), reparsed.status());
				assertEquals(board.sideToMove(), reparsed.sideToMove());
			}
		}
	}

	@Test
	void aNonDigitIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("44x4"));
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("4 4"));
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("40"));
	}

	@Test
	void aColumnOffTheBoardIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("8"));
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("6", new Geometry(5, 4)));
	}

	@Test
	void anOverfullColumnIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("1111111"));
	}

	@Test
	void aMoveAfterTheGameEndedIsRejected() {
		// "1122334" is already a win for yellow; anything after it is not a position.
		assertThrows(IllegalArgumentException.class, () -> MoveSequence.parse("11223345"));
	}

	@Test
	void isValidMatchesParse() {
		assertTrue(MoveSequence.isValid("4453", Geometry.STANDARD));
		assertTrue(MoveSequence.isValid("", Geometry.STANDARD));
		assertFalse(MoveSequence.isValid("8", Geometry.STANDARD));
		assertFalse(MoveSequence.isValid("1111111", Geometry.STANDARD));
		assertFalse(MoveSequence.isValid("11223345", Geometry.STANDARD));
	}

	@Test
	void nullIsTheStartPosition() {
		assertEquals(0, MoveSequence.parse(null).moves());
	}

	@Test
	void smallBoardsUseTheSameNotation() {
		Geometry small = new Geometry(5, 4);
		Board board = MoveSequence.parse("123", small);
		assertEquals(3, board.moves());
		assertEquals("123", MoveSequence.render(board));
	}
}
