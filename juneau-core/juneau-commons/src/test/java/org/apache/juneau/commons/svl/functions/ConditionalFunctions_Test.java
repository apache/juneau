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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link ConditionalFunctions}. */
class ConditionalFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(ConditionalFunctions.ALL).build();

	@Test void a01_if_true() { assertEquals("yes", vr.resolve("#{if(true, yes, no)}")); }
	@Test void a02_if_false() { assertEquals("no", vr.resolve("#{if(false, yes, no)}")); }
	@Test void a03_if_truthy_one() { assertEquals("yes", vr.resolve("#{if(1, yes, no)}")); }
	@Test void a04_if_truthy_yes() { assertEquals("yes", vr.resolve("#{if(yes, yes, no)}")); }

	@Test void a05_switch_match() { assertEquals("alpha", vr.resolve("#{switch(a, a, alpha, b, beta, c, gamma)}")); }
	@Test void a06_switch_default() { assertEquals("default", vr.resolve("#{switch(z, a, alpha, b, beta, default)}")); }
	@Test void a07_switch_noDefault_noMatch() { assertEquals("", vr.resolve("#{switch(z, a, alpha, b, beta)}")); }
	@Test void a08_switch_globStar() { assertEquals("YES", vr.resolve("#{switch(foobar, foo*, YES, *, NO)}")); }
	@Test void a09_switch_globEnd() { assertEquals("YES", vr.resolve("#{switch(foobar, *bar, YES, *, NO)}")); }
	@Test void a10_switch_globMid() { assertEquals("Fruit", vr.resolve("#{switch(Apple, *Ap*, Fruit, *Car*, Veg, *, NA)}")); }
	@Test void a11_switch_questionMark() { assertEquals("YES", vr.resolve("#{switch(abc, ???, YES, *, NO)}")); }
	@Test void a12_switch_questionMarkNo() { assertEquals("NO", vr.resolve("#{switch(abcd, ???, YES, *, NO)}")); }
	@Test void a13_switch_globStarOnlyDefault() { assertEquals("DEFAULT", vr.resolve("#{switch(anything, *, DEFAULT)}")); }

	@Test void a14_coalesce_first() { assertEquals("a", vr.resolve("#{coalesce(a, b, c)}")); }
	@Test void a15_coalesce_skipsEmpty() { assertEquals("b", vr.resolve("#{coalesce(\"\", b, c)}")); }
	@Test void a16_coalesce_allEmpty() { assertEquals("", vr.resolve("#{coalesce(\"\", \"\")}")); }

	@Test void a17_notEmpty_true() { assertEquals("true", vr.resolve("#{notEmpty(hello)}")); }
	@Test void a18_notEmpty_false() { assertEquals("false", vr.resolve("#{notEmpty(\"\")}")); }
}
