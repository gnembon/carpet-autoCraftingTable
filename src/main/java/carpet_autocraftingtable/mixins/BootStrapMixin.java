package carpet_autocraftingtable.mixins;

import carpet_autocraftingtable.CraftingTableBlockEntity;
import net.minecraft.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public class BootStrapMixin {
	@Inject(
		method="initialize",
		at=@At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/registry/Registry;freezeRegistries()V",
			shift = At.Shift.BEFORE
		)
	)
	private static void registerTableBeforeRegistryFreeze(CallbackInfo ci) {
		CraftingTableBlockEntity.init();
	}
}
