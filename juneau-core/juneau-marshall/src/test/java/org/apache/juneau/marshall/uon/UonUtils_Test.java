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
package org.apache.juneau.marshall.uon;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

class UonUtils_Test extends TestBase {

	@Test void a01_mergePairs_keyUonValue() throws Exception {
		var a = UonUtils.mergePairs("environment=prod", "period=7d");
		assertBean(a, "environment,period", "prod,7d");
	}

	@Test void a02_mergePairs_listValueWithoutAt() throws Exception {
		var a = UonUtils.mergePairs("flags=(a,b,c)");
		assertEquals("[a, b, c]", a.get("flags").toString());
	}

	@Test void a03_mergePairs_nestedMap() throws Exception {
		var a = UonUtils.mergePairs("nested=(x=1,y=2)");
		assertBean(a.getMap("nested"), "x,y", "1,2");
	}

	@Test void a04_mergePairs_laterKeyWins() throws Exception {
		var a = UonUtils.mergePairs("a=1", "a=2");
		assertBean(a, "a", "2");
	}

	@Test void a05_mergePairs_parenthesizedObject() throws Exception {
		var a = UonUtils.mergePairs("(environment=prod,period=7d)");
		assertBean(a, "environment,period", "prod,7d");
	}

	@Test void a06_mergePair_blankAndBareValueIgnored() throws Exception {
		var a = JsonMap.create();
		UonUtils.mergePair(a, null);
		UonUtils.mergePair(a, "");
		UonUtils.mergePair(a, "   ");
		UonUtils.mergePair(a, "noke");
		UonUtils.mergePair(a, "=novalue");
		assertTrue(a.isEmpty());
	}

	@Test void a07_mergePair_emptyValueIsEmptyString() throws Exception {
		var a = JsonMap.create();
		UonUtils.mergePair(a, "k=");
		assertEquals("", a.get("k"));
	}

	@Test void a08_parseValue_numberLiteralKeptAsNumber() throws Exception {
		assertInstanceOf(Number.class, UonUtils.parseValue("7"));
		assertInstanceOf(Number.class, UonUtils.parseValue("-1.5"));
		assertInstanceOf(Number.class, UonUtils.parseValue("+3"));
		assertInstanceOf(Number.class, UonUtils.parseValue("1e10"));
	}

	@Test void a09_parseValue_nonNumericKeptAsString() throws Exception {
		assertEquals("7d", UonUtils.parseValue("7d"));
	}

	@Test void a10_mergePairs_nullVararg() throws Exception {
		assertTrue(UonUtils.mergePairs((String[])null).isEmpty());
	}

	@Test void a11_mergePairs_nestedCommasInsideParens() throws Exception {
		var a = UonUtils.mergePairs("(a=(x=1,y=2),b=3)");
		assertBean(a.getMap("a"), "x,y", "1,2");
		assertBean(a, "b", "3");
	}
}
