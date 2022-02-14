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
import static org.apache.juneau.internal.CollectionUtils.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicMediaRangeArrayHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo/bar;x=1";
	private static final MediaRanges PARSED = MediaRanges.of("foo/bar;x=1");

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
		c.get().header(mediaRangeArrayHeader(HEADER,VALUE)).run().assertBody().is(VALUE);
		c.get().header(mediaRangeArrayHeader(HEADER,VALUE)).run().assertBody().is(VALUE);
		c.get().header(mediaRangeArrayHeader(HEADER,PARSED)).run().assertBody().is(VALUE);
		c.get().header(mediaRangeArrayHeader(HEADER,()->PARSED)).run().assertBody().is(VALUE);

		// Invalid usage.
		c.get().header(mediaRangeArrayHeader("","*")).run().assertBody().isEmpty();
		c.get().header(mediaRangeArrayHeader(null,"*")).run().assertBody().isEmpty();
		c.get().header(mediaRangeArrayHeader(null,()->null)).run().assertBody().isEmpty();
		c.get().header(mediaRangeArrayHeader(HEADER,(Supplier<MediaRanges>)null)).run().assertBody().isEmpty();
		c.get().header(mediaRangeArrayHeader(null,(Supplier<MediaRanges>)null)).run().assertBody().isEmpty();
		c.get().header(mediaRangeArrayHeader(HEADER,()->null)).run().assertBody().isEmpty();
	}

	@Test
	public void a02_match() throws Exception {
		assertInteger(accept("text/foo").match(list(MediaType.of("text/foo")))).is(0);
		assertInteger(accept("text/foo").match(list(MediaType.of("text/bar")))).is(-1);
		assertInteger(new Accept((String)null).match(list(MediaType.of("text/bar")))).is(-1);
		assertInteger(accept("text/foo").match(list(MediaType.of(null)))).is(-1);
		assertInteger(accept("text/foo").match(null)).is(-1);
	}

	@Test
	public void a03_getRange() throws Exception {
		assertString(accept("text/foo").getRange(0)).is("text/foo");
		assertString(accept("text/foo").getRange(1)).isNull();
		assertString(accept("text/foo").getRange(-1)).isNull();
		assertString(new Accept((String)null).getRange(0)).isNull();
	}

	@Test
	public void a04_hasSubtypePart() throws Exception {
		assertBoolean(accept("text/foo").hasSubtypePart("foo")).isTrue();
		assertBoolean(accept("text/foo").hasSubtypePart("bar")).isFalse();
		assertBoolean(accept("text/foo").hasSubtypePart(null)).isFalse();
		assertBoolean(new Accept((String)null).hasSubtypePart("foo")).isFalse();
	}

	@Test
	public void a05_getRanges() throws Exception {
		assertObject(accept("text/foo,text/bar").getRanges()).asJson().is("['text/foo','text/bar']");
		assertObject(new Accept((String)null).getRanges()).asJson().is("[]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
