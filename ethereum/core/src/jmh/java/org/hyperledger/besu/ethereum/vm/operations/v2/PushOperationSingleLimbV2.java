package org.hyperledger.besu.ethereum.vm.operations.v2;

import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.Operation;
import org.hyperledger.besu.evm.v2.operation.PushOperationV2;
import org.openjdk.jmh.annotations.Param;

public class PushOperationSingleLimbV2 extends PushOperationBenchmarkBaseV2 {
  @Param({"2", "3", "8", "RANDOM"})
  protected String pushSize;

  @Param protected Position pc;

  @Param({"SMALL", "BIG"})
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
    return new int[] {2, 9};
  }

  @Override
  protected Operation.OperationResult invoke(final MessageFrame frame, final byte[] code, final int pc, final int pushSize) {
    return PushOperationV2.SingleLimb.staticOperation(frame, code, pc, pushSize);
  }
}
