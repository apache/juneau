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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link TypeVariables}: type-variable-implementation discovery from a class hierarchy
 * ({@link TypeVariables#of(Type)}), the {@code get}/{@code containsKey}/{@code size}/{@code isEmpty}
 * accessors, and both {@code resolve(Type)} overloads (instance + static convenience).
 */
@SuppressWarnings({
	"unused" // Fields referenced only via reflection (getDeclaredField)
})
class TypeVariables_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//-----------------------------------------------------------------------------------------------------------------

	static class BeanA<T> {
		public T x;
	}

	static class BeanB extends BeanA<Integer> {}

	interface Container<T> {
		T get();
	}

	static class StringContainer implements Container<String> {
		@Override public String get() { return null; }
	}

	/** Actual type argument to BeanA is a TypeVariable (U), not a resolved Class. */
	static class BeanC<U extends Number> extends BeanA<U> {}

	/** Actual type argument to BeanA is itself a ParameterizedType - neither Class nor TypeVariable. */
	static class BeanD extends BeanA<List<String>> {}

	/**
	 * Actual type argument's first upper bound is a ParameterizedType (fails the Class check, forcing
	 * the bounds loop to continue) and its second upper bound is a plain interface (Class).
	 */
	static class BeanE<U extends Comparable<U> & Serializable> extends BeanA<U> {}

	/** Sole upper bound is a ParameterizedType, so the bounds loop runs to natural completion without ever breaking. */
	static class BeanF<W extends Comparable<W>> extends BeanA<W> {}

	static class Pair<K, V> {}

	static class StringIntPair extends Pair<String, Integer> {}

	static class ListHolder {
		List<String> list;
	}

	/**
	 * Array-typed fields to probe every branch of the GenericArrayType-handling logic in
	 * {@code resolve(Type)}. Per empirical reflection behavior, the JDK only ever produces a
	 * GenericArrayType when its component still contains an unresolved type variable or a
	 * parameterized type - the component is never itself a plain resolved Class.
	 */
	static class GenericArrayHolder<T> {
		T[] tArray;
		T[][] tArray2d;
		List<String>[] listArray;
		List<String>[][] listArray2d;
	}

	static class WildcardFieldHolder {
		List<? extends Number> field;
	}

	private static Type genericTypeOf(Class<?> owner, String field) {
		try {
			return owner.getDeclaredField(field).getGenericType();
		} catch (NoSuchFieldException e) {
			throw new AssertionError(e);
		}
	}

	private static Type wildcardType() {
		var pt = (ParameterizedType)genericTypeOf(WildcardFieldHolder.class, "field");
		return pt.getActualTypeArguments()[0];
	}

	//-----------------------------------------------------------------------------------------------------------------
	// of() / findTypeVarImpls() - class hierarchy + interface hierarchy discovery
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_of_nullType_returnsEmpty() {
		var tv = TypeVariables.of(null);
		assertTrue(tv.isEmpty());
	}

	@Test
	void a02_of_classExtendingParameterizedSuperclass_resolvesTypeArg() {
		var tv = TypeVariables.of(BeanB.class);
		assertEquals(Integer.class, tv.get(BeanA.class, 0));
	}

	@Test
	void a03_of_classImplementingParameterizedInterface_resolvesTypeArg() {
		var tv = TypeVariables.of(StringContainer.class);
		assertEquals(String.class, tv.get(Container.class, 0));
	}

	@Test
	void a04_of_typeArgIsTypeVariableWithClassBound_usesUpperBound() {
		// BeanC<U extends Number> extends BeanA<U> - U isn't a resolved Class, so the code
		// falls back to U's upper bound (Number) per the class-level javadoc's documented limitation.
		var tv = TypeVariables.of(BeanC.class);
		assertEquals(Number.class, tv.get(BeanA.class, 0));
	}

	@Test
	void a05_of_typeArgIsParameterizedType_neitherClassNorTypeVariable_notRecorded() {
		// BeanD extends BeanA<List<String>> - the actual type argument is itself a ParameterizedType,
		// matching neither the Class nor TypeVariable branches, so no implementation is recorded for it.
		var tv = TypeVariables.of(BeanD.class);
		assertTrue(tv.containsKey(BeanA.class));
		assertNull(tv.get(BeanA.class, 0));
	}

	@Test
	void a06_of_typeArgIsTypeVariable_multipleBounds_skipsNonClassBoundsUntilClassFound() {
		// BeanE<U extends Comparable<U> & Serializable> extends BeanA<U> - U's first bound
		// (Comparable<U>) is a ParameterizedType and fails the instanceof Class check, so the bounds
		// loop must continue to the second bound (Serializable, a plain interface Class) to find a match.
		var tv = TypeVariables.of(BeanE.class);
		assertEquals(Serializable.class, tv.get(BeanA.class, 0));
	}

	@Test
	void a07_of_typeArgIsTypeVariable_noClassBoundFound_loopExhaustsWithoutMatch() {
		// BeanF<W extends Comparable<W>> extends BeanA<W> - W's sole bound is a ParameterizedType,
		// so the bounds loop runs to completion (never breaks) and nothing is added for this slot.
		var tv = TypeVariables.of(BeanF.class);
		assertTrue(tv.containsKey(BeanA.class));
		assertNull(tv.get(BeanA.class, 0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// containsKey() / get() / get(clazz,index) / isEmpty() / size()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_containsKey_present_true() {
		assertTrue(TypeVariables.of(BeanB.class).containsKey(BeanA.class));
	}

	@Test
	void b02_containsKey_absent_false() {
		assertFalse(TypeVariables.of(BeanB.class).containsKey(String.class));
	}

	@Test
	void b03_containsKey_null_false() {
		assertFalse(TypeVariables.empty().containsKey(null));
	}

	@Test
	void b04_get_clazz_present_returnsList() {
		assertEquals(List.of(Integer.class), TypeVariables.of(BeanB.class).get(BeanA.class));
	}

	@Test
	void b05_get_clazz_absent_returnsNull() {
		assertNull(TypeVariables.of(BeanB.class).get(String.class));
	}

	@Test
	void b06_get_clazzAndIndex_missingClass_returnsNull() {
		assertNull(TypeVariables.of(BeanB.class).get(String.class, 0));
	}

	@Test
	void b07_get_clazzAndIndex_negativeIndex_returnsNull() {
		assertNull(TypeVariables.of(BeanB.class).get(BeanA.class, -1));
	}

	@Test
	void b08_get_clazzAndIndex_indexTooLarge_returnsNull() {
		assertNull(TypeVariables.of(BeanB.class).get(BeanA.class, 5));
	}

	@Test
	void b09_get_clazzAndIndex_validIndex_returnsValue() {
		assertEquals(Integer.class, TypeVariables.of(BeanB.class).get(BeanA.class, 0));
	}

	@Test
	void b10_isEmpty_emptyInstance_true() {
		assertTrue(TypeVariables.empty().isEmpty());
	}

	@Test
	void b11_isEmpty_nonEmptyInstance_false() {
		assertFalse(TypeVariables.of(BeanB.class).isEmpty());
	}

	@Test
	void b12_size_reflectsMapSize() {
		assertEquals(0, TypeVariables.empty().size());
		assertTrue(TypeVariables.of(BeanB.class).size() >= 1);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Type) instance method
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_resolve_class_returnsItself() {
		assertEquals(String.class, TypeVariables.empty().resolve(String.class));
	}

	@Test
	void c02_resolve_parameterizedType_returnsRawType() {
		assertEquals(List.class, TypeVariables.empty().resolve(genericTypeOf(ListHolder.class, "list")));
	}

	@Test
	void c03_resolve_genericArrayType_parameterizedTypeComponent_returnsArrayClass() {
		assertEquals(List[].class, TypeVariables.empty().resolve(genericTypeOf(GenericArrayHolder.class, "listArray")));
	}

	@Test
	void c04_resolve_genericArrayType_nestedGenericArrayComponent_returnsArrayClass() {
		assertEquals(List[][].class, TypeVariables.empty().resolve(genericTypeOf(GenericArrayHolder.class, "listArray2d")));
	}

	@Test
	void c05_resolve_genericArrayType_typeVariableComponent_returnsNull() {
		// tArray's component is a bare TypeVariable, which matches none of the Class/ParameterizedType/
		// GenericArrayType component checks, so resolution bottoms out at null.
		assertNull(TypeVariables.empty().resolve(genericTypeOf(GenericArrayHolder.class, "tArray")));
	}

	@Test
	void c06_resolve_genericArrayType_nestedArrayResolvingToNull_returnsNull() {
		// tArray2d's component is itself a GenericArrayType (T[]) whose own component is a bare
		// TypeVariable, so the recursive resolve() call returns null, exercising the "resolved == null"
		// side of the ternary rather than building an array class.
		assertNull(TypeVariables.empty().resolve(genericTypeOf(GenericArrayHolder.class, "tArray2d")));
	}

	@Test
	void c07_resolve_typeVariable_implementationFound_returnsResolvedClass() {
		var tv = TypeVariables.of(BeanB.class);
		var typeVar = BeanA.class.getTypeParameters()[0];
		assertEquals(Integer.class, tv.resolve(typeVar));
	}

	@Test
	void c08_resolve_typeVariable_noImplementationRecorded_returnsNull() {
		// BeanA's T is a real type parameter (varIndex found), but this TypeVariables instance
		// has no recorded implementation for BeanA at all.
		assertNull(TypeVariables.empty().resolve(BeanA.class.getTypeParameters()[0]));
	}

	@Test
	void c09_resolve_unrecognizedTypeKind_returnsNull() {
		// WildcardType is neither Class, ParameterizedType, GenericArrayType, nor TypeVariable.
		assertNull(TypeVariables.empty().resolve(wildcardType()));
	}

	@Test
	void c10_resolve_typeVariable_multipleTypeParams_matchesByNameNotPosition() {
		// Pair<K,V> declares two type parameters; resolving V's TypeVariable must skip past K (name
		// mismatch on the first loop iteration) before finding the matching name on the second.
		var tv = TypeVariables.of(StringIntPair.class);
		var vVar = Pair.class.getTypeParameters()[1];
		assertEquals(Integer.class, tv.resolve(vVar));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolve(Type, TypeVariables) static convenience method
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_staticResolve_class_returnsItself() {
		assertEquals(String.class, TypeVariables.resolve(String.class, null));
	}

	@Test
	void d02_staticResolve_parameterizedType_returnsRawType() {
		assertEquals(List.class, TypeVariables.resolve(genericTypeOf(ListHolder.class, "list"), null));
	}

	@Test
	void d03_staticResolve_genericArrayType_parameterizedTypeComponent_returnsArrayClass() {
		assertEquals(List[].class, TypeVariables.resolve(genericTypeOf(GenericArrayHolder.class, "listArray"), null));
	}

	@Test
	void d04_staticResolve_genericArrayType_nestedArrayComponent_returnsArrayClass() {
		assertEquals(List[][].class, TypeVariables.resolve(genericTypeOf(GenericArrayHolder.class, "listArray2d"), null));
	}

	@Test
	void d05_staticResolve_genericArrayType_nestedArrayResolvingToNull_returnsNull() {
		assertNull(TypeVariables.resolve(genericTypeOf(GenericArrayHolder.class, "tArray2d"), TypeVariables.empty()));
	}

	@Test
	void d06_staticResolve_typeVariable_nullTypeVars_returnsNull() {
		assertNull(TypeVariables.resolve(BeanA.class.getTypeParameters()[0], null));
	}

	@Test
	void d07_staticResolve_typeVariable_delegatesToInstanceResolve() {
		var tv = TypeVariables.of(BeanB.class);
		var typeVar = BeanA.class.getTypeParameters()[0];
		assertEquals(Integer.class, TypeVariables.resolve(typeVar, tv));
	}

	@Test
	void d08_staticResolve_unrecognizedTypeKind_returnsNull() {
		assertNull(TypeVariables.resolve(wildcardType(), null));
	}
}
