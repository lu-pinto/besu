/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import it.unich.jgmp.MPZ;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** The Mod operation. */
public class ModOperation extends AbstractFixedCostOperation {

  private static final OperationResult modSuccess = new OperationResult(5, null);

  /**
   * Instantiates a new Mod operation.
   *
   * @param gasCalculator the gas calculator
   */
  public ModOperation(final GasCalculator gasCalculator) {
    super(0x06, "MOD", 2, 1, gasCalculator, gasCalculator.getLowTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs Mod operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    final Bytes aBytes = frame.popStackItem();
    final Bytes bBytes = frame.popStackItem();
    if (bBytes.isZero()) {
      frame.pushStackItem(Bytes32.ZERO);
    } else {
      // Wrap as ByteBuffer for bufferImport
      ByteBuffer aBuf = ByteBuffer.wrap(aBytes.toArrayUnsafe());
      ByteBuffer bBuf = ByteBuffer.wrap(bBytes.toArrayUnsafe());

      // ---- Import 256-bit unsigned big-endian values into MPZ ----
      // order=1 (most-significant word first), size=1 (word size = 1 byte),
      // endian=1 (big-endian within words), nails=0
      final MPZ a = MPZ.bufferImport(/*order=*/1, /*size=*/1, /*endian=*/1, /*nails=*/0, aBuf);
      final MPZ b = MPZ.bufferImport(1, 1, 1, 0, bBuf);

      // ---- Compute a mod b (unsigned semantics, result 0 <= r < b) ----
      MPZ r = a.mod(b); // equivalent to mpz_mod

      // ---- Export back to big-endian bytes, fit to 32 bytes ----
      ByteBuffer outBuf = r.bufferExport(1, 1, 1, 0);

      // Read the exported bytes from the ByteBuffer
      byte[] raw;
      if (outBuf.hasArray()) {
        int offset = outBuf.arrayOffset() + outBuf.position();
        int len = outBuf.remaining();
        raw = Arrays.copyOfRange(outBuf.array(), offset, offset + len);
        outBuf.position(outBuf.limit());
      } else {
        raw = new byte[outBuf.remaining()];
        outBuf.get(raw);
      }

      // Keep least-significant 32 bytes if longer
      if (raw.length > 32) {
        raw = Arrays.copyOfRange(raw, raw.length - 32, raw.length);
      }

      // Left-pad with zeros to 32 bytes
      byte[] out = new byte[32];
      System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);

      frame.pushStackItem(Bytes32.wrap(out));
    }
    return modSuccess;
  }
}
