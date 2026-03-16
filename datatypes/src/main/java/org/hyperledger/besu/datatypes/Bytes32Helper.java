package org.hyperledger.besu.datatypes;

import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Bytes32Helper {
  public static final Bytes32 ZERO_BYTES32 = Bytes32.wrap(new byte[32]);

  public static Bytes32 leftPad(final Bytes bytes) {
    return leftPad(bytes, (byte) 0);
  }

  public static Bytes32 leftPad(final Bytes bytes, final byte padByte) {
    if (bytes instanceof Bytes32 bytes32) {
      return bytes32;
    }
    final byte[] bytesArray = bytes.toArrayUnsafe();
    if (bytes.size() == 32) {
      return Bytes32.wrap(bytesArray);
    }
    final byte[] newArray = new byte[32];
    if (padByte != 0) {
      Arrays.fill(newArray, 0, 32 - bytesArray.length, padByte);
    }
    final int length = Math.min(32, bytesArray.length);
    System.arraycopy(bytesArray, 0, newArray, 32 - length, length);
    return Bytes32.wrap(newArray);
  }
}
