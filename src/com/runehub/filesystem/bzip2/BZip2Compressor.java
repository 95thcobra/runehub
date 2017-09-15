package com.runehub.filesystem.bzip2;

import org.codehaus.plexus.archiver.bzip2.*;

import java.io.*;

public class BZip2Compressor {
    public BZip2Compressor() {
    }

    public static byte[] compress(byte[] data) {
        ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
        try {
            CBZip2OutputStream e = new CBZip2OutputStream(compressedBytes);
            e.write(data);
            e.close();
            return compressedBytes.toByteArray();
        } catch (IOException var3) {
            var3.printStackTrace();
            return null;
        }
    }
}
