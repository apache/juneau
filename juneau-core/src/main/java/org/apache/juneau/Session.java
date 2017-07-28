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

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
 *
 * <p>
 * Serializers and parsers use session objects to retrieve config properties and to use it as a scratchpad during
 * serialize and parse actions.
 *
 * @see PropertyStore
 */
public abstract class Session {

	private JuneauLogger logger;

	private final ObjectMap properties;
	private final Context ctx;
	private Map<String,Object> cache;
	private boolean closed;
	private List<String> warnings;                 // Any warnings encountered.


	/**
	 * Default constructor.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for the session.
	 * @param args
	 * 	Runtime arguments.
	 */
	protected Session(final Context ctx, SessionArgs args) {
		this.ctx = ctx;
		this.properties = args.properties != null ? args.properties : ObjectMap.EMPTY_MAP;
	}

	/**
	 * Returns the session-level properties.
	 *
	 * @return The session-level properties.
	 */
	protected final ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Returns the session property with the specified key.
	 *
	 * <p>
	 * The order of lookup for the property is as follows:
	 * <ul>
	 * 	<li>Override property passed in through the constructor.
	 * 	<li>Property defined on the context object.
	 * 	<li>System.property.
	 * </ul>
	 *
	 * @param key The property key.
	 * @return The property value, or <jk>null</jk> if it doesn't exist.
	 */
	public final String getProperty(String key) {
		return getProperty(key, null);
	}

	/**
	 * Same as {@link #getProperty(String)} but with a default value.
	 *
	 * @param key The property key.
	 * @param def The default value if the property doesn't exist or is <jk>null</jk>.
	 * @return The property value.
	 */
	public final String getProperty(String key, String def) {
		Object v = properties.get(key);
		if (v == null)
			v = ctx.getPropertyStore().getProperty(key, String.class, null);
		if (v == null)
			v = def;
		return StringUtils.toString(v);
	}

	/**
	 * Same as {@link #getProperty(String)} but transforms the value to the specified type.
	 *
	 * @param type The class type of the value.
	 * @param key The property key.
	 * @return The property value.
	 */
	public final <T> T getProperty(Class<T> type, String key) {
		return getProperty(type, key, null);
	}

	/**
	 * Same as {@link #getProperty(Class,String)} but with a default value.
	 *
	 * @param type The class type of the value.
	 * @param key The property key.
	 * @param def The default value if the property doesn't exist or is <jk>null</jk>.
	 * @return The property value.
	 */
	public final <T> T getProperty(Class<T> type, String key, T def) {
		T t = properties.get(type, key);
		if (t == null)
			t = ctx.getPropertyStore().getProperty(key, type, def);
		return t;
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
		if (cache == null)
			cache = new TreeMap<String,Object>();
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
		if (cacheObjects != null) {
			if (cache == null)
				cache = new TreeMap<String,Object>();
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
	public final void addWarning(String msg, Object... args) {
		if (warnings == null)
			warnings = new LinkedList<String>();
		getLogger().warning(msg, args);
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
	 * Returns the logger associated with this session.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own logger.
	 *
	 * @return The logger associated with this session.
	 */
	protected final JuneauLogger getLogger() {
		if (logger == null)
			logger = JuneauLogger.getLogger(getClass());
		return logger;
	}

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	@Overrideable
	public ObjectMap asMap() {
		return new ObjectMap();
	}

	@Override /* Object */
	public String toString() {
		try {
			return asMap().toString(JsonSerializer.DEFAULT_LAX_READABLE);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		}
	}

	/**
	 * Perform cleanup on this context object if necessary.
	 *
	 * @return <jk>true</jk> if this method wasn't previously called.
	 * @throws BeanRuntimeException If called more than once, or in debug mode and warnings occurred.
	 */
	public boolean close() throws BeanRuntimeException {
		if (closed)
			return false;
		closed = true;
		return true;
	}

	@Override /* Object */
	protected void finalize() throws Throwable {
//		if (! closed)
//			throw new RuntimeException("Session was not closed.");
	}
}
