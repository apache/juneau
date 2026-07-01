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
package org.apache.juneau.test.junit;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Marks a field or method on a JUnit 5 test class as a test-time bean override.
 *
 * <p>
 * {@code @TestBean} is the Juneau analog of Spring's {@code @MockBean} / {@code @TestConfiguration}, scoped to a
 * {@code BeanStore} overlay rather than a Spring application context.  The {@link JuneauBeanStoreExtension} discovers
 * {@code @TestBean}-annotated members on the test class, builds a {@link TestBeanStore} overlay containing the
 * declared overrides, and exposes it via {@link JuneauBeanStoreExtension#getStore()} so the test author can wire
 * it into the SUT explicitly (typically via a builder hook such as
 * {@code MockRestClient.Builder.overridingBeanStore(...)}).
 *
 * <p>
 * The wiring is intentionally <i>explicit</i>: the extension does not auto-discover {@code RestContext} /
 * {@code Microservice} fields on the test class.  Test authors write one line of plumbing
 * ({@code .overridingBeanStore(ext.getStore())}) and get full control.
 *
 * <p>
 * The annotation supports two wiring modes via {@link #mode()}: <b>Mode INJECT</b> (default, fresh-instance with
 * overrides via the SUT's builder) and <b>Mode OVERLAY</b> (existing-instance push/pop against a long-lived
 * SUT's bean store).  See {@link JuneauBeanStoreExtension} for the full Mode INJECT / Mode OVERLAY contracts.
 *
 * <h5 class='section'>Applies to:</h5>
 * <ul>
 * 	<li><b>Fields</b> &mdash; the field's current value (read via reflection) is registered as the override.
 * 	<li><b>Methods</b> &mdash; a parameterless factory method whose return type is type-compatible with the
 * 		override target.  Invoked once per scope (per-method for {@link Scope#METHOD}, per-class for
 * 		{@link Scope#CLASS}).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@ExtendWith</ja>(JuneauBeanStoreExtension.<jk>class</jk>)
 * 	<jk>class</jk> MyResourceTest {
 *
 * 		<ja>@TestBean</ja>
 * 		MyExternalApi <jv>mockApi</jv> = Mockito.<jsm>mock</jsm>(MyExternalApi.<jk>class</jk>);
 *
 * 		<ja>@TestBean</ja>(name=<js>"primary"</js>)
 * 		<jk>static</jk> MyService primaryService() { <jk>return new</jk> InMemoryMyService(<js>"p"</js>); }
 *
 * 		<ja>@Test</ja>
 * 		<jk>void</jk> aTest(TestBeanStore <jv>store</jv>) {
 * 			<jk>var</jk> <jv>client</jv> = MockRestClient.<jsm>create</jsm>(MyResource.<jk>class</jk>)
 * 				.overridingBeanStore(<jv>store</jv>)
 * 				.build();
 * 			<jv>client</jv>.get(<js>"/widgets/1"</js>).run().assertStatus().is(200);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JuneauBeanStoreExtension} - The JUnit 5 extension that discovers and applies these annotations.
 * 	<li class='jc'>{@link TestBeanStore} - The overlay builder the extension populates.
 * 	<li class='je'>{@link Scope} - Per-method vs per-class lifecycle selector.
 * </ul>
 *
 * @since 10.0.0
 */
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface TestBean {

	/**
	 * Qualifier name for the override.
	 *
	 * <p>
	 * Empty string (the default) means the override is registered as the <i>unnamed</i> bean of its type, matching
	 * the behavior of {@link TestBeanStore#override(Class, Object)} without a name argument.
	 *
	 * <p>
	 * When non-empty, the override is registered under this qualifier so the framework's
	 * {@code @Bean(name = "...")} parameter resolution can find it.  Two {@code @TestBean}-declared overrides of
	 * the same type but different names are both picked up &mdash; the qualifier is the disambiguator.
	 *
	 * @return The qualifier name.  Empty string means "unnamed".
	 * @since 10.0.0
	 */
	String name() default "";

	/**
	 * Explicit override target type.
	 *
	 * <p>
	 * {@code Object.class} (the default) is a sentinel meaning <i>"use the annotated member's declared type"</i>
	 * &mdash; the field's declared type for field overrides, the method's return type for method overrides.  This
	 * is the common case.
	 *
	 * <p>
	 * Set this explicitly when the member's declared type is a supertype of the intended registration type, for
	 * example an {@code Object} field carrying a {@code MyService} value, or a {@code @TestBean Supplier<MyService>}
	 * factory method whose semantic type is {@code MyService}.
	 *
	 * @return The override target type.  {@code Object.class} means "use the member's declared type".
	 * @since 10.0.0
	 */
	Class<?> type() default Object.class;

	/**
	 * Lifecycle scope of the override.
	 *
	 * <p>
	 * Defaults to {@link Scope#METHOD} &mdash; the override is rebuilt for every test method.  Use
	 * {@link Scope#CLASS} to share the override across all test methods in the class.
	 *
	 * <p>
	 * {@code CLASS}-scope overrides may only be declared on {@code static} fields and {@code static} methods.
	 * Instance members are not visible at {@code beforeAll} time and will be rejected with an error.
	 *
	 * @return The lifecycle scope.
	 * @since 10.0.0
	 */
	Scope scope() default Scope.METHOD;

	/**
	 * Wiring mode for the override.
	 *
	 * <p>
	 * Defaults to {@link Mode#INJECT} &mdash; the override is installed at SUT construction time via the SUT's
	 * {@code overridingBeanStore(...)} builder hook.  Use {@link Mode#OVERLAY} for push/pop overlays against a
	 * long-lived SUT that the test has
	 * {@linkplain JuneauBeanStoreExtension#attach(org.apache.juneau.commons.inject.WritableBeanStore) attach}-ed
	 * to the extension.
	 *
	 * <p>
	 * Mixing {@code Mode.INJECT} and {@code Mode.OVERLAY} annotations on the same scope is rejected at scope-build
	 * time with a clear {@link IllegalStateException}.
	 *
	 * @return The wiring mode.
	 * @since 10.0.0
	 */
	Mode mode() default Mode.INJECT;
}
