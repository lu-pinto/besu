/*
 * Copyright contributors to Hyperledger Besu.
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
package org.hyperledger.besu.evm.gascalculator.stateless;

public final class AccessMode {
  public static byte READ = 1;
  public static byte WRITE_SET = 2;
  public static byte WRITE_RESET = 4;

  private static boolean isRead(final byte accessMode) {
    return (accessMode & READ) != 0;
  }

  public static boolean isWrite(final byte accessMode) {
    return isWriteSet(accessMode) || isWriteReset(accessMode);
  }

  public static boolean isWriteSet(final byte accessMode) {
    return (accessMode & WRITE_SET) != 0;
  }

  private static boolean isWriteReset(final byte accessMode) {
    return (accessMode & WRITE_RESET) != 0;
  }

  public static String toString(final byte accessMode) {
    if (isRead(accessMode)) {
      return "READ";
    } else if (isWriteSet(accessMode)) {
      return "WRITE_SET";
    } else if (isWriteReset(accessMode)) {
      return "WRITE_RESET";
    }
    return "UNKNOWN_ACCESS_MODE";
  }
}