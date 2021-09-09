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

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.svl.vars.*;

/**
 * Builder class for building instances of {@link VarResolver}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc VarResolvers}
 * </ul>
 */
public class VarResolverBuilder {

	private final List<Var> vars;
	private BeanStore beanStore;

	/**
	 * Constructor.
	 */
	public VarResolverBuilder() {
		vars = AList.create();
		beanStore = BeanStore.create().build();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public VarResolverBuilder(VarResolver copyFrom) {
		vars = new ArrayList<>(Arrays.asList(copyFrom.ctx.vars));
		beanStore = BeanStore.of(copyFrom.ctx.beanStore);
	}

	/**
	 * Create a new var resolver using the settings in this builder.
	 *
	 * @return A new var resolver.
	 */
	public VarResolver build() {
		return new VarResolver(vars.toArray(new Var[vars.size()]), beanStore);
	}

	/**
	 * Register new variables with this resolver.
	 *
	 * @param vars
	 * 	The variable resolver classes.
	 * 	These classes must subclass from {@link Var} and have no-arg constructors.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder vars(Class<?>...vars) {
		for (Class<?> v : vars)
			this.vars.add(castOrCreate(Var.class, v));
		return this;
	}

	/**
	 * Register new variables with this resolver.
	 *
	 * @param vars
	 * 	The variable resolver classes.
	 * 	These classes must subclass from {@link Var} and have no-arg constructors.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder vars(Var...vars) {
		this.vars.addAll(Arrays.asList(vars));
		return this;
	}

	/**
	 * Register new variables with this resolver.
	 *
	 * @param vars
	 * 	The variable resolver classes.
	 * 	These classes must subclass from {@link Var} and have no-arg constructors.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder vars(VarList vars) {
		for (Object o : vars)
			this.vars.add(castOrCreate(Var.class, o));
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
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder defaultVars() {
		return vars(
			SystemPropertiesVar.class,
			EnvVariablesVar.class,
			ManifestFileVar.class,
			ArgsVar.class,
			SwitchVar.class,
			IfVar.class,
			CoalesceVar.class,
			PatternMatchVar.class,
			PatternReplaceVar.class,
			PatternExtractVar.class,
			UpperCaseVar.class,
			LowerCaseVar.class,
			NotEmptyVar.class,
			LenVar.class,
			SubstringVar.class);
	}

	/**
	 * Associates a bean store with this builder.
	 *
	 * @param value The bean store to associate with this var resolver.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder beanStore(BeanStore value) {
		this.beanStore = BeanStore.of(value);
		return this;
	}

	/**
	 * Adds a bean to the bean store in this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type.
	 * @param value The bean.
	 * @return This object (for method chaining).
	 */
	public <T> VarResolverBuilder bean(Class<T> c, T value) {
		beanStore.addBean(c, value);
		return this;
	}
}