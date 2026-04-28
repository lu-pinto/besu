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
import static org.hyperledger.besu.evm.operation.PushOperation.PUSH_BASE;
import static org.hyperledger.besu.evm.operation.SwapOperation.SWAP_BASE;

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.frame.MessageFrame.State;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.evm.internal.JumpDestOnlyCodeCache;
import org.hyperledger.besu.evm.internal.OverflowException;
import org.hyperledger.besu.evm.internal.UnderflowException;
import org.hyperledger.besu.evm.operation.AddModOperation;
import org.hyperledger.besu.evm.operation.AddModOperationOptimized;
import org.hyperledger.besu.evm.operation.AddOperation;
import org.hyperledger.besu.evm.operation.AddOperationOptimized;
import org.hyperledger.besu.evm.operation.AndOperation;
import org.hyperledger.besu.evm.operation.AndOperationOptimized;
import org.hyperledger.besu.evm.operation.ByteOperation;
import org.hyperledger.besu.evm.operation.ChainIdOperation;
import org.hyperledger.besu.evm.operation.CountLeadingZerosOperation;
import org.hyperledger.besu.evm.operation.DivOperation;
import org.hyperledger.besu.evm.operation.DivOperationOptimized;
import org.hyperledger.besu.evm.operation.DupNOperation;
import org.hyperledger.besu.evm.operation.DupOperation;
import org.hyperledger.besu.evm.operation.ExchangeOperation;
import org.hyperledger.besu.evm.operation.ExpOperation;
import org.hyperledger.besu.evm.operation.GtOperation;
import org.hyperledger.besu.evm.operation.InvalidOperation;
import org.hyperledger.besu.evm.operation.IsZeroOperation;
import org.hyperledger.besu.evm.operation.JumpDestOperation;
import org.hyperledger.besu.evm.operation.JumpOperation;
import org.hyperledger.besu.evm.operation.JumpiOperation;
import org.hyperledger.besu.evm.operation.LtOperation;
import org.hyperledger.besu.evm.operation.ModOperation;
import org.hyperledger.besu.evm.operation.ModOperationOptimized;
import org.hyperledger.besu.evm.operation.MulModOperation;
import org.hyperledger.besu.evm.operation.MulModOperationOptimized;
import org.hyperledger.besu.evm.operation.MulOperation;
import org.hyperledger.besu.evm.operation.MulOperationOptimized;
import org.hyperledger.besu.evm.operation.NotOperation;
import org.hyperledger.besu.evm.operation.NotOperationOptimized;
import org.hyperledger.besu.evm.operation.Operation;
import org.hyperledger.besu.evm.operation.Operation.OperationResult;
import org.hyperledger.besu.evm.operation.OperationRegistry;
import org.hyperledger.besu.evm.operation.OrOperation;
import org.hyperledger.besu.evm.operation.OrOperationOptimized;
import org.hyperledger.besu.evm.operation.PopOperation;
import org.hyperledger.besu.evm.operation.Push0Operation;
import org.hyperledger.besu.evm.operation.PushOperation;
import org.hyperledger.besu.evm.operation.SDivOperation;
import org.hyperledger.besu.evm.operation.SDivOperationOptimized;
import org.hyperledger.besu.evm.operation.SGtOperation;
import org.hyperledger.besu.evm.operation.SLtOperation;
import org.hyperledger.besu.evm.operation.SModOperation;
import org.hyperledger.besu.evm.operation.SModOperationOptimized;
import org.hyperledger.besu.evm.operation.SarOperation;
import org.hyperledger.besu.evm.operation.SarOperationOptimized;
import org.hyperledger.besu.evm.operation.ShlOperation;
import org.hyperledger.besu.evm.operation.ShlOperationOptimized;
import org.hyperledger.besu.evm.operation.ShrOperation;
import org.hyperledger.besu.evm.operation.ShrOperationOptimized;
import org.hyperledger.besu.evm.operation.SignExtendOperation;
import org.hyperledger.besu.evm.operation.StopOperation;
import org.hyperledger.besu.evm.operation.SubOperation;
import org.hyperledger.besu.evm.operation.SubOperationOptimized;
import org.hyperledger.besu.evm.operation.SwapNOperation;
import org.hyperledger.besu.evm.operation.SwapOperation;
import org.hyperledger.besu.evm.operation.VirtualOperation;
import org.hyperledger.besu.evm.operation.XorOperation;
import org.hyperledger.besu.evm.operation.XorOperationOptimized;
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
import java.util.function.Function;

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
    this.endOfScriptStop = new VirtualOperation(new StopOperation(gasCalculator));
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
    Operation op = operations.get(ChainIdOperation.OPCODE);
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
    if (evmConfiguration.enableEvmV2()) {
      runToHaltV2(frame, tracing);
      return;
    }
    evmSpecVersion.maybeWarnVersion();

    var operationTracer = tracing == OperationTracer.NO_TRACING ? null : tracing;
    byte[] code = frame.getCode().getBytes().toArrayUnsafe();
    Operation[] operationArray = operations.getOperations();
    while (frame.getState() == MessageFrame.State.CODE_EXECUTING) {
      Operation currentOperation;
      int opcode;
      int pc = frame.getPC();
      try {
        opcode = code[pc] & 0xff;
        currentOperation = operationArray[opcode];
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
        result = MhDispatcher.dispatch(opcode, frame, currentOperation, this);
      } catch (final OverflowException oe) {
        result = OVERFLOW_RESPONSE;
      } catch (final UnderflowException ue) {
        result = UNDERFLOW_RESPONSE;
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
      if (frame.getState() == State.CODE_EXECUTING) {
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
   * EVM v2 execution loop using long[] stack representation. Only opcodes explicitly listed in the
   * switch are handled via the v2 path; all others fall through to the v1 operation registry. This
   * skeleton stub establishes the dispatch structure for incremental v2 operation rollout.
   */
  // Note: like runToHalt, this is performance-critical code. Benchmark before refactoring.
  private void runToHaltV2(final MessageFrame frame, final OperationTracer tracing) {
    evmSpecVersion.maybeWarnVersion();

    var operationTracer = tracing == OperationTracer.NO_TRACING ? null : tracing;
    byte[] code = frame.getCode().getBytes().toArrayUnsafe();
    Operation[] operationArray = operations.getOperations();
    while (frame.getState() == MessageFrame.State.CODE_EXECUTING) {
      Operation currentOperation;
      int opcode;
      int pc = frame.getPC();
      try {
        opcode = code[pc] & 0xff;
        currentOperation = operationArray[opcode];
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
  public Operation[] getOperationsUnsafe() {
    return operations.getOperations();
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
