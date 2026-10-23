package com.foldpaper.villagerconnectfour.client;

import com.foldpaper.villagerconnectfour.client.gui.ClientConnectFourGame;
import com.foldpaper.villagerconnectfour.client.gui.ConnectFourScreen;
import com.foldpaper.villagerconnectfour.net.ConnectFourChallengeC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourOpenS2C;
import com.foldpaper.villagerconnectfour.net.ConnectFourStateS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;

public class VillagerConnectFourClient implements ClientModInitializer {
	// The merchant GUI texture is 276x166 (see MerchantScreen's constructor).
	private static final int MERCHANT_GUI_WIDTH = 276, MERCHANT_GUI_HEIGHT = 166;
	/** Minimum button width; the real width grows to fit the label. See onInitializeClient. */
	private static final int BUTTON_W = 70, BUTTON_H = 20, STACK_STEP = 22;
	/** Left+right breathing room around the label, matching vanilla's button padding. */
	private static final int LABEL_PADDING = 10;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ConnectFourOpenS2C.TYPE, (payload, context) ->
				Minecraft.getInstance().gui.setScreen(new ConnectFourScreen(new ClientConnectFourGame(payload))));
		ClientPlayNetworking.registerGlobalReceiver(ConnectFourStateS2C.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof ConnectFourScreen board
					&& board.gameId() == payload.gameId()) {
				board.onServerState(payload);
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof MerchantScreen)) return;
			Component label = Component.translatable("villagerconnectfour.play");
			int x = scaledWidth / 2 + MERCHANT_GUI_WIDTH / 2 + 4;
			// Fit the label instead of trusting a fixed width. "Connect Four" is longer than the
			// 70px that suits Villager Chess's label, and Villager Checkers already shipped once
			// rendering as "Play Checker" for exactly this reason. Clamped so a wide translation
			// still cannot push the button off the right edge.
			int w = Math.max(BUTTON_W, client.font.width(label) + LABEL_PADDING);
			w = Math.min(w, Math.max(BUTTON_W, scaledWidth - x - 2));
			int y = (scaledHeight - MERCHANT_GUI_HEIGHT) / 2 + STACK_STEP * occupiedRows(screen, x, w);
			y = Math.min(y, scaledHeight - BUTTON_H - 2);
			Screens.getWidgets(screen).add(Button.builder(label,
					button -> ClientPlayNetworking.send(new ConnectFourChallengeC2S()))
					.bounds(x, y, w, BUTTON_H)
					.build());
		});
	}

	/**
	 * How many widgets already sit in the column we want, so the sibling Villager Games mods
	 * stack instead of landing on the same pixel. Vanilla's merchant widgets all live inside
	 * the 276-wide panel, left of x, so they never count. Which mod gets which row depends on
	 * init order and may vary between launches - they never overlap, which is the point.
	 */
	private static int occupiedRows(Screen screen, int x, int width) {
		int count = 0;
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget.getX() + widget.getWidth() > x && widget.getX() < x + width) count++;
		}
		return count;
	}
}
