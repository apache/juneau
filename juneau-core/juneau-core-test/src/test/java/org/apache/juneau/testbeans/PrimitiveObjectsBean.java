// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.testbeans;

import java.math.*;
import java.util.*;

import org.apache.juneau.utils.*;

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

	public PrimitiveObjectsBean init() {
		// primitive objects
		poBoolean = true;
		poByte = 1;
		poChar = 'a';
		poShort = 2;
		poInt = 3;
		poLong = 4l;
		poFloat = 5f;
		poDouble = 6d;
		poNumber = 7;
		poBigInteger = new BigInteger("8");
		poBigDecimal = new BigDecimal("9");

		// primitive object arrays
		poaBoolean = new Boolean[][]{{true},{false},null};
		poaByte = new Byte[][]{{1},{2},null};
		poaChar = new Character[][]{{'a'},{'b'},null};
		poaShort = new Short[][]{{1},{2},null};
		poaInt = new Integer[][]{{1},{2},null};
		poaLong = new Long[][]{{1l},{2l},null};
		poaFloat = new Float[][]{{1f},{2f},null};
		poaDouble = new Double[][]{{1d},{2d},null};
		poaNumber = new Number[][]{{1},{2},null};
		poaBigInteger = new BigInteger[][]{{new BigInteger("1")}, {new BigInteger("2")}, null};
		poaBigDecimal = new BigDecimal[][]{{new BigDecimal("1")}, {new BigDecimal("2")}, null};

		// Anonymous list of primitives
		poalBoolean = new AList<Boolean[]>().append(new Boolean[]{Boolean.TRUE}).append(null);
		poalByte = new AList<Byte[]>().append(new Byte[]{1}).append(null);
		poalChar = new AList<Character[]>().append(new Character[]{'a'}).append(null);
		poalShort = new AList<Short[]>().append(new Short[]{1}).append(null);
		poalInt = new AList<Integer[]>().append(new Integer[]{1}).append(null);
		poalLong = new AList<Long[]>().append(new Long[]{1l}).append(null);
		poalFloat = new AList<Float[]>().append(new Float[]{1f}).append(null);
		poalDouble = new AList<Double[]>().append(new Double[]{1d}).append(null);
		poalNumber = new AList<Number[]>().append(new Integer[]{1}).append(null);
		poalBigInteger = new AList<BigInteger[]>().append(new BigInteger[]{new BigInteger("1")}).append(null);
		poalBigDecimal = new AList<BigDecimal[]>().append(new BigDecimal[]{new BigDecimal("1")}).append(null);

		// Regular list of primitives
		polBoolean = new ArrayList<Boolean[]>();
		polBoolean.add(new Boolean[]{Boolean.TRUE});
		polBoolean.add(null);
		polByte = new ArrayList<Byte[]>();
		polByte.add(new Byte[]{1});
		polByte.add(null);
		polChar = new ArrayList<Character[]>();
		polChar.add(new Character[]{'a'});
		polChar.add(null);
		polShort = new ArrayList<Short[]>();
		polShort.add(new Short[]{1});
		polShort.add(null);
		polInt = new ArrayList<Integer[]>();
		polInt.add(new Integer[]{1});
		polInt.add(null);
		polLong = new ArrayList<Long[]>();
		polLong.add(new Long[]{1l});
		polLong.add(null);
		polFloat = new ArrayList<Float[]>();
		polFloat.add(new Float[]{1f});
		polFloat.add(null);
		polDouble = new ArrayList<Double[]>();
		polDouble.add(new Double[]{1d});
		polDouble.add(null);
		polNumber = new ArrayList<Number[]>();
		polNumber.add(new Number[]{1});
		polNumber.add(null);
		polBigInteger = new ArrayList<BigInteger[]>();
		polBigInteger.add(new BigInteger[]{new BigInteger("1")});
		polBigInteger.add(null);
		polBigDecimal = new ArrayList<BigDecimal[]>();
		polBigDecimal.add(new BigDecimal[]{new BigDecimal("1")});
		polBigDecimal.add(null);

		return this;
	}
}