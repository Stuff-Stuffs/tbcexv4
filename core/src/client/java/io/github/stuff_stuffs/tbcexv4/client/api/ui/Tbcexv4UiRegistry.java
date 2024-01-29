package io.github.stuff_stuffs.tbcexv4.client.api.ui;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.DamageResistanceStat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.RegisteredStat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Tbcexv4UiRegistry {
    private static final Object2ReferenceMap<Stat<?>, StatRenderer> STAT_RENDERERS = new Object2ReferenceOpenHashMap<>();

    public static void register(final Stat<?> stat, final StatRenderer renderer) {
        if (STAT_RENDERERS.putIfAbsent(stat, renderer) != null) {
            throw new RuntimeException();
        }
    }

    public static List<Component> render(final BattleParticipantView participant) {
        final List<Component> components = new ArrayList<>(STAT_RENDERERS.size());
        for (final Object2ReferenceMap.Entry<Stat<?>, StatRenderer> entry : Object2ReferenceMaps.fastIterable(STAT_RENDERERS)) {
            if (entry.getValue().visible(participant)) {
                components.add(entry.getValue().create(participant));
            }
        }
        return components;
    }

    public interface StatRenderer {
        boolean visible(BattleParticipantView participant);

        Component create(BattleParticipantView participant);
    }

    public static StatRenderer basic(final Stat<Double> stat, final Predicate<BattleParticipantView> visible) {
        return new StatRenderer() {
            @Override
            public boolean visible(final BattleParticipantView participant) {
                return visible.test(participant);
            }

            @Override
            public Component create(final BattleParticipantView participant) {
                final FlowLayout layout = Containers.horizontalFlow(Sizing.fill(), Sizing.fill());
                if (stat instanceof final RegisteredStat<Double> registeredStat) {
                    layout.child(Components.label(Text.of(Tbcexv4Registries.Stats.REGISTRY.getId(registeredStat).toString())));
                } else if (stat instanceof final DamageResistanceStat resistanceStat) {
                    final Identifier damageTypeId = Tbcexv4Registries.DamageTypes.REGISTRY.getId(resistanceStat.damageType());
                    layout.child(Components.label(Text.of("Resistance to " + damageTypeId)));
                }
                layout.child(Components.label(Text.of(Double.toString(participant.stats().get(stat)))));
                return layout;
            }
        };
    }

    private Tbcexv4UiRegistry() {
    }
}
