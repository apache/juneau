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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not guaranteed to be thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",   // VarResolver resources managed by calling code
	"java:S115"   // Constants use UPPER_snakeCase convention (e.g., PROP_contextBeanStore)
})
public class VarResolverSession {

	// Property name constants
	private static final String PROP_contextBeanStore = "context.beanStore";
	private static final String PROP_sessionBeanStore = "session.beanStore";
	private static final String PROP_var = "var";

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

	private final VarResolver context;

	private final WritableBeanStore beanStore;

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
		"rawtypes",   // Raw types necessary for generic resolution
		"unchecked",  // Type erasure requires unchecked operations
		"java:S3776"  // Cognitive complexity acceptable for recursive resolution logic
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
			.a(PROP_contextBeanStore, this.context.beanStore)
			.a(PROP_var, this.context.getVarMap().keySet())
			.a(PROP_sessionBeanStore, beanStore);
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
}
