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
package org.apache.juneau.assertions;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.assertions.Assertions.assertBean;
import static org.apache.juneau.assertions.Assertions.assertList;
import static org.apache.juneau.assertions.Assertions.assertMap;
import static org.apache.juneau.assertions.Assertions.assertThrowable;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;

@TestMethodOrder(MethodName.class)
@Deprecated
class Assertions_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_assertDate() {
		assertDate(new Date()).isAfter(new Date(0));
	}

	@Test void a02_assertInteger() {
		assertInteger(2).isGt(1);
	}

	@Test void a03_assertLong() {
		assertLong(2L).isGt(1L);
	}

	@Test void a04_assertObject() {
		assertObject("foo").asJson().is("'foo'");
	}

	@Test void a06_assertThrowable() {
		assertThrowable(null).isNull();
	}

	@Test void a07_assertArray() {
		assertArray(new String[0]).isEmpty();
	}

	@Test void a08_assertCollection() {
		assertCollection(alist()).isEmpty();
	}

	@Test void a09_assertList() {
		assertList(alist()).isEmpty();
	}

	@Test void a10_assertStream() throws Exception {
		assertBytes(inputStream("foo")).asString().is("foo");
		assertBytes((InputStream)null).asString().isNull();
	}

	@Test void a11_assertBytes() {
		assertBytes("foo".getBytes()).asString().is("foo");
		assertBytes((byte[])null).asString().isNull();
	}

	@Test void a12_assertReader() throws Exception {
		assertReader(reader("foo")).is("foo");
		assertReader((Reader)null).isNull();
	}

	@Test void a13_assertThrown() {
		assertThrows(RuntimeException.class, ()->{throw new RuntimeException("foo");}, "foo");
		assertThrown(()->{}).isNull();
	}

	@Test void a14_assertZonedDateTime() {
		assertZonedDateTime(ZonedDateTime.now()).isExists();
	}

	@Test void a15_assertBean() {
		assertBean("123").isExists();
	}

	@Test void a16_assertBoolean() {
		assertTrue(true);
	}

	@Test void a17_assertVersion() {
		assertVersion(Version.of("2")).isGt(Version.of("1"));
	}

	@Test void a18_assertComparable() {
		assertComparable(2).isGt(1);
	}

	@Test void a19_assertBeanList() {
		assertBeanList(alist(ABean.get())).asJson().is("[{a:1,b:'foo'}]");
	}

	@Test void a20a_assertIntArray() {
		assertIntArray(new int[]{1}).asLength().is(1);
	}

	@Test void a20b_assertLongArray() {
		assertLongArray(new long[]{1L}).asLength().is(1);
	}

	@Test void a20c_assertShortArray() {
		assertShortArray(new short[]{1}).asLength().is(1);
	}

	@Test void a20d_assertFloatArray() {
		assertFloatArray(new float[]{1}).asLength().is(1);
	}

	@Test void a20e_assertDoubleArray() {
		assertDoubleArray(new double[]{1}).asLength().is(1);
	}

	@Test void a20f_assertBooleanArray() {
		assertBooleanArray(new boolean[]{true}).asLength().is(1);
	}

	@Test void a20g_assertCharArray() {
		assertCharArray(new char[]{'a'}).asLength().is(1);
	}

	@Test void a20h_assertByteArray() {
		assertByteArray(new byte[]{1}).asLength().is(1);
	}

	@Test void a21_assertMap() {
		assertMap(map(1,2)).asSize().is(1);
	}

	@Test void a24_assertOptional() {
		assertOptional(Optional.of(1)).isNotNull();
	}

	@Test void a25_assertStringList() {
		assertStringList(alist()).isNotNull();
		assertStringList(null).isNull();
	}
}