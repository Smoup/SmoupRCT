package hw.smoup.smouprct.mixin.client;

import hw.smoup.smouprct.client.JoinMemory;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает клики игрока по слотам контейнера, чтобы запоминать сервер,
 * на который он заходит (см. {@link JoinMemory}).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"))
    private void smouprct$onSlotClick(int containerId, int slotId, int mouseButton,
                                      ClickType clickType, Player player, CallbackInfo ci) {
        JoinMemory.onSlotClick(slotId);
    }
}
