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
package org.apache.juneau.junit5;

import java.util.function.*;

import org.apache.juneau.commons.inject.*;

/**
 * Fluent {@link BasicBeanStore} subclass for declaring test-time bean overrides.
 *
 * <p>
 * A {@code TestBeanStore} is an overlay-builder.  Tests register replacements for production beans
 * via {@link #override} and then plug the resulting store into the SUT &mdash; typically through a
 * builder hook such as {@code MockRestClient.Builder.overridingBeanStore(...)} (the Mode INJECT wiring
 * pattern; see {@link JuneauBeanStoreExtension} for the full contract).  The framework threads the
 * overlay into the {@code overridingParent} slot of the SUT's bean store, putting registered overrides
 * at tier 1 of the resolution chain (above local resource entries and the Spring bridge).
 *
 * <p>
 * <b>No JUnit 5 dependency.</b>  This class is intentionally a plain {@link BasicBeanStore} subclass
 * so that callers from non-JUnit harnesses (TestNG, raw {@code main(...)}, Spring Test, etc.) can
 * use it without dragging in {@code junit-jupiter-api}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Build the overlay.</jc>
 * 	<jk>var</jk> <jv>overlay</jv> = <jk>new</jk> TestBeanStore()
 * 		.override(MyExternalApi.<jk>class</jk>, mockApi)
 * 		.override(CallLogger.<jk>class</jk>, () -&gt; spyLogger);
 *
 * 	<jc>// Wire into the SUT.</jc>
 * 	<jk>try</jk> (<jv>client</jv> = MockRestClient.<jsm>create</jsm>(MyResource.<jk>class</jk>)
 * 			.overridingBeanStore(<jv>overlay</jv>)
 * 			.build()) {
 * 		<jv>client</jv>.get(<js>"/widgets/1"</js>).run().assertStatus().is(200);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore} - The bean store this overlay extends.
 * 	<li class='jc'>{@link StackOverlay} - The stack-aware composition primitive used for class-scope and Mode OVERLAY layering.
 * 	<li class='jc'>{@link JuneauBeanStoreExtension} - Mode INJECT vs Mode OVERLAY contracts and the JUnit 5 lifecycle owner.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Fluent override(...) methods return this (a Closeable BeanStore) owned by the caller/SUT; Eclipse JDT @Owning warning is by design.
})
public class TestBeanStore extends BasicBeanStore {

	/**
	 * Constructor.
	 */
	public TestBeanStore() {
		super();
	}

	/**
	 * Parent-chaining constructor.
	 *
	 * <p>
	 * Lookups consult this store's local entries first and then fall through to the supplied parent.  Used by
	 * {@link JuneauBeanStoreExtension} to stack a per-method overlay on top of a per-class overlay so method-scope
	 * overrides shadow class-scope overrides of the same {@code (type, name)} pair.
	 *
	 * @param parent The parent bean store.  Can be <jk>null</jk> for a standalone overlay.
	 * @since 10.0.0
	 */
	public TestBeanStore(BeanStore parent) {
		super(parent);
	}

	/**
	 * Registers an unnamed bean as a test-time override.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.  Must not be <jk>null</jk>.
	 * @param bean The override instance.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <T> TestBeanStore override(Class<T> type, T bean) {
		addBean(type, bean);
		return this;
	}

	/**
	 * Registers a named bean as a test-time override.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.  Must not be <jk>null</jk>.
	 * @param bean The override instance.  Can be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object.
	 */
	public <T> TestBeanStore override(Class<T> type, T bean, String name) {
		addBean(type, bean, name);
		return this;
	}

	/**
	 * Registers an unnamed supplier-backed bean as a test-time override.
	 *
	 * <p>
	 * The supplier is invoked lazily on first lookup, mirroring {@link BasicBeanStore#addSupplier}.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.  Must not be <jk>null</jk>.
	 * @param supplier The override supplier.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public <T> TestBeanStore override(Class<T> type, Supplier<T> supplier) {
		addSupplier(type, supplier);
		return this;
	}

	/**
	 * Registers a named supplier-backed bean as a test-time override.
	 *
	 * <p>
	 * The supplier is invoked lazily on first lookup, mirroring {@link BasicBeanStore#addSupplier}.
	 *
	 * @param <T> The bean type.
	 * @param type The bean type.  Must not be <jk>null</jk>.
	 * @param supplier The override supplier.  Must not be <jk>null</jk>.
	 * @param name The bean name.  Can be <jk>null</jk> for unnamed beans.
	 * @return This object.
	 */
	public <T> TestBeanStore override(Class<T> type, Supplier<T> supplier, String name) {
		addSupplier(type, supplier, name);
		return this;
	}
}
