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
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Coverage for {@code @Conditional}, {@code @ConditionalOnClass},
 * {@code @ConditionalOnMissingBean}, and {@code @ConditionalOnProperty} on both
 * {@code @Configuration} types (class-level) and {@code @Bean} members (member-level).
 *
 * <p>The evaluation semantics asserted here mirror the plan:
 * <ul>
 * 	<li>Class-level skip cascades to imports.
 * 	<li>Member-level skip applies only to that specific {@code @Bean} member.
 * 	<li>Evaluation is eager at registration time.
 * </ul>
 */
@SuppressWarnings({
	"java:S2094", // Empty fixture/config classes are intentional test fixtures.
	"java:S2093" // BasicBeanStore is AutoCloseable, but these try/finally blocks restore global Settings state, not the store; converting to try-with-resources would invoke store.close() (triggering @PreDestroy lifecycle these registration-semantics tests don't exercise) and dislocate the Settings setup/teardown.
})
class Conditional_Test extends TestBase {

	//------------------------------------------------------------------------------------------------
	// Fixtures.
	//------------------------------------------------------------------------------------------------

	public static class SvcA { public final String tag; public SvcA(String tag) { this.tag = tag; } }
	public static class SvcB { public SvcB() { /* intentionally empty */ } }

	public static class AlwaysTrue implements Condition {
		public AlwaysTrue() { /* intentionally empty */ }
		@Override public boolean matches(ConditionContext ctx) { return true; }
	}

	public static class AlwaysFalse implements Condition {
		public AlwaysFalse() { /* intentionally empty */ }
		@Override public boolean matches(ConditionContext ctx) { return false; }
	}

	public static class HasBeanProperty implements Condition {
		public HasBeanProperty() { /* intentionally empty */ }
		@Override public boolean matches(ConditionContext ctx) {
			return ctx.settings().get("conditional.test.enabled").isPresent();
		}
	}

	/**
	 * Custom condition that exercises every {@link ConditionContext} accessor at least once.
	 *
	 * <p>Used to give the test suite coverage of {@code beanStore()}, {@code classLoader()}, and
	 * {@code annotatedElement()} (in addition to {@code settings()}, which is reached by
	 * {@link HasBeanProperty}).
	 */
	public static class FullContextSnapshot implements Condition {
		public FullContextSnapshot() { /* intentionally empty */ }
		@Override public boolean matches(ConditionContext ctx) {
			return ctx.beanStore() != null
				&& ctx.classLoader() != null
				&& ctx.annotatedElement() != null
				&& ctx.settings() != null;
		}
	}

	@Configuration
	@Conditional(FullContextSnapshot.class)
	public static class FullContextGatedConfig {
		public FullContextGatedConfig() { /* intentionally empty */ }
		@Bean public SvcA svcA() { return new SvcA("full-context"); }
	}

	@Configuration
	public static class ClassLevelTrueConfig {
		public ClassLevelTrueConfig() { /* intentionally empty */ }
		@Bean public SvcA svcA() { return new SvcA("class-level-true"); }
	}

	@Configuration
	@Conditional(AlwaysFalse.class)
	public static class ClassLevelFalseConfig {
		public ClassLevelFalseConfig() { /* intentionally empty */ }
		@Bean public SvcA svcA() { return new SvcA("class-level-false"); }
	}

	@Configuration(imports = { ClassLevelFalseConfig.class })
	public static class ImporterOfFalse {
		public ImporterOfFalse() { /* intentionally empty */ }
		@Bean public SvcB svcB() { return new SvcB(); }
	}

	@Configuration
	public static class MemberLevelConfig {
		public MemberLevelConfig() { /* intentionally empty */ }
		@Bean @Conditional(AlwaysTrue.class) public SvcA enabled() { return new SvcA("enabled"); }
		@Bean @Conditional(AlwaysFalse.class) public SvcB disabled() { return new SvcB(); }
	}

	/**
	 * Configuration that uses {@code @Conditional} on instance fields to exercise the field-level
	 * conditional cascade in {@code BasicBeanStore.registerConfiguration}.  Both fields are non-static
	 * which forces the lazy {@code instance = BeanInstantiator.of(configType, this).run()} path on the
	 * first eligible field AND the "instance != null" short-circuit on subsequent ones.
	 */
	@Configuration
	public static class FieldConditionalConfig {
		public FieldConditionalConfig() { /* intentionally empty */ }
		@Bean(name = "enabled") @Conditional(AlwaysTrue.class) public SvcA enabledField = new SvcA("enabled-field");
		@Bean(name = "disabled") @Conditional(AlwaysFalse.class) public SvcA disabledField = new SvcA("disabled-field");
		@Bean(name = "second") public SvcA secondField = new SvcA("second-field");
	}

	@Configuration
	public static class OnClassConfig {
		public OnClassConfig() { /* intentionally empty */ }
		@Bean @ConditionalOnClass("java.lang.String") public SvcA presentBean() { return new SvcA("present"); }
		@Bean @ConditionalOnClass("definitely.does.not.Exist") public SvcB absentBean() { return new SvcB(); }
	}

	@Configuration
	public static class OnMissingBeanConfig {
		public OnMissingBeanConfig() { /* intentionally empty */ }
		@Bean(name = "second") @ConditionalOnMissingBean(SvcA.class) public SvcA conditional() {
			return new SvcA("conditional");
		}
	}

	@Configuration
	public static class OnMissingBeanNamedConfig {
		public OnMissingBeanNamedConfig() { /* intentionally empty */ }
		// type=Object.class signals "match by bean name only". The conditional bean is skipped if a
		// bean of any type already exists under "primaryName" in the local store.
		@Bean(name = "secondary")
		@ConditionalOnMissingBean(name = "primaryName")
		public SvcA conditional() { return new SvcA("conditional"); }
	}

	@Configuration
	public static class OnPropertyConfig {
		public OnPropertyConfig() { /* intentionally empty */ }
		@Bean @ConditionalOnProperty(name = "conditional.test.enabled") public SvcA enabled() {
			return new SvcA("enabled");
		}
		@Bean @ConditionalOnProperty(name = "conditional.test.missing", matchIfMissing = true) public SvcB missing() {
			return new SvcB();
		}
	}

	@Configuration
	public static class OnPropertyHavingValueConfig {
		public OnPropertyHavingValueConfig() { /* intentionally empty */ }
		// Property must equal "expected" — when it's present but different, registration is skipped.
		@Bean @ConditionalOnProperty(name = "conditional.test.matchedValue", havingValue = "expected")
		public SvcA matched() { return new SvcA("matched"); }
	}

	@Configuration
	public static class CustomConditionalConfig {
		public CustomConditionalConfig() { /* intentionally empty */ }
		@Bean @Conditional(HasBeanProperty.class) public SvcA gated() { return new SvcA("gated"); }
	}

	//------------------------------------------------------------------------------------------------
	// Tests - class-level conditionals.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_classLevelFalse_skipsClass() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(ClassLevelFalseConfig.class);
		assertFalse(store.getBean(SvcA.class).isPresent());
	}

	@Test
	void a02_classLevelFalse_skipsImporterChain() {
		// The importer itself is registerable but its imported FalseConfig skips silently.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(ImporterOfFalse.class);
		assertTrue(store.getBean(SvcB.class).isPresent(),
			"Importer's own beans should still register when only the import is skipped");
		assertFalse(store.getBean(SvcA.class).isPresent(),
			"Skipped class-level conditional should not contribute beans");
	}

	@Test
	void a03_classLevelTrue_registers() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(ClassLevelTrueConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent());
	}

	//------------------------------------------------------------------------------------------------
	// Tests - member-level conditionals.
	//------------------------------------------------------------------------------------------------

	@Test
	void b01_memberLevelTrue_registers_falseSkips() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(MemberLevelConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent(), "True member should register");
		assertFalse(store.getBean(SvcB.class).isPresent(), "False member should be skipped");
	}

	@Test
	void b02_fieldLevelConditional_appliesPerField() {
		// First eligible instance field forces lazy instantiation; subsequent eligible field reuses
		// the cached instance (instance != null branch).  Disabled field is skipped entirely.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(FieldConditionalConfig.class);

		assertTrue(store.getBean(SvcA.class, "enabled").isPresent(),
			"AlwaysTrue field-level @Conditional must register the bean");
		assertTrue(store.getBean(SvcA.class, "second").isPresent(),
			"Unconditional field must still register (exercises instance != null branch)");
		assertFalse(store.getBean(SvcA.class, "disabled").isPresent(),
			"AlwaysFalse field-level @Conditional must skip the bean");
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @ConditionalOnClass.
	//------------------------------------------------------------------------------------------------

	@Test
	void c01_onClass_present_registers() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OnClassConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent());
	}

	@Test
	void c02_onClass_absent_skips() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OnClassConfig.class);
		assertFalse(store.getBean(SvcB.class).isPresent());
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @ConditionalOnMissingBean.
	//------------------------------------------------------------------------------------------------

	@Test
	void d01_onMissingBean_skipsWhenPresent() {
		// Pre-populate SvcA in the store; @ConditionalOnMissingBean should then skip the conditional bean.
		var store = new BasicBeanStore(null);
		store.addBean(SvcA.class, new SvcA("existing"));
		store.registerConfiguration(OnMissingBeanConfig.class);
		assertEquals("existing", store.getBean(SvcA.class).get().tag);
		assertFalse(store.getBean(SvcA.class, "second").isPresent(),
			"Conditional-on-missing-bean should skip when type already present");
	}

	@Test
	void d02_onMissingBean_registersWhenAbsent() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OnMissingBeanConfig.class);
		var second = store.getBean(SvcA.class, "second");
		assertTrue(second.isPresent(), "When no existing SvcA, conditional should register");
		assertEquals("conditional", second.get().tag);
	}

	@Test
	void d03_onMissingBeanByName_skipsWhenNameAlreadyPresent() {
		// type=Object.class + name="primaryName" means "skip if anything with that name exists".
		var store = new BasicBeanStore(null);
		store.addBean(SvcB.class, new SvcB(), "primaryName");
		store.registerConfiguration(OnMissingBeanNamedConfig.class);
		assertFalse(store.getBean(SvcA.class, "secondary").isPresent(),
			"Name-only @ConditionalOnMissingBean should skip when a bean with that name exists");
	}

	@Test
	void d04_onMissingBeanByName_registersWhenNameAbsent() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OnMissingBeanNamedConfig.class);
		assertTrue(store.getBean(SvcA.class, "secondary").isPresent(),
			"Name-only @ConditionalOnMissingBean should register when no bean has that name");
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @ConditionalOnProperty.
	//------------------------------------------------------------------------------------------------

	@Test
	void e01_onProperty_skipsWhenMissing() {
		Settings.get().unsetLocal("conditional.test.enabled");
		var store = new BasicBeanStore(null);
		store.registerConfiguration(OnPropertyConfig.class);
		assertFalse(store.getBean(SvcA.class).isPresent());
		assertTrue(store.getBean(SvcB.class).isPresent(), "matchIfMissing=true should register the bean");
	}

	@Test
	void e02_onProperty_registersWhenPresent() {
		try {
			Settings.get().setLocal("conditional.test.enabled", "true");
			var store = new BasicBeanStore(null);
			store.registerConfiguration(OnPropertyConfig.class);
			assertTrue(store.getBean(SvcA.class).isPresent());
		} finally {
			Settings.get().unsetLocal("conditional.test.enabled");
		}
	}

	@Test
	void e03_onProperty_havingValueMatches_registers() {
		try {
			Settings.get().setLocal("conditional.test.matchedValue", "expected");
			var store = new BasicBeanStore(null);
			store.registerConfiguration(OnPropertyHavingValueConfig.class);
			assertTrue(store.getBean(SvcA.class).isPresent());
		} finally {
			Settings.get().unsetLocal("conditional.test.matchedValue");
		}
	}

	@Test
	void e04_onProperty_havingValueMismatch_skips() {
		try {
			Settings.get().setLocal("conditional.test.matchedValue", "different");
			var store = new BasicBeanStore(null);
			store.registerConfiguration(OnPropertyHavingValueConfig.class);
			assertFalse(store.getBean(SvcA.class).isPresent(),
				"Property is present but does not equal havingValue — bean must be skipped");
		} finally {
			Settings.get().unsetLocal("conditional.test.matchedValue");
		}
	}

	//------------------------------------------------------------------------------------------------
	// Tests - custom @Conditional with ConditionContext access.
	//------------------------------------------------------------------------------------------------

	@Test
	void f01_customCondition_sees_settings() {
		try {
			Settings.get().setLocal("conditional.test.enabled", "yes");
			var store = new BasicBeanStore(null);
			store.registerConfiguration(CustomConditionalConfig.class);
			assertTrue(store.getBean(SvcA.class).isPresent());
		} finally {
			Settings.get().unsetLocal("conditional.test.enabled");
		}
	}

	@Test
	void f02_customCondition_skipsWhenFalse() {
		Settings.get().unsetLocal("conditional.test.enabled");
		var store = new BasicBeanStore(null);
		store.registerConfiguration(CustomConditionalConfig.class);
		assertFalse(store.getBean(SvcA.class).isPresent());
	}

	@Test
	void f03_customCondition_seesEntireContext() {
		// FullContextSnapshot dereferences beanStore(), classLoader(), and annotatedElement() so all
		// four ConditionContext accessors are exercised at least once via the public API path.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(FullContextGatedConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent(),
			"FullContextSnapshot must observe non-null beanStore/classLoader/element/settings");
	}

	//------------------------------------------------------------------------------------------------
	// Tests - WritableBeanStore.registerConfigurations defensive-null branch.
	//------------------------------------------------------------------------------------------------

	@Test
	void g01_registerConfigurations_nullVarargs_isNoOp() {
		var store = new BasicBeanStore(null);
		var result = store.registerConfigurations((Class<?>[]) null);
		assertSame(store, result, "registerConfigurations(null) should return the same store");
		assertFalse(store.getBean(SvcA.class).isPresent(),
			"No bean should be registered when configTypes is null");
	}

	//------------------------------------------------------------------------------------------------
	// Tests - logger-fine lambda coverage.
	//
	// BasicBeanStore.matchesConditions() emits two LOGGER.fine(...) messages for skip events.  When
	// the BasicBeanStore logger is at FINE level, the lambdas actually execute and their message-
	// construction bodies become observable to JaCoCo.  The tests below temporarily raise the level
	// and verify the skip still happens (the captured handler is asserted on too).
	//------------------------------------------------------------------------------------------------

	@org.apache.juneau.commons.inject.Configuration
	@ConditionalOnClass("definitely.does.not.Exist")
	public static class ClassLevelOnClassMissingConfig {
		public ClassLevelOnClassMissingConfig() { /* intentionally empty */ }
		@Bean public SvcA svcA() { return new SvcA("never"); }
	}

	@Test
	void h01_loggerFine_messageLambdasExecute() {
		var logger = java.util.logging.Logger.getLogger(BasicBeanStore.class.getName());
		var originalLevel = logger.getLevel();
		var originalUseParent = logger.getUseParentHandlers();
		var captured = new java.util.ArrayList<String>();
		var handler = new java.util.logging.Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { captured.add(r.getMessage()); }
			@Override public void flush() { /* no-op */ }
			@Override public void close() { /* no-op */ }
		};
		handler.setLevel(java.util.logging.Level.FINE);
		try {
			logger.setLevel(java.util.logging.Level.FINE);
			logger.setUseParentHandlers(false);
			logger.addHandler(handler);

			// 1) AlwaysFalse @Conditional path → "Skipping conditional element due to @Conditional: …"
			var store1 = new BasicBeanStore(null);
			store1.registerConfiguration(ClassLevelFalseConfig.class);
			assertFalse(store1.getBean(SvcA.class).isPresent());

			// 2) @ConditionalOnClass("definitely.does.not.Exist") → "Skipping conditional element due to missing class …"
			var store2 = new BasicBeanStore(null);
			store2.registerConfiguration(ClassLevelOnClassMissingConfig.class);
			assertFalse(store2.getBean(SvcA.class).isPresent());

			assertTrue(captured.stream().anyMatch(m -> m.contains("@Conditional")),
				"Expected a @Conditional skip log; got: " + captured);
			assertTrue(captured.stream().anyMatch(m -> m.contains("missing class")),
				"Expected a missing-class skip log; got: " + captured);
		} finally {
			logger.removeHandler(handler);
			logger.setLevel(originalLevel);
			logger.setUseParentHandlers(originalUseParent);
		}
	}
}
