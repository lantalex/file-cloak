package io.github.lantalex.cloaked;

import com.sun.nio.file.ExtendedOpenOption;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class FileCloak implements AutoCloseable {

    private static final String EXTENSION = ".hidden";
    private static final Cleaner CLEANER = Cleaner.create();

    private final SharedFileChannel fileChannel;
    private final Cleaner.Cleanable cleanable;

    private FileCloak(FileChannel fileChannel) {
        final SharedFileChannel sharedFileChannel = SharedFileChannel.create(fileChannel);
        this.fileChannel = sharedFileChannel;
        this.cleanable = CLEANER.register(this, sharedFileChannel::release);
    }

    public static FileCloak create(Path pathToOriginalFile) throws IOException {

        try (FileChannel originalFileChannel = FileChannel.open(pathToOriginalFile, StandardOpenOption.READ)) {

            Path pathToFile = pathToOriginalFile.resolveSibling(pathToOriginalFile.getFileName() + EXTENSION);

            final FileChannel cloakedFileChannel = createFileChannel(pathToFile);

            copyFile(originalFileChannel, cloakedFileChannel);

            return new FileCloak(cloakedFileChannel);
        }
    }

    private static FileChannel createFileChannel(Path pathToFile) throws IOException {
        Files.deleteIfExists(pathToFile);

        final FileChannel cloakedFileChannel = FileChannel.open(pathToFile, getOpenOptions());

        if (isWindows()) {
            Files.setAttribute(pathToFile, "dos:hidden", Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);
        } else {
            Files.deleteIfExists(pathToFile);
        }

        return cloakedFileChannel;
    }

    private static void copyFile(FileChannel originalFileChannel, FileChannel cloakedFileChannel) throws IOException {
        for (long position = 0, remainingSize = originalFileChannel.size(); remainingSize > 0; ) {
            long copiedBytes = originalFileChannel.transferTo(position, remainingSize, cloakedFileChannel);
            remainingSize -= copiedBytes;
            position += copiedBytes;
        }
        cloakedFileChannel.force(true);
    }

    private static OpenOption[] getOpenOptions() {

        List<OpenOption> defaultOptions = List.of(
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.DELETE_ON_CLOSE
        );

        List<OpenOption> extraOptions = isWindows()
                ? List.of(ExtendedOpenOption.NOSHARE_READ, ExtendedOpenOption.NOSHARE_WRITE, ExtendedOpenOption.NOSHARE_DELETE)
                : List.of();

        return Stream
                .concat(defaultOptions.stream(), extraOptions.stream())
                .toArray(OpenOption[]::new);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public InputStream getInputStream() throws IOException {
        return new CloakedFileInputStream(fileChannel);
    }

    @Override
    public void close() {
        cleanable.clean();
    }
}
