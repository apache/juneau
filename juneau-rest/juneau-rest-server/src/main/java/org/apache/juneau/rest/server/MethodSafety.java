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


// Single-type import, not the usual wildcard: this package declares its own Method type, which would otherwise
// shadow java.lang.reflect.Method here.
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.response.*;
import static org.apache.juneau.commons.utils.Shorts.*;

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
 * <p>
 * The same check fails an operation declared {@link RestOp#read() read=true} that is bound to a method defined as
 * unsafe, to {@code "*"} or to {@code "RRPC"}, or that is also {@link Mutating @Mutating}.  A read can be reached by
 * overriding a safe request, so a misdeclared read would be a write behind a plain link.
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

	/** The standard methods defined as unsafe, which an operation can never declare a read. */
	private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE", "CONNECT");

	/** The query parameter that can claim a request's method. */
	private static final String OVERRIDE_PARAM = "method";

	/** The headers that can claim a request's method. */
	private static final List<String> OVERRIDE_HEADERS = List.of("X-Method", "X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override");

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
	 * The method a request is judged on by a write-protection check.
	 *
	 * <p>
	 * A request is judged on the method it <b>claims</b>, not only on its request line.  A {@code method} query
	 * parameter, or an {@code X-Method}, {@code X-HTTP-Method-Override}, {@code X-HTTP-Method} or
	 * {@code X-Method-Override} header, naming anything other than a safe method makes the request a write, whether or
	 * not the resource it reaches is configured to honour that override.  A check that followed dispatch's override
	 * configuration instead would open a hole the day a resource's allowed overrides changed.
	 *
	 * <p>
	 * The rules fail closed:
	 * <ul>
	 * 	<li>The request-line method is the floor.  An unsafe request line is returned (upper-cased); an override can raise a
	 * 		request's severity but never lower it, so {@code POST ?method=GET} stays {@code POST}.
	 * 	<li>Any unsafe claim wins, so {@code ?method=GET&method=DELETE} is {@code DELETE}.
	 * 	<li>An unknown method is unsafe, so {@code ?method=FROB} is judged as a write.
	 * 	<li>A claim with a malformed percent-escape is judged on its raw text, which is never a safe method name.
	 * 	<li>A blank claim is ignored.
	 * </ul>
	 *
	 * @param method The request line's method.  Can be <jk>null</jk>, which is returned as <jk>null</jk> (unsafe).
	 * @param rawQuery The raw, undecoded query string.  Can be <jk>null</jk>.
	 * @param headers Looks up a request header by name, returning <jk>null</jk> when absent.  Must not be <jk>null</jk>.
	 * @return The upper-cased request-line method when it is not safe, else the first unsafe claim (upper-cased), else
	 * 	the upper-cased request-line method.
	 */
	public static String effectiveMethod(String method, String rawQuery, UnaryOperator<String> headers) {
		reqnn("headers", headers);
		var m = method == null ? null : method.toUpperCase(Locale.ROOT);
		if (! isSafe(m))
			return m;
		for (var claim : queryClaims(rawQuery)) {
			var c = unsafeClaim(claim);
			if (c != null)
				return c;
		}
		for (var h : OVERRIDE_HEADERS) {
			var c = unsafeClaim(headers.apply(h));
			if (c != null)
				return c;
		}
		return m;
	}

	/**
	 * The names of the request headers that can claim a request's method, in precedence order.
	 *
	 * <p>
	 * The order is part of the contract: dispatch honours the first admitted header in this order.
	 *
	 * @return An immutable list of header names.  Never <jk>null</jk>.
	 */
	public static List<String> overrideHeaders() {
		return OVERRIDE_HEADERS;
	}

	/**
	 * Every value of the {@code method} query parameter, in order.
	 *
	 * <p>
	 * The one parser shared by the write-protection filters and by dispatch, so they cannot disagree about what a query
	 * string claims.  The raw query is split on {@code &}, each name and value is percent-decoded, and the name is matched
	 * case-insensitively.  Values are returned untrimmed.  A malformed percent-escape leaves the raw text.
	 *
	 * @param rawQuery The raw, undecoded query string.  Can be <jk>null</jk>.
	 * @return The decoded values, in order.  Never <jk>null</jk>; empty when there is no claim.
	 */
	public static List<String> queryClaims(String rawQuery) {
		if (rawQuery == null)
			return List.of();
		var l = new ArrayList<String>();
		for (var pair : rawQuery.split("&")) {
			var i = pair.indexOf('=');
			if (i > 0 && OVERRIDE_PARAM.equalsIgnoreCase(decode(pair.substring(0, i))))
				l.add(decode(pair.substring(i + 1)));
		}
		return List.copyOf(l);
	}

	/** The claimed method upper-cased when it is unsafe, else <jk>null</jk> (absent, blank or safe). */
	private static String unsafeClaim(String claim) {
		if (ib(claim))
			return null;
		var c = claim.trim().toUpperCase(Locale.ROOT);
		return isSafe(c) ? null : c;
	}

	/** Decodes a query-string component; a malformed escape leaves the raw text, which is then judged as is. */
	private static String decode(String s) {
		try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return s;
		}
	}

	/**
	 * Fails when any operation declared {@link Mutating @Mutating} is bound to a safe method, or any operation
	 * declared {@link RestOp#read() read=true} is bound to a method that cannot be a read.
	 *
	 * @param ops The operations of one resource.  Must not be <jk>null</jk>.
	 * @throws InternalServerError If a mutating operation is bound to a safe method, or a read declaration is
	 * 	misplaced or contradicts the operation's method or a {@link Mutating @Mutating} declaration.
	 */
	public static void check(List<RestOpContext> ops) {
		reqnn("ops", ops);
		for (var op : ops)
			checkOperation(op.getHttpMethod(), op.getJavaMethod(), op.isRead());
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
		checkOperation(httpMethod, javaMethod, false);
	}

	/**
	 * Fails when {@code javaMethod} is declared {@link Mutating @Mutating} and {@code httpMethod} is safe, or when the
	 * operation is declared {@link RestOp#read() read=true} and {@code httpMethod} cannot be a read.
	 *
	 * <p>
	 * A read declaration is refused on the methods defined as unsafe ({@code POST}, {@code PUT}, {@code PATCH},
	 * {@code DELETE}, {@code CONNECT}), on {@code "*"} (which answers {@code DELETE} too), on {@code "RRPC"} (which
	 * dispatches arbitrary interface methods), and together with {@link Mutating @Mutating}.  It is accepted on a
	 * custom method, and is redundant on a safe one.
	 *
	 * @param httpMethod The resolved HTTP method, as dispatch sees it.  Can be <jk>null</jk>, which is not safe.
	 * 	{@code "*"} matches every method and is therefore treated as including a safe one.
	 * @param javaMethod The Java method implementing the operation.  Must not be <jk>null</jk>.
	 * @param read Whether the operation is declared {@link RestOp#read() read=true}.
	 * @throws InternalServerError If the declarations contradict each other or the method.
	 * @since 10.0.0
	 */
	public static void checkOperation(String httpMethod, Method javaMethod, boolean read) {
		reqnn("javaMethod", javaMethod);

		// Resolved through MethodInfo rather than Method.getAnnotation, so that an operation inheriting its
		// declaration from a superclass or interface method is seen. Method.getAnnotation does not walk overrides,
		// which would let an inherited @Mutating go unchecked; MethodInfo walks matching methods child-to-parent,
		// the same way the framework resolves every other method annotation.
		var mutating = MethodInfo.of(javaMethod.getDeclaringClass(), javaMethod).getAnnotations(Mutating.class)
			.findFirst().map(AnnotationInfo::inner).orElse(null);
		if (read)
			checkRead(httpMethod, javaMethod, nn(mutating));
		if (mutating == null)
			return;

		// A wildcard operation answers GET along with everything else, so it is bound to a safe method too.
		var wildcard = eq(httpMethod, "*");
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

	/** Fails when a read declaration contradicts the operation's method or a {@link Mutating @Mutating} declaration. */
	private static void checkRead(String httpMethod, Method javaMethod, boolean mutating) {
		// Normalized as the operation table does, so a direct caller passing "rrpc" or "METHOD" gets the same verdict.
		var m = httpMethod == null ? null : ucr(httpMethod);
		if (eq(m, "METHOD"))
			m = "*";
		String why;
		if (mutating)
			why = "is also annotated @Mutating, which declares the opposite";
		else if (eq(m, "*"))
			why = "is bound to every method, including unsafe ones such as DELETE";
		else if (eq(m, "RRPC"))
			why = "is a remote-proxy (RRPC) operation, which dispatches arbitrary interface methods";
		else if (nn(m) && UNSAFE_METHODS.contains(m))
			why = "is bound to '" + m + "', which is defined as unsafe";
		else
			return;
		throw new InternalServerError(
			"Operation '" + javaMethod.getDeclaringClass().getSimpleName() + "." + javaMethod.getName()
			+ "' is declared @RestOp(read=true) but " + why + "."
			+ " A read can be reached by overriding a GET, so declaring a write a read would let a plain link change state."
			+ " Remove read=true, or bind the operation to a custom method that does not change state."
			+ " Note that @RestOp with no method= infers the method from the Java method name.");
	}
}
