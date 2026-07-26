package com.foldpaper.villagerconnectfour.game;

import com.foldpaper.villagerconnectfour.ConnectFourConfig;
import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.connectfour.GameStatus;
import com.foldpaper.villagerconnectfour.connectfour.MoveSequence;
import com.foldpaper.villagerconnectfour.connectfour.engine.Engine;
import com.foldpaper.villagerconnectfour.connectfour.engine.TranspositionTable;
import com.foldpaper.villagerconnectfour.mixin.MerchantMenuAccessor;
import com.foldpaper.villagerconnectfour.net.ConnectFourDropC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourOpenS2C;
import com.foldpaper.villagerconnectfour.net.ConnectFourOutcome;
import com.foldpaper.villagerconnectfour.net.ConnectFourStateS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server authority for all Connect Four games. Player drops are validated and applied on the
 * server thread; engine searches run on a single background thread and their results are
 * re-validated and applied in the end-of-tick hook.
 */
public final class ConnectFourSessionManager {
	public static final ConnectFourSessionManager INSTANCE = new ConnectFourSessionManager();

	private static final double START_DISTANCE_SQ = 8 * 8;
	private static final double ABANDON_DISTANCE_SQ = 16 * 16;
	private static final int VALIDATE_EVERY_TICKS = 20;
	private static final int MIN_THINK_MS = 300, EXTRA_THINK_MS = 1200;
	/** No move to animate — the game ended without one being played. */
	private static final int NO_COLUMN = -1;

	private final Map<UUID, ConnectFourSession> byPlayer = new HashMap<>();
	private final Map<UUID, ConnectFourSession> byVillager = new HashMap<>();
	private final ConcurrentLinkedQueue<PendingAiMove> completedSearches = new ConcurrentLinkedQueue<>();
	private final AtomicInteger nextGameId = new AtomicInteger(1);

	/**
	 * One table for every game on the server. A proven score is a fact about a position, not
	 * about the villager who worked it out, so sharing is correct — and it is what lets a Master
	 * arrive at exact play part-way through a game instead of starting cold on every move.
	 *
	 * <p>Safe only because {@link #executor()} is single-threaded. If that ever gains a second
	 * thread this has to become per-thread or be synchronised.
	 */
	private final TranspositionTable sharedTable = new TranspositionTable(20);

	private ExecutorService engineExecutor;
	private int tickCounter;

	private record PendingAiMove(UUID playerId, int gameId, int expectedMovesMade, int column) {}

	private ConnectFourSessionManager() {}

	/** Handles the button: resolves the villager from the player's open trade menu. */
	public void startGame(ServerPlayer player) {
		if (!(player.containerMenu instanceof MerchantMenu menu)) return;
		if (!(((MerchantMenuAccessor) menu).villagerconnectfour$getTrader() instanceof Villager villager)) return;
		if (!villager.isAlive() || villager.distanceToSqr(player) > START_DISTANCE_SQ) return;
		if (byVillager.containsKey(villager.getUUID())) {
			player.sendOverlayMessage(Component.translatable("villagerconnectfour.busy"));
			return;
		}
		endSession(byPlayer.get(player.getUUID()), null); // silently drop any previous game

		int level = villager.getVillagerData().level();
		ConnectFourSession session = new ConnectFourSession(
				nextGameId.getAndIncrement(), player.getUUID(), villager, level);
		byPlayer.put(player.getUUID(), session);
		byVillager.put(villager.getUUID(), session);

		player.closeContainer();
		ServerPlayNetworking.send(player, new ConnectFourOpenS2C(session.gameId,
				villager.getDisplayName().getString(), level, MoveSequence.render(session.board),
				villager.getId(), ConnectFourConfig.geometry().width(),
				ConnectFourConfig.geometry().height(), session.playerIsFirst(), session.isPlayersTurn()));

		// The one real departure from the Checkers sibling: there the player always moves first,
		// so opening the board is the whole of starting a game. Here the villager usually has
		// the first drop, and it has to be set going now — nothing else will ask for it.
		if (!session.isPlayersTurn()) scheduleAiMove(session);

		VillagerConnectFour.LOGGER.debug("connect four game {} started: {} vs villager level {}",
				session.gameId, player.getGameProfile().name(), level);
	}

	public void handleDrop(ServerPlayer player, ConnectFourDropC2S payload) {
		ConnectFourSession session = byPlayer.get(player.getUUID());
		if (session == null || session.gameId != payload.gameId() || !session.isPlayersTurn()) return;
		if (!session.board.canPlay(payload.column())) return;

		applyMove(session, payload.column());
		ConnectFourOutcome outcome = outcomeFor(session);
		sendState(player, session, payload.column(), outcome);
		if (outcome.isOver()) {
			villagerReaction(session, outcome);
			endSession(session, null);
		} else {
			scheduleAiMove(session);
		}
	}

	public void handleResign(ServerPlayer player, int gameId) {
		ConnectFourSession session = byPlayer.get(player.getUUID());
		if (session == null || session.gameId != gameId) return;
		villagerReaction(session, ConnectFourOutcome.PLAYER_RESIGNED);
		endSession(session, ConnectFourOutcome.PLAYER_RESIGNED);
	}

	public void onDisconnect(ServerPlayer player) {
		endSession(byPlayer.get(player.getUUID()), null);
	}

	/** Applies finished engine searches and periodically validates villager/player proximity. */
	public void tick(MinecraftServer server) {
		PendingAiMove pending;
		while ((pending = completedSearches.poll()) != null) {
			ConnectFourSession session = byPlayer.get(pending.playerId());
			if (session == null || session.gameId != pending.gameId()
					|| session.movesMade != pending.expectedMovesMade()) {
				continue; // session ended or was superseded while the engine was thinking
			}
			session.aiThinking = false;
			ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
			if (player == null || pending.column() < 0 || !session.board.canPlay(pending.column())) {
				endSession(session, null);
				continue;
			}
			applyMove(session, pending.column());
			ConnectFourOutcome outcome = outcomeFor(session);
			sendState(player, session, pending.column(), outcome);
			if (outcome.isOver()) {
				villagerReaction(session, outcome);
				endSession(session, null);
			}
		}

		if (++tickCounter % VALIDATE_EVERY_TICKS != 0) return;
		for (ConnectFourSession session : byPlayer.values().toArray(new ConnectFourSession[0])) {
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
			if (player == null) {
				endSession(session, null);
			} else if (!session.villager.isAlive() || session.villager.isRemoved()
					|| session.villager.distanceToSqr(player) > ABANDON_DISTANCE_SQ) {
				notifyEnd(player, session, ConnectFourOutcome.ABANDONED);
				endSession(session, null);
			}
		}
	}

	public void shutdown() {
		for (ConnectFourSession session : byPlayer.values()) session.stop.set(true);
		byPlayer.clear();
		byVillager.clear();
		completedSearches.clear();
		if (engineExecutor != null) {
			engineExecutor.shutdownNow();
			engineExecutor = null;
		}
	}

	private void scheduleAiMove(ConnectFourSession session) {
		session.aiThinking = true;
		Board snapshot = session.board.copy();
		int expectedMovesMade = session.movesMade;
		executor().submit(() -> {
			long start = System.nanoTime();
			int column = Engine.findMove(snapshot, session.profile, session.random,
					session.stop::get, sharedTable);
			// A little artificial thought makes instant replies feel alive. The Master answers
			// the opening from its book in microseconds, which without this reads as a bug.
			long thinkMs = MIN_THINK_MS + session.random.nextInt(EXTRA_THINK_MS);
			long elapsedMs = (System.nanoTime() - start) / 1_000_000;
			if (elapsedMs < thinkMs && !session.stop.get()) {
				try {
					Thread.sleep(thinkMs - elapsedMs);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			if (!session.stop.get()) {
				completedSearches.add(new PendingAiMove(
						session.playerId, session.gameId, expectedMovesMade, column));
			}
		});
	}

	private void applyMove(ConnectFourSession session, int column) {
		session.board.play(column);
		session.movesMade++;
	}

	private void sendState(ServerPlayer player, ConnectFourSession session, int lastColumn,
			ConnectFourOutcome outcome) {
		ServerPlayNetworking.send(player, new ConnectFourStateS2C(session.gameId,
				MoveSequence.render(session.board), lastColumn, outcome.ordinal(),
				session.isPlayersTurn()));
	}

	private void notifyEnd(ServerPlayer player, ConnectFourSession session, ConnectFourOutcome outcome) {
		ServerPlayNetworking.send(player, new ConnectFourStateS2C(session.gameId,
				MoveSequence.render(session.board), NO_COLUMN, outcome.ordinal(), false));
	}

	/** Ends a session; if {@code announce} is non-null the player is notified with that outcome. */
	private void endSession(ConnectFourSession session, ConnectFourOutcome announce) {
		if (session == null) return;
		session.stop.set(true);
		byPlayer.remove(session.playerId);
		byVillager.remove(session.villager.getUUID());
		if (announce != null) {
			ServerPlayer player = session.villager.level().getServer() != null
					? session.villager.level().getServer().getPlayerList().getPlayer(session.playerId)
					: null;
			if (player != null) notifyEnd(player, session, announce);
		}
	}

	private static void villagerReaction(ConnectFourSession session, ConnectFourOutcome outcome) {
		if (!session.villager.isAlive()) return;
		switch (outcome) {
			case PLAYER_WON -> session.villager.playSound(SoundEvents.VILLAGER_NO);
			case VILLAGER_WON, PLAYER_RESIGNED -> session.villager.playSound(SoundEvents.VILLAGER_CELEBRATE);
			case DRAW_FULL -> session.villager.playSound(SoundEvents.VILLAGER_YES);
			default -> {}
		}
	}

	/**
	 * The outcome from the player's point of view. Unlike the Checkers sibling this cannot map
	 * the winning colour to a player straight off, because who has yellow depends on the config.
	 */
	private static ConnectFourOutcome outcomeFor(ConnectFourSession session) {
		GameStatus status = session.board.status();
		return switch (status) {
			case ONGOING -> ConnectFourOutcome.ONGOING;
			case DRAW_FULL -> ConnectFourOutcome.DRAW_FULL;
			case WIN_YELLOW -> session.playerColour == Board.YELLOW
					? ConnectFourOutcome.PLAYER_WON : ConnectFourOutcome.VILLAGER_WON;
			case WIN_RED -> session.playerColour == Board.RED
					? ConnectFourOutcome.PLAYER_WON : ConnectFourOutcome.VILLAGER_WON;
		};
	}

	private ExecutorService executor() {
		if (engineExecutor == null) {
			engineExecutor = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "villagerconnectfour-engine");
				thread.setDaemon(true);
				return thread;
			});
		}
		return engineExecutor;
	}
}
