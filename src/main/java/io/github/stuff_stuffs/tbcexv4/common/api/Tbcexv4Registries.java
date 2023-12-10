package io.github.stuff_stuffs.tbcexv4.common.api;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.DamagePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.StartBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.DamagePhaseImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item.UnknownBattleItem;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.Set;

public final class Tbcexv4Registries {
    public static final class BattleActions {
        public static final RegistryKey<Registry<BattleActionType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("battle_actions"));
        public static final Registry<BattleActionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleActionType<?>> CODEC = REGISTRY.getCodec();
        public static final BattleActionType<SetupEnvironmentBattleAction> SETUP_ENVIRONMENT_TYPE = new BattleActionType<>(SetupEnvironmentBattleAction.CODEC_FACTORY);
        public static final BattleActionType<StartBattleAction> START_BATTLE_TYPE = new BattleActionType<>(StartBattleAction.CODEC_FACTORY);

        public static void init() {
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("setup_env"), SETUP_ENVIRONMENT_TYPE);
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("start"), START_BATTLE_TYPE);
        }
    }

    public static final class Stats {
        public static final RegistryKey<Registry<Stat<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("stats"));
        public static final Registry<Stat<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<Stat<?>> CODEC = REGISTRY.getCodec();
        public static final Stat<Double> MAX_HEALTH = new Stat<>() {
            @Override
            public Text displayName() {
                return Text.of("Max Health");
            }

            @Override
            public Double defaultValue() {
                return 0.0;
            }
        };

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("max_health"), MAX_HEALTH);
        }
    }

    public static final class StatModificationPhases {
        public static final RegistryKey<Registry<StatModificationPhase>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("stat_modification_phases"));
        public static final Registry<StatModificationPhase> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<StatModificationPhase> CODEC = REGISTRY.getCodec();
        public static final StatModificationPhase BASE_STATS = StatModificationPhase.create(Set.of(), Set.of());
        public static final StatModificationPhase DEFAULT = StatModificationPhase.create(Set.of(Tbcexv4.id("base_stats")), Set.of());

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("base_stats"), BASE_STATS);
            Registry.register(REGISTRY, Tbcexv4.id("default"), DEFAULT);
        }
    }

    public static final class DamagePhases {
        public static final RegistryKey<Registry<DamagePhase>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("damage_phases"));
        public static final Registry<DamagePhase> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<DamagePhase> CODEC = REGISTRY.getCodec();
        public static final Comparator<DamagePhase> COMPARATOR = DamagePhaseImpl.COMPARATOR;

        public static void init() {
            RegistryEntryAddedCallback.event(REGISTRY).register((rawId, id, object) -> DamagePhaseImpl.SORTED.setRelease(false));
        }
    }

    public static final class ItemTypes {
        public static final RegistryKey<Registry<BattleItemType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("item_types"));
        public static final Registry<BattleItemType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleItemType<?>> CODEC = REGISTRY.getCodec();
        public static final BattleItemType<?> UNKNOWN_BATTLE_ITEM_TYPE = new BattleItemType<>(UnknownBattleItem.CODEC_FACTORY);

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("unknown"), UNKNOWN_BATTLE_ITEM_TYPE);
        }
    }

    public static final class EquipmentSlots {
        public static final RegistryKey<Registry<EquipmentSlot>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("equipment_slots"));
        public static final Registry<EquipmentSlot> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<EquipmentSlot> CODEC = REGISTRY.getCodec();

        public static void init() {
        }
    }

    public static final class BattleParticipantAttachmentTypes {
        public static final RegistryKey<Registry<BattleParticipantAttachmentType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("battle_participant_attachment_types"));
        public static final Registry<BattleParticipantAttachmentType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleParticipantAttachmentType<?>> CODEC = REGISTRY.getCodec();

        public static void init() {
        }
    }

    public static void init() {
        BattleActions.init();
        Stats.init();
        StatModificationPhases.init();
        DamagePhases.init();
        ItemTypes.init();
        EquipmentSlots.init();
        BattleParticipantAttachmentTypes.init();
    }

    private Tbcexv4Registries() {
    }
}
