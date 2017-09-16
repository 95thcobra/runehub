package com.runehub.filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;

public final class MainFile {
    private final int id;
    private final RandomAccessFile data;
    private final RandomAccessFile index;
    private final byte[] readCachedBuffer;

    protected MainFile(int id, RandomAccessFile data, RandomAccessFile index, byte[] readCachedBuffer) throws IOException {
        this.id = id;
        this.data = data;
        this.index = index;
        this.readCachedBuffer = readCachedBuffer;
    }

    public Archive getArchive(int id) {
        return getArchive(id, null);
    }

    public Archive getArchive(int id, int[] keys) {
        byte[] data = getArchiveData(id);
        return data == null ? null : new Archive(id, data, keys);
    }

    public byte[] getArchiveData(int id) {
        RandomAccessFile var2 = data;
        synchronized (data) {
            try {
                if (index.length() < (long) (6 * id + 6)) {
                    return null;
                } else {
                    index.seek((long) (6 * id));
                    index.read(readCachedBuffer, 0, 6);
                    int e = (readCachedBuffer[2] & 255) + ((255 & readCachedBuffer[0]) << 16) + (readCachedBuffer[1] << 8 & '\uff00');
                    int sector = ((readCachedBuffer[3] & 255) << 16) - (-('\uff00' & readCachedBuffer[4] << 8) - (readCachedBuffer[5] & 255));
                    if (e < 0 || e > 1000000) {
                        return null;
                    } else if (sector <= 0 || data.length() / 520L < (long) sector) {
                        return null;
                    } else {
                        byte[] archive = new byte[e];
                        int readBytesCount = 0;
                        int nextSector;
                        for (int part = 0; e > readBytesCount; sector = nextSector) {
                            if (sector == 0) {
                                return null;
                            }
                            data.seek((long) (520 * sector));
                            int dataBlockSize = e - readBytesCount;
                            byte headerSize;
                            int currentIndex;
                            int currentPart;
                            int currentArchive;
                            if ('\uffff' < id ) {
                                headerSize = 10;
                                if (dataBlockSize > 510) {
                                    dataBlockSize = 510;
                                }
                                data.read(readCachedBuffer, 0, headerSize + dataBlockSize);
                                currentArchive = ((readCachedBuffer[1] & 255) << 16) + ((readCachedBuffer[0] & 255) << 24) + (('\uff00' & readCachedBuffer[2] << 8) - -(readCachedBuffer[3] & 255));
                                currentPart = ((readCachedBuffer[4] & 255) << 8) + (255 & readCachedBuffer[5]);
                                nextSector = (readCachedBuffer[8] & 255) + ('\uff00' & readCachedBuffer[7] << 8) + ((255 & readCachedBuffer[6]) << 16);
                                currentIndex = readCachedBuffer[9] & 255;
                            } else {
                                headerSize = 8;
                                if (dataBlockSize > 512) {
                                    dataBlockSize = 512;
                                }
                                data.read(readCachedBuffer, 0, headerSize + dataBlockSize);
                                currentArchive = (255 & readCachedBuffer[1]) + ('\uff00' & readCachedBuffer[0] << 8);
                                currentPart = ((readCachedBuffer[2] & 255) << 8) + (255 & readCachedBuffer[3]);
                                nextSector = (readCachedBuffer[6] & 255) + ('\uff00' & readCachedBuffer[5] << 8) + ((255 & readCachedBuffer[4]) << 16);
                                currentIndex = readCachedBuffer[7] & 255;
                            }
                            if (id != currentArchive || currentPart != part || this.id != currentIndex) {
                                return null;
                            }
                            if (nextSector < 0 || this.data.length() / 520L < (long) nextSector) {
                                return null;
                            }
                            for (int index = headerSize; dataBlockSize + headerSize > index; ++index) {
                                archive[readBytesCount++] = readCachedBuffer[index];
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
        return putArchiveData(archive.getId(), archive.getData());
    }

    public boolean putArchiveData(int id, byte[] archive) {
        boolean done = putArchiveData(id, archive, true);
        if (!done) {
            done = putArchiveData(id, archive, false);
        }
        return done;
    }

    public boolean putArchiveData(int id, byte[] archive, boolean exists) {
        RandomAccessFile var4 = data;
        synchronized (data) {
            try {
                int e;
                if (!exists) {
                    e = (int) ((data.length() + 519L) / 520L);
                    if (e == 0) {
                        e = 1;
                    }
                } else {
                    if ((long) (6 * id + 6) > index.length()) {
                        return false;
                    }
                    index.seek((long) (id * 6));
                    index.read(readCachedBuffer, 0, 6);
                    e = (readCachedBuffer[5] & 255) + ((readCachedBuffer[4] & 255) << 8) + (readCachedBuffer[3] << 16 & 16711680);
                    if (e <= 0 || (long) e > this.data.length() / 520L) {
                        return false;
                    }
                }
                readCachedBuffer[1] = (byte) (archive.length >> 8);
                readCachedBuffer[3] = (byte) (e >> 16);
                readCachedBuffer[2] = (byte) archive.length;
                readCachedBuffer[0] = (byte) (archive.length >> 16);
                readCachedBuffer[4] = (byte) (e >> 8);
                readCachedBuffer[5] = (byte) e;
                index.seek((long) (id * 6));
                index.write(readCachedBuffer, 0, 6);
                int dataWritten = 0;
                for (int part = 0; dataWritten < archive.length; ++part) {
                    int nextSector = 0;
                    int dataToWrite;
                    if (exists) {
                        data.seek((long) (e * 520));
                        data.read(readCachedBuffer, 0, 8);
                        dataToWrite = (255 & readCachedBuffer[1]) + ('\uff00' & readCachedBuffer[0] << 8);
                        int currentPart = (255 & readCachedBuffer[3]) + ('\uff00' & readCachedBuffer[2] << 8);
                        nextSector = ((255 & readCachedBuffer[4]) << 16) + ((255 & readCachedBuffer[5]) << 8) + (255 & readCachedBuffer[6]);
                        int currentIndexFileId = readCachedBuffer[7] & 255;
                        if (dataToWrite != id || part != currentPart || this.id != currentIndexFileId) {
                            return false;
                        }
                        if (nextSector < 0 || data.length() / 520L < (long) nextSector) {
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
                    readCachedBuffer[3] = (byte) part;
                    if (archive.length - dataWritten <= 512) {
                        nextSector = 0;
                    }
                    readCachedBuffer[0] = (byte) (id >> 8);
                    readCachedBuffer[1] = (byte) id;
                    readCachedBuffer[2] = (byte) (part >> 8);
                    readCachedBuffer[7] = (byte) this.id;
                    readCachedBuffer[4] = (byte) (nextSector >> 16);
                    readCachedBuffer[5] = (byte) (nextSector >> 8);
                    readCachedBuffer[6] = (byte) nextSector;
                    data.seek((long) (e * 520));
                    data.write(readCachedBuffer, 0, 8);
                    dataToWrite = archive.length - dataWritten;
                    if (dataToWrite > 512) {
                        dataToWrite = 512;
                    }
                    data.write(archive, dataWritten, dataToWrite);
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
        return id;
    }

    public int getArchivesCount() throws IOException {
        RandomAccessFile var1 = index;
        synchronized (index) {
            return (int) (index.length() / 6L);
        }
    }
}
