package com.foldpaper.villagerconnectfour.client.gui;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.Geometry;
import com.foldpaper.villagerconnectfour.connectfour.MoveSequence;
import com.foldpaper.villagerconnectfour.net.ConnectFourOpenS2C;
import com.foldpaper.villagerconnectfour.net.ConnectFourOutcome;
import com.foldpaper.villagerconnectfour.net.ConnectFourStateS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-side mirror of a game. Reuses the same pure-Java core the server does, so the board can
 * be redrawn and a move applied optimistically without waiting for a round trip; the server's
 * state packet remains authoritative and simply replaces it.
 *
 * <p>Much smaller than the Checkers equivalent, which needs a whole click-by-click turn-entry
 * state machine for jump chains. A Connect Four turn is one column, so there is no partial move
 * to hold, nothing to cancel, and no legal-move highlighting beyond "is this column full".
 */
public final class ClientConnectFourGame {
	public final int gameId;
	public final String villagerName;
	public final int villagerLevel;
	public final Geometry geometry;
	/** True when the player was dealt yellow, which is the same as saying they drop first. */
	public final boolean playerIsFirst;
	/** The actual villager entity for the side panel; null if it is not loaded client-side. */
	public final LivingEntity villagerEntity;

	public Board board;
	public ConnectFourOutcome outcome = ConnectFourOutcome.ONGOING;
	public boolean playerToMove;

	public ClientConnectFourGame(ConnectFourOpenS2C payload) {
		this.gameId = payload.gameId();
		this.villagerName = payload.villagerName();
		this.villagerLevel = payload.villagerLevel();
		this.geometry = new Geometry(payload.width(), payload.height());
		this.playerIsFirst = payload.playerIsFirst();
		this.playerToMove = payload.playerToMove();
		this.board = MoveSequence.parse(payload.moves(), geometry);
		this.villagerEntity = Minecraft.getInstance().level != null
				&& Minecraft.getInstance().level.getEntity(payload.villagerEntityId()) instanceof LivingEntity living
				? living : null;
	}

	/** The colour the player is playing. */
	public int playerColour() {
		return playerIsFirst ? Board.YELLOW : Board.RED;
	}

	public boolean isYellow(int colour) {
		return colour == Board.YELLOW;
	}

	/**
	 * Applies authoritative state. Returns the column of a move the client had not already seen,
	 * so the screen can animate it, or -1 when there is nothing new to show - which is the normal
	 * case for the player's own move, already applied optimistically.
	 */
	public int applyServerState(ConnectFourStateS2C payload) {
		int before = board.moves();
		board = MoveSequence.parse(payload.moves(), geometry);
		outcome = payload.outcome();
		playerToMove = payload.playerToMove();
		return board.moves() > before && payload.lastColumn() >= 0 ? payload.lastColumn() : -1;
	}

	/**
	 * Plays the player's own drop straight away rather than waiting for the server to confirm it.
	 * The server validates the same move against the same core and will send back a state that
	 * agrees; if it somehow does not, that state simply replaces this one.
	 */
	public void applyLocalMove(int column) {
		board.play(column);
		playerToMove = false;
	}

	public boolean canPlay(int column) {
		return !outcome.isOver() && board.canPlay(column);
	}

	/** Discs played out of the total the board holds. */
	public int discs() {
		return board.moves();
	}

	public int capacity() {
		return geometry.size();
	}
}
