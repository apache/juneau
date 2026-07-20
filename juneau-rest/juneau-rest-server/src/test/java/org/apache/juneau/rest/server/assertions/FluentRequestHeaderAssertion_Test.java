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
package org.apache.juneau.rest.server.assertions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.httppart.*;
import org.apache.juneau.test.assertions.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link FluentRequestHeaderAssertion}.
 *
 * <p>
 * Regression: the chained constructor used to hard-code {@code super(null, ...)}, discarding its {@code creator}
 * argument, so config (custom failure message, output stream, etc.) set on the creating assertion was lost when a
 * header assertion was built via the chained constructor.  The sibling query/form-param assertions correctly pass
 * {@code super(creator, ...)}.
 */
class FluentRequestHeaderAssertion_Test extends TestBase {

	@Test void a01_creatorMsgPropagates() {
		var header = mock(RequestHeader.class);
		when(header.asString()).thenReturn(Optional.of("actual"));

		// The custom failure message configured on the creating assertion must propagate to the header assertion
		// (it did not before the fix, because the chained ctor hard-coded super(null, ...)).
		var creator = new FluentStringAssertion<>("ignored", null).setMsg("CUSTOM {msg}");
		var h = new FluentRequestHeaderAssertion<>(creator, header, null);

		var e = assertThrows(RuntimeException.class, () -> h.is("expected"));
		assertTrue(e.getMessage().startsWith("CUSTOM "), () -> "Message did not carry creator config: " + e.getMessage());
	}

	@Test void a02_noCreatorUsesDefaultMsg() {
		var header = mock(RequestHeader.class);
		when(header.asString()).thenReturn(Optional.of("actual"));

		var h = new FluentRequestHeaderAssertion<>(header, null);

		var e = assertThrows(RuntimeException.class, () -> h.is("expected"));
		assertFalse(e.getMessage().startsWith("CUSTOM "));
	}
}
