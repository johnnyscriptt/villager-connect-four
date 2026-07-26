package com.foldpaper.villagerconnectfour.mixin;

import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantMenu.class)
public interface MerchantMenuAccessor {
	// The villagerconnectfour$ prefix keeps this from clashing with the sibling game mods,
	// which all mix into MerchantMenu the same way.
	@Accessor("trader")
	Merchant villagerconnectfour$getTrader();
}
