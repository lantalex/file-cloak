package io.github.lantalex.cloaked;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

class SharedFileChannel {

    private final AtomicInteger refCounter = new AtomicInteger(1);
    private final FileChannel fileChannel;

    private SharedFileChannel(FileChannel fileChannel) {
        this.fileChannel = fileChannel;
    }

    public static SharedFileChannel create(FileChannel fileChannel) {
        return new SharedFileChannel(fileChannel);
    }

    public FileChannel get() {
        refCounter.incrementAndGet();
        return fileChannel;
    }

    public void release() {
        if (refCounter.decrementAndGet() == 0) {
            try {
                //System.gc();
                fileChannel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
