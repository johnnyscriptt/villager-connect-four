package com.foldpaper.villagerconnectfour.client.gui;

/**
 * A disc falling into place. Purely cosmetic, and deliberately so: the logical board is updated
 * the instant a move is known, and input is gated on whose turn the <em>server</em> says it is,
 * so a disc still in flight can never swallow a click or let the two boards drift apart.
 *
 * <p>Driven off the wall clock rather than the render partial tick, matching how the Checkers
 * sibling animates its thinking dots and its refused-move flash.
 */
final class DropAnimation {
	private static final long FALL_MS = 240;
	/** A brief squash on landing; without it the disc stops dead and reads as a teleport. */
	private static final long SQUASH_MS = 80;

	final int column;
	final int targetRow;
	final boolean yellow;
	private final int fromRow;
	private final long startMs;

	DropAnimation(int column, int targetRow, boolean yellow, int height, long nowMs) {
		this.column = column;
		this.targetRow = targetRow;
		this.yellow = yellow;
		this.fromRow = height;   // the headroom cell above the top of the board
		this.startMs = nowMs;
	}

	boolean finished(long nowMs) {
		return nowMs - startMs >= FALL_MS + SQUASH_MS;
	}

	/** True once the disc has landed and should be drawn by the board itself. */
	boolean landed(long nowMs) {
		return nowMs - startMs >= FALL_MS;
	}

	/**
	 * Where the disc is, as a row coordinate that need not be a whole number. Accelerates rather
	 * than moving evenly — a disc that falls at a constant speed does not look like it is falling.
	 */
	float rowAt(long nowMs) {
		long elapsed = nowMs - startMs;
		if (elapsed >= FALL_MS) return targetRow;
		float progress = elapsed / (float) FALL_MS;
		return fromRow + (targetRow - fromRow) * progress * progress;
	}

	/** 0 at rest, 1 at the deepest part of the squash. */
	float squash(long nowMs) {
		long elapsed = nowMs - startMs - FALL_MS;
		if (elapsed < 0 || elapsed >= SQUASH_MS) return 0f;
		float half = SQUASH_MS / 2f;
		return elapsed < half ? elapsed / half : (SQUASH_MS - elapsed) / half;
	}
}
