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
package org.apache.juneau.rest.server.debug;

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.debug.format.*;

import jakarta.servlet.http.*;

/**
 * Debug configuration with class/method targeted rules.
 */
public class DebugConfig {

	/** Represents no debug config. */
	public abstract class Void extends DebugConfig {
		Void(BeanStore beanStore) {
			super(beanStore);
		}
	}

	/**
	 * Builder class.
	 */
	public static class Builder {

		private final BeanStore beanStore;
		private final Map<String,DebugRule> rules = new LinkedHashMap<>();
		private Predicate<HttpServletRequest> conditional = x -> "true".equalsIgnoreCase(x.getHeader("Debug"));
		private DebugFormat defaultFormat;
		private Level defaultLevel = Level.INFO;
		private boolean defaultCacheBodies = false;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
			defaultFormat = beanStore.getBean(BasicTextFormat.class).orElseGet(BasicTextFormat::new);
		}

		/**
		 * Adds a rule.
		 *
		 * @param target The target key.
		 * @param value The rule spec.
		 * @return This object.
		 */
		public Builder rule(String target, Consumer<DebugRule.Builder> value) {
			var b = DebugRule.create();
			value.accept(b);
			rules.put(target, b.build());
			return this;
		}

		/**
		 * Sets default format.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder defaultFormat(DebugFormat value) {
			defaultFormat = value;
			return this;
		}

		/**
		 * Sets default level.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder defaultLevel(Level value) {
			defaultLevel = value;
			return this;
		}

		/**
		 * Sets conditional rule predicate.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder conditional(Predicate<HttpServletRequest> value) {
			conditional = value;
			return this;
		}

		/**
		 * Sets default cache-body flag.
		 *
		 * @param value The value.
		 * @return This object.
		 */
		public Builder defaultCacheBodies(boolean value) {
			defaultCacheBodies = value;
			return this;
		}

		/**
		 * Builds this object.
		 *
		 * @return A new object.
		 */
		public DebugConfig build() {
			return new DebugConfig(this);
		}
	}

	/**
	 * Creates a builder.
	 *
	 * @param beanStore The bean store.
	 * @return A new builder.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	private final BeanStore beanStore;
	private final Predicate<HttpServletRequest> conditional;
	private final DebugFormat defaultFormat;
	private final Level defaultLevel;
	private final boolean defaultCacheBodies;
	private final Map<String,DebugRule> rules;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store.
	 */
	public DebugConfig(BeanStore beanStore) {
		this(create(beanStore));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	protected DebugConfig(Builder builder) {
		beanStore = builder.beanStore;
		conditional = builder.conditional;
		defaultFormat = builder.defaultFormat;
		defaultLevel = builder.defaultLevel;
		defaultCacheBodies = builder.defaultCacheBodies;
		rules = Map.copyOf(builder.rules);
	}

	/**
	 * Resolves debug for resource-level requests.
	 *
	 * @param context The context.
	 * @param req The request.
	 * @return A debug result.
	 */
	public DebugResult resolve(RestContext context, HttpServletRequest req) {
		return resolveInternal(context == null ? null : context.getResourceClass(), null, req);
	}

	/**
	 * Resolves debug for operation-level requests.
	 *
	 * @param context The context.
	 * @param req The request.
	 * @return A debug result.
	 */
	public DebugResult resolve(RestOpContext context, HttpServletRequest req) {
		return resolveInternal(context.getContext().getResourceClass(), context.getJavaMethod(), req);
	}

	@SuppressWarnings({
		"unused" // Method and parameter are reflectively available to subclasses; kept for API stability.
	})
	private DebugResult resolveInternal(Class<?> resourceClass, Method method, HttpServletRequest req) {
		var enabled = false;
		if (req != null) {
			if (isTrue(cast(Boolean.class, req.getAttribute("Debug"))))
				enabled = true;
			else
				enabled = conditional.test(req);
		}
		var cacheBodies = defaultCacheBodies && enabled;
		return new DebugResult(enabled, defaultFormat, defaultLevel, cacheBodies);
	}

	/** Returns bean store. */
	protected BeanStore beanStore() { return beanStore; }

	/**
	 * Returns the configured rule for the specified target key, or <jk>null</jk> if none.
	 *
	 * @param target The target key.
	 * @return The configured rule, or <jk>null</jk>.
	 */
	public DebugRule getRuleFor(String target) {
		return rules.get(target);
	}
}
