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

import static java.util.Optional.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.Context.Args;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;

/**
 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
 */
public abstract class ContextSession {

	private final OMap properties;
	private Map<String,Object> cache;
	private List<String> warnings;	// Any warnings encountered.

	private final Context ctx;
	private final boolean debug;
	private final boolean unmodifiable;


	/**
	 * Default constructor.
	 *
	 * @param ctx The context object.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected ContextSession(Context ctx, Args args) {
		this.ctx = ctx;
		this.unmodifiable = args.unmodifiable;
		OMap sp = args.properties;
		if (args.unmodifiable)
			sp = sp.unmodifiable();
		properties = sp;
		debug = ofNullable(args.debug).orElse(ctx.isDebug());
	}

	/**
	 * Returns the session properties on this session.
	 *
	 * @return The session properties on this session.  Never <jk>null</jk>.
	 */
	public final OMap getSessionProperties() {
		return properties;
	}

	/**
	 * Returns the context that created this session.
	 *
	 * @return The context that created this session.
	 */
	public Context getContext() {
		return ctx;
	}

	/**
	 * Adds an arbitrary object to this session's cache.
	 *
	 * <p>
	 * Can be used to store objects for reuse during a session.
	 *
	 * @param key The key.  Can be any string.
	 * @param val The cached object.
	 */
	public final void addToCache(String key, Object val) {
		if (unmodifiable)
			return;
		if (cache == null)
			cache = new TreeMap<>();
		cache.put(key, val);
	}

	/**
	 * Adds arbitrary objects to this session's cache.
	 *
	 * <p>
	 * Can be used to store objects for reuse during a session.
	 *
	 * @param cacheObjects
	 * 	The objects to add to this session's cache.
	 * 	No-op if <jk>null</jk>.
	 */
	public final void addToCache(Map<String,Object> cacheObjects) {
		if (unmodifiable)
			return;
		if (cacheObjects != null) {
			if (cache == null)
				cache = new TreeMap<>();
			cache.putAll(cacheObjects);
		}
	}

	/**
	 * Returns an object stored in the session cache.
	 *
	 * @param c The class type of the object.
	 * @param key The session object key.
	 * @return The cached object, or <jk>null</jk> if it doesn't exist.
	 */
	@SuppressWarnings("unchecked")
	public final <T> T getFromCache(Class<T> c, String key) {
		return cache == null ? null : (T)cache.get(key);
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void addWarning(String msg, Object... args) {
		if (unmodifiable)
			return;
		if (warnings == null)
			warnings = new LinkedList<>();
		warnings.add((warnings.size() + 1) + ": " + format(msg, args));
	}

	/**
	 * Returns <jk>true</jk> if warnings occurred in this session.
	 *
	 * @return <jk>true</jk> if warnings occurred in this session.
	 */
	public final boolean hasWarnings() {
		return warnings != null && warnings.size() > 0;
	}

	/**
	 * Returns the warnings that occurred in this session.
	 *
	 * @return The warnings that occurred in this session, or <jk>null</jk> if no warnings occurred.
	 */
	public final List<String> getWarnings() {
		return warnings;
	}

	/**
	 * Throws a {@link BeanRuntimeException} if any warnings occurred in this session.
	 */
	public void checkForWarnings() {
		if (warnings != null && ! warnings.isEmpty())
			throw new BeanRuntimeException("Warnings occurred in session: \n" + join(getWarnings(), "\n"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Configuration properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Debug mode.
	 *
	 * @see Context.Builder#debug()
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return debug;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the properties defined on this bean as a simple map for debugging purposes.
	 *
	 * <p>
	 * Use <c>SimpleJson.<jsf>DEFAULT</jsf>.println(<jv>thisBean</jv>)</c> to dump the contents of this bean to the console.
	 *
	 * @return A new map containing this bean's properties.
	 */
	public OMap toMap() {
		return OMap.create().filtered();
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}
}