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
package org.hyperledger.besu.evm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.frame.MessageFrame.State;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.evm.internal.JumpDestOnlyCodeCache;
import org.hyperledger.besu.evm.internal.OverflowException;
import org.hyperledger.besu.evm.internal.UnderflowException;
import org.hyperledger.besu.evm.operation.ChainIdOperation;
import org.hyperledger.besu.evm.operation.InvalidOperation;
import org.hyperledger.besu.evm.operation.Operation;
import org.hyperledger.besu.evm.operation.Operation.OperationResult;
import org.hyperledger.besu.evm.operation.OperationRegistry;
import org.hyperledger.besu.evm.operation.StopOperation;
import org.hyperledger.besu.evm.operation.VirtualOperation;
import org.hyperledger.besu.evm.tracing.OperationTracer;
import org.hyperledger.besu.evm.v2.operation.AddOperationV2;
import org.hyperledger.besu.evm.v2.operation.DivOperationV2;
import org.hyperledger.besu.evm.v2.operation.ModOperationV2;
import org.hyperledger.besu.evm.v2.operation.MulModOperationV2;
import org.hyperledger.besu.evm.v2.operation.MulOperationV2;
import org.hyperledger.besu.evm.v2.operation.SDivOperationV2;
import org.hyperledger.besu.evm.v2.operation.SModOperationV2;
import org.hyperledger.besu.evm.v2.operation.SarOperationV2;
import org.hyperledger.besu.evm.v2.operation.ShlOperationV2;
import org.hyperledger.besu.evm.v2.operation.ShrOperationV2;
import org.hyperledger.besu.evm.v2.operation.SubOperationV2;

import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.tuweni.bytes.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Evm. */
public class EVM {
  private static final Logger LOG = LoggerFactory.getLogger(EVM.class);

  /** The constant OVERFLOW_RESPONSE. */
  protected static final OperationResult OVERFLOW_RESPONSE =
      new OperationResult(0L, ExceptionalHaltReason.TOO_MANY_STACK_ITEMS);

  /** The constant UNDERFLOW_RESPONSE. */
  protected static final OperationResult UNDERFLOW_RESPONSE =
      new OperationResult(0L, ExceptionalHaltReason.INSUFFICIENT_STACK_ITEMS);

  private final OperationRegistry operations;
  private final GasCalculator gasCalculator;
  private final Operation endOfScriptStop;
  private final EvmConfiguration evmConfiguration;
  private final EvmSpecVersion evmSpecVersion;

  // Optimized operation flags
  private final boolean enableConstantinople;

  private final JumpDestOnlyCodeCache jumpDestOnlyCodeCache;

  /**
   * Instantiates a new Evm.
   *
   * @param operations the operations
   * @param gasCalculator the gas calculator
   * @param evmConfiguration the evm configuration
   * @param evmSpecVersion the evm spec version
   */
  public EVM(
      final OperationRegistry operations,
      final GasCalculator gasCalculator,
      final EvmConfiguration evmConfiguration,
      final EvmSpecVersion evmSpecVersion) {
    this.operations = operations;
    this.gasCalculator = gasCalculator;
    this.endOfScriptStop = new VirtualOperation(new StopOperation());
    this.evmConfiguration = evmConfiguration;
    this.evmSpecVersion = evmSpecVersion;
    this.jumpDestOnlyCodeCache = new JumpDestOnlyCodeCache(evmConfiguration);

    enableConstantinople = EvmSpecVersion.CONSTANTINOPLE.ordinal() <= evmSpecVersion.ordinal();
  }

  /**
   * Gets gas calculator.
   *
   * @return the gas calculator
   */
  public GasCalculator getGasCalculator() {
    return gasCalculator;
  }

  /**
   * Gets the max code size, taking configuration and version into account
   *
   * @return The max code size override, if not set the max code size for the EVM version.
   */
  public int getMaxCodeSize() {
    return evmConfiguration.maxCodeSizeOverride().orElse(evmSpecVersion.maxCodeSize);
  }

  /**
   * Gets the max initcode Size, taking configuration and version into account
   *
   * @return The max initcode size override, if not set the max initcode size for the EVM version.
   */
  public int getMaxInitcodeSize() {
    return evmConfiguration.maxInitcodeSizeOverride().orElse(evmSpecVersion.maxInitcodeSize);
  }

  /**
   * Returns the non-fork related configuration parameters of the EVM.
   *
   * @return the EVM configuration.
   */
  public EvmConfiguration getEvmConfiguration() {
    return evmConfiguration;
  }

  /**
   * Returns the configured EVM spec version for this EVM
   *
   * @return the evm spec version
   */
  public EvmSpecVersion getEvmVersion() {
    return evmSpecVersion;
  }

  /**
   * Return the ChainId this Executor is using, or empty if the EVM version does not expose chain
   * ID.
   *
   * @return the ChainId, or empty if not exposed.
   */
  public Optional<Bytes> getChainId() {
    Operation op = operations.getOperation(ChainIdOperation.OPCODE);
    if (op instanceof ChainIdOperation chainIdOperation) {
      return Optional.of(chainIdOperation.getChainId());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Run to halt.
   *
   * @param frame the frame
   * @param tracing the tracing
   */
  // Note to maintainers: lots of Java idioms and OO principals are being set aside in the
  // name of performance. This is one of the hottest sections of code.
  //
  // Please benchmark before refactoring.
  public void runToHalt(final MessageFrame frame, final OperationTracer tracing) {
//    if (evmConfiguration.enableEvmV2()) {
//      runToHaltV2(frame, tracing);
//      return;
//    }
    evmSpecVersion.maybeWarnVersion();

    var operationTracer = tracing == OperationTracer.NO_TRACING ? null : tracing;
    byte[] code = frame.getCode().getBytes().toArrayUnsafe();
    while (frame.getState() == MessageFrame.State.CODE_EXECUTING) {
      // tracing not supported
      if (operationTracer == null) {
        try {
          if (runFusedCode(frame, code)) {
            continue;
          }
        } catch (ArrayIndexOutOfBoundsException aiiobe) {
          ExceptionalHaltReason reason;
          if (frame.getStack().isFull()) {
            reason = ExceptionalHaltReason.TOO_MANY_STACK_ITEMS;
          } else if (frame.getStack().isEmpty()) {
            reason = ExceptionalHaltReason.INSUFFICIENT_STACK_ITEMS;
          } else {
            throw aiiobe;
          }
          LOG.trace("MessageFrame evaluation halted because of {}", reason);
          frame.setExceptionalHaltReason(Optional.of(reason));
          frame.setState(State.EXCEPTIONAL_HALT);
          return;
        }
      }

      int opcode = code[frame.getPC()] & 0xff;
      Operation currentOperation = operations.getOperation(opcode);
      frame.setCurrentOperation(currentOperation);
      if (operationTracer != null) {
        operationTracer.tracePreExecution(frame);
      }

      OperationResult result;
      try {
        result = MhDispatcher.dispatch(opcode, frame, currentOperation, this);
      } catch (ArrayIndexOutOfBoundsException aiiobe) {
        if (frame.getStack().isFull()) {
          result = OVERFLOW_RESPONSE;
        } else if (frame.getStack().isEmpty()) {
          result = UNDERFLOW_RESPONSE;
        } else {
          throw aiiobe;
        }
      }
      final ExceptionalHaltReason haltReason = result.getHaltReason();
      if (haltReason != null) {
        LOG.trace("MessageFrame evaluation halted because of {}", haltReason);
        frame.setExceptionalHaltReason(Optional.of(haltReason));
        frame.setState(State.EXCEPTIONAL_HALT);
      } else if (frame.decrementRemainingGas(result.getGasCost()) < 0) {
        frame.setExceptionalHaltReason(Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
        frame.setState(State.EXCEPTIONAL_HALT);
      }

      final int currentPC = frame.getPC();
      final int opSize = result.getPcIncrement();
      frame.setPC(currentPC + opSize);

      if (operationTracer != null) {
        operationTracer.tracePostExecution(frame, result);
      }
    }
  }

  private int getNextPc(final MessageFrame frame, final byte[] code, final int pcStart) {
    int pc = pcStart;
    int cost;
    int totalCost = 0;
    int opcode = pc >= code.length ? StopOperation.OPCODE : code[pc] & 0xff;

    while ((cost = operations.getStaticGas(opcode)) != -1) {
      totalCost += cost;
      int pcIncrement;
      if ((pcIncrement = operations.getPcIncrement(opcode)) == -1 || opcode == StopOperation.OPCODE) {
        // break here as we don't know where we are going to land without executing
        // OR we are stopping
        pc++;
        break;
      }
      pc += pcIncrement;
      opcode = pc >= code.length ? StopOperation.OPCODE : code[pc] & 0xff;
    }

    if (frame.decrementRemainingGas(totalCost) < 0) {
      frame.setExceptionalHaltReason(Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
      frame.setState(State.EXCEPTIONAL_HALT);
      return -1;
    }
    return pc;
  }

  private boolean runFusedCode(final MessageFrame frame, final byte[] code) {
    int pc = frame.getPC();
    int endPc = getNextPc(frame, code, pc);
    boolean status = endPc < 0;
    while (pc < endPc) {
      int opcode = pc >= code.length ? StopOperation.OPCODE : code[pc] & 0xff;
      Operation currentOperation = operations.getOperation(opcode);
      int pcIncrement = operations.getPcIncrement(opcode);
      switch (opcode) {
        case 0x00 -> {
          status = true;
          currentOperation.execute(frame, this);
        }
        case 0x01 -> currentOperation.execute(frame, this);
        case 0x02 -> currentOperation.execute(frame, this);
        case 0x03 -> currentOperation.execute(frame, this);
        case 0x04 -> currentOperation.execute(frame, this);
        case 0x05 -> currentOperation.execute(frame, this);
        case 0x06 -> currentOperation.execute(frame, this);
        case 0x07 -> currentOperation.execute(frame, this);
        case 0x08 -> currentOperation.execute(frame, this);
        case 0x09 -> currentOperation.execute(frame, this);
        case 0x0b -> currentOperation.execute(frame, this);
        case 0x10 -> currentOperation.execute(frame, this);
        case 0x11 -> currentOperation.execute(frame, this);
        case 0x12 -> currentOperation.execute(frame, this);
        case 0x13 -> currentOperation.execute(frame, this);
        case 0x14 -> currentOperation.execute(frame, this);
        case 0x15 -> currentOperation.execute(frame, this);
        case 0x16 -> currentOperation.execute(frame, this);
        case 0x17 -> currentOperation.execute(frame, this);
        case 0x18 -> currentOperation.execute(frame, this);
        case 0x19 -> currentOperation.execute(frame, this);
        case 0x1a -> currentOperation.execute(frame, this);
        case 0x1b -> currentOperation.execute(frame, this);
        case 0x1c -> currentOperation.execute(frame, this);
        case 0x1d -> currentOperation.execute(frame, this);
        case 0x1e -> currentOperation.execute(frame, this);
        case 0x30 -> currentOperation.execute(frame, this);
        case 0x32 -> currentOperation.execute(frame, this);
        case 0x33 -> currentOperation.execute(frame, this);
        case 0x34 -> currentOperation.execute(frame, this);
        case 0x35 -> currentOperation.execute(frame, this);
        case 0x36 -> currentOperation.execute(frame, this);
        case 0x38 -> currentOperation.execute(frame, this);
        case 0x3a -> currentOperation.execute(frame, this);
        case 0x3d -> currentOperation.execute(frame, this);
        case 0x40 -> currentOperation.execute(frame, this);
        case 0x41 -> currentOperation.execute(frame, this);
        case 0x42 -> currentOperation.execute(frame, this);
        case 0x43 -> currentOperation.execute(frame, this);
        case 0x44 -> currentOperation.execute(frame, this);
        case 0x45 -> currentOperation.execute(frame, this);
        case 0x46 -> currentOperation.execute(frame, this);
        case 0x47 -> currentOperation.execute(frame, this);
        case 0x48 -> currentOperation.execute(frame, this);
        case 0x49 -> currentOperation.execute(frame, this);
        case 0x4a -> currentOperation.execute(frame, this);
        case 0x4b -> currentOperation.execute(frame, this);
        case 0x50 -> currentOperation.execute(frame, this);
     	// JUMP and JUMPI
        case 0x56, 0x57 -> {
          var result = currentOperation.execute(frame, this);
          endPc = pc;
          pcIncrement = 0;
          status = true;
          if (result.getHaltReason() == null) {
             pcIncrement = result.getPcIncrement();
             endPc = getNextPc(frame, code, pc + pcIncrement);
          } else {
            frame.setExceptionalHaltReason(Optional.of(result.getHaltReason()));
            frame.setState(State.EXCEPTIONAL_HALT);
          }
        }
        case 0x58 -> currentOperation.execute(frame, this);
        case 0x5a -> currentOperation.execute(frame, this);
        case 0x5b -> currentOperation.execute(frame, this);
        case 0x5f -> currentOperation.execute(frame, this);
        // PUSH1-32
        case 0x60,
             0x61,
             0x62,
             0x63,
             0x64,
             0x65,
             0x66,
             0x67,
             0x68,
             0x69,
             0x6a,
             0x6b,
             0x6c,
             0x6d,
             0x6e,
             0x6f,
             0x70,
             0x71,
             0x72,
             0x73,
             0x74,
             0x75,
             0x76,
             0x77,
             0x78,
             0x79,
             0x7a,
             0x7b,
             0x7c,
             0x7d,
             0x7e,
             0x7f -> currentOperation.execute(frame, this);
        // DUP1-16
        case 0x80,
             0x81,
             0x82,
             0x83,
             0x84,
             0x85,
             0x86,
             0x87,
             0x88,
             0x89,
             0x8a,
             0x8b,
             0x8c,
             0x8d,
             0x8e,
             0x8f -> currentOperation.execute(frame, this);
        // SWAP1-16
        case 0x90,
             0x91,
             0x92,
             0x93,
             0x94,
             0x95,
             0x96,
             0x97,
             0x98,
             0x99,
             0x9a,
             0x9b,
             0x9c,
             0x9d,
             0x9e,
             0x9f -> currentOperation.execute(frame, this);
        // DUPN (EIP-8024)
        case 0xe6 -> currentOperation.execute(frame, this);
        // SWAPN (EIP-8024)
        case 0xe7 -> currentOperation.execute(frame, this);
        // EXCHANGE (EIP-8024)
        case 0xe8 -> currentOperation.execute(frame, this);
        default -> throw new IllegalStateException("invalid static opcode 0x" + Integer.toHexString(opcode));
      };
      pc += pcIncrement;
      frame.setPC(pc);
    }

    return status;
  }

  /**
   * EVM v2 execution loop using long[] stack representation. Only opcodes explicitly listed in the
   * switch are handled via the v2 path; all others fall through to the v1 operation registry. This
   * skeleton stub establishes the dispatch structure for incremental v2 operation rollout.
   */
  // Note: like runToHalt, this is performance-critical code. Benchmark before refactoring.
  @SuppressWarnings("unused")
  private void runToHaltV2(final MessageFrame frame, final OperationTracer tracing) {
    evmSpecVersion.maybeWarnVersion();

    var operationTracer = tracing == OperationTracer.NO_TRACING ? null : tracing;
    byte[] code = frame.getCode().getBytes().toArrayUnsafe();
    while (frame.getState() == MessageFrame.State.CODE_EXECUTING) {
      Operation currentOperation;
      int opcode;
      int pc = frame.getPC();
      try {
        opcode = code[pc] & 0xff;
        currentOperation = operations.getOperation(opcode);
      } catch (ArrayIndexOutOfBoundsException aiiobe) {
        opcode = 0;
        currentOperation = endOfScriptStop;
      }
      frame.setCurrentOperation(currentOperation);
      if (operationTracer != null) {
        operationTracer.tracePreExecution(frame);
      }

      OperationResult result;
      try {
        result =
            switch (opcode) {
              case 0x01 -> AddOperationV2.staticOperation(frame);
              case 0x02 -> MulOperationV2.staticOperation(frame);
              case 0x03 -> SubOperationV2.staticOperation(frame);
              case 0x04 -> DivOperationV2.staticOperation(frame);
              case 0x05 -> SDivOperationV2.staticOperation(frame);
              case 0x06 -> ModOperationV2.staticOperation(frame);
              case 0x07 -> SModOperationV2.staticOperation(frame);
              case 0x09 -> MulModOperationV2.staticOperation(frame);
              case 0x1b ->
                  enableConstantinople
                      ? ShlOperationV2.staticOperation(frame)
                      : InvalidOperation.invalidOperationResult(opcode);
              case 0x1c ->
                  enableConstantinople
                      ? ShrOperationV2.staticOperation(frame)
                      : InvalidOperation.invalidOperationResult(opcode);
              case 0x1d ->
                  enableConstantinople
                      ? SarOperationV2.staticOperation(frame)
                      : InvalidOperation.invalidOperationResult(opcode);
              // TODO EVMv2: implement remaining opcodes in v2; until then fall through to v1
              default -> {
                frame.setCurrentOperation(currentOperation);
                yield currentOperation.execute(frame, this);
              }
            };
      } catch (final OverflowException oe) {
        result = OVERFLOW_RESPONSE;
      } catch (final UnderflowException ue) {
        result = UNDERFLOW_RESPONSE;
      }
      final ExceptionalHaltReason haltReason = result.getHaltReason();
      if (haltReason != null) {
        LOG.trace("MessageFrame evaluation halted because of {}", haltReason);
        frame.setExceptionalHaltReason(Optional.of(haltReason));
        frame.setState(MessageFrame.State.EXCEPTIONAL_HALT);
      } else if (frame.decrementRemainingGas(result.getGasCost()) < 0) {
        frame.setExceptionalHaltReason(Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
        frame.setState(MessageFrame.State.EXCEPTIONAL_HALT);
      }
      if (frame.getState() == MessageFrame.State.CODE_EXECUTING) {
        final int currentPC = frame.getPC();
        final int opSize = result.getPcIncrement();
        frame.setPC(currentPC + opSize);
      }
      if (operationTracer != null) {
        operationTracer.tracePostExecution(frame, result);
      }
    }
  }

  /**
   * Get Operations (unsafe)
   *
   * @return Operations array
   */
  @VisibleForTesting
  public Operation[] getOperations() {
    return IntStream.range(0, OperationRegistry.NUM_OPERATIONS)
      .mapToObj(operations::getOperation)
      .toArray(Operation[]::new);
  }

  public int getStaticGas(final int opcode) {
    return operations.getStaticGas(opcode);
  }

  /**
   * Gets or creates code instance with a cached jump destination.
   *
   * @param codeHash the code hash
   * @param codeBytes the code bytes
   * @return the code instance with the cached jump destination
   */
  public Code getOrCreateCachedJumpDest(final Hash codeHash, final Bytes codeBytes) {
    checkNotNull(codeHash);

    Code result = jumpDestOnlyCodeCache.getIfPresent(codeHash);
    if (result == null) {
      result = new Code(codeBytes);
      jumpDestOnlyCodeCache.put(codeHash, result);
    }

    return result;
  }
}
