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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Reflection-based base class for {@link VarFunction} implementations that prefer typed
 * argument signatures over manual {@code List<Object>} unpacking.
 *
 * <p>
 * Subclasses declare one or more public {@code invoke(...)} methods whose parameter lists mirror
 * the function's expected arguments. Per-arg target types and arities are derived once at
 * construction via reflection; per-call dispatch picks the matching overload by argument count
 * and runs one reflective invoke + N {@link ArgCoercer} coercions.
 *
 * <h5 class='section'>Method-resolution rules:</h5>
 * <ul>
 * 	<li>The subclass declares one or more public methods named {@code invoke}. Each must have a
 * 		distinct fixed-arity (or be the single variadic overload — see below).
 * 	<li>The return type must be {@code String} (or a subtype convertible via
 * 		{@link Object#toString()}).
 * 	<li>Parameter types may be any of: {@code String}, {@code int}, {@code long}, {@code double},
 * 		{@code boolean} (or their wrappers), {@code String[]}, or {@code Object}. The final slot
 * 		may be {@code String[]} for variadic functions.
 * 	<li>If the subclass needs the {@link VarResolverSession session} (e.g. to look up beans),
 * 		declare {@code VarResolverSession} as the <i>first</i> parameter.
 * 	<li>At most one variadic overload (final {@code String[]} slot) per subclass; combined with
 * 		fixed-arity overloads as long as the arity counts don't overlap.
 * </ul>
 *
 * <h5 class='section'>Example — multi-arity:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> Substring <jk>extends</jk> TypedFunction {
 * 		<ja>@Override</ja> <jk>public</jk> String name() { <jk>return</jk> <js>"substring"</js>; }
 * 		<jk>public</jk> String invoke(String <jv>s</jv>, <jk>int</jk> <jv>start</jv>) { <jk>return</jk> <jv>s</jv>.substring(<jv>start</jv>); }
 * 		<jk>public</jk> String invoke(String <jv>s</jv>, <jk>int</jk> <jv>start</jv>, <jk>int</jk> <jv>end</jv>) { <jk>return</jk> <jv>s</jv>.substring(<jv>start</jv>, <jv>end</jv>); }
 * 	}
 * </p>
 */
@SuppressWarnings({
	"java:S3776", // Cognitive complexity acceptable for the reflection-driven dispatch.
})
public abstract class TypedFunction implements VarFunction {

	/** Per-overload reflection metadata. */
	private static final class Overload {
		final Method method;
		final Class<?>[] argTypes;     // excluding leading session arg if present
		final boolean takesSession;
		final boolean variadic;
		final int fixedCount;          // arg count excluding the variadic slot

		Overload(Method method, Class<?>[] argTypes, boolean takesSession, boolean variadic) {
			this.method = method;
			this.argTypes = argTypes;
			this.takesSession = takesSession;
			this.variadic = variadic;
			this.fixedCount = variadic ? argTypes.length - 1 : argTypes.length;
		}

		boolean acceptsArity(int n) {
			if (variadic)
				return n >= fixedCount;
			return n == fixedCount;
		}
	}

	/** All declared {@code invoke} overloads, sorted by descending arity for predictable matching. */
	private final Overload[] overloads;
	private final int minArity;
	private final int maxArity;

	protected TypedFunction() {
		this.overloads = findInvokeOverloads(getClass());
		var lo = Integer.MAX_VALUE;
		var hi = 0;
		var anyVariadic = false;
		for (var o : overloads) {
			if (o.variadic) {
				anyVariadic = true;
				lo = Math.min(lo, o.fixedCount);
			} else {
				lo = Math.min(lo, o.fixedCount);
				hi = Math.max(hi, o.fixedCount);
			}
		}
		this.minArity = lo == Integer.MAX_VALUE ? 0 : lo;
		this.maxArity = anyVariadic ? Integer.MAX_VALUE : hi;
	}

	@Override
	public final int minArity() { return minArity; }

	@Override
	public final int maxArity() { return maxArity; }

	@Override
	public final String invoke(VarResolverSession session, List<Object> args) {
		var n = args.size();
		Overload o = null;
		for (var c : overloads) {
			if (c.acceptsArity(n)) { o = c; break; }
		}
		if (o == null)
			throw illegalArg("Function ''{0}'' has no overload accepting {1} argument(s)", name(), n);

		var coerced = ArgCoercer.coerce(name(), o.argTypes, args);
		try {
			Object[] effective;
			if (o.takesSession) {
				effective = new Object[coerced.length + 1];
				effective[0] = session;
				System.arraycopy(coerced, 0, effective, 1, coerced.length);
			} else {
				effective = coerced;
			}
			var result = o.method.invoke(this, effective);
			return result == null ? "" : result.toString();
		} catch (InvocationTargetException e) {
			var cause = e.getTargetException();
			if (cause instanceof RuntimeException re)
				throw re;
			throw illegalArg("Function ''{0}'' threw {1}: {2}", name(), cause.getClass().getSimpleName(), cause.getMessage());
		} catch (IllegalAccessException e) {
			throw illegalArg("Function ''{0}'' invoke method must be public: {1}", name(), e.getMessage());
		}
	}

	private static Overload[] findInvokeOverloads(Class<? extends TypedFunction> cls) {
		var found = new ArrayList<Overload>();
		for (var m : cls.getMethods()) {
			if (!"invoke".equals(m.getName()))
				continue;
			if (m.getDeclaringClass() == TypedFunction.class
				|| m.getDeclaringClass() == Object.class
				|| m.getDeclaringClass() == VarFunction.class)
				continue;
			var allParams = m.getParameterTypes();
			var takesSession = allParams.length > 0 && allParams[0] == VarResolverSession.class;
			var startIdx = takesSession ? 1 : 0;
			var argTypes = Arrays.copyOfRange(allParams, startIdx, allParams.length);
			var variadic = argTypes.length > 0 && argTypes[argTypes.length - 1] == String[].class;
			found.add(new Overload(m, argTypes, takesSession, variadic));
		}
		if (found.isEmpty())
			throw illegalArg("TypedFunction subclass {0} must declare a public invoke(...) method", cls.getName());
		// Sort by descending fixedCount so larger arity overloads get matched before any
		// variadic overload they might overlap with.
		found.sort((a, b) -> Integer.compare(b.fixedCount, a.fixedCount));
		return found.toArray(new Overload[0]);
	}
}
