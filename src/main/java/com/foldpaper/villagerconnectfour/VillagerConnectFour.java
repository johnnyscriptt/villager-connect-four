package com.foldpaper.villagerconnectfour;

import com.foldpaper.villagerconnectfour.game.ConnectFourSessionManager;
import com.foldpaper.villagerconnectfour.net.ConnectFourChallengeC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourDropC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourOpenS2C;
import com.foldpaper.villagerconnectfour.net.ConnectFourResignC2S;
import com.foldpaper.villagerconnectfour.net.ConnectFourStateS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerConnectFour implements ModInitializer {
	public static final String MOD_ID = "villagerconnectfour";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConnectFourConfig.load();

		PayloadTypeRegistry.serverboundPlay().register(ConnectFourChallengeC2S.TYPE, ConnectFourChallengeC2S.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ConnectFourDropC2S.TYPE, ConnectFourDropC2S.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ConnectFourResignC2S.TYPE, ConnectFourResignC2S.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConnectFourOpenS2C.TYPE, ConnectFourOpenS2C.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConnectFourStateS2C.TYPE, ConnectFourStateS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ConnectFourChallengeC2S.TYPE,
				(payload, context) -> ConnectFourSessionManager.INSTANCE.startGame(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ConnectFourDropC2S.TYPE,
				(payload, context) -> ConnectFourSessionManager.INSTANCE.handleDrop(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(ConnectFourResignC2S.TYPE,
				(payload, context) -> ConnectFourSessionManager.INSTANCE.handleResign(context.player(), payload.gameId()));

		ServerTickEvents.END_SERVER_TICK.register(ConnectFourSessionManager.INSTANCE::tick);
		ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> ConnectFourSessionManager.INSTANCE.onDisconnect(handler.getPlayer()));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ConnectFourSessionManager.INSTANCE.shutdown());

		LOGGER.info("Villager Connect Four initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
