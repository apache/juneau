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
package org.apache.juneau.rest;

import static org.apache.juneau.rest.RestServerConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link RestContext#getApiFormat()} resolution precedence:
 * annotation &gt; system property &gt; default ({@code "swagger"}).
 */
class Rest_ApiFormat_Resolution_Test extends TestBase {

	private static RestContext build(Class<?> resourceClass) throws Exception {
		var resource = resourceClass.getDeclaredConstructor().newInstance();
		return new RestContext(new RestContext.Args(resourceClass, null, null, () -> resource, "", null));
	}

	@Rest
	public static class Default {}

	@Rest(apiFormat="openapi")
	public static class OpenApi {}

	@Rest(apiFormat="both")
	public static class Both {}

	@Rest(apiFormat="BOTH")
	public static class BothUpper {}

	@Rest(apiFormat="bogus")
	public static class Bogus {}

	@Test void a01_default_noOverride() throws Exception {
		System.clearProperty(SYSPROP_apiFormat);
		assertEquals("swagger", build(Default.class).getApiFormat());
	}

	@Test void a02_annotation_openapi() throws Exception {
		System.clearProperty(SYSPROP_apiFormat);
		assertEquals("openapi", build(OpenApi.class).getApiFormat());
	}

	@Test void a03_annotation_both() throws Exception {
		System.clearProperty(SYSPROP_apiFormat);
		assertEquals("both", build(Both.class).getApiFormat());
	}

	@Test void a04_annotation_caseInsensitive() throws Exception {
		System.clearProperty(SYSPROP_apiFormat);
		assertEquals("both", build(BothUpper.class).getApiFormat());
	}

	@Test void a05_annotation_unknownValue_fallsBackToSwagger() throws Exception {
		System.clearProperty(SYSPROP_apiFormat);
		assertEquals("swagger", build(Bogus.class).getApiFormat());
	}

	@Test void a06_systemProperty_takesEffectWhenAnnotationEmpty() throws Exception {
		System.setProperty(SYSPROP_apiFormat, "openapi");
		try {
			assertEquals("openapi", build(Default.class).getApiFormat());
		} finally {
			System.clearProperty(SYSPROP_apiFormat);
		}
	}

	@Test void a07_annotationOverridesSystemProperty() throws Exception {
		System.setProperty(SYSPROP_apiFormat, "swagger");
		try {
			assertEquals("openapi", build(OpenApi.class).getApiFormat());
		} finally {
			System.clearProperty(SYSPROP_apiFormat);
		}
	}

	@Test void a08_systemProperty_unknownValue_fallsBackToSwagger() throws Exception {
		System.setProperty(SYSPROP_apiFormat, "garbage");
		try {
			assertEquals("swagger", build(Default.class).getApiFormat());
		} finally {
			System.clearProperty(SYSPROP_apiFormat);
		}
	}
}
