package com.ss.ugc.android.editor.track.utils;

import android.os.StatFs;
import android.text.TextUtils;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;

public final class IOUtils {

    // 防止被继承
    private IOUtils() {
    }

    public static void deleteFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return;
        }
        file.delete();
    }

    public static boolean exists(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    public static boolean mkdir(String path) {
        File dir = new File(path);
        return dir.mkdirs();
    }

    public static boolean copyFile(File srcFile, File dstFile) {
        if (srcFile.exists() && srcFile.isFile()) {
            if (dstFile.isDirectory()) {
                return false;
            }
            if (dstFile.exists()) {
                dstFile.delete();
            }
            try {
                final int byteNumber = 2048;
                byte[] buffer = new byte[byteNumber];
                BufferedInputStream input = new BufferedInputStream(
                        new FileInputStream(srcFile));
                BufferedOutputStream output = new BufferedOutputStream(
                        new FileOutputStream(dstFile));
                while (true) {
                    int count = input.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    output.write(buffer, 0, count);
                }
                input.close();
                output.flush();
                output.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean copyFile(String srcPath, String dstPath) {
        return copyFile(new File(srcPath), new File(dstPath));
    }

    public static String readFileFirstLine(String filePath) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            reader.close();
            return line;
        } catch (Exception e) {
            return "";
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void deletePath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        String[] tmpList = file.list();
        if (tmpList == null) {
            return;
        }
        for (String fileName : tmpList) {
            if (fileName == null) {
                continue;
            }
            String tmpPath;
            if (path.endsWith(File.separator)) {
                tmpPath = path + fileName;
            } else {
                tmpPath = path + File.separator + fileName;
            }
            File tmpFile = new File(tmpPath);
            if (tmpFile.isFile()) {
                tmpFile.delete();
            }
            if (tmpFile.isDirectory()) {
                deletePath(tmpPath);
            }
        }
        file.delete();
    }

    public static void clearPath(String path) {
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        String[] tmpList = file.list();
        for (String fileName : tmpList) {
            String tmpPath;
            if (path.endsWith(File.separator)) {
                tmpPath = path + fileName;
            } else {
                tmpPath = path + File.separator + fileName;
            }
            File tmpFile = new File(tmpPath);
            if (tmpFile.isFile()) {
                tmpFile.delete();
            }
            if (tmpFile.isDirectory()) {
                deletePath(tmpPath);
            }
        }
    }

    public static long getFileSize(String path) {
        if (TextUtils.isEmpty(path)) {
            return 0;
        }
        File file = new File(path);
        if (file == null || !file.exists()) {
            return 0;
        }
        long size = file.length();
        if (file.isDirectory()) {
            File[] childList = file.listFiles();
            if (childList != null) {
                for (File childFile : childList) {
                    try {
                        size += getFileSize(childFile.getAbsolutePath());
                    } catch (StackOverflowError e) {
                        // too many recursion may cause stack over flow
                        e.printStackTrace();
                        return 0;
                    } catch (OutOfMemoryError oomException) {
                        // too many call filenamesToFiles method
                        oomException.printStackTrace();
                        return 0;
                    }
                }
            }
        }
        return size;
    }

    public static String getFileName(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int lastIndex = path.lastIndexOf("/");
        if (lastIndex > 0 && lastIndex < path.length() - 1) {
            return path.substring(lastIndex + 1);
        } else {
            return path;
        }
    }

    public static String getParentFilePath(String filePath) {
        if (filePath.endsWith("/")) {
            filePath = filePath.substring(0, filePath.length() - 1);
        }
        int lastSplitIndex = filePath.lastIndexOf("/");
        if (lastSplitIndex >= 0) {
            return filePath.substring(0, lastSplitIndex);
        }
        return null;
    }

    public static String getFileNameWithoutExtension(String path) {
        String filename = getFileName(path);
        if (filename != null && filename.length() > 0) {
            int dotPos = filename.lastIndexOf('.');
            if (0 < dotPos) {
                return filename.substring(0, dotPos);
            }
        }
        return filename;
    }

    public static String getFileExtension(String path) {
        int index = path.lastIndexOf(".");
        if (index >= 0 && index < path.length() - 1) {
            return path.substring(index + 1).toUpperCase(Locale.getDefault());
        }
        return "";
    }

    public static boolean renameFile(String originPath, String destPath) {
        File origin = new File(originPath);
        File dest = new File(destPath);
        if (!origin.exists()) {
            return false;
        }
        return origin.renameTo(dest);
    }

    /**
     * <b>return the available size of filesystem<b/>
     *
     * @return the number of bytes available on the filesystem rooted at the given File
     */
    public static long getAvailableBytes(String root) {
        try {
            if (!TextUtils.isEmpty(root) && new File(root).exists()) {
                StatFs stat = new StatFs(root);
                long availableBlocks = (long) stat.getAvailableBlocks();
                return stat.getBlockSize() * availableBlocks;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * the size of all bytes of current filesystem where root path inside.<br>
     * <b>return the size of filesystem<b/>
     *
     * @param root
     * @return all bytes.
     */
    public static long getAllBytes(String root) {
        if (TextUtils.isEmpty(root)) {
            return 0L;
        }
        try {
            if (!TextUtils.isEmpty(root) && new File(root).exists()) {
                StatFs stat = new StatFs(root);
                long blocks = (long) stat.getBlockCount();
                return stat.getBlockSize() * blocks;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    /**
     * Check If the storage is able to download.
     *
     * @param file
     * @return able to write
     */
    public static boolean canWrite(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String testName = "." + System.currentTimeMillis();
        File testFile = new File(file, testName);

        boolean result = testFile.mkdir();
        if (result) {
            result = testFile.delete();
        }
        return result;
    }


    public static void write(String path, String content, boolean append) {
        FileWriter writer = null;
        try {
            File output = new File(path);
            if (!output.exists()) {
                output.getParentFile().mkdirs();
                output.createNewFile();
            }
            writer = new FileWriter(path, append);
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public static void close(Closeable in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void close(ZipFile zipFile) {
        if (zipFile == null) {
            return;
        }
        try {
            zipFile.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
