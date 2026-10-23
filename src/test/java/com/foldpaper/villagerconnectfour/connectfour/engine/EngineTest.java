package com.foldpaper.villagerconnectfour.connectfour.engine;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.GameStatus;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;
import com.foldpaper.villagerconnectfour.connectfour.MoveSequence;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine behaviour on positions with a known right answer.
 *
 * <p>Anything that plays whole games does so on a small board. The villager presets carry real
 * time budgets - three seconds a move at the top - so a full 7x6 self-play game would take a
 * minute of wall clock on its own. The mechanisms being tested are geometry-independent, so the
 * small board tests exactly the same code far faster; {@link SmallBoardOracleTest} is where the
 * search is actually proved correct.
 */
class EngineTest {
	/** Fixed profiles so the suite stays fast; the villager presets are asserted separately. */
	private static final DifficultyProfile STRONG = new DifficultyProfile(
			8, 500, DifficultyProfile.ThreatVision.NON_LOSING, true, false, 16, false, 0.0, 0);
	private static final DifficultyProfile TACTICAL = new DifficultyProfile(
			0, 50, DifficultyProfile.ThreatVision.IMMEDIATE, false, false, 0, false, 0.0, 0);
	private static final DifficultyProfile BLIND = new DifficultyProfile(
			0, 50, DifficultyProfile.ThreatVision.NONE, false, false, 0, false, 0.0, 0);

	private static int move(Board board, DifficultyProfile profile, long seed) {
		return Engine.findMove(board, profile, new Random(seed), () -> false, new TranspositionTable(16));
	}

	@Test
	void thereIsNoMoveWhenTheGameIsOver() {
		Board won = MoveSequence.parse("1122334");
		assertEquals(GameStatus.WIN_YELLOW, won.status());
		assertEquals(-1, move(won, STRONG, 1));
	}

	@Test
	void aForcedChoiceIsTakenWithoutSearching() {
		// Every column full but the last, and nobody has connected four.
		Board board = MoveSequence.parse("121212343434565656212121434343656565");
		assertEquals(GameStatus.ONGOING, board.status());
		assertEquals(1, board.legalMoves().length);
		assertEquals(6, move(board, BLIND, 1));
	}

	@Test
	void anImmediateWinIsTaken() {
		// Yellow has three along the bottom of columns 1-3 and can finish at either end.
		Board board = MoveSequence.parse("263741");
		assertEquals(Board.YELLOW, board.sideToMove());
		int col = move(board, TACTICAL, 1);
		board.play(col);
		assertEquals(GameStatus.WIN_YELLOW, board.status(), "played column " + col);
	}

	@Test
	void anImmediateThreatIsBlocked() {
		// Yellow threatens the bottom of column 4; red is to move and must answer there.
		Board board = MoveSequence.parse("2637");
		board.play(3);
		assertEquals(Board.RED, board.sideToMove());
		int col = move(board, TACTICAL, 1);
		assertTrue(col == 0 || col == 4, "red had to block an end, played column " + col);
	}

	@Test
	void theBlindRungDoesNeitherReliably() {
		// The Novice's defining trait: given the same winning position, it mostly walks past it.
		// One random stream for the whole sample, deliberately: java.util.Random seeded with
		// consecutive values returns strongly correlated first draws, so reseeding per trial
		// would sample the same column over and over and prove nothing.
		Board board = MoveSequence.parse("263741");
		Random random = new Random(11L);
		int taken = 0;
		for (int trial = 0; trial < 400; trial++) {
			int col = Engine.findMove(board, BLIND, random, () -> false, null);
			board.play(col);
			if (board.status() == GameStatus.WIN_YELLOW) taken++;
			board.unplay();
		}
		// The winning column is one of seven and only mildly favoured by the centre bias.
		assertTrue(taken < 160, "a rung with no threat vision should often miss the win, took " + taken);
	}

	@Test
	void everyPresetReturnsALegalColumnThroughAWholeGame() {
		Geometry small = new Geometry(5, 4);
		Random random = new Random(5L);
		TranspositionTable shared = new TranspositionTable(16);
		for (int level = 1; level <= 5; level++) {
			DifficultyProfile profile = DifficultyProfile.forVillagerLevel(level);
			for (int game = 0; game < 10; game++) {
				Board board = new Board(small);
				while (board.status() == GameStatus.ONGOING) {
					int col = Engine.findMove(board, profile, new Random(game), () -> false, shared);
					assertTrue(board.canPlay(col), "level " + level + " picked column " + col);
					board.play(col);
					if (board.status() != GameStatus.ONGOING) break;
					int[] legal = board.legalMoves();
					board.play(legal[random.nextInt(legal.length)]);
				}
			}
		}
	}

	@Test
	void theMasterOpensInTheCentre() {
		DifficultyProfile master = DifficultyProfile.forVillagerLevel(5);
		assertTrue(master.openingBook());
		assertEquals(3, move(Board.startpos(), master, 1), "the centre is the known winning opening");
	}

	@Test
	void theOpeningBookOnlyAppliesToTheStandardBoard() {
		// On a shape whose value is not the known one, the Master searches rather than assuming.
		Geometry small = new Geometry(5, 4);
		int col = Engine.findMove(new Board(small), DifficultyProfile.forVillagerLevel(5),
				new Random(1), () -> false, new TranspositionTable(16));
		assertTrue(col >= 0 && col < small.width());
	}

	@Test
	void searchIsDeterministicForASeed() {
		Board board = MoveSequence.parse("4433");
		for (int level = 1; level <= 4; level++) {
			DifficultyProfile profile = DifficultyProfile.forVillagerLevel(level);
			assertEquals(move(board, profile, 42), move(board, profile, 42),
					"level " + level + " is not reproducible");
		}
	}

	@Test
	void aStopRequestStillYieldsALegalMove() {
		Board board = MoveSequence.parse("4433");
		int col = Engine.findMove(board, STRONG, new Random(1), () -> true, new TranspositionTable(16));
		assertTrue(board.canPlay(col), "an interrupted search must still answer");
	}

	@Test
	void aTimeBudgetIsRespected() {
		DifficultyProfile brief = new DifficultyProfile(
				42, 120, DifficultyProfile.ThreatVision.NON_LOSING, true, false, 18, false, 0.0, 0);
		Board board = MoveSequence.parse("4433");
		long start = System.nanoTime();
		int col = Engine.findMove(board, brief, new Random(1), () -> false, new TranspositionTable(18));
		long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
		assertTrue(board.canPlay(col));
		assertTrue(elapsedMs < 3000, "a 120ms budget overran badly: " + elapsedMs + "ms");
	}

	@Test
	void onlyTheWeakRungsAreRandomised() {
		assertEquals(0.0, DifficultyProfile.forVillagerLevel(5).blunderChance());
		assertEquals(0, DifficultyProfile.forVillagerLevel(5).evalNoise());
		assertNotEquals(0.0, DifficultyProfile.forVillagerLevel(1).blunderChance());
		assertTrue(DifficultyProfile.forVillagerLevel(1).blunderChance()
				> DifficultyProfile.forVillagerLevel(3).blunderChance());
	}

	@Test
	void levelsClampToTheLadder() {
		assertEquals(DifficultyProfile.forVillagerLevel(1), DifficultyProfile.forVillagerLevel(0));
		assertEquals(DifficultyProfile.forVillagerLevel(5), DifficultyProfile.forVillagerLevel(99));
	}
}
