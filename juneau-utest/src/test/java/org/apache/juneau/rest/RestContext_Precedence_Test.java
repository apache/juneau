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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.stats.*;
import org.junit.jupiter.api.*;

/**
 * Validates the 9.5 bean-store precedence model on a {@link RestContext}.
 *
 * <p>
 * Resolution order, top-to-bottom:
 * <ol>
 * 	<li>{@code @RestInject} factory methods on the resource.  For non-framework types these are
 * 		registered as local entries directly; for framework types (e.g. {@link CallLogger},
 * 		{@link ThrownStore}) the per-bean memoizer captures the {@code @RestInject} value, and
 * 		{@code RestContext} promotes the memoizer-backed supplier into a local entry so that
 * 		{@code @RestInject} uniformly wins.
 * 	<li>User-supplied bean store from
 * 		{@code @RestInject WritableBeanStore createBeanStore(...)}, including its
 * 		{@link org.apache.juneau.rest.springboot.SpringBeanStore}-style fallback to a backing
 * 		{@code ApplicationContext}.  Consulted only when no {@code @RestInject} factory method
 * 		exists for the type.
 * 	<li>Memoizer-backed framework defaults (e.g. {@link BasicCallLogger}).  Fire only when neither
 * 		a per-resource {@code @RestInject} method nor a user-supplied bean-store binding exists.
 * </ol>
 *
 * <p>
 * Net effect: <b>{@code @RestInject} factory methods on the resource take precedence over
 * Spring/user-supplied bindings, which in turn take precedence over framework defaults.</b>
 * Spring/user-supplied bindings act as drop-in overrides for any type the resource doesn't
 * customize via {@code @RestInject}, with the framework filling in defaults for anything else.
 */
class RestContext_Precedence_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final CallLogger SPRING_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();
	private static final CallLogger RESTINJECT_LOGGER = BasicCallLogger.create(BasicBeanStore.INSTANCE).build();

	//-----------------------------------------------------------------------------------------------------------------
	// Spring-substitute bean store
	//
	// Simulates SpringBeanStore: holds bindings in an internal "Spring app context" map that's
	// only consulted from getBean(...) AFTER super.getBean(...) returns empty.  This places the
	// "Spring" layer at the BOTTOM of the resolution chain, below local entries and below
	// memoizer-backed default suppliers \u2014 exactly where real Spring sits.
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public static class SpringLikeBeanStore extends BasicBeanStore {

		private final Map<Class<?>, Object> springBindings = new HashMap<>();

		public SpringLikeBeanStore(BeanStore parent) {
			super(parent);
		}

		public <T> SpringLikeBeanStore bindSpring(Class<T> type, T instance) {
			springBindings.put(type, instance);
			return this;
		}

		@Override
		public <T> Optional<T> getBean(Class<T> beanType) {
			return getBean(beanType, null);
		}

		@Override
		public <T> Optional<T> getBean(Class<T> beanType, String name) {
			var o = super.getBean(beanType, name);
			if (o.isPresent())
				return o;
			return springLookup(beanType, name);
		}

		@Override
		public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) {
			return getBeanSupplier(beanType, null);
		}

		@Override
		public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
			var o = super.getBeanSupplier(beanType, name);
			if (o.isPresent())
				return o;
			var bound = springLookup(beanType, name).orElse(null);
			return bound == null ? Optional.empty() : Optional.of(() -> bound);
		}

		private <T> Optional<T> springLookup(Class<T> beanType, String name) {
			// "Spring" bindings are unnamed only — anything else falls through.
			if (name != null && !name.isEmpty())
				return Optional.empty();
			return Optional.ofNullable((T) springBindings.get(beanType));
		}
	}

	private static SpringLikeBeanStore springLikeBeanStore() {
		return new SpringLikeBeanStore(null).bindSpring(CallLogger.class, SPRING_LOGGER);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 1. @RestInject beats the memoizer-backed framework default.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A_RestInjectBeatsDefault {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject public CallLogger callLogger() { return RESTINJECT_LOGGER; }
	}

	@Test
	void a01_restInject_beatsDefault() {
		MockRestClient.buildLax(A_RestInjectBeatsDefault.class);
		assertSame(RESTINJECT_LOGGER, A_RestInjectBeatsDefault.callLoggerCapture);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 2. @RestInject beats Spring (Spring at fallback layer).
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B_RestInjectBeatsSpring {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject public WritableBeanStore createBeanStore() { return springLikeBeanStore(); }
		@RestInject public CallLogger callLogger() { return RESTINJECT_LOGGER; }
	}

	@Test
	void b01_restInject_beatsSpring() {
		MockRestClient.buildLax(B_RestInjectBeatsSpring.class);
		assertSame(RESTINJECT_LOGGER, B_RestInjectBeatsSpring.callLoggerCapture, "@RestInject method should win over Spring fallback");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 3. Spring beats the framework default for framework bean types (when no @RestInject method exists).
	//
	// Without an @RestInject CallLogger method, the framework's memoizer-backed default supplier sits
	// at level 4 of resolve(), below the user-supplied bean store (parent at level 3).  So Spring
	// overrides the framework default.  This is intentional: if the user wired a CallLogger into Spring,
	// they meant it to be used in preference to the auto-configured BasicCallLogger.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C_SpringBeatsDefault {
		@RestInject static CallLogger callLoggerCapture;
		@RestInject public WritableBeanStore createBeanStore() { return springLikeBeanStore(); }
	}

	@Test
	void c01_spring_beatsDefault_forFrameworkBean() {
		MockRestClient.buildLax(C_SpringBeatsDefault.class);
		assertSame(SPRING_LOGGER, C_SpringBeatsDefault.callLoggerCapture, "User-supplied bean store binding should win over framework default when no @RestInject is declared for the type");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 4. Spring fallback fills in for non-framework bean types when nothing else has a binding.
	//-----------------------------------------------------------------------------------------------------------------

	public static class CustomBean {
		final String marker;
		CustomBean(String marker) { this.marker = marker; }
	}

	private static final CustomBean SPRING_CUSTOM = new CustomBean("from-spring");

	private static SpringLikeBeanStore springLikeBeanStoreWithCustomBean() {
		return new SpringLikeBeanStore(null)
			.bindSpring(CallLogger.class, SPRING_LOGGER)
			.bindSpring(CustomBean.class, SPRING_CUSTOM);
	}

	@Rest
	public static class D_SpringFallbackForUserBean {
		@RestInject static CustomBean customBeanCapture;
		@RestInject public WritableBeanStore createBeanStore() { return springLikeBeanStoreWithCustomBean(); }
	}

	@Test
	void d01_spring_fillsInForUserBean() {
		MockRestClient.buildLax(D_SpringFallbackForUserBean.class);
		assertSame(SPRING_CUSTOM, D_SpringFallbackForUserBean.customBeanCapture, "Spring fallback should provide CustomBean since framework has no default for it and there is no @RestInject method");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 5. @RestInject for a non-framework bean type beats Spring fallback.
	//-----------------------------------------------------------------------------------------------------------------

	private static final CustomBean RESTINJECT_CUSTOM = new CustomBean("from-restinject");

	@Rest
	public static class E_RestInjectBeatsSpringForUserBean {
		@RestInject static CustomBean customBeanCapture;
		@RestInject public WritableBeanStore createBeanStore() { return springLikeBeanStoreWithCustomBean(); }
		@RestInject public CustomBean customBean() { return RESTINJECT_CUSTOM; }
	}

	@Test
	void e01_restInject_beatsSpring_forUserBean() {
		MockRestClient.buildLax(E_RestInjectBeatsSpringForUserBean.class);
		assertSame(RESTINJECT_CUSTOM, E_RestInjectBeatsSpringForUserBean.customBeanCapture, "@RestInject should win over Spring fallback for user-defined bean types too");
	}
}
