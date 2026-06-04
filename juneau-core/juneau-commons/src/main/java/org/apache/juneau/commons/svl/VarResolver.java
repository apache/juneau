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

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.functions.*;
import org.apache.juneau.commons.svl.vars.*;

/**
 * Utility class for resolving variables of the form <js>"$X{key}"</js> in strings.
 *
 * <p>
 * Variables are of the form <c>$X{key}</c>, where <c>X</c> can consist of zero or more ASCII characters.
 * <br>The variable key can contain anything, even nested variables that get recursively resolved.
 *
 * <p>
 * Variables are defined through the {@link Builder#vars(Class[])} method.
 *
 * <p>
 * The {@link Var} interface defines how variables are converted to values.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> SystemPropertiesVar <jk>extends</jk> SimpleVar {
 *
 * 		<jc>// Must have a no-arg constructor!</jc>
 * 		<jk>public</jk> SystemPropertiesVar() {
 * 			<jk>super</jk>(<js>"S"</js>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String resolve(VarResolverSession <jv>session</jv>, String <jv>value</jv>) {
 * 			<jk>return</jk> System.<jsm>getProperty</jsm>(<jv>value</jv>);
 * 		}
 * 	}
 *
 * 	<jc>// Create a variable resolver that resolves system properties (e.g. "$S{java.home}")</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SimpleVariableLanguageBasics">Simple Variable Language Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",  // VarResolver manages Closeable resources
	"java:S3740" // Raw Var/Class types used intentionally for dynamic variable resolver registration where type parameters are not available
})
public class VarResolver {

	/**
	 * Builder class.
	 */
	public static class Builder {

		private WritableBeanStore beanStore;
		final VarList vars;
		final LinkedHashMap<Class<?>,Object> userBeans;
		final List<VarFunction> functions;

		/**
		 * Constructor.
		 */
		protected Builder() {
			this.beanStore = new BasicBeanStore();
			vars = VarList.create();
			userBeans = new LinkedHashMap<>();
			functions = new ArrayList<>();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(VarResolver copyFrom) {
			this.beanStore = copyFrom.beanStore;
			vars = VarList.of(copyFrom.vars);
			userBeans = new LinkedHashMap<>();
			functions = list(copyFrom.functions);
		}

		/**
		 * Returns the bean store used by this builder.
		 *
		 * @return The bean store used by this builder.
		 */
		public WritableBeanStore beanStore() {
			return beanStore;
		}

		/**
		 * Adds a bean to the resolved {@link VarResolver}'s local bean store.
		 *
		 * <p>
		 * The bean is stored on this builder and merged into the resolver's local bean store at
		 * {@link #build()} time.  The parent bean store passed at construction is not mutated, so beans
		 * added here are isolated to {@link VarResolver}s built from this builder.
		 *
		 * @param <T> The bean type.
		 * @param c The bean type.
		 * @param value The bean.
		 * @return This object .
		 */
		public <T> Builder bean(Class<T> c, T value) {
			userBeans.put(c, value);
			return this;
		}

		/**
		 * Adds the default variables to this builder.
		 *
		 * <p>
		 * The default variables are:
		 * <ul>
		 * 	<li>{@link SystemPropertiesVar}
		 * 	<li>{@link EnvVariablesVar}
		 * 	<li>{@link EnvFileVar}
		 * 	<li>{@link DotenvVar}
		 * 	<li>{@link ArgsVar}
		 * 	<li>{@link ManifestFileVar}
		 * </ul>
		 *
		 * <p>
		 * The 11 transformation {@code Var}s previously listed here ({@code SwitchVar},
		 * {@code IfVar}, {@code CoalesceVar}, {@code PatternMatchVar}, {@code PatternReplaceVar},
		 * {@code PatternExtractVar}, {@code UpperCaseVar}, {@code LowerCaseVar},
		 * {@code NotEmptyVar}, {@code LenVar}, {@code SubstringVar}) were deleted in 10.0.0 and
		 * replaced by the {@code #{...}} script syntax. Use {@link #defaultFunctions()} to
		 * register the built-in catalog.
		 *
		 * @return This object .
		 */
		public Builder defaultVars() {
			vars.addDefault();
			return this;
		}


		/**
		 * Register new variables with this resolver.
		 *
		 * @param values
		 * 	The variable resolver classes.
		 * 	These classes must subclass from {@link Var} and have no-arg constructors.
		 * @return This object .
		 */
		@SafeVarargs
		public final Builder vars(Class<? extends Var>...values) {
			vars.append(values);
			return this;
		}

		/**
		 * Register new variables with this resolver.
		 *
		 * @param values
		 * 	The variable resolver classes.
		 * 	These classes must subclass from {@link Var} and have no-arg constructors.
		 * @return This object .
		 */
		public Builder vars(Var...values) {
			vars.append(values);
			return this;
		}

		/**
		 * Register new variables with this resolver.
		 *
		 * @param values
		 * 	The variable resolver classes.
		 * 	These classes must subclass from {@link Var} and have no-arg constructors.
		 * @return This object .
		 */
		public Builder vars(VarList values) {
			vars.append(values);
			return this;
		}

		/**
		 * Register one or more {@link VarFunction} instances on this builder.
		 *
		 * <p>
		 * Functions registered here win over {@code BeanStore} / {@code ServiceLoader} / built-in
		 * catalog entries with the same {@link VarFunction#name() name} (later-wins-on-collision).
		 * Discovery channels merge these registrations with the auto-discovered set at
		 * {@link #build()} time.
		 *
		 * @param values The functions to register.
		 * @return This builder.
		 */
		public Builder functions(VarFunction... values) {
			Collections.addAll(functions, values);
			return this;
		}

		/**
		 * Register one or more {@link VarFunction} <i>classes</i> on this builder. Each class
		 * must have a public no-arg constructor; instances are created via the bean store at
		 * {@link #build()} time so dependency injection works the same way it does for
		 * {@link Var}s.
		 *
		 * @param values The function classes to register.
		 * @return This builder.
		 */
		@SafeVarargs
		public final Builder functions(Class<? extends VarFunction>... values) {
			for (var c : values)
				functions.add(BeanInstantiator.of(VarFunction.class, beanStore).type(c).run());
			return this;
		}

		/**
		 * Registers the built-in {@link VarFunction} catalog plus any third-party functions
		 * discovered via {@link ServiceLoader#load(Class)} on {@link VarFunction}.
		 *
		 * <p>
		 * Registration order is:
		 * <ol>
		 * 	<li>Built-in catalog — string, type-conversion, arithmetic, boolean, conditional,
		 * 		regex, encoding, date, random/UUID, and JSON-navigation functions.
		 * 	<li>Third-party functions from {@code META-INF/services/org.apache.juneau.commons.svl.VarFunction}.
		 * </ol>
		 *
		 * <p>
		 * Later registrations win on name collision — third-party {@code ServiceLoader} entries
		 * override built-ins, and explicit {@link #functions(VarFunction...)} /
		 * {@link #functions(Class...)} calls override both.
		 *
		 * @return This builder.
		 */
		public Builder defaultFunctions() {
			for (var c : BUILTIN_FUNCTION_CLASSES)
				functions.add(BeanInstantiator.of(VarFunction.class, beanStore).type(c).run());
			for (var f : ServiceLoader.load(VarFunction.class, VarResolver.class.getClassLoader()))
				functions.add(f);
			return this;
		}

		/**
		 * Builds the var resolver.
		 *
		 * @return A new {@link VarResolver}.
		 */
		public VarResolver build() {
			return new VarResolver(this);
		}
	}

	/**
	 * Built-in {@link VarFunction} classes registered by {@link Builder#defaultFunctions()}.
	 *
	 * <p>
	 * Order matters only for collisions inside the built-in catalog itself (and there should be
	 * none). Third-party functions discovered via {@link ServiceLoader} register <i>after</i>
	 * this list, so they override built-ins on name collision.
	 */
	private static final Class<? extends VarFunction>[] BUILTIN_FUNCTION_CLASSES = (Class<? extends VarFunction>[]) concat(
		StringFunctions.ALL,
		TypeConversionFunctions.ALL,
		ArithmeticFunctions.ALL,
		BooleanFunctions.ALL,
		ConditionalFunctions.ALL,
		RegexFunctions.ALL,
		EncodingFunctions.ALL,
		DateFunctions.ALL,
		RandomFunctions.ALL,
		JsonFunctions.ALL
	);

	@SafeVarargs
	@SuppressWarnings({
		"unchecked" // Array allocation with generic component type; safe since the concat result is immediately typed by the return.
	})
	private static <T> Class<? extends T>[] concat(Class<? extends T>[]... arrays) {
		var total = 0;
		for (var a : arrays) total += a.length;
		var out = new Class[total];
		var idx = 0;
		for (var a : arrays) {
			System.arraycopy(a, 0, out, idx, a.length);
			idx += a.length;
		}
		return out;
	}

	/**
	 * Default string variable resolver with support for system properties, environment
	 * variables, and the built-in {@link VarFunction} catalog.
	 *
	 * <h5 class='section'>Built-in {@link Var} bindings:</h5>
	 * <ul>
	 * 	<li><c>$S{key[,default]}</c> - {@link SystemPropertiesVar}
	 * 	<li><c>$E{key[,default]}</c> - {@link EnvVariablesVar}
	 * 	<li><c>$EF{key[,default]}</c> - {@link EnvFileVar}
	 * 	<li><c>$DE{key[,default]}</c> - {@link DotenvVar}
	 * 	<li><c>$A{key[,default]}</c> - {@link ArgsVar}
	 * 	<li><c>$MF{key[,default]}</c> - {@link ManifestFileVar}
	 * </ul>
	 *
	 * <h5 class='section'>Built-in {@link VarFunction} catalog:</h5>
	 * <p>
	 * Ships ~68 built-in functions across 10 categories — string, type-conversion, arithmetic,
	 * boolean, conditional, regex, encoding, date/time, random/UUID, and JSON-navigation.
	 * Functions are invoked via the {@code #{name(args...)}} syntax. See
	 * {@link Builder#defaultFunctions()} for the registration mechanism.
	 *
	 * <p>
	 * The 11 transformation/conditional {@code Var}s previously bundled here
	 * ({@code SwitchVar}, {@code IfVar}, {@code CoalesceVar}, {@code PatternMatchVar},
	 * {@code PatternReplaceVar}, {@code PatternExtractVar}, {@code UpperCaseVar},
	 * {@code LowerCaseVar}, {@code NotEmptyVar}, {@code LenVar}, {@code SubstringVar}) were
	 * deleted in 10.0.0; their behavior is now covered by the {@code #{...}} script syntax.
	 */
	public static final VarResolver DEFAULT = create().defaultVars().defaultFunctions().build();

	/**
	 * Instantiates a new clean-slate {@link Builder} object.
	 *
	 * @return A new {@link Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	private static Var toVar(BeanStore bs, Object o) {
		if (o instanceof Class<?> o2) {
			@SuppressWarnings({
				"unchecked" // Cast is safe: parameterized by caller.
			})
			var subType = (Class<? extends Var>) o2;
			return BeanInstantiator.of(Var.class, bs).type(subType).run();
		}
		return (Var)o;
	}

	final Var[] vars;
	private final Map<String,Var> varMap;

	final VarFunction[] functions;
	private final Map<String,VarFunction> functionMap;

	final WritableBeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure on userBeans map: the runtime types match by construction
		"rawtypes"   // Same reason — Builder.bean(Class<T>, T) ensures Class/value pairing
	})
	protected VarResolver(Builder builder) {
		this.vars = builder.vars.stream().map(x -> toVar(builder.beanStore(), x)).toArray(Var[]::new);

		var m = new ConcurrentSkipListMap<String,Var>();
		for (var v : vars)
			m.put(v.getName(), v);

		this.varMap = u(m);
		this.functions = builder.functions.toArray(new VarFunction[0]);

		var fm = new ConcurrentSkipListMap<String,VarFunction>();
		for (var f : functions)
			fm.put(f.name(), f);
		this.functionMap = u(fm);

		var bs = new BasicBeanStore(builder.beanStore());
		builder.userBeans.forEach((c, v) -> bs.addBean((Class) c, v));
		this.beanStore = bs;
	}

	/**
	 * Adds a bean to this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type.
	 * @param value The bean.
	 * @return This object .
	 */
	public <T> VarResolver addBean(Class<T> c, T value) {
		beanStore.addBean(c, value);
		return this;
	}

	/**
	 * Returns a new builder object using the settings in this resolver as a base.
	 *
	 * @return A new var resolver builder.
	 */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Creates a new resolver session with no session objects.
	 *
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession() {
		return new VarResolverSession(this, null);
	}

	/**
	 * Same as {@link #createSession()} except allows you to specify a bean store for resolving beans.
	 *
	 * @param beanStore The bean store to associate with this session.
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession(WritableBeanStore beanStore) {
		return new VarResolverSession(this, beanStore);
	}

	/**
	 * Resolve variables in the specified string.
	 *
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolve(s);</code>.
	 * <br>This method can only be used if the string doesn't contain variables that rely on the existence of session
	 * variables.
	 *
	 * @param s The input string.
	 * @return The string with variables resolved, or the same string if it doesn't contain any variables to resolve.
	 */
	public String resolve(String s) {
		return createSession(null).resolve(s);
	}

	/**
	 * Compiles a template string into a reusable {@link VarTemplate}.
	 *
	 * <p>
	 * Compilation is the dual of {@link #resolve(String)} — instead of tokenizing and resolving
	 * in one pass, the input is parsed once and the result returned as an immutable
	 * {@link VarTemplate} that callers can reuse across many resolutions:
	 *
	 * <p class='bjava'>
	 * 	<jc>// Compile once...</jc>
	 * 	<jk>var</jk> <jv>tpl</jv> = <jv>vr</jv>.compile(<js>"hello, ${name:world}"</js>);
	 *
	 * 	<jc>// ...resolve many times.</jc>
	 * 	<jk>for</jk> (<jk>var</jk> <jv>req</jv> : <jv>requests</jv>) {
	 * 		<jk>var</jk> <jv>session</jv> = <jv>vr</jv>.createSession().bean(<jv>req</jv>);
	 * 		<jv>writer</jv>.write(<jv>tpl</jv>.resolve(<jv>session</jv>));
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The returned {@link VarTemplate} is immutable and threadsafe; the {@link VarResolverSession}
	 * passed to {@link VarTemplate#resolve(VarResolverSession)} inherits its own threadsafety
	 * contract (sessions are <b>not</b> threadsafe). Use {@link #resolveSupplier(String)} or
	 * {@link VarTemplate#asSupplierWithFreshSessions(VarResolver)} for a threadsafe re-evaluating
	 * Supplier.
	 *
	 * <h5 class='section'>Compile-binding contract:</h5>
	 * <p>
	 * The compiled template is bound to <i>this</i> resolver. Cached {@link Var} (and, in the
	 * future, function) references are resolved at compile time. Passing the compiled template
	 * to a session belonging to a different {@link VarResolver} is undefined behavior.
	 * Rebuilding the resolver invalidates compiled templates from the previous instance.
	 *
	 * <p>
	 * Templates are <b>not</b> cached on the resolver. Callers that compile the same input
	 * repeatedly (e.g. framework consumers compiling per {@code @Value} field) should cache
	 * the returned {@link VarTemplate} on their own per-site metadata.
	 *
	 * @param input The template string to compile. May be {@code null}.
	 * @return A new {@link VarTemplate} bound to this resolver.
	 */
	public VarTemplate compile(String input) {
		return VarTemplateCompiler.compile(this, input);
	}

	/**
	 * Returns a threadsafe {@link Supplier} that resolves the given template against this
	 * resolver each time {@link Supplier#get()} is called.
	 *
	 * <p>
	 * Each call to {@link Supplier#get()} opens a fresh {@link VarResolverSession}, so the
	 * returned {@code Supplier} is safe to share across threads — making it the recommended
	 * default for user-facing code (e.g. {@code @Value Supplier<String>} field types,
	 * hot-reload config patterns).
	 *
	 * <p>
	 * Equivalent to {@code compile(input).asSupplierWithFreshSessions(this)}.
	 *
	 * <p>
	 * For a session-bound (and therefore <i>not</i> threadsafe) variant, see
	 * {@link VarResolverSession#resolveSupplier(String)}.
	 *
	 * @param input The template string. May be {@code null}.
	 * @return A threadsafe {@code Supplier<String>}.
	 */
	public Supplier<String> resolveSupplier(String input) {
		return compile(input).asSupplierWithFreshSessions(this);
	}

	/**
	 * Resolve variables in the specified string and sends the results to the specified writer.
	 *
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolveTo(s, w);</code>.
	 * <br>This method can only be used if the string doesn't contain variables that rely on the existence of session
	 * variables.
	 *
	 * @param s The input string.
	 * @param w The writer to send the result to.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void resolveTo(String s, Writer w) throws IOException {
		createSession(null).resolveTo(s, w);
	}

	/**
	 * Returns an unmodifiable map of {@link Var Vars} associated with this context.
	 *
	 * @return A map whose keys are var names (e.g. <js>"S"</js>) and values are {@link Var} instances.
	 */
	protected Map<String,Var> getVarMap() { return varMap; }

	/**
	 * Returns an array of variables define in this variable resolver context.
	 *
	 * @return A new array containing the variables in this context.
	 */
	protected Var[] getVars() { return Arrays.copyOf(vars, vars.length); }

	/**
	 * Returns an unmodifiable map of {@link VarFunction VarFunctions} associated with this context.
	 *
	 * @return A map whose keys are function names (e.g. <js>"upper"</js>) and values are
	 *	 {@link VarFunction} instances.
	 */
	protected Map<String,VarFunction> getFunctionMap() { return functionMap; }
}
