package io.github.lantalex.cloaked;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class CloakedFileInputStream extends InputStream {

    private static final int DEFAULT_MAPPING_SIZE_1GB = 0x40000000;
    private static final Cleaner CLEANER = Cleaner.create();

    private final FileChannel cloakedFileChannel;
    private final Cleaner.Cleanable cleanable;
    private final long fileSize;

    private MappedByteBuffer mapping;
    private long mappingLimit;
    private long position;

    CloakedFileInputStream(SharedFileChannel cloakedSharedFileChannel) throws IOException {
        this.cloakedFileChannel = cloakedSharedFileChannel.get();
        this.fileSize = cloakedFileChannel.size();
        this.cleanable = CLEANER.register(this, cloakedSharedFileChannel::release);
    }

    @Override
    public int read() throws IOException {
        if (remaining() <= 0) {
            return -1;
        }

        if (position >= mappingLimit) {
            updateMapping();
        }

        position++;
        return mapping.get() & 0xFF;
    }

    private void updateMapping() throws IOException {
        int nextMappingSize = (int) Math.min(DEFAULT_MAPPING_SIZE_1GB, remaining());

        mapping = cloakedFileChannel.map(FileChannel.MapMode.READ_ONLY, position, nextMappingSize);
        mappingLimit = position + nextMappingSize;
    }

    @Override
    public int available() {
        final long remaining = remaining();

        return remaining <= Integer.MAX_VALUE
                ? (int) remaining
                : Integer.MAX_VALUE;
    }

    @Override
    public long skip(long n) throws IOException {

        if (n <= 0 || remaining() <= 0) {
            return 0;
        }

        if (n < mappingLimit - position) {
            position += n;
            return n;
        }

        long skip = mappingLimit - position;
        position = mappingLimit;
        updateMapping();
        return skip;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining() <= 0) {
            return -1;
        }

        if (position >= mappingLimit) {
            updateMapping();
        }

        int size = Math.min((int) (mappingLimit - position), len);

        position += size;
        mapping.get(b, off, size);
        return size;
    }

    private long remaining() {
        return fileSize - position;
    }

    @Override
    public void close() {
        mapping = null;
        cleanable.clean();
    }
}
