package com.runehub.filesystem.gzip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class GZipCompressor {
    public GZipCompressor() {
    }

    public static byte[] compress(byte[] data) {
        ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
        try {
            GZIPOutputStream e = new GZIPOutputStream(compressedBytes);
            e.write(data);
            e.finish();
            e.close();
            return compressedBytes.toByteArray();
        } catch (IOException var3) {
            var3.printStackTrace();
            return null;
        }
    }
}
