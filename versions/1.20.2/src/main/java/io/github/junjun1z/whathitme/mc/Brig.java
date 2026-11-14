package io.github.junjun1z.whathitme.mc;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.github.junjun1z.whathitme.core.cmd.Completion;

/**
 * Registers /whm and /whathitme on a Brigadier dispatcher (any source type: server CommandSourceStack, Forge client
 * commands, Fabric client commands): one greedy argument, split and completed by the core. The same on every version
 * since 1.13.
 */
public final class Brig {

    private Brig() {}

    public static <S> void register(CommandDispatcher<S> d, BiConsumer<S, String[]> run, BiFunction<S, String[], List<String>> complete) {
        for (String name : new String[] {"whm", "whathitme"}) {
            d.register(LiteralArgumentBuilder.<S>literal(name)
                .executes(c -> {
                    run.accept(c.getSource(), new String[0]);
                    return 1;
                })
                .then(RequiredArgumentBuilder.<S, String>argument("args", StringArgumentType.greedyString())
                    .suggests((c, b) -> {
                        String rem = b.getRemaining();
                        SuggestionsBuilder off = b.createOffset(b.getStart() + Completion.lastArgStart(rem));
                        for (String s : complete.apply(c.getSource(), Completion.split(rem))) off.suggest(s);
                        return off.buildFuture();
                    })
                    .executes(c -> {
                        run.accept(c.getSource(), Completion.args(StringArgumentType.getString(c, "args")));
                        return 1;
                    })));
        }
    }
}
