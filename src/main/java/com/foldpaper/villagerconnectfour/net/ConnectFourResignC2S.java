package com.foldpaper.villagerconnectfour.net;

import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The player gave up, or closed the board while a game was still running. */
public record ConnectFourResignC2S(int gameId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConnectFourResignC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerConnectFour.id("resign"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConnectFourResignC2S> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, ConnectFourResignC2S::gameId,
					ConnectFourResignC2S::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
