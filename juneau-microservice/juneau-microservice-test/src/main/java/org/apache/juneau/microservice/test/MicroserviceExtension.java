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
package org.apache.juneau.microservice.test;

import java.lang.reflect.*;
import java.net.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.test.junit.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit 5 extension behind {@link MicroserviceTest @MicroserviceTest}: boots a whole
 * {@link Microservice} (config + lifecycle + embedded Jetty on an ephemeral port) for the test class, with
 * {@link org.apache.juneau.test.junit.TestBean @TestBean} mock-bean injection and convenience parameter resolution.
 *
 * <h5 class='topic'>Lifecycle (per class)</h5>
 *
 * <p>
 * The microservice is built + {@link Microservice#start() started} once in {@code beforeAll} and
 * {@link Microservice#stop() stopped} in {@code afterAll}. A fresh instance is booted per class and stopped
 * cleanly &mdash; never reused, since {@code Microservice} restart is unsupported (the {@code stopped} flag is
 * one-way, the bean store is closed, and each {@code start()} adds a JVM shutdown hook). The bound port is read
 * from the live {@link ServerConnector} (never hard-coded), mirroring the established fixture pattern.
 *
 * <h5 class='topic'>Mock-bean injection</h5>
 *
 * <p>
 * {@link org.apache.juneau.test.junit.TestBean @TestBean} declarations are discovered via
 * {@link JuneauBeanStoreExtension#discoverOverrides(Object)}. <b>Mode INJECT</b> (the default) installs the
 * overrides via {@code Microservice.Builder.overridingBeanStore(...)} <i>before</i> boot, so the service reads
 * them from startup. <b>Mode OVERLAY</b> pushes the overrides onto the booted instance's bean store for the
 * class duration and pops them in teardown.
 *
 * <h5 class='topic'>Parameter resolution</h5>
 *
 * <p>
 * Resolves {@link RestClient} (bound to the booted root URL), {@link Microservice}, {@link WritableBeanStore},
 * and the bound port ({@code int} / {@code Integer}) on test + lifecycle method parameters.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource"
})
public class MicroserviceExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

	/** Default name of the optional {@code static Microservice.Builder} supplier method on the test class. */
	public static final String BUILDER_SUPPLIER_METHOD = "microserviceBuilder";

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MicroserviceExtension.class);
	private static final String KEY_STATE = "state";

	/** Per-class booted state. */
	private static final class State {
		Microservice microservice;
		RestClient client;
		URI rootUrl;
		WritableBeanStore beanStore;
		Snapshot overlaySnapshot;   // non-null when Mode OVERLAY was pushed
	}

	@Override /* BeforeAllCallback */
	public void beforeAll(ExtensionContext context) throws Exception {
		var testClass = context.getRequiredTestClass();
		var ann = findAnnotation(testClass);

		// Discover @TestBean overrides from the test class (static members participate at class-boot time).
		var instance = instantiateForDiscovery(testClass);
		var overrides = JuneauBeanStoreExtension.discoverOverrides(instance);

		var builder = resolveBuilder(testClass, ann);

		// The user's configurations come first (so @Bean Servlet methods are visible), then the ephemeral Jetty
		// server (port 0), then JettyConfiguration wires the lifecycle. BeanStore returns the first registered
		// match, so a user-supplied @Bean Server still wins.
		builder.configurations(ann.configurations());
		builder.configurations(EphemeralJettyServerConfig.class, JettyConfiguration.class);

		var injecting = ! overrides.isEmpty() && overrides.mode() == Mode.INJECT;
		if (injecting)
			builder.overridingBeanStore(overrides.store());

		var state = new State();
		state.microservice = builder.build();
		state.microservice.start();
		state.beanStore = state.microservice.getBeanStore();
		state.rootUrl = resolveRootUrl(state.microservice);
		state.client = RestClient.builder().rootUrl(state.rootUrl.toString()).build();

		// Mode OVERLAY: push the overrides onto the booted instance's bean store for the class duration.
		if (! overrides.isEmpty() && overrides.mode() == Mode.OVERLAY)
			state.overlaySnapshot = state.beanStore.pushOverlay(overrides.store());

		context.getStore(NAMESPACE).put(KEY_STATE, state);
	}

	@Override /* AfterAllCallback */
	public void afterAll(ExtensionContext context) {
		var state = (State) context.getStore(NAMESPACE).remove(KEY_STATE);
		if (state == null)
			return;
		try {
			if (state.overlaySnapshot != null)
				state.beanStore.popOverlay(state.overlaySnapshot);
		} finally {
			try {
				if (state.client != null)
					state.client.close();
			} catch (Exception e) {  // HTT: RestClient.close() failure is not reproducible against the in-process JDK transport.
				// Best-effort close; never mask teardown.
			} finally {
				if (state.microservice != null)
					safeStop(state.microservice);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ParameterResolver
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ParameterResolver */
	public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
		var t = pc.getParameter().getType();
		return t == RestClient.class || t == Microservice.class || t == WritableBeanStore.class
			|| t == int.class || t == Integer.class;
	}

	@Override /* ParameterResolver */
	public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
		var state = readState(ec);
		if (state == null)
			throw new ParameterResolutionException("@MicroserviceTest has no booted microservice to resolve from.");
		var t = pc.getParameter().getType();
		if (t == RestClient.class)
			return state.client;
		if (t == Microservice.class)
			return state.microservice;
		if (t == WritableBeanStore.class)
			return state.beanStore;
		if (t == int.class || t == Integer.class)
			return state.rootUrl.getPort();
		throw new ParameterResolutionException("Unsupported @MicroserviceTest parameter type: " + t.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static MicroserviceTest findAnnotation(Class<?> testClass) {
		for (var c = testClass; c != null; c = c.getSuperclass()) {
			var a = c.getAnnotation(MicroserviceTest.class);
			if (a != null)
				return a;
		}
		throw new ExtensionContextException("@MicroserviceTest annotation not found on " + testClass.getName());
	}

	@SuppressWarnings("java:S3011") // setAccessible required: builder supplier method may be package-private or private by test-class convention.
	private static Microservice.Builder resolveBuilder(Class<?> testClass, MicroserviceTest ann) {
		var m = findBuilderSupplier(testClass, ann.builderMethod());
		if (m == null)
			return Microservice.create();
		try {
			m.setAccessible(true);
			var result = m.invoke(null);
			if (! (result instanceof Microservice.Builder b))
				throw new ExtensionContextException(
					"@MicroserviceTest builder method '" + ann.builderMethod() + "' on " + testClass.getName()
					+ " must return a Microservice.Builder.");
			return b;
		} catch (ReflectiveOperationException e) {
			throw new ExtensionContextException(
				"Failed to invoke @MicroserviceTest builder method '" + ann.builderMethod() + "' on " + testClass.getName(), e);
		}
	}

	private static Method findBuilderSupplier(Class<?> testClass, String name) {
		for (var c = testClass; c != null; c = c.getSuperclass()) {
			try {
				var m = c.getDeclaredMethod(name);
				if (Modifier.isStatic(m.getModifiers()))
					return m;
				throw new ExtensionContextException(
					"@MicroserviceTest builder method '" + name + "' on " + c.getName() + " must be static.");
			} catch (NoSuchMethodException e) {
				// Try the superclass.
			}
		}
		return null;
	}

	/**
	 * Instantiates the test class via its no-arg constructor purely to drive {@code @TestBean} discovery (which
	 * reads instance + static members). Falls back to a hierarchy-static-only scan if no usable no-arg ctor exists.
	 */
	@SuppressWarnings("java:S3011") // setAccessible required: test class no-arg constructor may be package-private for JUnit lifecycle reasons.
	private static Object instantiateForDiscovery(Class<?> testClass) {
		try {
			var ctor = testClass.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		} catch (ReflectiveOperationException e) {
			// No accessible no-arg ctor (e.g. @TestInstance(PER_CLASS) with constructor injection). Static-only
			// @TestBean discovery still works via a throwaway minimal instance of Object's identity is impossible,
			// so re-raise with guidance.
			throw new ExtensionContextException(
				"@MicroserviceTest requires a no-arg constructor on " + testClass.getName()
				+ " to discover @TestBean overrides before boot.", e);
		}
	}

	private static URI resolveRootUrl(Microservice microservice) {
		var component = microservice.getBeanStore().getBean(JettyServerComponent.class).orElseThrow(
			() -> new ExtensionContextException("@MicroserviceTest could not find a JettyServerComponent after start()."));
		var server = component.getServer();
		var localPort = -1;
		for (var c : server.getConnectors()) {
			if (c instanceof ServerConnector sc) {
				localPort = sc.getLocalPort();
				break;
			}
		}
		if (localPort <= 0)
			throw new ExtensionContextException("@MicroserviceTest could not determine the bound ServerConnector port after start().");
		return URI.create("http://localhost:" + localPort);
	}

	private static void safeStop(Microservice microservice) {
		try {
			microservice.stop();
		} catch (Exception e) {  // HTT: Microservice.stop() failure mid-teardown is not reproducible in the happy-path test boot.
			throw new ExtensionContextException("@MicroserviceTest failed to stop the microservice.", e);
		}
	}

	private static State readState(ExtensionContext context) {
		for (var c = context; c != null; c = c.getParent().orElse(null)) {
			var s = (State) c.getStore(NAMESPACE).get(KEY_STATE);
			if (s != null)
				return s;
		}
		return null;
	}
}
