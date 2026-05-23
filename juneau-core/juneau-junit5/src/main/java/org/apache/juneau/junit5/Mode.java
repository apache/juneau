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

/**
 * Wiring mode for a {@link TestBean @TestBean}-declared override.
 *
 * <p>
 * Selects how the {@link JuneauBeanStoreExtension} threads the overlay into the system under test.  See
 * {@link JuneauBeanStoreExtension} for the full {@link #INJECT} vs {@link #OVERLAY} contracts.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link TestBean} - The annotation that carries this mode selector.
 * 	<li class='jc'>{@link JuneauBeanStoreExtension} - The JUnit 5 extension that honors this mode.
 * </ul>
 *
 * @since 9.5.0
 */
public enum Mode {

	/**
	 * Construction-time wiring &mdash; the default.
	 *
	 * <p>
	 * The bean is wired into the SUT at construction time via the builder's
	 * {@code overridingBeanStore(...)} setter; the SUT doesn't exist until the overlay is applied.  The overlay is
	 * installed in the {@code overridingParent} slot of the SUT's bean store at construction time and is
	 * <i>universal</i> &mdash; every bean type is eligible for replacement, including framework-managed ones that
	 * subsequently pin into per-op memoizers.  Best for fresh-instance test isolation.
	 *
	 * <p>
	 * {@code INJECT} overrides do not require the extension to be
	 * {@linkplain JuneauBeanStoreExtension#attach(org.apache.juneau.commons.inject.WritableBeanStore) attached} to
	 * an existing bean store.
	 */
	INJECT,

	/**
	 * Push/pop wiring on a live SUT.
	 *
	 * <p>
	 * The bean is overlaid onto an already-constructed SUT for the duration of a test scope, then removed via
	 * {@link org.apache.juneau.commons.inject.WritableBeanStore#popOverlay(org.apache.juneau.commons.inject.Snapshot)}.
	 * Requires {@link JuneauBeanStoreExtension#attach(org.apache.juneau.commons.inject.WritableBeanStore)} to point
	 * the extension at the SUT's bean store; per-test overlays are pushed via
	 * {@link org.apache.juneau.commons.inject.WritableBeanStore#pushOverlay(org.apache.juneau.commons.inject.BeanStore)}
	 * at {@code beforeEach} / {@code beforeAll} and popped at {@code afterEach} / {@code afterAll}.  Best when SUT
	 * construction is expensive and tests share a live instance.
	 *
	 * <p>
	 * {@code OVERLAY} overrides only influence beans the framework resolves through the bean store per-call.  Beans
	 * pinned into per-op memoizers at boot are not re-resolved &mdash; for those, use {@link #INJECT} (fresh SUT).
	 */
	OVERLAY
}
