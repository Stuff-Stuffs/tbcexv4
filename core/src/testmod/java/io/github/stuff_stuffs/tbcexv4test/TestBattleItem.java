package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.datafixers.FunctionType;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemRarity;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Movement;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.NeighbourFinder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.PatherOptions;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.PlanType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plans;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.SingleTargetPlan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.ParticipantTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChoosers;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class TestBattleItem implements BattleItem {
    public static final Codec<TestBattleItem> CODEC = Codec.unit(TestBattleItem::new);
    private static final PlanType HEAL_PLAN_TYPE = new PlanType() {
        @Override
        public Text name() {
            return Text.of("HEAL");
        }

        @Override
        public Text description() {
            return Text.of("TODO");
        }
    };

    @Override
    public BattleItemType<?> type() {
        return Tbcexv4Test.TEST_BATTLE_ITEM_TYPE;
    }

    @Override
    public Text name() {
        return Text.of("Test Item");
    }

    @Override
    public BattleItemRarity rarity() {
        return BattleItemRarity.of(BattleItemRarity.RarityClass.LEGENDARY, 1);
    }

    @Override
    public List<Text> description() {
        return List.of(Text.of("Lorem ipsum"));
    }

    @Override
    public void actions(final BattleParticipantView participant, final InventoryHandle handle, final Consumer<Plan> consumer) {
        BattleItem.super.actions(participant, handle, consumer);
        final BattlePos pos = participant.pos();
        final BattleStateView battleState = participant.battleState();
        final List<NeighbourFinder> finders = new ArrayList<>();
        NeighbourFinder.GATHER_EVENT.invoker().gather(participant, finders::add);
        if (!finders.isEmpty()) {
            final Pather pather = Pather.create(finders.toArray(NeighbourFinder[]::new), Pather.PathingNode::onFloor);
            final Pather.Paths cache = pather.compute(new Pather.PathingNode((Pather.PathingNode) null, 0, 0, Movement.WALK, true, pos.x(), pos.y(), pos.z()), PatherOptions.NONE, participant);
            final List<BattleParticipantView> targets = new ArrayList<>();
            for (final BattleParticipantHandle pHandle : battleState.participants()) {
                final BattleParticipantView p = battleState.participant(pHandle);
                if (battleState.relation(p.team(), participant.team()) == BattleParticipantTeamRelation.ALLY) {
                    targets.add(p);
                }
            }
            final var plan = Plans.pathPrefix(battleState, cache, node -> {
                Set<BattleParticipantHandle> attackable = null;
                for (final BattleParticipantView target : targets) {
                    BattlePos battlePos = node.pos();
                    final double distanced = BattleParticipantBounds.distance2(participant.bounds(), new BattlePos(battlePos.x(), battlePos.y(), battlePos.z()), target.bounds(), target.pos());
                    if (distanced < 2) {
                        if (attackable == null) {
                            attackable = new ObjectOpenHashSet<>(2);
                            attackable.add(target.handle());
                        } else {
                            attackable.add(target.handle());
                        }
                    }
                }
                return attackable;
            }, (node, handles) -> {
                final Optional<TargetChooser<ParticipantTarget>> chooser = TargetChoosers.helpParticipant(participant, handles::contains, 3);
                if (chooser.isPresent()) {
                    return new SingleTargetPlan<>(chooser.get(), (FunctionType<ParticipantTarget, List<BattleAction>>) participantTarget -> List.of(new WalkBattleAction(participant.handle(), node), new HealBattleAction(participant.handle(), participantTarget.participant())), HEAL_PLAN_TYPE);
                }
                return Plan.EMPTY_PLAN;
            }, value -> 5 / battleState.participant(value).health(), 1, HEAL_PLAN_TYPE);
            plan.ifPresent(consumer);
        }
    }
}
