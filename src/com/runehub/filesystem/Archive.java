package com.runehub.filesystem;

import com.runehub.filesystem.buffer.*;
import com.runehub.filesystem.bzip2.*;
import com.runehub.filesystem.crc32.*;
import com.runehub.filesystem.gzip.*;
import com.runehub.filesystem.whirlpool.*;

import java.util.*;

public class Archive {
    private final int id;
    private int revision;
    private int compression;
    private byte[] data;
    private int[] keys;

    protected Archive(int id, byte[] archive, int[] keys) {
        this.id = id;
        this.keys = keys;
        decompress(archive);
    }

    public Archive(int id, int compression, int revision, byte[] data) {
        this.id = id;
        this.compression = compression;
        this.revision = revision;
        this.data = data;
    }

    public byte[] compress() {
        ByteBuffer stream = new ByteBuffer();
        stream.writeByte(compression);
        byte[] compressedData;
        switch (compression) {
            case 0:
                compressedData = data;
                stream.writeInt(data.length);
                break;
            case 1:
                compressedData = Objects.requireNonNull(BZip2Compressor.compress(data));
                stream.writeInt(compressedData.length);
                stream.writeInt(this.data.length);
            default:
                compressedData = Objects.requireNonNull(GZipCompressor.compress(data));
                stream.writeInt(compressedData.length);
                stream.writeInt(data.length);
        }
        stream.writeBytes(compressedData);
        if (keys != null && keys.length == 4)
            stream.encodeXTEA(keys, 5, stream.getOffset());
        if (revision != -1)
            stream.writeShort(this.revision);
        byte[] compressed = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(compressed, 0, compressed.length);
        return compressed;
    }

    private void decompress(byte[] archive) {
        ByteBuffer stream = new ByteBuffer(archive);
        if (keys != null && keys.length == 4)
            stream.decodeXTEA(keys);
        compression = stream.readUnsignedByte();
        int compressedLength = stream.readInt();
        if (compressedLength >= 0 && compressedLength <= 1000000) {
            int length;
            switch (compression) {
                case 0:
                    data = new byte[compressedLength];
                    checkRevision(compressedLength, archive, stream.getOffset());
                    stream.readBytes(data, 0, compressedLength);
                    break;
                case 1:
                    length = stream.readInt();
                    if (length <= 0) {
                        data = null;
                    } else {
                        data = new byte[length];
                        checkRevision(compressedLength, archive, stream.getOffset());
                        BZip2Decompressor.decompress(data, archive, compressedLength, 9);
                    }
                    break;
                default:
                    length = stream.readInt();
                    if (length > 0 && length <= 1000000000) {
                        data = new byte[length];
                        checkRevision(compressedLength, archive, stream.getOffset());
                        if (!GZipDecompressor.decompress(stream, data)) {
                            data = null;
                        }
                    } else {
                        data = null;
                    }
            }
        } else {
            throw new RuntimeException("INVALID ARCHIVE HEADER");
        }
    }

    private void checkRevision(int compressedLength, byte[] archive, int o) {
        ByteBuffer stream = new ByteBuffer(archive);
        int offset = stream.getOffset();
        if (stream.getLength() - (compressedLength + o) >= 2) {
            stream.setOffset(stream.getLength() - 2);
            revision = stream.readUnsignedShort();
            stream.setOffset(offset);
        } else {
            revision = -1;
        }
    }

    public Object[] editNoRevision(byte[] data, MainFile mainFile) {
        this.data = data;
        if (compression == 1) {
            compression = 2;
        }
        byte[] compressed = compress();
        return !mainFile.putArchiveData(id, compressed) ? null : new Object[]{CRC32HGenerator.getHash(compressed), Whirlpool.getHash(compressed, 0, compressed.length)};
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public int getDecompressedLength() {
        return data.length;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public int getCompression() {
        return compression;
    }

    public int[] getKeys() {
        return keys;
    }

    public void setKeys(int[] keys) {
        this.keys = keys;
    }
}
