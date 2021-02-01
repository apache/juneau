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

import java.io.*;
import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.header.BasicMediaTypeHeader.of;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicMediaTypeHeader_Test {


	private static final String HEADER = "Foo";

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER,multi=true) String[] h) {
			return new StringReader(h == null ? "null" : StringUtils.join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {
		RestClient c = client().build();

		c.get().header(of(null,(Object)null)).run().assertBody().isEmpty();
		c.get().header(of("","*")).run().assertBody().isEmpty();
		c.get().header(of(HEADER,(Object)null)).run().assertBody().isEmpty();
		c.get().header(of(null,"*")).run().assertBody().isEmpty();

		c.get().header(of(null,()->null)).run().assertBody().isEmpty();
		c.get().header(of(HEADER,(Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(null,(Supplier<?>)null)).run().assertBody().isEmpty();
		c.get().header(of(HEADER,()->null)).run().assertBody().isEmpty();

		c.get().header(new BasicMediaTypeHeader(HEADER,null)).run().assertBody().isEmpty();
		c.get().header(new BasicMediaTypeHeader(HEADER,((Supplier<?>)()->null))).run().assertBody().isEmpty();

		c.get().header(of(HEADER,"foo/bar;x=1")).run().assertBody().is("foo/bar;x=1");
	}

	@Test
	public void a02_getType() throws Exception {
		assertString(ContentType.of("text/foo").getType()).is("text");
		assertString(new ContentType((String)null).getType()).isEmpty();
	}

	@Test
	public void a03_getSubType() throws Exception {
		assertString(ContentType.of("text/foo").getSubType()).is("foo");
		assertString(new ContentType((String)null).getSubType()).is("*");
	}

	@Test
	public void a04_hasSubType() throws Exception {
		assertBoolean(ContentType.of("text/foo+bar").hasSubType("bar")).isTrue();
		assertBoolean(ContentType.of("text/foo+bar").hasSubType("baz")).isFalse();
		assertBoolean(ContentType.of("text/foo+bar").hasSubType(null)).isFalse();
		assertBoolean(new ContentType((String)null).hasSubType("bar")).isFalse();
	}

	@Test
	public void a05_getSubTypes() throws Exception {
		assertObject(ContentType.of("text/foo+bar").getSubTypes()).json().is("['foo','bar']");
		assertObject(new ContentType((String)null).getSubTypes()).json().is("['*']");
	}

	@Test
	public void a06_isMeta() throws Exception {
		assertBoolean(ContentType.of("text/foo+bar").isMetaSubtype()).isFalse();
		assertBoolean(ContentType.of("text/*").isMetaSubtype()).isTrue();
		assertBoolean(new ContentType((String)null).isMetaSubtype()).isTrue();
	}

	@Test
	public void a07_match() throws Exception {
		assertInteger(ContentType.of("text/foo").match(MediaType.of("text/foo"),true)).is(100000);
		assertInteger(new ContentType((String)null).match(MediaType.of("text/foo"),true)).is(0);
	}

	@Test
	public void a08_getParameters() throws Exception {
		assertObject(ContentType.of("text/foo;x=1;y=2").getParameters()).json().is("['x=1','y=2']");
		assertObject(new ContentType((String)null).getParameters()).json().is("[]");
	}

	@Test
	public void a09_getParameter() throws Exception {
		assertString(ContentType.of("text/foo;x=1;y=2").getParameter("x")).is("1");
		assertString(ContentType.of("text/foo;x=1;y=2").getParameter("z")).isNull();
		assertString(ContentType.of("text/foo;x=1;y=2").getParameter(null)).isNull();
		assertObject(new ContentType((String)null).getParameter("x")).isNull();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class);
	}
}
