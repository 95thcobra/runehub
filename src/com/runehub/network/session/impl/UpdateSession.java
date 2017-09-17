package com.runehub.network.session.impl;

import com.runehub.filesystem.*;
import com.runehub.filesystem.buffer.*;
import com.runehub.filesystem.whirlpool.*;
import com.runehub.network.session.*;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;

import java.math.*;

/**
 * @author Tylurr <tylerjameshurst@gmail.com>
 * @since 9/15/2017
 */
public class UpdateSession extends Session {
    private static final int[] KEYS = {1441, 78700, 44880, 39771, 363186, 44375, 0, 16140, 7316, 271148, 810710, 216189, 379672, 454149, 933950, 21006,
            25367, 17247, 1244, 1, 14856, 1494, 119, 882901, 1818764, 3963, 3618};
    private static final BigInteger EXPONENT = new BigInteger
            ("95776340111155337321344029627634178888626101791582245228586750697996713454019354716577077577558156976177994479837760989691356438974879647293064177555518187567327659793331431421153203931914933858526857396428052266926507860603166705084302845740310178306001400777670591958466653637275131498866778592148380588481");
    private static final BigInteger MODULUS = new BigInteger
            ("119555331260995530494627322191654816613155476612603817103079689925995402263457895890829148093414135342420807287820032417458428763496565605970163936696811485500553506743979521465489801746973392901885588777462023165252483988431877411021816445058706597607453280166045122971960003629860919338852061972113876035333");
    private final byte[] UKEY = createUKeyData();
    private int state;

    public UpdateSession(FileSystem fileSystem, ChannelHandlerContext ctx, Channel channel) {
        super(fileSystem, ctx, channel);
    }

    @Override
    public void onMessageReceived(ByteBuffer buffer) {
        while (buffer.getRemaining() > 0 && channel.isConnected()) {
            int opcode = buffer.readUnsignedByte();
            switch (opcode) {
                case 0:
                case 1:
                    int index = buffer.readUnsignedByte();
                    int archive = buffer.readInt();
                    if (archive < 0)
                        return;
                    if (index != 255) {
                        if (fileSystem.getIndexes().length <= index || fileSystem.getIndexes()[index] == null || !fileSystem.getIndexes()[index]
                                .archiveExists(archive))
                            return;
                    } else if (archive != 255) {
                        if (fileSystem.getIndexes().length <= archive || fileSystem.getIndexes()[archive] == null)
                            return;
                    }
                    if (index == 255 && archive == 255) {
                        write(toBuffer(255, 255, UKEY));
                    } else {
                        write(getArchiveRequest(index, archive, opcode == 1));
                    }
                    break;
                case 7:
                    onSessionExit();
                    break;
                case 4:
                    state = buffer.readUnsignedByte();
                    if (buffer.readUnsignedShort() != 0)
                        onSessionExit();
                    break;
                default:
                    buffer.skip(11);
                    break;
            }
        }
    }

    private ChannelBuffer getArchiveRequest(int indexId, int archiveId, boolean priority) {
        byte[] archive = indexId == 255 ? fileSystem.getIndex255().getArchiveData(archiveId) : fileSystem.getIndexes()[indexId].getMainFile()
                .getArchiveData(archiveId);
        if (archive == null)
            return null;
        int compression = archive[0] & 0xff;
        int length = ((archive[1] & 0xff) << 24) + ((archive[2] & 0xff) << 16) + ((archive[3] & 0xff) << 8) + (archive[4] & 0xff);
        int settings = compression;
        if (!priority)
            settings |= 0x80;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeByte(indexId);
        buffer.writeInt(archiveId);
        buffer.writeByte(settings);
        buffer.writeInt(length);
        int realLength = compression != 0 ? length + 4 : length;
        for (int index = 5; index < realLength + 5; index++) {
            if (buffer.writerIndex() % 512 == 0)
                buffer.writeByte(255);
            buffer.writeByte(archive[index]);
        }
        if (state != 0) {
            for (int i = 0; i < buffer.arrayOffset(); i++)
                buffer.setByte(i, buffer.getByte(i) ^ state);
        }
        return buffer;
    }

    public void startup() {
        write(new ByteBuffer(1 + KEYS.length * 4).writeByte(0).writeInts(KEYS));
    }

    private byte[] createUKeyData() {
        ByteBuffer stream = new ByteBuffer();
        stream.writeByte(fileSystem.getIndexes().length);
        for (int index = 0; index < fileSystem.getIndexes().length; index++) {
            if (fileSystem.getIndexes()[index] == null) {
                stream.writeInt(0);
                stream.writeInt(0);
                stream.writeBytes(new byte[64]);
                continue;
            }
            stream.writeInt(fileSystem.getIndexes()[index].getCRC());
            stream.writeInt(fileSystem.getIndexes()[index].getTable().getRevision());
            stream.writeBytes(fileSystem.getIndexes()[index].getWhirlpool());
        }
        byte[] archive = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(archive, 0, archive.length);
        ByteBuffer hashStream = new ByteBuffer(65);
        hashStream.writeByte(0);
        hashStream.writeBytes(Whirlpool.getHash(archive, 0, archive.length));
        byte[] hash = new byte[hashStream.getOffset()];
        hashStream.setOffset(0);
        hashStream.getBytes(hash, 0, hash.length);
        hash = fileSystem.cryptRSA(hash, EXPONENT, MODULUS);
        stream.writeBytes(hash);
        archive = new byte[stream.getOffset()];
        stream.setOffset(0);
        stream.getBytes(archive, 0, archive.length);
        return archive;
    }

    public final ByteBuffer toBuffer(int indexFileId, int containerId, byte[] archive) {
        ByteBuffer stream = new ByteBuffer(archive.length + 4);
        stream.writeByte(indexFileId);
        stream.writeInt(containerId);
        stream.writeByte(0);
        stream.writeInt(archive.length);
        int offset = 10;
        for (byte bit : archive) {
            if (offset == 512) {
                stream.writeByte(255);
                offset = 1;
            }
            stream.writeByte(bit);
            offset++;
        }
        return stream;
    }
}
