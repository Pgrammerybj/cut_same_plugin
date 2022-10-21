/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ss.ugc.android.editor.track.diskcache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

/** Junk drawer of utility methods. */
final class Util {
  static final Charset US_ASCII = Charset.forName("US-ASCII");
  static final Charset UTF_8 = Charset.forName("UTF-8");

  private Util() {
  }

  static String readFully(Reader reader) throws IOException {
    try {
      StringWriter writer = new StringWriter();
      char[] buffer = new char[1024];
      int count;
      while ((count = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, count);
      }
      return writer.toString();
    } finally {
      reader.close();
    }
  }

  /**
   * Deletes the contents of {@code dir}. Throws an IOException if any file
   * could not be deleted, or if {@code dir} is not a readable directory.
   */
  static void deleteContents(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IOException("not a readable directory: " + dir);
    }
    for (File file : files) {
      if (file.isDirectory()) {
        deleteContents(file);
      }
      if (!file.delete()) {
        throw new IOException("failed to delete file: " + file);
      }
    }
  }

  static void closeQuietly(/*Auto*/Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }

  private static final char[] SHA_256_CHARS = new char[64];

  /**
   * Returns the hex string of the given byte array representing a SHA256 hash.
   */
  public static String sha256BytesToHex(byte[] bytes) {
    synchronized (SHA_256_CHARS) {
      return bytesToHex(bytes, SHA_256_CHARS);
    }
  }
  private static final char[] HEX_CHAR_ARRAY = "0123456789abcdef".toCharArray();


  // Taken from:
  // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
  // /9655275#9655275
  @SuppressWarnings("PMD.UseVarargs")
  private static String bytesToHex(byte[] bytes, char[] hexChars) {
    int v;
    for (int j = 0; j < bytes.length; j++) {
      v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_CHAR_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_CHAR_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }
}
