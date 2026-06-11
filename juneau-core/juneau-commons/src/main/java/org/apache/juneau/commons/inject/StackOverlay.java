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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Stack-aware {@link BeanStore} overlay.
 *
 * <p>
 * A {@code StackOverlay} delegates to a LIFO stack of {@link BeanStore} frames.  Each lookup walks the stack
 * from top to bottom and returns the first match.  When the stack is empty the lookup returns
 * {@link Optional#empty()} so that an outer chain (typically a {@link BasicBeanStore}'s {@code overridingParent}
 * slot) can fall through to its remaining tiers.
 *
 * <p>
 * This class is designed to live in the {@code overridingParent} slot of a {@link BasicBeanStore}.  Pushing a
 * frame inserts a new tier-1 layer; popping it removes that layer.  Stack order is enforced LIFO &mdash; popping
 * out of order or popping an empty stack throws {@link IllegalStateException}.
 *
 * <p>
 * The implementation uses a {@link ConcurrentLinkedDeque} so that reads from concurrent HTTP request threads
 * see a consistent view while pushes and pops are in flight.  Pushes/pops are expected to be infrequent (test
 * setup/teardown); reads are the hot path.
 *
 * <p>
 * <b>Composition, not inheritance.</b>  {@code StackOverlay} {@code implements} {@link BeanStore} rather than
 * extending {@link BasicBeanStore} so that it does <i>not</i> inherit the entries / defaults / parent storage
 * that a {@link BasicBeanStore} provides.  A {@code StackOverlay} is purely a stack of pluggable bean stores.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an overlay-capable bean store.</jc>
 * 	StackOverlay <jv>overlay</jv> = <jk>new</jk> StackOverlay();
 * 	BasicBeanStore <jv>store</jv> = <jk>new</jk> BasicBeanStore(<jk>null</jk>, <jv>overlay</jv>);
 *
 * 	<jc>// Push a per-test override frame.</jc>
 * 	<jv>overlay</jv>.push(<jk>new</jk> BasicBeanStore().addBean(MyService.<jk>class</jk>, <jv>mock</jv>));
 * 	<jv>store</jv>.getBean(MyService.<jk>class</jk>);  <jc>// returns the mock</jc>
 *
 * 	<jc>// Pop it at end of test.</jc>
 * 	<jv>overlay</jv>.pop();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicBeanStore} - The bean store this overlay sits in front of.
 * </ul>
 *
 * @since 10.0.0
 */
public final class StackOverlay implements BeanStore {

	private final Deque<BeanStore> frames = new ConcurrentLinkedDeque<>();

	/**
	 * Constructor.
	 */
	public StackOverlay() { /* no-op */ }

	/**
	 * Pushes a new overlay frame onto the top of the stack.
	 *
	 * <p>
	 * The frame becomes the highest-priority lookup target for subsequent {@link #getBean(Class)} /
	 * {@link #getBeanSupplier(Class)} calls until it is popped.
	 *
	 * @param overlay The bean store to push.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public StackOverlay push(BeanStore overlay) {
		assertArgNotNull("overlay", overlay);
		frames.push(overlay);
		return this;
	}

	/**
	 * Pops the top frame off the stack.
	 *
	 * @return The popped frame.
	 * @throws IllegalStateException If the stack is empty.
	 */
	public BeanStore pop() {
		var f = frames.pollFirst();
		if (f == null)
			throw new IllegalStateException("Cannot pop from an empty StackOverlay.");
		return f;
	}

	/**
	 * Returns the current depth of the stack.
	 *
	 * @return The number of frames currently on the stack.
	 */
	public int depth() {
		return frames.size();
	}

	/**
	 * Returns the top frame on the stack without removing it.
	 *
	 * @return The top frame, or <jk>null</jk> if the stack is empty.
	 */
	public BeanStore peek() {
		return frames.peekFirst();
	}

	@Override /* BeanStore */
	public <T> Optional<T> getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}

	@Override /* BeanStore */
	public <T> Optional<T> getBean(Class<T> beanType, String name) {
		for (var f : frames) {
			var b = f.getBean(beanType, name);
			if (b.isPresent())
				return b;
		}
		return opte();
	}

	@Override /* BeanStore */
	public <T> Map<String,T> getBeansOfType(Class<T> beanType) {
		// Walk the stack bottom-to-top so that higher (later-pushed) frames overwrite lower frames
		// for entries with the same name.  Iterating an iterator gives top-to-bottom; we reverse
		// that here by snapshotting and walking the reversed iterator.
		Map<String,T> result = new LinkedHashMap<>();
		var snapshot = new ArrayList<>(frames);
		for (var i = snapshot.size() - 1; i >= 0; i--)
			snapshot.get(i).getBeansOfType(beanType).forEach(result::put);
		return result;
	}

	@Override /* BeanStore */
	public boolean hasBean(Class<?> beanType) {
		return hasBean(beanType, null);
	}

	@Override /* BeanStore */
	public boolean hasBean(Class<?> beanType, String name) {
		for (var f : frames)
			if (f.hasBean(beanType, name))
				return true;
		return false;
	}

	@Override /* BeanStore */
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) {
		return getBeanSupplier(beanType, null);
	}

	@Override /* BeanStore */
	public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) {
		for (var f : frames) {
			var s = f.getBeanSupplier(beanType, name);
			if (s.isPresent())
				return s;
		}
		return opte();
	}

	@Override /* BeanStore */
	public <T> Optional<Class<? extends T>> getBeanType(Class<T> beanType) {
		for (var f : frames) {
			var t = f.getBeanType(beanType);
			if (t.isPresent())
				return t;
		}
		return opte();
	}

	@Override /* Overridden from Object */
	public String toString() {
		// Intentionally avoid recursing into frame.toString() — frames may back-reference the outer
		// BasicBeanStore that holds this overlay in its overridingParent slot, which would cause
		// stack overflows via BasicBeanStore.properties().  A flat representation (depth + identity
		// of each frame) is sufficient for diagnostics.
		var sb = new StringBuilder("StackOverlay{depth=").append(frames.size()).append(", frames=[");
		var first = true;
		for (var f : frames) {
			if (!first)
				sb.append(", ");
			first = false;
			sb.append(cns(f))
				.append('@')
				.append(Integer.toHexString(System.identityHashCode(f)));
		}
		return sb.append("]}").toString();
	}
}
