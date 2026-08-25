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
package org.apache.juneau.rest.server.springboot;

import java.lang.reflect.*;
import java.util.*;

import org.slf4j.*;
import org.springframework.beans.factory.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;

/**
 * Spring Boot logging bridge auto-configuration.
 *
 * <p>
 * When enabled (default), and only when Logback is the active SLF4J backend, installs one JUL
 * {@code LevelChangePropagator} listener so Spring logging-level changes propagate to JUL.
 *
 * <p>
 * This is additive by default ({@code juneau.rest.logging.propagate-levels.reset-jul=false}) and
 * silently no-ops when the backend is not Logback.
 *
 * @since 10.0.0
 */
@AutoConfiguration
@ConditionalOnClass(name="ch.qos.logback.classic.jul.LevelChangePropagator")
@ConditionalOnProperty(name="juneau.rest.logging.propagate-levels", havingValue="true", matchIfMissing=true)
public class JuneauRestLoggingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name="juneauRestLogLevelPropagatorInstaller")
	public Runnable juneauRestLogLevelPropagatorInstaller(Environment env) {
		var resetJul = env.getProperty("juneau.rest.logging.propagate-levels.reset-jul", Boolean.class, false);
		return () -> install(LoggerFactory.getILoggerFactory(), resetJul);
	}

	@Bean
	@ConditionalOnBean(name="juneauRestLogLevelPropagatorInstaller")
	public SmartInitializingSingleton juneauRestLogLevelPropagatorBootstrap(Runnable juneauRestLogLevelPropagatorInstaller) {
		return juneauRestLogLevelPropagatorInstaller::run;
	}

	void install(ILoggerFactory loggerFactory, boolean resetJul) {
		var loggerContextClass = loadClass("ch.qos.logback.classic.LoggerContext");
		if (loggerContextClass == null || ! loggerContextClass.isInstance(loggerFactory))
			return;

		var context = loggerContextClass.cast(loggerFactory);
		if (hasPropagator(context))
			return;

		var propagator = newPropagator();
		if (propagator == null)
			return;

		invoke(propagator, "setContext", context);
		invoke(propagator, "setResetJUL", new Class<?>[] { boolean.class }, resetJul);
		invokeIfPresent(propagator, "start");
		invoke(context, "addListener", new Class<?>[] { loadClass("ch.qos.logback.classic.spi.LoggerContextListener") }, propagator);
	}

	private static boolean hasPropagator(Object context) {
		var levelChangePropagatorClass = loadClass("ch.qos.logback.classic.jul.LevelChangePropagator");
		if (levelChangePropagatorClass == null)
			return false;
		for (var listener : listeners(context))
			if (levelChangePropagatorClass.isInstance(listener))
				return true;
		return false;
	}

	@SuppressWarnings({
		"unchecked" // Logback context listener list returns raw listener type at runtime.
	})
	private static List<Object> listeners(Object context) {
		try {
			var out = invoke(context, "getCopyOfListenerList", new Class<?>[0]);
			if (out instanceof List<?> out2)
				return (List<Object>)out2;
		} catch (Exception e) {
			// Fall through to field-based lookup used by older Logback versions.
		}
		for (var c = context.getClass(); c != null; c = c.getSuperclass()) {
			for (var f : c.getDeclaredFields()) {
				if (! List.class.isAssignableFrom(f.getType()) || ! f.getName().toLowerCase(Locale.ROOT).contains("listener"))
					continue;
				try {
					forceAccessible(f);
					var out = f.get(context);
					if (out instanceof List<?> out2)
						return (List<Object>)out2;
				} catch (Exception e) {
					// Try next field.
				}
			}
		}
		return List.of();
	}

	@SuppressWarnings({
		"java:S3011" // Reflective fallback for Logback versions predating getCopyOfListenerList(); the private listener field has no non-reflective accessor.
	})
	private static void forceAccessible(Field f) {
		f.setAccessible(true);
	}

	private static Object newPropagator() {
		try {
			return Class.forName(JuneauResetResistantLevelChangePropagator.class.getName()).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	private static Class<?> loadClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	private static Object invoke(Object target, String method, Class<?>[] parameterTypes, Object...args) {
		try {
			var m = target.getClass().getMethod(method, parameterTypes);
			return m.invoke(target, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object invoke(Object target, String method, Object arg) {
		try {
			for (var m : target.getClass().getMethods()) {
				if (! m.getName().equals(method) || m.getParameterCount() != 1)
					continue;
				if (m.getParameters()[0].getType().isAssignableFrom(arg.getClass()))
					return m.invoke(target, arg);
			}
			throw new NoSuchMethodException(method);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static void invokeIfPresent(Object target, String method) {
		try {
			for (var m : target.getClass().getMethods()) {
				if (m.getName().equals(method) && m.getParameterCount() == 0) {
					m.invoke(target);
					return;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
