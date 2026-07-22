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

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;

/**
 * System property variable resolver.
 *
 * <p>
 * The format for this var is <js>"$S{systemProperty[,defaultValue]}"</js>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a variable resolver that resolves system properties (e.g. "$S{java.home}")</jc>
 * 	VarResolver <jv>varResolver</jv> = VarResolver.<jsm>create</jsm>().vars(SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(<jv>varResolver</jv>.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 *
 * <p>
 * Since this is a {@link SimpleVar}, any variables contained in the result will be recursively resolved.
 * <br>Likewise, if the arguments contain any variables, those will be resolved before they are passed to this var.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 */
public class SystemPropertiesVar extends DefaultingVar {
	private final SystemPropertyPropertySource source = new SystemPropertyPropertySource();


	/** The name of this variable. */
	public static final String NAME = "S";

	/**
	 * Constructor.
	 */
	public SystemPropertiesVar() {
		super(NAME);
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var v = source.get(key);
		return v.isPresent() ? v.value().orElse(null) : null;
	}

	/**
	 * System properties are mutable in principle (via {@link System#setProperty}) but treated
	 * as stable in normal application code. This var opts in to compile-time stable-value
	 * folding.
	 *
	 * <h5 class='section'>Caveat:</h5>
	 * <p>
	 * If a deployment relies on {@link System#setProperty} mutations <i>after</i>
	 * {@link VarResolver} build, those mutations will <b>not</b> be picked up by templates
	 * that were compiled before the mutation. Either re-build the resolver after the property
	 * change, or override this method to return {@code false} via a subclass.
	 *
	 * @return Always {@code true} for the default implementation.
	 */
	@Override /* Overridden from Var */
	protected boolean isStable() {
		return true;
	}
}
