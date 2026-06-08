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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Direct tests for {@link JsrSupport} FQN-based matchers.
 *
 * <p>These tests target the small helper methods that aren't easily reachable through the higher-level
 * bean-store / instantiator integration tests — namely {@code isSingletonAnnotation},
 * {@code isProviderType}, and the null / negative paths.
 */
@SuppressWarnings({
	"java:S2094"  // Intentionally empty bean class used as test fixture.
})
class JsrSupport_Test extends TestBase {

	//------------------------------------------------------------------------------------------------
	// Test fixtures.
	//------------------------------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	private @interface SomeUnrelated {}

	@SomeUnrelated
	@Singleton
	private static class Marker {}

	@SomeUnrelated
	private static class Unmarked {}

	/** Test helper to capture an {@code AnnotationInfo} from a class. */
	private static AnnotationInfo<?> annotationOf(Class<?> source, Class<? extends Annotation> annType) {
		return Arrays.stream(source.getAnnotations())
			.filter(a -> a.annotationType().equals(annType))
			.map(a -> AnnotationInfo.of(ClassInfo.of(source), a))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Annotation not found"));
	}

	//------------------------------------------------------------------------------------------------
	// isSingletonAnnotation.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_isSingletonAnnotation_juneau() {
		var info = annotationOf(Marker.class, Singleton.class);
		assertTrue(JsrSupport.isSingletonAnnotation(info), "Juneau @Singleton must be detected");
	}

	@Test
	void a02_isSingletonAnnotation_unrelated_returnsFalse() {
		var info = annotationOf(Marker.class, SomeUnrelated.class);
		assertFalse(JsrSupport.isSingletonAnnotation(info), "Unrelated annotation must not match @Singleton");
	}

	//------------------------------------------------------------------------------------------------
	// isProviderType — null and negative branches.
	//------------------------------------------------------------------------------------------------

	@Test
	void b01_isProviderType_null_returnsFalse() {
		assertFalse(JsrSupport.isProviderType(null), "null type must not match");
	}

	@Test
	void b02_isProviderType_unrelatedClass_returnsFalse() {
		assertFalse(JsrSupport.isProviderType(String.class),
			"java.lang.String is not a recognized Provider type");
	}

	@Test
	void b03_isProviderType_juneauProvider_returnsTrue() {
		assertTrue(JsrSupport.isProviderType(Provider.class),
			"org.apache.juneau.commons.inject.Provider must be recognized");
	}

	//------------------------------------------------------------------------------------------------
	// isQualifierMeta — negative branch (annotation not annotated with @Qualifier).
	//------------------------------------------------------------------------------------------------

	@Test
	void c01_isQualifierMeta_unrelated_returnsFalse() {
		var info = annotationOf(Unmarked.class, SomeUnrelated.class);
		assertFalse(JsrSupport.isQualifierMeta(info),
			"An annotation type with no @Qualifier meta-annotation must not match");
	}
}
