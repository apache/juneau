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

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Phase G regression: verifies the field-type autodetect contract for {@code @Value} —
 *
 * <ul>
 * 	<li>Bare {@code String} field: resolves once at bean construction. Subsequent reads
 * 		return the captured value (existing behavior, preserved).
 * 	<li>{@code Supplier<String>} field: injected with a {@link Supplier} that
 * 		re-evaluates the compiled template on every {@link Supplier#get()} call, each
 * 		time against a fresh {@link org.apache.juneau.commons.svl.VarResolverSession}.
 * 	<li>Cross-thread {@code Supplier.get()} is safe to share.
 * 	<li>Literal-template Supplier returns a constant-folding fast path (every
 * 		{@code .get()} returns the same cached string with no SVL work).
 * </ul>
 */
class Value_SupplierFieldType_Test extends TestBase {

	private static final String P_KEY = "Value_SupplierFieldType_Test.key";
	private static final String P_COUNTER = "Value_SupplierFieldType_Test.counter";

	public static class StringBean {
		@Value("${" + P_KEY + ":default}")
		String greeting;
	}

	public static class SupplierBean {
		@Value("${" + P_KEY + ":default}")
		Supplier<String> greeting;
	}

	public static class LiteralSupplierBean {
		@Value("constant")
		Supplier<String> greeting;
	}

	public static class ConstructorSupplierBean {
		final Supplier<String> greeting;

		@Inject
		public ConstructorSupplierBean(@Value("${" + P_KEY + ":default}") Supplier<String> greeting) {
			this.greeting = greeting;
		}
	}

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(P_KEY);
		Settings.get().unsetGlobal(P_COUNTER);
		ValueResolver.clearTemplateCache();
	}

	@Test void a01_bareStringField_resolvesOnce() {
		Settings.get().setGlobal(P_KEY, "initial");
		var bean = BeanInstantiator.of(StringBean.class, new BasicBeanStore(null)).run();
		assertEquals("initial", bean.greeting);

		// Mutating Settings should NOT change the captured String — it's a one-shot read.
		Settings.get().setGlobal(P_KEY, "mutated");
		assertEquals("initial", bean.greeting);
	}

	@Test void a02_supplierField_reEvaluatesPerGet() {
		Settings.get().setGlobal(P_KEY, "first");
		var bean = BeanInstantiator.of(SupplierBean.class, new BasicBeanStore(null)).run();
		assertNotNull(bean.greeting, "Supplier<String> field must be injected (not null)");
		assertEquals("first", bean.greeting.get());

		Settings.get().setGlobal(P_KEY, "second");
		assertEquals("second", bean.greeting.get(), "Supplier<String> re-evaluates per .get()");

		Settings.get().setGlobal(P_KEY, "third");
		assertEquals("third", bean.greeting.get());
	}

	@Test void a03_supplierField_constructorParam() {
		Settings.get().setGlobal(P_KEY, "ctor1");
		var bean = BeanInstantiator.of(ConstructorSupplierBean.class, new BasicBeanStore(null)).run();
		assertEquals("ctor1", bean.greeting.get());

		Settings.get().setGlobal(P_KEY, "ctor2");
		assertEquals("ctor2", bean.greeting.get(), "Constructor-injected Supplier<String> re-evaluates per .get()");
	}

	@Test void a04_literalSupplier_constantFoldsFastPath() {
		var bean = BeanInstantiator.of(LiteralSupplierBean.class, new BasicBeanStore(null)).run();
		var s1 = bean.greeting.get();
		var s2 = bean.greeting.get();
		assertEquals("constant", s1);
		assertEquals("constant", s2);
		// Identity check — the fast-path Supplier captures the literal once and returns the same
		// String reference on every .get() (no per-call session allocation).
		assertSame(s1, s2, "Literal-template Supplier returns the same cached String reference per .get()");
	}

	@Test void a05_supplierField_crossThreadSafety() throws Exception {
		Settings.get().setGlobal(P_KEY, "threadsafe");
		var bean = BeanInstantiator.of(SupplierBean.class, new BasicBeanStore(null)).run();
		var exec = Executors.newFixedThreadPool(8);
		try {
			var results = new ArrayList<Future<String>>();
			for (var i = 0; i < 64; i++)
				results.add(exec.submit(bean.greeting::get));
			for (var f : results)
				assertEquals("threadsafe", f.get(5, TimeUnit.SECONDS));
		} finally {
			exec.shutdownNow();
			assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));
		}
	}

	@Test void a06_supplierField_defaultExpression() {
		// No Settings entry — falls back to the ${key:default} default expression each call.
		var bean = BeanInstantiator.of(SupplierBean.class, new BasicBeanStore(null)).run();
		assertEquals("default", bean.greeting.get());
		assertEquals("default", bean.greeting.get());
	}
}
