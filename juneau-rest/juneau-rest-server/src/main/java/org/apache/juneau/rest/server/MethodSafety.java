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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

// Single-type import, not the usual wildcard: this package declares its own Method type, which would otherwise
// shadow java.lang.reflect.Method here.
import java.lang.reflect.Method;
import java.util.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;

/**
 * Which HTTP methods are safe, and the startup check that an operation declared {@link Mutating} is not bound to
 * one of them.
 *
 * <p>
 * This is the single definition of the safe-method set for this framework.  Anything that needs to know whether a
 * request may change state &mdash; notably
 * {@link org.apache.juneau.rest.server.filter.LoopbackBoundary#isStateChanging(String)}, which delegates here
 * &mdash; reads it from this class, so a security filter and a startup check cannot come to disagree about which
 * methods are writes.  A divergence between those two would be silent and would defeat both.
 *
 * <h5 class='section'>The check</h5>
 * <p>
 * {@link #check(List)} runs when a resource's operation table is assembled, from
 * {@link RestOperations#RestOperations(RestOperations.Builder)} &mdash; the one constructor every operation table
 * passes through, so there is no registration to remember and no path around it.  It fails the resource when an
 * operation carrying {@link Mutating @Mutating} resolves to a safe method.
 * <p>
 * The resolved method is read from {@link RestOpContext#getHttpMethod()}, which is what dispatch itself uses.  That
 * matters more than it sounds: the method may have been <b>inferred</b> from the Java method name rather than
 * written down (see {@link RestOp#method()}), and a check that re-derived it independently could disagree with
 * dispatch about which operation is bound to what.  A security-relevant check that disagrees with the router is
 * worse than none.
 * <p>
 * A wildcard operation ({@code @RestOp(method="*")}) matches {@code GET} among everything else, so it is a
 * contradiction too and is reported as one.
 *
 * <h5 class='section'>Why the rule is a declared contradiction rather than a detected one</h5>
 * <p>
 * See {@link Mutating}, which records why inferring "this handler mutates" is not soundly possible and why a check
 * that can wrongly refuse to start an application is worse than no check at all.  The short form: this class only
 * ever reports a contradiction the developer wrote down, so it cannot produce a false positive, and it cannot catch
 * a handler that mutates and says nothing.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link Mutating}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.filter.LoopbackBoundary}
 * </ul>
 *
 * @since 10.0.0
 */
public class MethodSafety {

	/**
	 * The request methods RFC 9110 defines as safe.
	 *
	 * <p>
	 * A method absent from this set &mdash; including one this framework does not recognize &mdash; is treated as
	 * state-changing, so an unusual or future method fails closed rather than skipping write checks.
	 */
	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

	/**
	 * Constructor.
	 */
	protected MethodSafety() {}

	/**
	 * Whether {@code method} is defined as safe, and therefore promises not to change state.
	 *
	 * @param method The request method.  Can be <jk>null</jk>, which is not safe.
	 * @return <jk>true</jk> if the method is one of {@code GET}, {@code HEAD}, {@code OPTIONS} or {@code TRACE}.
	 */
	public static boolean isSafe(String method) {
		return method != null && SAFE_METHODS.contains(method.toUpperCase(Locale.ROOT));
	}

	/**
	 * Fails when any operation declared {@link Mutating @Mutating} is bound to a safe method.
	 *
	 * @param ops The operations of one resource.  Must not be <jk>null</jk>.
	 * @throws InternalServerError If a mutating operation is bound to a safe method.
	 */
	public static void check(List<RestOpContext> ops) {
		assertArgNotNull("ops", ops);
		for (var op : ops)
			checkOperation(op.getHttpMethod(), op.getJavaMethod());
	}

	/**
	 * Fails when {@code javaMethod} is declared {@link Mutating @Mutating} and {@code httpMethod} is safe.
	 *
	 * <p>
	 * The per-operation half of {@link #check(List)}, taking the two facts it compares rather than a built
	 * {@link RestOpContext}, so the rule can be exercised directly.
	 *
	 * @param httpMethod The resolved HTTP method, as dispatch sees it.  Can be <jk>null</jk>, which is not safe.
	 * 	{@code "*"} matches every method and is therefore treated as including a safe one.
	 * @param javaMethod The Java method implementing the operation.  Must not be <jk>null</jk>.
	 * @throws InternalServerError If {@code javaMethod} is declared mutating and {@code httpMethod} is safe.
	 */
	public static void checkOperation(String httpMethod, Method javaMethod) {
		assertArgNotNull("javaMethod", javaMethod);

		// Resolved through MethodInfo rather than Method.getAnnotation, so that an operation inheriting its
		// declaration from a superclass or interface method is seen. Method.getAnnotation does not walk overrides,
		// which would let an inherited @Mutating go unchecked; MethodInfo walks matching methods child-to-parent,
		// the same way the framework resolves every other method annotation.
		var mutating = MethodInfo.of(javaMethod.getDeclaringClass(), javaMethod).getAnnotations(Mutating.class)
			.findFirst().map(AnnotationInfo::inner).orElse(null);
		if (mutating == null)
			return;

		// A wildcard operation answers GET along with everything else, so it is bound to a safe method too.
		var wildcard = "*".equals(httpMethod);
		if (! wildcard && ! isSafe(httpMethod))
			return;

		var what = mutating.value().isBlank() ? "" : " It changes: " + mutating.value() + ".";
		throw new InternalServerError(
			"Operation '" + javaMethod.getDeclaringClass().getSimpleName() + "." + javaMethod.getName()
			+ "' is annotated @Mutating but is bound to "
			+ (wildcard ? "every method, including safe ones" : "'" + httpMethod + "', which is a safe method")
			+ "." + what
			+ " A safe method promises not to change state, and CSRF and origin checks are applied only to methods"
			+ " that are not safe, so this operation would be reachable without them."
			+ " Bind it to POST/PUT/PATCH/DELETE, or remove @Mutating if it does not actually change state."
			+ " Note that @RestOp with no method= infers the method from the Java method name and defaults to GET.");
	}
}
