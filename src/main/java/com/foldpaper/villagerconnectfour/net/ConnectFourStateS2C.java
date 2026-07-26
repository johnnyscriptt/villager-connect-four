package com.foldpaper.villagerconnectfour.net;

import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Authoritative board state after any drop or game end. {@code lastColumn} is the column just
 * played so the client can animate the disc falling into it, and is -1 when there is no move to
 * show — a game abandoned or resigned rather than played.
 */
public record ConnectFourStateS2C(int gameId, String moves, int lastColumn, int outcomeId,
		boolean playerToMove) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConnectFourStateS2C> TYPE =
			new CustomPacketPayload.Type<>(VillagerConnectFour.id("state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConnectFourStateS2C> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, ConnectFourStateS2C::gameId,
					ByteBufCodecs.STRING_UTF8, ConnectFourStateS2C::moves,
					ByteBufCodecs.VAR_INT, ConnectFourStateS2C::lastColumn,
					ByteBufCodecs.VAR_INT, ConnectFourStateS2C::outcomeId,
					ByteBufCodecs.BOOL, ConnectFourStateS2C::playerToMove,
					ConnectFourStateS2C::new);

	public ConnectFourOutcome outcome() {
		return ConnectFourOutcome.byId(outcomeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
