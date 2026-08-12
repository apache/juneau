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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Additional coverage-focused tests for {@link ClassMeta}, targeting branches not already exercised by
 * {@link ClassMeta_Test} or the broader marshall test suite.
 */
class ClassMetaCoverage_Test extends TestBase {

	MarshallingContext bc = MarshallingContext.DEFAULT;

	//------------------------------------------------------------------------------------------------------------------
	// a0x - getArg(int)/getArgs(): out-of-range index handling on an ARGS ClassMeta.
	//------------------------------------------------------------------------------------------------------------------

	// getArgsClassMeta() is a protected MarshallingSession method; same-package access from this test class.
	MarshallingSession bs = bc.getSession();

	@Test void a01_getArg_negativeIndex_throws() {
		var args = bs.getArgsClassMeta(new Type[]{String.class, int.class});
		var e = assertThrows(BeanRuntimeException.class, () -> args.getArg(-1));
		assertTrue(e.getMessage().contains("Invalid argument index"));
	}

	@Test void a02_getArg_tooLargeIndex_throws() {
		var args = bs.getArgsClassMeta(new Type[]{String.class, int.class});
		assertThrows(BeanRuntimeException.class, () -> args.getArg(2));
	}

	@Test void a03_getArg_validIndex_returnsArgType() {
		var args = bs.getArgsClassMeta(new Type[]{String.class, int.class});
		assertEquals(String.class, args.getArg(0).inner());
		assertEquals(int.class, args.getArg(1).inner());
		assertEquals(2, args.getArgs().size());
	}

	@Test void a04_getArgs_nonArgsClassMeta_returnsNull() {
		assertNull(bc.getClassMeta(String.class).getArgs());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - Categories.same() via ClassMeta.same(): reached only for primitives with matching/non-matching category
	// bitsets (both int and long map to the NUMBER-only category; boolean maps to no numeric category at all).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_same_primitivesWithMatchingCategoryBits() {
		assertTrue(bc.getClassMeta(int.class).same(bc.getClassMeta(long.class)));
	}

	@Test void b02_same_primitivesWithDifferentCategoryBits() {
		assertFalse(bc.getClassMeta(int.class).same(bc.getClassMeta(boolean.class)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - findParameters(): a BeanMap subclass is categorized as MAP but its key/value types are always null
	// (findKeyValueTypes()'s BEANMAP short-circuit), exercising getParameters()'s kt==null||vt==null guard.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getParameters_beanMap_returnsEmptyArray() {
		// BeanMap itself is categorized as MAP+BEANMAP; findKeyValueTypes() short-circuits to (null,null)
		// for BEANMAP, exercising getParameters()'s kt==null||vt==null guard.
		var t = bc.getClassMeta(BeanMap.class);
		assertTrue(t.isMap());
		assertNull(t.getKeyType());
		assertNull(t.getValueType());
		assertEquals(0, t.getParameters().length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - hasInputStreamMutater()/hasReaderMutater()/hasMutaterTo(): remaining exclusion branches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_hasInputStreamMutater_excludesString() {
		assertFalse(bc.getClassMeta(String.class).hasInputStreamMutater());
	}

	@Test void d02_hasReaderMutater_excludesString() {
		assertFalse(bc.getClassMeta(String.class).hasReaderMutater());
	}

	@Test void d03_hasMutaterTo_excludesMapTarget() {
		assertFalse(bc.getClassMeta(Object.class).hasMutaterTo(Map.class));
	}

	@Test void d04_hasMutaterTo_excludesCollectionTarget() {
		assertFalse(bc.getClassMeta(Object.class).hasMutaterTo(List.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - getExample(): collection/array/map recursive-example synthesis, both with and without a no-arg-
	// constructible target (canCreateNewInstance() true vs. false).
	//------------------------------------------------------------------------------------------------------------------

	private final JsonParserSession jpSession = JsonParser.DEFAULT.getSession();

	public ArrayList<Integer> fList;
	public Map<Integer,Integer> fMap;

	@Test void e01_getExample_collection_constructibleTarget_wrapsInNewInstance() throws Exception {
		// example(Class<T>,T) - Integer avoids the String/(Class,String)-manifest overload ambiguity.
		// ArrayList (concrete, no-arg constructible) drives canCreateNewInstance()==true; a raw List
		// interface reference would instead be non-constructible and hit the singleton fallback.
		var s = MarshallingContext.create().example(Integer.class, 42).build().getSession();
		var t = s.getClassMeta(getClass().getField("fList").getGenericType());
		// Use a wildcard-typed ClassMeta<?> reference so the compiler doesn't insert an implicit checkcast
		// to the (unrelated at runtime) generic return type at the call site.
		ClassMeta<?> wt = t;
		var result = (List<?>)wt.getExample(s, jpSession);
		assertNotNull(result);
		assertEquals(42, result.get(0));
	}

	// A Collection subtype with no no-arg constructor (abstract, no impl class registered), forcing
	// getExample()'s canCreateNewInstance()==false branch (Collections.singleton(...) fallback).
	public abstract static class E02_NoCtorCollection extends AbstractCollection<Integer> {}

	@Test void e02_getExample_collection_nonConstructibleTarget_usesSingleton() {
		var s = MarshallingContext.create().example(Integer.class, 42).build().getSession();
		ClassMeta<?> t = s.getClassMeta(E02_NoCtorCollection.class);
		assertFalse(t.canCreateNewInstance());
		var result = t.getExample(s, jpSession);
		assertNotNull(result);
		assertEquals(42, ((Collection<?>)result).iterator().next());
	}

	@Test void e03_getExample_array_wrapsElementExample() {
		var s = MarshallingContext.create().example(Integer.class, 42).build().getSession();
		var t = s.getClassMeta(Integer[].class);
		var result = t.getExample(s, jpSession);
		assertNotNull(result);
		assertEquals(1, result.length);
		assertEquals(42, result[0]);
	}

	@Test void e04_getExample_map_constructibleTarget_wrapsInNewInstance() throws Exception {
		// A properly-parameterized Map<Integer,Integer> field guarantees key/value types resolve to
		// Integer (a raw HashMap.class reference would instead default both to Object).
		var s = MarshallingContext.create().example(Integer.class, 42).build().getSession();
		var t = s.getClassMeta(getClass().getField("fMap").getGenericType());
		ClassMeta<?> wt = t;
		var result = (Map<?,?>)wt.getExample(s, jpSession);
		assertNotNull(result);
		assertEquals(42, result.get(42));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x - canCreateNewBean(Object)/canCreateNewInstance(Object)/canCreateNewInstanceFromString(Object):
	// non-static member class outer-object matching.
	//------------------------------------------------------------------------------------------------------------------

	public class F01_Inner {
		public F01_Inner() {}
	}

	@Test void f01_canCreateNewBean_memberClass_wrongOuterType_returnsFalse() {
		var t = bc.getClassMeta(F01_Inner.class);
		assertFalse(t.canCreateNewBean("not-the-right-outer-type"));
	}

	@Test void f02_canCreateNewBean_memberClass_nullOuter_returnsFalse() {
		var t = bc.getClassMeta(F01_Inner.class);
		assertFalse(t.canCreateNewBean(null));
	}

	@Test void f03_canCreateNewInstance_memberClass_returnsFalse() {
		// canCreateNewInstance() (no outer arg) always returns false for non-static member classes.
		var t = bc.getClassMeta(F01_Inner.class);
		assertFalse(t.canCreateNewInstance());
	}

	@Test void f04_canCreateNewInstanceWithOuter_memberClass_nullOuter_returnsFalse() {
		var t = bc.getClassMeta(F01_Inner.class);
		assertFalse(t.canCreateNewInstance(null));
	}

	public class F05_InnerWithStringCtor {
		@SuppressWarnings({
			"unused" // Parameter's presence in the constructor signature (not its value) is what canCreateNewInstanceFromString() probes for.
		})
		public F05_InnerWithStringCtor(String s) {}
	}

	@Test void f05_canCreateNewInstanceFromString_memberClass_nullOuter_returnsFalse() {
		var t = bc.getClassMeta(F05_InnerWithStringCtor.class);
		assertFalse(t.canCreateNewInstanceFromString(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g0x - Simple pure boolean predicates not directly exercised elsewhere.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_isMethod() throws Exception {
		var m = Object.class.getMethod("toString");
		assertTrue(bc.getClassMeta(m.getClass()).isMethod());
		assertFalse(bc.getClassMeta(String.class).isMethod());
	}

	@Test void g02_isNullable_primitiveChar_isTrue_otherPrimitives_areFalse() {
		assertTrue(bc.getClassMeta(char.class).isNullable());
		assertFalse(bc.getClassMeta(int.class).isNullable());
		assertTrue(bc.getClassMeta(String.class).isNullable());
	}

	@Test void g03_isSet() {
		assertTrue(bc.getClassMeta(HashSet.class).isSet());
		assertFalse(bc.getClassMeta(ArrayList.class).isSet());
	}

	@Test void g04_isStream() {
		assertTrue(bc.getClassMeta(java.util.stream.Stream.class).isStream());
		assertFalse(bc.getClassMeta(ArrayList.class).isStream());
	}

	//------------------------------------------------------------------------------------------------------------------
	// h0x - toString(Object): null-argument short-circuit.
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_toStringOfObject_nullArg_returnsNull() {
		assertNull(bc.getClassMeta(String.class).toString((Object)null));
	}

	@Test void h02_toStringOfObject_nonNullArg_delegatesToObjectToString() {
		assertEquals("foo", bc.getClassMeta(String.class).toString((Object)"foo"));
	}
}
