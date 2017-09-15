package com.runehub.filesystem;

import lombok.*;

import java.util.*;

public class ArchiveReference {
    @Getter
    @Setter
    private int nameHash, crc, revision;
    @Getter
    @Setter
    private byte[] whirpool;
    @Getter
    @Setter
    private FileReference[] files;
    @Getter
    @Setter
    private int[] validFileIds;
    @Getter
    @Setter private boolean updatedRevision, needsFilesSort;

    public ArchiveReference() {
    }

    public void updateRevision() {
        if (!updatedRevision) {
            ++revision;
            updatedRevision = true;
        }
    }

    public void removeFileReference(int fileId) {
        int[] newValidFileIds = new int[validFileIds.length - 1];
        int count = 0;
        int[] ids = validFileIds;
        int max = validFileIds.length;
        for (int i = 0; i < max; ++i) {
            int id = ids[i];
            if (id != fileId)
                newValidFileIds[count++] = id;
        }
        validFileIds = newValidFileIds;
        files[fileId] = null;
    }

    public void addEmptyFileReference(int fileId) {
        needsFilesSort = true;
        int[] newValidFileIds = Arrays.copyOf(validFileIds, validFileIds.length + 1);
        newValidFileIds[newValidFileIds.length - 1] = fileId;
        validFileIds = newValidFileIds;
        if (files.length <= fileId) {
            FileReference[] newFiles = Arrays.copyOf(this.files, fileId + 1);
            newFiles[fileId] = new FileReference();
            files = newFiles;
        } else
            files[fileId] = new FileReference();
    }

    public void sortFiles() {
        Arrays.sort(validFileIds);
        needsFilesSort = false;
    }

    public void reset() {
        whirpool = null;
        updatedRevision = true;
        revision = 0;
        nameHash = 0;
        crc = 0;
        files = new FileReference[0];
        validFileIds = new int[0];
        needsFilesSort = false;
    }

    public void copyHeader(ArchiveReference fromReference) {
        setCrc(fromReference.getCrc());
        setNameHash(fromReference.getNameHash());
        setWhirpool(fromReference.getWhirpool());
        int[] validFiles = fromReference.getValidFileIds();
        setValidFileIds(Arrays.copyOf(validFiles, validFiles.length));
        FileReference[] files = fromReference.getFiles();
        setFiles(Arrays.copyOf(files, files.length));
    }
}
