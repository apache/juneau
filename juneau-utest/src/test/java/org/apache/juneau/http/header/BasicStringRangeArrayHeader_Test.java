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
package org.apache.juneau.http.header;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicStringRangeArrayHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo, bar";
	private static final StringRanges PARSED = StringRanges.of("foo, bar");

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : StringUtils.join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		// Normal usage.
		c.get().header(stringRangeArrayHeader(HEADER,VALUE)).run().assertBody().is(VALUE);
		c.get().header(stringRangeArrayHeader(HEADER,VALUE)).run().assertBody().is(VALUE);
		c.get().header(stringRangeArrayHeader(HEADER,PARSED)).run().assertBody().is(VALUE);
		c.get().header(stringRangeArrayHeader(HEADER,()->PARSED)).run().assertBody().is(VALUE);

		// Invalid usage.
		c.get().header(stringRangeArrayHeader("","*")).run().assertBody().isEmpty();
		c.get().header(stringRangeArrayHeader(null,"*")).run().assertBody().isEmpty();
		c.get().header(stringRangeArrayHeader(null,()->null)).run().assertBody().isEmpty();
		c.get().header(stringRangeArrayHeader(HEADER,(Supplier<StringRanges>)null)).run().assertBody().isEmpty();
		c.get().header(stringRangeArrayHeader(null,(Supplier<StringRanges>)null)).run().assertBody().isEmpty();
		c.get().header(stringRangeArrayHeader(HEADER,()->null)).run().assertBody().isEmpty();
	}

	@Test
	public void a02_getRange() throws Exception {
		assertString(stringRangeArrayHeader(HEADER,PARSED).getRange(0)).is("foo");
	}

	@Test
	public void a03_getRanges() throws Exception {
		assertObject(stringRangeArrayHeader(HEADER,PARSED).getRanges()).asJson().is("['foo','bar']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
