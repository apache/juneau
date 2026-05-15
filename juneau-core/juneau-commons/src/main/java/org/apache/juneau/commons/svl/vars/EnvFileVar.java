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
 * Environment-file variable resolver.
 *
 * <p>
 * The format for this var is <js>"$EF{key[,default]}"</js>.
 *
 * <p>
 * This variable is functionally identical to {@link DotenvVar}, but provides a
 * generic env-file naming option for callers that do not want dotenv-specific naming.
 */
public class EnvFileVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "EF";

	/**
	 * Creates an {@link EnvFileVar} bound to a specific env file path.
	 *
	 * @param path The env file path.
	 * @return A new {@link EnvFileVar} instance.
	 */
	public static EnvFileVar create(Path path) {
		return new EnvFileVar(path);
	}

	private final DotenvPropertySource source;

	/**
	 * Constructor.
	 */
	public EnvFileVar() {
		super(NAME);
		this.source = new DotenvPropertySource();
	}

	private EnvFileVar(Path path) {
		super(NAME);
		this.source = new DotenvPropertySource(path);
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var result = source.get(key);
		return result.isPresent() ? result.value().orElse(null) : null;
	}
}
