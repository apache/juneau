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

/** Tests for {@link TypeConversionFunctions}. */
class TypeConversionFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(TypeConversionFunctions.ALL).build();

	@Test void a01_toInt() { assertEquals("42", vr.resolve("#{toInt(42)}")); }
	@Test void a02_toLong() { assertEquals("999999999999", vr.resolve("#{toLong(999999999999)}")); }
	@Test void a03_toDouble() { assertEquals("3.14", vr.resolve("#{toDouble(3.14)}")); }
	@Test void a04_toBool_true() { assertEquals("true", vr.resolve("#{toBool(yes)}")); }
	@Test void a05_toBool_false() { assertEquals("false", vr.resolve("#{toBool(no)}")); }
}
