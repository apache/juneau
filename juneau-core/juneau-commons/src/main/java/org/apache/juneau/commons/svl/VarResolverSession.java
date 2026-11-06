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

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.utils.*;

/**
 * A var resolver session that combines a {@link VarResolver} with one or more session objects.
 *
 * <p>
 * Instances of this class are considered light-weight and fast to construct, use, and discard.
 *
 * <p>
 * This class contains the workhorse code for var resolution.
 *
 * <p>
 * Instances of this class are created through the {@link VarResolver#createSession()} and
 * {@link VarResolver#createSession(WritableBeanStore)} methods.
 *
 * <h2 class='section'>Notes:</h2><ul>
 * 	<li class='warn'>This class is not guaranteed to be thread safe.
 * </ul>
 *
 * <h2 class='section'>See Also:</h2><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Marshall Simple Variable Language</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S1192", // Duplicated literals (argument/property names) read more clearly inline than as constants
	"resource" // VarResolver resources managed by calling code
})
public class VarResolverSession {

	private static boolean containsVars(Collection<?> c) {
		var f = Flag.create();
		c.forEach(x -> {
			if (x instanceof CharSequence && x.toString().contains("$"))
				f.set();
		});
		return f.isSet();
	}

	private static boolean containsVars(Map<?,?> m) {
		var f = Flag.create();
		m.forEach((k, v) -> {
			if (v instanceof CharSequence && v.toString().contains("$"))
				f.set();
		});
		return f.isSet();
	}

	private static boolean containsVars(Object array) {
		for (var i = 0; i < Array.getLength(array); i++) {
			var o = Array.get(array, i);
			if (o instanceof CharSequence && o.toString().contains("$"))
				return true;
		}
		return false;
	}

	private static final Set<String> HTML_DOC_PASSTHROUGH_VARS = Set.of("W", "C", "RS");

	private static final Set<String> SCRIPT_STYLE_REFUSED_VARS = Set.of("RQ", "RH", "RF", "RP", "RA", "SA", "R");

	private final VarResolver context;

	private final WritableBeanStore beanStore;

	private int recursionDepth;

	/** Number of {@code ${...}} references inside {@code #{...}} currently being resolved on this session. */
	private int exprVarDepth;

	private boolean htmlDocEncode;

	private boolean scriptStyleRefuse;

	private Function<Object,String> valueFormatter;
	private Function<String,Object> scope;

	/**
	 * Constructor.
	 *
	 * @param context
	 * 	The {@link VarResolver} context object that contains the {@link Var Vars} and context objects associated with
	 * 	that resolver.
	 * @param beanStore The bean store to use for resolving beans needed by vars.  Can be <jk>null</jk> (this session will then have no parent bean store, and only beans explicitly added via {@link #bean(Class, Object)} will be resolvable).
	 *
	 */
	public VarResolverSession(VarResolver context, WritableBeanStore beanStore) {
		this.context = context;
		this.beanStore = new BasicBeanStore(beanStore);
	}

	/**
	 * Looks up a registered function by exact name.
	 *
	 * <p>
	 * Functions excluded by {@link VarResolver.Builder#allowFunctions(String...)} are not returned, so a
	 * <jk>null</jk> result means absent <i>or denied</i>; use {@link #isFunctionDenied(String)} to tell them apart.
	 *
	 * @param name The function name.
	 * @return The function, or <jk>null</jk> if none is registered under that name (or it is not allowed).
	 */
	public VarFunction findFunction(String name) { return context.findFunction(name); }

	/**
	 * Returns <jk>true</jk> if the named function (or operator) is excluded by the resolver's allowlist.
	 *
	 * @param name The function name (e.g. <js>"upper"</js> or <js>"$op.add"</js>).
	 * @return <jk>true</jk> if the name was registered and filtered out by the allowlist, or is an <c>$op.*</c> name the allowlist does not permit.
	 */
	public boolean isFunctionDenied(String name) { return context.isDenied(name) || (name.startsWith("$op.") && ! context.isAllowed(name)); }

	/**
	 * Resolves a <c>${Type:body}</c> reference to a typed value for the expression evaluator.
	 *
	 * <p>
	 * Finds the var by prefix and returns <c>var.doResolveValue(session, body)</c>, so defaults ({@link DefaultingVar})
	 * and multipart keys are honoured.  The body is resolved with the existing nested-marker machinery first when the
	 * var allows nesting and the body contains a <c>$</c>.  The {@link #resolveScriptStyle(String)} refusal and the
	 * {@link #resolveHtmlDoc(String)} encoding apply exactly as they do to template <c>${...}</c> references: under
	 * HTML-doc encoding a value from a non-passthrough var is escaped, and a non-<c>String</c> value other than a
	 * <c>Number</c> or <c>Boolean</c> is first converted with <c>String.valueOf</c> (as the template path does), so
	 * markup inside a collection or bean cannot bypass the escaping.
	 *
	 * @param prefix The var name (e.g. <js>"S"</js>).
	 * @param body The var body (already in the var's own syntax, e.g. <js>"key,default"</js>).
	 * @return The value, or <jk>null</jk> if absent or (lenient mode) the var is unknown.
	 * @throws VarResolverException If the var is unknown and the resolver is strict, or the var fails.
	 */
	Object resolveValue(String prefix, String body) {
		var v = isScriptStyleRefused(prefix) ? null : getVar(prefix);
		if (v == null) {
			if (context.strict)
				throw new VarResolverException("Unknown or unavailable variable '%s'", prefix);
			return null;
		}
		try {
			var b = (v.allowNested() && body.indexOf('$') != -1) ? resolve(body) : body;
			var r = v.doResolveValue(this, b);
			// HTML-doc chrome: values are escaped unless the var is an HTML passthrough, as in templates.
			if (! htmlDocEncode || r == null || r instanceof Number || r instanceof Boolean)
				return r;
			return applyHtmlDocChromeEncoding(prefix, r instanceof String s2 ? s2 : String.valueOf(r));
		} catch (VarResolverException e) {
			throw e;
		} catch (Exception e) {
			throw new VarResolverException(e, "Problem occurred resolving variable '%s': %s", prefix, e.getMessage());
		}
	}

	/**
	 * Overrides the value formatter for this session.
	 *
	 * <p>
	 * The override <em>replaces</em> the resolver's formatter and also formats {@link Structure} values.
	 *
	 * @param value The formatter.  <jk>null</jk> reverts to the resolver's formatter.
	 * @return This object.
	 */
	public VarResolverSession valueFormatter(Function<Object,String> value) {
		this.valueFormatter = value;
		return this;
	}

	/**
	 * Sets the render scope for this session.
	 *
	 * <p>
	 * Bare identifiers inside <c>#{...}</c> expressions (outside any <c>.?[...]</c> / <c>.![...]</c> element) are looked
	 * up through this function.  A <jk>null</jk> result means the name is absent.  Inside a projection the element is the
	 * innermost scope and this function is the fallback for names the element lacks.
	 *
	 * <p>
	 * Expressions that contain identifiers are never folded at compile time, so a scope can differ per session.
	 * core <c>${...}</c> references do not read this scope; a resolver can register a var (such as the template layer's <c>P</c> var) that does.
	 *
	 * @param value The scope lookup.  <jk>null</jk> clears the scope (every identifier is absent).
	 * @return This object.
	 */
	public VarResolverSession scope(Function<String,Object> value) {
		this.scope = value;
		return this;
	}

	/**
	 * Returns the render scope.
	 *
	 * @return The render scope, or an always-absent scope if none was set.  Never <jk>null</jk>.
	 */
	public Function<String,Object> scope() { return scope == null ? ExprSegment.EMPTY_SCOPE : scope; }

	/**
	 * Formats the typed result of an expression as text.
	 *
	 * @param value The value.  May be <jk>null</jk>.
	 * @return The text; never <jk>null</jk>.
	 */
	public String formatValue(Object value) {
		String r;
		if (valueFormatter != null)
			r = valueFormatter.apply(value);
		else if (value instanceof Structure)
			r = value.toString();
		else
			r = context.valueFormatter.apply(value);
		return r == null ? "" : r;
	}

	/** @return The maximum collection size for expressions, and for the functions that build lists, tables and joins. */
	public int maxCollectionSize() { return context.maxCollectionSize; }

	/** @return The maximum expression nesting depth. */
	int maxExprDepth() { return context.maxExprDepth; }

	/**
	 * Enters a {@code ${...}} reference inside {@code #{...}}.
	 *
	 * <p>
	 * Such references compile their body lazily, so a deep {@code #{${...#{${...}}...}}} chain would recurse at
	 * evaluation time.  The chain is capped at {@code maxExprDepth}; every successful call must be paired with
	 * {@link #exitExprVar()}.
	 *
	 * @return <jk>false</jk> (and no depth change) if the cap is reached.
	 */
	boolean enterExprVar() {
		if (exprVarDepth >= context.maxExprDepth)
			return false;
		exprVarDepth++;
		return true;
	}

	/** Leaves a reference entered with {@link #enterExprVar()}. */
	void exitExprVar() {
		exprVarDepth--;
	}

	/**
	 * Returns whether the owning resolver is in strict mode.
	 *
	 * @return <jk>true</jk> if strict.
	 */
	public boolean isStrict() { return context.strict; }

	/**
	 * Adds a bean to this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type.
	 * @param value The bean.
	 * @return This object.
	 */
	public <T> VarResolverSession bean(Class<T> c, T value) {
		beanStore.addBean(c, value);
		return this;
	}

	/**
	 * Returns the bean from the registered bean store.
	 *
	 * @param <T> The value type.
	 * @param c The bean type.
	 * @return
	 * 	The bean.
	 * 	<br>Never <jk>null</jk>.
	 */
	public <T> Optional<T> getBean(Class<T> c) {
		Optional<T> t = beanStore.getBean(c);
		if (! t.isPresent())
			t = context.beanStore.getBean(c);
		return t;
	}

	/**
	 * Resolve all variables in the specified string.
	 *
	 * @param s
	 * 	The string to resolve variables in.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return
	 * 	The new string with all variables resolved, or the same string if no variables were found.
	 * 	<br>Returns <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public String resolve(String s) {
		if (s == null)
			return null;
		if (s.isEmpty())
			return s;
		return context.compile(s).resolve(this);
	}

	/**
	 * Re-resolves a var's output, enforcing the resolver's maximum recursion depth.
	 *
	 * @param s The string to resolve.
	 * @return The resolved string.
	 * @throws VarResolverException If the maximum recursion depth is exceeded.
	 */
	String resolveRecursive(String s) {
		if (++recursionDepth > context.maxRecursionDepth) {
			recursionDepth--;
			throw new VarResolverException("Maximum variable recursion depth (%s) exceeded while resolving '%s'", context.maxRecursionDepth, s.length() > 80 ? s.substring(0, 80) + "..." : s);
		}
		try {
			return resolve(s);
		} finally {
			recursionDepth--;
		}
	}

	/**
	 * Resolves SVL variables for HTML-doc chrome fields that write raw HTML.
	 *
	 * <p>
	 * Literal characters from the template (including tags) pass through unchanged. Each SVL
	 * substitution is XML-escaped via {@link StringUtils#escapeXml(String)} before insert, except
	 * the HTML-passthrough vars {@code $W}, {@code $C}, and {@code $RS}.
	 *
	 * <p>
	 * Plain {@link #resolve(String)} is unchanged. Use this only for HTML-doc chrome (aside, header,
	 * footer, nav HTML fragments, head, no-results).
	 *
	 * @param s The chrome template. Can be <jk>null</jk>.
	 * @return The resolved chrome string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public String resolveHtmlDoc(String s) {
		htmlDocEncode = true;
		try {
			return resolve(s);
		} finally {
			htmlDocEncode = false;
		}
	}

	/**
	 * Resolves SVL variables for {@code <script>} and {@code <style>} chrome.
	 *
	 * <p>
	 * Request-derived vars ({@code $RQ}, {@code $RH}, {@code $RF}, {@code $RP}, {@code $RA},
	 * {@code $SA}, {@code $R}) are left unresolved (same as an unknown var). Other vars including
	 * {@code $W}, {@code $C}, and {@code $RS} still resolve.
	 *
	 * @param s The script or style template. Can be <jk>null</jk>.
	 * @return The resolved string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public String resolveScriptStyle(String s) {
		scriptStyleRefuse = true;
		try {
			return resolve(s);
		} finally {
			scriptStyleRefuse = false;
		}
	}

	/**
	 * Compiles the given template string into a reusable {@link VarTemplate}.
	 *
	 * <p>
	 * Equivalent to {@link VarResolver#compile(String) this.context.compile(input)}; provided on
	 * the session for symmetry with {@link #resolve(String)}. The returned template is bound to
	 * the underlying {@link VarResolver}, not to this session — see the {@link VarTemplate}
	 * class javadoc for the compile-binding contract.
	 *
	 * @param input The template string. May be {@code null}.
	 * @return A new {@link VarTemplate} bound to the underlying resolver.
	 */
	public VarTemplate compile(String input) {
		return context.compile(input);
	}

	/**
	 * Returns a session-bound {@link Supplier} that re-resolves the given template against
	 * <i>this</i> session each time {@link Supplier#get()} is called.
	 *
	 * <p>
	 * Unlike {@link VarResolver#resolveSupplier(String)} — which opens a fresh session per
	 * {@code .get()} and is threadsafe to share — this Supplier captures the existing session
	 * and reuses it. <b>It is therefore not threadsafe</b>; it inherits the standard
	 * "{@link VarResolverSession} is not guaranteed to be thread safe" contract.
	 *
	 * <p>
	 * Use this variant only when the caller owns the session lifecycle and the perf saving of
	 * skipping per-call session construction matters. For shared / cross-thread use cases,
	 * prefer {@link VarResolver#resolveSupplier(String)}.
	 *
	 * @param input The template string. May be {@code null}.
	 * @return A {@code Supplier<String>} bound to this session.
	 */
	public Supplier<String> resolveSupplier(String input) {
		return compile(input).asSupplier(this);
	}

	/**
	 * Resolves the specified strings in the string array.
	 *
	 * @param in The string array containing variables to resolve.  Must not be <jk>null</jk> or a {@link NullPointerException} is thrown, though individual elements can be <jk>null</jk>.
	 * @return An array with resolved strings.  Elements are <jk>null</jk> where the corresponding input element was <jk>null</jk>.
	 */
	public String[] resolve(String[] in) {
		var out = new String[in.length];
		for (var i = 0; i < in.length; i++)
			out[i] = resolve(in[i]);
		return out;
	}

	/**
	 * Convenience method for resolving variables in arbitrary objects.
	 *
	 * <p>
	 * Supports resolving variables in the following object types:
	 * <ul>
	 * 	<li>{@link CharSequence}
	 * 	<li>Arrays containing values of type {@link CharSequence}.
	 * 	<li>Collections containing values of type {@link CharSequence}.
	 * 		<br>Collection class must have a no-arg constructor.
	 * 	<li>Maps containing values of type {@link CharSequence}.
	 * 		<br>Map class must have a no-arg constructor.
	 * </ul>
	 *
	 * @param <T> The value type.
	 * @param o The object.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The same object if no resolution was needed, otherwise a new object or data structure if resolution was
	 * needed.  Returns <jk>null</jk> if the input was <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S3776", // Cognitive complexity acceptable for recursive resolution logic
		"rawtypes", // Raw types necessary for generic resolution
		"unchecked" // Type erasure requires unchecked operations
	})
	public <T> T resolve(T o) {
		if (o == null)
			return null;
		if (o instanceof CharSequence o2)
			return (T)resolve(o2.toString());
		if (isArray(o)) {
			if (! containsVars(o))
				return o;
			var o2 = Array.newInstance(o.getClass().getComponentType(), Array.getLength(o));
			for (var i = 0; i < Array.getLength(o); i++)
				Array.set(o2, i, resolve(Array.get(o, i)));
			return (T)o2;
		}
		if (o instanceof Set o2) {
			try {
				if (! containsVars(o2))
					return o;
				Set o3 = info(o).getDeclaredConstructor(x -> x.isPublic() && x.getParameterCount() == 0).map(ci -> safe(() -> (Set)ci.inner().newInstance())).orElseGet(LinkedHashSet::new);
				Set o4 = o3;
				o2.forEach(x -> o4.add(resolve(x)));
				return (T)o3;
			} catch (VarResolverException e) {
				throw e;
			} catch (Exception e) {
				throw new VarResolverException(e, "Problem occurred resolving set.");
			}
		}
		if (o instanceof List o2) {
			try {
				if (! containsVars(o2))
					return o;
				List o3 = info(o).getDeclaredConstructor(x -> x.isPublic() && x.getParameterCount() == 0).map(ci -> safe(() -> (List)ci.inner().newInstance())).orElseGet(() -> list());
				List o4 = o3;
				o2.forEach(x -> o4.add(resolve(x)));
				return (T)o3;
			} catch (VarResolverException e) {
				throw e;
			} catch (Exception e) {
				throw new VarResolverException(e, "Problem occurred resolving collection.");
			}
		}
		if (o instanceof Map o2) {
			try {
				if (! containsVars(o2))
					return o;
				Map o3 = info(o).getDeclaredConstructor(x -> x.isPublic() && x.getParameterCount() == 0).map(ci -> safe(() -> (Map)ci.inner().newInstance())).orElseGet(LinkedHashMap::new);
				Map o4 = o3;
				o2.forEach((k, v) -> o4.put(k, resolve(v)));
				return (T)o3;
			} catch (VarResolverException e) {
				throw e;
			} catch (Exception e) {
				throw new VarResolverException(e, "Problem occurred resolving map.");
			}
		}
		return o;
	}

	/**
	 * Resolves variables in the specified string and sends the output to the specified writer.
	 *
	 * <p>
	 * More efficient than first parsing to a string and then serializing to the writer since this method doesn't need
	 * to construct a large string.
	 *
	 * @param s The string to resolve variables in.  Can be <jk>null</jk>, in which case the writer is left unchanged.
	 * @param out The writer to write to.  Must not be <jk>null</jk>.
	 * @return The same writer.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Writer resolveTo(String s, Writer out) throws IOException {
		if (s == null)
			return out;
		if (s.isEmpty()) {
			return out;
		}
		return context.compile(s).resolveTo(this, out);
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a("context.beanStore", this.context.beanStore)
			.a("var", this.context.getVarMap().keySet())
			.a("session.beanStore", beanStore);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}

	/**
	 * Returns the {@link Var} with the specified name.
	 *
	 * @param name The var name (e.g. <js>"S"</js>).  Must not be <jk>null</jk>.
	 * @return The {@link Var} instance, or <jk>null</jk> if no <c>Var</c> is associated with the specified name.
	 */
	protected Var getVar(String name) {
		Var v = this.context.getVarMap().get(name);
		return nn(v) && v.canResolve(this) ? v : null;
	}

	/**
	 * Returns <jk>true</jk> if this session is resolving HTML-doc chrome and substitutions should be XML-escaped.
	 *
	 * @return <jk>true</jk> if HTML-doc chrome encoding is enabled.
	 */
	boolean isHtmlDocEncode() {
		return htmlDocEncode;
	}

	/**
	 * Returns <jk>true</jk> if {@code varName} must be left unresolved in script/style chrome.
	 *
	 * @param varName The SVL var prefix (e.g. <js>"RQ"</js>).
	 * @return <jk>true</jk> if the var should fall through like an unknown var.
	 */
	boolean isScriptStyleRefused(String varName) {
		return scriptStyleRefuse && SCRIPT_STYLE_REFUSED_VARS.contains(varName);
	}

	/**
	 * XML-escapes an SVL substitution when HTML-doc chrome encoding is enabled and the var is not passthrough.
	 *
	 * @param varName The SVL var prefix.
	 * @param replacement The resolved substitution text.
	 * @return The possibly escaped replacement.
	 */
	String applyHtmlDocChromeEncoding(String varName, String replacement) {
		if (! htmlDocEncode || replacement == null || HTML_DOC_PASSTHROUGH_VARS.contains(varName))
			return replacement;
		return StringUtils.escapeXml(replacement);
	}
}
