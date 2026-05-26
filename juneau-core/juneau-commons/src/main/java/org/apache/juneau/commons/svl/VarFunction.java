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

import java.util.*;

/**
 * SPI for SVL {@code #{...}} script-evaluation functions.
 *
 * <p>
 * A {@code VarFunction} is invoked when an SVL template contains a
 * <c>#{name(arg1, arg2, ...)}</c> reference — distinct from {@code $X{...}} variables. Functions
 * compose freely with {@code ${...}} lookups and other {@code #{...}} calls; arguments are
 * pre-resolved by the runtime before {@link #invoke(VarResolverSession, List) invoke} is called.
 *
 * <h5 class='section'>Naming note:</h5>
 * <p>
 * Named {@code VarFunction} rather than the shorter {@code Function} because Juneau's
 * serializer/parser/test code makes heavy use of {@link java.util.function.Function} and
 * routinely co-imports {@code org.apache.juneau.commons.svl.*}; a bare {@code Function} type in
 * this package would trigger ambiguous-reference errors across many callers. The chosen name
 * also matches the existing {@code Var}/{@code VarResolver}/{@code VarTemplate}/{@code VarResolverSession}
 * naming family.
 *
 * <p>
 * Implementations can register through any of four discovery channels (later wins on name
 * collision):
 * <ol>
 * 	<li>The built-in catalog auto-registered by {@link VarResolver#DEFAULT}.
 * 	<li>{@code META-INF/services/org.apache.juneau.commons.svl.VarFunction} (Java {@link java.util.ServiceLoader}).
 * 	<li>{@code BeanStore.getBeans(VarFunction.class)} on the resolver's bean store.
 * 	<li>Explicit registrations via {@code VarResolver.Builder.functions(...)}.
 * </ol>
 *
 * <p>
 * For typed reflection-based dispatch (the recommended path), extend
 * {@link TypedFunction} — it derives {@link #minArity()}, {@link #maxArity()}, and per-arg
 * coercion automatically from the subclass's {@code invoke(...)} method signature, eliminating
 * the per-function plumbing.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Direct VarFunction impl (less common — TypedFunction is preferred).</jc>
 * 	<jk>public class</jk> Reverse <jk>implements</jk> VarFunction {
 * 		<ja>@Override</ja> <jk>public</jk> String name() { <jk>return</jk> <js>"reverse"</js>; }
 * 		<ja>@Override</ja> <jk>public int</jk> minArity() { <jk>return</jk> 1; }
 * 		<ja>@Override</ja> <jk>public int</jk> maxArity() { <jk>return</jk> 1; }
 * 		<ja>@Override</ja> <jk>public</jk> String invoke(VarResolverSession <jv>session</jv>, List&lt;Object&gt; <jv>args</jv>) {
 * 			<jk>return new</jk> StringBuilder((String) <jv>args</jv>.get(0)).reverse().toString();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SimpleVariableLanguageBasics">Simple Variable Language Basics</a>
 * 	<li class='jc'>{@link TypedFunction}
 * </ul>
 */
public interface VarFunction {

	/**
	 * The function's invocation name as it appears between {@code #{} and the opening
	 * parenthesis in the template. Must consist of ASCII letters, digits, and underscores; must
	 * begin with a letter.
	 *
	 * @return The invocation name. Never {@code null} or empty.
	 */
	String name();

	/**
	 * Minimum argument count this function accepts.
	 *
	 * @return The minimum number of args. {@code 0} means the function may be called with no
	 *	 args. Returns {@code -1} for "any number of args" — equivalent to declaring
	 *	 {@code minArity == 0 && maxArity == Integer.MAX_VALUE}.
	 */
	int minArity();

	/**
	 * Maximum argument count this function accepts.
	 *
	 * @return The maximum number of args, or {@link Integer#MAX_VALUE} for unbounded variadic
	 *	 functions.
	 */
	int maxArity();

	/**
	 * Invokes this function with already-resolved, type-coerced arguments.
	 *
	 * <p>
	 * The runtime resolves each {@code #{...}} arg template against the session, then coerces
	 * the resolved string to the target Java type via {@code ArgCoercer}. The resulting
	 * {@code List<Object>} is passed in here.
	 *
	 * @param session The current resolver session. Must not be {@code null}.
	 * @param args The coerced arguments. Size is in {@code [minArity(), maxArity()]}. Each
	 *	 element is the type the function expects per its signature; for variadic /
	 *	 {@code Object}-typed slots, elements remain {@link String}.
	 * @return The function's result as a {@link String}. May be empty but should not be
	 *	 {@code null} (the runtime treats {@code null} as the empty string for SVL output).
	 */
	String invoke(VarResolverSession session, List<Object> args);
}
