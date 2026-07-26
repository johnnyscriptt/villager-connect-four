package com.foldpaper.villagerconnectfour.connectfour;

/**
 * How a position travels: the columns played, in order, as digits — {@code "4453"}. The empty
 * string is the start position. Columns are written 1-based because that is what the published
 * literature and the board's own coordinate labels use; the core is 0-based throughout.
 *
 * <p>This is the analogue of the Checkers sibling's draughts FEN, and it is a move list rather
 * than a picture of the board for three reasons:
 *
 * <ol>
 *   <li><b>It cannot describe an illegal position.</b> A grid encoding can put a disc in mid-air,
 *       give one side four more discs than the other, or show two completed lines at once, and
 *       every one of those has to be checked for. Here the only reachable values are legal
 *       positions, and {@link #parse}'s four rejections — not a digit, column out of range,
 *       column already full, a move after the game ended — are the whole legality specification.</li>
 *   <li><b>It is what the published solver test sets are written in</b>, so reference positions
 *       paste straight into a test. That is the same property that made PDN FEN the right
 *       choice for Checkers.</li>
 *   <li><b>It carries the history</b>, so the move list panel and the drop animation's ordering
 *       come free instead of needing a second field on the wire.</li>
 * </ol>
 *
 * <p>What it deliberately does not carry is which side the human plays. That is a property of
 * the session rather than of the position, so it rides on the open packet — exactly where
 * Checkers puts its capture rule.
 */
public final class MoveSequence {
	/** The empty board. */
	public static final String START = "";

	private MoveSequence() {}

	/** The columns played so far, as 1-based digits. */
	public static String render(Board board) {
		StringBuilder out = new StringBuilder();
		for (int col : board.history()) out.append((char) ('1' + col));
		return out.toString();
	}

	/** Replays {@code moves} onto a standard board. */
	public static Board parse(String moves) {
		return parse(moves, Geometry.STANDARD);
	}

	/**
	 * Replays {@code moves} onto a board of the given shape.
	 *
	 * @throws IllegalArgumentException if any character is not a digit, names a column off the
	 *     board, names a column already full, or comes after the game has been decided
	 */
	public static Board parse(String moves, Geometry geometry) {
		Board board = new Board(geometry);
		if (moves == null) return board;
		for (int i = 0; i < moves.length(); i++) {
			char c = moves.charAt(i);
			if (c < '1' || c > '9') {
				throw new IllegalArgumentException("not a column at index " + i + ": '" + c + "'");
			}
			int col = c - '1';
			if (col >= geometry.width()) {
				throw new IllegalArgumentException("column " + c + " is off a " + geometry + " board");
			}
			if (board.status().isOver()) {
				throw new IllegalArgumentException("move at index " + i + " comes after the game ended");
			}
			if (!board.canPlay(col)) {
				throw new IllegalArgumentException("column " + c + " is already full at index " + i);
			}
			board.play(col);
		}
		return board;
	}

	/** True if {@code moves} replays cleanly onto a board of this shape. */
	public static boolean isValid(String moves, Geometry geometry) {
		try {
			parse(moves, geometry);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/** One column as it is written for a human: 0 becomes {@code "1"}. */
	public static String column(int col) {
		return String.valueOf(col + 1);
	}
}
