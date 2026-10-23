package com.foldpaper.villagerconnectfour.game;

import com.foldpaper.villagerconnectfour.ConnectFourConfig;
import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.engine.DifficultyProfile;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One running game between a player and a villager. All state is server-side.
 *
 * <p>Colour tracks move order rather than who is holding the mouse - yellow always drops first -
 * so which colour the player has depends on the {@code first-player} setting, and both sides
 * record it once here rather than working it out again at every call site.
 */
final class ConnectFourSession {
	final int gameId;
	final UUID playerId;
	final Villager villager;
	final Board board = new Board(ConnectFourConfig.geometry());
	final DifficultyProfile profile;
	final Random random = new Random();
	/** Cancels an in-flight engine search when the session dies. */
	final AtomicBoolean stop = new AtomicBoolean();

	/** {@link Board#YELLOW} or {@link Board#RED} - whichever the player was dealt. */
	final int playerColour;

	/** Moves applied so far; used to reject stale engine results. */
	int movesMade;
	/** True while the engine is computing the villager's reply. */
	boolean aiThinking;

	ConnectFourSession(int gameId, UUID playerId, Villager villager, int villagerLevel) {
		this.gameId = gameId;
		this.playerId = playerId;
		this.villager = villager;
		this.profile = DifficultyProfile.forVillagerLevel(villagerLevel);
		this.playerColour = ConnectFourConfig.villagerMovesFirst() ? Board.RED : Board.YELLOW;
	}

	int villagerColour() {
		return -playerColour;
	}

	boolean playerIsFirst() {
		return playerColour == Board.YELLOW;
	}

	boolean isPlayersTurn() {
		return !aiThinking && board.sideToMove() == playerColour;
	}
}
