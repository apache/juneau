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

import java.util.*;

import org.apache.juneau.commons.settings.*;
import org.springframework.context.*;
import org.springframework.context.event.*;

/**
 * Process-wide registry that gives each Spring {@link ApplicationContext} exactly one
 * {@link SpringEnvironmentPropertySource} in the {@link Settings} singleton, however many {@link SpringBeanStore}s are
 * built on it.
 *
 * <p>
 * Invariant: a context has an entry here if and only if its bridge is in {@code Settings}; both sides change inside
 * the same {@link #LOCK} section.  Strong keys add no leak: {@code Settings} already holds the bridge, and the bridge's
 * supplier holds the context.
 *
 * <p>
 * A bridge is removed when the last store that acquired it is cleared or, for a {@link ConfigurableApplicationContext}
 * that was not yet closed at the first acquire, when the context publishes its {@link ContextClosedEvent}.
 */
final class SpringEnvironmentBridges {

	private static final Object LOCK = new Object();

	// Identity keys: contexts may override equals/hashCode, and mocked contexts must see no calls.
	private static final Map<ApplicationContext,Registration> REGISTRATIONS = new IdentityHashMap<>();

	private SpringEnvironmentBridges() {}

	/** One context's bridge, its reference count and its close listener. */
	static final class Registration {
		final ApplicationContext context;
		final SpringEnvironmentPropertySource source;
		int refs;  // Guarded by LOCK.
		CloseListener listener;  // Guarded by LOCK; null when the context cannot publish a close event.

		Registration(ApplicationContext context) {
			this.context = context;
			this.source = new SpringEnvironmentPropertySource(context::getEnvironment);
		}
	}

	/** Removes its registration when its own context closes. */
	static final class CloseListener implements ApplicationListener<ApplicationEvent> {
		private final Registration registration;

		CloseListener(Registration registration) {
			this.registration = registration;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			// Spring republishes a child context's events to its parent, so match on the event's own context.
			if (event instanceof ContextClosedEvent e && e.getApplicationContext() == registration.context)
				forceRemove(registration);
		}
	}

	/**
	 * Returns the context's registration, creating it (and adding its bridge to {@code Settings}) on first use.
	 *
	 * @param context The application context.  Must not be <jk>null</jk>.
	 * @return The registration, with its reference count incremented.
	 */
	static Registration acquire(ApplicationContext context) {
		synchronized (LOCK) {
			var r = REGISTRATIONS.get(context);
			if (r == null) {
				r = new Registration(context);
				Settings.get().addSource(r.source);
				REGISTRATIONS.put(context, r);
				if (context instanceof ConfigurableApplicationContext c && ! c.isClosed()) {
					r.listener = new CloseListener(r);
					try {
						c.addApplicationListener(r.listener);
					} catch (RuntimeException e) {
						remove(r);
						throw e;
					}
					// The context may have closed (and published its event) between the isClosed() check and the add.
					if (c.isClosed())
						remove(r);
				}
			}
			r.refs++;
			return r;
		}
	}

	/**
	 * Releases one reference; the last one removes the bridge.  A no-op if the registration was already
	 * force-removed (and possibly replaced by a newer one for the same context).
	 *
	 * @param r The registration returned by {@link #acquire(ApplicationContext)}.
	 */
	static void release(Registration r) {
		synchronized (LOCK) {
			if (REGISTRATIONS.get(r.context) != r)
				return;
			if (--r.refs == 0)
				remove(r);
		}
	}

	/**
	 * Removes the registration whatever its reference count (the context closed).
	 *
	 * @param r The registration.
	 */
	static void forceRemove(Registration r) {
		synchronized (LOCK) {
			if (REGISTRATIONS.get(r.context) == r)
				remove(r);
		}
	}

	private static void remove(Registration r) {
		REGISTRATIONS.remove(r.context);
		Settings.get().removeSource(r.source);
		if (r.listener != null && r.context instanceof ConfigurableApplicationContext c)
			c.removeApplicationListener(r.listener);
	}

	/**
	 * Clears the miss cache of the context's bridge.
	 *
	 * @param context The application context.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the context had a registered bridge.
	 */
	static boolean invalidate(ApplicationContext context) {
		Registration r;
		synchronized (LOCK) {
			r = context == null ? null : REGISTRATIONS.get(context);
		}
		if (r == null)
			return false;
		r.source.invalidate();
		return true;
	}

	/** @return The number of registered contexts (tests). */
	static int size() {
		synchronized (LOCK) {
			return REGISTRATIONS.size();
		}
	}

	/**
	 * @param context The application context.
	 * @return <jk>true</jk> if the context has a registered bridge (tests).
	 */
	static boolean isRegistered(ApplicationContext context) {
		synchronized (LOCK) {
			return REGISTRATIONS.containsKey(context);
		}
	}
}
