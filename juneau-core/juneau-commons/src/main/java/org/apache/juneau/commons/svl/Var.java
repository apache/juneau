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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;

/**
 * Abstract superclass of all Simple Var Language variables.
 *
 * <p>
 * Vars are used to convert simple variables of the form <js>"$varName{varKey}"</js> into something else by the
 * {@link VarResolver} class.
 *
 * <p>
 * Subclasses must implement one of the following two methods:
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #resolve(VarResolverSession,String)} - For simple vars.
 * 	<li class='jm'>{@link #resolveTo(VarResolverSession,Writer,String)} - For streamed vars.
 * </ul>
 *
 * <p>
 * Subclasses MUST implement a no-arg constructor so that class names can be passed to the
 * {@link VarResolver.Builder#vars(Class...)} method.
 * <br><b>They must also be thread safe!</b>
 *
 * <p>
 * Two direct abstract subclasses are provided to differentiated between simple and streamed vars:
 * <ul class='javatree'>
 * 	<li class='jac'>{@link SimpleVar}
 * 	<li class='jac'>{@link StreamedVar}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"java:S1452"  // Wildcard may appear in subclass method signatures
})
public abstract class Var {

	// Argument name constants for assertArgNotNull
	private static final String ARG_name = "name";

	private final String name;
	final boolean streamed;

	/**
	 * Constructor.
	 *
	 * @param name The name of this variable.
	 * @param streamed
	 * 	Whether this variable is 'streamed', meaning the {@link #resolveTo(VarResolverSession, Writer, String)} method
	 * 	is implemented.
	 * 	If <jk>false</jk>, then the {@link #resolve(VarResolverSession, String)} method is implemented.
	 */
	protected Var(String name, boolean streamed) {
		assertArgNotNull(ARG_name, name);
		this.name = name;
		this.streamed = streamed;

		for (var i = 0; i < name.length(); i++) {
			// Need to make sure only ASCII characters are used.
			var c = name.charAt(i);
			if (c < 'A' || c > 'z' || (c > 'Z' && c < 'a'))
				throw iaex("Invalid var name.  Must consist of only uppercase and lowercase ASCII letters.");
		}
	}

	/**
	 * The interface that needs to be implemented for subclasses of {@link SimpleVar}.
	 *
	 * @param session The session object used for a single instance of a var resolution.
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 * @throws Exception Any exception can be thrown.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	public abstract String resolve(VarResolverSession session, String arg) throws Exception;

	/**
	 * The interface that needs to be implemented for subclasses of {@link StreamedVar}.
	 *
	 * @param session The session object used for a single instance of a var resolution.
	 * @param w The writer to send the resolved value to.
	 * @param arg The inside argument of the variable.
	 * @throws Exception Any exception can be thrown.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	public abstract void resolveTo(VarResolverSession session, Writer w, String arg) throws Exception;

	/**
	 * Returns whether nested variables are supported by this variable.
	 *
	 * <p>
	 * For example, in <js>"$X{$Y{xxx}}"</js>, $Y is a nested variable that will be resolved if this method returns
	 * <jk>true</jk>.
	 *
	 * <p>
	 * The default implementation of this method always returns <jk>true</jk>.
	 * Subclasses can override this method to override the default behavior.
	 *
	 * @return <jk>true</jk> if nested variables are supported by this variable.
	 */
	protected boolean allowNested() {
		return true;
	}

	/**
	 * Returns whether variables in the resolved contents of this variable should also be resolved.
	 *
	 * <p>
	 * For example, if <js>"$X{xxx}"</js> resolves to <js>"$Y{xxx}"</js>, then the $Y variable will be recursively
	 * resolved if this method returns <jk>true</jk>.
	 *
	 * <p>
	 * The default implementation of this method always returns <jk>true</jk>.
	 * <br>Subclasses can override this method to override the default behavior.
	 *
	 * <div class='warn'>
	 * As a general rule, variables that resolve user-entered data should not be recursively resolved as this may
	 * cause a security hole.
	 * </div>
	 *
	 * @return <jk>true</jk> if resolved variables should be recursively resolved.
	 */
	protected boolean allowRecurse() {
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this variable can be resolved in the specified session.
	 *
	 * <p>
	 * For example, some variable cannot resolve unless specific context or session objects are available.
	 *
	 * @param session The current session.
	 * @return <jk>true</jk> if this variable can be resolved in the specified session.
	 */
	protected boolean canResolve(VarResolverSession session) {
		return true;
	}

	/**
	 * Returns {@code true} if this variable's resolved value is stable for the lifetime of the
	 * resolver — same body produces the same output regardless of when (or in what session) it
	 * is resolved.
	 *
	 * <p>
	 * Stable vars opt in to <b>compile-time folding</b> by {@link VarTemplateCompiler}: rather
	 * than producing a {@link VarRefSegment} that dispatches at resolve time, the compiler
	 * resolves the var <i>once</i> at compile time and stores a {@link LiteralSegment} carrying
	 * the resolved value. This eliminates the per-resolve dispatch cost for hot paths like
	 * {@code @Value("${HOSTNAME:localhost}")} where the underlying var (env / system property
	 * / etc.) is treated as a build-time constant by the application.
	 *
	 * <p>
	 * Default: {@code false}. Vars that read mutable session-scoped state (request beans, args,
	 * config files) <b>must</b> leave this {@code false} — folding would cache an arbitrary
	 * resolution as a literal and break per-session correctness.
	 *
	 * <p>
	 * Common opt-ins:
	 * <ul>
	 * 	<li>{@link org.apache.juneau.commons.svl.vars.SystemPropertiesVar SystemPropertiesVar} — system
	 * 		properties are mutable in principle (via {@link System#setProperty}) but treated as
	 * 		stable in normal application code; if a deployment relies on post-resolver mutations,
	 * 		opt-out by sub-classing.
	 * 	<li>{@link org.apache.juneau.commons.svl.vars.EnvVariablesVar EnvVariablesVar} — process
	 * 		environment is immutable.
	 * 	<li>{@link org.apache.juneau.commons.svl.vars.DotenvVar DotenvVar} /
	 * 		{@link org.apache.juneau.commons.svl.vars.EnvFileVar EnvFileVar} — .env files are read
	 * 		once at startup.
	 * </ul>
	 *
	 * <h5 class='section'>Folding contract:</h5>
	 * <p>
	 * When a {@link Var} returns {@code true} here, the compiler invokes
	 * {@link #doResolve(VarResolverSession, String)} on a no-bean session at compile time.
	 * Implementations must not throw on absent session beans during stable folding; if the
	 * stable resolution would itself depend on session state (i.e. it isn't really stable),
	 * either return {@code false} from this method or throw a {@link VarResolverException}
	 * from {@link #doResolve(VarResolverSession, String)} which the compiler treats as
	 * "cannot fold; emit a normal {@link VarRefSegment}".
	 *
	 * @return {@code true} if this variable's resolution is invariant across sessions.
	 */
	protected boolean isStable() {
		return false;
	}

	/**
	 * The method called from {@link VarResolver}.
	 *
	 * <p>
	 * Can be overridden to intercept the request and do special handling.
	 * <br>Default implementation simply calls resolve(String).
	 *
	 * @param session The session object used for a single instance of a string resolution.
	 * @param arg The inside argument of the variable.
	 * @return The resolved value.
	 * @throws Exception Any exception can be thrown.
	 */
	protected String doResolve(VarResolverSession session, String arg) throws Exception {
		return resolve(session, arg);
	}

	/**
	 * Return the name of this variable.
	 *
	 * <p>
	 * For example, the system property variable returns <js>"S"</js> since the format of the variable is
	 * <js>"$S{system.property}"</js>.
	 *
	 * @return The name of this variable.
	 */
	protected String getName() { return name; }
}
