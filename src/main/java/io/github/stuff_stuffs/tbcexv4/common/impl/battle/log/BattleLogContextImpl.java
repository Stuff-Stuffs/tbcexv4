package io.github.stuff_stuffs.tbcexv4.common.impl.battle.log;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BattleLogContextImpl implements BattleLogContext {
    private final List<Entry> entries;
    private int indentation = 0;

    public BattleLogContextImpl() {
        entries = new ArrayList<>();
    }

    @Override
    public void pushIndent() {
        indentation++;
    }

    @Override
    public void popIndent() {
        indentation = Math.max(indentation - 1, 0);
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void accept(final Text text) {
        entries.add(new Entry(text, indentation));
    }

    public List<Text> collect() {
        final List<Text> texts = new ArrayList<>(entries.size());
        for (final Entry entry : entries) {
            final Text indented = Tbcexv4Util.concat(Text.of(" ".repeat(entry.indents)), entry.text);
            texts.add(indented);
        }
        return texts;
    }

    private record Entry(Text text, int indents) {
    }
}
