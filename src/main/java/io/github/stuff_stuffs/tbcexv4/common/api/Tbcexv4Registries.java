package io.github.stuff_stuffs.tbcexv4.common.api;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.DamagePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.EndBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.NoopBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.StartBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequestType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.DebugBattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.DebugBattleActionRequestType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAIControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.*;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.RegisteredStat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.InOrderTurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManagerType;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.DamagePhaseImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item.UnknownBattleItem;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat.StatModificationPhaseImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Tbcexv4Registries {
    public static final class BattleActions {
        public static final RegistryKey<Registry<BattleActionType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("battle_actions"));
        public static final Registry<BattleActionType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleActionType<?>> CODEC = REGISTRY.getCodec();
        public static final BattleActionType<SetupEnvironmentBattleAction> SETUP_ENVIRONMENT_TYPE = new BattleActionType<>(SetupEnvironmentBattleAction.CODEC_FACTORY);
        public static final BattleActionType<StartBattleAction> START_BATTLE_TYPE = new BattleActionType<>(StartBattleAction.CODEC_FACTORY);
        public static final BattleActionType<NoopBattleAction> NOOP_TYPE = new BattleActionType<>(context -> NoopBattleAction.CODEC);
        public static final BattleActionType<EndBattleAction> END_TYPE = new BattleActionType<>(context -> EndBattleAction.CODEC);

        public static void init() {
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("setup_env"), SETUP_ENVIRONMENT_TYPE);
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("start"), START_BATTLE_TYPE);
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("noop"), NOOP_TYPE);
            Registry.register(BattleActions.REGISTRY, Tbcexv4.id("end"), END_TYPE);
        }
    }

    public static final class Stats {
        public static final RegistryKey<Registry<RegisteredStat<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("stats"));
        public static final Registry<RegisteredStat<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<RegisteredStat<?>> CODEC = REGISTRY.getCodec();
        public static final RegisteredStat<Double> MAX_HEALTH = new RegisteredStat<>() {
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
        public static final Comparator<StatModificationPhase> COMPARATOR = StatModificationPhaseImpl.COMPARATOR;
        public static final StatModificationPhase BASE_STATS = StatModificationPhase.create(Set.of(), Set.of());
        public static final StatModificationPhase DEFAULT = StatModificationPhase.create(Set.of(Tbcexv4.id("base_stats")), Set.of());

        public static void init() {
            RegistryEntryAddedCallback.event(REGISTRY).register((rawId, id, object) -> StatModificationPhaseImpl.SORTED.setFalse());
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
            RegistryEntryAddedCallback.event(REGISTRY).register((rawId, id, object) -> DamagePhaseImpl.SORTED.setFalse());
        }
    }

    public static final class ItemTypes {
        public static final RegistryKey<Registry<BattleItemType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("item_types"));
        public static final Registry<BattleItemType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleItemType<?>> CODEC = REGISTRY.getCodec();
        public static final BattleItemType<UnknownBattleItem> UNKNOWN_BATTLE_ITEM_TYPE = new BattleItemType<>(
                UnknownBattleItem.CODEC_FACTORY,
                (first, second) -> {
                    if (first.item() instanceof final UnknownBattleItem firstItem && second.item() instanceof final UnknownBattleItem secondItem) {
                        if (firstItem.item == secondItem.item && firstItem.nbt.equals(secondItem.nbt)) {
                            final IntSet sourceSlots = new IntOpenHashSet();
                            sourceSlots.addAll(firstItem.sourceSlots);
                            sourceSlots.addAll(secondItem.sourceSlots);
                            return Optional.of(new BattleItemStack(new UnknownBattleItem(firstItem.item, firstItem.nbt, sourceSlots), first.count() + second.count()));
                        }
                    }
                    return Optional.empty();
                },
                (first, second) -> first.item == second.item && first.nbt.equals(second.nbt)
        );

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

    public static final class TurnManagerTypes {
        public static final RegistryKey<Registry<TurnManagerType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("turn_manager_types"));
        public static final Registry<TurnManagerType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<TurnManagerType<?>> CODEC = REGISTRY.getCodec();
        public static final TurnManagerType<Unit> IN_ORDER_TURN_MANAGER_TYPE = new TurnManagerType<>((context, unit) -> new InOrderTurnManager(), context -> Codec.unit(Unit.INSTANCE));

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("in_order"), IN_ORDER_TURN_MANAGER_TYPE);
        }
    }

    public static final class BattleActionRequestTypes {
        public static final RegistryKey<Registry<BattleActionRequestType<?>>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("battle_action_request_type"));
        public static final Registry<BattleActionRequestType<?>> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final Codec<BattleActionRequestType<?>> CODEC = REGISTRY.getCodec();
        public static final BattleActionRequestType<DebugBattleActionRequest> DEBUG_TYPE = new DebugBattleActionRequestType();

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("debug"), DEBUG_TYPE);
        }
    }

    public static final class DamageTypes {
        public static final RegistryKey<Registry<DamageType>> KEY = RegistryKey.ofRegistry(Tbcexv4.id("damage_types"));
        public static final Registry<DamageType> REGISTRY = FabricRegistryBuilder.createSimple(KEY).buildAndRegister();
        public static final DamageType ROOT = new DamageType(Text.of("root"), Text.of("Pure damage"), Set.of(), Set.of());

        public static void init() {
            Registry.register(REGISTRY, Tbcexv4.id("basic"), ROOT);
        }
    }

    public static final class BattleParticipantAttachmentTypes {
        public static final BattleParticipantAttachmentType<BattleParticipantPlayerControllerAttachmentView, BattleParticipantPlayerControllerAttachment> PLAYER_CONTROLLED = new BattleParticipantAttachmentType<>((participantView, attachment) -> Text.of("Controlled by: " + attachment.controllerId()), (participantView, attachment) -> true, Function.identity());
        public static final BattleParticipantAttachmentType<Unit, BattleParticipantAIControllerAttachment> AI_CONTROLLER = new BattleParticipantAttachmentType<>((participantView, attachment) -> Text.of("Controlled by AI"), (participantView, attachment) -> true, mut -> Unit.INSTANCE);
    }

    public static final class TargetTypes {
        public static final TargetType<ParticipantTarget> PARTICIPANT_TARGET = new TargetType<>() {
            @Override
            public Text name() {
                return Text.of("Participant");
            }

            @Override
            public Text description(final ParticipantTarget target) {
                return Text.of("TODO");
            }
        };

        public static final TargetType<PosTarget> POS_TARGET = new TargetType<>() {
            @Override
            public Text name() {
                return Text.of("Participant");
            }

            @Override
            public Text description(final PosTarget target) {
                return Text.of("TODO");
            }
        };
    }

    public static final class DefaultPlans {
        private static final List<PlanFactory> FACTORIES = new ObjectArrayList<>();

        public static void forEach(final BattleParticipantView participant, final Consumer<Plan> consumer) {
            for (int i = 0, size = FACTORIES.size(); i < size; i++) {
                final PlanFactory factory = FACTORIES.get(i);
                factory.create(participant, consumer);
            }
        }

        public static void register(final PlanFactory factory) {
            FACTORIES.add(factory);
        }
    }

    public static void init() {
        BattleActions.init();
        Stats.init();
        StatModificationPhases.init();
        DamagePhases.init();
        ItemTypes.init();
        EquipmentSlots.init();
        TurnManagerTypes.init();
        DamageTypes.init();
        BattleActionRequestTypes.init();
    }

    private Tbcexv4Registries() {
    }
}
