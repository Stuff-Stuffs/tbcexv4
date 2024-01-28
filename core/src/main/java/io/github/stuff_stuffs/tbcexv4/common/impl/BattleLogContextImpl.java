package io.github.stuff_stuffs.tbcexv4.common.impl;

import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BattleLogContextImpl implements BattleLogContext {
    private final BattleLogLevel level;
    private final Map<BattleTracerView.Timestamp, Text> messages = new Object2ReferenceOpenHashMap<>();
    private int indentDepth = 0;
    private boolean building = false;

    public BattleLogContextImpl(final BattleLogLevel level) {
        this.level = level;
    }

    @Override
    public void pushIndent() {
        if (building) {
            throw new RuntimeException();
        }
        indentDepth++;
    }

    @Override
    public void popIndent() {
        if (building) {
            throw new RuntimeException();
        }
        indentDepth--;
    }

    @Override
    public MessageBuilder createMessage(final BattleTracerView.Timestamp timestamp) {
        if (building) {
            throw new RuntimeException();
        }
        building = true;
        return new MessageBuilderImpl(this, timestamp);
    }

    @Override
    public BattleLogLevel level() {
        return level;
    }

    public Text at(final BattleTracerView.Timestamp timestamp) {
        return messages.get(timestamp);
    }

    private void append(final BattleTracerView.Timestamp timestamp, final List<Text> texts) {
        final int size = texts.size();
        final Text[] arr = new Text[size + 1];
        for (int i = 0; i < size; i++) {
            arr[i + 1] = texts.get(i);
        }
        arr[0] = Text.of(" ".repeat(indentDepth));
        final Text concat = Tbcexv4Util.concat(arr);
        final Text old = messages.put(timestamp, concat);
        if (old != null) {
            messages.put(timestamp, Tbcexv4Util.concat(old, Text.of("\n"), Text.of(" ".repeat(indentDepth)), concat));
        }
    }

    private static final class MessageBuilderImpl implements MessageBuilder {
        private final BattleLogContextImpl parent;
        private final BattleTracerView.Timestamp timestamp;
        private final List<Text> texts = new ArrayList<>();
        private boolean open = true;

        private MessageBuilderImpl(final BattleLogContextImpl parent, final BattleTracerView.Timestamp timestamp) {
            this.parent = parent;
            this.timestamp = timestamp;
        }

        private void checkClosed() {
            if (!open) {
                throw new RuntimeException();
            }
        }

        @Override
        public void append(final String s) {
            checkClosed();
            texts.add(Text.of(s));
        }

        @Override
        public void appendTranslatable(final String s) {
            checkClosed();
            texts.add(Text.translatable(s));
        }

        @Override
        public void append(final Object o) {
            checkClosed();
            texts.add(Text.of(Objects.toString(o)));
        }

        @Override
        public void appendTranslatable(final String s, final Object... args) {
            checkClosed();
            texts.add(Text.translatable(s, args));
        }

        @Override
        public void close() {
            checkClosed();
            open = false;
            parent.building = false;
            parent.append(timestamp, texts);
        }
    }
}
