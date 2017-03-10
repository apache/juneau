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

import java.text.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
 * <p>
 * Serializers and parsers use session objects to retrieve config properties and to use it
 * 	as a scratchpad during serialize and parse actions.
 *
 * @see PropertyStore
 */
public abstract class Session {

	private JuneauLogger logger;

	private final ObjectMap properties;
	private Map<String,Object> cache;
	private boolean closed;
	private List<String> warnings;                 // Any warnings encountered.

	/**
	 * Default constructor.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for the session.
	 * @param op Properties associated with this session.
	 */
	protected Session(Context ctx, ObjectMap op) {
		this.properties = op != null ? op : ObjectMap.EMPTY_MAP;
	}

	/**
	 * Returns the properties associated with this session.
	 *
	 * @return The properties associated with this session.
	 */
	public ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Adds an arbitrary object to this session's cache.
	 * Can be used to store objects for reuse during a session.
	 *
	 * @param key The key.  Can be any string.
	 * @param val The cahed object.
	 */
	public void addToCache(String key, Object val) {
		if (cache == null)
			cache = new TreeMap<String,Object>();
		cache.put(key, val);
	}

	/**
	 * Adds arbitrary objects to this session's cache.
	 * Can be used to store objects for reuse during a session.
	 *
	 * @param cacheObjects The objects to add to this session's cache.
	 * No-op if <jk>null</jk>.
	 */
	public void addToCache(Map<String,Object> cacheObjects) {
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
	public <T> T getFromCache(Class<T> c, String key) {
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
		msg = args.length == 0 ? msg : MessageFormat.format(msg, args);
		warnings.add((warnings.size() + 1) + ": " + msg);
	}

	/**
	 * Returns <jk>true</jk> if warnings occurred in this session.
	 *
	 * @return <jk>true</jk> if warnings occurred in this session.
	 */
	public boolean hasWarnings() {
		return warnings != null && warnings.size() > 0;
	}

	/**
	 * Returns the warnings that occurred in this session.
	 *
	 * @return The warnings that occurred in this session, or <jk>null</jk> if no warnings occurred.
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	/**
	 * Returns the logger associated with this session.
	 * Subclasses can override this method to provide their own logger.
	 *
	 * @return The logger associated with this session.
	 */
	protected JuneauLogger getLogger() {
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
