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

import java.util.*;

/**
 * A {@link TemplateSegment} that resolves a {@code #{name(args...)}} script call.
 *
 * <p>
 * Built by {@link VarTemplateCompiler}'s recursive-descent parser. The function {@link #name} is
 * the literal token between {@code #{}} and the opening parenthesis; {@link #argTemplates} hold
 * the parsed arguments — each is itself a {@link VarTemplate} so that nested {@code ${...}} and
 * {@code #{...}} resolve naturally per arg.
 *
 * <p>
 * The {@link #cachedFn} field is set at compile time via the four-channel discovery chain
 * (built-in / ServiceLoader / BeanStore / explicit). When null, the function name was
 * unregistered at compile time; {@link #resolve(VarResolverSession, StringBuilder) resolve}
 * then throws {@link IllegalArgumentException} with a lazy-fail message:
 * "{@code No such function 'foo'}".
 *
 * <p>
 * If a function reference is unregistered at compile time, the compiler still produces a valid
 * {@link ScriptSegment} (with {@link #cachedFn} {@code null}); the failure surfaces lazily at
 * resolve time so templates that reference functions only available in some sessions don't
 * fail-fast at build time.
 */
final class ScriptSegment extends TemplateSegment {

	/**
	 * The function reference cached at compile time. {@code null} when the function name is
	 * unregistered (lazy-fail: error surfaces at resolve time).
	 */
	final VarFunction cachedFn;

	/** The function name as written in the source (for error messages and diagnostics). */
	final String name;

	/**
	 * The arguments — each pre-compiled into a {@link VarTemplate} so nested var/script
	 * markers compose naturally.
	 */
	final VarTemplate[] argTemplates;

	ScriptSegment(VarFunction cachedFn, String name, VarTemplate[] argTemplates) {
		this.cachedFn = cachedFn;
		this.name = name;
		this.argTemplates = argTemplates;
	}

	@Override
	void resolve(VarResolverSession session, StringBuilder out) {
		if (cachedFn == null)
			throw illegalArg("No such function ''{0}''", name);

		// Resolve each arg template to a String, then dispatch to the function. TypedFunction
		// handles ArgCoercer.coerce(...) internally; direct VarFunction implementations get the
		// raw String args via the args list.
		var args = new ArrayList<Object>(argTemplates.length);
		for (var t : argTemplates)
			args.add(t.resolve(session));

		var n = args.size();
		if (n < cachedFn.minArity() || n > cachedFn.maxArity()) {
			var maxStr = cachedFn.maxArity() == Integer.MAX_VALUE ? "any" : String.valueOf(cachedFn.maxArity());
			throw illegalArg("Function ''{0}'' expected {1}..{2} arg(s), got {3}",
				name, cachedFn.minArity(), maxStr, n);
		}

		var result = cachedFn.invoke(session, args);
		if (result != null)
			out.append(result);
	}
}
