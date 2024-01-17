package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

public interface Plan {
    Set<TargetType<?>> targetTypes();

    <T extends Target> TargetChooser<T> ofType(TargetType<T> type);

    Plan addTarget(Target target);

    boolean canBuild();

    List<BattleAction> build();

    PlanType type();

    Plan EMPTY_PLAN = new Plan() {
        @Override
        public Set<TargetType<?>> targetTypes() {
            return Set.of();
        }

        @Override
        public <T extends Target> TargetChooser<T> ofType(final TargetType<T> type) {
            throw new IllegalArgumentException();
        }

        @Override
        public Plan addTarget(final Target target) {
            throw new IllegalArgumentException();
        }

        @Override
        public boolean canBuild() {
            return false;
        }

        @Override
        public List<BattleAction> build() {
            throw new IllegalArgumentException();
        }

        @Override
        public PlanType type() {
            return new PlanType() {
                @Override
                public Text name() {
                    return Text.of("ERROR");
                }

                @Override
                public Text description() {
                    return Text.of("You should not be able to see this!");
                }
            };
        }
    };
}
