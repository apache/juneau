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

/**
 * Common super class for all serializers, parsers, and serializer/parser groups.
 */
public abstract class CoreObject {

	private final BeanContext beanContext;

	/** A snapshot of all the modifiable settings for this object + override properties. */
	protected final PropertyStore propertyStore;

	/**
	 * Constructor.
	 *
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	protected CoreObject(PropertyStore propertyStore) {
		this.propertyStore = propertyStore.create(getOverrideProperties());
		this.beanContext = createContext(BeanContext.class);
	}

	/**
	 * Creates a new builder class for this object so that a new object can be created that expands upon the current
	 * object's settings.
	 *
	 * @return A new builder.
	 */
	public CoreObjectBuilder builder() {
		throw new NoSuchMethodError();
	}

	/**
	 * Method used by subclasses to override context factory properties for just this instance without modifying the
	 * context factory itself.
	 *
	 * <p>
	 * Subclasses can call this parent method to append to override properties defined higher in the parent chain.
	 *
	 * @return The override properties.  Never <jk>null</jk>.
	 */
	protected ObjectMap getOverrideProperties() {
		return new ObjectMap();
	}

	/**
	 * Returns a copy of the context factory passed in to the constructor with any override properties applied from the
	 * {@link #getOverrideProperties()}.
	 *
	 * @return
	 * 	The context factory on this class.
	 * 	Multiple calls to this method returns the same factory.
	 */
	public PropertyStore createPropertyStore() {
		return PropertyStore.create(propertyStore);
	}

	/**
	 * Creates a read-only context object of the specified type using the context factory on this class.
	 *
	 * @param c The context class to create.
	 * @return The new context object.
	 */
	protected <T extends Context> T createContext(Class<T> c) {
		return propertyStore.getContext(c);
	}

	/**
	 * Returns the bean context to use for this class.
	 *
	 * @return The bean context object.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the universal <code>Object</code> metadata object.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getBeanContext().object();</code>.
	 * </ul>
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link Object} class.
	 */
	public ClassMeta<Object> object() {
		return getBeanContext().object();
	}

	/**
	 * Returns the universal <code>String</code> metadata object.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This is equivalent to calling <code>getBeanContext().string();</code>.
	 * </ul>
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link String} class.
	 */
	public ClassMeta<String> string() {
		return getBeanContext().string();
	}
}
