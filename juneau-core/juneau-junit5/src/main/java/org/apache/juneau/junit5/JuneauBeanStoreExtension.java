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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit 5 extension that discovers {@link TestBean @TestBean}-annotated members on a test class and exposes them as
 * a {@link TestBeanStore} overlay.
 *
 * <a id="ModeInject"></a>
 * <h5 class='section'>Mode INJECT &mdash; fresh-instance with overrides (default):</h5>
 * <p>
 * The extension supports two wiring patterns.  <b>Mode INJECT</b> is the default and what every
 * {@code @TestBean}-based test ships with out of the box.  The contract:
 * <ul>
 * 	<li>The SUT (a {@code RestContext} via {@code MockRestClient}, a {@code Microservice}, a {@code SerializerSet},
 * 		etc.) is constructed <i>after</i> the overlay is built.  The overlay is threaded into the builder via
 * 		{@code overridingBeanStore(...)} (see {@link org.apache.juneau.commons.inject.BeanStoreOverridable}), which
 * 		installs it in the {@code overridingParent} slot of the SUT's bean store at construction time.
 * 	<li>Because the overlay sits at tier&nbsp;1 of the resolution chain, every framework-managed bean lookup
 * 		consults it <i>before</i> any local {@code @Bean} factory or memoizer-backed default.  Mode INJECT is
 * 		therefore <i>universal</i>: all bean types are eligible for replacement, including those that subsequently
 * 		get pinned into a per-op memoizer.
 * 	<li>The overlay's lifetime is bounded by the SUT's lifetime.  For a per-test fresh SUT (the most common
 * 		shape), the overlay is discarded when the SUT goes out of scope at {@code @AfterEach}.  For a per-class
 * 		shared SUT (built once in {@code @BeforeAll}), the overlay survives until the SUT is torn down at
 * 		{@code @AfterAll}.  No explicit push/pop is required.
 * </ul>
 *
 * <p>
 * {@link org.apache.juneau.junit5.TestBean @TestBean}, {@link TestBeanStore},
 * {@link org.apache.juneau.commons.inject.BeanStoreOverridable BeanStoreOverridable}, and the {@code overridingBeanStore(...)}
 * setters on {@code MockRestClient.Builder}, {@code Microservice.Builder}, {@code SerializerSet.Builder},
 * {@code ParserSet.Builder}, and {@code EncoderSet.Builder} all implement this Mode INJECT contract.
 *
 * <a id="ModeOverlay"></a>
 * <h5 class='section'>Mode OVERLAY &mdash; existing-instance with push/pop:</h5>
 * <p>
 * <b>Mode OVERLAY</b> is the opt-in pattern for tests that hold a <i>long-lived</i> SUT (typically a
 * {@code Microservice} booted in {@code @BeforeAll}) and want to apply per-test overlays without rebuilding it.
 * The test calls {@link #attach(org.apache.juneau.commons.inject.WritableBeanStore)} to point the extension at the
 * SUT's bean store; the extension then uses
 * {@link org.apache.juneau.commons.inject.WritableBeanStore#pushOverlay(org.apache.juneau.commons.inject.BeanStore)}
 * at {@code beforeEach} / {@code beforeAll} and
 * {@link org.apache.juneau.commons.inject.WritableBeanStore#popOverlay(org.apache.juneau.commons.inject.Snapshot)}
 * at {@code afterEach} / {@code afterAll} so the SUT's bean store is restored between tests.
 *
 * <p>
 * Mode OVERLAY requires {@code @TestBean(mode = Mode.OVERLAY)} on the participating annotations.  Mixing
 * {@code Mode.INJECT} and {@code Mode.OVERLAY} on the same scope is rejected with a clear
 * {@link IllegalStateException}.
 *
 * <h5 class='section'>Overlay storage:</h5>
 * <p>
 * The extension owns one or two {@link TestBeanStore} overlays per test execution:
 * <ul>
 * 	<li>A <b>class-scope</b> overlay built at {@code beforeAll}, containing every {@code @TestBean(scope = CLASS)}
 * 		declared on {@code static} fields/methods of the test class hierarchy.  Lives until {@code afterAll}.
 * 	<li>A <b>method-scope</b> overlay built at {@code beforeEach}, containing every {@code @TestBean(scope = METHOD)}
 * 		declared on instance (or static, for convenience) fields/methods.  The method-scope overlay's parent is set
 * 		to the class-scope overlay (if any), so a method-scope override of {@code (Type, name)} shadows a
 * 		class-scope override of the same pair while still letting the class-scope's other entries fall through.
 * 		Lives until {@code afterEach}.
 * </ul>
 *
 * <p>
 * Wiring into the system under test is <i>explicit</i> &mdash; the extension never reflects on the test class to
 * find a {@code RestContext} / {@code Microservice} field and never mutates the SUT after construction.  Test
 * authors retrieve the current overlay via {@link #getStore()} and plug it into the SUT's builder hook:
 *
 * <p class='bjava'>
 * 	<ja>@ExtendWith</ja>(JuneauBeanStoreExtension.<jk>class</jk>)
 * 	<jk>class</jk> MyResourceTest {
 * 		<ja>@TestBean</ja> MyService <jv>mockSvc</jv> = Mockito.<jsm>mock</jsm>(MyService.<jk>class</jk>);
 *
 * 		<ja>@Test</ja>
 * 		<jk>void</jk> aTest(TestBeanStore <jv>store</jv>) {
 * 			<jk>var</jk> <jv>client</jv> = MockRestClient.<jsm>create</jsm>(MyResource.<jk>class</jk>)
 * 				.overridingBeanStore(<jv>store</jv>)
 * 				.build();
 * 			<jv>client</jv>.get(<js>"/svc"</js>).run().assertStatus().is(200);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * Two registration styles are supported:
 * <ul>
 * 	<li><b>Declarative:</b> {@code @ExtendWith(JuneauBeanStoreExtension.class)} on the test class.
 * 	<li><b>Programmatic:</b> a {@code @RegisterExtension} field initialized with {@link #create()}, so the test can
 * 		invoke {@link #getStore()} from within {@code @BeforeAll} / {@code @BeforeEach} bodies as well as from test
 * 		methods.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link TestBean} - The annotation discovered by this extension.
 * 	<li class='jc'>{@link TestBeanStore} - The overlay type the extension produces.
 * 	<li class='je'>{@link Scope} - Per-method vs per-class lifecycle selector.
 * 	<li class='je'>{@link Mode} - Mode INJECT (fresh instance) vs Mode OVERLAY (existing instance push/pop) selector.
 * </ul>
 *
 * @since 9.5.0
 */
@SuppressWarnings({
	"java:S3011" // setAccessible(true) is required to read package-private/private @TestBean members on user test classes
})
public class JuneauBeanStoreExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback,
		AfterEachCallback, ParameterResolver {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(JuneauBeanStoreExtension.class);
	private static final String KEY_CLASS_STORE = "class-store";
	private static final String KEY_METHOD_STORE = "method-store";
	private static final String KEY_CLASS_SNAPSHOT = "class-snapshot";
	private static final String KEY_METHOD_SNAPSHOT = "method-snapshot";

	/**
	 * Creates a new, unconfigured extension instance for programmatic registration via {@code @RegisterExtension}.
	 *
	 * <p>
	 * Equivalent to {@code new JuneauBeanStoreExtension()} but reads more naturally at the registration site:
	 *
	 * <p class='bjava'>
	 * 	<ja>@RegisterExtension</ja>
	 * 	JuneauBeanStoreExtension <jv>ext</jv> = JuneauBeanStoreExtension.<jsm>create</jsm>();
	 * </p>
	 *
	 * @return A new extension instance.  Never <jk>null</jk>.
	 * @since 9.5.0
	 */
	public static JuneauBeanStoreExtension create() {
		return new JuneauBeanStoreExtension();
	}

	private ExtensionContext currentClassContext;
	private ExtensionContext currentMethodContext;
	private volatile WritableBeanStore attached;

	/**
	 * Constructor.
	 */
	public JuneauBeanStoreExtension() { /* no-op */ }

	//-----------------------------------------------------------------------------------------------------------------
	// Mode OVERLAY attach / detach
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Points this extension at a long-lived SUT's {@link WritableBeanStore} for Mode OVERLAY push/pop wiring.
	 *
	 * <p>
	 * Call from a {@code @BeforeAll} or {@code @BeforeEach} body when the participating {@code @TestBean} declarations
	 * use {@link Mode#OVERLAY}.  Once attached, the extension will:
	 * <ul>
	 * 	<li>{@link WritableBeanStore#pushOverlay(BeanStore) push} the class-scope overlay onto the attached store at
	 * 		{@code beforeAll} (if a non-empty class-scope overlay exists), and
	 * 		{@link WritableBeanStore#popOverlay(Snapshot) pop} it at {@code afterAll}.
	 * 	<li>Push the method-scope overlay at {@code beforeEach} and pop it at {@code afterEach}.
	 * </ul>
	 *
	 * <p>
	 * Calling {@code attach(...)} a second time before {@link #detach()} is permitted &mdash; the most recent
	 * attachment wins.  If never called, the extension stays in Mode INJECT.
	 *
	 * @param store The bean store to push/pop overlays against.  Must not be <jk>null</jk>.
	 * @return This extension, for fluent chaining.
	 * @throws NullPointerException If {@code store} is <jk>null</jk>.
	 * @since 9.5.0
	 */
	public JuneauBeanStoreExtension attach(WritableBeanStore store) {
		Objects.requireNonNull(store, "store must not be null");
		attached = store;
		return this;
	}

	/**
	 * Clears the {@link #attach(WritableBeanStore) attached} bean store reference.
	 *
	 * <p>
	 * Called automatically at {@code afterAll}; tests may also call it explicitly between scenarios.  Has no effect
	 * if the extension is not currently attached.
	 *
	 * @return This extension, for fluent chaining.
	 * @since 9.5.0
	 */
	public JuneauBeanStoreExtension detach() {
		attached = null;
		return this;
	}

	/**
	 * Returns the {@link #attach(WritableBeanStore) attached} bean store, or empty if none.
	 *
	 * <p>
	 * Surface for diagnostic / testability purposes &mdash; the push/pop lifecycle uses the field directly.
	 *
	 * @return The attached store, or {@link Optional#empty()} if not currently attached.
	 * @since 9.5.0
	 */
	public Optional<WritableBeanStore> getAttachedStore() {
		return Optional.ofNullable(attached);
	}

	/**
	 * Returns the current effective overlay &mdash; method-scope if a test method is executing, otherwise the
	 * class-scope overlay.
	 *
	 * <p>
	 * This is the accessor most tests plug into builder hooks such as
	 * {@code MockRestClient.Builder.overridingBeanStore(...)}.
	 *
	 * @return The current overlay.  Never <jk>null</jk>.
	 * @throws IllegalStateException If no class-scope or method-scope overlay is currently active (e.g. the accessor
	 * 	is called from a {@code @BeforeAll} body and the extension is not yet bound to a test class).
	 * @since 9.5.0
	 */
	public TestBeanStore getStore() {
		var s = currentMethodStore();
		if (s != null)
			return s;
		s = currentClassStore();
		if (s != null)
			return s;
		throw new IllegalStateException("JuneauBeanStoreExtension is not currently bound to a test class or test method.");
	}

	/**
	 * Returns the overlay for the specified scope, if one is currently active.
	 *
	 * <p>
	 * Allows a test to reach past the current effective overlay and inspect a specific lifecycle layer.  For
	 * example, a test running under both a class-scope and a method-scope overlay can use
	 * {@code getStore(Scope.CLASS)} to retrieve the shared per-class layer directly.
	 *
	 * @param scope The lifecycle scope to query.  Must not be <jk>null</jk>.
	 * @return The overlay for that scope, or {@link Optional#empty()} if no such overlay is active.
	 * @since 9.5.0
	 */
	public Optional<TestBeanStore> getStore(Scope scope) {
		Objects.requireNonNull(scope, "scope must not be null");
		return Optional.ofNullable(scope == Scope.METHOD ? currentMethodStore() : currentClassStore());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Lifecycle callbacks
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* BeforeAllCallback */
	public void beforeAll(ExtensionContext context) {
		currentClassContext = context;
		var testClass = context.getRequiredTestClass();
		var scoped = buildClassScopeStoreWithMode(testClass);
		context.getStore(NAMESPACE).put(KEY_CLASS_STORE, scoped.store());
		// Mode OVERLAY push (if attached) — only fires when at least one class-scope @TestBean declared
		// mode = Mode.OVERLAY.  All declarations in the scope must agree on the mode (mixing throws in
		// buildClassScopeStoreWithMode).
		var snapshot = pushOverlayIfOverlayMode(scoped);
		if (snapshot != null)
			context.getStore(NAMESPACE).put(KEY_CLASS_SNAPSHOT, snapshot);
	}

	@Override /* AfterAllCallback */
	public void afterAll(ExtensionContext context) {
		popOverlayIfPresent(context, KEY_CLASS_SNAPSHOT);
		context.getStore(NAMESPACE).remove(KEY_CLASS_STORE);
		currentClassContext = null;
	}

	@Override /* BeforeEachCallback */
	public void beforeEach(ExtensionContext context) {
		currentMethodContext = context;
		var classStore = readClassStore(context);
		var testInstance = context.getRequiredTestInstance();
		var scoped = buildMethodScopeStoreWithMode(testInstance, classStore);
		context.getStore(NAMESPACE).put(KEY_METHOD_STORE, scoped.store());
		var snapshot = pushOverlayIfOverlayMode(scoped);
		if (snapshot != null)
			context.getStore(NAMESPACE).put(KEY_METHOD_SNAPSHOT, snapshot);
	}

	@Override /* AfterEachCallback */
	public void afterEach(ExtensionContext context) {
		popOverlayIfPresent(context, KEY_METHOD_SNAPSHOT);
		context.getStore(NAMESPACE).remove(KEY_METHOD_STORE);
		currentMethodContext = null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Mode OVERLAY push/pop helpers
	//-----------------------------------------------------------------------------------------------------------------

	private PushedOverlay pushOverlayIfOverlayMode(ScopedStore scoped) {
		if (scoped.mode() != Mode.OVERLAY || scoped.empty())
			return null;
		var store = attached;
		if (store == null)
			throw new IllegalStateException(
				"@TestBean(mode = Mode.OVERLAY) declared but JuneauBeanStoreExtension was not attached to a"
				+ " WritableBeanStore.  Call extension.attach(sut.getBeanStore()) from @BeforeAll / @BeforeEach"
				+ " before the test runs.");
		var snapshot = store.pushOverlay(scoped.store());
		return new PushedOverlay(store, snapshot);
	}

	private static void popOverlayIfPresent(ExtensionContext context, String key) {
		var pushed = (PushedOverlay) context.getStore(NAMESPACE).remove(key);
		if (pushed == null)
			return;
		// Pop against the original store reference captured at push time, so the pop works even if the test
		// has since detach()-ed the extension or attach()-ed it to a different store.
		pushed.store().popOverlay(pushed.snapshot());
	}

	/** Captures the {@link WritableBeanStore} an overlay was pushed against, plus the resulting {@link Snapshot}. */
	private record PushedOverlay(WritableBeanStore store, Snapshot snapshot) { /* no body */ }

	@Override /* ParameterResolver */
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.getParameter().getType() == TestBeanStore.class;
	}

	@Override /* ParameterResolver */
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		// Prefer the method-scope store from the supplied extensionContext (the parameter being resolved belongs to
		// the test method currently executing); fall back to class-scope when the parameter is resolved outside a
		// method context (rare — e.g. constructor injection on a TestInstance.Lifecycle.PER_METHOD test class).
		var methodStore = (TestBeanStore) extensionContext.getStore(NAMESPACE).get(KEY_METHOD_STORE);
		if (methodStore != null)
			return methodStore;
		var classStore = readClassStore(extensionContext);
		if (classStore != null)
			return classStore;
		throw new ParameterResolutionException(
			"JuneauBeanStoreExtension cannot resolve TestBeanStore parameter: no overlay is active.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Discovery
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builds a class-scope overlay for the supplied test class.
	 *
	 * <p>
	 * Walks the class hierarchy parent-to-child registering every {@code @TestBean(scope = CLASS)} declared on a
	 * {@code static} field or {@code static} method.  Non-static {@code scope = CLASS} declarations throw an
	 * {@link ExtensionContextException} &mdash; instance state isn't available at {@code beforeAll} time.
	 *
	 * <p>
	 * Package-private to allow direct unit testing of the discovery logic without driving the full JUnit lifecycle.
	 */
	static TestBeanStore buildClassScopeStore(Class<?> testClass) {
		return buildClassScopeStoreWithMode(testClass).store();
	}

	/**
	 * Builds a method-scope overlay for the supplied test instance, optionally chained on top of a class-scope
	 * overlay.
	 *
	 * <p>
	 * Package-private to allow direct unit testing of the discovery logic without driving the full JUnit lifecycle.
	 */
	static TestBeanStore buildMethodScopeStore(Object testInstance, TestBeanStore classScopeParent) {
		return buildMethodScopeStoreWithMode(testInstance, classScopeParent).store();
	}

	/**
	 * {@link #buildClassScopeStore(Class)} plus the unified mode (INJECT or OVERLAY) of all class-scope
	 * declarations.
	 *
	 * <p>
	 * Mixing {@code Mode.INJECT} and {@code Mode.OVERLAY} declarations in the same scope throws an
	 * {@link IllegalStateException}.
	 */
	static ScopedStore buildClassScopeStoreWithMode(Class<?> testClass) {
		var store = new TestBeanStore();
		var modeTracker = new ModeTracker(Scope.CLASS);
		populateStaticOverrides(store, testClass, modeTracker);
		return new ScopedStore(store, modeTracker.resolve(), modeTracker.empty());
	}

	/**
	 * {@link #buildMethodScopeStore(Object, TestBeanStore)} plus the unified mode (INJECT or OVERLAY) of all
	 * method-scope declarations.
	 *
	 * <p>
	 * Mixing {@code Mode.INJECT} and {@code Mode.OVERLAY} declarations in the same scope throws an
	 * {@link IllegalStateException}.
	 */
	static ScopedStore buildMethodScopeStoreWithMode(Object testInstance, TestBeanStore classScopeParent) {
		var store = new TestBeanStore(classScopeParent);
		var modeTracker = new ModeTracker(Scope.METHOD);
		populateInstanceOverrides(store, testInstance, modeTracker);
		return new ScopedStore(store, modeTracker.resolve(), modeTracker.empty());
	}

	/**
	 * Walks the test-class hierarchy and registers every {@code @TestBean(scope = CLASS)} declared on a
	 * {@code static} field or {@code static} method.
	 *
	 * <p>
	 * Non-static {@code scope = CLASS} declarations are rejected with an {@link ExtensionContextException} &mdash;
	 * instance state isn't available at {@code beforeAll} time.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for hierarchical test bean override wiring
	})
	private static void populateStaticOverrides(TestBeanStore store, Class<?> testClass, ModeTracker modeTracker) {
		for (var c : classHierarchy(testClass)) {
			for (var f : c.getDeclaredFields()) {
				var ann = f.getAnnotation(TestBean.class);
				if (ann == null || ann.scope() != Scope.CLASS)
					continue;
				if (!Modifier.isStatic(f.getModifiers()))
					throw new ExtensionContextException(
						"@TestBean(scope = CLASS) field must be static: " + describe(f));
				modeTracker.observe(ann.mode(), describe(f));
				register(store, f, null, ann);
			}
			for (var m : c.getDeclaredMethods()) {
				var ann = m.getAnnotation(TestBean.class);
				if (ann == null || ann.scope() != Scope.CLASS)
					continue;
				if (!Modifier.isStatic(m.getModifiers()))
					throw new ExtensionContextException(
						"@TestBean(scope = CLASS) method must be static: " + describe(m));
				modeTracker.observe(ann.mode(), describe(m));
				register(store, m, null, ann);
			}
		}
	}

	/**
	 * Walks the test-instance class hierarchy and registers every {@code @TestBean(scope = METHOD)} declared on a
	 * field or method.
	 *
	 * <p>
	 * Honors both instance and static members ("behave consistently with
	 * non-static" for {@code scope = METHOD} on static fields).
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for hierarchical test bean instance override wiring
	})
	private static void populateInstanceOverrides(TestBeanStore store, Object testInstance, ModeTracker modeTracker) {
		var testClass = testInstance.getClass();
		for (var c : classHierarchy(testClass)) {
			for (var f : c.getDeclaredFields()) {
				var ann = f.getAnnotation(TestBean.class);
				if (ann == null || ann.scope() != Scope.METHOD)
					continue;
				modeTracker.observe(ann.mode(), describe(f));
				register(store, f, Modifier.isStatic(f.getModifiers()) ? null : testInstance, ann);
			}
			for (var m : c.getDeclaredMethods()) {
				var ann = m.getAnnotation(TestBean.class);
				if (ann == null || ann.scope() != Scope.METHOD)
					continue;
				modeTracker.observe(ann.mode(), describe(m));
				register(store, m, Modifier.isStatic(m.getModifiers()) ? null : testInstance, ann);
			}
		}
	}

	/** Carries the discovered overlay plus the resolved {@link Mode} for a single scope. */
	record ScopedStore(TestBeanStore store, Mode mode, boolean empty) { /* no body */ }

	/**
	 * Tracks the {@link Mode} of {@code @TestBean} declarations encountered within a single scope and rejects
	 * mixed-mode declarations.
	 */
	private static final class ModeTracker {
		private final Scope scope;
		private Mode observed;
		private String firstMember;
		private boolean empty = true;

		ModeTracker(Scope scope) {
			this.scope = scope;
		}

		void observe(Mode mode, String memberDesc) {
			empty = false;
			if (observed == null) {
				observed = mode;
				firstMember = memberDesc;
				return;
			}
			if (observed != mode)
				throw new IllegalStateException(
					"Mixed @TestBean modes in " + scope + " scope: " + firstMember + " declares Mode." + observed
					+ " but " + memberDesc + " declares Mode." + mode
					+ ".  All @TestBean declarations in a single scope must use the same mode.");
		}

		Mode resolve() {
			return observed == null ? Mode.INJECT : observed;
		}

		boolean empty() {
			return empty;
		}
	}

	private static void register(TestBeanStore store, Field field, Object instance, TestBean ann) {
		Object value;
		try {
			field.setAccessible(true);
			value = field.get(instance);
		} catch (IllegalAccessException e) {
			throw new ExtensionContextException("Could not read @TestBean field: " + describe(field), e);
		}
		var type = ann.type() == Object.class ? field.getType() : ann.type();
		registerValue(store, type, value, ann.name(), describe(field));
	}

	private static void register(TestBeanStore store, Method method, Object instance, TestBean ann) {
		if (method.getParameterCount() != 0)
			throw new ExtensionContextException(
				"@TestBean factory method must be parameterless: " + describe(method));
		Object value;
		try {
			method.setAccessible(true);
			value = method.invoke(instance);
		} catch (ReflectiveOperationException e) {
			throw new ExtensionContextException(
				"Failed to invoke @TestBean factory method: " + describe(method),
				e.getCause() != null ? e.getCause() : e);
		}
		var type = ann.type() == Object.class ? method.getReturnType() : ann.type();
		registerValue(store, type, value, ann.name(), describe(method));
	}

	@SuppressWarnings({
		"unchecked" // The type token came from either the annotated member's declared type or ann.type(); the value's runtime class is compatible by construction.
	})
	private static void registerValue(TestBeanStore store, Class<?> type, Object value, String name, String memberDesc) {
		if (value != null && !type.isInstance(value))
			throw new ExtensionContextException(
				"@TestBean value is not assignable to declared type " + type.getName() + ": " + memberDesc);
		store.override((Class<Object>) type, value, name.isEmpty() ? null : name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Internal helpers
	//-----------------------------------------------------------------------------------------------------------------

	private TestBeanStore currentMethodStore() {
		return currentMethodContext == null ? null : (TestBeanStore) currentMethodContext.getStore(NAMESPACE).get(KEY_METHOD_STORE);
	}

	private TestBeanStore currentClassStore() {
		return currentClassContext == null ? null : readClassStore(currentClassContext);
	}

	/**
	 * Reads the class-scope store from the nearest enclosing context that owns one.
	 *
	 * <p>
	 * JUnit's {@code ExtensionContext.Store} keys are scoped per context.  When a method-scope callback fires, the
	 * incoming context is the method-level context whose own store does <i>not</i> contain the class-scope entry;
	 * the class-scope entry lives on the parent (class-level) context.  This helper walks parents until it finds
	 * one whose store contains the class-scope entry, or returns {@code null} if none does.
	 */
	private static TestBeanStore readClassStore(ExtensionContext context) {
		for (var c = context; c != null; c = c.getParent().orElse(null)) {
			var s = (TestBeanStore) c.getStore(NAMESPACE).get(KEY_CLASS_STORE);
			if (s != null)
				return s;
		}
		return null;
	}

	private static List<Class<?>> classHierarchy(Class<?> testClass) {
		// Walk parent-to-child so a subclass's @TestBean of the same (type, name) wins over the parent's via the
		// "last write wins" semantics of BasicBeanStore.addBean(...).
		var hierarchy = new ArrayList<Class<?>>();
		for (var c = testClass; c != null && c != Object.class; c = c.getSuperclass())
			hierarchy.add(c);
		Collections.reverse(hierarchy);
		return hierarchy;
	}

	private static String describe(Field f) {
		return f.getDeclaringClass().getName() + "#" + f.getName();
	}

	private static String describe(Method m) {
		return m.getDeclaringClass().getName() + "#" + m.getName() + "()";
	}
}
