package org.hyperledger.besu.datatypes;

import java.math.BigInteger;

import it.unich.jgmp.MPZ;
import org.apache.tuweni.bytes.AbstractBytes;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.MutableBytes;

public class UInt256New extends AbstractBytes {
  private static final BigInteger MASK_256_BITS = BigInteger.valueOf(2).pow(256).subtract(BigInteger.ONE);
  private static final BigInteger MOD_BASE = BigInteger.TWO.pow(256);

  private final BigInteger value;

  private UInt256New(final byte[] byteArray) {
    this(new BigInteger(1, byteArray));
  }

  private UInt256New(final BigInteger value) {
    BigInteger newValue = value;
    if (newValue.bitCount() > 32 * 8) {
      newValue = newValue.and(MASK_256_BITS);
    }
    this.value = newValue;
  }

  public static UInt256New fromArray(final byte[] byteArray) {
    return new UInt256New(byteArray);
  }

  public UInt256New subtract(final UInt256New y) {
    return new UInt256New(value.subtract(y.value));
  }

  public UInt256New add(final UInt256New y) {
    return new UInt256New(value.add(y.value));
  }

  public UInt256New mod(final UInt256New y) {
    final MPZ op1 = new MPZ(value);
    final MPZ op2 = new MPZ(y.value);
    return new UInt256New(op1.mod(op2).getBigInteger());
  }

  public UInt256New mul(final UInt256New y) {
    final MPZ op1 = new MPZ(value);
    final MPZ op2 = new MPZ(y.value);
    return new UInt256New(op1.mul(op2).getBigInteger());
  }

  public UInt256New exp(final UInt256New y) {
    final MPZ op = new MPZ(value);
    final MPZ exp = new MPZ(y.value);
    final MPZ mod = new MPZ(MOD_BASE);

    return new UInt256New(op.powmAssign(exp, mod).getBigInteger());
  }

  public Bytes divide(final UInt256New y) {
    final MPZ op1 = new MPZ(value);
    final MPZ op2 = new MPZ(y.value);
    return new UInt256New(op1.divexact(op2).getBigInteger());
  }

  @Override
  public int size() {
    return 32;
  }

  @Override
  public byte get(final int i) {
    throw new UnsupportedOperationException("get(int) is not supported in the EVM");
  }

  @Override
  public boolean isZero() {
    return value.signum() == 0;
  }

  @Override
  public Bytes slice(final int i, final int length) {
    throw new UnsupportedOperationException("slice is not supported in the EVM");
  }

  @Override
  public Bytes copy() {
    throw new UnsupportedOperationException("copy is not supported in the EVM");
  }

  @Override
  public MutableBytes mutableCopy() {
    throw new UnsupportedOperationException("mutableCopy is not supported in the EVM");
  }

  @Override
  public int bitLength() {
    return value.bitLength();
  }
}
