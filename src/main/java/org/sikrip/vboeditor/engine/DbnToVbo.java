package org.sikrip.vboeditor.engine;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Converter from dbn file to vbo.
 *
 * Some info regarding the dbn file:
 *
 * [HEADER]
 * SATS(1)
 * TIME(3)
 * LATITUDE(4)
 * LONGITUDE(4)
 * VELOCITY(2)
 * HEADING(2)
 * HEIGHT(4)
 * YAW__(2)
 * YAW_(2)
 * YAW(2)
 * SLIP(2)
 * CHKSUM(2)
 *
 * Data packet size: 30
 */
public class DbnToVbo {

    // 30 for the data and then the bytes 13, 10, 36
    private static final int DATA_PACKET_SIZE = 33;

    public static void convert(String dbnFilePath, String vboFilePath) {
        final DecimalFormat satsFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        satsFormat.applyPattern("000");

        final DecimalFormat coordinatesFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        coordinatesFormat.applyPattern("00000.00000000");

        final DecimalFormat commonDecimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        commonDecimalFormat.applyPattern("0000.0000");

        final String fileTag = String.format(
            "File created on %s",
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date())
        );
        try (
                final InputStream dbnStream = new FileInputStream(dbnFilePath);
                final FileWriter writer = new FileWriter(vboFilePath);
                final BufferedWriter vboWriter = new BufferedWriter(writer)
        ) {
            vboWriter.write(fileTag +
                    "\n" +
                    "\n" +
                    "[header]\n" +
                    "satellites\n" +
                    "time\n" +
                    "latitude\n" +
                    "longitude\n" +
                    "velocity kmh\n" +
                    "heading\n" +
                    "height\n" +
                    "YAW__\n" +
                    "YAW_\n" +
                    "YAW\n" +
                    "SLIP\n" +
                    "\n" +
                    "[comments]\n" +
                    "Converted from .dbn file" +
                    "\n" +
                    "[column names]\n" +
                    "sats time lat long velocity heading height YAW__ YAW_ YAW SLIP\n" +
                    "[data]\n"
            );

            // Read until the [DATA] section is reached.
            do {
                if (dbnStream.read() == '[' &&
                    dbnStream.read() == 'D' &&
                    dbnStream.read() == 'A' &&
                    dbnStream.read() == 'T' &&
                    dbnStream.read() == 'A' &&
                    dbnStream.read() == ']') {
                    // the bytes 13, 10, 36 follow the [DATA]
                    dbnStream.read();
                    dbnStream.read();
                    dbnStream.read();
                    break;
                }
            } while (dbnStream.available() >= 6);

            final byte[] dataPacketBytes = new byte[DATA_PACKET_SIZE];
            while (dbnStream.read(dataPacketBytes) == DATA_PACKET_SIZE) {

                final byte sats = dataPacketBytes[0];

                final int time = convertThreeBytesToUnsingedInt(dataPacketBytes[3], dataPacketBytes[2], dataPacketBytes[1]) * 10;

                final double latitude = convertFourBytesToUnsingedInt(dataPacketBytes[7], dataPacketBytes[6], dataPacketBytes[5], dataPacketBytes[4]) / 100000.0;

                final double longitude = -(convertFourBytesToUnsingedInt(dataPacketBytes[11], dataPacketBytes[10], dataPacketBytes[9], dataPacketBytes[8]) / 100000.0);

                final double velocity = convertTwoBytesToUnsignedInt(dataPacketBytes[13], dataPacketBytes[12]) / 100.0;

                final double heading = convertTwoBytesToUnsignedInt(dataPacketBytes[15], dataPacketBytes[14]) / 100.0;

                final double height = convertFourBytesToUnsingedInt(dataPacketBytes[19], dataPacketBytes[18], dataPacketBytes[17], dataPacketBytes[16]) / 100.0;

                final double yaw__ = convertTwoBytesToSignedInt(dataPacketBytes[21], dataPacketBytes[20]) / 10.0;
                final double yaw_ = convertTwoBytesToSignedInt(dataPacketBytes[23], dataPacketBytes[22]) / 10.0;
                final double yaw = convertTwoBytesToSignedInt(dataPacketBytes[25], dataPacketBytes[24]) / 10.0;
                final double slip = convertTwoBytesToSignedInt(dataPacketBytes[27], dataPacketBytes[26]) / 10.0;

                final int checksum = convertTwoBytesToUnsignedInt(dataPacketBytes[29], dataPacketBytes[28]);
                final int checksumCalc = calculateDbnChecksum(dataPacketBytes, 28);

                if (checksum != checksumCalc) {
                    throw new RuntimeException();
                }

                vboWriter.write(
                    String.format(
                        "%s %s %s %s %s %s %s %s  %s %s %s\n",
                        satsFormat.format(sats),
                        millisToVboTime(time),
                        coordinatesFormat.format(latitude),
                        coordinatesFormat.format(longitude),
                        commonDecimalFormat.format(velocity),
                        commonDecimalFormat.format(heading),
                        commonDecimalFormat.format(height),
                        commonDecimalFormat.format(yaw__),
                        commonDecimalFormat.format(yaw_),
                        commonDecimalFormat.format(yaw),
                        commonDecimalFormat.format(slip)
                    )
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static int calculateDbnChecksum(byte[] data, int length) {
        int crc = 0;
        for (int i = 0; i < length; i++) {
            crc = crc ^ (data[i] * 256);
            crc = crc % 65536;
            for (int j = 8; j > 0; j--) {
                if ((crc & 32768) == 32768) {
                    crc = crc * 2;
                    crc = (crc ^ 4132);
                } else {
                    crc = crc * 2;
                }
                crc = crc % 65536;
            }
        }
        return crc & 0xffff; // convert to unsigned int
    }

    private static String millisToVboTime(int millis) {
        final int hour = millis / (1000 * 60 * 60);
        final int minute = (millis - hour * 1000 * 60 * 60) / (1000 * 60);
        final int second = (millis - hour * 1000 * 60 * 60 - minute * 1000 * 60) / 1000;
        final int secondDecimal = millis - hour * 1000 * 60 * 60 - minute * 1000 * 60 - second * 1000;

        return String.format("%02d%02d%02d.%03d", hour, minute, second, secondDecimal);
    }

    private static int convertFourBytesToUnsingedInt(byte b1, byte b2, byte b3, byte b4) {
        return (b4 & 0xFF) << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b1 & 0xFF);
    }

    private static int convertThreeBytesToUnsingedInt(byte b1, byte b2, byte b3) {
        return (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b1 & 0xFF);
    }

    private static int convertTwoBytesToUnsignedInt(byte b1, byte b2) {
        return (b2 & 0xFF) << 8 | (b1 & 0xFF);
    }

    private static int convertTwoBytesToSignedInt(byte b1, byte b2) {
        return b2 << 8 | b1;
    }
}
