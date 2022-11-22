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

/**
 * Abstract superclass of all Simple Var Language variables that write their values directly to a writer.
 *
 * <p>
 * Note the difference between this class and {@link SimpleVar} that returns simple string values.
 * <br>Unlike the {@link SimpleVar} class, the output from this class cannot contain nested variables.
 * <br>However, this class can be more efficient for variables that produce large amounts of output so that the creation
 * of large in-memory strings is avoided.
 *
 * <p>
 * Subclasses must implement the following method:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #resolveTo(VarResolverSession, java.io.Writer, String)}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SimpleVariableLanguage">Simple Variable Language</a>
 * </ul>
 */
public abstract class StreamedVar extends Var {

	/**
	 * Constructor.
	 *
	 * @param name The variable name (e.g. <js>"C"</js> for variables of the form <js>"$C{...}"</js>)
	 */
	public StreamedVar(String name) {
		super(name, true);
	}

	@Override /* Var */
	public String resolve(VarResolverSession session, String arg) throws Exception {
		throw new UnsupportedOperationException("Cannot call resolve() on StreamedVar class");
	}
}
