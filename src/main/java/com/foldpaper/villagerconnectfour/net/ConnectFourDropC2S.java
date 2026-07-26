package com.foldpaper.villagerconnectfour.net;

import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A move attempt: the 0-based column the player wants to drop into. A Connect Four turn is a
 * single choice, so unlike the Checkers sibling's hop path there is nothing else to send. The
 * server checks the column against its own board and ignores anything it would not allow.
 */
public record ConnectFourDropC2S(int gameId, int column) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConnectFourDropC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerConnectFour.id("drop"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConnectFourDropC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ConnectFourDropC2S::gameId,
			ByteBufCodecs.VAR_INT, ConnectFourDropC2S::column,
			ConnectFourDropC2S::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
