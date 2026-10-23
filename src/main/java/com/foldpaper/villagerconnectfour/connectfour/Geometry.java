package com.foldpaper.villagerconnectfour.connectfour;

/**
 * The shape of a board, and the bit patterns that follow from it. Pure Java - no Minecraft.
 *
 * <p>A position is two {@code long}s laid out one column per {@code height + 1} bits: column
 * {@code c} owns bits {@code c*h1 .. c*h1 + height-1} - row 0 at the bottom - and the top bit
 * of each column is a <em>sentinel</em> that is always clear. The standard 7x6 board is
 * 7 columns of 7 bits, 49 in all, comfortably inside a long.
 *
 * <p>That empty sentinel row is what earns its keep. A win is found by and-ing a bitboard with
 * shifted copies of itself, and without a gap between columns a shift-by-one would run off the
 * top of one column straight into the bottom of the next, inventing vertical wins that are not
 * there; the same gap stops the diagonal shifts wrapping round an edge. It buys correctness in
 * the hottest function in the engine for the price of one wasted bit per column, so
 * {@link #isWin} needs no edge masking at all.
 *
 * <p>It also makes dropping a disc a single add: adding 1 to the bottom of a column carries up
 * through the discs already there and lands on the lowest empty cell.
 *
 * <p>This is a class rather than a record because almost all of it is derived - the masks are
 * precomputed once per shape and read on every node of the search. The rest of the core follows
 * the Checkers sibling's record-heavy style; this is the one place it does not fit.
 *
 * <p>Sizes other than 7x6 exist for two reasons: the config exposes a few, and - far more
 * importantly - the tests need small boards. A 4x4 or 5x4 game is small enough that a test can
 * brute-force its true value with an independent naive search and check the real solver against
 * it, which is the strongest correctness oracle this project has.
 */
public final class Geometry {
	/** Discs in a row needed to win. It is Connect <em>Four</em>; {@link #isWin} assumes this. */
	public static final int CONNECT = 4;

	/** The standard board: 7 columns, 6 rows. Strongly solved, and a win for whoever moves first. */
	public static final Geometry STANDARD = new Geometry(7, 6);

	private final int width;
	private final int height;
	/** Bits per column - the playable rows plus the sentinel. */
	private final int h1;
	private final int size;

	/** Every playable cell set; the sentinel row is clear. */
	private final long board;
	/** Bit 0 of every column. */
	private final long bottom;

	private final long[] bottomMask;
	private final long[] topMask;
	private final long[] columnMask;
	/** Shift distances for the four directions: vertical, horizontal, and the two diagonals. */
	private final int[] strides;

	public Geometry(int width, int height) {
		if (width < CONNECT || height < CONNECT) {
			throw new IllegalArgumentException("board must be at least " + CONNECT + "x" + CONNECT
					+ ", got " + width + "x" + height);
		}
		if (width * (height + 1) > 64) {
			throw new IllegalArgumentException("board needs " + width * (height + 1)
					+ " bits, which does not fit in a long: " + width + "x" + height);
		}
		this.width = width;
		this.height = height;
		this.h1 = height + 1;
		this.size = width * height;
		this.strides = new int[] {1, h1, h1 - 1, h1 + 1};

		this.bottomMask = new long[width];
		this.topMask = new long[width];
		this.columnMask = new long[width];
		long bottomAll = 0;
		for (int col = 0; col < width; col++) {
			bottomMask[col] = 1L << (col * h1);
			topMask[col] = 1L << (col * h1 + height - 1);
			columnMask[col] = ((1L << height) - 1) << (col * h1);
			bottomAll |= bottomMask[col];
		}
		this.bottom = bottomAll;
		this.board = bottomAll * ((1L << height) - 1);
	}

	/** Parses {@code "7x6"}. Throws {@link IllegalArgumentException} on anything else. */
	public static Geometry parse(String text) {
		String[] parts = text.trim().toLowerCase().split("x");
		if (parts.length != 2) throw new IllegalArgumentException("not a board size: " + text);
		try {
			return new Geometry(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("not a board size: " + text, e);
		}
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	/** Cells on the board, and so the number of moves in a full game. */
	public int size() {
		return size;
	}

	int h1() {
		return h1;
	}

	long board() {
		return board;
	}

	long bottom() {
		return bottom;
	}

	long bottomMask(int col) {
		return bottomMask[col];
	}

	long topMask(int col) {
		return topMask[col];
	}

	/** Every playable cell of one column. Public because the engine orders moves by column. */
	public long columnMask(int col) {
		return columnMask[col];
	}

	int[] strides() {
		return strides;
	}

	/** The column a bit index falls in. */
	public int colOf(int bit) {
		return bit / h1;
	}

	/** The row a bit index falls in, 0 at the bottom. */
	public int rowOf(int bit) {
		return bit % h1;
	}

	int bitOf(int col, int row) {
		return col * h1 + row;
	}

	/**
	 * True if {@code discs} contains four in a row. No edge masking is needed: any run that
	 * would wrap between columns has to cross a sentinel bit, and those are never set.
	 */
	public boolean isWin(long discs) {
		for (int stride : strides) {
			long pairs = discs & (discs >>> stride);
			if ((pairs & (pairs >>> (2 * stride))) != 0) return true;
		}
		return false;
	}

	/**
	 * The four cells of a completed line, as bit indices, or null if there is no line. Returns
	 * the lowest such line when several exist - it is only used to highlight the win on screen.
	 */
	public int[] winningLine(long discs) {
		for (int stride : strides) {
			long pairs = discs & (discs >>> stride);
			long quads = pairs & (pairs >>> (2 * stride));
			if (quads == 0) continue;
			int base = Long.numberOfTrailingZeros(quads);
			return new int[] {base, base + stride, base + 2 * stride, base + 3 * stride};
		}
		return null;
	}

	/**
	 * Empty cells that would immediately complete a line for {@code discs}. Each direction looks
	 * for a pair and then tests both the cell that extends it and the gap that splits it, so a
	 * broken three like {@code X X . X} is found as well as a solid one.
	 *
	 * <p>Applying the same pattern to the vertical direction can in principle name a gap cell
	 * with discs floating above it, which is why some implementations special-case it. No
	 * reachable Connect Four position looks like that, so the uniform loop is safe here.
	 */
	public long winningSpots(long discs, long occupied) {
		long spots = 0;
		for (int stride : strides) {
			long pair = (discs << stride) & (discs << (2 * stride));
			spots |= pair & (discs << (3 * stride));
			spots |= pair & (discs >>> stride);
			pair = (discs >>> stride) & (discs >>> (2 * stride));
			spots |= pair & (discs << stride);
			spots |= pair & (discs >>> (3 * stride));
		}
		return spots & (board ^ occupied);
	}

	/** Reflects a bitboard left to right. Connect Four is symmetric, which the tests lean on. */
	public long mirror(long discs) {
		long out = 0;
		long single = (1L << h1) - 1;
		for (int col = 0; col < width; col++) {
			long column = (discs >>> (col * h1)) & single;
			out |= column << ((width - 1 - col) * h1);
		}
		return out;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Geometry g && g.width == width && g.height == height;
	}

	@Override
	public int hashCode() {
		return width * 31 + height;
	}

	@Override
	public String toString() {
		return width + "x" + height;
	}
}
