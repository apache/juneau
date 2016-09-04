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

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;

/**
 * Abstract superclass of all Simple Var Language variables.
 * <p>
 * Vars are used to convert simple variables of the form <js>"$varName{varKey}"</js> into
 * 	something else by the {@link VarResolver} class.
 * <p>
 * Subclasses must implement one of the following two methods:
 * <ul>
 * 	<li>{@link #resolve(VarResolverSession,String)} - For simple vars.
 * 	<li>{@link #resolveTo(VarResolverSession,Writer,String)} - For streamed vars.
 * </ul>
 * Subclasses MUST implement a no-arg constructor so that class names can be passed to the {@link VarResolver#addVars(Class...)} method.
 * They must also be thread safe!
 * <p>
 * Two direct abstract subclasses are provided to differentiated between simple and streamed vars:
 * <ul>
 * 	<li>{@link SimpleVar}
 * 	<li>{@link StreamedVar}
 * </ul>
 *
 * @see org.apache.juneau.svl
 * @author James Bognar (james.bognar@salesforce.com)
 */
public abstract class Var {

	private final String name;
	final boolean streamed;

	/**
	 * Constructor.
	 *
	 * @param name The name of this variable.
	 * @param streamed Whether this variable is 'streamed', meaning the {@link #resolveTo(VarResolverSession, Writer, String)}
	 * 	method is implemented.  If <jk>false</jk>, then the {@link #resolve(VarResolverSession, String)} method is implemented.
	 */
	public Var(String name, boolean streamed) {
		this.name = name;
		this.streamed = streamed;

		if (name == null)
			illegalArg("Invalid var name.  Must consist of only uppercase and lowercase ASCII letters.");
		else for (int i = 0; i < name.length(); i++) {
		// Need to make sure only ASCII characters are used.
			char c = name.charAt(i);
			if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a'))
				illegalArg("Invalid var name.  Must consist of only uppercase and lowercase ASCII letters.");
		}
	}

	/**
	 * Return the name of this variable.
	 * <p>
	 * For example, the system property variable returns <js>"S"</js> since the format of the
	 * 	variable is <js>"$S{system.property}"</js>.
	 *
	 * @return The name of this variable.
	 */
	protected String getName() {
		return name;
	}

	/**
	 * The method called from {@link VarResolver}.
	 * Can be overridden to intercept the request and do special handling.
	 * Default implementation simply calls resolve(String).
	 *
	 * @param session The session object used for a single instance of a string resolution.
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 */
	protected String doResolve(VarResolverSession session, String arg) {
		return resolve(session, arg);
	}

	/**
	 * The interface that needs to be implemented for subclasses of {@link SimpleVar}.
	 *
	 * @param session The session object used for a single instance of a var resolution.
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 */
	public abstract String resolve(VarResolverSession session, String arg);

	/**
	 * The interface that needs to be implemented for subclasses of {@link StreamedVar}.
	 *
	 * @param session The session object used for a single instance of a var resolution.
	 * @param w The writer to send the resolved value to.
	 * @param arg The inside argument of the variable.
	 */
	public abstract void resolveTo(VarResolverSession session, Writer w, String arg);
}
