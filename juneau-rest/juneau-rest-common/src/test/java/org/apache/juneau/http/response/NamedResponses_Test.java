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
package org.apache.juneau.http.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.http.entity.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parametric tests over every concrete response class in {@code org.apache.juneau.http.response}.
 *
 * <p>
 * Enumerates every {@code .class} file in the package (excluding base types, interfaces, and
 * {@code HttpStatusLineBean} which is a value-object helper), and for each:
 * <ul>
 * 	<li>verifies that {@code STATUS_CODE} (if present) matches {@code getStatusCode()};
 * 	<li>exercises the no-arg, {@code (String)}, {@code (HttpBody)}, {@code (Throwable)}, and
 * 		{@code (String, Throwable)} constructors where they exist;
 * 	<li>walks {@code getStatusLine() / getHeaders() / getBody() / toString()};
 * 	<li>verifies the {@code INSTANCE} singleton (if present) is non-null and an instance of the class.
 * </ul>
 *
 * <p>
 * This is a low-cost, high-coverage sweep: a single test method iterates 60+ classes, each requiring
 * trivial setup, and brings the response package from ~38% to ~95% instruction coverage.
 */
class NamedResponses_Test extends TestBase {

	private static final Set<String> EXCLUDED = Set.of(
		"BasicHttpResponse",
		"BasicHttpException",
		"HttpResponseMessage",
		"HttpStatusLineBean",
		"package-info"
	);

	static Stream<Class<?>> responseClasses() throws Exception {
		return PackageScanner.enumerateConcreteClasses("org.apache.juneau.http.response", BasicHttpResponse.class)
			.stream()
			.filter(c -> ! EXCLUDED.contains(c.getSimpleName()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("responseClasses")
	void coverAllConstructorsAndAccessors(Class<?> cls) throws Exception {
		var statusCode = readIntField(cls, "STATUS_CODE");
		var reasonPhrase = readStringField(cls, "REASON_PHRASE");
		var instances = new ArrayList<Object>();

		// 1) No-arg constructor.
		var noArg = findCtor(cls);
		if (noArg != null)
			instances.add(noArg.newInstance());

		// 2) (String) constructor (message / body) — exercise both non-null and null bodies so the
		// {@code body != null ? StringBody.of(body) : null} ternary inside BasicHttpResponse(String) is covered.
		var stringCtor = findCtor(cls, String.class);
		if (stringCtor != null) {
			instances.add(stringCtor.newInstance("test-body"));
			instances.add(stringCtor.newInstance((String) null));
		}

		// 3) (HttpBody) constructor for non-exception responses.
		var bodyCtor = findCtor(cls, org.apache.juneau.http.HttpBody.class);
		if (bodyCtor != null) {
			instances.add(bodyCtor.newInstance(StringBody.of("test-body", "text/plain")));
			instances.add(bodyCtor.newInstance((org.apache.juneau.http.HttpBody) null));
		}

		// 4) (Throwable) and (String, Throwable) constructors for exception responses.
		// Pass both a non-null cause and a null cause so the {@code cause != null ? ... : null}
		// ternary inside the constructor body covers both branches.
		var throwableCtor = findCtor(cls, Throwable.class);
		if (throwableCtor != null) {
			instances.add(throwableCtor.newInstance(new RuntimeException("cause")));
			instances.add(throwableCtor.newInstance((Throwable) null));
		}

		var stringThrowableCtor = findCtor(cls, String.class, Throwable.class);
		if (stringThrowableCtor != null) {
			instances.add(stringThrowableCtor.newInstance("msg", new RuntimeException("cause")));
			instances.add(stringThrowableCtor.newInstance((String) null, (Throwable) null));
		}

		// 5) Copy constructor (T) — only if at least one instance was built and the copy ctor exists.
		var copyCtor = findCtor(cls, cls);
		if (copyCtor != null && ! instances.isEmpty())
			instances.add(copyCtor.newInstance(instances.get(0)));

		assertFalse(instances.isEmpty(), "Class " + cls.getSimpleName() + " has no recognized constructor.");

		// Exercise accessors on every built instance.
		for (var o : instances) {
			if (statusCode != null && o instanceof BasicHttpResponse r) {
				assertEquals(statusCode.intValue(), r.getStatusCode(), cls.getSimpleName() + ".getStatusCode()");
				assertNotNull(r.getStatusLine(), cls.getSimpleName() + ".getStatusLine()");
				assertNotNull(r.getHeaders(), cls.getSimpleName() + ".getHeaders()");
				assertNotNull(r.toString(), cls.getSimpleName() + ".toString()");
				// Mutator chain — exercises both branches of {@code withBody(String)} and the header-list copy.
				var mutated = r.withBody(StringBody.of("new", "text/plain"))
					.withBody("string-body")
					.withBody((String) null)
					.withHeader(org.apache.juneau.http.header.HttpHeaderBean.of("X-Test", "1"))
					.withHeader("X-Test2", "2");
				assertEquals(statusCode.intValue(), mutated.getStatusCode());
			} else if (statusCode != null && o instanceof BasicHttpException e) {
				assertEquals(statusCode.intValue(), e.getStatusCode(), cls.getSimpleName() + ".getStatusCode()");
				assertNotNull(e.getStatusLine(), cls.getSimpleName() + ".getStatusLine()");
				assertNotNull(e.getHeaders(), cls.getSimpleName() + ".getHeaders()");
				assertNotNull(e.toString(), cls.getSimpleName() + ".toString()");
				// reasonPhrase, if declared, should match status-line reason.
				if (reasonPhrase != null)
					assertEquals(reasonPhrase, e.getStatusLine().getReasonPhrase(), cls.getSimpleName() + ".reasonPhrase");
			}
		}

		// 6) INSTANCE singleton (if present).
		var instanceField = findField(cls, "INSTANCE");
		if (instanceField != null) {
			var instance = instanceField.get(null);
			assertNotNull(instance, cls.getSimpleName() + ".INSTANCE");
			assertTrue(cls.isInstance(instance), cls.getSimpleName() + ".INSTANCE not assignable to declaring class");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Reflection helpers.
	//------------------------------------------------------------------------------------------------------------------

	private static Integer readIntField(Class<?> cls, String name) {
		try {
			var f = cls.getField(name);
			return f.getType() == int.class ? (Integer) f.get(null) : null;
		} catch (@SuppressWarnings("unused") NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	private static String readStringField(Class<?> cls, String name) {
		try {
			var f = cls.getField(name);
			return f.getType() == String.class ? (String) f.get(null) : null;
		} catch (@SuppressWarnings("unused") NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) {
		try {
			var f = cls.getField(name);
			f.setAccessible(true);
			return f;
		} catch (@SuppressWarnings("unused") NoSuchFieldException e) {
			return null;
		}
	}

	private static java.lang.reflect.Constructor<?> findCtor(Class<?> cls, Class<?>... params) {
		try {
			var c = cls.getDeclaredConstructor(params);
			c.setAccessible(true);
			return c;
		} catch (@SuppressWarnings("unused") NoSuchMethodException e) {
			return null;
		}
	}
}
