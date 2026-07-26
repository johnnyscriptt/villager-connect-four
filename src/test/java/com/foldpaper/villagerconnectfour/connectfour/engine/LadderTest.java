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
 * The difficulty ladder, tested as <em>behaviour</em> rather than as a set of numbers.
 *
 * <p>The design constraint the whole engine is built on is that each rung fails in a different
 * way, not merely less often. A ladder built by turning one depth knob would pass a
 * "stronger beats weaker" test just as well, so that test alone proves nothing interesting. The
 * discriminating tests here each name a specific mistake and assert that one rung makes it and
 * the next does not.
 */
class LadderTest {
	private static final Geometry SMALL = new Geometry(5, 4);

	private static DifficultyProfile level(int n) {
		return DifficultyProfile.forVillagerLevel(n);
	}

	private static int move(Board board, DifficultyProfile profile, long seed) {
		return Engine.findMove(board, profile, new Random(seed), () -> false, new TranspositionTable(16));
	}

	// ------------------------------------------------- rung 1 -> 2: can it see?

	/**
	 * Trials share one random stream on purpose. {@link Random} seeded with consecutive values
	 * returns strongly correlated first draws, so reseeding per trial would sample the same
	 * column repeatedly and turn these into tests that pass without measuring anything.
	 */
	private static final int TRIALS = 400;

	@Test
	void theApprenticeTakesAWinTheNoviceWalksPast() {
		Board position = MoveSequence.parse("263741");
		assertEquals(Board.YELLOW, position.sideToMove());
		assertTrue(position.isWinningMove(4), "column 5 finishes yellow's bottom row");

		int apprenticeTook = sample(position, level(2));
		int noviceTook = sample(position, level(1));

		// The Apprentice looks, and only its blunder chance stops it.
		assertTrue(apprenticeTook > TRIALS * 0.6,
				"the Apprentice should nearly always take it: " + apprenticeTook + "/" + TRIALS);
		assertTrue(noviceTook < TRIALS * 0.4,
				"the Novice should mostly miss it: " + noviceTook + "/" + TRIALS);
		assertTrue(apprenticeTook > noviceTook * 2, apprenticeTook + " vs " + noviceTook);
	}

	@Test
	void theApprenticeBlocksAThreatTheNoviceIgnores() {
		Board position = MoveSequence.parse("2637");
		position.play(3);   // yellow makes an open three along the bottom
		assertEquals(Board.RED, position.sideToMove());

		int apprenticeBlocked = sample(position, level(2));
		int noviceBlocked = sample(position, level(1));

		assertTrue(apprenticeBlocked > TRIALS * 0.6,
				"the Apprentice should block: " + apprenticeBlocked + "/" + TRIALS);
		assertTrue(noviceBlocked < TRIALS * 0.45,
				"the Novice should often not: " + noviceBlocked + "/" + TRIALS);
		assertTrue(apprenticeBlocked > noviceBlocked, apprenticeBlocked + " vs " + noviceBlocked);
	}

	/**
	 * How often the profile picks the column the position calls for — the winning one if it has
	 * a win, otherwise the one that answers the opponent's threat.
	 */
	private static int sample(Board position, DifficultyProfile profile) {
		Random random = new Random(2026L);
		long threats = position.opponentWinningSpots() & position.possibleMoves();
		int hits = 0;
		for (int trial = 0; trial < TRIALS; trial++) {
			Board board = position.copy();
			int col = Engine.findMove(board, profile, random, () -> false, null);
			boolean right = board.isWinningMove(col)
					|| (threats & board.geometry().columnMask(col)) != 0;
			if (right) hits++;
		}
		return hits;
	}

	// --------------------------------- rung 2 -> 3: does it know a block can be a gift?

	/**
	 * The mechanism the Journeyman is built on, checked directly on the core rather than through
	 * the engine. Testing it through play would be weaker and flakier: once a position is lost
	 * whatever it does, every move scores a near-identical forced loss, and the rung's blunder
	 * chance will happily pick the one that loses soonest — correctly, since they all lose.
	 */
	@Test
	void aNonLosingMoveNeverHandsTheOpponentAnImmediateWin() {
		Random random = new Random(17L);
		int checked = 0, escapes = 0;
		for (int game = 0; game < 400; game++) {
			Board board = new Board(SMALL);
			while (board.status() == GameStatus.ONGOING) {
				long safe = board.nonLosingMoves();
				for (int col : board.legalMoves()) {
					if ((safe & board.geometry().columnMask(col)) == 0) continue;
					board.play(col);
					if (board.status() == GameStatus.ONGOING) {
						assertTrue((board.winningSpots() & board.possibleMoves()) == 0,
								"column " + col + " was called safe but hangs a win at "
										+ MoveSequence.render(board));
					}
					board.unplay();
					checked++;
				}
				if (safe != 0) escapes++;
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
		assertTrue(checked > 2000, "expected a decent sample, got " + checked);
		assertTrue(escapes > 100, "the filter should usually leave something playable: " + escapes);
	}

	@Test
	void theJourneymanHangsFarFewerWinsThanTheApprentice() {
		assertTrue(hangRate(level(3)) < hangRate(level(2)),
				"seeing non-losing moves should show up as fewer hung wins");
	}

	/** How often a profile leaves the opponent an immediate win when it did not have to. */
	private static int hangRate(DifficultyProfile profile) {
		Random random = new Random(71L);
		int hung = 0;
		for (int game = 0; game < 60; game++) {
			Board board = new Board(SMALL);
			while (board.status() == GameStatus.ONGOING) {
				boolean escapable = board.nonLosingMoves() != 0;
				board.play(Engine.findMove(board, profile, random, () -> false, null));
				if (escapable && board.status() == GameStatus.ONGOING
						&& (board.winningSpots() & board.possibleMoves()) != 0) {
					hung++;
				}
				if (board.status() != GameStatus.ONGOING) break;
				int[] legal = board.legalMoves();
				board.play(legal[random.nextInt(legal.length)]);
			}
		}
		return hung;
	}

	// ------------------------------ rung 3 -> 4: does it understand parity?

	@Test
	void theExpertWeighsThreatsByParityAndTheJourneymanDoesNot() {
		assertTrue(level(4).parityAware(), "the Expert is the rung where parity switches on");
		assertTrue(!level(3).parityAware(), "the Journeyman is deliberately blind to it");
		assertTrue(level(5).parityAware());
	}

	// ---------------------------------- rung 4 -> 5: does it prove anything?

	@Test
	void onlyTheMasterSolves() {
		for (int level = 1; level <= 4; level++) {
			assertTrue(!level(level).exactSolve(), "level " + level + " should estimate, not prove");
		}
		assertTrue(level(5).exactSolve());
	}

	@Test
	void everyRungSwitchesOnSomethingTheOneBelowLacks() {
		// The ladder's whole premise, asserted as a property of the table rather than of play:
		// no rung is merely a deeper version of its predecessor.
		for (int level = 2; level <= 5; level++) {
			DifficultyProfile lower = level(level - 1), upper = level(level);
			boolean newMechanism = upper.threatVision() != lower.threatVision()
					|| upper.parityAware() != lower.parityAware()
					|| upper.exactSolve() != lower.exactSolve();
			assertTrue(newMechanism, "level " + level + " only turns a number up");
		}
	}

	// ------------------------------------------------------------- self-play

	@Test
	void higherRungsBeatLowerOnesOverASeries() {
		// Small board so the strong rungs stay quick; the mechanisms are geometry-independent.
		for (int level = 2; level <= 5; level++) {
			int stronger = 0, weaker = 0;
			for (int game = 0; game < 12; game++) {
				// Alternate who starts: on a solved game, moving first is worth a great deal.
				int result = play(level(level), level(level - 1), game % 2 == 0, game);
				if (result > 0) stronger++;
				else if (result < 0) weaker++;
			}
			assertTrue(stronger >= weaker,
					"level " + level + " lost the series to level " + (level - 1)
							+ " (" + stronger + "-" + weaker + ")");
		}
	}

	/** Plays one game; returns +1 if {@code a} won, -1 if {@code b} won, 0 for a draw. */
	private static int play(DifficultyProfile a, DifficultyProfile b, boolean aStarts, long seed) {
		Board board = new Board(SMALL);
		TranspositionTable shared = new TranspositionTable(16);
		Random random = new Random(seed);
		while (board.status() == GameStatus.ONGOING) {
			boolean aToMove = (board.sideToMove() == Board.YELLOW) == aStarts;
			DifficultyProfile profile = aToMove ? a : b;
			int col = Engine.findMove(board, profile, random, () -> false, shared);
			board.play(col);
		}
		GameStatus status = board.status();
		if (status == GameStatus.DRAW_FULL) return 0;
		boolean yellowWon = status == GameStatus.WIN_YELLOW;
		boolean aWon = yellowWon == aStarts;
		return aWon ? 1 : -1;
	}
}
