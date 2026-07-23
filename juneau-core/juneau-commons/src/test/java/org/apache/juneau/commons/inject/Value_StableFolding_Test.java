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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Phase G regression: verifies that stable-folded {@link Var}s inside
 * {@code @Value} expressions fold to a literal at compile time, so subsequent bean
 * constructions read the cached literal text with no runtime dispatch.
 *
 * <p>
 * The 4 documented stable built-ins are {@code EnvVariablesVar} / {@code SystemPropertiesVar} /
 * {@code ManifestFileVar} / {@code ArgsVar}. This test exercises {@code SystemPropertiesVar}
 * (via {@code $S{...}}) because it's the easiest to control deterministically in a test —
 * env vars and manifest files are environment-dependent.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class Value_StableFolding_Test extends TestBase {

	public static class SysPropBean {
		@Value("$S{Value_StableFolding_Test.sysprop}")
		String value;
	}

	@AfterEach
	void cleanup() {
		System.clearProperty("Value_StableFolding_Test.sysprop");
		ValueResolver.clearTemplateCache();
	}

	@Test void a01_systemPropertyFoldsAtCompileTime() {
		System.setProperty("Value_StableFolding_Test.sysprop", "fred");
		// Compile through ValueResolver's cache so the field-injection path is exercised.
		var t = ValueResolver.getCompiledTemplate("$S{Value_StableFolding_Test.sysprop}");
		assertTrue(t.isLiteral(),
			"$S{...} with SystemPropertiesVar opts-in to stable folding — template should be literal after compile");

		// Mutate the property AFTER compile — folded value is frozen.
		System.setProperty("Value_StableFolding_Test.sysprop", "barney");
		var bean = BeanInstantiator.of(SysPropBean.class, new BasicBeanStore(null)).run();
		assertEquals("fred", bean.value,
			"Folded value is captured at compile time and ignores later System.setProperty calls (documented caveat)");
	}

	@Test void a02_literalValueExpressionIsLiteral() {
		var t = ValueResolver.getCompiledTemplate("plain string");
		assertTrue(t.isLiteral());
	}

	@Test void a03_propertyVarDoesNotFold_dynamicReads() {
		// PropertyVar stays unstable — Settings can be updated at runtime, so the
		// @Value("${key}") form must NOT fold at compile time.
		var t = ValueResolver.getCompiledTemplate("${Value_StableFolding_Test.dynamic:default}");
		assertFalse(t.isLiteral(),
			"PropertyVar (the ${...} shortcut) is unstable; template must not fold to literal");
	}
}
