package org.sikrip.vboeditor.engine;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.sikrip.vboeditor.engine.DbnToVbo.calculateDbnChecksum;

/**
 * Converter from vbo file to dbn.
 */
public class VboToDbn {

    private static final byte[] NEW_LINE_BYTES = new byte[]{(byte) 13, (byte) 10};
    private static final byte[] NEW_DATA_LINE_BYTES = new byte[]{(byte) 13, (byte) 10, (byte) 36};
    private static final byte ZERO = (byte) 0;

    /**
     * Converts from .vbo to .dbn file format.
     * @param vboFilePath the path of the vbo file
     * @param dbnFilePath the path of the dbn file (output)
     */
    public static void convert(String vboFilePath, String dbnFilePath) {
        try (
                final BufferedReader vboReader = new BufferedReader(new FileReader(vboFilePath));
                final FileOutputStream dbnWriter = new FileOutputStream(dbnFilePath);
        ) {
            String inputLine;

            while (!(inputLine = vboReader.readLine()).startsWith("File created")) {
                // Read until the 'File created...' line
            }

            dbnWriter.write(inputLine.getBytes(StandardCharsets.UTF_8));
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("[HEADER]".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("SATS(1)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("TIME(3)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("LATITUDE(4)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("LONGITUDE(4)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("VELOCITY(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("HEADING(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("HEIGHT(4)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("YAW__(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("YAW_(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("YAW(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("SLIP(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("CHKSUM(2)".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("[COMMENTS]".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("Converted using vbotools".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("Find more on www.vbotools.com".getBytes());
            dbnWriter.write(NEW_LINE_BYTES);
            dbnWriter.write("[DATA]".getBytes());

            dbnWriter.write(NEW_DATA_LINE_BYTES);

            while (!(inputLine = vboReader.readLine()).equalsIgnoreCase("[header]")) {
                // Read until the '[header]' section is reached
            }

            final List<String> headerNames = new ArrayList<>();
            while (!(inputLine = vboReader.readLine()).startsWith("[")) { // read until the next section
                if (!"".equals(inputLine)) {
                    headerNames.add(inputLine.toLowerCase());
                }
            }

            final int satsIdx = headerNames.indexOf("satellites");
            final int timeIdx = headerNames.indexOf("time");
            final int latIdx = headerNames.indexOf("latitude");
            final int longIdx = headerNames.indexOf("longitude");
            final int velocityIdx = headerNames.indexOf("velocity kmh");
            final int headingIdx = headerNames.indexOf("heading");
            final int heightIdx = headerNames.indexOf("height");

            while (!(inputLine = vboReader.readLine()).equalsIgnoreCase("[data]")) {
                // Read until the '[data]' section is reached
            }
            while ((inputLine = vboReader.readLine()) != null) {
                final String[] inputDataArray = inputLine.replaceAll(",", ".").split(" ");

                final byte[] payload = new byte[28];
                int payloadIdx = 0;
                // sats(1)
                payload[payloadIdx++] = Byte.parseByte(inputDataArray[satsIdx]);

                // time(3)
                final int time = (int) (vboTimeToMillis(inputDataArray[timeIdx]) / 10);
                final byte[] timeBytes = intToByteArray(time); //
                // skip timeBytes[0] as the time is 3 bytes on dbn file
                payload[payloadIdx++] = timeBytes[1];
                payload[payloadIdx++] = timeBytes[2];
                payload[payloadIdx++] = timeBytes[3];

                // LATITUDE(4)
                final int lat = BigDecimal.valueOf(Double.parseDouble(inputDataArray[latIdx])).multiply(BigDecimal.valueOf(100000)).intValue();
                final byte[] latBytes = intToByteArray(lat);
                payload[payloadIdx++] = latBytes[0];
                payload[payloadIdx++] = latBytes[1];
                payload[payloadIdx++] = latBytes[2];
                payload[payloadIdx++] = latBytes[3];

                // LONGITUDE(4)
                final int longitude = -BigDecimal.valueOf(Double.parseDouble(inputDataArray[longIdx])).multiply(BigDecimal.valueOf(100000)).intValue();
                final byte[] longBytes = intToByteArray(longitude);
                payload[payloadIdx++] = longBytes[0];
                payload[payloadIdx++] = longBytes[1];
                payload[payloadIdx++] = longBytes[2];
                payload[payloadIdx++] = longBytes[3];

                // VELOCITY(2)
                final int velocity = BigDecimal.valueOf(Double.parseDouble(inputDataArray[velocityIdx])).multiply(BigDecimal.valueOf(100)).intValue();
                final byte[] velocityBytes = intToByteArray(velocity);
                // skip velocityBytes[0] and velocityBytes[1] as the velocity is 2 bytes on dbn file
                payload[payloadIdx++] = velocityBytes[2];
                payload[payloadIdx++] = velocityBytes[3];

                // HEADING(2)
                final int heading = BigDecimal.valueOf(Double.parseDouble(inputDataArray[headingIdx])).multiply(BigDecimal.valueOf(100)).intValue();
                final byte[] headingBytes = intToByteArray(heading);
                // skip headingBytes[0] and headingBytes[1] as the heading is 2 bytes on dbn file
                payload[payloadIdx++] = headingBytes[2];
                payload[payloadIdx++] = headingBytes[3];;

                // HEIGHT(4)
                final int height = BigDecimal.valueOf(Double.parseDouble(inputDataArray[heightIdx])).multiply(BigDecimal.valueOf(100)).intValue();
                final byte[] heightBytes = intToByteArray(height);
                payload[payloadIdx++] = heightBytes[0];
                payload[payloadIdx++] = heightBytes[1];
                payload[payloadIdx++] = heightBytes[2];
                payload[payloadIdx++] = heightBytes[3];

                // YAW__ (2)
                payload[payloadIdx++] = ZERO;
                payload[payloadIdx++] = ZERO;

                // YAW_ (2)
                payload[payloadIdx++] = ZERO;
                payload[payloadIdx++] = ZERO;

                // YAW (2)
                payload[payloadIdx++] = ZERO;
                payload[payloadIdx++] = ZERO;

                // SLIP (2)
                payload[payloadIdx++] = ZERO;
                payload[payloadIdx++] = ZERO;

                if (payloadIdx != 28) {
                    throw new RuntimeException("Not entire payload written");
                }
                dbnWriter.write(payload);

                // CHECKSUM(2)
                final int checksum = calculateDbnChecksum(payload, 28);
                final byte[] checksumBytes = intToByteArray(checksum);
                // skip checksumBytes[0] and checksumBytes[1] as the checksum is 2 bytes on dbn file
                dbnWriter.write(checksumBytes[2]);
                dbnWriter.write(checksumBytes[3]);

                // End of data line
                dbnWriter.write(NEW_DATA_LINE_BYTES);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    /**
     * Converts the vbo format of time (UTC time since midnight in the form HH:MM:SS.SS) to milliseconds.
     *
     * @param time the time in the format used by the vbo files
     * @return the equivalent time in milliseconds
     */
    private static long vboTimeToMillis(String time) {
        final int length = time.length();
        if (length == 10 || length == 9) {
            final long hh = Long.parseLong(time.substring(0, 2));
            final long mm = Long.parseLong(time.substring(2, 4));
            final long ss = Long.parseLong(time.substring(4, 6));
            final long millis;
            if (length==9) {
                millis = Long.parseLong(time.substring(7, length)) * 10;
            } else {
                millis = Long.parseLong(time.substring(7, length));
            }
            return millis + ss * 1000 + mm * 60 * 1000 + hh * 60 * 60 * 1000;
        }
        throw new IllegalArgumentException(String.format("Unexpected VBO time value %s", time));
    }
}
