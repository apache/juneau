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

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.inject.Named;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link ParameterInfo} — focuses on:
 * <ul>
 * 	<li>{@link ParameterInfo#canResolve} branches
 * 	<li>{@link ParameterInfo#getMissingType}
 * 	<li>{@link ParameterInfo#resolveValue} for collections, arrays, maps, optionals,
 * 		Provider proxies, and otherBeans fallback
 * 	<li>The {@code resolveValue(BeanStore, Object enclosingInstance, Object... otherBeans)} overload
 * </ul>
 */
@SuppressWarnings({
	"resource", // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
	"unused"    // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class ParameterInfo_Resolution_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Fixtures
	//-----------------------------------------------------------------------------------------------------------------

	public interface MyService { String name(); }

	public static class MyServiceImpl implements MyService {
		private final String name;
		public MyServiceImpl(String name) { this.name = name; }
		@Override public String name() { return name; }
	}

	public static class SingleBeanHolder {
		public SingleBeanHolder(MyService svc) {}
	}

	public static class ListBeanHolder {
		public ListBeanHolder(List<MyService> svcs) {}
	}

	public static class SetBeanHolder {
		public SetBeanHolder(Set<MyService> svcs) {}
	}

	public static class MapBeanHolder {
		public MapBeanHolder(Map<String,MyService> svcs) {}
	}

	public static class ArrayBeanHolder {
		public ArrayBeanHolder(MyService[] svcs) {}
	}

	public static class OptionalBeanHolder {
		public OptionalBeanHolder(Optional<MyService> svc) {}
	}

	public static class OptionalListHolder {
		public OptionalListHolder(Optional<List<MyService>> svcs) {}
	}

	public static class OptionalScalarHolder {
		public OptionalScalarHolder(Optional<String> name) {}
	}

	public static class JuneauProviderHolder {
		public JuneauProviderHolder(Provider<MyService> svc) {}
	}

	public static class NamedHolder {
		public NamedHolder(@Named("foo") MyService svc) {}
	}

	public static class TwoArgHolder {
		// Used to test enclosingInstance: index-0 must match outer type, index-1 must be resolved normally.
		public TwoArgHolder(SingleBeanHolder outer, MyService svc) {}
	}

	private static ParameterInfo paramOf(Class<?> ownerClass, int paramIndex) {
		var ci = ConstructorInfo.of(ownerClass.getDeclaredConstructors()[0]);
		return ci.getParameter(paramIndex);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// canResolve(BeanStore, Object...)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_canResolve_singleBean_present() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"));
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertTrue(pi.canResolve(bs));
	}

	@Test void a02_canResolve_singleBean_absent_falsePath() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertFalse(pi.canResolve(bs));
	}

	@Test void a03_canResolve_otherBeans_match() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertTrue(pi.canResolve(bs, new MyServiceImpl("via-otherBeans")));
	}

	@Test void a04_canResolve_otherBeans_noMatch() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		// otherBeans contains an unrelated type → still false.
		assertFalse(pi.canResolve(bs, "irrelevant"));
	}

	@Test void a05_canResolve_optional_alwaysTrue() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(OptionalBeanHolder.class, 0);
		assertTrue(pi.canResolve(bs));  // even without bean, Optional path returns true
	}

	@Test void a06_canResolve_listCollection_alwaysTrue() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(ListBeanHolder.class, 0);
		assertTrue(pi.canResolve(bs));  // collection types always resolve
	}

	@Test void a07_canResolve_arrayCollection_alwaysTrue() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(ArrayBeanHolder.class, 0);
		assertTrue(pi.canResolve(bs));
	}

	@Test void a08_canResolve_namedQualifier_present() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"), "foo");
		var pi = paramOf(NamedHolder.class, 0);
		assertTrue(pi.canResolve(bs));
	}

	@Test void a09_canResolve_namedQualifier_absent() {
		var bs = new BasicBeanStore(null);
		// Wrong name in the store
		bs.addBean(MyService.class, new MyServiceImpl("a"), "bar");
		var pi = paramOf(NamedHolder.class, 0);
		assertFalse(pi.canResolve(bs));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getMissingType(BeanStore, Object...)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_getMissingType_present_returnsNull() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"));
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertNull(pi.getMissingType(bs));
	}

	@Test void b02_getMissingType_absent_returnsTypeName() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertEquals("MyService", pi.getMissingType(bs));
	}

	@Test void b03_getMissingType_namedAbsent_returnsTypeAtName() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(NamedHolder.class, 0);
		assertEquals("MyService@foo", pi.getMissingType(bs));
	}

	@Test void b04_getMissingType_namedPresent_returnsNull() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"), "foo");
		var pi = paramOf(NamedHolder.class, 0);
		assertNull(pi.getMissingType(bs));
	}

	@Test void b05_getMissingType_optional_returnsNull() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(OptionalBeanHolder.class, 0);
		assertNull(pi.getMissingType(bs));
	}

	@Test void b06_getMissingType_collection_returnsNull() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(ListBeanHolder.class, 0);
		assertNull(pi.getMissingType(bs));
	}

	@Test void b07_getMissingType_otherBeansMatch_returnsNull() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertNull(pi.getMissingType(bs, new MyServiceImpl("a")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveValue(BeanStore, Object...) - single bean / otherBeans
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_resolveValue_singleBean() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertSame(inst, pi.resolveValue(bs));
	}

	@Test void c02_resolveValue_otherBeansFallback() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("via-other");
		var pi = paramOf(SingleBeanHolder.class, 0);
		// Bean store empty → falls back to otherBeans (use varargs array to disambiguate from 3-arg overload)
		assertSame(inst, pi.resolveValue(bs, new Object[]{inst}));
	}

	@Test void c03_resolveValue_missing_throws() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(SingleBeanHolder.class, 0);
		assertThrows(ExecutableException.class, () -> pi.resolveValue(bs));
	}

	@Test void c04_resolveValue_named() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("foo-instance");
		bs.addBean(MyService.class, inst, "foo");
		var pi = paramOf(NamedHolder.class, 0);
		assertSame(inst, pi.resolveValue(bs));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveValue(BeanStore, Object...) - Optional<T>
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_resolveValue_optionalPresent() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		var pi = paramOf(OptionalBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Optional);
		assertSame(inst, ((Optional<?>) r).orElse(null));
	}

	@Test void d02_resolveValue_optionalEmpty() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(OptionalBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Optional);
		assertTrue(((Optional<?>) r).isEmpty());
	}

	@Test void d03_resolveValue_optionalScalar_empty() {
		// Optional<String> with no @Value annotation — Optional<String> on the bean store always wraps.
		var bs = new BasicBeanStore(null);
		var pi = paramOf(OptionalScalarHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Optional);
	}

	@Test void d04_resolveValue_optionalListEmpty() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(OptionalListHolder.class, 0);
		var r = pi.resolveValue(bs);
		// Should be an Optional wrapping the (possibly empty) list.
		assertTrue(r instanceof Optional);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveValue(BeanStore, Object...) - collections / arrays / maps
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_resolveValue_list() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"));
		bs.addBean(MyService.class, new MyServiceImpl("b"), "named");
		var pi = paramOf(ListBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof List);
	}

	@Test void e02_resolveValue_set() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"));
		var pi = paramOf(SetBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Set);
	}

	@Test void e03_resolveValue_map() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"), "alpha");
		bs.addBean(MyService.class, new MyServiceImpl("b"), "beta");
		var pi = paramOf(MapBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Map);
	}

	@Test void e04_resolveValue_array() {
		var bs = new BasicBeanStore(null);
		bs.addBean(MyService.class, new MyServiceImpl("a"));
		var pi = paramOf(ArrayBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof MyService[]);
	}

	@Test void e05_resolveValue_emptyList() {
		// No beans of type → empty list (collection types never throw)
		var bs = new BasicBeanStore(null);
		var pi = paramOf(ListBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof List);
	}

	@Test void e06_resolveValue_emptyArray() {
		var bs = new BasicBeanStore(null);
		var pi = paramOf(ArrayBeanHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof MyService[]);
		assertEquals(0, ((MyService[]) r).length);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveValue(BeanStore, Object...) - Provider<T>
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_resolveValue_juneauProvider() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		var pi = paramOf(JuneauProviderHolder.class, 0);
		var r = pi.resolveValue(bs);
		assertTrue(r instanceof Provider);
		// Calling get() resolves the bean.
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var p = (Provider<MyService>) r;
		assertSame(inst, p.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveValue(BeanStore, Object enclosingInstance, Object... otherBeans)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_resolveValue_withEnclosing_index0Match() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		// SingleBeanHolder's first param is MyService — pass an instance via enclosingInstance.
		var pi = paramOf(SingleBeanHolder.class, 0);
		var enclosing = new MyServiceImpl("encl");
		// Force 3-arg overload to disambiguate from resolveValue(BeanStore, Object...).
		var r = pi.resolveValue(bs, enclosing, new Object[0]);
		// enclosingInstance type matches → returned as-is
		assertSame(enclosing, r);
	}

	@Test void g02_resolveValue_withEnclosing_indexNonZero_delegatesToBeanStore() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		// Index 1 → enclosingInstance branch is skipped.
		var pi = paramOf(TwoArgHolder.class, 1);
		var r = pi.resolveValue(bs, new SingleBeanHolder(inst), new Object[0]);
		assertSame(inst, r);
	}

	@Test void g03_resolveValue_withEnclosing_nullEnclosing() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		var pi = paramOf(SingleBeanHolder.class, 0);
		var r = pi.resolveValue(bs, (Object) null, new Object[0]);
		// enclosingInstance == null → falls through to plain resolveValue
		assertSame(inst, r);
	}

	@Test void g04_resolveValue_withEnclosing_typeMismatch_delegates() {
		var bs = new BasicBeanStore(null);
		var inst = new MyServiceImpl("a");
		bs.addBean(MyService.class, inst);
		var pi = paramOf(SingleBeanHolder.class, 0);
		// Pass a String as enclosingInstance — type mismatch → falls through.
		var r = pi.resolveValue(bs, "not-the-right-type", new Object[0]);
		assertSame(inst, r);
	}
}
