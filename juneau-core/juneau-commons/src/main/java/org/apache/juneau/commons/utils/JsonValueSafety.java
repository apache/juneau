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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Bounded, iterative walker that validates already-materialized JSON values against structural safety limits.
 *
 * <p>
 * This helper defends against pathological inputs (excessive nesting, unbounded node counts, cyclic references, and
 * runaway traversal times) before a value is serialized or otherwise consumed.  It walks only in-memory maps,
 * collections, arrays, and scalar leaves; it never resolves <js>"$ref"</js> or performs any I/O.
 *
 * <p>
 * All violations are reported as neutral {@link IllegalArgumentException}s.  Typed or adaptor callers are responsible
 * for mapping these to the appropriate JSON-RPC error code, which keeps this core class free of any revision-specific
 * error policy.
 */
public final class JsonValueSafety {

	/** Maximum allowed nesting depth of the walked value. */
	public static final int MAX_DEPTH = 64;

	/** Maximum allowed number of distinct nodes (containers and leaves) in the walked value. */
	public static final int MAX_NODES = 10_000;

	/** Maximum allowed wall-clock time, in milliseconds, for a single traversal. */
	public static final long MAX_TRAVERSAL_MILLIS = 100;

	private JsonValueSafety() {}

	/**
	 * Computes a fresh traversal deadline from the current time.
	 *
	 * @return An absolute deadline in {@link System#nanoTime()} units, {@link #MAX_TRAVERSAL_MILLIS} milliseconds in the future.
	 */
	public static long deadlineNanos() {
		return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(MAX_TRAVERSAL_MILLIS);
	}

	/**
	 * Validates the specified value against the structural safety limits using a fresh {@link #deadlineNanos() deadline}.
	 *
	 * @param value The value to walk.  Can be <jk>null</jk>, in which case this method returns immediately.
	 * @param label A human-readable label describing the value; used as a prefix in violation messages.
	 * @throws IllegalArgumentException If the value violates any of the depth, node-count, traversal-time, or JSON-shape constraints.
	 */
	public static void check(Object value, String label) {
		check(value, label, deadlineNanos());
	}

	/**
	 * Validates the specified value against the structural safety limits using the specified traversal deadline.
	 *
	 * @param root The value to walk.  Can be <jk>null</jk>, in which case this method returns immediately.
	 * @param label A human-readable label describing the value; used as a prefix in violation messages.
	 * @param deadlineNanos An absolute deadline in {@link System#nanoTime()} units after which the traversal is aborted.
	 * @throws IllegalArgumentException
	 * 	If the value exceeds {@link #MAX_DEPTH}, exceeds {@link #MAX_NODES}, exceeds the traversal deadline, contains a
	 * 	non-string JSON object key, or contains a leaf that is not a JSON value type ({@link String}, {@link Number}, or
	 * 	{@link Boolean}).
	 */
	public static void check(Object root, String label, long deadlineNanos) {
		if (root == null)
			return;
		var seen = Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
		var stack = new ArrayDeque<Framed>();
		stack.push(new Framed(root, 1));
		var nodes = 0;
		while (! stack.isEmpty()) {
			// Bounded per-iteration work runs before the deadline check so a structurally-oversized input
			// deterministically reports its depth/node-count violation instead of racing the wall clock.
			var frame = stack.pop();
			if (frame.depth() > MAX_DEPTH)
				throw iaex("%s exceeds maximum nesting depth of %s", label, MAX_DEPTH);
			nodes = visitFrame(frame, label, seen, stack, nodes);
			if (System.nanoTime() >= deadlineNanos)
				throw iaex("%s traversal exceeded %s ms", label, MAX_TRAVERSAL_MILLIS);
		}
	}

	/**
	 * Processes one popped stack frame: counts it toward the node budget, and - if it's an unseen container -
	 * pushes its children onto {@code stack} for further traversal.
	 *
	 * @return The updated node count.
	 */
	private static int visitFrame(Framed frame, String label, Set<Object> seen, ArrayDeque<Framed> stack, int nodes) {
		var value = frame.value();
		if (value == null)
			return checkNodeBudget(nodes, label);
		var container = value instanceof Map<?,?> || value instanceof Collection<?> || value.getClass().isArray();
		if (container && ! seen.add(value))
			return nodes;
		nodes = checkNodeBudget(nodes, label);
		pushChildren(value, frame.depth(), label, stack);
		return nodes;
	}

	private static int checkNodeBudget(int nodes, String label) {
		if (++nodes > MAX_NODES)
			throw iaex("%s exceeds maximum node count of %s", label, MAX_NODES);
		return nodes;
	}

	private static void pushChildren(Object value, int depth, String label, ArrayDeque<Framed> stack) {
		if (value instanceof Map<?,?> value2) {
			for (var entry : value2.entrySet()) {
				if (! (entry.getKey() instanceof String))
					throw iaex("%s contains non-string JSON object key %s", label, entry.getKey());
				stack.push(new Framed(entry.getValue(), depth + 1));
			}
		} else if (value instanceof Collection<?> value2) {
			for (var child : value2)
				stack.push(new Framed(child, depth + 1));
		} else if (value.getClass().isArray()) {
			for (var i = 0; i < Array.getLength(value); i++)
				stack.push(new Framed(Array.get(value, i), depth + 1));
		} else if (! (value instanceof String || value instanceof Number || value instanceof Boolean))
			throw iaex("%s contains non-JSON value type %s", label, value.getClass().getName());
	}

	/**
	 * Computes the number of nanoseconds remaining before the specified deadline.
	 *
	 * @param deadlineNanos An absolute deadline in {@link System#nanoTime()} units.
	 * @return The remaining nanoseconds, or <c>0</c> if the deadline has already passed.
	 */
	public static long remainingNanos(long deadlineNanos) {
		return Math.max(0, deadlineNanos - System.nanoTime());
	}

	private record Framed(Object value, int depth) {}
}
