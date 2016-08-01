/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

/**
 * Common super class for all core-API serializers, parsers, and serializer/parser groups.
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Maintains an inner {@link BeanContextFactory} instance that can be used by serializer and parser subclasses
 * 		to work with beans in a consistent way.
 * <p>
 * 	Provides several duplicate convenience methods from the {@link BeanContextFactory} class to set properties on that class from this class.
 * <p>
 * 	Also implements the {@link Lockable} interface to allow for easy locking and cloning.
 *
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public abstract class CoreApi extends Lockable {

	/** The bean context used by this object. */
	protected transient BeanContextFactory beanContextFactory = new BeanContextFactory();
	private BeanContext beanContext;


	/**
	 * Returns the current value of the {@code beanContext} setting.
	 *
	 * @return The current setting value.
	 */
	public final BeanContext getBeanContext() {
		if (beanContext == null)
			beanContext = beanContextFactory.getBeanContext();
		return beanContext;
	}

	/**
	 * Sets a property on this class.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi setProperty(String property, Object value) throws LockedException {
		checkLock();
		beanContextFactory.setProperty(property, value);
		return this;
	}

	/**
	 * Sets multiple properties on this class.
	 *
	 * @param properties The properties to set on this class.
	 * @return This class (for method chaining).
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public CoreApi setProperties(ObjectMap properties) throws LockedException {
		checkLock();
		beanContextFactory.setProperties(properties);
		return this;
	}

	/**
	 * Shortcut for calling <code>getBeanContext().addNotBeanClasses(Class...)</code>.
	 *
	 * @see BeanContextFactory#addNotBeanClasses(Class...)
	 * @param classes The new setting value for the bean context.
	 * @throws LockedException If {@link BeanContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 */
	public CoreApi addNotBeanClasses(Class<?>...classes) throws LockedException {
		checkLock();
		beanContextFactory.addNotBeanClasses(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getBeanContext().addFilters(Class...)</code>.
	 *
	 * @see BeanContextFactory#addFilters(Class...)
	 * @param classes The new setting value for the bean context.
	 * @throws LockedException If {@link BeanContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 */
	public CoreApi addFilters(Class<?>...classes) throws LockedException {
		checkLock();
		beanContextFactory.addFilters(classes);
		return this;
	}

	/**
	 * Shortcut for calling <code>getBeanContext().addImplClass(Class, Class)</code>.
	 *
	 * @see BeanContextFactory#addImplClass(Class, Class)
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation class.
	 * @throws LockedException If {@link BeanContextFactory#lock()} was called on this class or the bean context.
	 * @param <T> The class type of the interface.
	 * @return This object (for method chaining).
	 */
	public <T> CoreApi addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		checkLock();
		beanContextFactory.addImplClass(interfaceClass, implClass);
		return this;
	}

	/**
	 * Shortcut for calling <code>getBeanContext().setClassLoader(ClassLoader)</code>.
	 *
	 * @see BeanContextFactory#setClassLoader(ClassLoader)
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link BeanContextFactory#lock()} was called on this class or the bean context.
	 * @return This object (for method chaining).
	 */
	public CoreApi setClassLoader(ClassLoader classLoader) throws LockedException {
		checkLock();
		beanContextFactory.setClassLoader(classLoader);
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
		super.lock();
		beanContextFactory.lock();
		beanContext = beanContextFactory.getBeanContext();
		return this;
	}

	@Override /* Lockable */
	public CoreApi clone() throws CloneNotSupportedException{
		CoreApi c = (CoreApi)super.clone();
		c.beanContextFactory = beanContextFactory.clone();
		c.beanContext = null;
		return c;
	}
}
