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

import static org.apache.juneau.ClassMeta.ClassCategory.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
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
 * @author James Bognar (james.bognar@salesforce.com)
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties={"innerClass","classCategory","elementType","keyType","valueType","notABeanReason","initException","beanMeta"})
public final class ClassMeta<T> implements Type {

	/** Class categories. */
	enum ClassCategory {
		MAP, COLLECTION, CLASS, NUMBER, DECIMAL, BOOLEAN, CHAR, DATE, ARRAY, ENUM, BEAN, UNKNOWN, OTHER, CHARSEQ, STR, OBJ, URI, BEANMAP, READER, INPUTSTREAM
	}

	final BeanContext beanContext;                    // The bean context that created this object.
	ClassCategory classCategory = UNKNOWN;            // The class category.
	final Class<T> innerClass;                        // The class being wrapped.
	ClassMeta<?>
		serializedClassMeta,                          // The transformed class type (if class has swap associated with it).
		elementType = null,                           // If ARRAY or COLLECTION, the element class type.
		keyType = null,                               // If MAP, the key class type.
		valueType = null;                             // If MAP, the value class type.
	InvocationHandler invocationHandler;              // The invocation handler for this class (if it has one).
	volatile BeanMeta<T> beanMeta;                    // The bean meta for this bean class (if it's a bean).
	Method fromStringMethod;                          // The static valueOf(String) or fromString(String) method (if it has one).
	Constructor<? extends T> noArgConstructor;        // The no-arg constructor for this class (if it has one).
	Constructor<T> stringConstructor;                 // The X(String) constructor (if it has one).
	Constructor<T> numberConstructor;                 // The X(Number) constructor (if it has one).
	Class<? extends Number> numberConstructorType;    // The class type of the object in the number constructor.
	Constructor<T> objectMapConstructor;              // The X(ObjectMap) constructor (if it has one).
	Method toObjectMapMethod;                         // The toObjectMap() method (if it has one).
	Method namePropertyMethod;                        // The method to set the name on an object (if it has one).
	Method parentPropertyMethod;                      // The method to set the parent on an object (if it has one).
	String notABeanReason;                            // If this isn't a bean, the reason why.
	PojoSwap<T,?> pojoSwap;                           // The object POJO swap associated with this bean (if it has one).
	BeanFilter<? extends T> beanFilter;               // The bean filter associated with this bean (if it has one).
	boolean
		isDelegate,                                   // True if this class extends Delegate.
		isAbstract,                                   // True if this class is abstract.
		isMemberClass;                                // True if this is a non-static member class.

	private MetadataMap extMeta = new MetadataMap();  // Extended metadata
	private ClassLexicon classLexicon;

	private Throwable initException;                   // Any exceptions thrown in the init() method.
	private boolean hasChildPojoSwaps;                 // True if this class or any subclass of this class has a PojoSwap associated with it.
	private Object primitiveDefault;                   // Default value for primitive type classes.
	private Map<String,Method> remoteableMethods,      // Methods annotated with @Remoteable.  Contains all public methods if class is annotated with @Remotable.
		publicMethods;                                 // All public methods, including static methods.

	private static final Boolean BOOLEAN_DEFAULT = false;
	private static final Character CHARACTER_DEFAULT = (char)0;
	private static final Short SHORT_DEFAULT = (short)0;
	private static final Integer INTEGER_DEFAULT = 0;
	private static final Long LONG_DEFAULT = 0l;
	private static final Float FLOAT_DEFAULT = 0f;
	private static final Double DOUBLE_DEFAULT = 0d;
	private static final Byte BYTE_DEFAULT = (byte)0;

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
		this.innerClass = innerClass;
		this.beanContext = beanContext;
		if (! delayedInit)
			init();
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	ClassMeta init() {

		try {
			beanFilter = findBeanFilter(beanContext);
			pojoSwap = findPojoSwap(beanContext);

			classLexicon = beanContext.getClassLexicon();
			for (Pojo p : ReflectionUtils.findAnnotationsParentFirst(Pojo.class, innerClass))
				if (p.classLexicon().length > 0)
					classLexicon = new ClassLexicon(p.classLexicon());
			for (Bean b : ReflectionUtils.findAnnotationsParentFirst(Bean.class, innerClass))
				if (b.classLexicon().length > 0)
					classLexicon = new ClassLexicon(b.classLexicon());

			serializedClassMeta = (pojoSwap == null ? this : beanContext.getClassMeta(pojoSwap.getSwapClass()));
			if (serializedClassMeta == null)
				serializedClassMeta = this;

			if (innerClass != Object.class) {
				this.noArgConstructor = beanContext.getImplClassConstructor(innerClass, Visibility.PUBLIC);
				if (noArgConstructor == null)
					noArgConstructor = findNoArgConstructor(innerClass, Visibility.PUBLIC);
			}

			this.hasChildPojoSwaps = beanContext.hasChildPojoSwaps(innerClass);

			Class c = innerClass;

			if (c.isPrimitive()) {
				if (c == Boolean.TYPE)
					classCategory = BOOLEAN;
				else if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE || c == Long.TYPE || c == Float.TYPE || c == Double.TYPE) {
					if (c == Float.TYPE || c == Double.TYPE)
						classCategory = DECIMAL;
					else
						classCategory = NUMBER;
				}
				else if (c == Character.TYPE)
					classCategory = CHAR;
			} else {
				if (isParentClass(Delegate.class, c))
					isDelegate = true;
				if (c == Object.class)
					classCategory = OBJ;
				else if (c.isEnum())
					classCategory = ENUM;
				else if (c.equals(Class.class))
					classCategory = CLASS;
				else if (isParentClass(CharSequence.class, c)) {
					if (c.equals(String.class))
						classCategory = STR;
					else
						classCategory = CHARSEQ;
				}
				else if (isParentClass(Number.class, c)) {
					if (isParentClass(Float.class, c) || isParentClass(Double.class, c))
						classCategory = DECIMAL;
					else
						classCategory = NUMBER;
				}
				else if (isParentClass(Collection.class, c))
					classCategory = COLLECTION;
				else if (isParentClass(Map.class, c)) {
					if (isParentClass(BeanMap.class, c))
						classCategory = BEANMAP;
					else
						classCategory = MAP;
				}
				else if (c == Character.class)
					classCategory = CHAR;
				else if (c == Boolean.class)
					classCategory = BOOLEAN;
				else if (isParentClass(Date.class, c) || isParentClass(Calendar.class, c))
					classCategory = DATE;
				else if (c.isArray())
					classCategory = ARRAY;
				else if (isParentClass(URL.class, c) || isParentClass(URI.class, c) || c.isAnnotationPresent(org.apache.juneau.annotation.URI.class))
					classCategory = URI;
				else if (isParentClass(Reader.class, c))
					classCategory = READER;
				else if (isParentClass(InputStream.class, c))
					classCategory = INPUTSTREAM;
			}

			isMemberClass = c.isMemberClass() && ! isStatic(c);

			// Find static fromString(String) or equivalent method.
			// fromString() must be checked before valueOf() so that Enum classes can create their own
			//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
			// valueOf() is used by enums.
			// parse() is used by the java logging Level class.
			// forName() is used by Class and Charset
			for (String methodName : new String[]{"fromString","valueOf","parse","parseString","forName"}) {
				if (this.fromStringMethod == null) {
					for (Method m : c.getMethods()) {
						if (isStatic(m) && isPublic(m) && isNotDeprecated(m)) {
							String mName = m.getName();
							if (mName.equals(methodName) && m.getReturnType() == innerClass) {
								Class<?>[] args = m.getParameterTypes();
								if (args.length == 1 && args[0] == String.class) {
									this.fromStringMethod = m;
									break;
								}
							}
						}
					}
				}
			}

			// Find toObjectMap() method if present.
			for (Method m : c.getMethods()) {
				if (isPublic(m) && isNotDeprecated(m) && ! isStatic(m)) {
					String mName = m.getName();
					if (mName.equals("toObjectMap")) {
						if (m.getParameterTypes().length == 0 && m.getReturnType() == ObjectMap.class) {
							this.toObjectMapMethod = m;
							break;
						}
					}
				}
			}

			// Find @NameProperty and @ParentProperty methods if present.
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(ParentProperty.class) && m.getParameterTypes().length == 1) {
					m.setAccessible(true);
					parentPropertyMethod = m;
				}
				if (m.isAnnotationPresent(NameProperty.class) && m.getParameterTypes().length == 1) {
					m.setAccessible(true);
					namePropertyMethod = m;
				}
			}

			// Find constructor(String) method if present.
			for (Constructor cs : c.getConstructors()) {
				if (isPublic(cs) && isNotDeprecated(cs)) {
					Class<?>[] args = cs.getParameterTypes();
					if (args.length == (isMemberClass ? 2 : 1)) {
						Class<?> arg = args[(isMemberClass ? 1 : 0)];
						if (arg == String.class)
							this.stringConstructor = cs;
						else if (ObjectMap.class.isAssignableFrom(arg))
							this.objectMapConstructor = cs;
						else if (classCategory != NUMBER && (Number.class.isAssignableFrom(arg) || (arg.isPrimitive() && (arg == int.class || arg == short.class || arg == long.class || arg == float.class || arg == double.class)))) {
							this.numberConstructor = cs;
							this.numberConstructorType = (Class<? extends Number>)ClassUtils.getWrapperIfPrimitive(arg);
						}
					}
				}
			}

			// Note:  Primitive types are normally abstract.
			isAbstract = Modifier.isAbstract(c.getModifiers()) && ! isPrimitive();

			// If this is an array, get the element type.
			if (classCategory == ARRAY)
				elementType = beanContext.getClassMeta(innerClass.getComponentType());

			// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
			else if (classCategory == MAP) {
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
			else if (classCategory == COLLECTION) {
				ClassMeta[] parameters = beanContext.findParameters(innerClass, innerClass);
				if (parameters != null && parameters.length == 1) {
					elementType = parameters[0];
				} else {
					elementType = beanContext.getClassMeta(Object.class);
				}
			}

			// If the category is unknown, see if it's a bean.
			// Note that this needs to be done after all other initialization has been done.
			else if (classCategory == UNKNOWN) {

				BeanMeta newMeta = null;
				try {
					newMeta = new BeanMeta(this, beanContext, beanFilter, null);
					notABeanReason = newMeta.notABeanReason;
				} catch (RuntimeException e) {
					notABeanReason = e.getMessage();
					throw e;
				}
				if (notABeanReason != null)
					classCategory = OTHER;
				else {
					beanMeta = newMeta;
					classCategory = BEAN;
				}
			}

			if (c.isPrimitive()) {
				if (c == Boolean.TYPE)
					primitiveDefault = BOOLEAN_DEFAULT;
				else if (c == Character.TYPE)
					primitiveDefault = CHARACTER_DEFAULT;
				else if (c == Short.TYPE)
					primitiveDefault = SHORT_DEFAULT;
				else if (c == Integer.TYPE)
					primitiveDefault = INTEGER_DEFAULT;
				else if (c == Long.TYPE)
					primitiveDefault = LONG_DEFAULT;
				else if (c == Float.TYPE)
					primitiveDefault = FLOAT_DEFAULT;
				else if (c == Double.TYPE)
					primitiveDefault = DOUBLE_DEFAULT;
				else if (c == Byte.TYPE)
					primitiveDefault = BYTE_DEFAULT;
			} else {
				if (c == Boolean.class)
					primitiveDefault = BOOLEAN_DEFAULT;
				else if (c == Character.class)
					primitiveDefault = CHARACTER_DEFAULT;
				else if (c == Short.class)
					primitiveDefault = SHORT_DEFAULT;
				else if (c == Integer.class)
					primitiveDefault = INTEGER_DEFAULT;
				else if (c == Long.class)
					primitiveDefault = LONG_DEFAULT;
				else if (c == Float.class)
					primitiveDefault = FLOAT_DEFAULT;
				else if (c == Double.class)
					primitiveDefault = DOUBLE_DEFAULT;
				else if (c == Byte.class)
					primitiveDefault = BYTE_DEFAULT;
			}
		} catch (NoClassDefFoundError e) {
			this.initException = e;
		} catch (RuntimeException e) {
			this.initException = e;
			throw e;
		}

		if (innerClass.getAnnotation(Remoteable.class) != null) {
			remoteableMethods = getPublicMethods();
		} else {
			for (Method m : innerClass.getMethods()) {
				if (m.getAnnotation(Remoteable.class) != null) {
					if (remoteableMethods == null)
						remoteableMethods = new LinkedHashMap<String,Method>();
					remoteableMethods.put(ClassUtils.getMethodSignature(m), m);
				}
			}
		}
		if (remoteableMethods != null)
			remoteableMethods = Collections.unmodifiableMap(remoteableMethods);

		return this;
	}

	/**
	 * Returns the class lexicon in use for this class.
	 * The order of lookup for the lexicon is as follows:
	 * <ol>
	 * 	<li>Lexicon defined via {@link Bean#classLexicon()} or {@link Pojo#classLexicon()} (or {@link BeanFilter} equivalent).
	 * 	<li>Lexicon defined via {@link BeanContext#BEAN_classLexicon} context property.
	 * </ol>
	 *
	 * @return The class lexicon in use for this class.  Never <jk>null</jk>.
	 */
	public ClassLexicon getClassLexicon() {
		return classLexicon;
	}

	/**
	 * Returns the category of this class.
	 *
	 * @return The category of this class.
	 */
	public ClassCategory getClassCategory() {
		return classCategory;
	}

	/**
	 * Returns <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 */
	public boolean isAssignableFrom(Class<?> c) {
		return isParentClass(innerClass, c);
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
	 * Returns <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 */
	public boolean isInstanceOf(Class<?> c) {
		return isParentClass(c, innerClass);
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

	@SuppressWarnings("unchecked")
	private BeanFilter<? extends T> findBeanFilter(BeanContext context) {
		try {
			if (context == null)
				return null;
			BeanFilter<? extends T> f = context.findBeanFilter(innerClass);
			if (f != null)
				return f;
			List<Bean> ba = ReflectionUtils.findAnnotations(Bean.class, innerClass);
			if (! ba.isEmpty())
				f = new AnnotationBeanFilter<T>(innerClass, ba);
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
				Class<?> c = p.swap();
				if (c != Null.class) {
					if (ClassUtils.isParentClass(PojoSwap.class, c))
						return (PojoSwap<T,?>)c.newInstance();
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
	 * Locates the no-arg constructor for the specified class.
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 *
	 * @param c The class from which to locate the no-arg constructor.
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	@SuppressWarnings({"rawtypes","unchecked"})
	protected static <T> Constructor<? extends T> findNoArgConstructor(Class<T> c, Visibility v) {
		int mod = c.getModifiers();
		if (Modifier.isAbstract(mod))
			return null;
		boolean isMemberClass = c.isMemberClass() && ! isStatic(c);
		for (Constructor cc : c.getConstructors()) {
			mod = cc.getModifiers();
			if (cc.getParameterTypes().length == (isMemberClass ? 1 : 0) && v.isVisible(mod) && isNotDeprecated(cc))
				return v.transform(cc);
		}
		return null;
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
	 * Returns the {@link Class} object that this class type wraps.
	 *
	 * @return The wrapped class object.
	 */
	public Class<T> getInnerClass() {
		return innerClass;
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
	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning
	 * 	it's a representation of some other object.
	 *
	 * @return <jk>true</jk> if this class implements {@link Delegate}.
	 */
	public boolean isDelegate() {
		return isDelegate;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
	 */
	public boolean isMap() {
		return classCategory == MAP || classCategory == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 */
	public boolean isBeanMap() {
		return classCategory == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
	 */
	public boolean isCollection() {
		return classCategory == COLLECTION;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Class}.
	 *
	 * @return <jk>true</jk> if this class is {@link Class}.
	 */
	public boolean isClass() {
		return classCategory == CLASS;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link Enum}.
	 *
	 * @return <jk>true</jk> if this class is an {@link Enum}.
	 */
	public boolean isEnum() {
		return classCategory == ENUM;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() {
		return classCategory == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public boolean isBean() {
		return classCategory == BEAN;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is {@link Object}.
	 */
	public boolean isObject() {
		return classCategory == OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is not {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is not {@link Object}.
	 */
	public boolean isNotObject() {
		return classCategory != OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
	 */
	public boolean isNumber() {
		return classCategory == NUMBER || classCategory == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 */
	public boolean isDecimal() {
		return classCategory == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Boolean}.
	 */
	public boolean isBoolean() {
		return classCategory == BOOLEAN;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 */
	public boolean isCharSequence() {
		return classCategory == STR || classCategory == CHARSEQ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link String}.
	 *
	 * @return <jk>true</jk> if this class is a {@link String}.
	 */
	public boolean isString() {
		return classCategory == STR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Character}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Character}.
	 */
	public boolean isChar() {
		return classCategory == CHAR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a primitive.
	 *
	 * @return <jk>true</jk> if this class is a primitive.
	 */
	public boolean isPrimitive() {
		return innerClass.isPrimitive();
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 */
	public boolean isDate() {
		return classCategory == DATE;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 *
	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 */
	public boolean isUri() {
		return classCategory == URI;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Reader}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Reader}.
	 */
	public boolean isReader() {
		return classCategory == READER;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
	 *
	 * @return <jk>true</jk> if this class is an {@link InputStream}.
	 */
	public boolean isInputStream() {
		return classCategory == INPUTSTREAM;
	}

	/**
	 * Returns <jk>true</jk> if instance of this object can be <jk>null</jk>.
	 * <p>
	 * 	Objects can be <jk>null</jk>, but primitives cannot, except for chars which can be represented
	 * 	by <code>(<jk>char</jk>)0</code>.
	 *
	 * @return <jk>true</jk> if instance of this class can be null.
	 */
	public boolean isNullable() {
		if (innerClass.isPrimitive())
			return classCategory == CHAR;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class or one of it's methods are annotated with {@link Remoteable @Remotable}.
	 *
	 * @return <jk>true</jk> if this class is remoteable.
	 */
	public boolean isRemoteable() {
		return remoteableMethods != null;
	}

	/**
	 * All methods on this class annotated with {@link Remoteable @Remotable}, or all public methods if class is annotated.
	 * Keys are method signatures (see {@link ClassUtils#getMethodSignature(Method)})
	 *
	 * @return All remoteable methods on this class.
	 */
	public Map<String,Method> getRemoteableMethods() {
		return remoteableMethods;
	}

	/**
	 * All public methods on this class including static methods.
	 * Keys are method signatures (see {@link ClassUtils#getMethodSignature(Method)}).
	 *
	 * @return The public methods on this class.
	 */
	public Map<String,Method> getPublicMethods() {
		if (publicMethods == null) {
			synchronized(this) {
				Map<String,Method> map = new LinkedHashMap<String,Method>();
				for (Method m : innerClass.getMethods())
					if (isPublic(m) && isNotDeprecated(m))
						map.put(ClassUtils.getMethodSignature(m), m);
				publicMethods = Collections.unmodifiableMap(map);
			}
		}
		return publicMethods;
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
	 * @param c The name of the metadata class to create.
	 * @return Extended metadata on this class.  Never <jk>null</jk>.
	 */
	public <M extends ClassMetaExtended> M getExtendedMeta(Class<M> c) {
		return extMeta.get(c, this);
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
	 * Returns <jk>true</jk> if this class has an <code>ObjectMap toObjectMap()</code> method.
	 *
	 * @return <jk>true</jk> if class has a <code>toObjectMap()</code> method.
	 */
	public boolean hasToObjectMapMethod() {
		return toObjectMapMethod != null;
	}

	/**
	 * Returns the method annotated with {@link NameProperty @NameProperty}.
	 *
	 * @return The method annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not exist.
	 */
	public Method getNameProperty() {
		return namePropertyMethod;
 	}

	/**
	 * Returns the method annotated with {@link ParentProperty @ParentProperty}.
	 *
	 * @return The method annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not exist.
	 */
	public Method getParentProperty() {
		return parentPropertyMethod;
 	}

	/**
	 * Converts an instance of this class to an {@link ObjectMap}.
	 *
	 * @param t The object to convert to a map.
	 * @return The converted object, or <jk>null</jk> if method does not have a <code>toObjectMap()</code> method.
	 * @throws BeanRuntimeException Thrown by <code>toObjectMap()</code> method invocation.
	 */
	public ObjectMap toObjectMap(Object t) throws BeanRuntimeException {
		try {
			if (toObjectMapMethod != null)
				return (ObjectMap)toObjectMapMethod.invoke(t);
			return null;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
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
	 * Returns the default value for primitives such as <jk>int</jk> or <jk>Integer</jk>.
	 *
	 * @return The default value, or <jk>null</jk> if this class type is not a primitive.
	 */
	@SuppressWarnings("unchecked")
	public T getPrimitiveDefault() {
		return (T)primitiveDefault;
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
		Constructor<T> c = stringConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.newInstance(outer, arg);
			return c.newInstance(arg);
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
	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no numeric constructor on the object.
	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
	 * 	does not have one of the methods described above.
	 * @throws InvocationTargetException If the underlying constructor throws an exception.
	 */
	public T newInstanceFromNumber(Object outer, Number arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
		Constructor<T> c = numberConstructor;
		if (c != null) {
			Object arg2 = beanContext.convertToType(arg, numberConstructor.getParameterTypes()[0]);
			if (isMemberClass)
				return c.newInstance(outer, arg2);
			return c.newInstance(arg2);
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
		Constructor<T> c = objectMapConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.newInstance(outer, arg);
			return c.newInstance(arg);
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
		Constructor<? extends T> c = getConstructor();
		if (c != null)
			return c.newInstance((Object[])null);
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
		String name = innerClass.getName();
		if (simple) {
			int i = name.lastIndexOf('.');
			name = name.substring(i == -1 ? 0 : i+1).replace('$', '.');
		}
		switch(classCategory) {
			case ARRAY:
				return elementType.toString(sb, simple).append('[').append(']');
			case MAP:
				return sb.append(name).append(keyType.isObject() && valueType.isObject() ? "" : "<"+keyType.toString(simple)+","+valueType.toString(simple)+">");
			case BEANMAP:
				return sb.append(BeanMap.class.getName()).append('<').append(name).append('>');
			case COLLECTION:
				return sb.append(name).append(elementType.isObject() ? "" : "<"+elementType.toString(simple)+">");
			case OTHER:
				if (simple)
					return sb.append(name);
				sb.append("OTHER-").append(name).append(",notABeanReason=").append(notABeanReason);
				if (initException != null)
					sb.append(",initException=").append(initException);
				return sb;
			default:
				return sb.append(name);
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified object is an instance of this class.
	 * This is a simple comparison on the base class itself and not on
	 * any generic parameters.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is an instance of this class.
	 */
	public boolean isInstance(Object o) {
		if (o != null)
			return ClassUtils.isParentClass(this.innerClass, o.getClass());
		return false;
	}

	/**
	 * Returns a readable name for this class (e.g. <js>"java.lang.String"</js>, <js>"boolean[]"</js>).
	 *
	 * @return The readable name for this class.
	 */
	public String getReadableName() {
		return ClassUtils.getReadableClassName(this.innerClass);
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
}
