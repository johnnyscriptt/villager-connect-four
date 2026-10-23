package com.foldpaper.villagerconnectfour.connectfour;

import java.util.Arrays;

/**
 * A Connect Four position with play/unplay. Pure Java - no Minecraft imports.
 *
 * <p>The board is two bitboards over a {@link Geometry}, which owns the layout and the bit
 * tricks. {@link #position} always holds the discs of the side <em>to move</em> rather than a
 * fixed colour, which is why {@link #play} begins by xor-ing it with the mask: the discs that
 * were mine become theirs the moment the turn changes hands.
 *
 * <p>Colour tracks move order and nothing else - {@link #YELLOW} always drops first,
 * {@link #RED} always second. Which of them the human plays is a session setting decided well
 * above this class. Tying colour to move order rather than to a player keeps the parity
 * argument in {@code Evaluator} resting on a fixed invariant instead of a runtime flag.
 *
 * <p>Colours are signed so a negamax search can flip a score with a multiply, matching the
 * Checkers sibling.
 */
public final class Board {
	/** Drops first. */
	public static final int YELLOW = 1;
	/** Drops second. */
	public static final int RED = -1;
	/** Returned by {@link #discAt} for a cell with nothing in it. */
	public static final int EMPTY = 0;

	private final Geometry geometry;

	/** Discs of the side to move. */
	private long position;
	/** Every disc on the board, either colour. */
	private long mask;
	private int moves;

	private final int[] history;
	private final long[] undoPosition;
	private final long[] undoMask;

	public Board() {
		this(Geometry.STANDARD);
	}

	public Board(Geometry geometry) {
		this.geometry = geometry;
		this.history = new int[geometry.size()];
		this.undoPosition = new long[geometry.size()];
		this.undoMask = new long[geometry.size()];
	}

	public static Board startpos() {
		return new Board(Geometry.STANDARD);
	}

	public Geometry geometry() {
		return geometry;
	}

	// ---------------------------------------------------------------- queries

	/** {@link #YELLOW} or {@link #RED} - whose turn it is. */
	public int sideToMove() {
		return (moves & 1) == 0 ? YELLOW : RED;
	}

	/** Discs played so far. */
	public int moves() {
		return moves;
	}

	/** The columns dropped into, in order. */
	public int[] history() {
		return Arrays.copyOf(history, moves);
	}

	/**
	 * True if {@code col} exists and has room. Deliberately says nothing about whether the game
	 * is over - it is asked once per column on every node of the search, so it stays a couple of
	 * instructions. {@link #legalMoves} is the one that knows about winning.
	 */
	public boolean canPlay(int col) {
		return col >= 0 && col < geometry.width() && (mask & geometry.topMask(col)) == 0;
	}

	/** The columns that can still be played, left to right. Empty once the game is over. */
	public int[] legalMoves() {
		if (status() != GameStatus.ONGOING) return new int[0];
		int[] out = new int[geometry.width()];
		int n = 0;
		for (int col = 0; col < geometry.width(); col++) {
			if (canPlay(col)) out[n++] = col;
		}
		return Arrays.copyOf(out, n);
	}

	/** The row a disc dropped into {@code col} would land on, or -1 if it cannot be played. */
	public int landingRow(int col) {
		if (!canPlay(col)) return -1;
		for (int row = 0; row < geometry.height(); row++) {
			if ((mask & (1L << geometry.bitOf(col, row))) == 0) return row;
		}
		return -1;
	}

	/** {@link #YELLOW}, {@link #RED} or {@link #EMPTY}. Row 0 is the bottom of the board. */
	public int discAt(int col, int row) {
		long bit = 1L << geometry.bitOf(col, row);
		if ((mask & bit) == 0) return EMPTY;
		// position holds the side to move, so a disc found in it belongs to that side.
		return (position & bit) != 0 ? sideToMove() : -sideToMove();
	}

	/** Whether dropping into {@code col} would win for the side to move. */
	public boolean isWinningMove(int col) {
		return canPlay(col)
				&& geometry.isWin(position | ((mask + geometry.bottomMask(col)) & geometry.columnMask(col)));
	}

	public GameStatus status() {
		// position is the side to move, so the side that just moved is its complement.
		if (geometry.isWin(position ^ mask)) {
			return sideToMove() == YELLOW ? GameStatus.WIN_RED : GameStatus.WIN_YELLOW;
		}
		return moves == geometry.size() ? GameStatus.DRAW_FULL : GameStatus.ONGOING;
	}

	/**
	 * The cells of the completed line as {@code {col, row}} pairs, or null if nobody has won.
	 * Used only to highlight the win on screen.
	 */
	public int[][] winningLine() {
		int[] bits = geometry.winningLine(position ^ mask);
		if (bits == null) return null;
		int[][] cells = new int[bits.length][];
		for (int i = 0; i < bits.length; i++) {
			cells[i] = new int[] {geometry.colOf(bits[i]), geometry.rowOf(bits[i])};
		}
		return cells;
	}

	// ------------------------------------------------------------ move making

	/** Drops a disc for the side to move. Refuses once the game has been won or drawn. */
	public void play(int col) {
		if (!canPlay(col)) throw new IllegalArgumentException("column " + col + " is not playable");
		if (status() != GameStatus.ONGOING) throw new IllegalStateException("the game is already over");
		undoPosition[moves] = position;
		undoMask[moves] = mask;
		history[moves] = col;
		position ^= mask;                             // the turn changes hands
		mask |= mask + geometry.bottomMask(col);      // the carry lands the disc on the lowest free cell
		moves++;
	}

	/** Takes back the last drop. */
	public void unplay() {
		if (moves == 0) throw new IllegalStateException("no move to take back");
		moves--;
		position = undoPosition[moves];
		mask = undoMask[moves];
	}

	public Board copy() {
		Board copy = new Board(geometry);
		copy.position = position;
		copy.mask = mask;
		copy.moves = moves;
		System.arraycopy(history, 0, copy.history, 0, moves);
		System.arraycopy(undoPosition, 0, copy.undoPosition, 0, moves);
		System.arraycopy(undoMask, 0, copy.undoMask, 0, moves);
		return copy;
	}

	/**
	 * A value identifying this position uniquely - the transposition table key. Adding the two
	 * bitboards works because {@code mask} marks every occupied cell and {@code position} marks
	 * the subset belonging to one side, so the sum encodes both without collision.
	 */
	public long key() {
		return position + mask;
	}

	// ------------------------------------------------------- engine bit tricks

	/** The side to move's discs. */
	public long position() {
		return position;
	}

	/** The opponent's discs. */
	public long opponent() {
		return position ^ mask;
	}

	public long mask() {
		return mask;
	}

	/** Every cell a disc could land on right now, as a bitboard. */
	public long possibleMoves() {
		return (mask + geometry.bottom()) & geometry.board();
	}

	/** Empty cells that would immediately complete a line for the side to move. */
	public long winningSpots() {
		return geometry.winningSpots(position, mask);
	}

	/** Empty cells that would immediately complete a line for the opponent. */
	public long opponentWinningSpots() {
		return geometry.winningSpots(position ^ mask, mask);
	}

	/**
	 * Playable cells that do not lose on the spot: they answer the opponent's immediate threat
	 * and do not hand them a win in the cell directly above. Returns 0 when the opponent has two
	 * separate immediate threats, because only one of them can be blocked - the position is
	 * already lost, and saying so is what lets the search stop early.
	 */
	public long nonLosingMoves() {
		long possible = possibleMoves();
		long opponentWins = opponentWinningSpots();
		long forced = possible & opponentWins;
		if (forced != 0) {
			if ((forced & (forced - 1)) != 0) return 0;   // two threats at once cannot both be met
			possible = forced;
		}
		// Playing directly beneath a cell that wins for them just lifts them onto it.
		return possible & ~(opponentWins >>> 1);
	}

	/** The column a bitboard move bit falls in. */
	public int columnOf(long moveBit) {
		return geometry.colOf(Long.numberOfTrailingZeros(moveBit));
	}
}
