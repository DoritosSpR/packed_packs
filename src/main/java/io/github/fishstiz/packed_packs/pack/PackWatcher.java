package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.packed_packs.util.PackUtil;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PackWatcher implements AutoCloseable {
    private static final int WATCH_DEPTH = 2;
    private final WatchService watcher;
    private final Set<Path> roots = ConcurrentHashMap.newKeySet();

    public PackWatcher() throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
    }

    public void addRoot(Path rootPath) throws IOException {
        if (Files.notExists(rootPath) || !Files.isDirectory(rootPath)) {
            return;
        }

        this.roots.add(rootPath);
        this.watchDirRecursive(rootPath, 0);
    }

    private void watchDirRecursive(Path dir, int currentDepth) throws IOException {
        if (currentDepth > WATCH_DEPTH || (currentDepth > 1 && !PackUtil.hasFolderConfig(dir.getParent()))) {
            return;
        }

        this.watchDir(dir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                    this.watchDirRecursive(entry, currentDepth + 1);
                }
            }
        }
    }

    private void watchDir(Path dir) throws IOException {
        dir.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    public boolean pollForChanges() throws IOException {
        boolean changed = false;
        WatchKey key;

        while ((key = this.watcher.poll()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                changed = true;
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path parent = (Path) key.watchable();
                    Path child = parent.resolve((Path) event.context());
                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) && (PackUtil.hasFolderConfig(child) || this.roots.contains(parent))) {
                        int depth = getDepth(child);
                        this.watchDirRecursive(child, depth);
                    }
                }
            }

            key.reset();
        }
        return changed;
    }

    private int getDepth(Path path) {
        Path absolutePath = path.toAbsolutePath().normalize();

        for (Path root : roots) {
            Path absoluteRoot = root.toAbsolutePath().normalize();

            if (absolutePath.startsWith(absoluteRoot)) {
                return absoluteRoot.relativize(absolutePath).getNameCount();
            }
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        this.watcher.close();
    }
}