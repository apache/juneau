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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.StreamUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Assertions_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a00_basic() {
		new Assertions();
	}

	@Test
	public void a01_assertDate() throws Exception {
		assertDate(new Date()).isAfter(new Date(0));
	}

	@Test
	public void a02_assertInteger() throws Exception {
		assertInteger(2).isGt(1);
	}

	@Test
	public void a03_assertLong() throws Exception {
		assertLong(2l).isGt(1l);
	}

	@Test
	public void a04_assertObject() throws Exception {
		assertObject("foo").asJson().is("'foo'");
	}

	@Test
	public void a05_assertString() throws Exception {
		assertString("foo").is("foo");
		assertString(optional("foo")).is("foo");
		assertString(empty()).isNull();
	}

	@Test
	public void a06_assertThrowable() throws Exception {
		assertThrowable(null).isNull();
	}

	@Test
	public void a07_assertArray() throws Exception {
		assertArray(new String[0]).isEmpty();
	}

	@Test
	public void a08_assertCollection() throws Exception {
		assertCollection(alist()).isEmpty();
	}

	@Test
	public void a09_assertList() throws Exception {
		assertList(alist()).isEmpty();
	}

	@Test
	public void a10_assertStream() throws Exception {
		assertBytes(inputStream("foo")).asString().is("foo");
		assertBytes((InputStream)null).asString().isNull();
	}

	@Test
	public void a11_assertBytes() throws Exception {
		assertBytes("foo".getBytes()).asString().is("foo");
		assertBytes((byte[])null).asString().isNull();
	}

	@Test
	public void a12_assertReader() throws Exception {
		assertReader(reader("foo")).is("foo");
		assertReader((Reader)null).isNull();
	}

	@Test
	public void a13_assertThrown() throws Exception {
		assertThrown(()->{throw new RuntimeException("foo");}).asMessage().is("foo");
		assertThrown(()->{}).isNull();
	}

	@Test
	public void a14_assertZonedDateTime() throws Exception {
		assertZonedDateTime(ZonedDateTime.now()).isExists();
	}

	@Test
	public void a15_assertBean() throws Exception {
		assertBean("123").isExists();
	}

	@Test
	public void a16_assertBoolean() throws Exception {
		assertBoolean(true).isTrue();
	}

	@Test
	public void a17_assertVersion() throws Exception {
		assertVersion(Version.of("2")).isGt(Version.of("1"));
	}

	@Test
	public void a18_assertComparable() throws Exception {
		assertComparable(2).isGt(1);
	}

	@Test
	public void a19_assertBeanList() throws Exception {
		assertBeanList(alist(ABean.get())).asJson().is("[{a:1,b:'foo'}]");
	}

	@Test
	public void a20a_assertIntArray() throws Exception {
		assertIntArray(new int[]{1}).asLength().is(1);
	}

	@Test
	public void a20b_assertLongArray() throws Exception {
		assertLongArray(new long[]{1l}).asLength().is(1);
	}

	@Test
	public void a20c_assertShortArray() throws Exception {
		assertShortArray(new short[]{1}).asLength().is(1);
	}

	@Test
	public void a20d_assertFloatArray() throws Exception {
		assertFloatArray(new float[]{1}).asLength().is(1);
	}

	@Test
	public void a20e_assertDoubleArray() throws Exception {
		assertDoubleArray(new double[]{1}).asLength().is(1);
	}

	@Test
	public void a20f_assertBooleanArray() throws Exception {
		assertBooleanArray(new boolean[]{true}).asLength().is(1);
	}

	@Test
	public void a20g_assertCharArray() throws Exception {
		assertCharArray(new char[]{'a'}).asLength().is(1);
	}

	@Test
	public void a20h_assertByteArray() throws Exception {
		assertByteArray(new byte[]{1}).asLength().is(1);
	}

	@Test
	public void a21_assertMap() throws Exception {
		assertMap(map(1,2)).asSize().is(1);
	}

	@Test
	public void a24_assertOptional() throws Exception {
		assertOptional(empty()).isNull();
		assertOptional(optional(1)).isNotNull();
	}

	@Test
	public void a25_assertStringList() throws Exception {
		assertStringList(alist()).isNotNull();
		assertStringList(null).isNull();
	}
}
