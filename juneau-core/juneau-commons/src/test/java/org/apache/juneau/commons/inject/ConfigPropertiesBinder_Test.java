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

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Engine unit tests for {@link ConfigPropertiesBinder}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConfigPropertiesBinder_Test extends TestBase {

	// =================================================================================
	// Test helpers
	// =================================================================================

	private static PropertySource map(String... kv) {
		assertEquals(0, kv.length % 2, "map() requires an even number of key/value arguments");
		var m = new LinkedHashMap<String,String>();
		for (var i = 0; i < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return name -> m.containsKey(name) ? PropertyLookupResult.present(Optional.of(m.get(name))) : PropertyLookupResult.missing();
	}

	// =================================================================================
	// A. Basic binding
	// =================================================================================

	public static class A01_Target {
		public String name = "default-name";
	}

	@Test void a01_bindsPresentKey() {
		var settings = Settings.create().addSource(map("app.name", "orders")).build();
		var target = ConfigPropertiesBinder.of(new A01_Target(), "app").settings(settings).run();
		assertEquals("orders", target.name);
	}

	@Test void a02_bindOnlyPresent_leavesFieldInitializerDefaultUntouched() {
		var settings = Settings.create().build();
		var target = ConfigPropertiesBinder.of(new A01_Target(), "app").settings(settings).run();
		assertEquals("default-name", target.name);
	}

	public static class A03_Target {
		public String allowedHeaderParams;
	}

	@Test void a03_relaxedMatching_upperSnakeEnvStyleKey() {
		// Env-style key MY_SECTION_MY_KEY-equivalent for "app.allowedHeaderParams" -> APP_ALLOWED_HEADER_PARAMS.
		var settings = Settings.create().addSource(map("APP_ALLOWED_HEADER_PARAMS", "Accept")).build();
		var target = ConfigPropertiesBinder.of(new A03_Target(), "app").settings(settings).run();
		assertEquals("Accept", target.allowedHeaderParams);
	}

	@Test void a04_relaxedMatching_lowerDottedKey() {
		var settings = Settings.create().addSource(map("app.allowed.header.params", "Accept")).build();
		var target = ConfigPropertiesBinder.of(new A03_Target(), "app").settings(settings).run();
		assertEquals("Accept", target.allowedHeaderParams);
	}

	@Test void a05_relaxedFalse_onlyVerbatimMatches() {
		var settings = Settings.create().addSource(map("APP_ALLOWED_HEADER_PARAMS", "Accept")).build();
		var target = ConfigPropertiesBinder.of(new A03_Target(), "app").settings(settings).relaxed(false).run();
		assertNull(target.allowedHeaderParams);
	}

	public static class A06_Target {
		public String timeout;
	}

	@Test void a06_globalOverrideWinsOverSource() {
		// Per-test Settings instance carries its own global store, so no teardown is needed.
		var settings = Settings.create().addSource(map("app.timeout", "30")).build();
		settings.setGlobal("app.timeout", "99");
		var target = ConfigPropertiesBinder.of(new A06_Target(), "app").settings(settings).run();
		assertEquals("99", target.timeout);
	}

	@Test void a07_localOverrideWinsOverGlobal() {
		var settings = Settings.create().addSource(map("app.timeout", "30")).build();
		settings.setGlobal("app.timeout", "99");
		settings.setLocal("app.timeout", "7");
		var target = ConfigPropertiesBinder.of(new A06_Target(), "app").settings(settings).run();
		assertEquals("7", target.timeout);
	}

	public enum A08_Mode { ALWAYS, NEVER }

	public static class A08_Target {
		public boolean enabled;
		public A08_Mode mode;
		public Duration timeout;
		public List<String> tags;
	}

	@Test void a08_convertsPrimitiveBooleanEnumDurationAndCustomType() {
		var settings = Settings.create()
			.addSource(map(
				"app.enabled", "true",
				"app.mode", "ALWAYS",
				"app.timeout", "PT5S",
				"app.tags", "a,b,c"
			))
			.addTypeFunction(List.class, s -> List.of(s.split(",")))
			.build();
		var target = ConfigPropertiesBinder.of(new A08_Target(), "app").settings(settings).run();
		assertTrue(target.enabled);
		assertEquals(A08_Mode.ALWAYS, target.mode);
		assertEquals(Duration.ofSeconds(5), target.timeout);
		assertEquals(List.of("a", "b", "c"), target.tags);
	}

	public static class A09_Target {
		public String name;
	}

	@Test void a09_unrelatedSiblingKeyUnderSamePrefixIsIgnored() {
		var settings = Settings.create()
			.addSource(map("app.name", "orders", "app.somethingUnrelated", "whatever"))
			.build();
		var target = ConfigPropertiesBinder.of(new A09_Target(), "app").settings(settings).run();
		assertEquals("orders", target.name);
	}

	public static class A10_Target {
		public String name = "unset";
	}

	@Test void a10_ofTargetPrefix_minimalFormUsesDefaults() {
		// No .settings(...)/.relaxed(...)/.validator(...) calls — exercises the of(target, prefix).run()
		// minimal form and its Settings.get() / relaxed=true / NO_OP defaults.
		var target = ConfigPropertiesBinder.of(new A10_Target(), "ConfigPropertiesBinder_Test.a10_unused").run();
		assertEquals("unset", target.name); // Nothing registered under this prefix in the real Settings.get() chain.
	}

	public static class A11_Target {
		public Integer maxConnections;
		public Long maxBytes;
		public Boolean strict;
	}

	@Test void a11_wrapperTypes_integerLongBoolean() {
		var settings = Settings.create()
			.addSource(map("app.maxConnections", "10", "app.maxBytes", "9999999999", "app.strict", "true"))
			.build();
		var target = ConfigPropertiesBinder.of(new A11_Target(), "app").settings(settings).run();
		assertEquals(Integer.valueOf(10), target.maxConnections);
		assertEquals(Long.valueOf(9999999999L), target.maxBytes);
		assertEquals(Boolean.TRUE, target.strict);
	}

	public static class A12_Target {
		private String secret;
	}

	@Test void a12_privateFieldBinding() {
		var settings = Settings.create().addSource(map("app.secret", "hunter2")).build();
		var target = ConfigPropertiesBinder.of(new A12_Target(), "app").settings(settings).run();
		assertEquals("hunter2", target.secret);
	}

	// =================================================================================
	// B. Nested @ConfigProperties fields
	// =================================================================================

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B_Nested {
		public String host = "localhost";
		public int port = 80;
	}

	public static class B01_Target {
		public B_Nested server = new B_Nested();
	}

	@Test void b01_nestedBeanBindsFromSubPrefix() {
		var settings = Settings.create()
			.addSource(map("app.server.host", "example.com", "app.server.port", "8443"))
			.build();
		var target = ConfigPropertiesBinder.of(new B01_Target(), "app").settings(settings).run();
		assertEquals("example.com", target.server.host);
		assertEquals(8443, target.server.port);
	}

	public static class B02_Target {
		public B_Nested server; // null initially; a key resolves under app.server, so it must be instantiated.
	}

	@Test void b02_nullNestedFieldIsInstantiatedWhenAKeyResolvesUnderItsSubPrefix() {
		var settings = Settings.create().addSource(map("app.server.host", "example.com")).build();
		var target = ConfigPropertiesBinder.of(new B02_Target(), "app").settings(settings).run();
		assertNotNull(target.server);
		assertEquals("example.com", target.server.host);
		assertEquals(80, target.server.port); // default preserved (bind-only-present).
	}

	public static class B03_Target {
		public B_Nested server; // null initially; no key anywhere under app.server, so it must stay null.
	}

	@Test void b03_nullNestedFieldWithNoResolvableKeysStaysNull() {
		var settings = Settings.create().addSource(map("app.unrelated", "whatever")).build();
		var target = ConfigPropertiesBinder.of(new B03_Target(), "app").settings(settings).run();
		assertNull(target.server);
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B04_Inner {
		public String value = "inner-default";
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B04_Middle {
		public B04_Inner inner;
	}

	public static class B04_Target {
		public B04_Middle middle;
	}

	@Test void b04_twoLevelNesting() {
		var settings = Settings.create().addSource(map("app.middle.inner.value", "deep")).build();
		var target = ConfigPropertiesBinder.of(new B04_Target(), "app").settings(settings).run();
		assertNotNull(target.middle);
		assertNotNull(target.middle.inner);
		assertEquals("deep", target.middle.inner.value);
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B05_A {
		public B05_B b; // null; lazily materialized by the binder (can't eagerly new() a real mutual cycle).
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B05_B {
		public String value; // resolvable leaf, forcing B05_B to actually materialize.
		public B05_A a; // circular back-reference to the type already on the active bind path.
	}

	@Test void b05_circularNestingThrows() {
		var settings = Settings.create().addSource(map("app.b.value", "x")).build();
		var target = new B05_A();
		assertThrowsWithMessage(RuntimeException.class, List.of("Circular @ConfigProperties nesting detected", "B05_B.a"),
			() -> ConfigPropertiesBinder.of(target, "app").settings(settings).run());
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class B06_Base {
		public String value = "base-default";
	}

	// Declared field type is a SUBCLASS of an @ConfigProperties-annotated base — @ConfigProperties itself is not
	// @Inherited, but the binder's nested-field detection (fieldType.hasAnnotation()) walks the field type's class
	// hierarchy, so this still counts as a nested @ConfigProperties field (see the ConfigProperties class javadoc).
	public static class B06_Sub extends B06_Base {}

	public static class B06_Target {
		public B06_Sub sub = new B06_Sub();
	}

	@Test void b06_nestedFieldWithSubclassDeclaredTypeOfAnnotatedBaseStillBinds() {
		var settings = Settings.create().addSource(map("app.sub.value", "example")).build();
		var target = ConfigPropertiesBinder.of(new B06_Target(), "app").settings(settings).run();
		assertEquals("example", target.sub.value);
	}

	// =================================================================================
	// C. Post-bind validator hook
	// =================================================================================

	public static class C_Target {
		public String name;
	}

	@Test void c01_validatorHookIsInvokedExactlyOnceAfterBinding() {
		var settings = Settings.create().addSource(map("app.name", "orders")).build();
		var calls = new ArrayList<Object>();
		ConfigPropertiesValidator spy = calls::add;
		var target = ConfigPropertiesBinder.of(new C_Target(), "app").settings(settings).validator(spy).run();
		assertEquals(1, calls.size());
		assertSame(target, calls.get(0));
	}

	@Test void c02_defaultNoOpValidatorDoesNotThrow() {
		var settings = Settings.create().build();
		assertDoesNotThrow(() -> ConfigPropertiesBinder.of(new C_Target(), "app").settings(settings).run());
		ConfigPropertiesValidator.NO_OP.validate(new C_Target()); // direct no-op invocation is also safe.
	}

	@ConfigProperties(prefix = "unused-when-nested")
	public static class C03_Inner {
		public String value;
	}

	public static class C03_Target {
		public C03_Inner inner = new C03_Inner();
	}

	@Test void c03_validatorInvokedInnermostFirstUnderNesting() {
		var settings = Settings.create().addSource(map("app.inner.value", "x")).build();
		var order = new ArrayList<Object>();
		ConfigPropertiesValidator spy = order::add;
		var target = ConfigPropertiesBinder.of(new C03_Target(), "app").settings(settings).validator(spy).run();
		assertEquals(List.of(target.inner, target), order);
	}

	// =================================================================================
	// D. Null-arg contracts
	// =================================================================================

	public static class D_Target {
		public String name;
	}

	@Test void d01_nullTarget_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "target", () -> ConfigPropertiesBinder.of((D_Target)null, "app"));
	}

	@Test void d02_nullPrefix_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "prefix", () -> ConfigPropertiesBinder.of(new D_Target(), null));
	}

	@Test void d03_nullSettings_throwsAtRun() {
		assertThrowsWithMessage(IllegalArgumentException.class, "settings",
			() -> ConfigPropertiesBinder.of(new D_Target(), "app").settings(null).run());
	}

	@Test void d04_nullValidator_throwsAtRun() {
		assertThrowsWithMessage(IllegalArgumentException.class, "validator",
			() -> ConfigPropertiesBinder.of(new D_Target(), "app").validator(null).run());
	}

	// =================================================================================
	// E. Conversion-error context
	// =================================================================================

	public static class E01_Target {
		public int port;
	}

	@Test void e01_conversionFailure_wrapsWithKeyAndFieldContext() {
		var settings = Settings.create().addSource(map("app.port", "not-a-number")).build();
		var target = new E01_Target();
		assertThrowsWithMessage(RuntimeException.class, List.of("app.port", "E01_Target.port"),
			() -> ConfigPropertiesBinder.of(target, "app").settings(settings).run());
	}

	// =================================================================================
	// F. Precedence pinning test: candidate-major spelling resolution order.
	// =================================================================================

	public static class F01_Target {
		public int maxRetries;
	}

	@Test void f01_candidateMajorPrecedence_verbatimHitInLowerPrecedenceSourceOutranksRelaxedOverrideInHigherPrecedenceSource() {
		// Pins the documented "candidate-major" rule: each spelling is resolved through the FULL source
		// chain before the next spelling is tried. The verbatim spelling "app.maxRetries" resolves against the
		// registered (lowest-precedence) source before the relaxed spelling "APP_MAX_RETRIES" is even tried,
		// so the global override set under that relaxed spelling never gets a chance to win — even though a
		// global override normally outranks a registered source (see a06/a07 above).
		var settings = Settings.create().addSource(map("app.maxRetries", "5")).build();
		settings.setGlobal("APP_MAX_RETRIES", "99");
		var target = ConfigPropertiesBinder.of(new F01_Target(), "app").settings(settings).run();
		assertEquals(5, target.maxRetries);
	}

	// =================================================================================
	// G. Field exclusion (static / final / synthetic) — negative coverage
	// =================================================================================

	/**
	 * Non-static (inner) member class: besides the declared {@code staticField}/{@code finalField}, the compiler
	 * adds a synthetic {@code this$0} field referencing the enclosing {@code ConfigPropertiesBinder_Test} instance.
	 * Config keys are supplied that would match all three field names, so the test fails loudly if any of the
	 * {@code isNotStatic()}/{@code isNotFinal()}/{@code isNotSynthetic()} exclusion checks in {@code bind(...)} were
	 * ever removed or weakened.
	 */
	public class G01_Target {
		public static String staticField = "static-default";
		public final String finalField = "final-default";
	}

	@Test void g01_staticFinalAndSyntheticFieldsAreExcludedFromBinding() {
		var settings = Settings.create()
			.addSource(map(
				"app.staticField", "should-not-be-written",
				"app.finalField", "should-not-be-written",
				"app.this$0", "should-not-be-written"
			))
			.build();
		var target = this.new G01_Target();
		assertDoesNotThrow(() -> ConfigPropertiesBinder.of(target, "app").settings(settings).run());
		assertEquals("static-default", G01_Target.staticField); // static field: never written.
		assertEquals("final-default", target.finalField); // final field: left at its initializer.
	}

	// =================================================================================
	// H. Nested-instantiation error context
	// =================================================================================

	@ConfigProperties(prefix = "unused-when-nested")
	public static class H01_Nested {
		public String value;

		// Deliberately no no-arg constructor, so materialization must fail with field context (see h01 below).
		public H01_Nested(String value) {
			this.value = value;
		}
	}

	public static class H01_Target {
		public H01_Nested nested; // null; a key resolves under app.nested, forcing materialization to be attempted.
	}

	@Test void h01_nestedInstantiationFailure_wrapsWithFieldContext() {
		var settings = Settings.create().addSource(map("app.nested.value", "x")).build();
		var target = new H01_Target();
		assertThrowsWithMessage(RuntimeException.class, List.of("Could not instantiate nested @ConfigProperties field", "H01_Target.nested"),
			() -> ConfigPropertiesBinder.of(target, "app").settings(settings).run());
	}

	// =================================================================================
	// I. Caller-scoped PropertySource beans — consulted ahead of the global Settings chain
	// =================================================================================

	public static class I_Target {
		public String name = "default-name";
	}

	@Test void i01_scopedPropertySourceOverridesGlobalSettings() {
		// A scoped PropertySource bean supplied via beanStore(...) wins over the global Settings chain.
		var settings = Settings.create().addSource(map("app.name", "from-global")).build();
		var store = new BasicBeanStore(null);
		store.addBean(PropertySource.class, map("app.name", "from-scoped"));
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).beanStore(store).run();
		assertEquals("from-scoped", target.name);
	}

	@Test void i02_scopedPropertySourceSupplementsWhenGlobalMisses() {
		// Global chain has no key; the scoped source supplies it.
		var settings = Settings.create().build();
		var store = new BasicBeanStore(null);
		store.addBean(PropertySource.class, map("app.name", "from-scoped"));
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).beanStore(store).run();
		assertEquals("from-scoped", target.name);
	}

	@Test void i03_emptyBeanStoreFallsThroughToGlobalSettings_noBehaviorChange() {
		// A store with no PropertySource beans must behave exactly like binding against Settings alone.
		var settings = Settings.create().addSource(map("app.name", "from-global")).build();
		var store = new BasicBeanStore(null);
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).beanStore(store).run();
		assertEquals("from-global", target.name);
	}

	@Test void i04_scopedSourceMissKey_fallsThroughToGlobalForThatKey() {
		// Scoped source is present but doesn't carry this key; resolution falls through to the global chain.
		var settings = Settings.create().addSource(map("app.name", "from-global")).build();
		var store = new BasicBeanStore(null);
		store.addBean(PropertySource.class, map("app.unrelated", "x"));
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).beanStore(store).run();
		assertEquals("from-global", target.name);
	}

	@Test void i05_nullBeanStore_isDefault_bindsFromSettingsOnly() {
		// beanStore(...) never set: falls back to global Settings only.
		var settings = Settings.create().addSource(map("app.name", "from-global")).build();
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).run();
		assertEquals("from-global", target.name);
	}

	@Test void i06_settingsOverrideWinsOverScopedSource() {
		// Mirrors PropertyVar.resolve: a Settings.setGlobal/setLocal test-override outranks a scoped
		// PropertySource, exactly like the @Value resolution path.
		var settings = Settings.create().build();
		settings.setGlobal("app.name", "from-override");
		var store = new BasicBeanStore(null);
		store.addBean(PropertySource.class, map("app.name", "from-scoped"));
		var target = ConfigPropertiesBinder.of(new I_Target(), "app").settings(settings).beanStore(store).run();
		assertEquals("from-override", target.name);
	}

	// =================================================================================
	// J. Coexistence hardening — binder must not clobber @Value/@Inject-owned fields
	// =================================================================================

	public static class J01_Target {
		@Value("${app.label:v-default}")
		public String label = "init-label";
		public String name = "init-name";
	}

	@Test void j01_valueAnnotatedFieldIsLeftToValue_evenWhenMatchingConfigKeyExists() {
		// A matching "app.label" key must NOT be bound by the config binder over a @Value-owned field;
		// the binder leaves it at its initializer for the injection engine to resolve separately.
		var settings = Settings.create().addSource(map("app.label", "config-should-not-win", "app.name", "bound")).build();
		var target = ConfigPropertiesBinder.of(new J01_Target(), "app").settings(settings).run();
		assertEquals("init-label", target.label); // binder skipped the @Value field
		assertEquals("bound", target.name);       // ordinary field still bound
	}

	public static class J02_Target {
		@Inject
		public String injected = "init-injected";
		public String name = "init-name";
	}

	@Test void j02_injectAnnotatedFieldIsLeftToInject_evenWhenMatchingConfigKeyExists() {
		var settings = Settings.create().addSource(map("app.injected", "config-should-not-win", "app.name", "bound")).build();
		var target = ConfigPropertiesBinder.of(new J02_Target(), "app").settings(settings).run();
		assertEquals("init-injected", target.injected); // binder skipped the @Inject field
		assertEquals("bound", target.name);
	}
}
