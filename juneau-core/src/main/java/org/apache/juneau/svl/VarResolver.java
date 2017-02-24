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

import static java.text.MessageFormat.*;
import static org.apache.juneau.svl.VarResolverContext.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.svl.vars.*;

/**
 * Utility class for resolving variables of the form <js>"$X{key}"</js> in strings.
 * <p>
 * Variables are of the form <code>$X{key}</code>, where <code>X</code> can consist of zero or more ASCII characters.<br>
 * 	The variable key can contain anything, even nested variables that get recursively resolved.
 * <p>
 * Variables are defined through the {@link #addVars(Class[])} method.
 * <p>
 * The {@link Var} interface defines how variables are converted to values.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
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
 * 	VarResolver r = <jk>new</jk> VarResolver().addVars(SystemPropertiesVar.<js>class</js>);
 *
 * 	<jc>// Use it!</jc>
 * 	System.<jsf>out</jsf>.println(r.resolve(<js>"java.home is set to $S{java.home}"</js>));
 * </p>
 *
 * <h6 class='topic'>Context objects</h6>
 * <p>
 * Var resolvers can have zero or more context objects associated with them.
 * <p>
 * Context objects are arbitrary objects associated with this var resolver, such as
 * 	a {@link ConfigFile} object.
 * They are set through the {@link #setContextObject(String, Object)} method.
 * They can be any class type.
 * <p>
 * Context objects can be retrieved by {@link Var} classes through the {@link VarResolverSession#getSessionObject(Class, String)} method.
 *
 * <h6 class='topic'>Session objects</h6>
 * <p>
 * Session objects are considered more ephemeral than context objects.
 * While a context object is unlikely to ever change, a session object may change on every
 * 	use of the var resolver.
 * For example, the server API defines various <code>Var</code> objects that use the <code>RestRequest</code>
 * 	object as a session object for the duration of a single HTTP request.
 * <p>
 * Session objects are used by calling the {@link #createSession()} or {@link #createSession(Map)} methods to create an instance
 * 	of a {@link VarResolverSession} object that contains {@link VarResolverSession#resolve(String)} and {@link VarResolverSession#resolveTo(String,Writer)} methods
 * 	that are identical to {@link VarResolver#resolve(String)} and {@link VarResolver#resolveTo(String, Writer)} except that the <code>Var</code> objects
 * 	have access to the session objects through the {@link VarResolverSession#getSessionObject(Class, String)} method.
 * <p>
 * Session objects are specified through either the {@link #createSession(Map)} method or the {@link VarResolverSession#setSessionObject(String, Object)} methods.
 *
 * <h6 class='topic'>Cloning</h6>
 * <p>
 * Var resolvers can be cloned by using the {@link #clone()} method.
 * Cloning a resolver will copy it's {@link Var} class names and context objects.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Create a resolver that copies the default resolver and adds $C and $ARG vars.</jc>
 * 	VarResolver myVarResolver = VarResolver.<jsf>DEFAULT</jsf>.clone().addVars(ConfigVar.<jk>class</jk>, ArgsVar.<jk>class</jk>);
 * </p>
 *
 * @see org.apache.juneau.svl
 */
public class VarResolver extends CoreApi {

	/**
	 * Default string variable resolver with support for system properties and environment variables:
	 * <p>
	 * <ul>
	 * 	<li><code>$S{key}</code>,<code>$S{key,default}</code> - System properties.
	 * 	<li><code>$E{key}</code>,<code>$E{key,default}</code> - Environment variables.
	 * 	<li><code>$IF{booleanValue,thenValue[,elseValue]}</code> - If-else patterns.
	 * 	<li><code>$SWITCH{test,matchPattern,thenValue[,matchPattern,thenValue][,elseValue]}</code> - Switch patterns.
	 * </ul>
	 *
	 * @see SystemPropertiesVar
	 * @see EnvVariablesVar
	 */
	public static final VarResolver DEFAULT = new VarResolver().addVars(SystemPropertiesVar.class, EnvVariablesVar.class, SwitchVar.class, IfVar.class).lock();

	/**
	 * Construct an empty var resolver with no vars.
	 */
	public VarResolver() {}

	/**
	 * Register new variables with this resolver.
	 *
	 * @param vars The variable resolver classes.
	 * These classes must subclass from {@link Var} and have no-arg constructors.
	 * @return This object (for method chaining).
	 */
	public VarResolver addVars(Class<?>...vars) {
		checkLock();
		ContextFactory cf = getContextFactory();
		for (Class<?> v : vars) {
			try {
				v.newInstance();
			} catch (InstantiationException e) {
				throw new UnsupportedOperationException(format("Cannot instantiate variable class {0}.  Must have a public no-arg constructor.", v.getName()));
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			cf.addToProperty(SVL_vars, v);
		}
		return this;
	}

	/**
	 * Associates a context object with this resolver.
	 *
	 * @param name The name of the context object.
	 * @param object The context object.
	 * @return This object (for method chaining).
	 */
	public VarResolver setContextObject(String name, Object object) {
		getContextFactory().putToProperty(SVL_context, name, object);
		return this;
	}

	/**
	 * Creates a new resolver session with no session objects.
	 * <p>
	 * Session objects can be associated with the specified session using the {@link VarResolverSession#setSessionObject(String, Object)} method.
	 *
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession() {
		return new VarResolverSession(getContext(VarResolverContext.class), null);
	}

	/**
	 * Same as {@link #createSession()} except allows you to specify session objects as a map.
	 *
	 * @param sessionObjects The session objects to associate with the sessino.
	 * @return A new resolver session.
	 */
	public VarResolverSession createSession(Map<String,Object> sessionObjects) {
		return new VarResolverSession(getContext(VarResolverContext.class), sessionObjects);
	}

	/**
	 * Resolve variables in the specified string.
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolve(s);</code>.
	 * This method can only be used if the string doesn't contain variables that rely on the existence of session variables.
	 *
	 * @param s The input string.
	 * @return The string with variables resolved, or the same string if it doesn't contain any variables to resolve.
	 */
	public String resolve(String s) {
		return createSession(null).resolve(s);
	}

	/**
	 * Resolve variables in the specified string and sends the results to the specified writer.
	 * <p>
	 * This is a shortcut for calling <code>createSession(<jk>null</jk>).resolveTo(s, w);</code>.
	 * This method can only be used if the string doesn't contain variables that rely on the existence of session variables.
	 *
	 * @param s The input string.
	 * @param w The writer to send the result to.
	 * @throws IOException
	 */
	public void resolveTo(String s, Writer w) throws IOException {
		createSession(null).resolveTo(s, w);
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Lockable */
	public VarResolver lock() {
		super.lock();
		return this;
	}

	@Override /* Lockable */
	public VarResolver clone() {
		try {
			return (VarResolver)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}

