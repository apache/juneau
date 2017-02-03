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

import static org.apache.juneau.ClassMetaSimple.ClassCategory.*;

import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * A wrapper class around the {@link Class} object that provides cached information
 * about that class.
 *
 * <p>
 * 	Instances of this class can be created through the {@link BeanContext#getClassMeta(Class)} method.
 * <p>
 * 	The {@link BeanContext} class will cache and reuse instances of this class except for the following class types:
 * <ul>
 * 	<li>Arrays
 * 	<li>Maps with non-Object key/values.
 * 	<li>Collections with non-Object key/values.
 * </ul>
 * <p>
 * 	This class is tied to the {@link BeanContext} class because it's that class that makes the determination
 * 	of what is a bean.
 *
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties="innerClass,classCategory,elementType,keyType,valueType,notABeanReason,initException,beanMeta")
public final class ClassMeta<T> extends ClassMetaSimple<T> {

	final BeanContext beanContext;                    // The bean context that created this object.
	ClassMeta<?>
		serializedClassMeta,                           // The transformed class type (if class has swap associated with it).
		elementType = null,                            // If ARRAY or COLLECTION, the element class type.
		keyType = null,                                // If MAP, the key class type.
		valueType = null;                              // If MAP, the value class type.
	InvocationHandler invocationHandler;              // The invocation handler for this class (if it has one).
	BeanMeta<T> beanMeta;                             // The bean meta for this bean class (if it's a bean).
	String dictionaryName, resolvedDictionaryName;    // The dictionary name of this class if it has one.
	String notABeanReason;                            // If this isn't a bean, the reason why.
	PojoSwap<T,?> pojoSwap;                           // The object POJO swap associated with this bean (if it has one).
	BeanFilter beanFilter;                            // The bean filter associated with this bean (if it has one).

	private MetadataMap extMeta = new MetadataMap();  // Extended metadata
	private Throwable initException;                  // Any exceptions thrown in the init() method.
	private boolean hasChildPojoSwaps;                // True if this class or any subclass of this class has a PojoSwap associated with it.

	/**
	 * Shortcut for calling <code>ClassMeta(innerClass, beanContext, <jk>false</jk>)</code>.
	 */
	ClassMeta(Class<T> innerClass, BeanContext beanContext) {
		this(innerClass, beanContext, false);
	}

	/**
	 * Construct a new {@code ClassMeta} based on the specified {@link Class}.
	 *
	 * @param innerClass The class being wrapped.
	 * @param beanContext The bean context that created this object.
	 * @param delayedInit Don't call init() in constructor.
	 * 	Used for delayed initialization when the possibility of class reference loops exist.
	 */
	ClassMeta(Class<T> innerClass, BeanContext beanContext, boolean delayedInit) {
		super(innerClass);
		this.beanContext = beanContext;
		if (! delayedInit)
			init();
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	ClassMeta init() {

		try {
			beanFilter = findBeanFilter(beanContext);
			pojoSwap = findPojoSwap(beanContext);

			if (innerClass != Object.class && this.noArgConstructor == null) {
				this.noArgConstructor = (Constructor<T>) beanContext.getImplClassConstructor(innerClass, Visibility.PUBLIC);
			}

			this.hasChildPojoSwaps = beanContext.hasChildPojoSwaps(innerClass);

			if (swapMethod != null) {
				this.pojoSwap = new PojoSwap<T,Object>(innerClass, swapMethod.getReturnType()) {
					@Override
					public Object swap(BeanSession session, Object o) throws SerializeException {
						try {
							return swapMethod.invoke(o, session);
						} catch (Exception e) {
							throw new SerializeException(e);
						}
					}
					@Override
					public T unswap(BeanSession session, Object f, ClassMeta<?> hint) throws ParseException {
						try {
							if (swapConstructor != null)
								return swapConstructor.newInstance(f);
							return super.unswap(session, f, hint);
						} catch (Exception e) {
							throw new ParseException(e);
						}
					}
				};
			}

			serializedClassMeta = (pojoSwap == null ? this : beanContext.getClassMeta(pojoSwap.getSwapClass()));
			if (serializedClassMeta == null)
				serializedClassMeta = this;

			// If this is an array, get the element type.
			if (cc == ARRAY)
				elementType = beanContext.getClassMeta(innerClass.getComponentType());

			// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
			else if (cc == MAP) {
				ClassMeta[] parameters = beanContext.findParameters(innerClass, innerClass);
				if (parameters != null && parameters.length == 2) {
					keyType = parameters[0];
					valueType = parameters[1];
				} else {
					keyType = beanContext.getClassMeta(Object.class);
					valueType = beanContext.getClassMeta(Object.class);
				}
			}

			// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
			else if (cc == COLLECTION) {
				ClassMeta[] parameters = beanContext.findParameters(innerClass, innerClass);
				if (parameters != null && parameters.length == 1) {
					elementType = parameters[0];
				} else {
					elementType = beanContext.getClassMeta(Object.class);
				}
			}

			// If the category is unknown, see if it's a bean.
			// Note that this needs to be done after all other initialization has been done.
			else if (cc == OTHER) {

				BeanMeta newMeta = null;
				try {
					newMeta = new BeanMeta(this, beanContext, beanFilter, null);
					notABeanReason = newMeta.notABeanReason;
				} catch (RuntimeException e) {
					notABeanReason = e.getMessage();
					throw e;
				}
				if (notABeanReason == null) {
					beanMeta = newMeta;
				}
			}

		} catch (NoClassDefFoundError e) {
			this.initException = e;
		} catch (RuntimeException e) {
			this.initException = e;
			throw e;
		}

		if (isBean())
			dictionaryName = resolvedDictionaryName = getBeanMeta().getDictionaryName();

		if (isArray()) {
			resolvedDictionaryName = getElementType().getResolvedDictionaryName();
			if (resolvedDictionaryName != null)
				resolvedDictionaryName += "^";
		}

		return this;
	}

	/**
	 * Returns the bean dictionary name associated with this class.
	 * <p>
	 * The lexical name is defined by {@link Bean#typeName()}.
	 *
	 * @return The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this isn't a bean.
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns the resolved bean dictionary name associated with this class.
	 * <p>
	 * Unlike {@link #getDictionaryName()}, this method automatically resolves multidimensional arrays
	 *  (e.g. <js>"X^^"</js> and returns array class metas accordingly if the base class has a type name.
	 *
	 * @return The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this isn't a bean.
	 */
	public String getResolvedDictionaryName() {
		return resolvedDictionaryName;
	}

	/**
	 * Returns <jk>true</jk> if this class as subtypes defined through {@link Bean#subTypes}.
	 *
	 * @return <jk>true</jk> if this class has subtypes.
	 */
	public boolean hasSubTypes() {
		return beanFilter != null && beanFilter.getSubTypeProperty() != null;
	}

	/**
	 * Returns <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 * <p>
	 * Used when transforming bean properties to prevent having to look up transforms if we know for certain
	 * that no transforms are associated with a bean property.
	 *
	 * @return <jk>true</jk> if this class or any child classes has a {@link PojoSwap} associated with it.
	 */
	public boolean hasChildPojoSwaps() {
		return hasChildPojoSwaps;
	}

	private BeanFilter findBeanFilter(BeanContext context) {
		try {
			if (context == null)
				return null;
			BeanFilter f = context.findBeanFilter(innerClass);
			if (f != null)
				return f;
			List<Bean> ba = ReflectionUtils.findAnnotations(Bean.class, innerClass);
			if (! ba.isEmpty())
				f = new AnnotationBeanFilterBuilder(innerClass, ba).build();
			return f;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private PojoSwap<T,?> findPojoSwap(BeanContext context) {
		try {
			Pojo p = innerClass.getAnnotation(Pojo.class);
			if (p != null) {
				Class<?> swapClass = p.swap();
				if (swapClass != Null.class) {
					if (ClassUtils.isParentClass(PojoSwap.class, swapClass))
						return (PojoSwap<T,?>)swapClass.newInstance();
					throw new RuntimeException("TODO - Surrogate classes not yet supported.");
				}
			}
			if (context == null)
				return null;
			PojoSwap<T,?> f = context.findPojoSwap(innerClass);
			return f;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set element type on non-cached <code>Collection</code> types.
	 *
	 * @param elementType The class type for elements in the collection class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setElementType(ClassMeta<?> elementType) {
		this.elementType = elementType;
		return this;
	}

	/**
	 * Set key type on non-cached <code>Map</code> types.
	 *
	 * @param keyType The class type for keys in the map class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setKeyType(ClassMeta<?> keyType) {
		this.keyType = keyType;
		return this;
	}

	/**
	 * Set value type on non-cached <code>Map</code> types.
	 *
	 * @param valueType The class type for values in the map class represented by this metadata.
	 * @return This object (for method chaining).
	 */
	protected ClassMeta<T> setValueType(ClassMeta<?> valueType) {
		this.valueType = valueType;
		return this;
	}

	/**
	 * Returns the serialized (swapped) form of this class if there is an {@link PojoSwap} associated with it.
	 *
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	@BeanIgnore
	public ClassMeta<?> getSerializedClassMeta() {
		return serializedClassMeta;
	}

	/**
	 * For array and {@code Collection} types, returns the class type of the components of the array or {@code Collection}.
	 *
	 * @return The element class type, or <jk>null</jk> if this class is not an array or Collection.
	 */
	public ClassMeta<?> getElementType() {
		return elementType;
	}

	/**
	 * For {@code Map} types, returns the class type of the keys of the {@code Map}.
	 *
	 * @return The key class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getKeyType() {
		return keyType;
	}

	/**
	 * For {@code Map} types, returns the class type of the values of the {@code Map}.
	 *
	 * @return The value class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getValueType() {
		return valueType;
	}

	/**
	 * Returns <jk>true</jk> if this class is a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public boolean isBean() {
		return beanMeta != null;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 */
	public boolean isMapOrBean() {
		return cc == MAP || cc == BEANMAP || beanMeta != null;
	}

	/**
	 * Returns the {@link PojoSwap} associated with this class.
	 *
	 * @return The {@link PojoSwap} associated with this class, or <jk>null</jk> if there is no POJO swap
	 * 	associated with this class.
	 */
	public PojoSwap<T,?> getPojoSwap() {
		return pojoSwap;
	}

	/**
	 * Returns the {@link BeanMeta} associated with this class.
	 *
	 * @return The {@link BeanMeta} associated with this class, or <jk>null</jk> if there is no bean meta
	 * 	associated with this class.
	 */
	public BeanMeta<T> getBeanMeta() {
		return beanMeta;
	}

	/**
	 * Returns the no-arg constructor for this class.
	 *
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public Constructor<? extends T> getConstructor() {
		return noArgConstructor;
	}

	/**
	 * Returns the language-specified extended metadata on this class.
	 *
	 * @param extMetaClass The name of the metadata class to create.
	 * @return Extended metadata on this class.  Never <jk>null</jk>.
	 */
	public <M extends ClassMetaExtended> M getExtendedMeta(Class<M> extMetaClass) {
		return extMeta.get(extMetaClass, this);
	}

	/**
	 * Returns the interface proxy invocation handler for this class.
	 *
	 * @return The interface proxy invocation handler, or <jk>null</jk> if it does not exist.
	 */
	public InvocationHandler getProxyInvocationHandler() {
		if (invocationHandler == null && beanMeta != null && beanContext.useInterfaceProxies && innerClass.isInterface())
			invocationHandler = new BeanProxyInvocationHandler<T>(beanMeta);
		return invocationHandler;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 *
	 * @return <jk>true</jk> if a new instance of this class can be constructed.
	 */
	public boolean canCreateNewInstance() {
		if (isMemberClass)
			return false;
		if (noArgConstructor != null)
			return true;
		if (getProxyInvocationHandler() != null)
			return true;
		if (isArray() && elementType.canCreateNewInstance())
			return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match
	 * 	the class type of the defining class.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if a new instance of this class can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewInstance(Object outer) {
		if (isMemberClass)
			return outer != null && noArgConstructor != null && noArgConstructor.getParameterTypes()[0] == outer.getClass();
		return canCreateNewInstance();
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated as a bean.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match
	 * 	the class type of the defining class.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if a new instance of this bean can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewBean(Object outer) {
		if (beanMeta == null)
			return false;
		// Beans with transforms with subtype properties are assumed to be constructable.
		if (beanFilter != null && beanFilter.getSubTypeProperty() != null)
			return true;
		if (beanMeta.constructor == null)
			return false;
		if (isMemberClass)
			return outer != null && beanMeta.constructor.getParameterTypes()[0] == outer.getClass();
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromString(Object outer) {
		if (fromStringMethod != null)
			return true;
		if (stringConstructor != null) {
			if (isMemberClass)
				return outer != null && stringConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromNumber(Object outer) {
		if (numberConstructor != null) {
			if (isMemberClass)
				return outer != null && numberConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns the class type of the parameter of the numeric constructor.
	 *
	 * @return The class type of the numeric constructor, or <jk>null</jk> if no such constructor exists.
	 */
	public Class<? extends Number> getNewInstanceFromNumberClass() {
		return numberConstructorType;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromObjectMap(Object outer) {
		if (objectMapConstructor != null) {
			if (isMemberClass)
				return outer != null && objectMapConstructor.getParameterTypes()[0] == outer.getClass();
			return true;
		}
		return false;
	}

	/**
	 * Returns the reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 *
	 * @return The reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	public synchronized String getNotABeanReason() {
		return notABeanReason;
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * Returns any exception that was throw in the <code>init()</code> method.
	 *
	 * @return The cached exception.
	 */
	public Throwable getInitException() {
		return initException;
	}

	/**
	 * Returns the {@link BeanContext} that created this object.
	 *
	 * @return The bean context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>String</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public static</jk> T valueOf(String in);</code>
	 * 	<li><code><jk>public static</jk> T fromString(String in);</code>
	 * 	<li><code><jk>public</jk> T(String in);</code>
	 * </ul>
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no string constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstanceFromString(Object outer, String arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Method m = fromStringMethod;
		if (m != null)
			return (T)m.invoke(null, arg);
		Constructor<T> con = stringConstructor;
		if (con != null) {
			if (isMemberClass)
				return con.newInstance(outer, arg);
			return con.newInstance(arg);
		}
		throw new InstantiationError("No string constructor or valueOf(String) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type from a <code>Number</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(Number in);</code>
	 * </ul>
	 *
	 * @param session The current bean session.
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no numeric constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromNumber(BeanSession session, Object outer, Number arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> con = numberConstructor;
		if (con != null) {
			Object arg2 = session.convertToType(arg, numberConstructor.getParameterTypes()[0]);
			if (isMemberClass)
				return con.newInstance(outer, arg2);
			return con.newInstance(arg2);
		}
		throw new InstantiationError("No string constructor or valueOf(Number) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type from an <code>ObjectMap</code> input.
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public</jk> T(ObjectMap in);</code>
	 * </ul>
	 *
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromObjectMap(Object outer, ObjectMap arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> con = objectMapConstructor;
		if (con != null) {
			if (isMemberClass)
				return con.newInstance(outer, arg);
			return con.newInstance(arg);
		}
		throw new InstantiationError("No map constructor method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type.
	 *
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isArray())
			return (T)Array.newInstance(getInnerClass().getComponentType(), 0);
		Constructor<? extends T> con = getConstructor();
		if (con != null)
			return con.newInstance((Object[])null);
		InvocationHandler h = getProxyInvocationHandler();
		if (h != null)
			return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { getInnerClass(), java.io.Serializable.class }, h);
		if (isArray())
			return (T)Array.newInstance(this.elementType.innerClass,0);
		return null;
	}

	/**
	 * Same as {@link #newInstance()} except for instantiating non-static member classes.
	 *
	 * @param outer The instance of the owning object of the member class instance.  Can be <jk>null</jk> if instantiating a non-member or static class.
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If one of the following occurs:
	 * 	<ul class='spaced-list'>
	 * 		<li>The number of actual and formal parameters differ.
	 * 		<li>An unwrapping conversion for primitive arguments fails.
	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
	 * 		<li>The constructor pertains to an enum type.
	 * 	</ul>
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstance(Object outer) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (isMemberClass)
			return noArgConstructor.newInstance(outer);
		return newInstance();
	}

	/**
	 * Checks to see if the specified class type is the same as this one.
	 *
	 * @param t The specified class type.
	 * @return <jk>true</jk> if the specified class type is the same as the class for this type.
	 */
	@Override /* Object */
	public boolean equals(Object t) {
		if (t == null || ! (t instanceof ClassMeta))
			return false;
		ClassMeta<?> t2 = (ClassMeta<?>)t;
		return t2.getInnerClass() == this.getInnerClass();
	}

	/**
	 * Similar to {@link #equals(Object)} except primitive and Object types that are similar
	 * are considered the same. (e.g. <jk>boolean</jk> == <code>Boolean</code>).
	 *
	 * @param cm The class meta to compare to.
	 * @return <jk>true</jk> if the specified class-meta is equivalent to this one.
	 */
	public boolean same(ClassMeta<?> cm) {
		if (equals(cm))
			return true;
		return (isPrimitive() && cc == cm.cc);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}

	/**
	 * Same as {@link #toString()} except use simple class names.
	 *
	 * @param simple Print simple class names only (no package).
	 * @return A new string.
	 */
	public String toString(boolean simple) {
		return toString(new StringBuilder(), simple).toString();
	}

	/**
	 * Appends this object as a readable string to the specified string builder.
	 *
	 * @param sb The string builder to append this object to.
	 * @param simple Print simple class names only (no package).
	 * @return The same string builder passed in (for method chaining).
	 */
	protected StringBuilder toString(StringBuilder sb, boolean simple) {
		String n = innerClass.getName();
		if (simple) {
			int i = n.lastIndexOf('.');
			n = n.substring(i == -1 ? 0 : i+1).replace('$', '.');
		}
		if (cc == ARRAY)
			return elementType.toString(sb, simple).append('[').append(']');
		if (cc == MAP)
			return sb.append(n).append(keyType.isObject() && valueType.isObject() ? "" : "<"+keyType.toString(simple)+","+valueType.toString(simple)+">");
		if (cc == BEANMAP)
			return sb.append(BeanMap.class.getName()).append('<').append(n).append('>');
		if (cc == COLLECTION)
			return sb.append(n).append(elementType.isObject() ? "" : "<"+elementType.toString(simple)+">");
		if (cc == OTHER && beanMeta == null) {
			if (simple)
				return sb.append(n);
			sb.append("OTHER-").append(n).append(",notABeanReason=").append(notABeanReason);
			if (initException != null)
				sb.append(",initException=").append(initException);
			return sb;
		}
		return sb.append(n);
	}

	static class LocaleAsString {
		private static Method forLanguageTagMethod;
		static {
			try {
				forLanguageTagMethod = Locale.class.getMethod("forLanguageTag", String.class);
			} catch (NoSuchMethodException e) {}
		}

		public static final Locale fromString(String localeString) {
			if (forLanguageTagMethod != null) {
				if (localeString.indexOf('_') != -1)
					localeString = localeString.replace('_', '-');
				try {
					return (Locale)forLanguageTagMethod.invoke(null, localeString);
				} catch (Exception e) {
					throw new BeanRuntimeException(e);
				}
			}
			String[] v = localeString.toString().split("[\\-\\_]");
			if (v.length == 1)
				return new Locale(v[0]);
			else if (v.length == 2)
				return new Locale(v[0], v[1]);
			else if (v.length == 3)
				return new Locale(v[0], v[1], v[2]);
			throw new BeanRuntimeException("Could not convert string ''{0}'' to a Locale.", localeString);
		}
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}
