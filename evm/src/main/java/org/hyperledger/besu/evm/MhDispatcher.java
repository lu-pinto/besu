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
package org.hyperledger.besu.evm;

import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.Operation;
import org.hyperledger.besu.evm.operation.Operation.OperationResult;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * Static MethodHandles.tableSwitch-based dispatcher (JDK 17+) that routes every opcode through
 * {@link Operation#execute}.
 *
 * <p>Drop-in alternative to the {@code switch} block in {@code EVM.runToHalt} (the EVM v1 path,
 * i.e. when {@code enableEvmV2} is false) for A/B comparison with the JIT's tableswitch lowering.
 *
 * <h2>What this dispatches</h2>
 *
 * <p>Every table entry points to the same {@link MethodHandle}: an unbound virtual handle for
 * {@code Operation.execute(MessageFrame, EVM)} obtained via
 * {@link MethodHandles.Lookup#findVirtual}, with arguments permuted to match the dispatcher's
 * {@code (int, MessageFrame, Operation, EVM) -> OperationResult} shape. At call time, the
 * {@code Operation} parameter passed by the caller becomes the receiver of the virtual call.
 *
 * <p>Functionally this is equivalent to writing
 * {@code currentOperation.execute(frame, evm)} in every {@code case} arm of the source switch.
 *
 * <h2>Cost vs. the source switch — read this before benchmarking</h2>
 *
 * <p>Going through {@link Operation#execute} statically <b>loses per-opcode specialisation</b>.
 * The source v1 switch has a separate {@code case} arm per opcode, and C2 collects an
 * <em>independent receiver type profile</em> per arm — that's why you see
 * {@code AddOperationOptimized.staticOperation} inlined at 0x01, {@code MulOperationOptimized}
 * inlined at 0x02, and so on in the disassembly. Here, every table slot is the same MH, so after
 * the tableswitch unfolds there is exactly <em>one</em> {@code Operation.execute} call site shared
 * by all 256 opcodes. With ~200 distinct {@code Operation} subclasses across a hardfork, that site
 * is megamorphic and C2 cannot inline through it.
 *
 * <p>Expect this dispatcher to be <b>slower</b> than the source switch on real workloads,
 * regardless of how well the MH chain itself folds. The only way to recover per-opcode
 * specialisation while still using {@code Operation::execute} is to bake the receiver
 * <em>per-opcode</em> into the table — which requires the dispatcher to be built from a specific
 * {@link Operation} registry (i.e. per-EVM-instance, not static). See git history of this file for
 * that variant; it requires {@code -XX:+TrustFinalNonStaticFields} or {@code @Stable} to fold the
 * instance-field load.
 *
 * <h2>Why a static final field still matters</h2>
 *
 * <p>Even though the underlying call is polymorphic, the MH chain root must still be a JVM
 * compile-time constant for {@link MethodHandle#invokeExact} to inline at all. Storing
 * {@link #DISPATCHER} in a {@code static final} field guarantees that. Without it you get
 * {@code MethodHandle::invokeBasic ... receiver not constant} from the inlining log and the entire
 * table-switch turns into an adapter trampoline.
 */
public final class MhDispatcher {

  private MhDispatcher() {}

  /** Dispatcher signature: (opcode, frame, currentOperation, evm) -&gt; OperationResult. */
  private static final MethodType DISPATCH_TYPE =
      MethodType.methodType(
          OperationResult.class, int.class, MessageFrame.class, Operation.class, EVM.class);

  /**
   * Final root of the MH chain. C2 specialises {@link MethodHandle#invokeExact} through this when
   * called from {@link #dispatch}. The targets all point to the same unbound-virtual handle for
   * {@link Operation#execute}, so the compiled code reduces to a tableswitch followed by a single
   * shared polymorphic call.
   */
  private static final MethodHandle DISPATCHER = build();

  /**
   * Single dispatch entry point. Invoke from the v1 {@code runToHalt} loop in place of the
   * source-level {@code switch}.
   *
   * @param opcode the EVM opcode (0..255)
   * @param frame the message frame
   * @param currentOperation the operation looked up in the registry for this opcode; receiver of
   *     the virtual {@code execute} call
   * @param evm the EVM instance, passed to {@code execute}
   * @return the OperationResult for the executed opcode
   */
  public static OperationResult dispatch(
      final int opcode,
      final MessageFrame frame,
      final Operation currentOperation,
      final EVM evm) {
    try {
      return (OperationResult) DISPATCHER.invokeExact(opcode, frame, currentOperation, evm);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      // invokeExact declares Throwable; opcode handlers don't throw checked exceptions in practice.
      throw new IllegalStateException("Unexpected checked throwable from opcode handler", t);
    }
  }

  /** Build the table at class-init time. Runs once. */
  private static MethodHandle build() {
    final MethodHandles.Lookup lookup = MethodHandles.lookup();

    // Unbound virtual handle for Operation.execute:
    //   (Operation receiver, MessageFrame frame, EVM evm) -> OperationResult
    final MethodHandle execute;
    try {
      execute =
          lookup.findVirtual(
              Operation.class,
              "execute",
              MethodType.methodType(OperationResult.class, MessageFrame.class, EVM.class));
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }

    // Adapt to the dispatcher shape (int, MessageFrame, Operation, EVM) -> OperationResult.
    //
    // Source MT positions:  [0=Operation, 1=MessageFrame, 2=EVM]
    // Target MT positions:  [0=int,        1=MessageFrame, 2=Operation, 3=EVM]
    //
    // For each source-MT position, give the target-MT index that supplies its argument:
    //   src[0] (Operation)    <- tgt[2]
    //   src[1] (MessageFrame) <- tgt[1]
    //   src[2] (EVM)          <- tgt[3]
    // tgt[0] (int) is unused — permuteArguments drops it implicitly.
    final MethodHandle target = MethodHandles.permuteArguments(execute, DISPATCH_TYPE, 2, 1, 3);

    // Every opcode points to the same MH. tableSwitch is technically redundant for correctness —
    // we keep it so the dispatcher shape is comparable to the source switch's tableswitch
    // lowering. The default arm is the same target.
    final MethodHandle[] targets = new MethodHandle[256];
    Arrays.fill(targets, target);

    return MethodHandles.tableSwitch(target, targets);
  }
}
