package io.github.fishstiz.packed_packs.gui.layouts.pack;

import io.github.fishstiz.fidgetz.gui.components.EditableList;
import io.github.fishstiz.fidgetz.gui.components.FidgetzButton;
import io.github.fishstiz.fidgetz.gui.components.FidgetzText;
import io.github.fishstiz.fidgetz.gui.components.RenderableRectWidget;
import io.github.fishstiz.fidgetz.gui.layouts.FlexLayout;
import io.github.fishstiz.packed_packs.config.DevConfig;
import io.github.fishstiz.packed_packs.util.AliasRegex;
import io.github.fishstiz.packed_packs.pack.PackAssetManager;
import io.github.fishstiz.packed_packs.util.constants.GuiConstants;
import io.github.fishstiz.packed_packs.util.constants.Theme;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.server.packs.repository.Pack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class PackAliasLayout implements Layout {
    private static final int LIST_WIDTH = 296;
    private static final int LIST_HEIGHT = 128;
    private static final int MAX_ALIAS_LENGTH = 255;
    private final Pattern unescapedForwardSlash = Pattern.compile("(?<!\\\\)/");
    private final Pattern unclosedParenthesis = Pattern.compile("(?<!\\\\)\\((?=[^)]*$)");
    private final Pattern unclosedBracket = Pattern.compile("(?<!\\\\)\\[(?![^]\\[]*])");
    private final Pattern danglingBackslash = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\\\$");
    private final Pattern boundary = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})[$^]|(\\\\[bB])");
    private final Pattern anyChar = Pattern.compile("\\\\[wWdDsS]|(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\.");
    private final Pattern escapedChar = Pattern.compile("(\\\\u[0-9a-fA-F]{4})|(\\\\([0-3][0-7]{2}|[0-7]{1,2}))|(\\\\[^wWdDsS])");
    private final Pattern quantifier = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})(?<=(?<!\\\\)(\\\\\\\\){0,128})(\\{\\d+,?\\d*}|[+*?])");
    private final Pattern openCharSet = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\[\\^?(?=[^]]*(?<=(?<!\\\\)(\\\\\\\\){0,128})])");
    private final Pattern openCaptureGroup = Pattern.compile("(?<=(?<!\\\\)(\\\\\\\\){0,128})\\((\\?(<\\w+>|:|!|=|<!|<=))?(?=.*(?<=(?<!\\\\)(\\\\\\\\){0,128})\\))");
    private final Pattern alternation = Pattern.compile("(?<!(?<!\\\\)(\\\\\\\\){0,128}\\[[^]]{0,255})(?<=(?<!\\\\)(\\\\\\\\){0,128})\\|");
    private final DevConfig.Packs config;
    private final PackAssetManager assetManager;
    private EditableList<String> aliases;
    private LinearLayout layout;
    private Pack pack;

    public PackAliasLayout(DevConfig.Packs config, PackAssetManager assetManager) {
        this.config = config;
        this.assetManager = assetManager;
        this.layout = LinearLayout.vertical().spacing(GuiConstants.SPACING);
    }

    public void saveAliases() {
        if (this.pack != null && this.aliases != null) {
            this.config.setAliases(this.pack.getId(), this.aliases.extractItems());
            this.pack = null;
            this.aliases = null;
        }
    }

    public void editAliases(Pack pack, Runnable onClose) {
        this.pack = pack;

        this.aliases = EditableList.builder(this.config.getAliases(pack.getId()))
                .setDimensions(LIST_WIDTH, LIST_HEIGHT)
                .setMaxTextLength(MAX_ALIAS_LENGTH)
                .setSaveValidator(value -> !Objects.equals(value, pack.getId()))
                .addTextStylizer(AliasRegex.createStylizer(this.unescapedForwardSlash, Theme.RED_700.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.unclosedParenthesis, Theme.RED_700.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.unclosedBracket, Theme.RED_700.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.danglingBackslash, Theme.RED_700.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.anyChar, Theme.ORANGE_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.escapedChar, Theme.MAGENTA_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.boundary, Theme.BROWN_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.quantifier, Theme.BLUE_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.openCharSet, Theme.YELLOW_500.getARGB()))
                .addTextStylizer(AliasRegex.createClosingStylizer('[', ']', Theme.YELLOW_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.openCaptureGroup, Theme.GREEN_500.getARGB()))
                .addTextStylizer(AliasRegex.createClosingStylizer('(', ')', Theme.GREEN_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(this.alternation, Theme.GREEN_500.getARGB()))
                .addTextStylizer(AliasRegex.createStylizer(AliasRegex.REGEX_PREFIX_PATTERN, Theme.GRAY_500.getARGB()))
                .build();

        FidgetzText<Void> name = FidgetzText.<Void>builder()
                .setMessage(pack.getTitle())
                .setOffsetY(1)
                .build();
        RenderableRectWidget<Void> icon = RenderableRectWidget.<Void>builder(this.assetManager.getIcon(pack))
                .makeSquare()
                .build();
        FidgetzButton<Void> closeButton = FidgetzButton.<Void>builder()
                .makeSquare()
                .setSprite(GuiConstants.CROSS_SPRITE)
                .setOnPress(onClose)
                .build();

        final FlexLayout titleLayout = FlexLayout.horizontal(this.aliases::getWidth).spacing(GuiConstants.SPACING);
        titleLayout.addChild(icon);
        titleLayout.addFlexChild(name);
        titleLayout.addChild(closeButton);

        this.layout = LinearLayout.vertical().spacing(GuiConstants.SPACING);
        this.layout.addChild(titleLayout);
        this.layout.addChild(this.aliases);
    }

    @Override
    public void setX(int x) {
        this.layout.setX(x);
    }

    @Override
    public void setY(int y) {
        this.layout.setY(y);
    }

    @Override
    public int getX() {
        return this.layout.getX();
    }

    @Override
    public int getY() {
        return this.layout.getY();
    }

    @Override
    public int getWidth() {
        return this.layout.getWidth();
    }

    @Override
    public int getHeight() {
        return this.layout.getHeight();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        this.layout.visitChildren(visitor);
    }

    @Override
    public void arrangeElements() {
        this.layout.arrangeElements();
    }
}
