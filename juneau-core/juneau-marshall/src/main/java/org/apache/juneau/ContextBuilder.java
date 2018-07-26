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

import java.util.*;

/**
 * Builder class for building instances of serializers and parsers.
 */
public abstract class ContextBuilder {

	/** Contains all the modifiable settings for the implementation class. */
	protected final PropertyStoreBuilder psb;

	/**
	 * Constructor.
	 * Default settings.
	 */
	public ContextBuilder() {
		this.psb = PropertyStore.create();
	}

	/**
	 * Constructor.
	 *
	 * @param ps The initial configuration settings for this builder.
	 */
	public ContextBuilder(PropertyStore ps) {
		if (ps == null)
			ps = PropertyStore.DEFAULT;
		this.psb = ps.builder();
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Used in cases where multiple context builder are sharing the same property store builder.
	 * <br>(e.g. <code>HtlmlDocBuilder</code>)
	 *
	 * @param psb The property store builder to use.
	 */
	protected ContextBuilder(PropertyStoreBuilder psb) {
		this.psb = psb;
	}

	/**
	 * Returns access to the inner property store builder.
	 *
	 * <p>
	 * Used in conjunction with {@link #ContextBuilder(PropertyStoreBuilder)} when builders share property store builders.
	 *
	 * @return The inner property store builder.
	 */
	protected PropertyStoreBuilder getPropertyStoreBuilder() {
		return psb;
	}

	/**
	 * Build the object.
	 *
	 * @return The built object.
	 * Subsequent calls to this method will create new instances.
	 */
	public abstract Context build();

	/**
	 * Copies the settings from the specified property store into this builder.
	 *
	 * @param copyFrom The factory whose settings are being copied.
	 * @return This object (for method chaining).
	 */
	public ContextBuilder apply(PropertyStore copyFrom) {
		this.psb.apply(copyFrom);
		return this;
	}

	/**
	 * Build a new instance of the specified object.
	 *
	 * @param c The subclass of {@link Context} to instantiate.
	 * @return A new object using the settings defined in this builder.
	 */

	public <T extends Context> T build(Class<T> c) {
		return ContextCache.INSTANCE.create(c, getPropertyStore());
	}

	/**
	 * Returns a read-only snapshot of the current property store on this builder.
	 *
	 * @return A property store object.
	 */
	public PropertyStore getPropertyStore() {
		return psb.build();
	}

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Sets a configuration property on this object.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(String, Object)
	 */
	public ContextBuilder set(String name, Object value) {
		psb.set(name, value);
		return this;
	}

	/**
	 * Sets or adds to a SET or LIST property.
	 *
	 * @param append
	 * 	If <jk>true</jk>, the previous value is appended to.  Otherwise, the previous value is replaced.
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(String, Object)
	 */
	public ContextBuilder set(boolean append, String name, Object value) {
		if (append)
			psb.addTo(name, value);
		else
			psb.set(name, value);
		return this;
	}

	/**
	 * Sets multiple configuration properties on this object.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#set(java.util.Map)
	 */
	public ContextBuilder set(Map<String,Object> properties) {
		psb.set(properties);
		return this;
	}

	/**
	 * Adds multiple configuration properties on this object.
	 *
	 * @param properties The properties to set on this class.
	 * @return This object (for method chaining).
	 * @see PropertyStoreBuilder#add(java.util.Map)
	 */
	public ContextBuilder add(Map<String,Object> properties) {
		psb.add(properties);
		return this;
	}

	/**
	 * Adds a value to a SET or LIST property.
	 *
	 * @param name The property name.
	 * @param value The new value to add to the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ContextBuilder addTo(String name, Object value) {
		psb.addTo(name, value);
		return this;
	}

	/**
	 * Adds or overwrites a value to a SET, LIST, or MAP property.
	 *
	 * @param name The property name.
	 * @param key The property value map key.
	 * @param value The property value map value.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a MAP property.
	 */
	public ContextBuilder addTo(String name, String key, Object value) {
		psb.addTo(name, key, value);
		return this;
	}

	/**
	 * Removes a value from a SET, LIST, or MAP property.
	 *
	 * @param name The property name.
	 * @param value The property value in the SET property.
	 * @return This object (for method chaining).
	 * @throws ConfigException If property is not a SET property.
	 */
	public ContextBuilder removeFrom(String name, Object value) {
		psb.removeFrom(name, value);
		return this;
	}
}