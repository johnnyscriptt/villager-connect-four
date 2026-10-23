package com.foldpaper.villagerconnectfour.client.gui;

import com.foldpaper.villagerconnectfour.connectfour.Board;
import com.foldpaper.villagerconnectfour.net.ConnectFourDropC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourOutcome;
import com.foldpaper.villagerconnectfour.net.ConnectFourResignC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourStateS2C;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Connect Four board. Input is a column rather than a cell: hovering shows a ghost disc in
 * the headroom above the column it would fall into, and clicking anywhere in that column drops
 * it. A full column refuses with a flash and a dull note rather than silently doing nothing.
 *
 * <p>Row 0 is the bottom of the board in the core and is drawn at the bottom here, so unlike the
 * Checkers sibling there is no row flip - only the one row of headroom above the slab, which the
 * ghost and the start of every fall live in.
 */
public class ConnectFourScreen extends Screen {
	private static final int SLAB = 0xFF2A4AA8, SLAB_EDGE = 0xFF1B2F6B;
	private static final int FRAME_OUTER = 0xFF141C34;
	private static final int COLUMN_TINT = 0x22FFFFFF, LAST_MOVE_TINT = 0x40FFE94D;
	private static final int REFUSED_TINT = 0x90FF3030;
	private static final int WIN_OUTLINE = 0xFFFFF2A0;
	private static final int PANEL = 0xE0242018, MUTED_TEXT = 0xFFB0A890;
	private static final int VILLAGER_PANEL_W = 70, INFO_PANEL_W = 96;
	private static final int MIN_CELL = 14, MAX_CELL = 30;
	private static final long REFUSED_FLASH_MS = 220;
	/** Discs not part of the winning line fade back once the game is decided. */
	private static final float DIMMED = 0.55f;

	private final ClientConnectFourGame game;
	private int cell, boardX, boardY, villagerX, infoX;
	private Button actionButton;
	private boolean playedEndSound;
	private int refusedColumn = -1;
	private long refusedUntilMs;
	private DropAnimation falling;

	public ConnectFourScreen(ClientConnectFourGame game) {
		super(Component.translatable("villagerconnectfour.title", game.villagerName));
		this.game = game;
	}

	public int gameId() {
		return game.gameId;
	}

	public void onServerState(ConnectFourStateS2C payload) {
		boolean wasOver = game.outcome.isOver();
		int landed = game.applyServerState(payload);
		if (landed >= 0) {
			// The disc is already on the logical board; this only animates it arriving.
			int row = rowOfLastDiscIn(landed);
			boolean yellow = row >= 0 && game.board.discAt(landed, row) == Board.YELLOW;
			falling = new DropAnimation(landed, Math.max(row, 0), yellow,
					game.geometry.height(), System.currentTimeMillis());
			ui(SoundEvents.WOOD_PLACE, 1.3f, 0.6f);
		}
		if (!wasOver && game.outcome == ConnectFourOutcome.PLAYER_WON && !playedEndSound) {
			playedEndSound = true;
			ui(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.9f);
		}
		if (actionButton != null && game.outcome.isOver()) {
			actionButton.setMessage(Component.translatable("villagerconnectfour.close"));
		}
	}

	/** The row the topmost disc in a column sits on, or -1 if the column is empty. */
	private int rowOfLastDiscIn(int column) {
		for (int row = game.geometry.height() - 1; row >= 0; row--) {
			if (game.board.discAt(column, row) != Board.EMPTY) return row;
		}
		return -1;
	}

	@Override
	protected void init() {
		int cols = game.geometry.width(), rows = game.geometry.height();
		int availH = (height - 90) / (rows + 1);   // +1 for the headroom row
		int availW = (width - VILLAGER_PANEL_W - INFO_PANEL_W - 44) / cols;
		cell = Math.clamp(Math.min(availH, availW), MIN_CELL, MAX_CELL);

		int clusterW = VILLAGER_PANEL_W + 8 + 8 + cols * cell + 8 + INFO_PANEL_W;
		int clusterX = Math.max(4, (width - clusterW) / 2);
		villagerX = clusterX;
		boardX = clusterX + VILLAGER_PANEL_W + 8 + 8;
		infoX = boardX + cols * cell + 8;
		boardY = Math.max(24 + cell, (height - (rows * cell + 48)) / 2 + 14);

		actionButton = addRenderableWidget(Button.builder(
				Component.translatable(game.outcome.isOver()
						? "villagerconnectfour.close" : "villagerconnectfour.resign"),
				b -> onClose())
				.bounds(boardX + cols * cell - 60, boardY + rows * cell + 26, 60, 16)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gg, mouseX, mouseY, partialTick);
		gg.nextStratum();

		gg.centeredText(font, title, width / 2, boardY - cell - 14, 0xFFFFFFFF);
		drawBoard(gg, mouseX, mouseY);
		drawVillagerPanel(gg, mouseX, mouseY);
		drawInfoPanel(gg);
		gg.centeredText(font, statusLine(), boardX + game.geometry.width() * cell / 2,
				boardY + game.geometry.height() * cell + 15, MUTED_TEXT);

		if (game.outcome.isOver()) drawEndBanner(gg);
	}

	private void drawBoard(GuiGraphicsExtractor gg, int mouseX, int mouseY) {
		int cols = game.geometry.width(), rows = game.geometry.height();
		int right = boardX + cols * cell, bottom = boardY + rows * cell;
		long now = System.currentTimeMillis();

		if (falling != null && falling.finished(now)) falling = null;

		int hovered = hoveredColumn(mouseX, mouseY);

		// The slab, with a hole cut for every cell.
		gg.fill(boardX - 5, boardY - 5, right + 5, bottom + 5, FRAME_OUTER);
		gg.fill(boardX - 3, boardY - 3, right + 3, bottom + 3, SLAB_EDGE);
		gg.fill(boardX, boardY, right, bottom, SLAB);
		if (hovered >= 0) {
			gg.fill(cellX(hovered), boardY - cell, cellX(hovered) + cell, bottom, COLUMN_TINT);
		}
		if (refusedColumn >= 0 && now < refusedUntilMs) {
			gg.fill(cellX(refusedColumn), boardY, cellX(refusedColumn) + cell, bottom, REFUSED_TINT);
		}

		int[][] winners = game.outcome.isOver() ? game.board.winningLine() : null;
		for (int col = 0; col < cols; col++) {
			for (int row = 0; row < rows; row++) {
				int x = cellX(col), y = cellY(row);
				Discs.hole(gg, x, y, cell);
				int disc = game.board.discAt(col, row);
				if (disc == Board.EMPTY) continue;
				// A disc still in the air is drawn by the animation instead of in its socket.
				if (falling != null && !falling.landed(now)
						&& falling.column == col && falling.targetRow == row) {
					continue;
				}
				float alpha = winners != null && !contains(winners, col, row) ? DIMMED : 1.0f;
				Discs.draw(gg, disc == Board.YELLOW, x, y, cell, alpha);
			}
		}

		if (winners != null) {
			for (int[] winner : winners) {
				Discs.outline(gg, cellX(winner[0]), cellY(winner[1]), cell, WIN_OUTLINE);
			}
		}

		drawFallingDisc(gg, now);
		drawGhost(gg, hovered);
	}

	private void drawFallingDisc(GuiGraphicsExtractor gg, long now) {
		if (falling == null || falling.landed(now)) return;
		int x = cellX(falling.column);
		int y = boardY + Math.round((game.geometry.height() - 1 - falling.rowAt(now)) * cell);
		Discs.draw(gg, falling.yellow, x, y, cell, 1.0f);
	}

	/** The disc the player is about to drop, hanging over the column they are pointing at. */
	private void drawGhost(GuiGraphicsExtractor gg, int hovered) {
		if (hovered < 0 || !game.playerToMove || game.outcome.isOver()) return;
		boolean full = !game.board.canPlay(hovered);
		if (full) return;
		Discs.draw(gg, game.playerColour() == Board.YELLOW, cellX(hovered), boardY - cell, cell, 0.45f);
	}

	private static boolean contains(int[][] cells, int col, int row) {
		for (int[] cellPair : cells) {
			if (cellPair[0] == col && cellPair[1] == row) return true;
		}
		return false;
	}

	private void drawVillagerPanel(GuiGraphicsExtractor gg, int mouseX, int mouseY) {
		int rows = game.geometry.height();
		int x2 = villagerX + VILLAGER_PANEL_W, y2 = boardY + rows * cell;
		gg.fill(villagerX - 2, boardY - 2, x2 + 2, y2 + 2, FRAME_OUTER);
		gg.fill(villagerX, boardY, x2, y2, PANEL);
		if (game.villagerEntity != null && !game.villagerEntity.isRemoved()) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(gg, villagerX, boardY, x2, y2,
					(int) (rows * cell * 0.34f), 0.0625f, mouseX, mouseY, game.villagerEntity);
		}
		if (!game.playerToMove && !game.outcome.isOver()) {
			int dots = (int) (System.currentTimeMillis() / 300 % 4);
			gg.centeredText(font, Component.literal("thinking" + ".".repeat(dots)),
					villagerX + VILLAGER_PANEL_W / 2, y2 - 12, 0xFFFFF0A0);
		}
		gg.centeredText(font, Component.literal(game.villagerName),
				villagerX + VILLAGER_PANEL_W / 2, y2 + 4, 0xFFFFFFFF);
		gg.centeredText(font, Component.translatable("merchant.level." + game.villagerLevel),
				villagerX + VILLAGER_PANEL_W / 2, y2 + 14, MUTED_TEXT);
	}

	private void drawInfoPanel(GuiGraphicsExtractor gg) {
		int mini = 8;
		// Who is who, so the colours are never ambiguous.
		Discs.draw(gg, game.playerColour() == Board.YELLOW, infoX, boardY, mini, 1.0f);
		gg.text(font, Component.translatable("villagerconnectfour.info.you"),
				infoX + mini + 4, boardY + 1, 0xFFFFFFFF);
		if (game.playerIsFirst) {
			gg.text(font, Component.translatable("villagerconnectfour.info.first"),
					infoX + mini + 26, boardY + 1, MUTED_TEXT);
		}
		Discs.draw(gg, game.playerColour() != Board.YELLOW, infoX, boardY + 12, mini, 1.0f);
		gg.text(font, Component.literal(game.villagerName), infoX + mini + 4, boardY + 13, MUTED_TEXT);

		gg.text(font, Component.translatable("villagerconnectfour.info.discs",
				String.valueOf(game.discs()), String.valueOf(game.capacity())),
				infoX, boardY + 26, MUTED_TEXT);

		int listY = boardY + 40;
		int maxLines = Math.max(1, (game.geometry.height() * cell - 44) / 10);
		List<String> lines = moveLines();
		int start = Math.max(0, lines.size() - maxLines);
		for (int i = start; i < lines.size(); i++) {
			gg.text(font, lines.get(i), infoX + 2, listY + (i - start) * 10, MUTED_TEXT);
		}
	}

	/** The columns played, paired up as numbered turns. */
	private List<String> moveLines() {
		int[] history = game.board.history();
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < history.length; i += 2) {
			String line = (i / 2 + 1) + ". " + (history[i] + 1);
			if (i + 1 < history.length) line += "  " + (history[i + 1] + 1);
			lines.add(line);
		}
		return lines;
	}

	private void drawEndBanner(GuiGraphicsExtractor gg) {
		gg.nextStratum();
		gg.fill(0, 0, width, height, 0x70101010);
		int cx = boardX + game.geometry.width() * cell / 2;
		int cy = boardY + game.geometry.height() * cell / 2;
		boolean solvedHint = game.outcome == ConnectFourOutcome.VILLAGER_WON
				&& game.villagerLevel >= 5 && !game.playerIsFirst;
		int halfHeight = solvedHint ? 38 : 26;
		gg.fill(cx - 130, cy - halfHeight, cx + 130, cy + halfHeight, 0xF01E1A14);
		gg.fill(cx - 130, cy - halfHeight, cx + 130, cy - halfHeight + 2, SLAB_EDGE);
		gg.fill(cx - 130, cy + halfHeight - 2, cx + 130, cy + halfHeight, SLAB_EDGE);
		gg.pose().pushMatrix();
		gg.pose().scale(2, 2);
		gg.centeredText(font, bannerTitle(), cx / 2, (cy - halfHeight + 12) / 2, 0xFFFFE080);
		gg.pose().popMatrix();
		if (solvedHint) {
			// Losing to a perfect opponent should teach something rather than just annoy.
			gg.centeredText(font, Component.translatable("villagerconnectfour.banner.solved_hint"),
					cx, cy + 6, MUTED_TEXT);
		}
		gg.centeredText(font, Component.translatable("villagerconnectfour.banner.close_hint"),
				cx, cy + halfHeight - 12, MUTED_TEXT);
	}

	private Component bannerTitle() {
		return switch (game.outcome) {
			case PLAYER_WON -> Component.translatable("villagerconnectfour.banner.win");
			case VILLAGER_WON -> Component.translatable("villagerconnectfour.banner.loss");
			case PLAYER_RESIGNED -> Component.translatable("villagerconnectfour.status.resigned");
			case ABANDONED -> Component.translatable("villagerconnectfour.status.abandoned");
			default -> Component.translatable("villagerconnectfour.banner.draw");
		};
	}

	private Component statusLine() {
		if (game.outcome != ConnectFourOutcome.ONGOING) return Component.empty();
		return game.playerToMove
				? Component.translatable("villagerconnectfour.status.your_move")
				: Component.translatable("villagerconnectfour.status.thinking", game.villagerName);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;

		if (game.outcome.isOver()) {
			onClose();
			return true;
		}
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !game.playerToMove) return false;

		int column = hoveredColumn((int) event.x(), (int) event.y());
		if (column < 0) return false;
		if (!game.board.canPlay(column)) {
			refuse(column);
			return true;
		}
		drop(column);
		return true;
	}

	/** Flashes a column red so a refused click reads as the rule, not as a broken UI. */
	private void refuse(int column) {
		refusedColumn = column;
		refusedUntilMs = System.currentTimeMillis() + REFUSED_FLASH_MS;
		ui(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.4f);
	}

	private void drop(int column) {
		int row = game.board.landingRow(column);
		boolean yellow = game.playerColour() == Board.YELLOW;
		game.applyLocalMove(column);
		falling = new DropAnimation(column, Math.max(row, 0), yellow,
				game.geometry.height(), System.currentTimeMillis());
		ui(SoundEvents.WOOD_PLACE, 1.3f, 0.6f);
		ClientPlayNetworking.send(new ConnectFourDropC2S(game.gameId, column));
	}

	private void ui(SoundEvent event, float pitch, float volume) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}

	// ---- geometry -------------------------------------------------------------------
	// Row 0 is the bottom of the board in the core and is drawn at the bottom, so the only
	// adjustment is the single row of headroom above the slab that the ghost hangs in.

	private int cellX(int col) {
		return boardX + col * cell;
	}

	private int cellY(int row) {
		return boardY + (game.geometry.height() - 1 - row) * cell;
	}

	/** The column under the cursor, counting the headroom row, or -1 if outside the board. */
	private int hoveredColumn(int x, int y) {
		int rows = game.geometry.height();
		if (y < boardY - cell || y > boardY + rows * cell) return -1;
		int col = Math.floorDiv(x - boardX, cell);
		return col < 0 || col >= game.geometry.width() ? -1 : col;
	}

	@Override
	public void onClose() {
		if (!game.outcome.isOver()) {
			ClientPlayNetworking.send(new ConnectFourResignC2S(game.gameId));
		}
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false; // the villager must keep thinking while the board is open
	}
}
