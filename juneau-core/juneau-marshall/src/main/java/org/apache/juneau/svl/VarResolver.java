// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.svl;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.vars.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public class VarResolver {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Default string variable resolver with support for system properties and environment variables:
	 *
	 * <ul>
	 * 	<li><c>$S{key[,default]}</c> - {@link SystemPropertiesVar}
	 * 	<li><c>$E{key[,default]}</c> - {@link EnvVariablesVar}
	 * 	<li><c>$A{key[,default]}</c> - {@link ArgsVar}
	 * 	<li><c>$MF{key[,default]}</c> - {@link ManifestFileVar}
	 * 	<li><c>$SW{stringArg,pattern:thenValue[,pattern:thenValue...]}</c> - {@link SwitchVar}
	 * 	<li><c>$IF{arg,then[,else]}</c> - {@link IfVar}
	 * 	<li><c>$CO{arg[,arg2...]}</c> - {@link CoalesceVar}
	 * 	<li><c>$PM{arg,pattern}</c> - {@link PatternMatchVar}
	 * 	<li><c>$PR{stringArg,pattern,replace}</c>- {@link PatternReplaceVar}
	 * 	<li><c>$PE{arg,pattern,groupIndex}</c> - {@link PatternExtractVar}
	 * 	<li><c>$UC{arg}</c> - {@link UpperCaseVar}
	 * 	<li><c>$LC{arg}</c> - {@link LowerCaseVar}
	 * 	<li><c>$NE{arg}</c> - {@link NotEmptyVar}
	 * 	<li><c>$LN{arg[,delimiter]}</c> - {@link LenVar}
	 * 	<li><c>$ST{arg,start[,end]}</c> - {@link SubstringVar}
	 * </ul>
	 */
	public static final VarResolver DEFAULT = create().defaultVars().build();

	/**
	 * Instantiates a new clean-slate {@link Builder} object.
	 *
	 * @return A new {@link Builder} object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<VarResolver> {

		final VarList vars;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(VarResolver.class, BeanStore.create().build());
			vars = VarList.create();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(VarResolver copyFrom) {
			super(copyFrom.getClass(), copyFrom.beanStore);
			vars = VarList.of(copyFrom.vars);
		}

		@Override /* BeanBuilder */
		protected VarResolver buildDefault() {
			return new VarResolver(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

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
		 * Adds the default variables to this builder.
		 *
		 * <p>
		 * The default variables are:
		 * <ul>
		 * 	<li>{@link SystemPropertiesVar}
		 * 	<li>{@link EnvVariablesVar}
		 * 	<li>{@link ArgsVar}
		 * 	<li>{@link ManifestFileVar}
		 * 	<li>{@link SwitchVar}
		 * 	<li>{@link IfVar}
		 * 	<li>{@link CoalesceVar}
		 * 	<li>{@link PatternMatchVar}
		 * 	<li>{@link PatternReplaceVar}
		 * 	<li>{@link PatternExtractVar}
		 * 	<li>{@link UpperCaseVar}
		 * 	<li>{@link LowerCaseVar}
		 * 	<li>{@link NotEmptyVar}
		 * 	<li>{@link LenVar}
		 * 	<li>{@link SubstringVar}
		 * </ul>
		 *
		 * @return This object .
		 */
		public Builder defaultVars() {
			vars.addDefault();
			return this;
		}

		/**
		 * Adds a bean to the bean store in this session.
		 *
		 * @param <T> The bean type.
		 * @param c The bean type.
		 * @param value The bean.
		 * @return This object .
		 */
		public <T> Builder bean(Class<T> c, T value) {
			beanStore().addBean(c, value);
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final Var[] vars;
	private final Map<String,Var> varMap;
	final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected VarResolver(Builder builder) {
		this.vars = builder.vars.stream().map(x -> toVar(builder.beanStore(),x)).toArray(Var[]::new);

		Map<String,Var> m = new ConcurrentSkipListMap<>();
		for (Var v : vars)
			m.put(v.getName(), v);

		this.varMap = unmodifiable(m);
		this.beanStore = BeanStore.of(builder.beanStore());
	}

	private static Var toVar(BeanStore bs, Object o) {
		if (o instanceof Class)
			return bs.createBean(Var.class).type((Class<?>)o).run();
		return (Var)o;
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
	 * Returns an unmodifiable map of {@link Var Vars} associated with this context.
	 *
	 * @return A map whose keys are var names (e.g. <js>"S"</js>) and values are {@link Var} instances.
	 */
	protected Map<String,Var> getVarMap() {
		return varMap;
	}

	/**
	 * Returns an array of variables define in this variable resolver context.
	 *
	 * @return A new array containing the variables in this context.
	 */
	protected Var[] getVars() {
		return Arrays.copyOf(vars, vars.length);
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
	public VarResolverSession createSession(BeanStore beanStore) {
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
}