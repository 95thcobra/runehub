package com.runehub.filesystem;

import com.runehub.filesystem.buffer.*;
import com.runehub.filesystem.whirlpool.*;

import java.io.*;
import java.math.*;
import java.util.*;

public final class FileSystem {
    private Index[] indexes;
    private MainFile index255;
    private final String path;
    private RandomAccessFile data;
    private byte[] readCachedBuffer;
    private boolean newProtocol;

    public static FileSystem open(String path) {
        return new FileSystem(path);
    }

    private FileSystem(String path) {
        this.path = path;
        try {
            this.newProtocol = true;
            this.data = new RandomAccessFile(path + "main_file_cache.dat2", "rw");
            this.readCachedBuffer = new byte[520];
            this.index255 = new MainFile(255, this.data, new RandomAccessFile(path + "main_file_cache.idx255", "rw"), this.readCachedBuffer, newProtocol);
            int idxsCount = this.index255.getArchivesCount();
            this.indexes = new Index[idxsCount];
            for (int id = 0; id < idxsCount; ++id) {
                Index index = new Index(this.index255, new MainFile(id, this.data, new RandomAccessFile(path + "main_file_cache.idx" + id, "rw"), this
                        .readCachedBuffer, newProtocol), null);
                if (index.getTable() != null) {
                    this.indexes[id] = index;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final byte[] generateIndex255Archive255Current(BigInteger grab_server_private_exponent, BigInteger grab_server_modulus) {
        ByteBuffer stream = new ByteBuffer();
        stream.writeByte(this.getIndexes().length);
        for (int archive = 0; archive < this.getIndexes().length; ++archive) {
            if (this.getIndexes()[archive] == null) {
                stream.writeInt(0);
                stream.writeInt(0);
                stream.writeBytes(new byte[64]);
            } else {
                stream.writeInt(this.getIndexes()[archive].getCRC());
                stream.writeInt(this.getIndexes()[archive].getTable().getRevision());
                stream.writeBytes(this.getIndexes()[archive].getWhirlpool());
                int hashStream;
                if (this.getIndexes()[archive].getKeys() != null) {
                    int[] var8;
                    int var7 = (var8 = this.getIndexes()[archive].getKeys()).length;
                    for (int hash = 0; hash < var7; ++hash) {
                        hashStream = var8[hash];
                        stream.writeInt(hashStream);
                    }
                } else {
                    for (hashStream = 0; hashStream < 4; ++hashStream) {
                        stream.writeInt(0);
                    }
                }
            }
        }
        byte[] var9 = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(var9, 0, var9.length);
        ByteBuffer var10 = new ByteBuffer(65);
        var10.writeByte(0);
        var10.writeBytes(Whirlpool.getHash(var9, 0, var9.length));
        byte[] var11 = new byte[var10.getOffset()];
        var10.setOffset(0);
        var10.getBytes(var11, 0, var11.length);
        if (grab_server_private_exponent != null && grab_server_modulus != null) {
            var11 = cryptRSA(var11, grab_server_private_exponent, grab_server_modulus);
        }
        stream.writeBytes(var11);
        var9 = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(var9, 0, var9.length);
        return var9;
    }

    private byte[] cryptRSA(byte[] data, BigInteger exponent, BigInteger modulus) {
        return new BigInteger(data).modPow(exponent, modulus).toByteArray();
    }

    public byte[] generateIndex255Archive255() {
        return this.generateIndex255Archive255Current((BigInteger) null, (BigInteger) null);
    }

    public byte[] generateIndex255Archive255Outdated() {
        ByteBuffer stream = new ByteBuffer(this.indexes.length * 8);
        for (int archive = 0; archive < this.indexes.length; ++archive) {
            if (this.indexes[archive] == null) {
                stream.writeInt(0);
                stream.writeInt(0);
            } else {
                stream.writeInt(this.indexes[archive].getCRC());
                stream.writeInt(this.indexes[archive].getTable().getRevision());
            }
        }
        byte[] var3 = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(var3, 0, var3.length);
        return var3;
    }

    public Index[] getIndexes() {
        return this.indexes;
    }

    public MainFile getIndex255() {
        return this.index255;
    }

    public int addIndex(boolean named, boolean usesWhirpool, int tableCompression) throws IOException {
        int id = this.indexes.length;
        Index[] newIndexes = (Index[]) Arrays.copyOf(this.indexes, this.indexes.length + 1);
        this.resetIndex(id, newIndexes, named, usesWhirpool, tableCompression);
        this.indexes = newIndexes;
        return id;
    }

    public void resetIndex(int id, boolean named, boolean usesWhirpool, int tableCompression) throws FileNotFoundException, IOException {
        this.resetIndex(id, this.indexes, named, usesWhirpool, tableCompression);
    }

    public void resetIndex(int id, Index[] indexes, boolean named, boolean usesWhirpool, int tableCompression) throws FileNotFoundException, IOException {
        ByteBuffer stream = new ByteBuffer(4);
        stream.writeByte(5);
        stream.writeByte((named ? 1 : 0) | (usesWhirpool ? 2 : 0));
        stream.writeShort(0);
        byte[] archiveData = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(archiveData, 0, archiveData.length);
        Archive archive = new Archive(id, tableCompression, -1, archiveData);
        this.index255.putArchiveData(id, archive.compress());
        indexes[id] = new Index(this.index255, new MainFile(id, this.data, new RandomAccessFile(this.path + "main_file_cache.idx" + id, "rw"), this
                .readCachedBuffer, this.newProtocol), (int[]) null);
    }

}
