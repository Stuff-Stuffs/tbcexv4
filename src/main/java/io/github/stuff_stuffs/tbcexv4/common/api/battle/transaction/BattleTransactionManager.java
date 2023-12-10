package io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction;

public interface BattleTransactionManager {
    BattleTransaction open();

    boolean isOpen();
}
