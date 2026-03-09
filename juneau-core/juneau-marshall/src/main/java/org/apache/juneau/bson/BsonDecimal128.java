/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * IEEE 754-2008 Decimal128 BID (Binary Integer Decimal) encoding for BSON.
 *
 * <p>
 * Supports conversion between <code>BigDecimal</code> and the 16-byte BSON Decimal128 format.
 * Used by {@link BsonSerializer} and {@link BsonParser} for decimal values.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BsonBasics">BSON Basics</a>
 * 	<li class='link'><a class="doclink" href="https://bsonspec.org/spec.html">BSON Specification</a>
 * </ul>
 */
public final class BsonDecimal128 {

	private static final MathContext DECIMAL128 = new MathContext(34);
	private static final long SIGN_BIT_MASK = 1L << 63;
	private static final long INFINITY_MASK = 0x7800000000000000L;
	private static final long NaN_MASK = 0x7c00000000000000L;
	private static final int MIN_EXPONENT = -6176;
	private static final int MAX_EXPONENT = 6111;
	private static final int EXPONENT_OFFSET = 6176;
	private static final int MAX_BIT_LENGTH = 113;
	private static final BigInteger TEN = new BigInteger("10");
	private static final BigInteger ZERO = BigInteger.ZERO;
	private static final BigInteger ONE = BigInteger.ONE;

	private final long high;
	private final long low;

	private BsonDecimal128(long high, long low) {
		this.high = high;
		this.low = low;
	}

	/**
	 * Creates a Decimal128 from the given high and low 64-bit values (IEEE 754-2008 BID encoding).
	 *
	 * @param high The high-order 64 bits.
	 * @param low The low-order 64 bits.
	 * @return The Decimal128 instance.
	 */
	public static BsonDecimal128 fromIEEE754BIDEncoding(long high, long low) {
		return new BsonDecimal128(high, low);
	}

	/**
	 * Creates a Decimal128 from the given BigDecimal.
	 *
	 * @param value The decimal value.
	 * @return The Decimal128 instance.
	 * @throws NumberFormatException If the value is out of Decimal128 range.
	 */
	public static BsonDecimal128 fromBigDecimal(BigDecimal value) {
		return new BsonDecimal128(value, value.signum() == -1);
	}

	private BsonDecimal128(BigDecimal initialValue, boolean isNegative) {
		var value = clampAndRound(initialValue);
		long localHigh = 0;
		long localLow = 0;

		int exponent = -value.scale();
		if (exponent < MIN_EXPONENT || exponent > MAX_EXPONENT)
			throw new NumberFormatException("Exponent out of Decimal128 range: " + exponent);

		if (value.unscaledValue().bitLength() > MAX_BIT_LENGTH)
			throw new NumberFormatException("Significand out of Decimal128 range");

		var significand = value.unscaledValue().abs();
		int bitLength = significand.bitLength();

		for (var i = 0; i < Math.min(64, bitLength); i++)
			if (significand.testBit(i))
				localLow |= 1L << i;

		for (var i = 64; i < bitLength; i++)
			if (significand.testBit(i))
				localHigh |= 1L << (i - 64);

		long biasedExponent = exponent + EXPONENT_OFFSET;
		localHigh |= biasedExponent << 49;

		if (value.signum() == -1 || isNegative)
			localHigh |= SIGN_BIT_MASK;

		this.high = localHigh;
		this.low = localLow;
	}

	private static BigDecimal clampAndRound(BigDecimal initialValue) {
		var value = initialValue;
		if (-initialValue.scale() > MAX_EXPONENT) {
			int diff = -initialValue.scale() - MAX_EXPONENT;
			if (initialValue.unscaledValue().equals(ZERO))
				value = new BigDecimal(initialValue.unscaledValue(), -MAX_EXPONENT);
			else if (diff + initialValue.precision() > 34)
				throw new NumberFormatException("Exponent out of Decimal128 range: " + initialValue);
			else {
				var multiplier = TEN.pow(diff);
				value = new BigDecimal(initialValue.unscaledValue().multiply(multiplier), initialValue.scale() + diff);
			}
		} else if (-initialValue.scale() < MIN_EXPONENT) {
			int diff = initialValue.scale() + MIN_EXPONENT;
			int undiscardedPrecision = ensureExactRounding(initialValue, diff);
			var divisor = undiscardedPrecision == 0 ? ONE : TEN.pow(diff);
			value = new BigDecimal(initialValue.unscaledValue().divide(divisor), initialValue.scale() - diff);
		} else {
			value = initialValue.round(DECIMAL128);
			int extraPrecision = initialValue.precision() - value.precision();
			if (extraPrecision > 0)
				ensureExactRounding(initialValue, extraPrecision);
		}
		return value;
	}

	private static int ensureExactRounding(BigDecimal initialValue, int extraPrecision) {
		var significand = initialValue.unscaledValue().abs().toString();
		int undiscardedPrecision = Math.max(0, significand.length() - extraPrecision);
		for (var i = undiscardedPrecision; i < significand.length(); i++)
			if (significand.charAt(i) != '0')
				throw new NumberFormatException("Conversion to Decimal128 would require inexact rounding: " + initialValue);
		return undiscardedPrecision;
	}

	/**
	 * Returns the high-order 64 bits.
	 *
	 * @return The high bits.
	 */
	public long getHigh() {
		return high;
	}

	/**
	 * Returns the low-order 64 bits.
	 *
	 * @return The low bits.
	 */
	public long getLow() {
		return low;
	}

	/**
	 * Converts to BigDecimal.
	 *
	 * @return The equivalent BigDecimal.
	 * @throws ArithmeticException If the value is NaN, Infinity, or negative zero.
	 */
	public BigDecimal toBigDecimal() {
		if (isNaN())
			throw new ArithmeticException("NaN cannot be converted to BigDecimal");
		if (isInfinite())
			throw new ArithmeticException("Infinity cannot be converted to BigDecimal");

		var bd = toBigDecimalNoNegativeZeroCheck();
		if (isNegative() && bd.signum() == 0)
			throw new ArithmeticException("Negative zero cannot be converted to BigDecimal");
		return bd;
	}

	private boolean isNaN() {
		return (high & NaN_MASK) == NaN_MASK;
	}

	private boolean isInfinite() {
		return (high & INFINITY_MASK) == INFINITY_MASK;
	}

	private boolean isNegative() {
		return (high & SIGN_BIT_MASK) == SIGN_BIT_MASK;
	}

	private boolean twoHighestCombinationBitsAreSet() {
		return (high & 3L << 61) == 3L << 61;
	}

	private int getExponent() {
		if (twoHighestCombinationBitsAreSet())
			return (int)((high & 0x1fffe00000000000L) >>> 47) - EXPONENT_OFFSET;
		return (int)((high & 0x7fff800000000000L) >>> 49) - EXPONENT_OFFSET;
	}

	private BigDecimal toBigDecimalNoNegativeZeroCheck() {
		int scale = -getExponent();
		if (twoHighestCombinationBitsAreSet())
			return BigDecimal.valueOf(0, scale);
		return new BigDecimal(new BigInteger(isNegative() ? -1 : 1, getSignificandBytes()), scale);
	}

	private byte[] getSignificandBytes() {
		var bytes = new byte[15];
		long mask = 0xFF;
		for (var i = 14; i >= 7; i--) {
			bytes[i] = (byte)((low & mask) >>> ((14 - i) << 3));
			mask <<= 8;
		}
		mask = 0xFF;
		for (var i = 6; i >= 1; i--) {
			bytes[i] = (byte)((high & mask) >>> ((6 - i) << 3));
			mask <<= 8;
		}
		bytes[0] = (byte)((high & 0x0001000000000000L) >>> 48);
		return bytes;
	}

}
