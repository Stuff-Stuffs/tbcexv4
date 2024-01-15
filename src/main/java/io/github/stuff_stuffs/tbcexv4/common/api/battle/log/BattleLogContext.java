package io.github.stuff_stuffs.tbcexv4.common.api.battle.log;

import io.github.stuff_stuffs.tbcexv4.common.impl.battle.log.BattleLogContextImpl;
import net.minecraft.text.Text;

public interface BattleLogContext {
    BattleLogContext DISABLED = new BattleLogContext() {
        @Override
        public void pushIndent() {

        }

        @Override
        public void popIndent() {

        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public void accept(final Text text) {

        }
    };

    void pushIndent();

    void popIndent();

    boolean enabled();

    void accept(Text text);

    static BattleLogContext create() {
        return new BattleLogContextImpl();
    }
}
