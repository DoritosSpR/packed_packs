package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.compat.ModAdditions;
import io.github.fishstiz.packed_packs.util.lang.CollectionsUtil;
import net.minecraft.Util;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.fishstiz.packed_packs.util.PackUtil.hasFolderConfig;
import static io.github.fishstiz.packed_packs.util.PackUtil.hasMcmeta;
import static java.nio.file.Files.isDirectory;
import static net.minecraft.Util.backgroundExecutor;

/**
 * Migrated from {@link java.nio.file.WatchService} due to registered subdirectories locking parent directory on Windows.
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-6972833">JDK-6972833</a>
 */
public class PackWatcher implements AutoCloseable {
    private static final long POLL_INTERVAL_MS = 1000;
    private static final long DEBOUNCED_CHANGE_DELAY_MS = 1000;
    private final FileAlterationMonitor monitor = new FileAlterationMonitor(POLL_INTERVAL_MS);
    private final Executor callbackExecutor;
    private final PollingDebouncer<Path> onChangeCallback;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile long lastPollTime;

    public PackWatcher(Collection<Path> directories, Runnable onChangeCallback, Executor callbackExecutor) {
        this.monitor.setThreadFactory(r -> {
            throw new IllegalStateException("PackWatcher monitor should not be creating a new thread.");
        });
        this.callbackExecutor = callbackExecutor;
        this.onChangeCallback = this.debounceCallback(onChangeCallback);
        CollectionsUtil.forEachDistinct(directories, this::addDirectory);
    }

    private void addDirectory(Path directory) {
        if (!isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;

        FileAlterationObserver observer = new FileAlterationObserver(directory.toFile());
        Path normalizedPath = directory.toAbsolutePath().normalize();
        observer.addListener(new DirectoryListener(normalizedPath));

        backgroundExecutor().execute(() -> {
            if (this.closed.get()) return;
            try {
                observer.initialize();
                if (!this.closed.get()) {
                    this.monitor.addObserver(observer);
                } else {
                    observer.destroy();
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to initialize observer for directory {}.", normalizedPath, e);
            }
        });
    }

    public void poll() {
        backgroundExecutor().execute(() -> {
            long currentTime = Util.getMillis();
            if (currentTime - this.lastPollTime >= this.monitor.getInterval()) {
                this.monitor.getObservers().forEach(FileAlterationObserver::checkAndNotify);
                this.lastPollTime = currentTime;
            }
            this.onChangeCallback.poll();
        });
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }

        backgroundExecutor().execute(() -> {
            for (FileAlterationObserver observer : this.monitor.getObservers()) {
                try {
                    observer.destroy();
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Error occurred while closing observer for {}", observer.getDirectory(), e);
                }
            }
            this.onChangeCallback.abort();
        });
    }

    private PollingDebouncer<Path> debounceCallback(Runnable callback) {
        return new PollingDebouncer<>(path -> {
            if (!this.closed.get() && !ModAdditions.discontinueChanges(path)) {
                callback.run();
            }
        }, DEBOUNCED_CHANGE_DELAY_MS);
    }

    class DirectoryListener extends FileAlterationListenerAdaptor {
        private static final int DIRECTORY_PACK_DEPTH = 1;
        private static final int PACK_CONTENTS_DEPTH = 2;
        private static final int NESTED_PACK_DEPTH = 3;
        private final Path root;

        DirectoryListener(Path root) {
            this.root = root;
        }

        @Override
        public void onDirectoryCreate(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onDirectoryChange(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onDirectoryDelete(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onFileCreate(File file) {
            this.onEvent(file);
        }

        @Override
        public void onFileChange(File file) {
            this.onEvent(file);
        }

        @Override
        public void onFileDelete(File file) {
            this.onEvent(file);
        }

        private void onEvent(File file) {
            Path path = file.toPath();
            int depth = this.root.relativize(path.toAbsolutePath().normalize()).getNameCount();

            if (switch (depth) {
                case DIRECTORY_PACK_DEPTH ->
                        !isDirectory(path, LinkOption.NOFOLLOW_LINKS) || hasMcmeta(path) || hasFolderConfig(path);
                case PACK_CONTENTS_DEPTH -> hasMcmeta(path.getParent()) || hasFolderConfig(path.getParent());
                case NESTED_PACK_DEPTH -> hasFolderConfig(path.getParent().getParent());
                default -> false;
            }) {
                PackWatcher.this.callbackExecutor.execute(() -> PackWatcher.this.onChangeCallback.accept(path));
            }
        }
    }
}
