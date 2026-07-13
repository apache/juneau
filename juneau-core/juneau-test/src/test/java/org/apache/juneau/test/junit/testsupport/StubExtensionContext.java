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
package org.apache.juneau.test.junit.testsupport;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.*;

/**
 * Minimal stub {@link ExtensionContext} used by extension-side unit tests that need to drive lifecycle
 * callbacks directly without spinning up a full JUnit Platform run.
 *
 * <p>
 * Implemented via {@link Proxy} so we don't have to match every method of the upstream
 * {@code ExtensionContext} / {@code Store} interface as JUnit evolves.  The proxy services the small
 * surface our extension actually touches ({@code getRequiredTestClass()}, {@code getRequiredTestInstance()},
 * {@code getStore(Namespace)}, {@code getParent()}) and throws {@link UnsupportedOperationException} for
 * everything else.
 */
public final class StubExtensionContext {

	private StubExtensionContext() { /* static factory only */ }

	/**
	 * Builds a stub {@link ExtensionContext}.
	 *
	 * @param testClass The test class to report from {@code getTestClass()} / {@code getRequiredTestClass()}.
	 * @param testInstance The test instance to report from {@code getTestInstance()} /
	 *                     {@code getRequiredTestInstance()}.  Can be {@code null} (then
	 *                     {@code getRequiredTestInstance()} will throw, mirroring JUnit).
	 * @return A new stub context.
	 */
	public static ExtensionContext of(Class<?> testClass, Object testInstance) {
		return of(testClass, testInstance, null);
	}

	/**
	 * Builds a stub {@link ExtensionContext} with an explicit parent for {@code getParent()} chains.
	 *
	 * @param testClass The test class.
	 * @param testInstance The test instance.  Can be {@code null}.
	 * @param parent The parent context, or {@code null}.
	 * @return A new stub context.
	 */
	public static ExtensionContext of(Class<?> testClass, Object testInstance, ExtensionContext parent) {
		var state = new State(testClass, testInstance, parent);
		return (ExtensionContext) Proxy.newProxyInstance(
			ExtensionContext.class.getClassLoader(),
			new Class<?>[]{ExtensionContext.class},
			new ContextHandler(state));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Internals
	//-----------------------------------------------------------------------------------------------------------------

	private static final class State {
		final Class<?> testClass;
		final Object testInstance;
		final ExtensionContext parent;
		final Map<ExtensionContext.Namespace, Map<Object, Object>> namespaces = new ConcurrentHashMap<>();

		State(Class<?> testClass, Object testInstance, ExtensionContext parent) {
			this.testClass = testClass;
			this.testInstance = testInstance;
			this.parent = parent;
		}
	}

	private static final class ContextHandler implements InvocationHandler {
		private final State state;

		ContextHandler(State state) { this.state = state; }

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			var name = method.getName();
			return switch (name) {
				case "getParent" -> o(state.parent);
				case "getRoot" -> state.parent == null ? proxy : state.parent.getRoot();
				case "getUniqueId" -> "stub:" + (state.testClass == null ? "?" : state.testClass.getName());
				case "getDisplayName" -> "stub:" + (state.testClass == null ? "?" : state.testClass.getName());
				case "getTags" -> Set.of();
				case "getElement" -> o((Object) state.testClass);
				case "getTestClass" -> o((Object) state.testClass);
				case "getRequiredTestClass" -> Objects.requireNonNull(state.testClass, "no test class");
				case "getEnclosingTestClasses" -> List.of();
				case "getTestInstanceLifecycle" -> oe();
				case "getTestInstance" -> o(state.testInstance);
				case "getRequiredTestInstance" -> Objects.requireNonNull(state.testInstance, "no test instance");
				case "getTestInstances" -> oe();
				case "getRequiredTestInstances" -> throw new UnsupportedOperationException("getRequiredTestInstances");
				case "getTestMethod" -> oe();
				case "getRequiredTestMethod" -> throw new UnsupportedOperationException("getRequiredTestMethod");
				case "getExecutionException" -> oe();
				case "getConfigurationParameter" -> oe();
				case "publishReportEntry", "publishFile", "publishDirectory" -> null;
				case "getStore" -> storeFor(args);
				case "getExecutionMode" -> throw new UnsupportedOperationException("getExecutionMode");
				case "getExecutableInvoker" -> throw new UnsupportedOperationException("getExecutableInvoker");
				case "equals" -> proxy == args[0];
				case "hashCode" -> System.identityHashCode(proxy);
				case "toString" -> "StubExtensionContext[" + (state.testClass == null ? "?" : state.testClass.getName()) + "]";
				default -> throw new UnsupportedOperationException("StubExtensionContext does not support " + name);
			};
		}

		private Store storeFor(Object[] args) {
			// Last argument is always the Namespace (either getStore(Namespace) or getStore(StoreScope, Namespace)).
			var ns = (ExtensionContext.Namespace) args[args.length - 1];
			var backing = state.namespaces.computeIfAbsent(ns, k -> new ConcurrentHashMap<>());
			return makeStore(backing);
		}

		private static Store makeStore(Map<Object, Object> backing) {
			return (Store) Proxy.newProxyInstance(
				Store.class.getClassLoader(),
				new Class<?>[]{Store.class},
				new StoreHandler(backing));
		}
	}

	private static final class StoreHandler implements InvocationHandler {
		private final Map<Object, Object> backing;

		StoreHandler(Map<Object, Object> backing) { this.backing = backing; }

		@Override
		@SuppressWarnings({
			"unchecked", // proxy-level dispatch — types are validated by JUnit's interface contract
			"rawtypes"
		})
		public Object invoke(Object proxy, Method method, Object[] args) {
			var name = method.getName();
			switch (name) {
				case "get":
					if (args.length == 1) return backing.get(args[0]);
					return ((Class<?>) args[1]).cast(backing.get(args[0]));
				case "put":
					backing.put(args[0], args[1]);
					return null;
				case "remove":
					if (args.length == 1) return backing.remove(args[0]);
					return ((Class<?>) args[1]).cast(backing.remove(args[0]));
				case "getOrDefault":
					var existing = backing.get(args[0]);
					return existing != null ? ((Class<?>) args[1]).cast(existing) : args[2];
				case "getOrComputeIfAbsent", "computeIfAbsent":
					if (args.length == 1) {
						var typeKey = (Class<?>) args[0];
						return typeKey.cast(backing.computeIfAbsent(typeKey, k -> instantiate(typeKey)));
					}
					if (args.length == 2) {
						var fn = (Function) args[1];
						return backing.computeIfAbsent(args[0], fn::apply);
					}
					var fn2 = (Function) args[1];
					var typed = backing.computeIfAbsent(args[0], fn2::apply);
					return ((Class<?>) args[2]).cast(typed);
				case "equals":
					return proxy == args[0];
				case "hashCode":
					return System.identityHashCode(proxy);
				case "toString":
					return "StubStore[" + backing.size() + "]";
				default:
					throw new UnsupportedOperationException("StubStore does not support " + name);
			}
		}

		private static Object instantiate(Class<?> type) {
			try {
				return type.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Could not instantiate " + type, e);
			}
		}
	}
}
