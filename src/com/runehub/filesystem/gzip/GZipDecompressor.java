package com.runehub.filesystem.gzip;

import com.runehub.filesystem.buffer.*;

import java.util.zip.*;

public class GZipDecompressor {
    private static final Inflater inflaterInstance = new Inflater(true);

    public GZipDecompressor() {
    }

    public static  boolean decompress(ByteBuffer stream, byte[] data) {
        Inflater var2 = inflaterInstance;
        synchronized (inflaterInstance) {
            if (stream.getBuffer()[stream.getOffset()] == 31 && stream.getBuffer()[stream.getOffset() + 1] == -117) {
                try {
                    inflaterInstance.setInput(stream.getBuffer(), stream.getOffset() + 10, -stream.getOffset() - 18 + stream.getBuffer().length);
                    inflaterInstance.inflate(data);
                } catch (Exception var4) {
                    inflaterInstance.reset();
                    return false;
                }
                inflaterInstance.reset();
                return true;
            } else
                return false;
        }
    }
}
