package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.StartBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.*;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.PlanType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plans;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.SingleTargetPlan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.*;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Tbcexv4Test implements ModInitializer {
    private static final PlanType PLAN_TYPE = new PlanType() {
        @Override
        public Text name() {
            return Text.of("ATTACK");
        }

        @Override
        public Text description() {
            return Text.of("TODO");
        }
    };
    private static final PlanType WALK_PLAN_TYPE = new PlanType() {
        @Override
        public Text name() {
            return Text.of("WALK");
        }

        @Override
        public Text description() {
            return Text.of("TODO");
        }
    };
    public static final BattleParticipantAttachmentType<RenderDataParticipantAttachmentView, RenderDataParticipantAttachment> RENDER_DATA_ATTACHMENT = new BattleParticipantAttachmentType<>((participantView, attachment) -> Text.of("TODO"), (participantView, attachment) -> false, Function.identity());
    public static final BattleActionType<PlayerJoinTestBattleAction> JOIN_TEST_TYPE = new BattleActionType<>(context -> PlayerJoinTestBattleAction.CODEC);
    public static final BattleActionType<WalkBattleAction> WALK_TYPE = new BattleActionType<>(context -> WalkBattleAction.CODEC);
    public static final BattleActionType<AttackBattleAction> ATTACK_TYPE = new BattleActionType<>(context -> AttackBattleAction.CODEC);
    public static final BattleActionType<HealBattleAction> HEAL_TYPE = new BattleActionType<>(context -> HealBattleAction.CODEC);
    public static final BattleItemType<TestBattleItem> TEST_BATTLE_ITEM_TYPE = new BattleItemType<>(context -> TestBattleItem.CODEC, (s0, s1) -> {
        if (s0.item() instanceof TestBattleItem && s1.item() instanceof TestBattleItem) {
            return Optional.of(new BattleItemStack(s0.item(), s0.count() + s1.count()));
        }
        return Optional.empty();
    }, (i0, i1) -> true);

    @Override
    public void onInitialize() {
        Registry.register(Tbcexv4Registries.BattleActionTypes.REGISTRY, Tbcexv4.id("join_test"), JOIN_TEST_TYPE);
        Registry.register(Tbcexv4Registries.BattleActionTypes.REGISTRY, Tbcexv4.id("walk"), WALK_TYPE);
        Registry.register(Tbcexv4Registries.BattleActionTypes.REGISTRY, Tbcexv4.id("attack"), ATTACK_TYPE);
        Registry.register(Tbcexv4Registries.BattleActionTypes.REGISTRY, Tbcexv4.id("heal"), HEAL_TYPE);
        Registry.register(Tbcexv4Registries.ItemTypes.REGISTRY, Tbcexv4.id("test"), TEST_BATTLE_ITEM_TYPE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tbcexv4test").then(CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                final ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                final ServerWorld world = (ServerWorld) player.getWorld();
                final ServerBattleWorld battleWorld = (ServerBattleWorld) world.getServer().getWorld(Tbcexv4.battleWorldKey(player.getWorld().getRegistryKey()));
                final int rad = 2;
                final int size = (rad * 2 + 1) * 16;
                final Optional<Battle> opt = battleWorld.battleManager().createBattle(size, size, size, Tbcexv4Registries.TurnManagerTypes.ENERGY_IN_ORDER_TURN_MANAGER_TYPE, Unit.INSTANCE);
                if (opt.isEmpty()) {
                    return 1;
                }
                final Battle battle = opt.get();
                final ChunkSectionPos center = ChunkSectionPos.from(player.getBlockPos());
                final List<PlayerJoinTestBattleAction.Entry> entries = new ArrayList<>();
                for (final SheepEntity friend : world.getEntitiesByType(EntityType.SHEEP, player.getBoundingBox().expand(rad * 12), i -> true)) {
                    final int x = (friend.getBlockX() - center.getMinX()) + rad * 16;
                    final int y = (friend.getBlockY() - center.getMinY()) + rad * 16;
                    final int z = (friend.getBlockZ() - center.getMinZ()) + rad * 16;
                    entries.add(new PlayerJoinTestBattleAction.Entry(new BattlePos(x, y, z), false));
                }
                for (final PigEntity enemy : world.getEntitiesByType(EntityType.PIG, player.getBoundingBox().expand(rad * 12), i -> true)) {
                    final int x = (enemy.getBlockX() - center.getMinX()) + rad * 16;
                    final int y = (enemy.getBlockY() - center.getMinY()) + rad * 16;
                    final int z = (enemy.getBlockZ() - center.getMinZ()) + rad * 16;
                    entries.add(new PlayerJoinTestBattleAction.Entry(new BattlePos(x, y, z), true));
                }
                battle.pushAction(new SetupEnvironmentBattleAction(BattleEnvironmentInitialState.of(world, center.add(-rad, -rad, -rad), center.add(rad, rad, rad))));
                final int x = (player.getBlockX() - center.getMinX()) + rad * 16;
                final int y = (player.getBlockY() - center.getMinY()) + rad * 16;
                final int z = (player.getBlockZ() - center.getMinZ()) + rad * 16;
                battle.pushAction(new PlayerJoinTestBattleAction(player.getUuid(), x, y, z, entries));
                battle.pushAction(new StartBattleAction());
                Tbcexv4Api.watch(player, battle);
                return 0;
            })));
            dispatcher.register(CommandManager.literal("tbcexv4Noop").then(CommandManager.argument("player", EntityArgumentType.player()).executes(new Command<ServerCommandSource>() {
                @Override
                public int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                    final PlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                    final Optional<BattleHandle> watching = Tbcexv4Api.watching((ServerPlayerEntity) player);
                    if (watching.isEmpty()) {
                        return 1;
                    }
                    if (!(player.getWorld() instanceof final ServerBattleWorld world)) {
                        return 1;
                    }
                    final Optional<? extends Battle> battle = world.battleManager().getOrLoadBattle(watching.get());
                    if (battle.isEmpty()) {
                        return 1;
                    }
                    final TurnManager manager = battle.get().turnManager();
                    final BattleAction action = manager.skipTurn(new BattleParticipantHandle(player.getUuid()));
                    battle.get().pushAction(action);
                    return 0;
                }
            })));
        });
        NeighbourFinder.GATHER_EVENT.register(new NeighbourFinder.Gather() {
            @Override
            public void gather(final BattleParticipantView participant, final Consumer<NeighbourFinder> consumer) {
                consumer.accept(new WalkNeighbourFinder());
                consumer.accept(new JumpNeighbourFinder());
                consumer.accept(new FallNeighbourFinder());
            }
        });
        Tbcexv4Registries.DefaultPlans.register((participant, consumer) -> {
            final BattlePos pos = participant.pos();
            final BattleStateView battleState = participant.battleState();
            final Pather.Paths cache;
            final List<NeighbourFinder> finders = new ArrayList<>();
            NeighbourFinder.GATHER_EVENT.invoker().gather(participant, finders::add);
            if (!finders.isEmpty()) {
                final Pather pather = Pather.create(finders.toArray(NeighbourFinder[]::new), Pather.PathingNode::onFloor);
                cache = pather.compute(new Pather.PathingNode((Pather.PathingNode) null, 0, 0, Movement.WALK, true, pos.x(), pos.y(), pos.z()), PatherOptions.NONE, participant);
            } else {
                return;
            }
            if (cache.all().findAny().isPresent()) {
                final Pather.Paths paths = cache;
                final Function<Pather.PathNode, PathTarget> factory = p -> new PathTarget(p, paths.isTerminal(p));
                Plans.acceptPlayer(participant, new SingleTargetPlan<>(new TargetChooser<PathTarget>() {
                    @Override
                    public TargetType<PathTarget> type() {
                        return Tbcexv4Registries.TargetTypes.PATH_TARGET;
                    }

                    @Override
                    public Iterator<? extends PathTarget> all() {
                        return paths.all().map(factory).iterator();
                    }

                    @Override
                    public PathTarget choose(final double temperature, final Random random, final BattleTransactionContext context) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public double weight() {
                        return 1;
                    }
                }, target -> List.of(new WalkBattleAction(participant.handle(), target.node())), WALK_PLAN_TYPE), consumer);
            }
            final List<BattleParticipantView> targets = new ArrayList<>();
            for (final BattleParticipantHandle handle : battleState.participants()) {
                final BattleParticipantView p = battleState.participant(handle);
                if (battleState.relation(p.team(), participant.team()) == BattleParticipantTeamRelation.HOSTILE) {
                    targets.add(p);
                }
            }
            final Optional<Plan> plan = Plans.pathPrefix(battleState, cache, node -> {
                Set<BattleParticipantHandle> attackable = null;
                for (final BattleParticipantView target : targets) {
                    final double distanced = BattleParticipantBounds.distance2(participant.bounds(), node.pos(), target.bounds(), target.pos());
                    if (distanced < 3) {
                        if (attackable == null) {
                            attackable = new ObjectOpenHashSet<>(3);
                            attackable.add(target.handle());
                        } else {
                            attackable.add(target.handle());
                        }
                    }
                }
                return attackable;
            }, (node, handles) -> {
                final Optional<TargetChooser<ParticipantTarget>> chooser = TargetChoosers.hurtParticipant(participant, handles::contains, 3);
                if (chooser.isPresent()) {
                    return new SingleTargetPlan<>(chooser.get(), (FunctionType<ParticipantTarget, List<BattleAction>>) participantTarget -> List.of(new WalkBattleAction(participant.handle(), node), new AttackBattleAction(participant.handle(), participantTarget.participant())), PLAN_TYPE);
                }
                return Plan.EMPTY_PLAN;
            }, value -> 5 / battleState.participant(value).health(), 1, PLAN_TYPE);
            if (plan.isPresent()) {
                consumer.accept(plan.get());
            }
            final Optional<TargetChooser<ParticipantTarget>> chooser = TargetChoosers.hurtParticipant(participant, s -> {
                final BattleParticipantView target = battleState.participant(s);
                final double distanced = BattleParticipantBounds.distance2(participant.bounds(), participant.pos(), target.bounds(), target.pos());
                if (s.equals(participant.handle())) {
                    return false;
                }
                return distanced < 3;
            }, 3);
            if (chooser.isPresent()) {
                consumer.accept(new SingleTargetPlan<>(chooser.get(), (FunctionType<ParticipantTarget, List<BattleAction>>) participantTarget -> List.of(new AttackBattleAction(participant.handle(), participantTarget.participant())), PLAN_TYPE));
            }
        });
        Registry.register(Tbcexv4Registries.DamageTypes.REGISTRY, Tbcexv4.id("physical"), new DamageType(Text.of("physical"), Text.of("todo"), Set.of(Tbcexv4.id("root")), Set.of()));
    }
}
