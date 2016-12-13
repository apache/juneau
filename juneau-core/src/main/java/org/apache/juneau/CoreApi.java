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
 * Common super class for all core-API serializers, parsers, and serializer/parser groups.
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * Maintains an inner {@link ContextFactory} instance that can be used by serializer and parser subclasses
 * 	to work with beans in a consistent way.
 * <p>
 * Provides several duplicate convenience methods from the {@link ContextFactory} class to set properties on that class from this class.
 * <p>
 * Also implements the {@link Lockable} interface to allow for easy locking and cloning.
 */
public abstract class CoreApi extends Lockable {

	private ContextFactory contextFactory = ContextFactory.create();
	private BeanContext beanContext;

	/**
	 * Returns the {@link ContextFactory} object associated with this class.
	 * <p>
	 * The context factory stores all configuration properties for this class.
	 * Adding/modifying properties on this factory will alter the behavior of this object.
	 * <p>
	 * Calling the {@link ContextFactory#lock()} method on the returned object will prevent
	 * 	any further modifications to the configuration for this object
	 * 	ANY ANY OTHERS THAT SHARE THE SAME FACTORY!.
	 * Note that calling the {@link #lock()} method on this class will only
	 * 	lock the configuration for this particular instance of the class.
	 *
	 * @return The context factory associated with this object.
	 */
	public ContextFactory getContextFactory() {
		return contextFactory;
	}

	/**
	 * Returns the bean context to use for this class.
	 *
	 * @return The bean context object.
	 */
	public BeanContext getBeanContext() {
		if (beanContext == null)
			return contextFactory.getContext(BeanContext.class);
		return beanContext;
	}

	/**
	 * Creates a {@link Context} class instance of the specified type.
	 *
	 * @param contextClass The class instance to create.
	 * @return A context class instance of the specified type.
	 */
	protected final <T extends Context> T getContext(Class<T> contextClass) {
		return contextFactory.getContext(contextClass);
	}

	/**
	 * Shortcut for calling <code>getContextFactory().setProperty(<jf>property</jf>, <jf>value</jf>);</code>.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object or {@link ContextFactory} object.
	 * @see ContextFactory#setProperty(String, Object)
	 */
	public CoreApi setProperty(String property, Object value) throws LockedException {
		checkLock();
		contextFactory.setProperty(property, value);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().setProperties(<jf>properties</jf>);</code>.
	 *
	 * @param properties The properties to set on this class.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 * @see ContextFactory#setProperties(java.util.Map)
	 */
	public CoreApi setProperties(ObjectMap properties) throws LockedException {
		checkLock();
		contextFactory.setProperties(properties);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().addNotBeanClasses(<jf>classes</jf>)</code>.
	 *
	 * @see ContextFactory#addToProperty(String,Object)
	 * @param classes The new setting value for the bean context.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#addToProperty(String, Object)
	 * @see BeanContext#BEAN_notBeanClasses
	 */
	public CoreApi addNotBeanClasses(Class<?>...classes) throws LockedException {
		checkLock();
		contextFactory.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().addBeanFilters(<jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#addToProperty(String, Object)
	 * @see BeanContext#BEAN_beanFilters
	 */
	public CoreApi addBeanFilters(Class<?>...classes) throws LockedException {
		checkLock();
		contextFactory.addBeanFilters(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().addPojoSwaps(<jf>classes</jf>)</code>.
	 *
	 * @param classes The new setting value for the bean context.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#addToProperty(String, Object)
	 * @see BeanContext#BEAN_pojoSwaps
	 */
	public CoreApi addPojoSwaps(Class<?>...classes) throws LockedException {
		checkLock();
		contextFactory.addPojoSwaps(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().addToDictionary(<jf>classes</jf>)</code>.
	 *
	 * @param classes The bean classes (or BeanDictionaryBuilder) classes to add to the bean dictionary.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#addToProperty(String, Object)
	 * @see BeanContext#BEAN_beanDictionary
	 */
	public CoreApi addToDictionary(Class<?>...classes) throws LockedException {
		checkLock();
		contextFactory.addToDictionary(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().addImplClass(<jf>interfaceClass</jf>, <jf>implClass</jf>)</code>.
	 *
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 * @see ContextFactory#putToProperty(String, Object, Object)
	 * @see BeanContext#BEAN_implClasses
	 */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		contextFactory.addImplClass(interfaceClass, implClass);
		return this;
	}

	/**
	 * Shortcut for calling <code>getContextFactory().setClassLoader(<jf>classLoader</jf>)</code>.
	 *
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link ContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 * @see ContextFactory#setClassLoader(ClassLoader)
	 */
	public CoreApi setClassLoader(ClassLoader classLoader) throws LockedException {
		checkLock();
		contextFactory.setClassLoader(classLoader);
		return this;
	}

	/**
	 * Shortcut for calling {@link BeanContext#object()}.
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link Object} class.
	 */
	public ClassMeta<Object> object() {
		return getBeanContext().object();
	}

	/**
	 * Shortcut for calling  {@link BeanContext#string()}.
	 *
	 * @return The reusable {@link ClassMeta} for representing the {@link String} class.
	 */
	public ClassMeta<String> string() {
		return getBeanContext().string();
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Lockable */
	public void checkLock() {
		super.checkLock();
		beanContext = null;
	}

	@Override /* Lockable */
	public CoreApi lock() {
		try {
			super.lock();
			contextFactory = contextFactory.clone();
			contextFactory.lock();
			beanContext = contextFactory.getContext(BeanContext.class);
			return this;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override /* Lockable */
	public CoreApi clone() throws CloneNotSupportedException {
		CoreApi c = (CoreApi)super.clone();
		c.contextFactory = ContextFactory.create(contextFactory);
		c.beanContext = null;
		return c;
	}
}
