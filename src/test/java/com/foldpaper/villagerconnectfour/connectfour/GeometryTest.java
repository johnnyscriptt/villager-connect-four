package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryTest {

	@Test
	void standardIsSevenBySix() {
		assertEquals(7, Geometry.STANDARD.width());
		assertEquals(6, Geometry.STANDARD.height());
		assertEquals(42, Geometry.STANDARD.size());
		assertEquals("7x6", Geometry.STANDARD.toString());
	}

	@Test
	void theSentinelRowIsNeverPartOfTheBoard() {
		for (Geometry g : new Geometry[] {Geometry.STANDARD, new Geometry(4, 4), new Geometry(8, 7)}) {
			assertEquals(g.size(), Long.bitCount(g.board()), g + " board mask");
			for (int col = 0; col < g.width(); col++) {
				long sentinel = 1L << (g.bitOf(col, g.height()));
				assertEquals(0, g.board() & sentinel, g + " sentinel of column " + col);
			}
		}
	}

	@Test
	void bitIndexRoundTrips() {
		Geometry g = Geometry.STANDARD;
		for (int col = 0; col < g.width(); col++) {
			for (int row = 0; row < g.height(); row++) {
				int bit = g.bitOf(col, row);
				assertEquals(col, g.colOf(bit));
				assertEquals(row, g.rowOf(bit));
			}
		}
	}

	@Test
	void parseReadsABoardSize() {
		assertEquals(Geometry.STANDARD, Geometry.parse("7x6"));
		assertEquals(new Geometry(5, 4), Geometry.parse(" 5X4 "));
	}

	@Test
	void parseRejectsNonsense() {
		assertThrows(IllegalArgumentException.class, () -> Geometry.parse("wide"));
		assertThrows(IllegalArgumentException.class, () -> Geometry.parse("7"));
		assertThrows(IllegalArgumentException.class, () -> Geometry.parse("7x6x5"));
	}

	@Test
	void boardsTooSmallToWinOnAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> new Geometry(3, 6));
		assertThrows(IllegalArgumentException.class, () -> new Geometry(7, 3));
	}

	@Test
	void boardsTooBigForALongAreRejected() {
		// 8x7 needs 8*8 = 64 bits and just fits; 9x7 needs 72 and does not.
		assertEquals(56, new Geometry(8, 7).size());
		assertThrows(IllegalArgumentException.class, () -> new Geometry(9, 7));
	}

	@Test
	void mirrorIsItsOwnInverse() {
		Geometry g = Geometry.STANDARD;
		Board board = MoveSequence.parse("4433221");
		long discs = board.position();
		assertEquals(discs, g.mirror(g.mirror(discs)));
		assertNotEquals(discs, g.mirror(discs), "this position is not symmetric");
	}

	@Test
	void mirrorMovesColumnsToTheOtherSide() {
		Geometry g = Geometry.STANDARD;
		long left = 1L << g.bitOf(0, 0);
		assertEquals(1L << g.bitOf(6, 0), g.mirror(left));
	}

	@Test
	void winningLineNamesFourRealCells() {
		Geometry g = Geometry.STANDARD;
		long discs = 0;
		for (int col = 0; col < 4; col++) discs |= 1L << g.bitOf(col, 0);
		int[] line = g.winningLine(discs);
		assertTrue(line != null);
		assertEquals(4, line.length);
		long rebuilt = 0;
		for (int bit : line) rebuilt |= 1L << bit;
		assertEquals(discs, rebuilt);
	}

	@Test
	void winningLineIsNullWithoutAWin() {
		Geometry g = Geometry.STANDARD;
		assertEquals(null, g.winningLine(1L << g.bitOf(0, 0)));
	}
}
