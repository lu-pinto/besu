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
package org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods;

import org.hyperledger.besu.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionResult;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.FourByteTracerResultConverter;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.OpCodeLoggerTracerResult;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.calltrace.CallTracer;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.diff.StateDiffTrace;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.diff.StateTraceGenerator;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.tracing.diff.StateTraceResult;
import org.hyperledger.besu.ethereum.debug.TraceOptions;
import org.hyperledger.besu.ethereum.debug.TracerType;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSpec;
import org.hyperledger.besu.ethereum.vm.DebugOperationTracer;
import org.hyperledger.besu.evm.tracing.OperationTracer;

import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonGetter;

/**
 * Encapsulates both the {@link OperationTracer} and the logic to build a {@link
 * DebugTraceTransactionResult} for a debug trace request.
 */
public interface DebugTraceTransactionStep
    extends Function<TransactionTrace, DebugTraceTransactionResult> {

  /**
   * The operation tracer to drive transaction execution with.
   *
   * @return the operation tracer
   */
  OperationTracer getOperationTracer();

  /**
   * Builds the debug trace result from the completed transaction execution trace.
   *
   * @param transactionTrace the trace produced by executing the transaction
   * @return the completed debug trace transaction result
   */
  DebugTraceTransactionResult buildResult(TransactionTrace transactionTrace);

  @Override
  default DebugTraceTransactionResult apply(final TransactionTrace transactionTrace) {
    return buildResult(transactionTrace);
  }

  /**
   * Creates a {@link DebugTraceTransactionStep} for the given trace options and protocol spec,
   * recording child call gas by default.
   *
   * @param traceOptions the trace options
   * @param protocolSpec the protocol spec
   * @return the step
   */
  static DebugTraceTransactionStep of(
      final TraceOptions traceOptions, final ProtocolSpec protocolSpec) {
    return of(traceOptions, protocolSpec, true);
  }

  /**
   * Creates a {@link DebugTraceTransactionStep} for the given trace options, protocol spec, and
   * child call gas recording flag.
   *
   * @param traceOptions the trace options
   * @param protocolSpec the protocol spec
   * @param recordChildCallGas whether opcode tracers should record child call gas
   * @return the step
   */
  static DebugTraceTransactionStep of(
      final TraceOptions traceOptions,
      final ProtocolSpec protocolSpec,
      final boolean recordChildCallGas) {
    return switch (traceOptions.tracerType()) {
      case CALL_TRACER -> {
        final CallTracer tracer = new CallTracer(traceOptions);
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(
                trace, tracer.buildResult(trace.getTransaction(), trace.getResult()));
          }
        };
      }
      case OPCODE_TRACER -> {
        final DebugOperationTracer tracer =
            new DebugOperationTracer(traceOptions.opCodeTracerConfig(), recordChildCallGas);
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(
                trace, new OpCodeLoggerTracerResult(trace, tracer.isLimitReached()));
          }
        };
      }
      case PRESTATE_TRACER -> {
        final DebugOperationTracer tracer =
            new DebugOperationTracer(traceOptions.opCodeTracerConfig(), recordChildCallGas);
        final var generator = new StateTraceGenerator();
        final boolean diffMode =
            Boolean.TRUE.equals(traceOptions.tracerConfig().getOrDefault("diffMode", false));
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            final StateDiffTrace diffTrace =
                (diffMode ? generator.generateStateDiff(trace) : generator.generatePreState(trace))
                    .findFirst()
                    .orElseGet(StateDiffTrace::new);
            return new DebugTraceTransactionResult(
                trace, new StateTraceResult(diffTrace, diffMode));
          }
        };
      }
      case FOUR_BYTE_TRACER -> {
        final DebugOperationTracer tracer =
            new DebugOperationTracer(traceOptions.opCodeTracerConfig(), recordChildCallGas);
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(
                trace, FourByteTracerResultConverter.convert(trace, protocolSpec));
          }
        };
      }
      case FLAT_CALL_TRACER -> {
        final DebugOperationTracer tracer =
            new DebugOperationTracer(traceOptions.opCodeTracerConfig(), recordChildCallGas);
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(trace, new UnimplementedTracerResult());
          }
        };
      }
    };
  }

  /**
   * Binds an externally provided {@link OperationTracer} to a result builder step.
   *
   * @param traceOptions the trace options
   * @param protocolSpec the protocol spec
   * @param tracer the operation tracer used to execute the transaction
   * @return the step
   */
  static DebugTraceTransactionStep of(
      final TraceOptions traceOptions,
      final ProtocolSpec protocolSpec,
      final OperationTracer tracer) {
    return switch (traceOptions.tracerType()) {
      case CALL_TRACER -> {
        if (!(tracer instanceof CallTracer callTracer)) {
          throw new IllegalArgumentException("CALL_TRACER requires CallTracer");
        }
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return callTracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(
                trace, callTracer.buildResult(trace.getTransaction(), trace.getResult()));
          }
        };
      }
      case OPCODE_TRACER -> {
        if (!(tracer instanceof DebugOperationTracer debugTracer)) {
          throw new IllegalArgumentException("OPCODE_TRACER requires DebugOperationTracer");
        }
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return debugTracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            return new DebugTraceTransactionResult(
                trace, new OpCodeLoggerTracerResult(trace, debugTracer.isLimitReached()));
          }
        };
      }
      case PRESTATE_TRACER -> {
        final var generator = new StateTraceGenerator();
        final boolean diffMode =
            Boolean.TRUE.equals(traceOptions.tracerConfig().getOrDefault("diffMode", false));
        yield new DebugTraceTransactionStep() {
          @Override
          public OperationTracer getOperationTracer() {
            return tracer;
          }

          @Override
          public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
            final StateDiffTrace diffTrace =
                (diffMode ? generator.generateStateDiff(trace) : generator.generatePreState(trace))
                    .findFirst()
                    .orElseGet(StateDiffTrace::new);
            return new DebugTraceTransactionResult(
                trace, new StateTraceResult(diffTrace, diffMode));
          }
        };
      }
      case FOUR_BYTE_TRACER ->
          new DebugTraceTransactionStep() {
            @Override
            public OperationTracer getOperationTracer() {
              return tracer;
            }

            @Override
            public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
              return new DebugTraceTransactionResult(
                  trace, FourByteTracerResultConverter.convert(trace, protocolSpec));
            }
          };
      case FLAT_CALL_TRACER ->
          new DebugTraceTransactionStep() {
            @Override
            public OperationTracer getOperationTracer() {
              return tracer;
            }

            @Override
            public DebugTraceTransactionResult buildResult(final TransactionTrace trace) {
              return new DebugTraceTransactionResult(trace, new UnimplementedTracerResult());
            }
          };
    };
  }

  /**
   * Helper to create an {@link OperationTracer} directly for callers that only require execution
   * tracing without result building.
   *
   * @param traceOptions the trace options
   * @param recordChildCallGas whether opcode tracers should record child call gas
   * @return the operation tracer
   */
  static OperationTracer createTracer(
      final TraceOptions traceOptions, final boolean recordChildCallGas) {
    if (traceOptions.tracerType() == TracerType.CALL_TRACER) {
      return new CallTracer(traceOptions);
    }
    return new DebugOperationTracer(traceOptions.opCodeTracerConfig(), recordChildCallGas);
  }

  class UnimplementedTracerResult {
    @JsonGetter("error")
    public String getError() {
      return "Not Yet Implemented";
    }
  }
}
