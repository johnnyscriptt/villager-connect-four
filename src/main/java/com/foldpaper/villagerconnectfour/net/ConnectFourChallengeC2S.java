package com.foldpaper.villagerconnectfour.net;

import com.foldpaper.villagerconnectfour.VillagerConnectFour;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Player clicked "Connect Four"; the server resolves the villager from the open trade menu. */
public record ConnectFourChallengeC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ConnectFourChallengeC2S> TYPE =
			new CustomPacketPayload.Type<>(VillagerConnectFour.id("challenge"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConnectFourChallengeC2S> CODEC =
			StreamCodec.unit(new ConnectFourChallengeC2S());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
