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
package org.apache.juneau.svl.vars;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

class PatternMatchVarTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test void a01_test() {
		var vr = VarResolver.create().vars(PatternMatchVar.class, SystemPropertiesVar.class).build();

		System.setProperty("PatternMatchVarTest.test", "foobar");

		assertEquals("true", vr.resolve("$PM{$S{PatternMatchVarTest.test},foobar}"));
		assertEquals("true", vr.resolve("$PM{ $S{ PatternMatchVarTest.test } , foobar }"));
		assertEquals("false", vr.resolve("$PM{$S{PatternMatchVarTest.test},foobar2}"));
		assertEquals("false", vr.resolve("$PM{ $S{ PatternMatchVarTest.test } , foobar2  }"));

		assertEquals("true", vr.resolve("$PM{$S{PatternMatchVarTest.test},foo*}"));
		assertEquals("true", vr.resolve("$PM{$S{PatternMatchVarTest.test},*bar}"));
		assertEquals("true", vr.resolve("$PM{$S{PatternMatchVarTest.test},*}"));
		assertEquals("true", vr.resolve("$PM{$S{PatternMatchVarTest.test},??????}"));

		assertEquals("false", vr.resolve("$PM{$S{PatternMatchVarTest.test},foox*}"));
		assertEquals("false", vr.resolve("$PM{$S{PatternMatchVarTest.test},*xbar}"));
		assertEquals("false", vr.resolve("$PM{$S{PatternMatchVarTest.test},?????}"));
		assertEquals("false", vr.resolve("$PM{$S{PatternMatchVarTest.test},???????}"));
	}
}
