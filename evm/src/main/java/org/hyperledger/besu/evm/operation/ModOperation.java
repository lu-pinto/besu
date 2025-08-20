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

import it.unich.jgmp.nativelib.LibGmp;
import it.unich.jgmp.nativelib.SizeT;
import it.unich.jgmp.nativelib.SizeTByReference;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import it.unich.jgmp.nativelib.MpzT;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** The Mod operation. */
public class ModOperation extends AbstractFixedCostOperation {

  private static final OperationResult modSuccess = new OperationResult(5, null);
  static final MpzT op1 = new MpzT();
  static final MpzT op2 = new MpzT();
  static final MpzT res = new MpzT();
  static final SizeT one = new SizeT(1);
  static final SizeT zero = new SizeT(0);
  static final ByteBuffer inA = ByteBuffer.allocateDirect(32);
  static final ByteBuffer inB = ByteBuffer.allocateDirect(32);
  static final ByteBuffer out = ByteBuffer.allocateDirect(32);
  static final SizeTByReference count = new SizeTByReference();

  static {
    LibGmp.mpz_init(op1);
    LibGmp.mpz_init(op2);
    LibGmp.mpz_init(res);
  }

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
      inA.clear();
      inA.put(aBytes.toArrayUnsafe()).flip();
      inB.clear();
      inB.put(bBytes.toArrayUnsafe()).flip();

      // ---- Import 256-bit unsigned big-endian values into MPZ ----
      // order=1 (most-significant word first), size=1 (word size = 1 byte),
      // endian=1 (big-endian within words), nails=0
      LibGmp.mpz_import(op1, new SizeT(32), 1, one, 1, zero, inA);
      LibGmp.mpz_import(op2, new SizeT(32), 1, one, 1, zero, inB);

      // ---- Compute a mod b (unsigned semantics, result 0 <= r < b) ----
      LibGmp.mpz_mod(res, op1, op2);

      if (LibGmp.mpz_sgn(res) == 0) {
        frame.pushStackItem(Bytes32.ZERO);
        return modSuccess;
      }

      // ---- Export back to big-endian bytes, fit to 32 bytes ----
      out.clear();
      LibGmp.mpz_export(out, count, 1, one, 1, zero, res);

      // Read the exported bytes from the ByteBuffer
      byte[] raw;
      if (out.hasArray()) {
        int offset = out.arrayOffset() + out.position();
        int len = out.remaining();
        raw = Arrays.copyOfRange(out.array(), offset, offset + len);
        out.position(out.limit());
      } else {
        raw = new byte[out.remaining()];
        out.get(raw);
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
