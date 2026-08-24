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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link ServerValuesVar} ({@code $FV}) driven through a real {@link VarResolver} session with a
 * {@link ServerValuesRegistry} bean added: scalar resolution, {@code DefaultingVar} fail-soft, no recursion,
 * non-scalar rejection, and fail-open when no registry is present.
 */
class ServerValuesVar_Test extends TestBase {

	private static VarResolver resolver() {
		return VarResolver.create().defaultVars().defaultFunctions().vars(new ServerValuesVar()).build();
	}

	private static VarResolverSession session(ServerValues sv) {
		var s = resolver().createSession();
		s.bean(ServerValuesRegistry.class, ServerValuesRegistry.of(sv));
		return s;
	}

	@Test void a01_resolvesSessionAwareScalar() {
		var s = session(ServerValues.create().value("failedCount", sess -> 7));
		assertEquals("Failures (7)", s.resolve("Failures ($FV{failedCount})"));
	}

	@Test void a02_missingName_resolvesEmpty() {
		var s = session(ServerValues.create().value("x", sess -> "y"));
		assertEquals("", s.resolve("$FV{missing}"));
	}

	@Test void a03_missingName_usesDefault() {
		var s = session(ServerValues.create().value("x", sess -> "y"));
		assertEquals("fallback", s.resolve("$FV{missing,fallback}"));
	}

	@Test void a04_presentName_ignoresDefault() {
		var s = session(ServerValues.create().value("x", sess -> "real"));
		assertEquals("real", s.resolve("$FV{x,fallback}"));
	}

	@Test void a05_allowRecurse_false_returnsProviderStringLiterally() {
		var s = session(ServerValues.create().value("x", sess -> "$S{java.version}"));
		assertEquals("$S{java.version}", s.resolve("$FV{x}"));
	}

	@Test void a06_nonScalar_rejected() {
		var s = session(ServerValues.create().value("list", sess -> List.of("a", "b")));
		assertThrows(RuntimeException.class, () -> s.resolve("$FV{list}"));
	}

	@Test void a07_noRegistry_tokenLeftLiteral() {
		var s = resolver().createSession();
		assertEquals("$FV{x}", s.resolve("$FV{x}"));
	}

	@Test void a08_allowRecurse_isFalse() {
		assertFalse(new ServerValuesVar().allowRecurse());
	}
}
