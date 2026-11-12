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
package org.apache.juneau.commons.svl;

import java.util.function.*;

/**
 * Evaluation state for one {@link ExprEvaluator} run.
 *
 * <p>
 * A top-level context carries the render scope (the root-scope lookup function) and an optional <c>this</c> value.
 * {@link #element(Object)} derives a child context for one element of a <c>.?[...]</c> / <c>.![...]</c> iteration:
 * <c>this</c> is the element, the element's properties shadow the outer scope, and the element-scope depth is one
 * deeper (it counts toward {@code maxExprDepth}).  All contexts of one evaluation share one element {@link #budget}, so
 * {@code maxCollectionSize} bounds the total number of elements iterated per expression evaluation.
 *
 * <p>
 * Kept as a separate package-private class (rather than nested in {@link ExprEvaluator}) because the compiled
 * expression segment builds one per render.
 */
final class ExprContext {

	final VarResolverSession session;
	final Object thisValue;
	final boolean strict;
	final int maxCollectionSize;
	final int maxExprDepth;
	final int depth;
	final String source;

	/** Elements evaluated so far by select/project in this evaluation: one budget shared by all element contexts. */
	final int[] budget;

	private final Function<String,Object> root;
	private final ExprContext parent;

	/**
	 * Creates a top-level context.
	 *
	 * @param session The resolver session (function lookup, strictness, limits).
	 * @param root The render-scope lookup; a <jk>null</jk> result means the name is absent.
	 * @param thisValue The value of <c>this</c> at top level (may be <jk>null</jk>).
	 * @param source The expression source (for error carets).
	 */
	ExprContext(VarResolverSession session, Function<String,Object> root, Object thisValue, String source) {
		this(session, root, thisValue, source, null, 0, new int[1]);
	}

	private ExprContext(VarResolverSession session, Function<String,Object> root, Object thisValue, String source, ExprContext parent, int depth, int[] budget) {
		this.session = session;
		this.root = root;
		this.thisValue = thisValue;
		this.source = source;
		this.parent = parent;
		this.depth = depth;
		this.budget = budget;
		this.strict = session.isStrict();
		this.maxCollectionSize = session.maxCollectionSize();
		this.maxExprDepth = session.maxExprDepth();
	}

	/**
	 * Derives the context for one iteration element.
	 *
	 * @param el The element.
	 * @return A child context one element-scope deeper.
	 */
	ExprContext element(Object el) {
		return new ExprContext(session, null, el, source, this, depth + 1, budget);
	}

	/**
	 * Looks up a bare identifier.
	 *
	 * @param name The name.
	 * @param pos Source offset (for errors).
	 * @return The value, or <jk>null</jk> if absent.
	 */
	Object lookup(String name, int pos) {
		if ("this".equals(name))
			return thisValue;
		if (parent != null) {
			var v = ExprEvaluator.elementProp(thisValue, name, this, pos);
			return v == ExprEvaluator.MISSING ? parent.lookup(name, pos) : v;
		}
		return root == null ? null : root.apply(name);
	}
}
