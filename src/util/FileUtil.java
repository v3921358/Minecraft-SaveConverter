package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Windy
 */
public class FileUtil {

    private static final String EXTENSION_SEPARATOR = ".";

    public static final String getExtension(final String fileName) {
        if (fileName == null) {
            return null;
        }
        final int lastIndex = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        if (lastIndex == -1) {
            return null;
        }
        return fileName.substring(lastIndex + 1);
    }

    public static String getFileSeparator() {
        return System.getProperty("file.separator");
    }

    public static String read(String filePath) {
        String line = null;
        StringBuilder buf = new StringBuilder();
        InputStreamReader fr = null;
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            fr = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
            BufferedReader bf = new BufferedReader(fr);
            while ((line = bf.readLine()) != null) {
                buf.append(line);
            }
        } catch (Exception e) {
            printError("FIleUtil.txt", "read", e, "filePath: " + filePath);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    fr = null;
                }
            }
        }
        return buf.toString();
    }

    public static void printError(final String file, final String functionName, final Throwable t, String msg) {
        System.err.println("[" + functionName + "] " + msg + " " + t.getLocalizedMessage());
        log("log/" + file, "[" + functionName + "] " + msg + System.lineSeparator() + getString(t));
    }

    public static void log(final String file, final String msg) {
        logToFile(file, "\r\n------------------------ " + DateUtil.getReadableTime() + " ------------------------\r\n" + msg);
    }

    public static void logToFile(final String file, final String msg) {
        FileOutputStream out = null;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            osw.write(msg);
            osw.flush();

        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    public static String getString(final Throwable e) {
        String retValue = null;
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            retValue = sw.toString();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (sw != null) {
                    sw.close();
                }
            } catch (IOException ignore) {
            }
        }
        return retValue;
    }

}
