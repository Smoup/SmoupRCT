package hw.smoup.smouprct.mixin.client;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import hw.smoup.smouprct.client.ChatCompletion;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Подставляет команды мода (с префиксом «.») в нативный список подсказок чата —
 * тот же попап, что и у серверных команд: расширяемый и листается по Tab.
 *
 * @see ChatCompletion
 */
@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow
    @Final
    EditBox input;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Shadow
    public abstract void hide();

    @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
    private void smouprct$suggestDotCommands(CallbackInfo ci) {
        String text = input.getValue();
        if (!text.startsWith(".")) return;

        ci.cancel();

        List<String> matches = ChatCompletion.matching(text);
        if (matches.isEmpty()) {
            hide();
            return;
        }

        StringRange range = StringRange.between(0, text.length());
        List<Suggestion> list = matches.stream().map(command -> new Suggestion(range, command)).toList();
        pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, list));
        showSuggestions(false);
    }
}
