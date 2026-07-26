package com.foldpaper.villagerconnectfour.connectfour.engine;

/**
 * Engine strength knobs for one villager trading level.
 *
 * <p>Connect Four is strongly solved: on the standard 7x6 board the player who drops first wins
 * with perfect play, opening in the centre. The villager drops first by default, so the top of
 * this ladder is a genuine wall rather than merely a strong opponent — that is the point of it,
 * and the config's {@code first-player} key is the sanctioned way past it.
 *
 * <p>Where the Checkers sibling's ladder is built on a <em>capture horizon</em>, this one is
 * built on <b>threat vision</b>: what the villager can work out about the cells that complete a
 * line. Each rung switches on a different mechanism rather than just raising a number:
 *
 * <ol>
 *   <li><b>Novice</b> — no vision at all. Prefers the middle and otherwise drops at random. Will
 *       not take a win sitting under its nose, and will not block yours.</li>
 *   <li><b>Apprentice</b> — one-ply tactics: takes a win if it has one, otherwise blocks yours.
 *       Blind to the fact that a block can be a gift, so it will fill the cell beneath your
 *       winning square and lift you onto it. Blind, too, to a double threat.</li>
 *   <li><b>Journeyman</b> — refuses to play any move that loses on the spot, and searches. Makes
 *       and recognises double threats. Blind to parity: it builds threats on rows that will
 *       be filled by the wrong player anyway.</li>
 *   <li><b>Expert</b> — parity-aware evaluation. Understands that a threat is only worth
 *       something if the race to fill the column reaches it on the right turn, so it steers
 *       toward its own parity and refuses to hand you yours. Still estimates past its horizon.</li>
 *   <li><b>Master</b> — solves. Searches to a terminal score with a transposition table that
 *       survives between moves, so its knowledge compounds over a game.</li>
 * </ol>
 *
 * @param maxDepth           search ceiling in plies; 0 means no tree search at all
 * @param timeBudgetMs       soft time limit for the whole search
 * @param threatVision       what the villager can see about cells that complete a line
 * @param parityAware        weight threats by whether their row falls to their owner in the race
 * @param exactSolve         search to a terminal, proven score rather than an estimate
 * @param transpositionBits  size of the transposition table as a power of two; 0 for none
 * @param openingBook        answer the empty standard board instantly with the known winning move
 * @param blunderChance      probability of ignoring what it worked out and picking loosely
 * @param evalNoise          random noise added to root scores before choosing
 */
public record DifficultyProfile(int maxDepth, long timeBudgetMs, ThreatVision threatVision,
		boolean parityAware, boolean exactSolve, int transpositionBits, boolean openingBook,
		double blunderChance, int evalNoise) {

	/** How much the villager understands about cells that would complete a line. */
	public enum ThreatVision {
		/** None. Does not look at whether anybody is about to win. */
		NONE,
		/** One ply: its own win now, and the opponent's win now. Nothing about what follows. */
		IMMEDIATE,
		/** Won't play into a loss, and knows a double threat has already decided the game. */
		NON_LOSING
	}

	/** Villager trading levels: 1 Novice, 2 Apprentice, 3 Journeyman, 4 Expert, 5 Master. */
	public static DifficultyProfile forVillagerLevel(int level) {
		return switch (Math.clamp(level, 1, 5)) {
			case 1 -> new DifficultyProfile(0, 150, ThreatVision.NONE, false, false, 0, false, 0.40, 200);
			case 2 -> new DifficultyProfile(0, 250, ThreatVision.IMMEDIATE, false, false, 0, false, 0.25, 120);
			case 3 -> new DifficultyProfile(5, 700, ThreatVision.NON_LOSING, false, false, 16, false, 0.06, 40);
			case 4 -> new DifficultyProfile(9, 1600, ThreatVision.NON_LOSING, true, false, 18, false, 0.01, 10);
			// maxDepth is nominal for the Master: exactSolve searches to terminal, bounded by
			// the clock and the table rather than by a ply count.
			default -> new DifficultyProfile(42, 3000, ThreatVision.NON_LOSING, true, true, 20, true, 0.0, 0);
		};
	}

	/** True when this rung looks any distance ahead at all. */
	public boolean searches() {
		return maxDepth > 0;
	}
}
