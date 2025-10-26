package io.github.fishstiz.packed_packs.util;

import io.github.fishstiz.fidgetz.util.text.PatternStylizer;
import io.github.fishstiz.fidgetz.util.text.TextStylizer;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.util.text.GroupCloseStylizer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AliasRegex {
    public static final String REGEX_PREFIX = "regex:";
    public static final Pattern REGEX_PREFIX_PATTERN = Pattern.compile("^" + Pattern.quote(REGEX_PREFIX));

    private AliasRegex() {
    }

    public static boolean isRegex(String alias) {
        return alias.startsWith(REGEX_PREFIX);
    }

    public static Map<Pattern, String> findPatternsFromKeys(Map<String, String> aliasMap) {
        Map<Pattern, String> patterns = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<String, String> aliasEntry : aliasMap.entrySet()) {
            String alias = aliasEntry.getKey();
            String canonicalId = aliasEntry.getValue();

            if (isRegex(alias)) {
                try {
                    patterns.put(Pattern.compile(alias.replaceFirst(REGEX_PREFIX_PATTERN.pattern(), "")), canonicalId);
                } catch (PatternSyntaxException e) {
                    PackedPacks.LOGGER.error("[packed_packs] Invalid regex syntax '{}' for id '{}'", alias, canonicalId, e);
                }
            }
        }
        return Collections.unmodifiableMap(patterns);
    }

    public static @Nullable String resolveCanonicalId(String id, Map<Pattern, String> patternMap) {
        for (Map.Entry<Pattern, String> patternEntry : patternMap.entrySet()) {
            if (patternEntry.getKey().matcher(id).matches()) {
                return patternEntry.getValue();
            }
        }
        return null;
    }

    public static TextStylizer createStylizer(Pattern pattern, int color) {
        return new PatternStylizer(AliasRegex::isRegex, pattern, color);
    }

    public static TextStylizer createClosingStylizer(char open, char close, int color) {
        return new GroupCloseStylizer(AliasRegex::isRegex, open, close, color);
    }
}
