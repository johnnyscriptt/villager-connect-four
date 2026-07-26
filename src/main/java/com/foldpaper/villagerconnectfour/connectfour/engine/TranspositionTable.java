package com.foldpaper.villagerconnectfour.connectfour.engine;

import java.util.Arrays;

/**
 * A fixed-size table of positions the search has already scored, so a position reached by two
 * different move orders is only worked out once. Connect Four transposes heavily — the same
 * discs arrived at in a different sequence is the normal case, not the exception — so this is
 * worth much more here than the equivalent would be in the Checkers sibling.
 *
 * <p>Open addressing with a single probe and always-replace: a collision overwrites rather than
 * chaining. Losing an entry costs a little work, never correctness, and keeping it to one array
 * read is what makes it worth having in the inner loop.
 *
 * <p><b>Not thread-safe.</b> The session manager runs every search on one single-threaded
 * executor, which is what makes it safe to share one table across games. If that executor ever
 * gains a second thread this must become per-thread or be synchronised.
 */
public final class TranspositionTable {
	/** The stored score is the true value of the position. */
	public static final byte EXACT = 0;
	/** The true value is at least the stored score. */
	public static final byte LOWER = 1;
	/** The true value is at most the stored score. */
	public static final byte UPPER = 2;

	/** Returned by {@link #probe} when nothing usable is stored. */
	public static final int MISS = Integer.MIN_VALUE;

	/**
	 * Marks a slot as never written. A real key is two bitboards added together and so is never
	 * negative — and the empty board's key is 0, which would otherwise be indistinguishable from
	 * a freshly allocated slot.
	 */
	private static final long UNUSED = -1L;

	private final int mask;
	private final long[] keys;
	private final short[] scores;
	private final byte[] depths;
	private final byte[] flags;

	public TranspositionTable(int bits) {
		int capacity = 1 << Math.clamp(bits, 8, 24);
		this.mask = capacity - 1;
		this.keys = new long[capacity];
		this.scores = new short[capacity];
		this.depths = new byte[capacity];
		this.flags = new byte[capacity];
		Arrays.fill(keys, UNUSED);
	}

	/**
	 * The stored score for {@code key} if it is deep enough and its bound settles the window,
	 * else {@link #MISS}.
	 */
	public int probe(long key, int depth, int alpha, int beta) {
		int i = index(key);
		if (keys[i] != key || depths[i] < depth) return MISS;
		int score = scores[i];
		return switch (flags[i]) {
			case EXACT -> score;
			case LOWER -> score >= beta ? score : MISS;
			case UPPER -> score <= alpha ? score : MISS;
			default -> MISS;
		};
	}

	public void store(long key, int depth, int score, byte flag) {
		int i = index(key);
		keys[i] = key;
		scores[i] = (short) score;
		depths[i] = (byte) Math.clamp(depth, 0, Byte.MAX_VALUE);
		flags[i] = flag;
	}

	public void clear() {
		Arrays.fill(keys, UNUSED);
		Arrays.fill(depths, (byte) 0);
	}

	/**
	 * Spreads the key before masking. Board keys are two bitboards added together, so their low
	 * bits are far from uniform — indexing on them raw would pile most positions into a fraction
	 * of the table.
	 */
	private int index(long key) {
		long h = key * 0x9E3779B97F4A7C15L;
		return (int) ((h >>> 32) ^ h) & mask;
	}
}
