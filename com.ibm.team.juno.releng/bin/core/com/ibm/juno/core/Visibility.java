/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core;

import java.lang.reflect.*;

/**
 * Defines class/field/method visibilities.
 * <p>
 * Used to specify minimum levels of visibility when detecting bean classes, methods, and fields.
 * Used in conjunction with the following bean context properties:
 * <ul>
 * 	<li>{@link BeanContextProperties#BEAN_beanConstructorVisibility}
 * 	<li>{@link BeanContextProperties#BEAN_beanClassVisibility}
 * 	<li>{@link BeanContextProperties#BEAN_beanFieldVisibility}
 * 	<li>{@link BeanContextProperties#BEAN_methodVisibility}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum Visibility {

	/** Ignore all */
	NONE,

	/** Include only <jk>public</jk> classes/fields/methods. */
	PUBLIC,

	/** Include only <jk>public</jk> or <jk>protected</jk> classes/fields/methods. */
	PROTECTED,

	/** Include all but <jk>private</jk> classes/fields/methods. */
	DEFAULT,

	/** Include all classes/fields/methods. */
	PRIVATE;

	/**
	 * Identifies if the specified mod matches this visibility.
	 * Example:
	 * <code>
	 * 	<jsf>PUBLIC</jsf>.isVisible(MyPublicClass.<jk>class</jk>.getModifiers()); <jc>//true</jk>
	 * 	<jsf>PUBLIC</jsf>.isVisible(MyPrivateClass.<jk>class</jk>.getModifiers()); <jc>//false</jk>
	 * 	<jsf>PRIVATE</jsf>.isVisible(MyPrivateClass.<jk>class</jk>.getModifiers()); <jc>//true</jk>
	 * 	<jsf>NONE</jsf>.isVisible(MyPublicClass.<jk>class</jk>.getModifiers()); <jc>//false</jk>
	 * </code>
	 *
	 * @param mod The modifier from the object being tested (e.g. results from {@link Class#getModifiers()}.
	 * @return <jk>true</jk> if this visibility matches the specified modifier attribute.
	 */
	public boolean isVisible(int mod) {
		switch(this) {
			case NONE: return false;
			case PRIVATE: return true;
			case DEFAULT: return ! Modifier.isPrivate(mod);
			case PROTECTED: return Modifier.isProtected(mod) || Modifier.isPublic(mod);
			default: return Modifier.isPublic(mod);
		}
	}

	/**
	 * Shortcut for <code>isVisible(x.getModifiers());</code>
	 *
	 * @param x The constructor to check.
	 * @return <jk>true</jk> if the constructor is at least as visible as this object.
	 */
	public boolean isVisible(Constructor<?> x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Shortcut for <code>isVisible(x.getModifiers());</code>
	 *
	 * @param x The method to check.
	 * @return <jk>true</jk> if the method is at least as visible as this object.
	 */
	public boolean isVisible(Method x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Shortcut for <code>isVisible(x.getModifiers());</code>
	 *
	 * @param x The field to check.
	 * @return <jk>true</jk> if the field is at least as visible as this object.
	 */
	public boolean isVisible(Field x) {
		return isVisible(x.getModifiers());
	}

	/**
	 * Makes constructor accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 * Security exceptions thrown on the call to {@link Constructor#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param x The constructor.
	 * @return The same constructor if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Constructor#setAccessible(boolean)} throws a security exception.
	 */
	public <T> Constructor<T> filter(Constructor<T> x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}

	/**
	 * Makes method accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 * Security exceptions thrown on the call to {@link Method#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param x The method.
	 * @return The same method if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Method#setAccessible(boolean)} throws a security exception.
	 */
	public <T> Method filter(Method x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}

	/**
	 * Makes field accessible if it matches the visibility requirements, or returns <jk>null</jk> if it doesn't.
	 * Security exceptions thrown on the call to {@link Field#setAccessible(boolean)} are quietly ignored.
	 *
	 * @param x The field.
	 * @return The same field if visibility requirements met, or <jk>null</jk> if visibility requirement not
	 * 	met or call to {@link Field#setAccessible(boolean)} throws a security exception.
	 */
	public Field filter(Field x) {
		if (x == null)
			return null;
		if (isVisible(x))
			if (! setAccessible(x))
				return null;
		return x;
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The constructor.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Constructor<?> x) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The method.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Method x) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Attempts to call <code>x.setAccessible(<jk>true</jk>)</code> and quietly ignores security exceptions.
	 *
	 * @param x The field.
	 * @return <jk>true</jk> if call was successful.
	 */
	public static boolean setAccessible(Field x) {
		try {
			if (! (x == null || x.isAccessible()))
				x.setAccessible(true);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}
}