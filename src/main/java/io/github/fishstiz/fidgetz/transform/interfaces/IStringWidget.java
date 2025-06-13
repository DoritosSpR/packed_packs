package io.github.fishstiz.fidgetz.transform.interfaces;

public interface IStringWidget extends ITextRenderer {
    void fidgetz$setAlignX(float horizontalAlignment);

    void fidgetz$setOffsetY(int offsetY);

    void fidgetz$setShadow(boolean shadow);

    boolean fidgetz$hasShadow();
}
