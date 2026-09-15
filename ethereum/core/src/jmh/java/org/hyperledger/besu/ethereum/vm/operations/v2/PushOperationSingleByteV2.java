package org.hyperledger.besu.ethereum.vm.operations.v2;

import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.Operation;
import org.hyperledger.besu.evm.v2.operation.PushOperationV2;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.infra.Blackhole;

public class PushOperationSingleByteV2 extends PushOperationBenchmarkBaseV2 {
  @Param({"0", "1", "RANDOM"})
  protected String pushSize;

  @Param
  protected Position pc;

  @Param({"SMALL"})
  private String codeSize;

  @Override
  protected Position getPc() {
    return pc;
  }

  @Override
  protected String getPushSize() {
    return pushSize;
  }

  @Override
  protected String getCodeSize() {
    return codeSize;
  }

  @Override
  protected int[] getPushRandomization() {
    return new int[] {0, 2};
  }

  @Override
  protected Operation.OperationResult invoke(final MessageFrame frame, final byte[] code, final int pc,
                                             final int pushSize) {
    return PushOperationV2.SingleByte.staticOperation(frame, code, pc, pushSize);
  }
}
