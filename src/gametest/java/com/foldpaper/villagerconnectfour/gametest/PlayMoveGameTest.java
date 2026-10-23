package com.foldpaper.villagerconnectfour.gametest;

import com.foldpaper.villagerconnectfour.client.gui.ClientConnectFourGame;
import com.foldpaper.villagerconnectfour.client.gui.ConnectFourScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;

/**
 * The player's real path into a game: open a villager's trades, press Connect Four, then drop a
 * disc with a real left click and wait for the villager's reply. Fails if the board never
 * accepts the click (the "can't play" report).
 */
public final class PlayMoveGameTest implements FabricClientGameTest {
	private static final int CENTRE_COLUMN = 3;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set noon");

			BlockPos base = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos origin = player.blockPosition().above(20);
				for (int x = -3; x <= 3; x++) {
					for (int z = -3; z <= 5; z++) {
						player.level().setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
					}
				}
				player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
				return origin;
			});
			context.waitFor(client -> client.player != null
					&& client.player.blockPosition().getY() == base.getY(), 100);
			world.getServer().runCommand("summon villager " + base.getX() + " " + base.getY() + " " + (base.getZ() + 2)
					+ " {NoAI:1b,VillagerData:{profession:\"minecraft:farmer\",level:1,type:\"minecraft:plains\"}}");
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);

			// Open the trade screen the way a right-click does (opening it is not under test).
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Villager villager = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(5))
						.getFirst();
				player.interactOn(villager, InteractionHand.MAIN_HAND, villager.position());
			});
			context.waitForScreen(MerchantScreen.class);
			context.waitTicks(5);

			AbstractWidget play = context.computeOnClient(client -> Screens.getWidgets(client.gui.screen()).stream()
					.filter(w -> w.getMessage().getString().equals(Component.translatable("villagerconnectfour.play").getString()))
					.findFirst().orElseThrow(() -> new AssertionError("no Connect Four button on the trade screen")));
			click(context, play.getX() + play.getWidth() / 2.0, play.getY() + play.getHeight() / 2.0);

			context.waitForScreen(ConnectFourScreen.class);
			context.waitFor(client -> game(client).playerToMove, 400);
			context.waitTicks(30);   // let any opening villager drop finish animating
			context.takeScreenshot("villagerconnectfour-board");

			int discsBefore = context.computeOnClient(client -> game(client).discs());
			double[] centre = context.computeOnClient(client -> {
				ConnectFourScreen screen = (ConnectFourScreen) client.gui.screen();
				int cell = intField(screen, "cell"), boardX = intField(screen, "boardX"), boardY = intField(screen, "boardY");
				return new double[] {boardX + CENTRE_COLUMN * cell + cell / 2.0, boardY + cell / 2.0};
			});
			click(context, centre[0], centre[1]);
			context.waitTicks(2);
			context.takeScreenshot("villagerconnectfour-after-move");

			int discsAfter = context.computeOnClient(client -> game(client).discs());
			if (discsAfter != discsBefore + 1) throw new AssertionError("clicking a column did not drop a disc");

			// The server accepted it and the villager answered.
			context.waitFor(client -> game(client).discs() >= discsBefore + 2 && game(client).playerToMove, 400);
			context.takeScreenshot("villagerconnectfour-reply");
			context.setScreen(() -> null);
		}
	}

	/** Moves the real cursor to a GUI-space point and presses the left button there. */
	private static void click(ClientGameTestContext context, double guiX, double guiY) {
		double scale = context.computeOnClient(client -> (double) client.getWindow().getGuiScale());
		context.getInput().setCursorPos(guiX * scale, guiY * scale);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static ClientConnectFourGame game(Minecraft client) {
		try {
			Field field = ConnectFourScreen.class.getDeclaredField("game");
			field.setAccessible(true);
			return (ClientConnectFourGame) field.get(client.gui.screen());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static int intField(ConnectFourScreen screen, String name) {
		try {
			Field field = ConnectFourScreen.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.getInt(screen);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
