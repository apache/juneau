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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.svl.vars.*;

/**
 * Simple list of variables that can consist of either variable classes or instances.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 *
 * @serial exclude
 */
public class VarList extends ArrayList<Object> {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns an empty list of variables.
	 *
	 * @return A new empty list of variables.
	 */
	public static VarList create() {
		return new VarList();
	}

	/**
	 * Creates a new list of variables.
	 *
	 * @param vars The variables to create.
	 * @return A new list of variables.
	 */
	@SafeVarargs
	public static final VarList of(Class<? extends Var>...vars) {
		return create().append(vars);
	}

	/**
	 * Creates a new list of variables.
	 *
	 * @param vars The variables to create.
	 * @return A new list of variables.
	 */
	public static VarList of(Var...vars) {
		return create().append(vars);
	}

	/**
	 * Constructor.
	 */
	protected VarList() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The list to copy.
	 */
	protected VarList(VarList copyFrom) {
		super(copyFrom);
	}

	/**
	 * Adds the default variables to this list.
	 *
	 * <p>
	 * The default variables are:
	 * <ul>
	 * 	<li>{@link PropertyVar}
	 * 	<li>{@link SystemPropertiesVar}
	 * 	<li>{@link EnvVariablesVar}
	 * 	<li>{@link EnvFileVar}
	 * 	<li>{@link DotenvVar}
	 * 	<li>{@link ArgsVar}
	 * 	<li>{@link ManifestFileVar}
	 * </ul>
	 *
	 * <p>
	 * The 11 transformation/conditional {@code Var}s that previously lived here
	 * ({@code SwitchVar}, {@code IfVar}, {@code CoalesceVar}, {@code PatternMatchVar},
	 * {@code PatternReplaceVar}, {@code PatternExtractVar}, {@code UpperCaseVar},
	 * {@code LowerCaseVar}, {@code NotEmptyVar}, {@code LenVar}, {@code SubstringVar}) were
	 * deleted in 10.0.0 and replaced by the {@code #{...}} script syntax. The
	 * built-in {@code VarFunction} catalog is registered separately via
	 * {@link VarResolver.Builder#defaultFunctions()}.
	 *
	 * @return This object.
	 */
	public VarList addDefault() {
		// @formatter:off
		return append(
			PropertyVar.class,
			SystemPropertiesVar.class,
			EnvVariablesVar.class,
			EnvFileVar.class,
			DotenvVar.class,
			ManifestFileVar.class,
			ArgsVar.class
		);
		// @formatter:on
	}

	/**
	 * Adds a list of variables to this list.
	 *
	 * @param vars The variables to append to this list.
	 * @return This object.
	 */
	@SafeVarargs
	public final VarList append(Class<? extends Var>...vars) {
		addAll(l(vars));
		return this;
	}

	/**
	 * Adds a list of variables to this list.
	 *
	 * @param vars The variables to append to this list.
	 * @return This object.
	 */
	public VarList append(Var...vars) {
		addAll(l(vars));
		return this;
	}

	/**
	 * Adds a list of variables to this list.
	 *
	 * @param vars The variables to append to this list.
	 * @return This object.
	 */
	public VarList append(VarList vars) {
		addAll(vars);
		return this;
	}

	/**
	 * Makes a copy of this list.
	 *
	 * @return A new copy of this list.
	 */
	public VarList copy() {
		return new VarList(this);
	}
}
