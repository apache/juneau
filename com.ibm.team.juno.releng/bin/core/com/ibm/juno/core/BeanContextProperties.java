/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.annotation.*;

/**
 * Configurable properties on the {@link BeanContextFactory} class.
 * <p>
 * 	Use the {@link BeanContextFactory#setProperty(String, Object)} method to set properties on
 * 	bean contexts.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class BeanContextProperties {

	/**
	 * Require no-arg constructor ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, a Java class must implement a default no-arg constructor to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireDefaultConstructor = "BeanContext.beansRequireDefaultConstructor";

	/**
	 * Require {@link Serializable} interface ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSerializable = "BeanContext.beansRequireSerializable";

	/**
	 * Require setters for getters ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, only getters that have equivalent setters will be considered as properties on a bean.
	 * Otherwise, they will be ignored.
	 */
	public static final String BEAN_beansRequireSettersForGetters = "BeanContext.beansRequireSettersForGetters";

	/**
	 * Require some properties ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, then a Java class must contain at least 1 property to be considered a bean.
	 * <p>
	 * The {@link Bean @Bean} annotation can be used on a class to override this setting when <jk>true</jk>.
	 */
	public static final String BEAN_beansRequireSomeProperties = "BeanContext.beansRequireSomeProperties";

	/**
	 * Put returns old value ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, then the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * <p>
	 * Disabled by default because it introduces a slight performance penalty.
	 */
	public static final String BEAN_beanMapPutReturnsOldValue = "BeanContext.beanMapPutReturnsOldValue";

	/**
	 * Look for bean constructors with the specified minimum visibility ({@link Visibility}, default={@link Visibility#PUBLIC}).
	 */
	public static final String BEAN_beanConstructorVisibility = "BeanContext.beanConstructorVisibility";

	/**
	 * Look for bean classes with the specified minimum visibility ({@link Visibility}, default={@link Visibility#PUBLIC}).
	 * <p>
	 * Classes are not considered beans unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean class is <jk>protected</jk>, then
	 * 	the class will not be interpreted as a bean class.
	 */
	public static final String BEAN_beanClassVisibility = "BeanContext.beanClassVisibility";

	/**
	 * Look for bean fields with the specified minimum visibility ({@link Visibility}, default={@link Visibility#PUBLIC}).
	 * <p>
	 * Fields are not considered bean properties unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean field is <jk>protected</jk>, then
	 * 	the field will not be interpreted as a bean property.
	 * <p>
	 * Use {@link Visibility#NONE} to prevent bean fields from being interpreted as bean properties altogether.
	 */
	public static final String BEAN_beanFieldVisibility = "BeanContext.beanFieldVisibility";

	/**
	 * Look for bean methods with the specified minimum visibility ({@link Visibility}, default={@link Visibility#PUBLIC}).
	 * <p>
	 * Methods are not considered bean getters/setters unless they meet the minimum visibility requirements.
	 * For example, if the visibility is <code>PUBLIC</code> and the bean method is <jk>protected</jk>, then
	 * 	the method will not be interpreted as a bean getter or setter.
	 */
	public static final String BEAN_methodVisibility = "BeanContext.methodVisibility";

	/**
	 * Use Java {@link Introspector} for determining bean properties ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * Using the built-in Java bean introspector will not pick up fields or non-standard getters/setters.
	 * Most {@link Bean @Bean} annotations will be ignored.
	 */
	public static final String BEAN_useJavaBeanIntrospector = "BeanContext.useJavaBeanIntrospector";

	/**
	 * Use interface proxies ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, then interfaces will be instantiated as proxy classes through the use of an {@link InvocationHandler}
	 * if there is no other way of instantiating them.
	 */
	public static final String BEAN_useInterfaceProxies = "BeanContext.useInterfaceProxies";

	/**
	 * Ignore unknown properties ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownBeanProperties = "BeanContext.ignoreUnknownBeanProperties";

	/**
	 * Ignore unknown properties with null values ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, trying to set a <jk>null</jk> value on a non-existent bean property will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreUnknownNullBeanProperties = "BeanContext.ignoreUnknownNullBeanProperties";

	/**
	 * Ignore properties without setters ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, trying to set a value on a bean property without a setter will silently be ignored.
	 * Otherwise, a {@code RuntimeException} is thrown.
	 */
	public static final String BEAN_ignorePropertiesWithoutSetters = "BeanContext.ignorePropertiesWithoutSetters";

	/**
	 * Ignore invocation errors on getters ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean getter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnGetters = "BeanContext.ignoreInvocationExceptionsOnGetters";

	/**
	 * Ignore invocation errors on setters ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, errors thrown when calling bean setter methods will silently be ignored.
	 * Otherwise, a {@code BeanRuntimeException} is thrown.
	 */
	public static final String BEAN_ignoreInvocationExceptionsOnSetters = "BeanContext.ignoreInvocationExceptionsOnSetters";

	/**
	 * Add to the list of packages whose classes should not be considered beans ({@link String}, comma-delimited list).
	 * <p>
	 * When specified, the current list of ignore packages are appended to.
	 * The default list of ignore packages are as follows:
	 * <ul>
	 * 	<li><code>java.lang</code>
	 * 	<li><code>java.lang.annotation</code>
	 * 	<li><code>java.lang.ref</code>
	 * 	<li><code>java.lang.reflect</code>
	 * 	<li><code>java.io</code>
	 * 	<li><code>java.net</code>
	 * 	<li><code>java.nio.*</code>
	 * 	<li><code>java.util.*</code>
	 * </ul>
	 * Any classes within these packages will be serialized to strings using {@link Object#toString()}.
	 * <p>
	 * Note that you can specify prefix patterns to include all subpackages.
	 */
	public static final String BEAN_addNotBeanPackages = "BeanContext.addNotBeanPackages";

	/**
	 * Remove from the list of packages whose classes should not be considered beans ({@link String}, comma-delimited list).
	 * <p>
	 * Essentially the opposite of {@link #BEAN_addNotBeanPackages}.
	 */
	public static final String BEAN_removeNotBeanPackages = "BeanContext.removeNotBeanPackages";
}
