package com.runehub.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;

public final class MainFile {
    private final int id;
    private final RandomAccessFile data;
    private final RandomAccessFile index;
    private final byte[] readCachedBuffer;
    private final boolean newProtocol;

    protected MainFile(int id, RandomAccessFile data, RandomAccessFile index, byte[] readCachedBuffer, boolean newProtocol) throws IOException {
        this.id = id;
        this.data = data;
        this.index = index;
        this.readCachedBuffer = readCachedBuffer;
        this.newProtocol = newProtocol;
    }

    public Archive getArchive(int id) {
        return this.getArchive(id, (int[]) null);
    }

    public Archive getArchive(int id, int[] keys) {
        byte[] data = this.getArchiveData(id);
        return data == null ? null : new Archive(id, data, keys);
    }

    public byte[] getArchiveData(int id) {
        RandomAccessFile var2 = this.data;
        synchronized (this.data) {
            try {
                if (this.index.length() < (long) (6 * id + 6)) {
                    return null;
                } else {
                    this.index.seek((long) (6 * id));
                    this.index.read(this.readCachedBuffer, 0, 6);
                    int e = (this.readCachedBuffer[2] & 255) + ((255 & this.readCachedBuffer[0]) << 16) + (this.readCachedBuffer[1] << 8 & '\uff00');
                    int sector = ((this.readCachedBuffer[3] & 255) << 16) - (-('\uff00' & this.readCachedBuffer[4] << 8) - (this.readCachedBuffer[5] & 255));
                    if (e < 0 || e > 1000000) {
                        return null;
                    } else if (sector <= 0 || this.data.length() / 520L < (long) sector) {
                        return null;
                    } else {
                        byte[] archive = new byte[e];
                        int readBytesCount = 0;
                        int nextSector;
                        for (int part = 0; e > readBytesCount; sector = nextSector) {
                            if (sector == 0) {
                                return null;
                            }
                            this.data.seek((long) (520 * sector));
                            int dataBlockSize = e - readBytesCount;
                            byte headerSize;
                            int currentIndex;
                            int currentPart;
                            int currentArchive;
                            if ('\uffff' < id && this.newProtocol) {
                                headerSize = 10;
                                if (dataBlockSize > 510) {
                                    dataBlockSize = 510;
                                }
                                this.data.read(this.readCachedBuffer, 0, headerSize + dataBlockSize);
                                currentArchive = ((this.readCachedBuffer[1] & 255) << 16) + ((this.readCachedBuffer[0] & 255) << 24) + (('\uff00' & this.readCachedBuffer[2] << 8) - -(this.readCachedBuffer[3] & 255));
                                currentPart = ((this.readCachedBuffer[4] & 255) << 8) + (255 & this.readCachedBuffer[5]);
                                nextSector = (this.readCachedBuffer[8] & 255) + ('\uff00' & this.readCachedBuffer[7] << 8) + ((255 & this.readCachedBuffer[6]) << 16);
                                currentIndex = this.readCachedBuffer[9] & 255;
                            } else {
                                headerSize = 8;
                                if (dataBlockSize > 512) {
                                    dataBlockSize = 512;
                                }
                                this.data.read(this.readCachedBuffer, 0, headerSize + dataBlockSize);
                                currentArchive = (255 & this.readCachedBuffer[1]) + ('\uff00' & this.readCachedBuffer[0] << 8);
                                currentPart = ((this.readCachedBuffer[2] & 255) << 8) + (255 & this.readCachedBuffer[3]);
                                nextSector = (this.readCachedBuffer[6] & 255) + ('\uff00' & this.readCachedBuffer[5] << 8) + ((255 & this.readCachedBuffer[4]) << 16);
                                currentIndex = this.readCachedBuffer[7] & 255;
                            }
                            if (this.newProtocol && id != currentArchive || currentPart != part || this.id != currentIndex) {
                                return null;
                            }
                            if (nextSector < 0 || this.data.length() / 520L < (long) nextSector) {
                                return null;
                            }
                            for (int index = headerSize; dataBlockSize + headerSize > index; ++index) {
                                archive[readBytesCount++] = this.readCachedBuffer[index];
                            }
                            ++part;
                        }
                        byte[] var10000 = archive;
                        return var10000;
                    }
                }
            } catch (Exception var15) {
                var15.printStackTrace();
                return null;
            }
        }
    }

    public boolean putArchive(Archive archive) {
        return this.putArchiveData(archive.getId(), archive.getData());
    }

    public boolean putArchiveData(int id, byte[] archive) {
        boolean done = this.putArchiveData(id, archive, true);
        if (!done) {
            done = this.putArchiveData(id, archive, false);
        }
        return done;
    }

    public boolean putArchiveData(int id, byte[] archive, boolean exists) {
        RandomAccessFile var4 = this.data;
        synchronized (this.data) {
            try {
                int e;
                if (!exists) {
                    e = (int) ((this.data.length() + 519L) / 520L);
                    if (e == 0) {
                        e = 1;
                    }
                } else {
                    if ((long) (6 * id + 6) > this.index.length()) {
                        return false;
                    }
                    this.index.seek((long) (id * 6));
                    this.index.read(this.readCachedBuffer, 0, 6);
                    e = (this.readCachedBuffer[5] & 255) + ((this.readCachedBuffer[4] & 255) << 8) + (this.readCachedBuffer[3] << 16 & 16711680);
                    if (e <= 0 || (long) e > this.data.length() / 520L) {
                        return false;
                    }
                }
                this.readCachedBuffer[1] = (byte) (archive.length >> 8);
                this.readCachedBuffer[3] = (byte) (e >> 16);
                this.readCachedBuffer[2] = (byte) archive.length;
                this.readCachedBuffer[0] = (byte) (archive.length >> 16);
                this.readCachedBuffer[4] = (byte) (e >> 8);
                this.readCachedBuffer[5] = (byte) e;
                this.index.seek((long) (id * 6));
                this.index.write(this.readCachedBuffer, 0, 6);
                int dataWritten = 0;
                for (int part = 0; dataWritten < archive.length; ++part) {
                    int nextSector = 0;
                    int dataToWrite;
                    if (exists) {
                        this.data.seek((long) (e * 520));
                        this.data.read(this.readCachedBuffer, 0, 8);
                        dataToWrite = (255 & this.readCachedBuffer[1]) + ('\uff00' & this.readCachedBuffer[0] << 8);
                        int currentPart = (255 & this.readCachedBuffer[3]) + ('\uff00' & this.readCachedBuffer[2] << 8);
                        nextSector = ((255 & this.readCachedBuffer[4]) << 16) + ((255 & this.readCachedBuffer[5]) << 8) + (255 & this.readCachedBuffer[6]);
                        int currentIndexFileId = this.readCachedBuffer[7] & 255;
                        if (dataToWrite != id || part != currentPart || this.id != currentIndexFileId) {
                            return false;
                        }
                        if (nextSector < 0 || this.data.length() / 520L < (long) nextSector) {
                            return false;
                        }
                    }
                    if (nextSector == 0) {
                        exists = false;
                        nextSector = (int) ((this.data.length() + 519L) / 520L);
                        if (nextSector == 0) {
                            ++nextSector;
                        }
                        if (nextSector == e) {
                            ++nextSector;
                        }
                    }
                    this.readCachedBuffer[3] = (byte) part;
                    if (archive.length - dataWritten <= 512) {
                        nextSector = 0;
                    }
                    this.readCachedBuffer[0] = (byte) (id >> 8);
                    this.readCachedBuffer[1] = (byte) id;
                    this.readCachedBuffer[2] = (byte) (part >> 8);
                    this.readCachedBuffer[7] = (byte) this.id;
                    this.readCachedBuffer[4] = (byte) (nextSector >> 16);
                    this.readCachedBuffer[5] = (byte) (nextSector >> 8);
                    this.readCachedBuffer[6] = (byte) nextSector;
                    this.data.seek((long) (e * 520));
                    this.data.write(this.readCachedBuffer, 0, 8);
                    dataToWrite = archive.length - dataWritten;
                    if (dataToWrite > 512) {
                        dataToWrite = 512;
                    }
                    this.data.write(archive, dataWritten, dataToWrite);
                    dataWritten += dataToWrite;
                    e = nextSector;
                }
                return true;
            } catch (Exception var12) {
                var12.printStackTrace();
                return false;
            }
        }
    }

    public int getId() {
        return this.id;
    }

    public int getArchivesCount() throws IOException {
        RandomAccessFile var1 = this.index;
        synchronized (this.index) {
            return (int) (this.index.length() / 6L);
        }
    }
}
