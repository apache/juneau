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
import org.apache.juneau.svl.vars.*;

/**
 * Builder class for building instances of {@link VarResolver}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc VarResolvers}
 * </ul>
 */
public class VarResolverBuilder {

	private final List<Var> vars = AList.of();
	private final Map<String,Object> contextObjects = new HashMap<>();

	/**
	 * Create a new var resolver using the settings in this builder.
	 *
	 * @return A new var resolver.
	 */
	public VarResolver build() {
		return new VarResolver(vars.toArray(new Var[vars.size()]), contextObjects);
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
	 * Associates a context object with this resolver.
	 *
	 * <p>
	 * A context object is essentially some environmental object that doesn't change but is used by vars to customize
	 * output.
	 *
	 * @param name The name of the context object.
	 * @param object The context object.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder contextObject(String name, Object object) {
		contextObjects.put(name, object);
		return this;
	}

	/**
	 * Associates multiple context objects with this resolver.
	 *
	 * <p>
	 * A context object is essentially some environmental object that doesn't change but is used by vars to customize
	 * output.
	 *
	 * @param map A map of context objects keyed by their name.
	 * @return This object (for method chaining).
	 */
	public VarResolverBuilder contextObjects(Map<String,Object> map) {
		contextObjects.putAll(map);
		return this;
	}
}