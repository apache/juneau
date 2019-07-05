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
import java.util.*;

import org.apache.juneau.svl.vars.*;

/**
 * Utility class for resolving variables of the form <js>"$X{key}"</js> in strings.
 *
 * <p>
 * Variables are of the form <c>$X{key}</c>, where <c>X</c> can consist of zero or more ASCII characters.
 * <br>The variable key can contain anything, even nested variables that get recursively resolved.
 *
 * <p>
 * Variables are defined through the {@link VarResolverBuilder#vars(Class[])} method.
 *
 * <p>
 * The {@link Var} interface defines how variables are converted to values.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> SystemPropertiesVar <jk>extends</jk> SimpleVar {
 *
 * 		<jc>// Must have a no-arg constructor!</jc>
 * 		<jk>public</jk> SystemPropertiesVar() {
 * 			<jk>super</jk>(<js>"S"</js>);
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> String resolve(VarResolverSession session, String varVal) {
 * 			<jk>return</jk> System.<jsm>getProperty</jsm>(varVal);
 * 		}
 * 	}
 *
 * 	<jc>// Create a variable resolver that resolves system properties (e.g. "$S{java.home}")</jc>
 * 	VarResolver r = VarResolver.<jsm>create</jsm>().vars(SystemPropertiesVar.<jk>class</jk>).build();
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-svl.VarResolvers}
 * </ul>
 */
public class VarResolver {

	/**
	 * Default string variable resolver with support for system properties and environment variables:
	 *
	 * <ul>
	 * 	<li><c>$S{key[,default]}</c> - {@link SystemPropertiesVar}
	 * 	<li><c>$E{key[,default]}</c> - {@link EnvVariablesVar}
	 * 	<li><c>$A{key[,default]}</c> - {@link ArgsVar}
	 * 	<li><c>$MF{key[,default]}</c> - {@link ManifestFileVar}
	 * 	<li><c>$SW{stringArg,pattern:thenValue[,pattern:thenValue...]}</c> - {@link SwitchVar}
	 * 	<li><c>$IF{arg,then[,else]}</c> - {@link IfVar}
	 * 	<li><c>$CO{arg[,arg2...]}</c> - {@link CoalesceVar}
	 * 	<li><c>$PM{arg,pattern}</c> - {@link PatternMatchVar}
	 * 	<li><c>$PR{stringArg,pattern,replace}</c>- {@link PatternReplaceVar}
	 * 	<li><c>$PE{arg,pattern,groupIndex}</c> - {@link PatternExtractVar}
	 * 	<li><c>$UC{arg}</c> - {@link UpperCaseVar}
	 * 	<li><c>$LC{arg}</c> - {@link LowerCaseVar}
	 * 	<li><c>$NE{arg}</c> - {@link NotEmptyVar}
	 * 	<li><c>$LN{arg[,delimiter]}</c> - {@link LenVar}
	 * 	<li><c>$ST{arg,start[,end]}</c> - {@link SubstringVar}
	 * </ul>
	 */
	public static final VarResolver DEFAULT = new VarResolverBuilder().defaultVars().build();

	final VarResolverContext ctx;

	/**
	 * Instantiates a new clean-slate {@link VarResolverBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> VarResolverBuilder()</code>.
	 *
	 * @return A new {@link VarResolverBuilder} object.
	 */
	public static VarResolverBuilder create() {
		return new VarResolverBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param vars The var classes
	 * @param contextObjects
	 */
	VarResolver(Class<? extends Var>[] vars, Map<String,Object> contextObjects) {
		this.ctx = new VarResolverContext(vars, contextObjects);
	}

	/**
	 * Returns a new builder object using the settings in this resolver as a base.
	 *
	 * @return A new var resolver builder.
	 */
	public VarResolverBuilder builder() {
		return new VarResolverBuilder()
			.vars(ctx.getVars())
			.contextObjects(ctx.getContextObjects());
	}

	/**
	 * Returns the read-only properties on this variable resolver.
	 *
	 * @return The read-only properties on this variable resolver.
	 */
	public VarResolverContext getContext() {
		return ctx;
	}

	/**
	 * Creates a new resolver session with no session objects.
	 *
	 * <p>
	 * Session objects can be associated with the specified session using the {@link VarResolverSession#sessionObject(String, Object)}
	 * method.
	 *
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession() {
		return new VarResolverSession(ctx, null);
	}

	/**
	 * Same as {@link #createSession()} except allows you to specify session objects as a map.
	 *
	 * @param sessionObjects The session objects to associate with the session.
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession(Map<String,Object> sessionObjects) {
		return new VarResolverSession(ctx, sessionObjects);
	}

	/**
	 * Resolve variables in the specified string.
	 *
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolve(s);</code>.
	 * <br>This method can only be used if the string doesn't contain variables that rely on the existence of session
	 * variables.
	 *
	 * @param s The input string.
	 * @return The string with variables resolved, or the same string if it doesn't contain any variables to resolve.
	 */
	public String resolve(String s) {
		return createSession(null).resolve(s);
	}

	/**
	 * Resolve variables in the specified string and sends the results to the specified writer.
	 *
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolveTo(s, w);</code>.
	 * <br>This method can only be used if the string doesn't contain variables that rely on the existence of session
	 * variables.
	 *
	 * @param s The input string.
	 * @param w The writer to send the result to.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void resolveTo(String s, Writer w) throws IOException {
		createSession(null).resolveTo(s, w);
	}
}