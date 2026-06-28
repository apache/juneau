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
package org.apache.juneau.rest.server.servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.ops.*;
import org.junit.jupiter.api.*;

/**
 * Annotation and type-hierarchy contract tests for {@link BasicRestResourceGroup}.
 *
 * <p>
 * These tests verify the static contract of the class (annotations, inheritance, declared API) without
 * spinning up a servlet container.  Runtime child-management methods ({@code addChild}/{@code removeChild})
 * require a live {@link RestContext} and are exercised by higher-level integration tests.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with intermediate invocations; non-throwing paths are intentional
})
class BasicRestResourceGroup_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// a — class-level @Rest annotation contract
	// -----------------------------------------------------------------------------------------

	@Test void a01_restAnnotationPresent() {
		assertTrue(BasicRestResourceGroup.class.isAnnotationPresent(Rest.class),
			"BasicRestResourceGroup must carry @Rest");
	}

	@Test void a02_restAnnotation_mixinsContainsNavigationMixin() {
		var rest = BasicRestResourceGroup.class.getAnnotation(Rest.class);
		assertNotNull(rest, "@Rest annotation must be present");
		var mixins = Arrays.asList(rest.mixins());
		assertTrue(mixins.contains(NavigationMixin.class),
			"@Rest(mixins) must include NavigationMixin; actual: " + mixins);
	}

	// -----------------------------------------------------------------------------------------
	// b — type hierarchy
	// -----------------------------------------------------------------------------------------

	@Test void b01_extendsBasicRestResource() {
		assertTrue(BasicRestResource.class.isAssignableFrom(BasicRestResourceGroup.class),
			"BasicRestResourceGroup must extend BasicRestResource");
	}

	@Test void b02_extendsRestResource() {
		assertTrue(RestResource.class.isAssignableFrom(BasicRestResourceGroup.class),
			"BasicRestResourceGroup must be a RestResource (transitively via BasicRestResource)");
	}

	@Test void b03_isAbstract() {
		int mod = BasicRestResourceGroup.class.getModifiers();
		assertTrue(java.lang.reflect.Modifier.isAbstract(mod),
			"BasicRestResourceGroup must be declared abstract — callers must subclass it");
	}

	// -----------------------------------------------------------------------------------------
	// c — public API surface
	// -----------------------------------------------------------------------------------------

	@Test void c01_getChildResources_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("getChildResources");
		assertEquals(RestChildren.class, m.getReturnType(),
			"getChildResources() must return RestChildren");
	}

	@Test void c02_addChild_byClass_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("addChild", Class.class);
		assertEquals(RestContext.class, m.getReturnType(),
			"addChild(Class) must return RestContext");
	}

	@Test void c03_addChild_byObject_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("addChild", Object.class);
		assertEquals(RestContext.class, m.getReturnType(),
			"addChild(Object) must return RestContext");
	}

	@Test void c04_addChild_byPathAndObject_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("addChild", String.class, Object.class);
		assertEquals(RestContext.class, m.getReturnType(),
			"addChild(String, Object) must return RestContext");
	}

	@Test void c05_removeChild_byPath_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("removeChild", String.class);
		assertEquals(RestContext.class, m.getReturnType(),
			"removeChild(String) must return RestContext");
	}

	@Test void c06_removeChild_byClass_methodPresent() throws NoSuchMethodException {
		var m = BasicRestResourceGroup.class.getMethod("removeChild", Class.class);
		assertEquals(RestContext.class, m.getReturnType(),
			"removeChild(Class) must return RestContext");
	}

	@Test void c07_allPublicMethods_noUnexpectedThrowables() {
		// Verify that each child-management method declares at most ServletException
		var childMgmtNames = Set.of("addChild", "removeChild");
		var violations = Arrays.stream(BasicRestResourceGroup.class.getMethods())
			.filter(m -> childMgmtNames.contains(m.getName()))
			.flatMap(m -> Arrays.stream(m.getExceptionTypes()))
			.filter(ex -> !jakarta.servlet.ServletException.class.isAssignableFrom(ex)
				         && !RuntimeException.class.isAssignableFrom(ex))
			.map(Class::getSimpleName)
			.toList();
		assertTrue(violations.isEmpty(),
			"Child-management methods must not declare unexpected checked exceptions: " + violations);
	}
}
