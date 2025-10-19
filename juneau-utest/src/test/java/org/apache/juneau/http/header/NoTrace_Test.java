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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class NoTrace_Test extends TestBase {

	private static final String HEADER = "No-Trace";
	private static final String VALUE = "true";
	private static final Boolean PARSED = Boolean.TRUE;

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : StringUtils.join(h, ','));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic() throws Exception {
		var c = client().build();

		// Normal usage.
		c.get().header(noTrace(VALUE)).run().assertContent(VALUE);
		c.get().header(noTrace(VALUE)).run().assertContent(VALUE);
		c.get().header(noTrace(PARSED)).run().assertContent(VALUE);
		c.get().header(noTrace(()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(noTrace((String)null)).run().assertContent().isEmpty();
		c.get().header(noTrace((Supplier<Boolean>)null)).run().assertContent().isEmpty();
		c.get().header(noTrace(()->null)).run().assertContent().isEmpty();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}