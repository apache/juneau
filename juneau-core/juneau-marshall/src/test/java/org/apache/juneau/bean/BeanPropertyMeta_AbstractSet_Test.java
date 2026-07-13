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
package org.apache.juneau.bean;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

/**
 * Tests for materialization of abstract {@link Collection} bean properties that have no setter and no
 * {@code @BeanProp(type=...)} override.
 *
 * <p>
 * Such fields are populated from a JSON array source by
 * {@code BeanPropertyMeta.setPropertyValue(...)}, which instantiates a concrete collection via
 * {@code createDefaultCollectionForAbstractType(...)}.  These tests pin the shape-based default table
 * ({@code Set}&rarr;{@code LinkedHashSet}, {@code SortedSet}&rarr;{@code TreeSet},
 * {@code List}/raw {@code Collection}&rarr;{@code ArrayList}, {@code Deque}/{@code Queue}&rarr;{@code ArrayDeque}),
 * the still-honored {@code @BeanProp(type=...)} override, and the best-effort-materialize-then-fallback
 * behavior for custom abstract-set subclasses.
 */
class BeanPropertyMeta_AbstractSet_Test extends TestBase {

	public enum Tag { URGENT, PRIORITY, LOW }

	//------------------------------------------------------------------------------------------------------------------
	// a01: abstract Set<EnumType> field, no setter, no @BeanProp -> LinkedHashSet (order-preserving), elements coerced.
	//------------------------------------------------------------------------------------------------------------------

	public static class A01_Bean { public Set<Tag> tags; }

	@Test void a01_setOfEnum_materializesLinkedHashSet() {
		var a = json("{tags:['URGENT','PRIORITY']}", A01_Bean.class);
		assertInstanceOf(LinkedHashSet.class, a.tags);
		assertEquals(List.of(Tag.URGENT, Tag.PRIORITY), new ArrayList<>(a.tags));  // Insertion order preserved.
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02: abstract SortedSet<EnumType> field -> TreeSet (natural ordering of the enum).
	//------------------------------------------------------------------------------------------------------------------

	public static class A02_Bean { public SortedSet<Tag> tags; }

	@Test void a02_sortedSetOfEnum_materializesTreeSet() {
		var a = json("{tags:['PRIORITY','URGENT','LOW']}", A02_Bean.class);
		assertInstanceOf(TreeSet.class, a.tags);
		// TreeSet orders by enum ordinal: URGENT(0), PRIORITY(1), LOW(2).
		assertEquals(List.of(Tag.URGENT, Tag.PRIORITY, Tag.LOW), new ArrayList<>(a.tags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a03: abstract List<EnumType> field, no setter -> ArrayList.
	//------------------------------------------------------------------------------------------------------------------

	public static class A03_Bean { public List<Tag> tags; }

	@Test void a03_listOfEnum_materializesArrayList() {
		var a = json("{tags:['LOW','URGENT']}", A03_Bean.class);
		assertInstanceOf(ArrayList.class, a.tags);
		assertEquals(List.of(Tag.LOW, Tag.URGENT), a.tags);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a04: abstract Deque<String> field -> ArrayDeque.
	//------------------------------------------------------------------------------------------------------------------

	public static class A04_Bean { public Deque<String> items; }

	@Test void a04_dequeOfString_materializesArrayDeque() {
		var a = json("{items:['x','y']}", A04_Bean.class);
		assertInstanceOf(ArrayDeque.class, a.items);
		assertEquals(List.of("x", "y"), new ArrayList<>(a.items));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a05: @BeanProp(type=TreeSet.class) override still honored (wins over the LinkedHashSet shape default for Set).
	//------------------------------------------------------------------------------------------------------------------

	public static class A05_Bean {
		@BeanProp(type=TreeSet.class, params={Tag.class})
		public Set<Tag> tags;
	}

	@Test void a05_beanPropTypeOverride_honored() {
		var a = json("{tags:['PRIORITY','URGENT']}", A05_Bean.class);
		assertInstanceOf(TreeSet.class, a.tags);  // Override wins; not the LinkedHashSet default.
		// params={Tag.class} carries the element type, so elements are coerced and TreeSet orders by enum ordinal.
		assertEquals(List.of(Tag.URGENT, Tag.PRIORITY), new ArrayList<>(a.tags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a06: OQA-4 - a custom *instantiable* AbstractSet subclass (public no-arg ctor) is materialized as ITSELF.
	//------------------------------------------------------------------------------------------------------------------

	public static class A06_InstantiableSet<E> extends AbstractSet<E> {
		private final Set<E> delegate = new LinkedHashSet<>();
		@Override public boolean add(E e) { return delegate.add(e); }
		@Override public Iterator<E> iterator() { return delegate.iterator(); }
		@Override public int size() { return delegate.size(); }
	}

	public static class A06_Bean { public A06_InstantiableSet<Tag> tags; }

	@Test void a06_instantiableCustomAbstractSet_materializedAsItself() {
		var a = json("{tags:['URGENT','LOW']}", A06_Bean.class);
		assertInstanceOf(A06_InstantiableSet.class, a.tags);
		assertEquals(List.of(Tag.URGENT, Tag.LOW), new ArrayList<>(a.tags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a07: OQA-4 - a genuinely *non-instantiable* abstract set field (AbstractSet) falls back to
	//      LinkedHashSet (the Set shape default; LinkedHashSet is-a AbstractSet, so the field assignment succeeds).
	//------------------------------------------------------------------------------------------------------------------

	public static class A07_Bean { public AbstractSet<Tag> tags; }

	@Test void a07_nonInstantiableAbstractSet_fallsBackToLinkedHashSet() {
		var a = json("{tags:['URGENT','PRIORITY']}", A07_Bean.class);
		assertInstanceOf(LinkedHashSet.class, a.tags);
		assertEquals(List.of(Tag.URGENT, Tag.PRIORITY), new ArrayList<>(a.tags));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a08: abstract Queue<String> field -> ArrayDeque (Queue shape default).
	//------------------------------------------------------------------------------------------------------------------

	public static class A08_Bean { public Queue<String> items; }

	@Test void a08_queueOfString_materializesArrayDeque() {
		var a = json("{items:['a','b']}", A08_Bean.class);
		assertInstanceOf(ArrayDeque.class, a.items);
		assertEquals(List.of("a", "b"), new ArrayList<>(a.items));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a09: raw abstract Collection field -> ArrayList (raw Collection default).
	//------------------------------------------------------------------------------------------------------------------

	public static class A09_Bean { public Collection<String> items; }

	@Test void a09_rawCollection_materializesArrayList() {
		var a = json("{items:['p','q']}", A09_Bean.class);
		assertInstanceOf(ArrayList.class, a.items);
		assertEquals(List.of("p", "q"), new ArrayList<>(a.items));
	}
}
