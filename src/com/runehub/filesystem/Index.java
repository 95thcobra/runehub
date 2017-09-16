package com.runehub.filesystem;

import com.runehub.filesystem.buffer.*;
import com.runehub.filesystem.crc32.*;
import com.runehub.filesystem.whirlpool.*;

import java.util.*;


public final class Index {
    private final MainFile mainFile;
    private final MainFile index255;
    private ReferenceTable table;
    private byte[][][] cachedFiles;
    private int crc;
    private byte[] whirlpool;

    protected Index(MainFile index255, MainFile mainFile, int[] keys) {
        this.mainFile = mainFile;
        this.index255 = index255;
        byte[] archiveData = index255.getArchiveData(getId());
        if (archiveData != null) {
            crc = CRC32HGenerator.getHash(archiveData);
            whirlpool = Whirlpool.getHash(archiveData, 0, archiveData.length);
            Archive archive = new Archive(getId(), archiveData, keys);
            table = new ReferenceTable(archive);
            resetCachedFiles();
        }
    }

    public void resetCachedFiles() {
        cachedFiles = new byte[getLastArchiveId() + 1][][];
    }

    public int getLastFileId(int archiveId) {
        return !archiveExists(archiveId) ? -1 : table.getArchives()[archiveId].getFiles().length - 1;
    }

    public int getLastArchiveId() {
        return table.getArchives().length - 1;
    }

    public int getValidArchivesCount() {
        return table.getValidArchiveIds().length;
    }

    public int getValidFilesCount(int archiveId) {
        return !archiveExists(archiveId) ? -1 : table.getArchives()[archiveId].getValidFileIds().length;
    }

    public boolean archiveExists(int archiveId) {
        if (archiveId < 0) {
            return false;
        } else {
            ArchiveReference[] archives = table.getArchives();
            return archives.length > archiveId && archives[archiveId] != null;
        }
    }

    public boolean fileExists(int archiveId, int fileId) {
        if (!archiveExists(archiveId)) {
            return false;
        } else {
            FileReference[] files = table.getArchives()[archiveId].getFiles();
            return files.length > fileId && files[fileId] != null;
        }
    }

    public int getArchiveId(String name) {
        int nameHash = getNameHash(name);
        ArchiveReference[] archives = this.table.getArchives();
        int[] validArchiveIds = this.table.getValidArchiveIds();
        int[] valid = validArchiveIds;
        int validLength = validArchiveIds.length;
        for (int i = 0; i < validLength; ++i) {
            int archiveId = valid[i];
            if (archives[archiveId].getNameHash() == nameHash)
                return archiveId;
        }
        return -1;
    }

    public int getFileId(int archiveId, String name) {
        if (!archiveExists(archiveId))
            return -1;
        else {
            int nameHash = getNameHash(name);
            FileReference[] files = table.getArchives()[archiveId].getFiles();
            int[] validFileIds = table.getArchives()[archiveId].getValidFileIds();
            for (int i = 0; i < validFileIds.length; ++i) {
                int fileId = validFileIds[i];
                if (files[fileId].getNameHash() == nameHash)
                    return fileId;
            }
            return -1;
        }
    }

    public byte[] getFile(int archiveId) {
        return !archiveExists(archiveId) ? null : getFile(archiveId, table.getArchives()[archiveId].getValidFileIds()[0]);
    }

    public byte[] getFile(int archiveId, int fileId) {
        return getFile(archiveId, fileId, null);
    }

    public byte[] getFile(int archiveId, int fileId, int[] keys) {
        try {
            if (!fileExists(archiveId, fileId))
                return null;
            else {
                if (cachedFiles[archiveId] == null || cachedFiles[archiveId][fileId] == null)
                    cacheArchiveFiles(archiveId, keys);
                byte[] e = cachedFiles[archiveId][fileId];
                cachedFiles[archiveId][fileId] = null;
                return e;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public boolean packIndex(FileSystem originalFileSystem) {
        return packIndex(originalFileSystem, false);
    }

    public boolean packIndex(FileSystem originalFileSystem, boolean checkCRC) {
        return packIndex(getId(), originalFileSystem, checkCRC);
    }

    public boolean packIndex(int id, FileSystem originalFileSystem, boolean checkCRC) {
        Index originalIndex = originalFileSystem.getIndexes()[id];
        int[] previousLength;
        int length = (previousLength = originalIndex.table.getValidArchiveIds()).length;
        for (int i = 0; i < length; ++i) {
            int archiveId = previousLength[i];
            if ((!checkCRC || !archiveExists(archiveId) || originalIndex.table.getArchives()[archiveId].getCrc() != table.getArchives()[archiveId].getCrc()
            ) && !putArchive(id, archiveId, originalFileSystem, false, false))
                return false;
        }
        if (!rewriteTable()) {
            return false;
        } else {
            resetCachedFiles();
            return true;
        }
    }

    public boolean putArchive(int archiveId, FileSystem originalFileSystem) {
        return putArchive(getId(), archiveId, originalFileSystem, true, true);
    }

    public boolean putArchive(int archiveId, FileSystem originalFileSystem, boolean rewriteTable, boolean resetCache) {
        return putArchive(getId(), archiveId, originalFileSystem, rewriteTable, resetCache);
    }

    public boolean putArchive(int id, int archiveId, FileSystem originalFileSystem, boolean rewriteTable, boolean resetCache) {
        Index originalIndex = originalFileSystem.getIndexes()[id];
        byte[] data = originalIndex.getMainFile().getArchiveData(archiveId);
        if (data == null)
            return false;
        else {
            if (!archiveExists(archiveId))
                table.addEmptyArchiveReference(archiveId);
            ArchiveReference reference = table.getArchives()[archiveId];
            reference.updateRevision();
            ArchiveReference originalReference = originalIndex.table.getArchives()[archiveId];
            reference.copyHeader(originalReference);
            int revision = reference.getRevision();
            data[data.length - 2] = (byte) (revision >> 8);
            data[data.length - 1] = (byte) revision;
            if (!mainFile.putArchiveData(archiveId, data))
                return false;
            else if (rewriteTable && !rewriteTable())
                return false;
            else {
                if (resetCache)
                    resetCachedFiles();
                return true;
            }
        }
    }

    public boolean putFile(int archiveId, int fileId, byte[] data) {
        return putFile(archiveId, fileId, 2, data, null, true, true, -1, -1);
    }

    public boolean removeFile(int archiveId, int fileId) {
        return removeFile(archiveId, fileId, 2, null);
    }

    public boolean removeFile(int archiveId, int fileId, int compression, int[] keys) {
        if (!fileExists(archiveId, fileId))
            return false;
        else {
            cacheArchiveFiles(archiveId, keys);
            ArchiveReference reference = table.getArchives()[archiveId];
            reference.removeFileReference(fileId);
            int filesCount = getValidFilesCount(archiveId);
            byte[] archiveData;
            if (filesCount == 1)
                archiveData = getFile(archiveId, reference.getValidFileIds()[0], keys);
            else {
                int[] archive = new int[filesCount];
                ByteBuffer closedArchive = new ByteBuffer();
                int i;
                int offset;
                for (i = 0; i < filesCount; ++i) {
                    offset = reference.getValidFileIds()[i];
                    byte[] fileData = Objects.requireNonNull(getFile(archiveId, offset, keys));
                    archive[i] = fileData.length;
                    closedArchive.writeBytes(fileData);
                }
                for (i = 0; i < archive.length; ++i) {
                    offset = archive[i];
                    if (i != 0)
                        offset -= archive[i - 1];
                    closedArchive.writeInt(offset);
                }
                closedArchive.writeByte(1);
                archiveData = new byte[closedArchive.getOffset()];
                closedArchive.setOffset(0);
                closedArchive.getBytes(archiveData, 0, archiveData.length);
            }
            reference.updateRevision();
            Archive archive = new Archive(archiveId, compression, reference.getRevision(), archiveData);
            byte[] compressed = archive.compress();
            reference.setCrc(CRC32HGenerator.getHash(compressed, 0, compressed.length - 2));
            reference.setWhirpool(Whirlpool.getHash(compressed, 0, compressed.length - 2));
            if (!mainFile.putArchiveData(archiveId, compressed))
                return false;
            else if (!rewriteTable())
                return false;
            else {
                resetCachedFiles();
                return true;
            }
        }
    }

    public boolean putFile(int archiveId, int fileId, int compression, byte[] data, int[] keys, boolean rewriteTable, boolean resetCache, int archiveName,
                           int fileName) {
        if (!archiveExists(archiveId)) {
            table.addEmptyArchiveReference(archiveId);
            resetCachedFiles();
            cachedFiles[archiveId] = new byte[1][];
        } else
            cacheArchiveFiles(archiveId, keys);

        ArchiveReference reference = table.getArchives()[archiveId];
        if (!fileExists(archiveId, fileId))
            reference.addEmptyFileReference(fileId);
        reference.sortFiles();
        int filesCount = getValidFilesCount(archiveId);
        byte[] archiveData;
        if (filesCount == 1)
            archiveData = data;
        else {
            int[] archive = new int[filesCount];
            ByteBuffer closedArchive = new ByteBuffer();
            int i;
            int offset;
            for (i = 0; i < filesCount; ++i) {
                offset = reference.getValidFileIds()[i];
                byte[] fileData;
                if (offset == fileId)
                    fileData = data;
                else
                    fileData = Objects.requireNonNull(getFile(archiveId, offset, keys));
                archive[i] = fileData.length;
                closedArchive.writeBytes(fileData);
            }
            for (i = 0; i < filesCount; ++i) {
                offset = archive[i];
                if (i != 0)
                    offset -= archive[i - 1];
                closedArchive.writeInt(offset);
            }
            closedArchive.writeByte(1);
            archiveData = new byte[closedArchive.getOffset()];
            closedArchive.setOffset(0);
            closedArchive.getBytes(archiveData, 0, archiveData.length);
        }
        reference.updateRevision();
        Archive archive = new Archive(archiveId, compression, reference.getRevision(), archiveData);
        byte[] compressed = archive.compress();
        reference.setCrc(CRC32HGenerator.getHash(compressed, 0, compressed.length - 2));
        reference.setWhirpool(Whirlpool.getHash(compressed, 0, compressed.length - 2));
        if (archiveName != -1)
            reference.setNameHash(archiveName);
        if (fileName != -1)
            reference.getFiles()[fileId].setNameHash(fileName);
        if (!mainFile.putArchiveData(archiveId, compressed))
            return false;
        else if (rewriteTable && !rewriteTable())
            return false;
        else {
            if (resetCache)
                resetCachedFiles();
            return true;
        }
    }

    public boolean encryptArchive(int archiveId, int[] keys) {
        return encryptArchive(archiveId, null, keys, true, true);
    }

    public boolean encryptArchive(int archiveId, int[] oldKeys, int[] keys, boolean rewriteTable, boolean resetCache) {
        if (!archiveExists(archiveId))
            return false;
        else {
            Archive archive = mainFile.getArchive(archiveId, oldKeys);
            if (archive == null)
                return false;
            else {
                ArchiveReference reference = table.getArchives()[archiveId];
                if (reference.getRevision() != archive.getRevision())
                    throw new RuntimeException("ERROR REVISION");
                else {
                    reference.updateRevision();
                    archive.setRevision(reference.getRevision());
                    archive.setKeys(keys);
                    byte[] closedArchive = archive.compress();
                    reference.setCrc(CRC32HGenerator.getHash(closedArchive, 0, closedArchive.length - 2));
                    reference.setWhirpool(Whirlpool.getHash(closedArchive, 0, closedArchive.length - 2));
                    if (!mainFile.putArchiveData(archiveId, closedArchive))
                        return false;
                    else if (rewriteTable && !rewriteTable())
                        return false;
                    else {
                        if (resetCache)
                            resetCachedFiles();
                        return true;
                    }
                }
            }
        }
    }

    public boolean rewriteTable() {
        table.updateRevision();
        table.sortTable();
        Object[] hashes = table.encodeHeader(index255);
        if (hashes == null)
            return false;
        else {
            crc = (Integer) hashes[0];
            whirlpool = (byte[]) hashes[1];
            return true;
        }
    }

    public int[] getKeys() {
        return table.getKeys();
    }

    public void setKeys(int[] keys) {
        table.setKeys(keys);
    }

    private void cacheArchiveFiles(int archiveId, int[] keys) {
        Archive archive = getArchive(archiveId, keys);
        int lastFileId = getLastFileId(archiveId);
        cachedFiles[archiveId] = new byte[lastFileId + 1][];
        if (archive != null) {
            byte[] data = archive.getData();
            if (data != null) {
                int filesCount = getValidFilesCount(archiveId);
                if (filesCount == 1) {
                    cachedFiles[archiveId][lastFileId] = data;
                } else {
                    int readPosition = data.length;
                    --readPosition;
                    int max = data[readPosition] & 255;
                    readPosition -= max * filesCount * 4;
                    ByteBuffer stream = new ByteBuffer(data);
                    stream.setOffset(readPosition);
                    int[] filesSize = new int[filesCount];
                    int sourceOffset;
                    int count;
                    for (int i = 0; i < max; ++i) {
                        sourceOffset = 0;
                        for (count = 0; count < filesCount; ++count)
                            filesSize[count] += sourceOffset += stream.readInt();
                    }
                    byte[][] var18 = new byte[filesCount][];
                    for (sourceOffset = 0; sourceOffset < filesCount; ++sourceOffset) {
                        var18[sourceOffset] = new byte[filesSize[sourceOffset]];
                        filesSize[sourceOffset] = 0;
                    }
                    stream.setOffset(readPosition);
                    sourceOffset = 0;
                    int fileId;
                    int i;
                    for (count = 0; count < max; ++count) {
                        fileId = 0;
                        for (i = 0; i < filesCount; ++i) {
                            fileId += stream.readInt();
                            System.arraycopy(data, sourceOffset, var18[i], filesSize[i], fileId);
                            sourceOffset += fileId;
                            filesSize[i] += fileId;
                        }
                    }
                    count = 0;
                    int[] var17;
                    int var16 = (var17 = table.getArchives()[archiveId].getValidFileIds()).length;
                    for (i = 0; i < var16; ++i) {
                        fileId = var17[i];
                        cachedFiles[archiveId][fileId] = var18[count++];
                    }
                }
            }
        }
    }

    public int getId() {
        return mainFile.getId();
    }

    public ReferenceTable getTable() {
        return table;
    }

    public MainFile getMainFile() {
        return mainFile;
    }

    public Archive getArchive(int id) {
        return mainFile.getArchive(id, null);
    }

    public Archive getArchive(int id, int[] keys) {
        return mainFile.getArchive(id, keys);
    }

    public int getCRC() {
        return crc;
    }

    public byte[] getWhirlpool() {
        return whirlpool;
    }

    private int getNameHash(String name) {
        name = name.toLowerCase();
        int hash = 0;
        for (int i = 0; i < name.length(); i++)
            hash = method1258(name.charAt(i)) + ((hash << 5) - hash);
        return hash;
    }

    private byte method1258(char c) {
        byte b;
        if (c > 0 && c < '\200' || c >= '\240' && c <= '\377') {
            b = (byte) c;
        } else if (c != '\u20AC') {
            if (c != '\u201A') {
                if (c != '\u0192') {
                    if (c == '\u201E') {
                        b = -124;
                    } else if (c != '\u2026') {
                        if (c != '\u2020') {
                            if (c == '\u2021') {
                                b = -121;
                            } else if (c == '\u02C6') {
                                b = -120;
                            } else if (c == '\u2030') {
                                b = -119;
                            } else if (c == '\u0160') {
                                b = -118;
                            } else if (c == '\u2039') {
                                b = -117;
                            } else if (c == '\u0152') {
                                b = -116;
                            } else if (c != '\u017D') {
                                if (c == '\u2018') {
                                    b = -111;
                                } else if (c != '\u2019') {
                                    if (c != '\u201C') {
                                        if (c == '\u201D') {
                                            b = -108;
                                        } else if (c != '\u2022') {
                                            if (c == '\u2013') {
                                                b = -106;
                                            } else if (c == '\u2014') {
                                                b = -105;
                                            } else if (c == '\u02DC') {
                                                b = -104;
                                            } else if (c == '\u2122') {
                                                b = -103;
                                            } else if (c != '\u0161') {
                                                if (c == '\u203A') {
                                                    b = -101;
                                                } else if (c != '\u0153') {
                                                    if (c == '\u017E') {
                                                        b = -98;
                                                    } else if (c != '\u0178') {
                                                        b = 63;
                                                    } else {
                                                        b = -97;
                                                    }
                                                } else {
                                                    b = -100;
                                                }
                                            } else {
                                                b = -102;
                                            }
                                        } else {
                                            b = -107;
                                        }
                                    } else {
                                        b = -109;
                                    }
                                } else {
                                    b = -110;
                                }
                            } else {
                                b = -114;
                            }
                        } else {
                            b = -122;
                        }
                    } else {
                        b = -123;
                    }
                } else {
                    b = -125;
                }
            } else {
                b = -126;
            }
        } else {
            b = -128;
        }
        return b;
    }
}
