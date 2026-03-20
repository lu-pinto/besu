/*
 * Copyright contributors to Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.datatypes;

import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class Bytes32Helper {
  public static final Bytes32 ZERO_BYTES32 = Bytes32.wrap(new byte[32]);

  private static final int BYTESIZE = 32;
  private static final byte[] ZERO_BYTES = new byte[BYTESIZE];

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

  public static Bytes32 add(final Bytes32 a, final Bytes32 b) {
    return Bytes32.wrap(add(a.toArrayUnsafe(), b.toArrayUnsafe()));
  }

  public static Bytes32 subtract(final Bytes32 a, final Bytes32 b) {
    return Bytes32.wrap(sub(a.toArrayUnsafe(), b.toArrayUnsafe()));
  }

  /**
   * Addition in bytes: x + y.
   *
   * <p>Compute the wrapping sum
   *
   * @param x The left value to add.
   * @param y The right value to add.
   * @return The sum x + y.
   */
  public static byte[] add(final byte[] x, final byte[] y) {
    if (isZero(x)) return y;
    if (isZero(y)) return x;
    return adc(x, y);
  }

  /**
   * Substraction in bytes: x - y.
   *
   * <p>Compute the wrapping difference
   *
   * @param x The left value.
   * @param y The right value to substract.
   * @return The wrapping difference x - y.
   */
  public static byte[] sub(final byte[] x, final byte[] y) {
    if (isZero(y)) return x;
    if (isZero(x)) return neg(y);
    return sbb(x, y);
  }

  private static boolean isZero(final byte[] arr) {
    int index = Arrays.mismatch(arr, ZERO_BYTES);
    return (index == -1 || index >= arr.length);
  }

  private static byte[] padLeft(final byte[] a) {
    if (a.length == BYTESIZE) return a;
    byte[] res = new byte[BYTESIZE];
    System.arraycopy(a, 0, res, BYTESIZE - a.length, a.length);
    return res;
  }

  private static byte[] adc(final byte[] a, final byte[] b) {
    int res;
    int carry = 0;
    byte[] x = padLeft(a);
    byte[] y = padLeft(b);
    byte[] sum = new byte[BYTESIZE];
    for (int i = 31; i >= 0; i--) {
      res = (x[i] & 0xFF) + (y[i] & 0xFF) + carry;
      sum[i] = (byte) res;
      carry = (res >> 8);
    }
    return sum;
  }

  private static byte[] neg(final byte[] a) {
    int res;
    int carry = 1;
    byte[] x = padLeft(a);
    byte[] out = new byte[BYTESIZE];
    for (int i = 31; i >= 0; i--) {
      res = (~x[i] & 0xFF) + carry;
      out[i] = (byte) res;
      carry = (res >> 8);
    }
    return out;
  }

  private static byte[] sbb(final byte[] a, final byte[] b) {
    int res;
    int borrow = 0;
    byte[] x = padLeft(a);
    byte[] y = padLeft(b);
    byte[] diff = new byte[BYTESIZE];
    for (int i = 31; i >= 0; i--) {
      res = (x[i] & 0xFF) - (y[i] & 0xFF) - borrow;
      diff[i] = (byte) res;
      borrow = (res < 0) ? 1 : 0;
    }
    return diff;
  }

  public static boolean greaterThanZero(final Bytes32 bytes) {
    return !isZero(bytes.toArrayUnsafe());
  }
}
