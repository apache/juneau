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

import java.io.*;

/**
 * Abstract superclass of all Simple Var Language variables that resolve to simple returned string values.
 *
 * <p>
 * Note the difference between this class and {@link StreamedVar} that streams values to writers.
 * <br>Unlike the {@link StreamedVar} class, the returned value from this class can contain nested variables that will be
 * recursively resolved by {@link VarResolver}.
 *
 * <p>
 * Subclasses must implement the following method:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #resolve(VarResolverSession, String)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
09 */
public abstract class SimpleVar extends Var {

	/**
	 * Constructor.
	 *
	 * @param name The variable name (e.g. <js>"C"</js> for variables of the form <js>"$C{...}"</js>)
	 */
	protected SimpleVar(String name) {
		super(name, false);
	}

	@Override /* Var */
	public void resolveTo(VarResolverSession session, Writer w, String arg) throws Exception {
		throw new UnsupportedOperationException("Cannot call streamTo() on SimpleVar class");
	}
}
