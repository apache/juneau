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
package org.apache.juneau.testutils.pojos;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import java.math.*;
import java.util.*;

@SuppressWarnings({})
public class PrimitiveObjectsBean {

	// primitive objects
	public Boolean poBoolean;
	public Byte poByte;
	public Character poChar;
	public Short poShort;
	public Integer poInt;
	public Long poLong;
	public Float poFloat;
	public Double poDouble;
	public Number poNumber;
	public BigInteger poBigInteger;
	public BigDecimal poBigDecimal;

	// uninitialized primitive objects
	public Boolean pouBoolean;
	public Byte pouByte;
	public Character pouChar;
	public Short pouShort;
	public Integer pouInt;
	public Long pouLong;
	public Float pouFloat;
	public Double pouDouble;
	public Number pouNumber;
	public BigInteger pouBigInteger;
	public BigDecimal pouBigDecimal;

	// primitive object arrays
	public Boolean[][] poaBoolean;
	public Byte[][] poaByte;
	public Character[][] poaChar;
	public Short[][] poaShort;
	public Integer[][] poaInt;
	public Long[][] poaLong;
	public Float[][] poaFloat;
	public Double[][] poaDouble;
	public Number[][] poaNumber;
	public BigInteger[][] poaBigInteger;
	public BigDecimal[][] poaBigDecimal;

	// primitive object arrays
	public Boolean[][] poauBoolean;
	public Byte[][] poauByte;
	public Character[][] poauChar;
	public Short[][] poauShort;
	public Integer[][] poauInt;
	public Long[][] poauLong;
	public Float[][] poauFloat;
	public Double[][] poauDouble;
	public Number[][] poauNumber;
	public BigInteger[][] poauBigInteger;
	public BigDecimal[][] poauBigDecimal;

	// Anonymous list of primitives (types not erased on objects
	public List<Boolean[]> poalBoolean;
	public List<Byte[]> poalByte;
	public List<Character[]> poalChar;
	public List<Short[]> poalShort;
	public List<Integer[]> poalInt;
	public List<Long[]> poalLong;
	public List<Float[]> poalFloat;
	public List<Double[]> poalDouble;
	public List<Number[]> poalNumber;
	public List<BigInteger[]> poalBigInteger;
	public List<BigDecimal[]> poalBigDecimal;

	// Regular list of primitives (types erased on objects)
	public List<Boolean[]> polBoolean;
	public List<Byte[]> polByte;
	public List<Character[]> polChar;
	public List<Short[]> polShort;
	public List<Integer[]> polInt;
	public List<Long[]> polLong;
	public List<Float[]> polFloat;
	public List<Double[]> polDouble;
	public List<Number[]> polNumber;
	public List<BigInteger[]> polBigInteger;
	public List<BigDecimal[]> polBigDecimal;

	private PrimitiveObjectsBean init() {
		// primitive objects
		poBoolean = true;
		poByte = 1;
		poChar = 'a';
		poShort = 2;
		poInt = 3;
		poLong = 4L;
		poFloat = 5f;
		poDouble = 6d;
		poNumber = 7;
		poBigInteger = BigInteger.valueOf(8L);
		poBigDecimal = new BigDecimal("9");

		// primitive object arrays
		poaBoolean = a(a(true),a(false),null);
		poaByte = a(a((byte)1),a((byte)2),null);
		poaChar = a(a('a'),a('b'),null);
		poaShort = a(a((short)1),a((short)2),null);
		poaInt = a(a(1),a(2),null);
		poaLong = a(a(1L),a(2L),null);
		poaFloat = a(a(1f),a(2f),null);
		poaDouble = a(a(1d),a(2d),null);
		poaNumber = a(a((Number)1),a((Number)2),null);
		poaBigInteger = a(a(BigInteger.valueOf(1L)),a(BigInteger.valueOf(2L)), null);
		poaBigDecimal = a(a(new BigDecimal("1")),a(new BigDecimal("2")), null);

		// Anonymous list of primitives
		poalBoolean = l(a(Boolean.TRUE),null);
		poalByte = l(a((byte)1),null);
		poalChar = l(a('a'),null);
		poalShort = l(a((short)1),null);
		poalInt = l(a(1),null);
		poalLong = l(a(1L),null);
		poalFloat = l(a(1f),null);
		poalDouble = l(a(1d),null);
		poalNumber = l(a(1),null);
		poalBigInteger = l(a(BigInteger.valueOf(1L)),null);
		poalBigDecimal = l(a(new BigDecimal("1")),null);

		// Regular list of primitives
		polBoolean = list();
		polBoolean.add(a(Boolean.TRUE));
		polBoolean.add(null);
		polByte = list();
		polByte.add(a((byte)1));
		polByte.add(null);
		polChar = list();
		polChar.add(a('a'));
		polChar.add(null);
		polShort = list();
		polShort.add(a((short)1));
		polShort.add(null);
		polInt = list();
		polInt.add(a(1));
		polInt.add(null);
		polLong = list();
		polLong.add(a(1L));
		polLong.add(null);
		polFloat = list();
		polFloat.add(a(1f));
		polFloat.add(null);
		polDouble = list();
		polDouble.add(a(1d));
		polDouble.add(null);
		polNumber = list();
		polNumber.add(a((Number)1));
		polNumber.add(null);
		polBigInteger = list();
		polBigInteger.add(a(BigInteger.valueOf(1L)));
		polBigInteger.add(null);
		polBigDecimal = list();
		polBigDecimal.add(a(new BigDecimal("1")));
		polBigDecimal.add(null);

		return this;
	}

	public static PrimitiveObjectsBean get() {
		return new PrimitiveObjectsBean().init();
	}
}