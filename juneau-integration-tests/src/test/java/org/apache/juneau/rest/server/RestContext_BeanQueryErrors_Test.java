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
package org.apache.juneau.rest.server;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.converter.*;
import org.junit.jupiter.api.*;

/**
 * Tests the central {@link BeanQuerySyntaxException} / {@link BeanQueryExecutionException} &rarr; HTTP mapping in
 * {@link RestContext#convertThrowable(Throwable)} and the problem-details {@code "code"} member.
 *
 * <p>
 * Both error-rendering paths are covered: an exception thrown from the {@code @RestOp} method itself (rendered by the
 * response processors) and one thrown from a {@link RestConverter} &mdash; the path {@code Queryable} takes &mdash;
 * which reaches {@code RestContext.handleError}.
 */
class RestContext_BeanQueryErrors_Test extends TestBase {

	private static BeanQuerySyntaxException syntaxError() {
		return new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_COLUMN, "Unknown column '%s' in search.", "pasword");
	}

	private static BeanQueryExecutionException executionError() {
		return new BeanQueryExecutionException(new RuntimeException("SELECT * FROM secret"), "Query execution failed for table '%s'.", "person");
	}

	/** Mirrors {@code Queryable}: a converter that rejects the query. */
	public static class SyntaxErrorConverter implements RestConverter {
		@Override /* RestConverter */
		public Object convert(RestRequest req, Object res) {
			throw syntaxError();
		}
	}

	/** Mirrors {@code Queryable} against a failing backend. */
	public static class ExecutionErrorConverter implements RestConverter {
		@Override /* RestConverter */
		public Object convert(RestRequest req, Object res) {
			throw executionError();
		}
	}

	@Rest
	public static class A implements BasicUniversalConfig {
		@RestGet("/syntax")
		public String syntax() {
			throw syntaxError();
		}
		@RestGet("/execution")
		public String execution() {
			throw executionError();
		}
		@RestGet(path="/converterSyntax", converters=SyntaxErrorConverter.class)
		public String converterSyntax() {
			return "x";
		}
		@RestGet(path="/converterExecution", converters=ExecutionErrorConverter.class)
		public String converterExecution() {
			return "x";
		}
	}

	@Rest(problemDetails="true")
	public static class B implements BasicUniversalConfig {
		@RestGet("/syntax")
		public String syntax() {
			throw syntaxError();
		}
		@RestGet("/execution")
		public String execution() {
			throw executionError();
		}
		@RestGet(path="/converterSyntax", converters=SyntaxErrorConverter.class)
		public String converterSyntax() {
			return "x";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Problem details off
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_syntaxError_400WithHeaderAndMessage() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().build();
		c.get("/syntax").run()
			.assertStatus(400)
			.assertHeader(BeanQueryRequest.ERROR_HEADER).is("UNKNOWN_COLUMN")
			.assertContent().isContains("Unknown column 'pasword' in search.");
	}

	@Test void a02_executionError_500GenericBodyNoCauseLeak() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().build();
		var r = c.get("/execution").run().assertStatus(500).assertHeader(BeanQueryRequest.ERROR_HEADER).isNull();
		var body = r.getContent().asString();
		assertContains("Query execution failed.", body);
		for (var leak : new String[]{"secret", "person", "SELECT"})
			Assertions.assertFalse(body.contains(leak), () -> "Leaked '" + leak + "' in: " + body);
	}

	@Test void a03_converterSyntaxError_400WithHeaderAndMessage() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().build();
		c.get("/converterSyntax").run()
			.assertStatus(400)
			.assertHeader(BeanQueryRequest.ERROR_HEADER).is("UNKNOWN_COLUMN")
			.assertContent().isContains("Unknown column 'pasword' in search.");
	}

	@Test void a04_converterExecutionError_500GenericBody() throws Exception {
		var c = MockRestClient.create(A.class).ignoreErrors().build();
		var r = c.get("/converterExecution").run().assertStatus(500).assertHeader(BeanQueryRequest.ERROR_HEADER).isNull();
		var body = r.getContent().asString();
		assertContains("Query execution failed.", body);
		Assertions.assertFalse(body.contains("secret"), () -> "Leaked cause in: " + body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Problem details on
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_syntaxError_problemDetailsCarriesCode() throws Exception {
		var c = MockRestClient.create(B.class).ignoreErrors().build();
		c.get("/syntax").run()
			.assertStatus(400)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertHeader(BeanQueryRequest.ERROR_HEADER).is("UNKNOWN_COLUMN")
			.assertContent().isContains("\"code\":\"UNKNOWN_COLUMN\"", "Unknown column 'pasword' in search.");
	}

	@Test void b02_executionError_problemDetailsNoCode() throws Exception {
		var c = MockRestClient.create(B.class).ignoreErrors().build();
		c.get("/execution").run()
			.assertStatus(500)
			.assertHeader(BeanQueryRequest.ERROR_HEADER).isNull()
			.assertContent().isContains("Query execution failed.")
			.assertContent().isNotContains("\"code\"", "secret");
	}

	@Test void b03_converterSyntaxError_problemDetailsCarriesCode() throws Exception {
		var c = MockRestClient.create(B.class).ignoreErrors().build();
		c.get("/converterSyntax").run()
			.assertStatus(400)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertHeader(BeanQueryRequest.ERROR_HEADER).is("UNKNOWN_COLUMN")
			.assertContent().isContains("\"code\":\"UNKNOWN_COLUMN\"", "Unknown column 'pasword' in search.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// convertThrowable directly
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_convertThrowable_keepsCauseForLogging() throws Exception {
		var rc = new RestContext(new RestContext.Args(A.class, null, null, A::new, "", null, null, null, RestContext.ContextKind.ROOT));
		var e = executionError();
		var t = rc.convertThrowable(e);
		assertString("Query execution failed.", t.getMessage());
		Assertions.assertSame(e, t.getCause());
	}
}
