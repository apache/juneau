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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.lang.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage-gap tests for {@link ObjectUtils}, closing branch combinations not
 * exercised by the primary {@link ObjectUtils_Test} (mixed-type comparisons, null-argument
 * permutations on two-line-per-branch {@code if} statements, and the less-common numeric
 * subtypes of {@link #abs(Number)}).
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class ObjectUtils_Coverage_Test extends TestBase {

	//====================================================================================================
	// equal(Object,Object) - Annotation and mixed-array branches
	//====================================================================================================
	@Test
	void a01_equal_bothAnnotations() throws Exception {
		Object ann1 = getClass().getDeclaredMethod("a01_equal_bothAnnotations").getAnnotation(Test.class);
		Object ann2 = getClass().getDeclaredMethod("a02_equal_annotationVsNonAnnotation").getAnnotation(Test.class);
		// @Test has no members, so per the Annotation contract any two instances are equal.
		assertTrue(equal(ann1, ann2));
	}

	@Test
	void a02_equal_annotationVsNonAnnotation() throws Exception {
		Object ann1 = getClass().getDeclaredMethod("a02_equal_annotationVsNonAnnotation").getAnnotation(Test.class);
		assertFalse(equal(ann1, "not-an-annotation"));
	}

	@Test
	void a03_equal_arrayVsNonArray() {
		assertFalse(equal(new int[]{1}, "not-an-array"));
	}

	//====================================================================================================
	// equal(Object,Object,BiPredicate) / notEqual(Object,Object,BiPredicate) - null permutations
	//====================================================================================================
	@Test
	void b01_equalBiPredicate_firstNullSecondNonNull() {
		assertFalse(equal(null, "x", String::equalsIgnoreCase));
	}

	@Test
	void b02_notEqualBiPredicate_firstNullSecondNonNull() {
		assertTrue(notEqual(null, "x", String::equalsIgnoreCase));
	}

	//====================================================================================================
	// compare(Object,Object) - differing classes / same non-Comparable class
	//====================================================================================================
	@Test
	void c01_compare_differentClasses() {
		assertEquals(0, compare(1, "x"));
	}

	@Test
	void c02_compare_sameClassNotComparable() {
		assertEquals(0, compare(new Object(), new Object()));
	}

	//====================================================================================================
	// lessThan / lessThanOrEqual / greaterThanOrEqual - remaining null permutations
	//====================================================================================================
	@Test
	void d01_lessThan_bothNull() {
		assertFalse(lessThan(null, null));
	}

	@Test
	void e01_lessThanOrEqual_firstNonNullSecondNull() {
		assertFalse(lessThanOrEqual("x", null));
	}

	@Test
	void f01_greaterThanOrEqual_bothNull() {
		assertTrue(greaterThanOrEqual(null, null));
	}

	@Test
	void f02_greaterThanOrEqual_firstNullSecondNonNull() {
		assertFalse(greaterThanOrEqual(null, "x"));
	}

	@Test
	void f03_greaterThanOrEqual_firstNonNullSecondNull() {
		assertTrue(greaterThanOrEqual("x", null));
	}

	//====================================================================================================
	// min / max - second-argument-null branch
	//====================================================================================================
	@Test
	void g01_min_secondNull() {
		assertEquals("a", min("a", null));
	}

	@Test
	void g02_max_secondNull() {
		assertEquals("a", max("a", null));
	}

	//====================================================================================================
	// anyTrue - null-array branch
	//====================================================================================================
	@Test
	void h01_anyTrue_nullArray() {
		assertFalse(anyTrue((boolean[])null));
	}

	//====================================================================================================
	// unwrap - direct (non-recursive) non-wrapper value, and the Holder branch
	//====================================================================================================
	@Test
	void i01_unwrap_plainValue() {
		assertEquals("plain", unwrap("plain"));
	}

	@Test
	void i02_unwrap_holder() {
		assertEquals("x", unwrap(Holder.of("x")));
		assertNull(unwrap(Holder.empty()));
	}

	//====================================================================================================
	// abs - Double/Float/Short/Byte branches, and the generic-Number fallback
	//====================================================================================================
	@Test
	void j01_abs_double() {
		assertEquals(5.5d, abs(-5.5d));
	}

	@Test
	void j02_abs_float() {
		assertEquals(5.5f, abs(-5.5f));
	}

	@Test
	void j03_abs_short() {
		assertEquals((short)5, abs((short)-5));
	}

	@Test
	void j04_abs_byte() {
		assertEquals((byte)5, abs((byte)-5));
	}

	@Test
	void j05_abs_fallbackNumberType_returnsWrongType() {
		// BUG: for Number subtypes other than Integer/Long/Double/Float/
		// Short/Byte, abs() falls through to `return (T)Double.valueOf(value.doubleValue());`,
		// silently producing a Double disguised as T instead of the caller's actual type. When the
		// call site's inferred T requires a narrowing checkcast back to the original type (BigDecimal
		// here), this throws ClassCastException at the call site rather than computing BigDecimal's
		// own abs(). Pinning the CURRENT (buggy) behavior here rather than fixing it.
		var value = BigDecimal.valueOf(-5);
		assertThrows(ClassCastException.class, () -> {
			@SuppressWarnings({
				"java:S1854" // Dead store: the checkcast to BigDecimal, inserted at this assignment, is what's under test.
			})
			BigDecimal result = abs(value);
		});
	}

	//====================================================================================================
	// isEmpty / isNotEmpty - array-length branches
	//====================================================================================================
	@Test
	void k01_isEmpty_nonEmptyArray() {
		assertFalse(isEmpty(new int[]{1, 2}));
	}

	@Test
	void k02_isNotEmpty_emptyArray() {
		assertFalse(isNotEmpty(new int[0]));
	}

	@Test
	void k03_isNotEmpty_nonEmptyArray() {
		assertTrue(isNotEmpty(new int[]{1}));
	}

	@Test
	void k04_isNotEmpty_nonArrayNonCollectionNonMapNonCharSequence() {
		assertTrue(isNotEmpty(42));
	}

	//====================================================================================================
	// protected constructor - documented subclassing contract
	//====================================================================================================

	/** Subclass used solely to exercise the protected no-arg constructor's documented "meant to be subclassed" contract. */
	static class Z01_ObjectUtilsSubclass extends ObjectUtils {}

	@Test
	void z01_protectedConstructor_allowsSubclassing() {
		assertNotNull(new Z01_ObjectUtilsSubclass());
	}
}
