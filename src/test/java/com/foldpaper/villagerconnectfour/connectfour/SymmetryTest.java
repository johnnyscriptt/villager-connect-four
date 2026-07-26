package com.foldpaper.villagerconnectfour.connectfour;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Connect Four is exactly symmetric left to right, which makes mirroring a free and unusually
 * sharp test: almost any error in the bit layout, the column indexing or the win scan breaks it,
 * and it needs no reference data at all.
 */
class SymmetryTest {

	/** The same game played into the mirrored columns. */
	private static String mirrorSequence(String moves, Geometry geometry) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < moves.length(); i++) {
			int col = moves.charAt(i) - '1';
			out.append((char) ('1' + (geometry.width() - 1 - col)));
		}
		return out.toString();
	}

	@Test
	void aMirroredGameEndsTheSameWay() {
		Random random = new Random(2026L);
		for (int game = 0; game < 800; game++) {
			Board board = Board.startpos();
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);

				String moves = MoveSequence.render(board);
				Board mirrored = MoveSequence.parse(mirrorSequence(moves, board.geometry()));
				assertEquals(board.status(), mirrored.status(), moves);
				assertEquals(board.sideToMove(), mirrored.sideToMove(), moves);
				assertEquals(board.moves(), mirrored.moves(), moves);
			}
		}
	}

	@Test
	void mirroringTheBitboardsMatchesMirroringTheMoves() {
		Random random = new Random(4242L);
		Geometry geometry = Geometry.STANDARD;
		for (int game = 0; game < 500; game++) {
			Board board = Board.startpos();
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);

				String moves = MoveSequence.render(board);
				Board mirrored = MoveSequence.parse(mirrorSequence(moves, geometry));
				assertEquals(geometry.mirror(board.position()), mirrored.position(), moves);
				assertEquals(geometry.mirror(board.mask()), mirrored.mask(), moves);
			}
		}
	}

	@Test
	void mirroringMovesEveryDiscToTheOppositeColumn() {
		Board board = MoveSequence.parse("1223733474744");
		Geometry geometry = board.geometry();
		Board mirrored = MoveSequence.parse(mirrorSequence(MoveSequence.render(board), geometry));
		for (int col = 0; col < geometry.width(); col++) {
			for (int row = 0; row < geometry.height(); row++) {
				assertEquals(board.discAt(col, row), mirrored.discAt(geometry.width() - 1 - col, row),
						"cell " + col + "," + row);
			}
		}
	}

	@Test
	void winningSpotsMirrorToo() {
		Random random = new Random(8080L);
		Geometry geometry = Geometry.STANDARD;
		for (int game = 0; game < 300; game++) {
			Board board = Board.startpos();
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);

				Board mirrored = MoveSequence.parse(
						mirrorSequence(MoveSequence.render(board), geometry));
				assertEquals(geometry.mirror(board.winningSpots()), mirrored.winningSpots());
				assertEquals(geometry.mirror(board.nonLosingMoves()), mirrored.nonLosingMoves());
			}
		}
	}

	@Test
	void symmetryHoldsOnSmallBoardsToo() {
		Geometry small = new Geometry(5, 4);
		Random random = new Random(11L);
		for (int game = 0; game < 500; game++) {
			Board board = new Board(small);
			while (board.status() == GameStatus.ONGOING) {
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);

				String moves = MoveSequence.render(board);
				Board mirrored = MoveSequence.parse(mirrorSequence(moves, small), small);
				assertEquals(board.status(), mirrored.status(), moves);
			}
		}
	}
}
