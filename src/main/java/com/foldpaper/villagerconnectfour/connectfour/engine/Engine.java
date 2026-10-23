package com.foldpaper.villagerconnectfour.connectfour.engine;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.GameStatus;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

/**
 * Negamax alpha-beta with iterative deepening, transposition, and per-level weakening. Never
 * mutates the caller's board, and is deterministic for a given seed and position.
 *
 * <p>Scores are from the point of view of the side to move. A forced win is worth
 * {@code MATE - discsWhenItLands}, so a win that arrives sooner scores higher and the search
 * prefers it; a forced loss is the negative of the same. Zero is a draw or an even position.
 *
 * <p>Note what that distance is measured from. Engines normally count plies from the root of the
 * search, which makes a mate score meaningless outside the search that produced it and is the
 * classic reason mate scores have to be adjusted on their way in and out of a transposition
 * table. Here the distance is counted in <b>discs on the board</b> instead - an absolute measure,
 * because every move adds exactly one disc and none is ever removed. A position therefore always
 * scores the same number whatever search reaches it and from wherever, so entries stay valid
 * across moves and across whole games, and the table can be shared without any adjustment at all.
 */
public final class Engine {
	/** A forced win, less the number of plies it takes to get there. */
	public static final int MATE = 10000;
	private static final int INF = 10500;
	/** How far below best a move can score and still be picked when the villager blunders. */
	private static final int BLUNDER_WINDOW = 60;

	/** The profile {@link #solveExact} uses: everything on, nothing random, no clock to speak of. */
	private static final DifficultyProfile SOLVER = new DifficultyProfile(
			Geometry.STANDARD.size(), 600_000L, DifficultyProfile.ThreatVision.NON_LOSING,
			true, true, 20, false, 0.0, 0);

	private Engine() {}

	/**
	 * Picks a column for the side to move, or -1 if the game is over.
	 *
	 * @param shared a table to reuse between moves; only consulted when the profile solves
	 *     exactly, because a proven terminal score is the only kind that stays true regardless
	 *     of which villager worked it out
	 */
	public static int findMove(Board board, DifficultyProfile profile, Random random,
			BooleanSupplier stop, TranspositionTable shared) {
		int[] legal = board.legalMoves();
		if (legal.length == 0) return -1;
		if (legal.length == 1) return legal[0];

		if (profile.openingBook() && board.moves() == 0
				&& board.geometry().equals(Geometry.STANDARD)) {
			// The centre column is the known winning first move on 7x6, and opening the search
			// from an empty board is by far the most expensive thing the Master ever does.
			return Geometry.STANDARD.width() / 2;
		}

		if (!profile.searches()) return shallowMove(board, profile, random, legal);

		TranspositionTable table = profile.exactSolve() ? shared
				: profile.transpositionBits() > 0 ? new TranspositionTable(profile.transpositionBits())
				: null;

		Board work = board.copy();
		Search search = new Search(work, profile, table,
				System.nanoTime() + profile.timeBudgetMs() * 1_000_000L, stop);

		int[] scores = new int[legal.length];
		int[] iteration = new int[legal.length];
		boolean completedAny = false;
		int remaining = board.geometry().size() - board.moves();
		int ceiling = profile.exactSolve() ? remaining : Math.min(profile.maxDepth(), remaining);

		for (int depth = 1; depth <= ceiling; depth++) {
			if (!search.searchRoot(legal, iteration, depth)) break;
			System.arraycopy(iteration, 0, scores, 0, scores.length);
			completedAny = true;
			if (isProven(scores)) break;   // more depth cannot improve on a forced result
		}
		if (!completedAny) return legal[random.nextInt(legal.length)];
		return pick(legal, scores, profile, random);
	}

	/**
	 * The proven value of a position, from the side to move's point of view. Used by the tests
	 * as the thing to check a naive brute force against; not used in play.
	 */
	public static int solveExact(Board board, TranspositionTable table, BooleanSupplier stop) {
		Board work = board.copy();
		Search search = new Search(work, SOLVER, table,
				System.nanoTime() + 600_000L * 1_000_000L, stop);
		return search.negamax(work.geometry().size(), -INF, INF);
	}

	/**
	 * True once the best move is a forced win, or the best of them is still a forced loss -
	 * either way more depth cannot change the choice. Ordinary evaluations never come close to
	 * this magnitude, and a mate is at most {@code size} plies away, so the margin is generous.
	 */
	private static boolean isProven(int[] scores) {
		int best = Integer.MIN_VALUE;
		for (int score : scores) best = Math.max(best, score);
		return Math.abs(best) > MATE - 100;
	}

	// -------------------------------------------------- rungs that do not search

	/**
	 * Novice and Apprentice. Neither looks past the move in front of it; the difference is that
	 * the Apprentice looks at that one, and that {@code blunderChance} sometimes stops it.
	 */
	private static int shallowMove(Board board, DifficultyProfile profile, Random random, int[] legal) {
		boolean paysAttention = profile.threatVision() != DifficultyProfile.ThreatVision.NONE
				&& random.nextDouble() >= profile.blunderChance();
		if (paysAttention) {
			for (int col : legal) {
				if (board.isWinningMove(col)) return col;
			}
			long urgent = board.opponentWinningSpots() & board.possibleMoves();
			if (urgent != 0) return board.columnOf(urgent);
		}
		return centreWeightedRandom(board.geometry(), legal, random);
	}

	/** Middle columns are worth more in Connect Four, and even a Novice drifts toward them. */
	private static int centreWeightedRandom(Geometry geometry, int[] legal, Random random) {
		int middle = geometry.width() / 2;
		int[] weights = new int[legal.length];
		int total = 0;
		for (int i = 0; i < legal.length; i++) {
			weights[i] = Math.max(1, middle + 1 - Math.abs(legal[i] - middle));
			total += weights[i];
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < legal.length; i++) {
			roll -= weights[i];
			if (roll < 0) return legal[i];
		}
		return legal[legal.length - 1];
	}

	// ------------------------------------------------------- root move choice

	private static int pick(int[] legal, int[] scores, DifficultyProfile profile, Random random) {
		int best = scores[0];
		for (int score : scores) best = Math.max(best, score);

		if (profile.blunderChance() > 0 && random.nextDouble() < profile.blunderChance()) {
			List<Integer> pool = new ArrayList<>();
			for (int i = 0; i < legal.length; i++) {
				if (scores[i] >= best - BLUNDER_WINDOW) pool.add(legal[i]);
			}
			return pool.get(random.nextInt(pool.size()));
		}

		int noise = profile.evalNoise();
		int bestColumn = legal[0];
		int bestNoisy = Integer.MIN_VALUE;
		for (int i = 0; i < legal.length; i++) {
			int noisy = scores[i] + (noise > 0 ? random.nextInt(2 * noise + 1) - noise : 0);
			if (noisy > bestNoisy) {
				bestNoisy = noisy;
				bestColumn = legal[i];
			}
		}
		return bestColumn;
	}

	// ------------------------------------------------------------- the search

	private static final class Search {
		private final Board board;
		private final Geometry geometry;
		private final DifficultyProfile profile;
		private final TranspositionTable table;
		private final long deadlineNanos;
		private final BooleanSupplier stop;
		private int nodes;
		private boolean aborted;

		Search(Board board, DifficultyProfile profile, TranspositionTable table,
				long deadlineNanos, BooleanSupplier stop) {
			this.board = board;
			this.geometry = board.geometry();
			this.profile = profile;
			this.table = table;
			this.deadlineNanos = deadlineNanos;
			this.stop = stop;
		}

		/** Scores every root move at {@code depth}; false if the iteration was cut short. */
		boolean searchRoot(int[] legal, int[] scores, int depth) {
			for (int i = 0; i < legal.length; i++) {
				board.play(legal[i]);
				int score = -negamax(depth - 1, -INF, INF);
				board.unplay();
				if (aborted) return false;
				scores[i] = score;
			}
			return true;
		}

		private int negamax(int depth, int alpha, int beta) {
			if (outOfTime()) return 0;

			// How far along the game is, counted absolutely rather than from the search root -
			// see the class javadoc. This is what makes a stored score portable.
			int discs = board.moves();

			GameStatus status = board.status();
			if (status.isOver()) {
				// Only the side that just moved can have completed a line, so if the game is
				// over and it is not a draw, the side to move here is the one that lost.
				return status == GameStatus.DRAW_FULL ? 0 : -(MATE - discs);
			}

			// Taking a win now beats anything the rest of the search could find.
			if ((board.winningSpots() & board.possibleMoves()) != 0) return MATE - (discs + 1);

			long allowed = profile.threatVision() == DifficultyProfile.ThreatVision.NON_LOSING
					? board.nonLosingMoves()
					: board.possibleMoves();
			// Nothing survives. Either the opponent has two immediate threats, or the one move
			// that answers their single threat sits under another of their winning cells, or
			// every playable cell does. In all three the opponent wins on their very next move,
			// so every move loses at the same distance and naming it here is exact, not a guess.
			if (allowed == 0) return -(MATE - (discs + 2));

			if (depth <= 0) return staticEval();

			int alphaOriginal = alpha;
			long key = board.key();
			if (table != null) {
				int hit = table.probe(key, depth, alpha, beta);
				if (hit != TranspositionTable.MISS) return hit;
			}

			int best = -INF;
			for (int col : order(allowed)) {
				board.play(col);
				int score = -negamax(depth - 1, -beta, -alpha);
				board.unplay();
				if (aborted) return 0;
				if (score > best) best = score;
				if (best > alpha) alpha = best;
				if (alpha >= beta) break;
			}

			if (table != null && !aborted) {
				byte flag = best <= alphaOriginal ? TranspositionTable.UPPER
						: best >= beta ? TranspositionTable.LOWER
						: TranspositionTable.EXACT;
				table.store(key, depth, best, flag);
			}
			return best;
		}

		private int staticEval() {
			nodes++;
			return Evaluator.evaluate(board, profile) * board.sideToMove();
		}

		/**
		 * Columns worth trying first: the ones that leave the most cells completing a line, then
		 * the ones nearest the middle. Cheap ordering, and alpha-beta lives or dies on it.
		 */
		private int[] order(long allowed) {
			int count = 0;
			for (int col = 0; col < geometry.width(); col++) {
				if ((allowed & geometry.columnMask(col)) != 0) count++;
			}
			int[] columns = new int[count];
			int[] ranks = new int[count];
			int n = 0;
			for (int col = 0; col < geometry.width(); col++) {
				long bit = allowed & geometry.columnMask(col);
				if (bit == 0) continue;
				columns[n] = col;
				ranks[n] = rank(col, bit);
				n++;
			}
			for (int i = 1; i < count; i++) {
				int rank = ranks[i], column = columns[i], j = i - 1;
				while (j >= 0 && ranks[j] < rank) {
					ranks[j + 1] = ranks[j];
					columns[j + 1] = columns[j];
					j--;
				}
				ranks[j + 1] = rank;
				columns[j + 1] = column;
			}
			return columns;
		}

		private int rank(int col, long bit) {
			long after = board.position() | bit;
			int threats = Long.bitCount(geometry.winningSpots(after, board.mask() | bit));
			int middle = geometry.width() / 2;
			return threats * 8 + (middle + 1 - Math.abs(col - middle));
		}

		private boolean outOfTime() {
			// Check the clock every so often rather than on every node.
			if ((++nodes & 1023) != 0) return aborted;
			if (System.nanoTime() > deadlineNanos || stop.getAsBoolean()) aborted = true;
			return aborted;
		}
	}
}
