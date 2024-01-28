package io.github.stuff_stuffs.tbcexv4util.log;

import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;

public interface BattleLogContext {
    BattleLogContext DISABLED = new BattleLogContext() {
        @Override
        public void pushIndent() {

        }

        @Override
        public void popIndent() {

        }

        @Override
        public MessageBuilder createMessage(BattleTracerView.Timestamp timestamp) {
            return new MessageBuilder() {
                @Override
                public void append(final String s) {

                }

                @Override
                public void appendTranslatable(final String s) {

                }

                @Override
                public void append(Object o) {

                }

                @Override
                public void appendTranslatable(final String s, final Object... args) {

                }

                @Override
                public void close() {

                }
            };
        }

        @Override
        public BattleLogLevel level() {
            return BattleLogLevel.NONE;
        }
    };

    void pushIndent();

    void popIndent();

    MessageBuilder createMessage(BattleTracerView.Timestamp timestamp);

    BattleLogLevel level();

    interface MessageBuilder extends AutoCloseable {
        void append(String s);

        void appendTranslatable(String s);

        void append(Object o);

        void appendTranslatable(String s, Object... args);

        @Override
        void close();
    }
}
