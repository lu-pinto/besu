package org.hyperledger.besu.datatypes;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.sun.jna.Pointer;
import it.unich.jgmp.MPZ;
import it.unich.jgmp.nativelib.LibGmp;
import it.unich.jgmp.nativelib.MpzT;
import it.unich.jgmp.nativelib.SizeT;
import it.unich.jgmp.nativelib.SizeTByReference;
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
    final MpzT op1 = getMpzt(value);
    final MpzT op2 = getMpzt(y.value);
    final MpzT res = new MpzT();
    LibGmp.mpz_init(res);
    LibGmp.mpz_mod(res, op1, op2);
    final BigInteger result;
    if (LibGmp.mpz_sgn(res) == 0) {
      result = BigInteger.ZERO;
    } else {
      result = toBigInteger(res);
    }
    cleanupMpzt(op1);
    cleanupMpzt(op2);
    cleanupMpzt(res);
    return new UInt256New(result);
  }

  public UInt256New mul(final UInt256New y) {
    final MpzT op1 = getMpzt(value);
    final MpzT op2 = getMpzt(y.value);
    final MpzT res = new MpzT();
    LibGmp.mpz_init(res);
    LibGmp.mpz_mul(res, op1, op2);
    final BigInteger result;
    if (LibGmp.mpz_sgn(res) == 0) {
      result = BigInteger.ZERO;
    } else {
      result = toBigInteger(res);
    }
    cleanupMpzt(op1);
    cleanupMpzt(op2);
    cleanupMpzt(res);
    return new UInt256New(result);
  }

  public UInt256New exp(final UInt256New y) {
    final MpzT op1 = getMpzt(value);
    final MpzT op2 = getMpzt(y.value);
    final MpzT op3 = getMpzt(MOD_BASE);
    final MpzT res = new MpzT();
    LibGmp.mpz_init(res);
    LibGmp.mpz_powm(res, op1, op2, op3);
    final BigInteger result;
    if (LibGmp.mpz_sgn(res) == 0) {
      result = BigInteger.ZERO;
    } else {
      result = toBigInteger(res);
    }
    cleanupMpzt(op1);
    cleanupMpzt(op2);
    cleanupMpzt(op3);
    cleanupMpzt(res);
    return new UInt256New(result);
  }

  public Bytes divide(final UInt256New y) {
    final MpzT op1 = getMpzt(value);
    final MpzT op2 = getMpzt(y.value);
    final MpzT res = new MpzT();
    LibGmp.mpz_init(res);
    LibGmp.mpz_divexact(res, op1, op2);
    final BigInteger result;
    if (LibGmp.mpz_sgn(res) == 0) {
      result = BigInteger.ZERO;
    } else {
      result = toBigInteger(res);
    }
    cleanupMpzt(op1);
    cleanupMpzt(op2);
    cleanupMpzt(res);
    return new UInt256New(result);
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

  private static MpzT getMpzt(final BigInteger number) {
    final MpzT mpzNative = new MpzT();
    LibGmp.mpz_init(mpzNative);
    ByteBuffer buffer = ByteBuffer.wrap(number.abs().toByteArray());
    LibGmp.mpz_import(mpzNative, new SizeT(buffer.capacity()), 1, new SizeT(1L), 0, new SizeT(0L), buffer);
    if (number.signum() < 0) {
      LibGmp.mpz_neg(mpzNative, mpzNative);
    }
    return mpzNative;
  }

  private static BigInteger toBigInteger(final MpzT mpzNative) {
    ByteBuffer buffer = bufferExport(mpzNative, 1, 1, 0, 0L);
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return new BigInteger(LibGmp.mpz_sgn(mpzNative), bytes);
  }

  private static void cleanupMpzt(final MpzT mpztNative) {
    LibGmp.__gmpz_clear(mpztNative.getPointer());
  }

  public static ByteBuffer bufferExport(final MpzT mpzNative, final int order, final int size, final int endian, final long nails) {
    SizeTByReference count = new SizeTByReference();
    Pointer p = LibGmp.mpz_export(null, count, order, new SizeT(size), endian, new SizeT(nails), mpzNative);
    return p.getByteBuffer(0L, count.getValue().longValue());
  }
}
