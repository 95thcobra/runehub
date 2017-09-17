package com.runehub.filesystem.buffer;

import com.runehub.game.model.entity.player.*;

public class ByteBuffer {
    private static final int[] BIT_MASK = new int[]{0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 131071, 262143,
            524287, 1048575, 2097151, 4194303, 8388607, 16777215, 33554431, 67108863, 134217727, 268435455, 536870911, 1073741823, 2147483647, -1};
    private int offset;
    private int length;
    private byte[] buffer;
    private int bitPosition;
    private int opcodeStart = 0;

    public ByteBuffer(int capacity) {
        buffer = new byte[capacity];
    }

    public ByteBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.length = buffer.length;
    }

    public ByteBuffer() {
        setBuffer(new byte[16]);
    }

    public ByteBuffer(byte[] buffer, boolean out) {
        this.setBuffer(buffer);
        this.offset = buffer.length;
        length = buffer.length;
    }

    public ByteBuffer(int[] buffer) {
        setBuffer(new byte[buffer.length]);
        for (int value : buffer)
            writeByte(value);
    }

    public ByteBuffer setId(int j) {
        this.opcodeStart = j;
        return this;
    }

    public ByteBuffer initBitAccess() {
        bitPosition = offset * 8;
        return this;
    }

    public ByteBuffer finishBitAccess() {
        offset = (7 + bitPosition) / 8;
        return this;
    }

    public int readBits(int bitOffset) {
        int bytePos = bitPosition >> 3;
        int i_8_ = -(0x7 & bitPosition) + 8;
        bitPosition += bitOffset;
        int value = 0;
        for (/**/; (~bitOffset) < (~i_8_); i_8_ = 8) {
            value += (BIT_MASK[i_8_] & buffer[bytePos++]) << -i_8_ + bitOffset;
            bitOffset -= i_8_;
        }
        if ((~i_8_) == (~bitOffset))
            value += buffer[bytePos] & BIT_MASK[i_8_];
        else
            value += (buffer[bytePos] >> -bitOffset + i_8_ & BIT_MASK[bitOffset]);
        return value;
    }

    public void checkCapacityPosition(int position) {
        if (position >= getBuffer().length) {
            byte[] newBuffer = new byte[position + 16];
            System.arraycopy(getBuffer(), 0, newBuffer, 0, getBuffer().length);
            setBuffer(newBuffer);
        }
    }

    public ByteBuffer writeBytes(byte[] b, int offset, int length) {
        checkCapacityPosition(this.getOffset() + length - offset);
        System.arraycopy(b, offset, getBuffer(), this.getOffset(), length);
        this.setOffset(this.getOffset() + (length - offset));
        return this;
    }

    public ByteBuffer writeBytes(byte[] b) {
        int offset = 0;
        int length = b.length;
        checkCapacityPosition(this.getOffset() + length - offset);
        System.arraycopy(b, offset, getBuffer(), this.getOffset(), length);
        this.setOffset(this.getOffset() + (length - offset));
        return this;
    }

    public void addBytes128(byte[] data, int offset, int len) {
        for (int k = offset; k < len; k++)
            writeByte((byte) (data[k] + 128));
    }

    public void addBytesS(byte[] data, int offset, int len) {
        for (int k = offset; k < len; k++)
            writeByte((byte) (-128 + data[k]));
    }

    public void addBytes_Reverse(byte[] data, int offset, int len) {
        for (int i = len - 1; i >= 0; i--) {
            writeByte((data[i]));
        }
    }

    public void addBytes_Reverse128(byte[] data, int offset, int len) {
        for (int i = len - 1; i >= 0; i--) {
            writeByte((byte) (data[i] + 128));
        }
    }

    public ByteBuffer writeByte(int i) {
        writeByte(i, offset++);
        return this;
    }

    public void writeNegativeByte(int i) {
        writeByte(-i, offset++);
    }

    public ByteBuffer writeByte(int i, int position) {
        checkCapacityPosition(position);
        getBuffer()[position] = (byte) i;
        return this;
    }

    public ByteBuffer writeByte128(int i) {
        writeByte(i + 128);
        return this;
    }

    public ByteBuffer writeByteC(int i) {
        writeByte(-i);
        return this;
    }

    public ByteBuffer write128Byte(int i) {
        writeByte(128 - i);
        return this;
    }

    public ByteBuffer writeShortLE128(int i) {
        writeByte(i + 128);
        writeByte(i >> 8);
        return this;
    }

    public ByteBuffer writeShort128(int i) {
        writeByte(i >> 8);
        writeByte(i + 128);
        return this;
    }

    public ByteBuffer writeSmart(int i) {
        if (i >= 128) {
            writeShort(i + 32768);
        } else {
            writeByte(i);
        }

        return this;
    }

    public ByteBuffer writeBigSmart(int i) {
        if (i >= Short.MAX_VALUE)
            writeInt(i - Integer.MAX_VALUE - 1);
        else {
            writeShort(i >= 0 ? i : 32767);
        }

        return this;
    }

    public ByteBuffer writeShort(int i) {
        writeByte(i >> 8);
        writeByte(i);
        return this;
    }

    public ByteBuffer writeShortLE(int i) {
        writeByte(i);
        writeByte(i >> 8);
        return this;
    }

    public ByteBuffer write24BitInteger(int i) {
        writeByte(i >> 16);
        writeByte(i >> 8);
        writeByte(i);
        return this;
    }

    public ByteBuffer write24BitIntegerV2(int i) {
        writeByte(i >> 16);
        writeByte(i);
        writeByte(i >> 8);
        return this;
    }

    public ByteBuffer writeInt(int i) {
        writeByte(i >> 24);
        writeByte(i >> 16);
        writeByte(i >> 8);
        writeByte(i);
        return this;
    }

    public ByteBuffer writeInts(int... ints) {
        for (int i : ints)
            writeInt(i);
        return this;
    }

    public ByteBuffer writeIntV1(int i) {
        writeByte(i >> 8);
        writeByte(i);
        writeByte(i >> 24);
        writeByte(i >> 16);
        return this;
    }

    public ByteBuffer writeIntV2(int i) {
        writeByte(i >> 16);
        writeByte(i >> 24);
        writeByte(i);
        writeByte(i >> 8);
        return this;
    }

    public ByteBuffer writeIntLE(int i) {
        writeByte(i);
        writeByte(i >> 8);
        writeByte(i >> 16);
        writeByte(i >> 24);
        return this;
    }

    public ByteBuffer writeLong(long l) {
        writeByte((int) (l >> 56));
        writeByte((int) (l >> 48));
        writeByte((int) (l >> 40));
        writeByte((int) (l >> 32));
        writeByte((int) (l >> 24));
        writeByte((int) (l >> 16));
        writeByte((int) (l >> 8));
        writeByte((int) l);
        return this;
    }

    public void writePSmarts(int i) {
        if (i < 128) {
            writeByte(i);
            return;
        }
        if (i < 32768) {
            writeShort(32768 + i);
        }
    }

    public ByteBuffer writeString(String s) {
        checkCapacityPosition(getOffset() + s.length() + 1);
        System.arraycopy(s.getBytes(), 0, getBuffer(), getOffset(), s.length());
        setOffset(getOffset() + s.length());
        writeByte(0);
        return this;
    }

    public void writeGJString2(String string) {
        byte[] packed = new byte[256];
        int length = packGJString2(0, packed, string);
        writeByte(0);
        writeBytes(packed, 0, length);
        writeByte(0);
    }

    private int packGJString2(int position, byte[] buffer, String String) {
        int length = String.length();
        int offset = position;
        for (int index = 0; length > index; index++) {
            int character = String.charAt(index);
            if (character > 127) {
                if (character > 2047) {
                    buffer[offset++] = (byte) ((character | 919275) >> 12);
                    buffer[offset++] = (byte) (128 | ((character >> 6) & 63));
                    buffer[offset++] = (byte) (128 | (character & 63));
                } else {
                    buffer[offset++] = (byte) ((character | 12309) >> 6);
                    buffer[offset++] = (byte) (128 | (character & 63));
                }
            } else
                buffer[offset++] = (byte) character;
        }
        return offset - position;
    }

    public void writeGJString(String s) {
        writeByte(0);
        writeString(s);
    }

    public void putGJString3(String s) {
        writeByte(0);
        writeString(s);
        writeByte(0);
    }

    public void writePacket(int id) {
        writeSmart(id);
    }

    public void writePacketVarByte(int id) {
        writePacket(id);
        writeByte(0);
        opcodeStart = getOffset() - 1;
    }

    public void writePacketVarShort(int id) {
        writePacket(id);
        writeShort(0);
        opcodeStart = getOffset() - 2;
    }

    public void endPacketVarByte() {
        writeByte(getOffset() - (opcodeStart + 2) + 1, opcodeStart);
    }

    public void endPacketVarShort() {
        int size = getOffset() - (opcodeStart + 2);
        writeByte(size >> 8, opcodeStart++);
        writeByte(size, opcodeStart);
    }

    public int getBitPos(int i) {
        return 8 * i - bitPosition;
    }

    public void writeBits(int numBits, int value) {
        int bytePos = bitPosition >> 3;
        int bitOffset = 8 - (bitPosition & 7);
        bitPosition += numBits;
        for (; numBits > bitOffset; bitOffset = 8) {
            checkCapacityPosition(bytePos);
            getBuffer()[bytePos] &= ~BIT_MASK[bitOffset];
            getBuffer()[bytePos++] |= value >> numBits - bitOffset & BIT_MASK[bitOffset];
            numBits -= bitOffset;
        }
        checkCapacityPosition(bytePos);
        if (numBits == bitOffset) {
            getBuffer()[bytePos] &= ~BIT_MASK[bitOffset];
            getBuffer()[bytePos] |= value & BIT_MASK[bitOffset];
        } else {
            getBuffer()[bytePos] &= ~(BIT_MASK[numBits] << bitOffset - numBits);
            getBuffer()[bytePos] |= (value & BIT_MASK[numBits]) << bitOffset - numBits;
        }
    }

    public void checkCapacity(int length) {
        if (offset + length >= buffer.length) {
            byte[] newBuffer = new byte[(offset + length) * 2];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }
    }

    public int read24BitInt() {
        return (readUnsignedByte() << 16) + (readUnsignedByte() << 8) + (readUnsignedByte());
    }

    public void skip(int length) {
        offset += length;
    }

    public int getRemaining() {
        return offset < length ? length - offset : 0;
    }

    public void addBytes(byte[] b, int offset, int length) {
        checkCapacity(length - offset);
        System.arraycopy(b, offset, buffer, this.offset, length);
        this.length += length - offset;
    }

    public int readPacket(Player player) {
        return readUnsignedByte();
    }

    public int readByte() {
        return getRemaining() > 0 ? buffer[offset++] : 0;
    }

    public void readBytes(byte buffer[], int off, int len) {
        for (int k = off; k < len + off; k++) {
            buffer[k] = (byte) readByte();
        }
    }

    public void readBytes(byte buffer[]) {
        readBytes(buffer, 0, buffer.length);
    }

    public int readSmart2() {
        int i = 0;
        int i_33_ = readUnsignedSmart();
        while (i_33_ == 32767) {
            i_33_ = readUnsignedSmart();
            i += 32767;
        }
        i += i_33_;
        return i;
    }

    public int readUnsignedByte() {
        return readByte() & 0xff;
    }

    public int readByte128() {
        return (byte) (readByte() - 128);
    }

    public int readByteC() {
        return (byte) -readByte();
    }

    public int read128Byte() {
        return (byte) (128 - readByte());
    }

    public int readUnsignedByte128() {
        return readUnsignedByte() - 128 & 0xff;
    }

    public int readUnsignedByteC() {
        return -readUnsignedByte() & 0xff;
    }

    public int readUnsigned128Byte() {
        return 128 - readUnsignedByte() & 0xff;
    }

    public int readShortLE() {
        int i = readUnsignedByte() + (readUnsignedByte() << 8);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readShort128() {
        int i = (readUnsignedByte() << 8) + (readByte() - 128 & 0xff);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readShortLE128() {
        int i = (readByte() - 128 & 0xff) + (readUnsignedByte() << 8);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int read128ShortLE() {
        int i = (128 - readByte() & 0xff) + (readUnsignedByte() << 8);
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readShort() {
        int i = (readUnsignedByte() << 8) + readUnsignedByte();
        if (i > 32767) {
            i -= 0x10000;
        }
        return i;
    }

    public int readUnsignedShortLE() {
        return readUnsignedByte() + (readUnsignedByte() << 8);
    }

    public int readUnsignedShort() {
        return (readUnsignedByte() << 8) + readUnsignedByte();
    }

    // @SuppressWarnings("unused")
    public int readBigSmart() {
        /*
         * if(Constants.REVISION < 670) return readUnsignedShort();
		 */
        if ((buffer[offset] ^ 0xffffffff) <= -1) {
            int value = readUnsignedShort();
            if (value == 32767) {
                return -1;
            }
            return value;
        }
        return readInt() & 0x7fffffff;
    }

    public int readUnsignedShort128() {
        return (readUnsignedByte() << 8) + (readByte() - 128 & 0xff);
    }

    public int readUnsignedShortLE128() {
        return (readByte() - 128 & 0xff) + (readUnsignedByte() << 8);
    }

    public int readInt() {
        return (readUnsignedByte() << 24) + (readUnsignedByte() << 16) + (readUnsignedByte() << 8) + readUnsignedByte();
    }

    public int readIntV1() {
        return (readUnsignedByte() << 8) + readUnsignedByte() + (readUnsignedByte() << 24) + (readUnsignedByte() << 16);
    }

    public int readIntV2() {
        return (readUnsignedByte() << 16) + (readUnsignedByte() << 24) + readUnsignedByte() + (readUnsignedByte() << 8);
    }

    public int readIntLE() {
        return readUnsignedByte() + (readUnsignedByte() << 8) + (readUnsignedByte() << 16) + (readUnsignedByte() << 24);
    }

    public long readLong() {
        long l = readInt() & 0xffffffffL;
        long l1 = readInt() & 0xffffffffL;
        return (l << 32) + l1;
    }

    public String readString() {
        StringBuilder s = new StringBuilder();
        int b;
        while ((b = readByte()) != 0)
            s.append((char) b);
        return s.toString();
    }

    public String readJagString() {
        readByte();
        String s = "";
        int b;
        while ((b = readByte()) != 0) {
            s += (char) b;
        }
        return s;
    }

    public int readUnsignedSmart() {
        int i = 0xff & buffer[offset];
        if (i >= 128) {
            return -32768 + readUnsignedShort();
        }
        return readUnsignedByte();
    }

	/*
     * public String readString() { String s = ""; int b; while ((b =
	 * readByte()) != 0) { s += (char) b; } return s; }
	 */

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void decodeXTEA(int keys[]) {
        decodeXTEA(keys, 5, length);
    }

    public void decodeXTEA(int keys[], int start, int end) {
        int l = offset;
        offset = start;
        int i1 = (end - start) / 8;
        for (int j1 = 0; j1 < i1; j1++) {
            int k1 = readInteger();
            int l1 = readInteger();
            int sum = 0xc6ef3720;
            int delta = 0x9e3779b9;
            for (int k2 = 32; k2-- > 0; ) {
                l1 -= keys[(sum & 0x1c84) >>> 11] + sum ^ (k1 >>> 5 ^ k1 << 4) + k1;
                sum -= delta;
                k1 -= (l1 >>> 5 ^ l1 << 4) + l1 ^ keys[sum & 3] + sum;
            }
            offset -= 8;
            writeInteger(k1);
            writeInteger(l1);
        }
        offset = l;
    }

    public final void encodeXTEA(int keys[], int start, int end) {
        int o = offset;
        int j = (end - start) / 8;
        offset = start;
        for (int k = 0; k < j; k++) {
            int l = readInteger();
            int i1 = readInteger();
            int sum = 0;
            int delta = 0x9e3779b9;
            for (int l1 = 32; l1-- > 0; ) {
                l += sum + keys[3 & sum] ^ i1 + (i1 >>> 5 ^ i1 << 4);
                sum += delta;
                i1 += l + (l >>> 5 ^ l << 4) ^ keys[(0x1eec & sum) >>> 11] + sum;
            }

            offset -= 8;
            writeInteger(l);
            writeInteger(i1);
        }
        offset = o;
    }

    private final int readInteger() {
        offset += 4;
        return ((0xff & buffer[-3 + offset]) << 16) + ((((0xff & buffer[-4 + offset]) << 24) + ((buffer[-2 + offset] & 0xff) << 8)) + (buffer[-1 + offset]
                & 0xff));
    }

    private final void writeInteger(int value) {
        buffer[offset++] = (byte) (value >> 24);
        buffer[offset++] = (byte) (value >> 16);
        buffer[offset++] = (byte) (value >> 8);
        buffer[offset++] = (byte) value;
    }

    public final void getBytes(byte data[], int off, int len) {
        for (int k = off; k < len + off; k++) {
            data[k] = buffer[offset++];
        }
    }

}
