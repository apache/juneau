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

/**
 * Marker interface for builders that accept an <i>overriding</i> {@link BeanStore} &mdash; a layer
 * whose entries take precedence over the builder's local bean registrations during construction-time
 * reflective injection.
 *
 * <p>
 * Implementing builders MUST install the supplied store in the {@code overridingParent} slot of the
 * underlying {@link BasicBeanStore} (see
 * {@link BasicBeanStore#BasicBeanStore(BeanStore, BeanStore)}).  The resulting bean-lookup order is:
 * <ol>
 * 	<li><i>Overriding parent</i> (the store passed to {@link #overridingBeanStore(BeanStore)}) &mdash; wins
 * 		for any {@code (type, name)} it knows about.
 * 	<li>The builder's local entries (e.g. {@code @Bean} factories declared on a {@code @Rest} resource,
 * 		beans registered directly via {@code addBean(...)} / {@code addSupplier(...)}).
 * 	<li>The builder's regular parent bean store (e.g. a Spring {@code ApplicationContext} bridge), if any.
 * 	<li>Framework-default suppliers (registered via {@code addDefaultSupplier(...)}).
 * </ol>
 *
 * <p>
 * The primary use case is test-time bean substitution: a test builds a {@code TestBeanStore}-style overlay
 * with mock beans, hands it to the builder via {@link #overridingBeanStore(BeanStore)}, and the system under
 * test resolves the mocks at construction time without any reflection on the SUT itself.  This is the
 * <b>Mode INJECT</b> wiring pattern in the test-injection model &mdash; the overlay is installed before the SUT
 * exists.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>overlay</jv> = <jk>new</jk> TestBeanStore().override(MyService.<jk>class</jk>, <jv>mockSvc</jv>);
 * 	<jk>var</jk> <jv>serializers</jv> = SerializerSet.<jsm>create</jsm>()
 * 		.overridingBeanStore(<jv>overlay</jv>)
 * 		.add(JsonSerializer.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Why a self-typed interface:</h5>
 * <p>
 * The {@code B extends BeanStoreOverridable<B>} self-type lets the method return the concrete builder
 * type rather than {@code BeanStoreOverridable<?>}, preserving the fluent chain at the call site:
 *
 * <p class='bjava'>
 * 	SerializerSet.Builder <jv>builder</jv> = SerializerSet.<jsm>create</jsm>()
 * 		.overridingBeanStore(<jv>overlay</jv>)   <jc>// still SerializerSet.Builder, not BeanStoreOverridable&lt;?&gt;</jc>
 * 		.add(JsonSerializer.<jk>class</jk>);     <jc>// fluent chain preserved</jc>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore} - The base lookup interface this layer extends.
 * 	<li class='jc'>{@link BasicBeanStore} - The default writable implementation with the {@code overridingParent} slot.
 * </ul>
 *
 * @param <B> The implementing builder's self type.
 * @since 10.0.0
 */
public interface BeanStoreOverridable<B extends BeanStoreOverridable<B>> {

	/**
	 * Installs the supplied {@link BeanStore} as the {@code overridingParent} of the underlying bean store
	 * used by this builder during construction-time reflective injection.
	 *
	 * <p>
	 * Passing {@code null} clears any previously-installed overlay.
	 *
	 * @param store The override layer.  Can be <jk>null</jk> to clear a previously-set value.
	 * @return This object.
	 * @since 10.0.0
	 */
	B overridingBeanStore(BeanStore store);
}
