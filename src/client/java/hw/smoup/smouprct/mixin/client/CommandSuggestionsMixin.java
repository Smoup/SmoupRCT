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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {

    @Shadow
    @Final
    EditBox input;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    public abstract void showSuggestions(boolean narrateFirstSuggestion);

    @Inject(method = "updateCommandInfo", at = @At("TAIL"))
    private void smouprct$suggestDotCommands(CallbackInfo ci) {
        String text = input.getValue();
        if (!text.startsWith(".")) return;

        List<String> matches = ChatCompletion.matching(text);
        if (matches.isEmpty()) return;

        StringRange range = StringRange.between(0, text.length());
        List<Suggestion> merged = new ArrayList<>();

        if (pendingSuggestions != null) {
            Suggestions existing = pendingSuggestions.getNow(null);
            if (existing != null) {
                merged.addAll(existing.getList());
            }
        }

        for (String command : matches) {
            boolean already = merged.stream().anyMatch(s -> s.getText().equals(command));
            if (!already) {
                merged.add(new Suggestion(range, command));
            }
        }

        pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(range, merged));
        showSuggestions(false);
    }
}
