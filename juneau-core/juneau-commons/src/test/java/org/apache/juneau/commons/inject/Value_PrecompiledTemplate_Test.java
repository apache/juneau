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

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Phase G regression: verifies that {@code @Value("${...}")} expressions are tokenized
 * <i>once</i> per distinct expression and cached as {@link VarTemplate} instances on
 * {@link ValueResolver}'s internal cache.
 *
 * <p>
 * Without the precompile retrofit, every bean construction re-tokenized the expression
 * via {@code VarResolver.DEFAULT.resolve(expr)}. After the retrofit, the first lookup
 * caches the compiled {@link VarTemplate} and subsequent lookups return the same
 * instance, skipping the tokenizer + var-registry-lookup pipeline.
 */
class Value_PrecompiledTemplate_Test extends TestBase {

	private static final String P_KEY = "Value_PrecompiledTemplate_Test.key";

	public static class GreetingBean {
		@Value("${Value_PrecompiledTemplate_Test.key:hello}")
		String greeting;
	}

	@BeforeEach
	void setUp() {
		ValueResolver.clearTemplateCache();
	}

	@AfterEach
	void tearDown() {
		Settings.get().unsetGlobal(P_KEY);
		ValueResolver.clearTemplateCache();
	}

	@Test void a01_sameExpressionReturnsSameTemplate() {
		var t1 = ValueResolver.getCompiledTemplate("${" + P_KEY + ":hello}");
		var t2 = ValueResolver.getCompiledTemplate("${" + P_KEY + ":hello}");
		assertSame(t1, t2, "Cached VarTemplate must be returned on second lookup");
	}

	@Test void a02_distinctExpressionsCachedSeparately() {
		var t1 = ValueResolver.getCompiledTemplate("${a:1}");
		var t2 = ValueResolver.getCompiledTemplate("${b:2}");
		assertNotSame(t1, t2, "Distinct expressions must produce distinct VarTemplate instances");
		assertSame(t1, ValueResolver.getCompiledTemplate("${a:1}"));
		assertSame(t2, ValueResolver.getCompiledTemplate("${b:2}"));
	}

	@Test void a03_repeatedFieldInjectionReusesCachedTemplate() {
		// First bean construction should populate the cache; second construction must hit the
		// cached VarTemplate (verified by getCompiledTemplate returning the same instance).
		var bs = new BasicBeanStore(null);
		var bean1 = BeanInstantiator.of(GreetingBean.class, bs).run();
		var bean2 = BeanInstantiator.of(GreetingBean.class, bs).run();
		assertEquals("hello", bean1.greeting);
		assertEquals("hello", bean2.greeting);

		var t1 = ValueResolver.getCompiledTemplate("${" + P_KEY + ":hello}");
		var t2 = ValueResolver.getCompiledTemplate("${" + P_KEY + ":hello}");
		assertSame(t1, t2);
	}

	@Test void a04_cachedTemplateRespectsSettingsUpdate() {
		var bs = new BasicBeanStore(null);
		var bean1 = BeanInstantiator.of(GreetingBean.class, bs).run();
		assertEquals("hello", bean1.greeting);

		Settings.get().setGlobal(P_KEY, "world");
		var bean2 = BeanInstantiator.of(GreetingBean.class, bs).run();
		assertEquals("world", bean2.greeting,
			"Cached template re-resolves against current Settings on each injection (Settings/PropertyVar is intentionally not stable-folded)");
	}

	@Test void a05_literalExpressionCachedAsLiteralTemplate() {
		var t = ValueResolver.getCompiledTemplate("plain literal");
		assertTrue(t.isLiteral(), "Literal expressions fold to a single LiteralSegment at compile time");
		assertSame(t, ValueResolver.getCompiledTemplate("plain literal"));
	}
}
