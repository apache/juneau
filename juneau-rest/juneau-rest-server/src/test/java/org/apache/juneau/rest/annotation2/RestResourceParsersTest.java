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
package org.apache.juneau.rest.annotation2;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.internal.IOUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(parsers).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourceParsersTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	public static class PA extends ReaderParser {
		public PA(PropertyStore ps) {
			super(ps, "text/a");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)("text/a - " + read(pipe.getReader()).trim());
				}
			};
		}
	}

	public static class PB extends ReaderParser {
		public PB(PropertyStore ps) {
			super(ps, "text/b");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)("text/b - " + read(pipe.getReader()).trim());
				}
			};
		}
	}

	public static class PC extends ReaderParser {
		public PC(PropertyStore ps) {
			super(ps, "text/c");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)("text/c - " + read(pipe.getReader()).trim());
				}
			};
		}
	}

	public static class PD extends ReaderParser {
		public PD(PropertyStore ps) {
			super(ps, "text/d");
		}
		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {
				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)("text/d - " + read(pipe.getReader()).trim());
				}
			};
		}
	}

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@RestResource(parsers=PA.class)
	public static class A {
		@RestMethod(name=PUT, path="/parserOnClass")
		public String a01(@Body String in) {
			return in;
		}
		@RestMethod(name=PUT, path="/parserOnMethod", parsers=PB.class)
		public String a02(@Body String in) {
			return in;
		}
		@RestMethod(name=PUT, path="/parserOverriddenOnMethod", parsers={Inherit.class, PB.class,PC.class})
		public String a03(@Body String in) {
			return in;
		}
		@RestMethod(name=PUT, path="/parserWithDifferentMediaTypes", parsers={Inherit.class, PD.class})
		public String a04(@Body String in) {
			return in;
		}
		@RestMethod(name=PUT, path="/validErrorResponse")
		public String a05(@Body String in) {
			return in;
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_parserOnClass() throws Exception {
		a.put("/parserOnClass", "test1").contentType("text/a").execute().assertBody("text/a - test1");
		a.put("/parserOnClass?noTrace=true", "test1").contentType("text/b").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/b'",
				"Supported media-types: ['text/a"
			);
	}
	@Test
	public void a02_parserOnMethod() throws Exception {
		a.put("/parserOnMethod", "test2").contentType("text/b").execute().assertBody("text/b - test2");
		a.put("/parserOnMethod?noTrace=true", "test2").contentType("text/a").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/a'",
				"Supported media-types: ['text/b']"
			);
	}
	@Test
	public void a03_parserOverriddenOnMethod() throws Exception {
		a.put("/parserOverriddenOnMethod", "test3").contentType("text/a").execute().assertBody("text/a - test3");
		a.put("/parserOverriddenOnMethod", "test3").contentType("text/b").execute().assertBody("text/b - test3");
	}
	@Test
	public void a04_parserWithDifferentMediaTypes() throws Exception {
		a.put("/parserWithDifferentMediaTypes", "test4").contentType("text/a").execute().assertBody("text/a - test4");
		a.put("/parserWithDifferentMediaTypes", "test4").contentType("text/d").execute().assertBody("text/d - test4");
	}
	@Test
	public void a05_validErrorResponse() throws Exception {
		a.put("/validErrorResponse?noTrace=true", "test1").contentType("text/bad").execute()
			.assertStatus(415)
			.assertBodyContains(
				"Unsupported media-type in request header 'Content-Type': 'text/bad'",
				"Supported media-types: ['text/a"
			);
	}
}
