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
package org.apache.juneau.commons.svl.vars;

import java.nio.file.*;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;

/**
 * Dotenv file variable resolver.
 *
 * <p>
 * The format for this var is <js>"$DE{key[,default]}"</js>.
 *
 * <p>
 * Values are resolved from {@link DotenvPropertySource} using either:
 * <ul class='spaced-list'>
 * 	<li>The default dotenv path resolution when using the no-arg constructor.
 * 	<li>The path provided via {@link #create(Path)}.
 * </ul>
 */
public class DotenvVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "DE";

	/**
	 * Creates a {@link DotenvVar} bound to a specific dotenv file path.
	 *
	 * @param path The dotenv file path.
	 * @return A new {@link DotenvVar} instance.
	 */
	public static DotenvVar create(Path path) {
		return new DotenvVar(path);
	}

	private final DotenvPropertySource source;

	/**
	 * Constructor.
	 */
	public DotenvVar() {
		super(NAME);
		this.source = new DotenvPropertySource();
	}

	private DotenvVar(Path path) {
		super(NAME);
		this.source = new DotenvPropertySource(path);
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var result = source.get(key);
		return result.isPresent() ? result.value().orElse(null) : null;
	}
}
