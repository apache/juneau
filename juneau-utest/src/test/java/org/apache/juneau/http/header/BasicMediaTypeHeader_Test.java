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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicMediaTypeHeader_Test {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo/bar";
	private static final MediaType PARSED = MediaType.of("foo/bar");

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
		c.get().header(mediaTypeHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(mediaTypeHeader(HEADER,(Supplier<MediaType>)null)).run().assertContent().isEmpty();
		c.get().header(mediaTypeHeader(HEADER,()->null)).run().assertContent().isEmpty();
		assertThrown(()->mediaTypeHeader("", VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->mediaTypeHeader(null, VALUE)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->mediaTypeHeader("", PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->mediaTypeHeader(null, PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->mediaTypeHeader("", ()->PARSED)).asMessage().is("Name cannot be empty on header.");
		assertThrown(()->mediaTypeHeader(null, ()->PARSED)).asMessage().is("Name cannot be empty on header.");
	}

	@Test
	public void a02_getType() throws Exception {
		assertString(contentType("text/foo").getType()).is("text");
		assertString(new ContentType((String)null).getType()).isEmpty();
	}

	@Test
	public void a03_getSubType() throws Exception {
		assertString(contentType("text/foo").getSubType()).is("foo");
		assertString(new ContentType((String)null).getSubType()).is("*");
	}

	@Test
	public void a04_hasSubType() throws Exception {
		assertBoolean(contentType("text/foo+bar").hasSubType("bar")).isTrue();
		assertBoolean(contentType("text/foo+bar").hasSubType("baz")).isFalse();
		assertBoolean(contentType("text/foo+bar").hasSubType(null)).isFalse();
		assertBoolean(new ContentType((String)null).hasSubType("bar")).isFalse();
	}

	@Test
	public void a05_getSubTypes() throws Exception {
		assertObject(contentType("text/foo+bar").getSubTypes()).asJson().is("['foo','bar']");
		assertObject(new ContentType((String)null).getSubTypes()).asJson().is("['*']");
	}

	@Test
	public void a06_isMeta() throws Exception {
		assertBoolean(contentType("text/foo+bar").isMetaSubtype()).isFalse();
		assertBoolean(contentType("text/*").isMetaSubtype()).isTrue();
		assertBoolean(new ContentType((String)null).isMetaSubtype()).isTrue();
	}

	@Test
	public void a07_match() throws Exception {
		assertInteger(contentType("text/foo").match(MediaType.of("text/foo"),true)).is(100000);
		assertInteger(new ContentType((String)null).match(MediaType.of("text/foo"),true)).is(0);
	}

	@Test
	public void a08_getParameters() throws Exception {
		assertObject(contentType("text/foo;x=1;y=2").getParameters()).asJson().is("[{name:'x',value:'1'},{name:'y',value:'2'}]");
		assertObject(new ContentType((String)null).getParameters()).asJson().is("[]");
	}

	@Test
	public void a09_getParameter() throws Exception {
		assertString(contentType("text/foo;x=1;y=2").getParameter("x")).is("1");
		assertString(contentType("text/foo;x=1;y=2").getParameter("z")).isNull();
		assertString(contentType("text/foo;x=1;y=2").getParameter(null)).isNull();
		assertObject(new ContentType((String)null).getParameter("x")).isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}
