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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.vars.*;
import org.junit.jupiter.api.*;

/**
 * Pins that {@code #{...}} results (and function/Structure/var output) are never re-resolved.
 */
class ExprRecurse_Test extends TestBase {

	static VarResolver resolver(boolean strict) {
		return VarResolver.create().defaultFunctions().vars(SystemPropertiesVar.class).strict(strict)
			.functions(ValueFormatter_Test.fn("txt", () -> "${S:x}"))
			.functions(ValueFormatter_Test.fn("st", () -> Structure.ul(List.of("${S:x}"))))
			.functions(ValueFormatter_Test.fn("lst", () -> List.of("${S:x}", "#{1+1}")))
			.build();
	}

	static void run(boolean strict) {
		var vr = resolver(strict);
		System.setProperty("j0439.rec", "#{1+1}");
		assertEquals("${S:x}", vr.resolve("#{txt()}"));
		assertEquals("- ${S:x}", vr.resolve("#{st()}"));
		assertEquals("#{1+1}", vr.resolve("#{${S:j0439.rec}}"));
		assertEquals("[\"${S:x}\",\"#{1+1}\"]", vr.resolve("#{lst()}"));
		assertEquals("a${S:x}b", vr.resolve("a#{txt()}b"));
	}

	@Test void a01_lenient() { run(false); }
	@Test void a02_strict() { run(true); }
}
