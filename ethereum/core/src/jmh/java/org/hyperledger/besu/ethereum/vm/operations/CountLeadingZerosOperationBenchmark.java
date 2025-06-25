package org.hyperledger.besu.ethereum.vm.operations;

import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.code.CodeV0;
import org.hyperledger.besu.evm.frame.BlockValues;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.CountLeadingZerosOperation;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
@OutputTimeUnit(value = TimeUnit.MILLISECONDS)
public class CountLeadingZerosOperationBenchmark {

  @Param({
    "true",
    "false"
  })
  public String useBase32Wrapping;

  @Param({
    "0x23",
    "0x2323232323232323232323232323232323232323232323232323232323232323",
//    "0x23232323232323232323232323232323232323232323232323232323232323",
//    "0x2323232323232323232323232323232323232323232323",
    "0x232323232323232323232323232323",
  })
  public String bytesHex;

  private Bytes bytes;

  private MessageFrame frame;

  @Setup
  public void setUp() {
    frame = MessageFrame.builder()
      .worldUpdater(mock(WorldUpdater.class))
        .originator(Address.ZERO)
        .gasPrice(Wei.ONE)
        .blobGasPrice(Wei.ONE)
      .blockValues(mock(BlockValues.class))
        .miningBeneficiary(Address.ZERO)
        .blockHashLookup((__, ___) -> Hash.ZERO)
        .type(MessageFrame.Type.MESSAGE_CALL)
        .initialGas(1)
        .address(Address.ZERO)
        .contract(Address.ZERO)
        .inputData(Bytes32.ZERO)
        .sender(Address.ZERO)
        .value(Wei.ZERO)
        .apparentValue(Wei.ZERO)
        .code(CodeV0.EMPTY_CODE)
        .completer(messageFrame -> {})
        .build();
    bytes = Bytes.fromHexString(bytesHex);
    CountLeadingZerosOperation.setAlternativeImpl(Boolean.parseBoolean(useBase32Wrapping));
  }

  @Benchmark
  public void executeOperation() {
    for (int i = 0; i < 1000000; i++) {
      frame.pushStackItem(bytes);
      CountLeadingZerosOperation.staticOperation(frame);
      frame.popStackItem();
    }
  }

}
