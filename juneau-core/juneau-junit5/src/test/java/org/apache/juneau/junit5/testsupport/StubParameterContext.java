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
package org.apache.juneau.junit5.testsupport;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.junit5.*;
import org.junit.jupiter.api.extension.*;

/**
 * Minimal stub {@link ParameterContext} that returns a real {@link Parameter} of the supplied type.
 *
 * <p>
 * Used by extension tests that need to invoke {@code supportsParameter(...)} directly without running a
 * full JUnit lifecycle.  Only {@link #getParameter()} is non-trivial; other accessors return empty
 * defaults.
 *
 * <p>
 * Internally this class declares concrete sink methods whose parameter types are the ones the extension
 * test cases want to check ({@code TestBeanStore} and {@code String}).  We reflect on those methods to
 * obtain a real {@link Parameter} reference — {@link Parameter} has no public constructor and is
 * {@code final}, so synthesizing one any other way would require deep JDK internals.
 */
public final class StubParameterContext implements ParameterContext {

	private final Parameter parameter;

	/**
	 * Constructor.
	 *
	 * @param parameterType The desired parameter type.  Must be one of the supported sink types
	 *                      (see {@link Sinks}).
	 */
	public StubParameterContext(Class<?> parameterType) {
		this.parameter = lookupSink(parameterType);
	}

	private static Parameter lookupSink(Class<?> type) {
		for (var m : Sinks.class.getDeclaredMethods()) {
			if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == type)
				return m.getParameters()[0];
		}
		throw new IllegalArgumentException("StubParameterContext: no sink declared for type " + type.getName()
			+ " — add it to Sinks if needed.");
	}

	@Override public Parameter getParameter() { return parameter; }
	@Override public int getIndex() { return 0; }
	@Override public Optional<Object> getTarget() { return Optional.empty(); }
	@Override public boolean isAnnotated(Class<? extends Annotation> annotationType) { return false; }
	@Override public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType) { return Optional.empty(); }
	@Override public <A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType) { return List.of(); }

	/**
	 * Sink methods used as real {@link Parameter} sources.  Add a method here when a test needs a new type.
	 */
	@SuppressWarnings({
		"unused", // Sink methods are never invoked; they exist only so reflection can read their declared Parameter objects.
		"java:S1172" // The 'v' parameters are required so each sink declares a Parameter of the target type for reflection.
	})
	private static final class Sinks {
		static void acceptString(String v) { /* no-op */ }
		static void acceptTestBeanStore(TestBeanStore v) { /* no-op */ }
	}
}
