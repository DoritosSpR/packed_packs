package io.github.fishstiz.packed_packs.compat.respackopts;

import io.gitlab.jfronny.respackopts.Respackopts;

import java.nio.file.Path;

public class RespackoptsUtil {
    /**
     * {@link io.gitlab.jfronny.respackopts.mixin.PackScreenMixin}
     *
     * @see <a href="https://git.jfronny.dev/JfMods/Respackopts/src/branch/master/common/src/client/java/io/gitlab/jfronny/respackopts/mixin/PackScreenMixin.java">
     * Respackopts watcher workaround
     * </a>
     */
    public static boolean isRespackOptsFile(Path path) {
        return path.getFileName().toString().endsWith(Respackopts.FILE_EXTENSION);
    }
}
