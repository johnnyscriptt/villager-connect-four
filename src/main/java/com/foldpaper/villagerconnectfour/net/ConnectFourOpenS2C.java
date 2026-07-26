package com.foldpaper.villagerconnectfour.net;

import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Opens the board. State travels as a move sequence — the columns played, as 1-based digits —
 * and {@code villagerEntityId} lets the client draw the actual villager beside it.
 *
 * <p>Unlike the Checkers sibling's open packet, this one carries <b>{@code playerToMove}</b> and
 * <b>{@code playerIsFirst}</b>. Checkers can leave both out because the player there always has
 * the first move; here who starts is a config setting, and by default it is the villager. The
 * client must not assume it is the player's turn just because a board appeared.
 *
 * <p>{@code width} and {@code height} come along too, so a server running a non-standard board
 * size draws correctly on a client that knows nothing about its config.
 */
public record ConnectFourOpenS2C(int gameId, String villagerName, int villagerLevel, String moves,
		int villagerEntityId, int width, int height, boolean playerIsFirst, boolean playerToMove)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConnectFourOpenS2C> TYPE =
			new CustomPacketPayload.Type<>(VillagerConnectFour.id("open"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConnectFourOpenS2C> CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, ConnectFourOpenS2C::gameId,
					ByteBufCodecs.STRING_UTF8, ConnectFourOpenS2C::villagerName,
					ByteBufCodecs.VAR_INT, ConnectFourOpenS2C::villagerLevel,
					ByteBufCodecs.STRING_UTF8, ConnectFourOpenS2C::moves,
					ByteBufCodecs.VAR_INT, ConnectFourOpenS2C::villagerEntityId,
					ByteBufCodecs.VAR_INT, ConnectFourOpenS2C::width,
					ByteBufCodecs.VAR_INT, ConnectFourOpenS2C::height,
					ByteBufCodecs.BOOL, ConnectFourOpenS2C::playerIsFirst,
					ByteBufCodecs.BOOL, ConnectFourOpenS2C::playerToMove,
					ConnectFourOpenS2C::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
