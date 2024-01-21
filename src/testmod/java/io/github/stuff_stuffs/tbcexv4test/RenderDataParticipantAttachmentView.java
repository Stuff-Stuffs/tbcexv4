package io.github.stuff_stuffs.tbcexv4test;

public interface RenderDataParticipantAttachmentView {
    Type type();

    enum Type {
        PLAYER,
        SHEEP,
        PIG;
    }
}
