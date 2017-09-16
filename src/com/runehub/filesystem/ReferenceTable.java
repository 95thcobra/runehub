package com.runehub.filesystem;

import com.runehub.filesystem.buffer.*;

import java.util.*;

public final class ReferenceTable {
    private final Archive archive;
    private int revision;
    private boolean named;
    private boolean usesWhirpool;
    private ArchiveReference[] archives;
    private int[] validArchiveIds;
    private boolean updatedRevision;
    private boolean needsArchivesSort;

    protected ReferenceTable(Archive archive) {
        this.archive = archive;
        decodeHeader();
    }

    public int[] getKeys() {
        return archive.getKeys();
    }

    public void setKeys(int[] keys) {
        archive.setKeys(keys);
    }

    public void sortArchives() {
        Arrays.sort(validArchiveIds);
        needsArchivesSort = false;
    }

    public void addEmptyArchiveReference(int archiveId) {
        needsArchivesSort = true;
        int[] newValidArchiveIds = Arrays.copyOf(validArchiveIds, validArchiveIds.length + 1);
        newValidArchiveIds[newValidArchiveIds.length - 1] = archiveId;
        validArchiveIds = newValidArchiveIds;
        ArchiveReference reference;
        if (archives.length <= archiveId) {
            ArchiveReference[] newArchives = Arrays.copyOf(archives, archiveId + 1);
            reference = newArchives[archiveId] = new ArchiveReference();
            archives = newArchives;
        } else {
            reference = archives[archiveId] = new ArchiveReference();
        }
        reference.reset();
    }

    public void sortTable() {
        if (needsArchivesSort) {
            sortArchives();
        }
        for (int index = 0; index < validArchiveIds.length; ++index) {
            ArchiveReference archive = archives[validArchiveIds[index]];
            if (archive.isNeedsFilesSort()) {
                archive.sortFiles();
            }
        }
    }

    public Object[] encodeHeader(MainFile mainFile) {
        ByteBuffer stream = new ByteBuffer();
        int protocol = getProtocol();
        stream.writeByte(protocol);
        if (protocol >= 6) {
            stream.writeInt(revision);
        }
        stream.writeByte((named ? 1 : 0) | (usesWhirpool ? 2 : 0));
        if (protocol >= 7) {
            stream.writeBigSmart(validArchiveIds.length);
        } else {
            stream.writeShort(validArchiveIds.length);
        }
        int data;
        int archive;
        for (data = 0; data < validArchiveIds.length; ++data) {
            archive = validArchiveIds[data];
            if (data != 0) {
                archive -= validArchiveIds[data - 1];
            }
            if (protocol >= 7) {
                stream.writeBigSmart(archive);
            } else {
                stream.writeShort(archive);
            }
        }
        if (named) {
            for (data = 0; data < validArchiveIds.length; ++data) {
                stream.writeInt(archives[validArchiveIds[data]].getNameHash());
            }
        }
        if (usesWhirpool) {
            for (data = 0; data < validArchiveIds.length; ++data) {
                stream.writeBytes(archives[validArchiveIds[data]].getWhirpool());
            }
        }
        for (data = 0; data < validArchiveIds.length; ++data) {
            stream.writeInt(archives[validArchiveIds[data]].getCrc());
        }
        for (data = 0; data < validArchiveIds.length; ++data) {
            stream.writeInt(archives[validArchiveIds[data]].getRevision());
        }
        for (data = 0; data < validArchiveIds.length; ++data) {
            archive = this.archives[validArchiveIds[data]].getValidFileIds().length;
            if (protocol >= 7) {
                stream.writeBigSmart(archive);
            } else {
                stream.writeShort(archive);
            }
        }
        int index2;
        ArchiveReference var8;
        for (data = 0; data < validArchiveIds.length; ++data) {
            var8 = archives[validArchiveIds[data]];
            for (index2 = 0; index2 < var8.getValidFileIds().length; ++index2) {
                int offset = var8.getValidFileIds()[index2];
                if (index2 != 0) {
                    offset -= var8.getValidFileIds()[index2 - 1];
                }
                if (protocol >= 7) {
                    stream.writeBigSmart(offset);
                } else {
                    stream.writeShort(offset);
                }
            }
        }
        if (named) {
            for (data = 0; data < validArchiveIds.length; ++data) {
                var8 = archives[validArchiveIds[data]];
                for (index2 = 0; index2 < var8.getValidFileIds().length; ++index2) {
                    stream.writeInt(var8.getFiles()[var8.getValidFileIds()[index2]].getNameHash());
                }
            }
        }
        byte[] var9 = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(var9, 0, var9.length);
        return this.archive.editNoRevision(var9, mainFile);
    }

    public int getProtocol() {
        if (archives.length > '\uffff') {
            return 7;
        } else {
            for (int index = 0; index < validArchiveIds.length; ++index) {
                if (index > 0 && validArchiveIds[index] - validArchiveIds[index - 1] > '\uffff') {
                    return 7;
                }
                if (archives[validArchiveIds[index]].getValidFileIds().length > '\uffff') {
                    return 7;
                }
            }
            return revision == 0 ? 5 : 6;
        }
    }

    public void updateRevision() {
        if (!updatedRevision) {
            ++revision;
            updatedRevision = true;
        }
    }

    private void decodeHeader() {
        ByteBuffer stream = new ByteBuffer(archive.getData());
        int protocol = stream.readUnsignedByte();
        if (protocol >= 5 && protocol <= 7) {
            if (protocol >= 6) {
                revision = stream.readInt();
            }
            int hash = stream.readUnsignedByte();
            named = (1 & hash) != 0;
            usesWhirpool = (2 & hash) != 0;
            int validArchivesCount = protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();
            validArchiveIds = new int[validArchivesCount];
            int lastArchiveId = 0;
            int biggestArchiveId = 0;
            int index;
            int archive;
            for (index = 0; index < validArchivesCount; ++index) {
                archive = lastArchiveId += protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();
                if (archive > biggestArchiveId) {
                    biggestArchiveId = archive;
                }
                validArchiveIds[index] = archive;
            }
            archives = new ArchiveReference[biggestArchiveId + 1];
            for (index = 0; index < validArchivesCount; ++index) {
                this.archives[validArchiveIds[index]] = new ArchiveReference();
            }
            if (named) {
                for (index = 0; index < validArchivesCount; ++index) {
                    archives[validArchiveIds[index]].setNameHash(stream.readInt());
                }
            }
            if (usesWhirpool) {
                for (index = 0; index < validArchivesCount; ++index) {
                    byte[] var13 = new byte[64];
                    stream.getBytes(var13, 0, 64);
                    archives[validArchiveIds[index]].setWhirpool(var13);
                }
            }
            for (index = 0; index < validArchivesCount; ++index) {
                archives[this.validArchiveIds[index]].setCrc(stream.readInt());
            }
            for (index = 0; index < validArchivesCount; ++index) {
                archives[this.validArchiveIds[index]].setRevision(stream.readInt());
            }
            for (index = 0; index < validArchivesCount; ++index) {
                archives[this.validArchiveIds[index]].setValidFileIds(new int[protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort()]);
            }
            int index2;
            for (index = 0; index < validArchivesCount; ++index) {
                archive = 0;
                index2 = 0;
                ArchiveReference archive1 = archives[this.validArchiveIds[index]];
                int index21;
                for (index21 = 0; index21 < archive1.getValidFileIds().length; ++index21) {
                    int fileId = archive += protocol >= 7 ? stream.readBigSmart() : stream.readUnsignedShort();
                    if (fileId > index2) {
                        index2 = fileId;
                    }
                    archive1.getValidFileIds()[index21] = fileId;
                }
                archive1.setFiles(new FileReference[index2 + 1]);
                for (index21 = 0; index21 < archive1.getValidFileIds().length; ++index21) {
                    archive1.getFiles()[archive1.getValidFileIds()[index21]] = new FileReference();
                }
            }
            if (named) {
                for (index = 0; index < validArchivesCount; ++index) {
                    ArchiveReference var14 = archives[validArchiveIds[index]];
                    for (index2 = 0; index2 < var14.getValidFileIds().length; ++index2) {
                        var14.getFiles()[var14.getValidFileIds()[index2]].setNameHash(stream.readInt());
                    }
                }
            }
        } else {
            throw new RuntimeException("INVALID PROTOCOL");
        }
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        updatedRevision = true;
        revision = revision;
    }

    public ArchiveReference[] getArchives() {
        return archives;
    }

    public int[] getValidArchiveIds() {
        return validArchiveIds;
    }

    public boolean isNamed() {
        return named;
    }

    public boolean usesWhirpool() {
        return usesWhirpool;
    }

    public int getCompression() {
        return archive.getCompression();
    }
}
