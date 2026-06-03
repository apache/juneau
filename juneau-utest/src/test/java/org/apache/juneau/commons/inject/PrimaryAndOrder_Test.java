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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@code @Primary} and {@code @Order} / {@code @Bean#priority()} ordering.
 *
 * <p>Asserts:
 * <ul>
 * 	<li>A single {@code @Primary} bean is returned by unqualified {@link BeanStore#getBean(Class)}.
 * 	<li>Multiple {@code @Primary} candidates throw {@link BeanCreationException}.
 * 	<li>{@link BeanStore#getBeansOfType(Class)} returns entries sorted by {@code @Order} value (lower first).
 * 	<li>When both {@code @Order} and {@code priority} are present, {@code @Order} wins.
 * 	<li>{@code @Bean#priority()} provides ordering in the absence of {@code @Order}.
 * </ul>
 */
@SuppressWarnings({"java:S2094"})
class PrimaryAndOrder_Test extends TestBase {

	//------------------------------------------------------------------------------------------------
	// Fixtures.
	//------------------------------------------------------------------------------------------------

	public static class Plugin { public final String tag; public Plugin(String tag) { this.tag = tag; } }

	@Configuration
	public static class SinglePrimaryConfig {
		public SinglePrimaryConfig() { /* intentionally empty */ }
		@Bean(name = "a") public Plugin a() { return new Plugin("a"); }
		@Bean(name = "b") @Primary public Plugin b() { return new Plugin("b"); }
		@Bean(name = "c") public Plugin c() { return new Plugin("c"); }
	}

	@Configuration
	public static class MultiplePrimaryConfig {
		public MultiplePrimaryConfig() { /* intentionally empty */ }
		@Bean(name = "a") @Primary public Plugin a() { return new Plugin("a"); }
		@Bean(name = "b") @Primary public Plugin b() { return new Plugin("b"); }
	}

	@Configuration
	public static class OrderedConfig {
		public OrderedConfig() { /* intentionally empty */ }
		@Bean(name = "low") @Order(10) public Plugin low() { return new Plugin("low"); }
		@Bean(name = "high") @Order(1) public Plugin high() { return new Plugin("high"); }
		@Bean(name = "mid") @Order(5) public Plugin mid() { return new Plugin("mid"); }
	}

	@Configuration
	public static class PriorityConfig {
		public PriorityConfig() { /* intentionally empty */ }
		@Bean(name = "alpha", priority = 100) public Plugin alpha() { return new Plugin("alpha"); }
		@Bean(name = "beta", priority = 50) public Plugin beta() { return new Plugin("beta"); }
		@Bean(name = "gamma", priority = 75) public Plugin gamma() { return new Plugin("gamma"); }
	}

	@Configuration
	public static class OrderWinsOverPriorityConfig {
		public OrderWinsOverPriorityConfig() { /* intentionally empty */ }
		@Bean(name = "x", priority = 1) @Order(100) public Plugin x() { return new Plugin("x"); }
		@Bean(name = "y", priority = 100) @Order(1) public Plugin y() { return new Plugin("y"); }
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @Primary.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_singlePrimary_isReturnedByUnnamedLookup() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(SinglePrimaryConfig.class);
		var p = store.getBean(Plugin.class);
		assertTrue(p.isPresent());
		assertEquals("b", p.get().tag);
	}

	@Test
	void a02_multiplePrimary_throws() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(MultiplePrimaryConfig.class);
		assertThrows(BeanCreationException.class, () -> store.getBean(Plugin.class));
	}

	@Test
	void a03_noPrimary_unnamedLookupReturnsEmpty() {
		// Only named beans, none primary.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OrderedConfig.class);
		assertFalse(store.getBean(Plugin.class).isPresent());
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @Order ordering.
	//------------------------------------------------------------------------------------------------

	@Test
	void b01_order_sortsAscending() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OrderedConfig.class);
		var beans = store.getBeansOfType(Plugin.class);
		assertEquals(3, beans.size());
		var keys = beans.keySet().toArray(new String[0]);
		assertEquals("high", keys[0]);
		assertEquals("mid", keys[1]);
		assertEquals("low", keys[2]);
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @Bean.priority() ordering.
	//------------------------------------------------------------------------------------------------

	@Test
	void c01_priority_sortsAscending() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(PriorityConfig.class);
		var beans = store.getBeansOfType(Plugin.class);
		var keys = beans.keySet().toArray(new String[0]);
		assertEquals("beta", keys[0]);
		assertEquals("gamma", keys[1]);
		assertEquals("alpha", keys[2]);
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @Order precedence over priority().
	//------------------------------------------------------------------------------------------------

	@Test
	void d01_order_winsOverPriority() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OrderWinsOverPriorityConfig.class);
		var beans = store.getBeansOfType(Plugin.class);
		var keys = beans.keySet().toArray(new String[0]);
		assertEquals("y", keys[0]);
		assertEquals("x", keys[1]);
	}
}
