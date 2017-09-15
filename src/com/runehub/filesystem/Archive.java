package com.runehub.filesystem;

import com.runehub.filesystem.buffer.*;
import com.runehub.filesystem.bzip2.*;
import com.runehub.filesystem.crc32.*;
import com.runehub.filesystem.gzip.*;
import com.runehub.filesystem.whirlpool.*;

public class Archive {
    private final int id;
    private int revision;
    private int compression;
    private byte[] data;
    private int[] keys;

    protected Archive(int id, byte[] archive, int[] keys) {
        this.id = id;
        this.keys = keys;
        this.decompress(archive);
    }

    public Archive(int id, int compression, int revision, byte[] data) {
        this.id = id;
        this.compression = compression;
        this.revision = revision;
        this.data = data;
    }

    public byte[] compress() {
        ByteBuffer stream = new ByteBuffer();
        stream.writeByte(this.compression);
        byte[] compressedData;
        switch (this.compression) {
            case 0:
                compressedData = this.data;
                stream.writeInt(this.data.length);
                break;
            case 1:
                compressedData = (byte[]) null;
                compressedData = BZip2Compressor.compress(this.data);
                stream.writeInt(compressedData.length);
                stream.writeInt(this.data.length);
            default:
                compressedData = GZipCompressor.compress(this.data);
                stream.writeInt(compressedData.length);
                stream.writeInt(this.data.length);
        }
        stream.writeBytes(compressedData);
        if (this.keys != null && this.keys.length == 4) {
            stream.encodeXTEA(this.keys, 5, stream.getOffset());
        }
        if (this.revision != -1) {
            stream.writeShort(this.revision);
        }
        byte[] compressed = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(compressed, 0, compressed.length);
        return compressed;
    }

    private void decompress(byte[] archive) {
        ByteBuffer stream = new ByteBuffer(archive);
        if (this.keys != null && this.keys.length == 4) {
            stream.decodeXTEA(this.keys);
        }
        this.compression = stream.readUnsignedByte();
        int compressedLength = stream.readInt();
        if (compressedLength >= 0 && compressedLength <= 1000000) {
            int length;
            switch (this.compression) {
                case 0:
                    this.data = new byte[compressedLength];
                    this.checkRevision(compressedLength, archive, stream.getOffset());
                    stream.readBytes(this.data, 0, compressedLength);
                    break;
                case 1:
                    length = stream.readInt();
                    if (length <= 0) {
                        this.data = null;
                    } else {
                        this.data = new byte[length];
                        this.checkRevision(compressedLength, archive, stream.getOffset());
                        BZip2Decompressor.decompress(this.data, archive, compressedLength, 9);
                    }
                    break;
                default:
                    length = stream.readInt();
                    if (length > 0 && length <= 1000000000) {
                        this.data = new byte[length];
                        this.checkRevision(compressedLength, archive, stream.getOffset());
                        if (!GZipDecompressor.decompress(stream, this.data)) {
                            this.data = null;
                        }
                    } else {
                        this.data = null;
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
            this.revision = stream.readUnsignedShort();
            stream.setOffset(offset);
        } else {
            this.revision = -1;
        }
    }

    public Object[] editNoRevision(byte[] data, MainFile mainFile) {
        this.data = data;
        if (this.compression == 1) {
            this.compression = 2;
        }
        byte[] compressed = this.compress();
        return !mainFile.putArchiveData(this.id, compressed) ? null : new Object[]{Integer.valueOf(CRC32HGenerator.getHash(compressed)), Whirlpool.getHash(compressed, 0, compressed.length)};
    }

    public int getId() {
        return this.id;
    }

    public byte[] getData() {
        return this.data;
    }

    public int getDecompressedLength() {
        return this.data.length;
    }

    public int getRevision() {
        return this.revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public int getCompression() {
        return this.compression;
    }

    public int[] getKeys() {
        return this.keys;
    }

    public void setKeys(int[] keys) {
        this.keys = keys;
    }
}
