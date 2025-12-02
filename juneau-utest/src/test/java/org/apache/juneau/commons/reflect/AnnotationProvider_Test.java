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
package org.apache.juneau.commons.reflect;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.commons.reflect.AnnotationTraversal.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.junit.jupiter.api.*;

class AnnotationProvider_Test extends TestBase {

	//====================================================================================================
	// Test annotations and classes
	//====================================================================================================

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface TestAnnotation {
		String value() default "default";
	}

	@Target({TYPE, METHOD, FIELD, CONSTRUCTOR, PARAMETER})
	@Retention(RUNTIME)
	public static @interface MultiTargetAnnotation {
		int value() default 0;
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface ParentAnnotation {
		String value() default "";
	}

	@TestAnnotation("class")
	public static class TestClass {
		@MultiTargetAnnotation(1)
		public String field1;

		@MultiTargetAnnotation(2)
		public TestClass() {}

		@MultiTargetAnnotation(3)
		public void method1() {}

		public void method2(@MultiTargetAnnotation(4) String param) {}
	}

	@ParentAnnotation("parent")
	public static class ParentClass {}

	@TestAnnotation("child")
	public static class ChildClass extends ParentClass {}

	// Test classes for MATCHING_METHODS traversal
	public static interface MatchingMethodInterface {
		@MultiTargetAnnotation(10)
		void matchingMethod(String param);
	}

	public static class MatchingMethodParent {
		@MultiTargetAnnotation(20)
		public void matchingMethod(String param) {}
	}

	public static class MatchingMethodChild extends MatchingMethodParent implements MatchingMethodInterface {
		@MultiTargetAnnotation(30)
		@Override
		public void matchingMethod(String param) {}
	}

	// Test classes for MATCHING_PARAMETERS traversal
	public static interface MatchingParameterInterface {
		void matchingParameterMethod(@MultiTargetAnnotation(100) String param);
	}

	public static class MatchingParameterParent {
		public void matchingParameterMethod(@MultiTargetAnnotation(200) String param) {}
	}

	public static class MatchingParameterChild extends MatchingParameterParent implements MatchingParameterInterface {
		@Override
		public void matchingParameterMethod(@MultiTargetAnnotation(300) String param) {}
	}

	//====================================================================================================
	// create() and INSTANCE
	//====================================================================================================

	@Test
	void a01_create_returnsBuilder() {
		var builder = AnnotationProvider.create();
		assertNotNull(builder);
		var provider = builder.build();
		assertNotNull(provider);
	}

	@Test
	void a02_instance_isNotNull() {
		assertNotNull(AnnotationProvider.INSTANCE);
	}

	//====================================================================================================
	// find(Class, ClassInfo, ...) - typed find for classes
	//====================================================================================================

	@Test
	void b01_find_typedClass_returnsAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var annotations = provider.find(TestAnnotation.class, ci, SELF);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals("class", annotations.get(0).getValue().orElse(null));
	}

	@Test
	void b02_find_typedClass_withParents_returnsAnnotationsFromHierarchy() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(ChildClass.class);
		var annotations = provider.find(TestAnnotation.class, ci, SELF, PARENTS);
		
		assertNotNull(annotations);
		// Should find annotation on child class
		assertTrue(annotations.size() >= 1);
		var childAnnotation = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("child"))
			.findFirst();
		assertTrue(childAnnotation.isPresent());
	}

	@Test
	void b03_find_typedClass_notFound_returnsEmptyList() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var annotations = provider.find(ParentAnnotation.class, ci, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.isEmpty());
	}

	@Test
	void b04_find_typedClass_withNullType_throwsException() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		assertThrows(IllegalArgumentException.class, () -> provider.find(null, ci, SELF));
	}

	@Test
	void b05_find_typedClass_withNullClassInfo_throwsException() {
		var provider = AnnotationProvider.create().build();
		ClassInfo nullClassInfo = null;
		assertThrows(IllegalArgumentException.class, () -> provider.find(TestAnnotation.class, nullClassInfo, SELF));
	}

	//====================================================================================================
	// find(ClassInfo, ...) - untyped find for classes
	//====================================================================================================

	@Test
	void c01_find_untypedClass_returnsAllAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var annotations = provider.find(ci, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
		assertTrue(annotations.stream().anyMatch(a -> a.isType(TestAnnotation.class)));
	}

	@Test
	void c02_find_untypedClass_withNullClassInfo_throwsException() {
		var provider = AnnotationProvider.create().build();
		assertThrows(IllegalArgumentException.class, () -> provider.find((ClassInfo)null, SELF));
	}

	//====================================================================================================
	// find(Class, FieldInfo, ...) - typed find for fields
	//====================================================================================================

	@Test
	void d01_find_typedField_returnsAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		var annotations = provider.find(MultiTargetAnnotation.class, field, SELF);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals(1, annotations.get(0).getInt("value").orElse(0));
	}

	@Test
	void d03_find_typedField_withRuntimeAnnotations_includesRuntimeAndDeclared() {
		// This test covers lines 1039-1041 - SELF traversal for FieldInfo with runtime annotations
		var runtimeAnnotation = new RuntimeOnAnnotation(new String[]{"org.apache.juneau.commons.reflect.AnnotationProvider_Test$TestClass.field1"}, "runtimeField");
		var provider = AnnotationProvider.create()
			.addRuntimeAnnotations(runtimeAnnotation)
			.build();
		var ci = ClassInfo.of(TestClass.class);
		var field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		// Call with SELF traversal - should include both runtime annotations (line 1040) and declared annotations (line 1041)
		var annotations = provider.find(TestAnnotation.class, field, SELF);
		
		assertNotNull(annotations);
		// Should find both the runtime annotation and the declared annotation (if any)
		assertTrue(annotations.size() >= 1, "Should find at least the runtime annotation");
		
		// Verify runtime annotation is found
		var runtimeAnnotationFound = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("runtimeField"))
			.findFirst();
		assertTrue(runtimeAnnotationFound.isPresent(), "Should find runtime annotation on field");
	}

	@Test
	void d02_find_typedField_notFound_returnsEmptyList() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		var annotations = provider.find(TestAnnotation.class, field, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.isEmpty());
	}

	//====================================================================================================
	// find(FieldInfo, ...) - untyped find for fields
	//====================================================================================================

	@Test
	void e01_find_untypedField_returnsAllAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		var annotations = provider.find(field, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
		assertTrue(annotations.stream().anyMatch(a -> a.isType(MultiTargetAnnotation.class)));
	}

	//====================================================================================================
	// find(Class, ConstructorInfo, ...) - typed find for constructors
	//====================================================================================================

	@Test
	void f01_find_typedConstructor_returnsAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		var annotations = provider.find(MultiTargetAnnotation.class, constructor, SELF);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals(2, annotations.get(0).getInt("value").orElse(0));
	}

	//====================================================================================================
	// find(ConstructorInfo, ...) - untyped find for constructors
	//====================================================================================================

	@Test
	void g01_find_untypedConstructor_returnsAllAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		var annotations = provider.find(constructor, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
		assertTrue(annotations.stream().anyMatch(a -> a.isType(MultiTargetAnnotation.class)));
	}

	//====================================================================================================
	// find(Class, MethodInfo, ...) - typed find for methods
	//====================================================================================================

	@Test
	void h01_find_typedMethod_returnsAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		var annotations = provider.find(MultiTargetAnnotation.class, method, SELF);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals(3, annotations.get(0).getInt("value").orElse(0));
	}

	//====================================================================================================
	// find(MethodInfo, ...) - untyped find for methods
	//====================================================================================================

	@Test
	void i01_find_untypedMethod_returnsAllAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		var annotations = provider.find(method, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
		assertTrue(annotations.stream().anyMatch(a -> a.isType(MultiTargetAnnotation.class)));
	}

	//====================================================================================================
	// find(Class, ParameterInfo, ...) - typed find for parameters
	//====================================================================================================

	@Test
	void j01_find_typedParameter_returnsAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method2")).orElse(null);
		assertNotNull(method);
		ParameterInfo param = method.getParameter(0);
		assertNotNull(param);
		
		var annotations = provider.find(MultiTargetAnnotation.class, param, SELF);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
		assertEquals(4, annotations.get(0).getInt("value").orElse(0));
	}

	//====================================================================================================
	// find(ParameterInfo, ...) - untyped find for parameters
	//====================================================================================================

	@Test
	void k01_find_untypedParameter_returnsAllAnnotations() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method2")).orElse(null);
		assertNotNull(method);
		var param = method.getParameter(0);
		assertNotNull(param);
		
		var annotations = provider.find(param, SELF);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
		assertTrue(annotations.stream().anyMatch(a -> a.isType(MultiTargetAnnotation.class)));
	}

	//====================================================================================================
	// has(Class, ClassInfo, ...) - check existence for classes
	//====================================================================================================

	@Test
	void l01_has_typedClass_exists_returnsTrue() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		
		assertTrue(provider.has(TestAnnotation.class, ci, SELF));
	}

	@Test
	void l02_has_typedClass_notExists_returnsFalse() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		
		assertFalse(provider.has(ParentAnnotation.class, ci, SELF));
	}

	//====================================================================================================
	// has(Class, ConstructorInfo, ...) - check existence for constructors
	//====================================================================================================

	@Test
	void l03_has_typedConstructor_exists_returnsTrue() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		ConstructorInfo constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		assertTrue(provider.has(MultiTargetAnnotation.class, constructor, SELF));
	}

	@Test
	void l04_has_typedConstructor_notExists_returnsFalse() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		ConstructorInfo constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		assertFalse(provider.has(TestAnnotation.class, constructor, SELF));
	}

	//====================================================================================================
	// has(Class, FieldInfo, ...) - check existence for fields
	//====================================================================================================

	@Test
	void l05_has_typedField_exists_returnsTrue() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		FieldInfo field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		assertTrue(provider.has(MultiTargetAnnotation.class, field, SELF));
	}

	@Test
	void l06_has_typedField_notExists_returnsFalse() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		FieldInfo field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		assertFalse(provider.has(TestAnnotation.class, field, SELF));
	}

	//====================================================================================================
	// has(Class, MethodInfo, ...) - check existence for methods
	//====================================================================================================

	@Test
	void l07_has_typedMethod_exists_returnsTrue() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		assertTrue(provider.has(MultiTargetAnnotation.class, method, SELF));
	}

	@Test
	void l08_has_typedMethod_notExists_returnsFalse() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		assertFalse(provider.has(TestAnnotation.class, method, SELF));
	}

	//====================================================================================================
	// has(Class, ParameterInfo, ...) - check existence for parameters
	//====================================================================================================

	@Test
	void l09_has_typedParameter_exists_returnsTrue() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method2")).orElse(null);
		assertNotNull(method);
		ParameterInfo param = method.getParameter(0);
		assertNotNull(param);
		
		assertTrue(provider.has(MultiTargetAnnotation.class, param, SELF));
	}

	@Test
	void l10_has_typedParameter_notExists_returnsFalse() {
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method2")).orElse(null);
		assertNotNull(method);
		ParameterInfo param = method.getParameter(0);
		assertNotNull(param);
		
		assertFalse(provider.has(TestAnnotation.class, param, SELF));
	}

	//====================================================================================================
	// Default traversal logic (lines 991-998) - when no traversals specified
	//====================================================================================================

	@Test
	void l11_find_typedClass_noTraversals_usesDefaultTraversals() {
		// This test covers line 991-992 - default traversals for ClassInfo (PARENTS, PACKAGE)
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(ChildClass.class);
		
		// Call with no traversals - should use default (PARENTS, PACKAGE)
		var annotations = provider.find(TestAnnotation.class, ci);
		
		assertNotNull(annotations);
		// Should find annotation from child class
		assertTrue(annotations.size() >= 1);
	}

	@Test
	void l12_find_typedMethod_noTraversals_usesDefaultTraversals() {
		// This test covers line 993-994 - default traversals for MethodInfo
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		// Call with no traversals - should use default (SELF, MATCHING_METHODS, DECLARING_CLASS, RETURN_TYPE, PACKAGE)
		var annotations = provider.find(MultiTargetAnnotation.class, method);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
	}

	@Test
	void l13_find_typedField_noTraversals_usesDefaultTraversals() {
		// This test covers line 995-996 - default traversals for FieldInfo (SELF)
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		FieldInfo field = ci.getPublicField(x -> x.hasName("field1")).orElse(null);
		assertNotNull(field);
		
		// Call with no traversals - should use default (SELF)
		var annotations = provider.find(MultiTargetAnnotation.class, field);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
	}

	@Test
	void l14_find_typedConstructor_noTraversals_usesDefaultTraversals() {
		// This test covers line 995-996 - default traversals for ConstructorInfo (SELF)
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		ConstructorInfo constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		// Call with no traversals - should use default (SELF)
		var annotations = provider.find(MultiTargetAnnotation.class, constructor);
		
		assertNotNull(annotations);
		assertEquals(1, annotations.size());
	}

	@Test
	void l15_find_typedParameter_noTraversals_usesDefaultTraversals() {
		// This test covers line 997-998 - default traversals for ParameterInfo
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(TestClass.class);
		var method = ci.getPublicMethod(x -> x.hasName("method2")).orElse(null);
		assertNotNull(method);
		ParameterInfo param = method.getParameter(0);
		assertNotNull(param);
		
		// Call with no traversals - should use default (SELF, MATCHING_PARAMETERS, PARAMETER_TYPE)
		var annotations = provider.find(MultiTargetAnnotation.class, param);
		
		assertNotNull(annotations);
		assertTrue(annotations.size() >= 1);
	}

	@Test
	void l16_find_typedClass_withPackageTraversal_nullPackage_handlesGracefully() {
		// This test covers line 1014 - when getPackage() returns null
		// Primitive types and arrays of primitives have no package
		var provider = AnnotationProvider.create().build();
		
		// int.class has no package (getPackage() returns null)
		var ci = ClassInfo.of(int.class);
		assertNull(ci.getPackage(), "int.class should have no package");
		
		// Call with PACKAGE traversal - should handle null package gracefully
		var annotations = provider.find(TestAnnotation.class, ci, AnnotationTraversal.PACKAGE);
		
		// Should not throw exception, just return empty list
		assertNotNull(annotations);
		assertEquals(0, annotations.size());
	}

	@Test
	void l17_find_typedMethod_withMatchingMethodsTraversal_includesParentAndInterfaceMethods() {
		// This test covers lines 1024-1025 - MATCHING_METHODS traversal
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(MatchingMethodChild.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("matchingMethod")).orElse(null);
		assertNotNull(method);
		
		// Verify the matching methods order: [child, interface, parent]
		var matchingMethods = method.getMatchingMethods();
		assertTrue(matchingMethods.size() >= 3, "Should have at least child, interface, and parent methods. Found: " + matchingMethods.size());
		
		// Verify we can find the parent method in the matching methods
		var parentMethod = matchingMethods.stream()
			.filter(m -> MatchingMethodParent.class.equals(m.getDeclaringClass().inner()))
			.findFirst();
		assertTrue(parentMethod.isPresent(), "Parent method should be in matching methods");
		
		// Verify the parent method has the annotation
		var parentMethodAnnotations = parentMethod.get().getDeclaredAnnotations(MultiTargetAnnotation.class).toList();
		assertTrue(parentMethodAnnotations.size() > 0, "Parent method should have annotation. Found " + parentMethodAnnotations.size() + " annotations");
		var parentMethodAnnotation = parentMethodAnnotations.get(0);
		// Try getInt first, then getValue as fallback
		var parentAnnotationValue = parentMethodAnnotation.getInt("value").orElse(null);
		if (parentAnnotationValue == null) {
			parentAnnotationValue = parentMethodAnnotation.getValue(Integer.class, "value").orElse(null);
		}
		assertEquals(20, parentAnnotationValue, "Parent method annotation should have value 20. Annotation: " + parentMethodAnnotation);
		
		// Skip the first (child method) - should have interface and parent
		var methodsAfterSkip = matchingMethods.stream().skip(1).toList();
		assertTrue(methodsAfterSkip.size() >= 2, "Should have interface and parent methods after skipping child");
		
		// Call with MATCHING_METHODS traversal - should include annotations from parent and interface methods
		// Note: skip(1) skips the child method itself, so we get interface and parent
		var annotations = provider.find(MultiTargetAnnotation.class, method, MATCHING_METHODS);
		
		assertNotNull(annotations);
		// Should find annotations from:
		// 1. Interface method (value=10) - from declared interfaces of child class
		// 2. Parent class method (value=20) - from parent class
		// Note: The child method itself is skipped by .skip(1)
		assertTrue(annotations.size() >= 2, "Should find annotations from parent and interface matching methods. Found: " + annotations.size());
		
		// Debug: print what we found
		var foundValues = annotations.stream()
			.map(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val;
			})
			.filter(v -> v != null)
			.toList();
		
		// Verify we have the interface annotation (value=10) - comes first after skip(1)
		var interfaceAnnotation = annotations.stream()
			.filter(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val != null && val.intValue() == 10;
			})
			.findFirst();
		assertTrue(interfaceAnnotation.isPresent(), "Should find annotation from interface method. Found values: " + foundValues);
		
		// Verify we have the parent annotation (value=20) - comes after interface
		var parentAnnotation = annotations.stream()
			.filter(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val != null && val.intValue() == 20;
			})
			.findFirst();
		assertTrue(parentAnnotation.isPresent(), "Should find annotation from parent class method. Found values: " + foundValues + ", matching methods count: " + matchingMethods.size());
	}

	@Test
	void l18_find_typedParameter_withMatchingParametersTraversal_includesParentAndInterfaceParameters() {
		// This test covers line 1054 - MATCHING_PARAMETERS traversal
		var provider = AnnotationProvider.create().build();
		var ci = ClassInfo.of(MatchingParameterChild.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("matchingParameterMethod")).orElse(null);
		assertNotNull(method);
		ParameterInfo param = method.getParameter(0);
		assertNotNull(param);
		
		// Verify the matching parameters order: [child, interface, parent]
		var matchingParameters = param.getMatchingParameters();
		assertTrue(matchingParameters.size() >= 3, "Should have at least child, interface, and parent parameters. Found: " + matchingParameters.size());
		
		// Verify we can find the parent parameter in the matching parameters
		var parentParameter = matchingParameters.stream()
			.filter(p -> {
				var paramMethod = p.getMethod();
				if (paramMethod != null) {
					return MatchingParameterParent.class.equals(paramMethod.getDeclaringClass().inner());
				}
				return false;
			})
			.findFirst();
		assertTrue(parentParameter.isPresent(), "Parent parameter should be in matching parameters");
		
		// Verify the parent parameter has the annotation
		var parentParameterAnnotations = parentParameter.get().getAnnotations(MultiTargetAnnotation.class).toList();
		assertTrue(parentParameterAnnotations.size() > 0, "Parent parameter should have annotation. Found " + parentParameterAnnotations.size() + " annotations");
		var parentParameterAnnotation = parentParameterAnnotations.get(0);
		var parentParameterValue = parentParameterAnnotation.getInt("value").orElse(null);
		if (parentParameterValue == null) {
			parentParameterValue = parentParameterAnnotation.getValue(Integer.class, "value").orElse(null);
		}
		assertEquals(200, parentParameterValue, "Parent parameter annotation should have value 200. Annotation: " + parentParameterAnnotation);
		
		// Skip the first (child parameter) - should have interface and parent
		var parametersAfterSkip = matchingParameters.stream().skip(1).toList();
		assertTrue(parametersAfterSkip.size() >= 2, "Should have interface and parent parameters after skipping child");
		
		// Call with MATCHING_PARAMETERS traversal - should include annotations from parent and interface parameters
		// Note: skip(1) skips the child parameter itself, so we get interface and parent
		var annotations = provider.find(MultiTargetAnnotation.class, param, MATCHING_PARAMETERS);
		
		assertNotNull(annotations);
		// Should find annotations from:
		// 1. Interface parameter (value=100) - from declared interfaces of child class
		// 2. Parent class parameter (value=200) - from parent class
		// Note: The child parameter itself is skipped by .skip(1)
		assertTrue(annotations.size() >= 2, "Should find annotations from parent and interface matching parameters. Found: " + annotations.size());
		
		// Debug: print what we found
		var foundValues = annotations.stream()
			.map(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val;
			})
			.filter(v -> v != null)
			.toList();
		
		// Verify we have the interface parameter annotation (value=100) - comes first after skip(1)
		var interfaceAnnotation = annotations.stream()
			.filter(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val != null && val.intValue() == 100;
			})
			.findFirst();
		assertTrue(interfaceAnnotation.isPresent(), "Should find annotation from interface parameter. Found values: " + foundValues);
		
		// Verify we have the parent parameter annotation (value=200) - comes after interface
		var parentAnnotation = annotations.stream()
			.filter(a -> {
				Integer val = a.getInt("value").orElse(null);
				if (val == null) {
					val = a.getValue(Integer.class, "value").orElse(null);
				}
				return val != null && val.intValue() == 200;
			})
			.findFirst();
		assertTrue(parentAnnotation.isPresent(), "Should find annotation from parent class parameter. Found values: " + foundValues + ", matching parameters count: " + matchingParameters.size());
	}

	@Test
	void l19_find_typedMethod_withRuntimeAnnotation_loadsFromAnnotationMap() {
		// This test covers line 1072 - load() method for Method objects
		var runtimeAnnotation = new RuntimeOnAnnotation(new String[]{"org.apache.juneau.commons.reflect.AnnotationProvider_Test$TestClass.method1"}, "runtimeMethod");
		var provider = AnnotationProvider.create()
			.addRuntimeAnnotations(runtimeAnnotation)
			.build();
		var ci = ClassInfo.of(TestClass.class);
		MethodInfo method = ci.getPublicMethod(x -> x.hasName("method1")).orElse(null);
		assertNotNull(method);
		
		// Call with SELF traversal - should trigger runtimeCache.get() which calls load() for Method
		// This covers line 1072: annotationMap.find(mi.inner())
		var annotations = provider.find(TestAnnotation.class, method, SELF);
		
		assertNotNull(annotations);
		// Should find the runtime annotation
		assertTrue(annotations.size() >= 1, "Should find at least the runtime annotation");
		
		// Verify runtime annotation is found
		var runtimeAnnotationFound = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("runtimeMethod"))
			.findFirst();
		assertTrue(runtimeAnnotationFound.isPresent(), "Should find runtime annotation on method");
	}

	@Test
	void l20_find_typedConstructor_withRuntimeAnnotation_loadsFromAnnotationMap() {
		// This test covers line 1080 - load() method for Constructor objects
		// Constructor format for ReflectionMap is "ClassName()" for no-arg constructor
		var runtimeAnnotation = new RuntimeOnAnnotation(new String[]{"org.apache.juneau.commons.reflect.AnnotationProvider_Test$TestClass()"}, "runtimeConstructor");
		var provider = AnnotationProvider.create()
			.addRuntimeAnnotations(runtimeAnnotation)
			.build();
		var ci = ClassInfo.of(TestClass.class);
		ConstructorInfo constructor = ci.getPublicConstructor(x -> x.getParameterCount() == 0).orElse(null);
		assertNotNull(constructor);
		
		// Call with SELF traversal - should trigger runtimeCache.get() which calls load() for Constructor
		// This covers line 1080: annotationMap.find(ci.inner())
		var annotations = provider.find(TestAnnotation.class, constructor, SELF);
		
		assertNotNull(annotations);
		// Should find the runtime annotation
		assertTrue(annotations.size() >= 1, "Should find at least the runtime annotation");
		
		// Verify runtime annotation is found
		var runtimeAnnotationFound = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("runtimeConstructor"))
			.findFirst();
		assertTrue(runtimeAnnotationFound.isPresent(), "Should find runtime annotation on constructor");
	}

	//====================================================================================================
	// Builder methods
	//====================================================================================================

	@Test
	void m01_builder_cacheMode_buildsProvider() {
		var provider = AnnotationProvider.create()
			.cacheMode(CacheMode.NONE)
			.build();
		assertNotNull(provider);
	}

	@Test
	void m02_builder_logOnExit_buildsProvider() {
		var provider = AnnotationProvider.create()
			.logOnExit()
			.build();
		assertNotNull(provider);
	}

	@Test
	void m03_builder_logOnExit_withBoolean_coversLine402() {
		// This test covers line 402 - logOnExit(boolean value)
		var provider1 = AnnotationProvider.create()
			.logOnExit(true)
			.build();
		assertNotNull(provider1);
		
		var provider2 = AnnotationProvider.create()
			.logOnExit(false)
			.build();
		assertNotNull(provider2);
	}

	@Test
	void m04_builder_chaining_buildsProvider() {
		var provider = AnnotationProvider.create()
			.cacheMode(CacheMode.NONE)
			.logOnExit()
			.build();
		assertNotNull(provider);
	}

	//====================================================================================================
	// addRuntimeAnnotations(Annotation...) - varargs version
	//====================================================================================================

	// Simple runtime annotation implementation for testing with onClass()
	private static class RuntimeTestAnnotation implements TestAnnotation {
		private final Class<?>[] onClass;
		private final String value;

		RuntimeTestAnnotation(Class<?>[] onClass, String value) {
			this.onClass = onClass;
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return TestAnnotation.class;
		}

		public Class<?>[] onClass() {
			return onClass;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof TestAnnotation))
				return false;
			TestAnnotation other = (TestAnnotation)obj;
			return value.equals(other.value());
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return "@TestAnnotation(value=" + value + ")";
		}
	}

	// Runtime annotation implementation with on() method (String[] targeting)
	private static class RuntimeOnAnnotation implements TestAnnotation {
		private final String[] on;
		private final String value;

		RuntimeOnAnnotation(String[] on, String value) {
			this.on = on;
			this.value = value;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return TestAnnotation.class;
		}

		public String[] on() {
			return on;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof TestAnnotation))
				return false;
			TestAnnotation other = (TestAnnotation)obj;
			return value.equals(other.value());
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return "@TestAnnotation(value=" + value + ")";
		}
	}

	@Test
	void n01_addRuntimeAnnotations_varargs_callsListVersion() {
		// This test covers line 245 - the varargs version that converts to list
		var runtimeAnnotation1 = new RuntimeTestAnnotation(new Class<?>[]{TestClass.class}, "runtime1");
		var runtimeAnnotation2 = new RuntimeTestAnnotation(new Class<?>[]{ParentClass.class}, "runtime2");
		
		var provider = AnnotationProvider.create()
			.addRuntimeAnnotations(runtimeAnnotation1, runtimeAnnotation2)  // Varargs - covers line 245
			.build();
		
		assertNotNull(provider);
		
		// Verify the runtime annotations are applied
		var ci = ClassInfo.of(TestClass.class);
		var annotations = provider.find(TestAnnotation.class, ci, SELF);
		
		// Should find the runtime annotation
		assertTrue(annotations.size() >= 1);
		var runtimeAnnotation = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("runtime1"))
			.findFirst();
		assertTrue(runtimeAnnotation.isPresent());
	}

	@Test
	void n04_addRuntimeAnnotations_withOnMethod_coversLine343() {
		// This test covers line 343 - the on() method returning String[] is processed
		var className = TestClass.class.getName();
		var runtimeAnnotation = new RuntimeOnAnnotation(new String[]{className}, "runtimeOn");
		
		var provider = AnnotationProvider.create()
			.addRuntimeAnnotations(runtimeAnnotation)
			.build();
		
		assertNotNull(provider);
		
		// Verify the runtime annotation is applied using on() method
		var ci = ClassInfo.of(TestClass.class);
		var annotations = provider.find(TestAnnotation.class, ci, SELF);
		
		// Should find the runtime annotation
		assertTrue(annotations.size() >= 1);
		var runtimeAnnotationFound = annotations.stream()
			.filter(a -> a.getValue().orElse("").equals("runtimeOn"))
			.findFirst();
		assertTrue(runtimeAnnotationFound.isPresent());
	}

	// Runtime annotation with invalid onClass() return type (not Class[])
	private static class InvalidOnClassAnnotation implements TestAnnotation {
		@Override
		public String value() {
			return "invalid";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return TestAnnotation.class;
		}

		public String onClass() {  // Wrong return type - should be Class[]
			return "invalid";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestAnnotation;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return "@TestAnnotation";
		}
	}

	// Runtime annotation with invalid on() return type (not String[])
	private static class InvalidOnAnnotation implements TestAnnotation {
		@Override
		public String value() {
			return "invalid";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return TestAnnotation.class;
		}

		public String on() {  // Wrong return type - should be String[]
			return "invalid";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestAnnotation;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return "@TestAnnotation";
		}
	}

	@Test
	void n02_addRuntimeAnnotations_invalidOnClassReturnType_throwsException() {
		// This test covers line 334 - onClass() method with wrong return type
		var invalidAnnotation = new InvalidOnClassAnnotation();
		
		assertThrows(BeanRuntimeException.class, () -> {
			AnnotationProvider.create()
				.addRuntimeAnnotations(invalidAnnotation)
				.build();
		});
	}

	@Test
	void n03_addRuntimeAnnotations_invalidOnReturnType_throwsException() {
		// This test covers line 341 - on() method with wrong return type
		var invalidAnnotation = new InvalidOnAnnotation();
		
		assertThrows(BeanRuntimeException.class, () -> {
			AnnotationProvider.create()
				.addRuntimeAnnotations(invalidAnnotation)
				.build();
		});
	}

	// Runtime annotation that throws an exception when onClass() is invoked
	private static class ThrowingOnClassAnnotation implements TestAnnotation {
		@Override
		public String value() {
			return "throwing";
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return TestAnnotation.class;
		}

		public Class<?>[] onClass() {
			throw new RuntimeException("Test exception from onClass()");
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TestAnnotation;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String toString() {
			return "@TestAnnotation";
		}
	}

	@Test
	void n05_addRuntimeAnnotations_throwingOnClass_coversLine349() {
		// This test covers line 349 - exception during method invocation (not BeanRuntimeException)
		var throwingAnnotation = new ThrowingOnClassAnnotation();
		
		// The exception from onClass() will be caught and wrapped in BeanRuntimeException
		assertThrows(BeanRuntimeException.class, () -> {
			AnnotationProvider.create()
				.addRuntimeAnnotations(throwingAnnotation)
				.build();
		});
	}
}

