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
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

/**
 * The DUPN operation (EIP-8024).
 *
 * <p>Duplicates the n'th stack item to the top of the stack, where n is decoded from a 1-byte
 * immediate operand. This extends the functionality of DUP1-DUP16 to allow accessing stack items at
 * depths 17-235.
 *
 * <p>The immediate operand uses a special encoding to preserve backward compatibility by avoiding
 * bytes that could be confused with JUMPDEST (0x5b) or PUSH opcodes (0x60-0x7f).
 */
public class DupNOperation extends AbstractOperation {

  /** The DUPN opcode value. */
  public static final int OPCODE = 0xe6;

  /**
   * Instantiates a new DUPN operation.
   *
   */
  public DupNOperation() {
    super(OPCODE, "DUPN", 0, 1, null);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final int pc = frame.getPC();
    //TODO: make MessageFrame::code a byte[] directly
    final byte[] code = frame.getCode().getBytes().toArrayUnsafe();
    // Get immediate byte, treating end-of-code as 0
    final int imm = (pc + 1 >= code.length) ? 0 : code[pc + 1] & 0xFF;

    // Check for invalid immediate range (91-127)
    if (!Eip8024Decoder.VALID_SINGLE[imm]) {
      return new OperationResult(0, ExceptionalHaltReason.INVALID_OPERATION);
    }

    final int n = Eip8024Decoder.DECODE_SINGLE[imm];

    // Duplicate the n'th stack item (1-indexed) to the top
    // In Besu's 0-indexed stack, the n'th item is at index n-1
    frame.pushStackItem(frame.getStackItem(n - 1));
    return new OperationResult();
  }

  /**
   * Decodes a single immediate byte to the stack index n.
   *
   * @param imm the immediate byte value (0-255)
   * @return the decoded n value, or -1 if the immediate is invalid
   */
  public static int decodeSingle(final int imm) {
    return Eip8024Decoder.decodeSingle(imm);
  }
}
