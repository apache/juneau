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
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A reusable stateless thread-safe read-only configuration, typically used for creating one-time use {@link Session}
 * objects.
 * {@review}
 *
 * <p>
 * Contexts are created through the {@link ContextBuilder#build()} method (and subclasses of {@link ContextBuilder}).
 *
 * <p>
 * Subclasses MUST implement the following constructor:
 *
 * <p class='bcode w800'>
 * 	<jk>public</jk> T(ContextProperties);
 * </p>
 *
 * <p>
 * Besides that restriction, a context object can do anything you desire.
 * <br>However, it MUST be thread-safe and all fields should be declared final to prevent modification.
 * <br>It should NOT be used for storing temporary or state information.
 *
 * @see ContextProperties
 */
@ConfigurableContext
public abstract class Context {

	static final String PREFIX = "Context";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <p>
	 * Enables the following additional information during serialization:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When bean getters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * 	<li>
	 * 		Enables {@link Serializer#BEANTRAVERSE_detectRecursions}.
	 * </ul>
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.Context#CONTEXT_debug CONTEXT_debug}
	 * 	<li><b>Name:</b>  <js>"Context.debug.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Context.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>CONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.ContextBuilder#debug()}
	 * 			<li class='jm'>{@link org.apache.juneau.SessionArgs#debug(Boolean)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String CONTEXT_debug = PREFIX + ".debug.b";


	final ContextProperties properties;
	private final int identityCode;

	private final boolean debug;

	/**
	 * Constructor for this class.
	 *
	 * <p>
	 * Subclasses MUST implement the same public constructor.
	 *
	 * @param cp The read-only configuration for this context object.
	 * @param allowReuse If <jk>true</jk>, subclasses that share the same property store values can be reused.
	 */
	public Context(ContextProperties cp, boolean allowReuse) {
		properties = cp == null ? ContextProperties.DEFAULT : cp;
		cp = properties;
		this.identityCode = allowReuse ? new HashCode().add(className(this)).add(cp).get() : System.identityHashCode(this);
		debug = cp.getBoolean(CONTEXT_debug).orElse(false);
	}

	/**
	 * Returns the keys found in the specified property group.
	 *
	 * <p>
	 * The keys are NOT prefixed with group names.
	 *
	 * @param group The group name.
	 * @return The set of property keys, or an empty set if the group was not found.
	 */
	public Set<String> getPropertyKeys(String group) {
		return properties.getKeys(group);
	}

	/**
	 * Returns the property store associated with this context.
	 *
	 * @return The property store associated with this context.
	 */
	public final ContextProperties getContextProperties() {
		return properties;
	}

	/**
	 * Creates a builder from this context object.
	 *
	 * <p>
	 * Builders are used to define new contexts (e.g. serializers, parsers) based on existing configurations.
	 *
	 * @return A new ContextBuilder object.
	 */
	public ContextBuilder copy() {
		throw new UnsupportedOperationException();  // Can't copy an abstract class.
	}

	/**
	 * Constructs the specified context class using the property store of this context class.
	 *
	 * @param c The context class to instantiate.
	 * @param <T> The context class to instantiate.
	 * @return The instantiated context class.
	 */
	public <T extends Context> T getContext(Class<T> c) {
		return ContextCache.INSTANCE.create(c, properties);
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	public Session createSession() {
		return createSession(createDefaultSessionArgs());
	}

	/**
	 * Create a new session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public abstract Session createSession(SessionArgs args);

	/**
	 * Defines default session arguments used when calling the {@link #createSession()} method.
	 *
	 * @return A SessionArgs object, possibly a read-only reusable instance.
	 */
	public abstract SessionArgs createDefaultSessionArgs();

	@Override /* Object */
	public int hashCode() {
		return identityCode;
	}

	/**
	 * Returns a uniqueness identity code for this context.
	 *
	 * @return A uniqueness identity code.
	 */
	public int identityCode() {
		return identityCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		// Context objects are considered equal if they're the same class and have the same set of properties.
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		Context c = (Context)o;
		return (c.properties.equals(properties));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode.
	 *
	 * @see #CONTEXT_debug
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return debug;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap
			.create()
			.filtered()
			.a(
				"Context",
				OMap
					.create()
					.filtered()
					.a("identityCode", identityCode)
					.a("properties", System.identityHashCode(properties))
			);
	}
}
