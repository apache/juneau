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


import java.lang.reflect.*;

/**
 * Information about a class that can be gathered through reflection alone.
 *
 * @param <T> The class type of the wrapped class.
 */
public class ClassMetaSimple<T> implements Type {

//	private static final ConcurrentHashMap<Class<?>,ClassMetaSimple<?>> cache = new ConcurrentHashMap<Class<?>,ClassMetaSimple<?>>();
//
//	/** Class categories. */
//	enum ClassCategory {
//		MAP, COLLECTION, CLASS, NUMBER, DECIMAL, BOOLEAN, CHAR, DATE, ARRAY, ENUM, OTHER, CHARSEQ, STR, OBJ, URI, BEANMAP, READER, INPUTSTREAM
//	}
//
//	final Class<T> innerClass;                              // The class being wrapped.
//	final ClassCategory cc;                                 // The class category.
//	final Method fromStringMethod;                          // The static valueOf(String) or fromString(String) or forString(String) method (if it has one).
//	Constructor<T>
//		noArgConstructor;                                    // The no-arg constructor for this class (if it has one).
//	final Constructor<T>
//		stringConstructor,                                   // The X(String) constructor (if it has one).
//		numberConstructor,                                   // The X(Number) constructor (if it has one).
//		swapConstructor,                                     // The X(Swappable) constructor (if it has one).
//		objectMapConstructor;                                // The X(ObjectMap) constructor (if it has one).
//	final ClassMetaSimple<?>
//		numberConstructorType;                               // The class type of the object in the number constructor.
//	final Class<?> swapMethodType;                          // The class type of the object in the number constructor.
//	final Method
//		toObjectMapMethod,                                   // The toObjectMap() method (if it has one).
//		swapMethod,                                          // The swap() method (if it has one).
//		namePropertyMethod,                                  // The method to set the name on an object (if it has one).
//		parentPropertyMethod;                                // The method to set the parent on an object (if it has one).
//	final boolean
//		isDelegate,                                          // True if this class extends Delegate.
//		isAbstract,                                          // True if this class is abstract.
//		isMemberClass;                                       // True if this is a non-static member class.
//	final Object primitiveDefault;                          // Default value for primitive type classes.
//	final Map<String,Method>
//		remoteableMethods,                                   // Methods annotated with @Remoteable.  Contains all public methods if class is annotated with @Remotable.
//		publicMethods;                                       // All public methods, including static methods.
//
//	ClassMetaSimple<?>
//		keyType,
//		valueType,
//		elementType;
//
//	private static final Boolean BOOLEAN_DEFAULT = false;
//	private static final Character CHARACTER_DEFAULT = (char)0;
//	private static final Short SHORT_DEFAULT = (short)0;
//	private static final Integer INTEGER_DEFAULT = 0;
//	private static final Long LONG_DEFAULT = 0l;
//	private static final Float FLOAT_DEFAULT = 0f;
//	private static final Double DOUBLE_DEFAULT = 0d;
//	private static final Byte BYTE_DEFAULT = (byte)0;
//
//
//	/**
//	 * Constructor.
//	 *
//	 * @param innerClass The class we're gathering information about.
//	 */
//	@SuppressWarnings({"unchecked","rawtypes"})
//	public ClassMetaSimple(Class<T> innerClass) {
//		this.innerClass = innerClass;
//
//		Class<T> c = innerClass;
//		ClassCategory _cc = ClassCategory.OTHER;
//		boolean _isDelegate = false;
//		Method
//			_fromStringMethod = null,
//			_toObjectMapMethod = null,
//			_swapMethod = null,
//			_parentPropertyMethod = null,
//			_namePropertyMethod = null;
//		Constructor<T>
//			_noArgConstructor = null,
//			_stringConstructor = null,
//			_objectMapConstructor = null,
//			_swapConstructor = null,
//			_numberConstructor = null;
//		ClassMetaSimple
//			_numberConstructorType = null,
//			_keyType = null,
//			_valueType = null,
//			_elementType = null;
//		Class<?>
//			_swapMethodType = null;
//		Object _primitiveDefault = null;
//		Map<String,Method>
//			_publicMethods = new LinkedHashMap<String,Method>(),
//			_remoteableMethods = null;
//
//		if (c.isPrimitive()) {
//			if (c == Boolean.TYPE)
//				_cc = BOOLEAN;
//			else if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE || c == Long.TYPE || c == Float.TYPE || c == Double.TYPE) {
//				if (c == Float.TYPE || c == Double.TYPE)
//					_cc = DECIMAL;
//				else
//					_cc = NUMBER;
//			}
//			else if (c == Character.TYPE)
//				_cc = CHAR;
//		} else {
//			if (isParentClass(Delegate.class, c))
//				_isDelegate = true;
//			if (c == Object.class)
//				_cc = OBJ;
//			else if (c.isEnum())
//				_cc = ENUM;
//			else if (c.equals(Class.class))
//				_cc = CLASS;
//			else if (isParentClass(CharSequence.class, c)) {
//				if (c.equals(String.class))
//					_cc = STR;
//				else
//					_cc = CHARSEQ;
//			}
//			else if (isParentClass(Number.class, c)) {
//				if (isParentClass(Float.class, c) || isParentClass(Double.class, c))
//					_cc = DECIMAL;
//				else
//					_cc = NUMBER;
//			}
//			else if (isParentClass(Collection.class, c))
//				_cc = COLLECTION;
//			else if (isParentClass(Map.class, c)) {
//				if (isParentClass(BeanMap.class, c))
//					_cc = BEANMAP;
//				else
//					_cc = MAP;
//			}
//			else if (c == Character.class)
//				_cc = CHAR;
//			else if (c == Boolean.class)
//				_cc = BOOLEAN;
//			else if (isParentClass(Date.class, c) || isParentClass(Calendar.class, c))
//				_cc = DATE;
//			else if (c.isArray())
//				_cc = ARRAY;
//			else if (isParentClass(URL.class, c) || isParentClass(URI.class, c) || c.isAnnotationPresent(org.apache.juneau.annotation.URI.class))
//				_cc = URI;
//			else if (isParentClass(Reader.class, c))
//				_cc = READER;
//			else if (isParentClass(InputStream.class, c))
//				_cc = INPUTSTREAM;
//		}
//
//		isMemberClass = c.isMemberClass() && ! isStatic(c);
//
//		// Find static fromString(String) or equivalent method.
//		// fromString() must be checked before valueOf() so that Enum classes can create their own
//		//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
//		// valueOf() is used by enums.
//		// parse() is used by the java logging Level class.
//		// forName() is used by Class and Charset
//		for (String methodName : new String[]{"fromString","valueOf","parse","parseString","forName","forString"}) {
//			if (_fromStringMethod == null) {
//				for (Method m : c.getMethods()) {
//					if (isStatic(m) && isPublic(m) && isNotDeprecated(m)) {
//						String mName = m.getName();
//						if (mName.equals(methodName) && m.getReturnType() == c) {
//							Class<?>[] args = m.getParameterTypes();
//							if (args.length == 1 && args[0] == String.class) {
//								_fromStringMethod = m;
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
//
//		// Special cases
//		try {
//			if (c == TimeZone.class)
//				_fromStringMethod = c.getMethod("getTimeZone", String.class);
//			else if (c == Locale.class)
//				_fromStringMethod = LocaleAsString.class.getMethod("fromString", String.class);
//		} catch (NoSuchMethodException e1) {}
//
//		// Find toObjectMap() method if present.
//		for (Method m : c.getMethods()) {
//			if (isPublic(m) && isNotDeprecated(m) && ! isStatic(m)) {
//				String mName = m.getName();
//				if (mName.equals("toObjectMap")) {
//					if (m.getParameterTypes().length == 0 && m.getReturnType() == ObjectMap.class) {
//						_toObjectMapMethod = m;
//						break;
//					}
//				} else if (mName.equals("swap")) {
//					if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == BeanSession.class) {
//						_swapMethod = m;
//						_swapMethodType = m.getReturnType();
//						break;
//					}
//				}
//			}
//		}
//
//		// Find @NameProperty and @ParentProperty methods if present.
//		for (Method m : c.getDeclaredMethods()) {
//			if (m.isAnnotationPresent(ParentProperty.class) && m.getParameterTypes().length == 1) {
//				m.setAccessible(true);
//				_parentPropertyMethod = m;
//			}
//			if (m.isAnnotationPresent(NameProperty.class) && m.getParameterTypes().length == 1) {
//				m.setAccessible(true);
//				_namePropertyMethod = m;
//			}
//		}
//
//		// Note:  Primitive types are normally abstract.
//		isAbstract = Modifier.isAbstract(c.getModifiers()) && ! c.isPrimitive();
//
//		// Find constructor(String) method if present.
//		for (Constructor cs : c.getConstructors()) {
//			if (isPublic(cs) && isNotDeprecated(cs)) {
//				Class<?>[] args = cs.getParameterTypes();
//				if (args.length == (isMemberClass ? 1 : 0) && c != Object.class && ! isAbstract) {
//					_noArgConstructor = cs;
//				} else if (args.length == (isMemberClass ? 2 : 1)) {
//					Class<?> arg = args[(isMemberClass ? 1 : 0)];
//					if (arg == String.class)
//						_stringConstructor = cs;
//					else if (ObjectMap.class.isAssignableFrom(arg))
//						_objectMapConstructor = cs;
//					else if (_swapMethodType != null && _swapMethodType.isAssignableFrom(arg))
//						_swapConstructor = cs;
//					else if (_cc != NUMBER && (Number.class.isAssignableFrom(arg) || (arg.isPrimitive() && (arg == int.class || arg == short.class || arg == long.class || arg == float.class || arg == double.class)))) {
//						_numberConstructor = cs;
//						_numberConstructorType = fromCache(ClassUtils.getWrapperIfPrimitive(arg));
//					}
//				}
//			}
//		}
//
//		if (c.isPrimitive()) {
//			if (c == Boolean.TYPE)
//				_primitiveDefault = BOOLEAN_DEFAULT;
//			else if (c == Character.TYPE)
//				_primitiveDefault = CHARACTER_DEFAULT;
//			else if (c == Short.TYPE)
//				_primitiveDefault = SHORT_DEFAULT;
//			else if (c == Integer.TYPE)
//				_primitiveDefault = INTEGER_DEFAULT;
//			else if (c == Long.TYPE)
//				_primitiveDefault = LONG_DEFAULT;
//			else if (c == Float.TYPE)
//				_primitiveDefault = FLOAT_DEFAULT;
//			else if (c == Double.TYPE)
//				_primitiveDefault = DOUBLE_DEFAULT;
//			else if (c == Byte.TYPE)
//				_primitiveDefault = BYTE_DEFAULT;
//		} else {
//			if (c == Boolean.class)
//				_primitiveDefault = BOOLEAN_DEFAULT;
//			else if (c == Character.class)
//				_primitiveDefault = CHARACTER_DEFAULT;
//			else if (c == Short.class)
//				_primitiveDefault = SHORT_DEFAULT;
//			else if (c == Integer.class)
//				_primitiveDefault = INTEGER_DEFAULT;
//			else if (c == Long.class)
//				_primitiveDefault = LONG_DEFAULT;
//			else if (c == Float.class)
//				_primitiveDefault = FLOAT_DEFAULT;
//			else if (c == Double.class)
//				_primitiveDefault = DOUBLE_DEFAULT;
//			else if (c == Byte.class)
//				_primitiveDefault = BYTE_DEFAULT;
//		}
//
//		for (Method m : c.getMethods())
//			if (isPublic(m) && isNotDeprecated(m))
//				_publicMethods.put(ClassUtils.getMethodSignature(m), m);
//
//		if (c.getAnnotation(Remoteable.class) != null) {
//			_remoteableMethods = _publicMethods;
//		} else {
//			for (Method m : c.getMethods()) {
//				if (m.getAnnotation(Remoteable.class) != null) {
//					if (_remoteableMethods == null)
//						_remoteableMethods = new LinkedHashMap<String,Method>();
//					_remoteableMethods.put(ClassUtils.getMethodSignature(m), m);
//				}
//			}
//		}
//
//		if (cc == ARRAY)
//			elementType = fromCache(innerClass.getComponentType());
//
//		// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
//		else if (cc == MAP) {
//			ClassMeta[] parameters = beanContext.findParameters(innerClass, innerClass);
//			if (parameters != null && parameters.length == 2) {
//				keyType = parameters[0];
//				valueType = parameters[1];
//			} else {
//				keyType = beanContext.getClassMeta(Object.class);
//				valueType = beanContext.getClassMeta(Object.class);
//			}
//		}
//
//		// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
//		else if (cc == COLLECTION) {
//			ClassMeta[] parameters = beanContext.findParameters(innerClass, innerClass);
//			if (parameters != null && parameters.length == 1) {
//				elementType = parameters[0];
//			} else {
//				elementType = beanContext.getClassMeta(Object.class);
//			}
//		}
//
//
//
//		this.cc = _cc;
//		this.isDelegate = _isDelegate;
//		this.fromStringMethod = _fromStringMethod;
//		this.toObjectMapMethod = _toObjectMapMethod;
//		this.swapMethod = _swapMethod;
//		this.swapMethodType = _swapMethodType;
//		this.parentPropertyMethod = _parentPropertyMethod;
//		this.namePropertyMethod =_namePropertyMethod;
//		this.noArgConstructor = _noArgConstructor;
//		this.stringConstructor = _stringConstructor;
//		this.objectMapConstructor =_objectMapConstructor;
//		this.swapConstructor = _swapConstructor;
//		this.numberConstructor = _numberConstructor;
//		this.numberConstructorType = _numberConstructorType;
//		this.primitiveDefault = _primitiveDefault;
//		this.publicMethods = _publicMethods;
//		this.remoteableMethods = _remoteableMethods;
//	}
//
////	public ClassMetaSimple(ClassMetaSimple<?> collectionClassMeta, ClassMetaSimple<?> elementType) {
////
////
////	}
//
//
//
//	private static ClassMetaSimple<?> fromCache(Class<?> c) {
//		ClassMetaSimple<?> cm = cache.get(c);
//		if (cm == null) {
//			cm = new ClassMetaSimple(c);
//			cache.putIfAbsent(c, cm);
//			cm = cache.get(c);
//		}
//		return cm;
//	}
//
//	/**
//	 * Returns the category of this class.
//	 *
//	 * @return The category of this class.
//	 */
//	public final ClassCategory getClassCategory() {
//		return cc;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a superclass of or the same as the specified class.
//	 *
//	 * @param child The comparison class.
//	 * @return <jk>true</jk> if this class is a superclass of or the same as the specified class.
//	 */
//	public final boolean isAssignableFrom(Class<?> child) {
//		return isParentClass(this.innerClass, child);
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of or the same as the specified class.
//	 *
//	 * @param parent The comparison class.
//	 * @return <jk>true</jk> if this class is a subclass of or the same as the specified class.
//	 */
//	public final boolean isInstanceOf(Class<?> parent) {
//		return isParentClass(parent, this.innerClass);
//	}
//
//	/**
//	 * Returns the {@link Class} object that this class type wraps.
//	 *
//	 * @return The wrapped class object.
//	 */
//	public final Class<T> getInnerClass() {
//		return innerClass;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning
//	 * 	it's a representation of some other object.
//	 *
//	 * @return <jk>true</jk> if this class implements {@link Delegate}.
//	 */
//	public final boolean isDelegate() {
//		return isDelegate;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
//	 */
//	public final boolean isMap() {
//		return cc == MAP || cc == BEANMAP;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
//	 */
//	public boolean isBeanMap() {
//		return cc == BEANMAP;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
//	 */
//	public boolean isCollection() {
//		return cc == COLLECTION;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
//	 */
//	public boolean isCollectionOrArray() {
//		return cc == COLLECTION || cc == ARRAY;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is {@link Class}.
//	 *
//	 * @return <jk>true</jk> if this class is {@link Class}.
//	 */
//	public boolean isClass() {
//		return cc == CLASS;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is an {@link Enum}.
//	 *
//	 * @return <jk>true</jk> if this class is an {@link Enum}.
//	 */
//	public boolean isEnum() {
//		return cc == ENUM;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is an array.
//	 *
//	 * @return <jk>true</jk> if this class is an array.
//	 */
//	public boolean isArray() {
//		return cc == ARRAY;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is {@link Object}.
//	 *
//	 * @return <jk>true</jk> if this class is {@link Object}.
//	 */
//	public boolean isObject() {
//		return cc == OBJ;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is not {@link Object}.
//	 *
//	 * @return <jk>true</jk> if this class is not {@link Object}.
//	 */
//	public boolean isNotObject() {
//		return cc != OBJ;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
//	 */
//	public boolean isNumber() {
//		return cc == NUMBER || cc == DECIMAL;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
//	 */
//	public boolean isDecimal() {
//		return cc == DECIMAL;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link Boolean}.
//	 */
//	public boolean isBoolean() {
//		return cc == BOOLEAN;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
//	 *
//	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
//	 */
//	public boolean isCharSequence() {
//		return cc == STR || cc == CHARSEQ;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link String}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link String}.
//	 */
//	public boolean isString() {
//		return cc == STR;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link Character}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link Character}.
//	 */
//	public boolean isChar() {
//		return cc == CHAR;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a primitive.
//	 *
//	 * @return <jk>true</jk> if this class is a primitive.
//	 */
//	public boolean isPrimitive() {
//		return innerClass.isPrimitive();
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
//	 */
//	public boolean isDate() {
//		return cc == DATE;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
//	 */
//	public boolean isUri() {
//		return cc == URI;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is a {@link Reader}.
//	 *
//	 * @return <jk>true</jk> if this class is a {@link Reader}.
//	 */
//	public boolean isReader() {
//		return cc == READER;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
//	 *
//	 * @return <jk>true</jk> if this class is an {@link InputStream}.
//	 */
//	public boolean isInputStream() {
//		return cc == INPUTSTREAM;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if instance of this object can be <jk>null</jk>.
//	 * <p>
//	 * 	Objects can be <jk>null</jk>, but primitives cannot, except for chars which can be represented
//	 * 	by <code>(<jk>char</jk>)0</code>.
//	 *
//	 * @return <jk>true</jk> if instance of this class can be null.
//	 */
//	public boolean isNullable() {
//		if (innerClass.isPrimitive())
//			return cc == CHAR;
//		return true;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class or one of it's methods are annotated with {@link Remoteable @Remotable}.
//	 *
//	 * @return <jk>true</jk> if this class is remoteable.
//	 */
//	public boolean isRemoteable() {
//		return remoteableMethods != null;
//	}
//
//	/**
//	 * All methods on this class annotated with {@link Remoteable @Remotable}, or all public methods if class is annotated.
//	 * Keys are method signatures.
//	 *
//	 * @return All remoteable methods on this class.
//	 */
//	public Map<String,Method> getRemoteableMethods() {
//		return remoteableMethods;
//	}
//
//	/**
//	 * All public methods on this class including static methods.
//	 * Keys are method signatures.
//	 *
//	 * @return The public methods on this class.
//	 */
//	public Map<String,Method> getPublicMethods() {
//		return publicMethods;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class has an <code>ObjectMap toObjectMap()</code> method.
//	 *
//	 * @return <jk>true</jk> if class has a <code>toObjectMap()</code> method.
//	 */
//	public boolean hasToObjectMapMethod() {
//		return toObjectMapMethod != null;
//	}
//
//	/**
//	 * Returns the method annotated with {@link NameProperty @NameProperty}.
//	 *
//	 * @return The method annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not exist.
//	 */
//	public Method getNameProperty() {
//		return namePropertyMethod;
// 	}
//
//	/**
//	 * Returns the method annotated with {@link ParentProperty @ParentProperty}.
//	 *
//	 * @return The method annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not exist.
//	 */
//	public Method getParentProperty() {
//		return parentPropertyMethod;
// 	}
//
//	/**
//	 * Converts an instance of this class to an {@link ObjectMap}.
//	 *
//	 * @param t The object to convert to a map.
//	 * @return The converted object, or <jk>null</jk> if method does not have a <code>toObjectMap()</code> method.
//	 * @throws BeanRuntimeException Thrown by <code>toObjectMap()</code> method invocation.
//	 */
//	public ObjectMap toObjectMap(Object t) throws BeanRuntimeException {
//		try {
//			if (toObjectMapMethod != null)
//				return (ObjectMap)toObjectMapMethod.invoke(t);
//			return null;
//		} catch (Exception e) {
//			throw new BeanRuntimeException(e);
//		}
//	}
//
//	/**
//	 * Returns the default value for primitives such as <jk>int</jk> or <jk>Integer</jk>.
//	 *
//	 * @return The default value, or <jk>null</jk> if this class type is not a primitive.
//	 */
//	@SuppressWarnings("unchecked")
//	public T getPrimitiveDefault() {
//		return (T)primitiveDefault;
//	}
//
//	/**
//	 * Returns <jk>true</jk> if the specified object is an instance of this class.
//	 * This is a simple comparison on the base class itself and not on
//	 * any generic parameters.
//	 *
//	 * @param o The object to check.
//	 * @return <jk>true</jk> if the specified object is an instance of this class.
//	 */
//	public boolean isInstance(Object o) {
//		if (o != null)
//			return ClassUtils.isParentClass(this.innerClass, o.getClass());
//		return false;
//	}
//
//	/**
//	 * Returns a readable name for this class (e.g. <js>"java.lang.String"</js>, <js>"boolean[]"</js>).
//	 *
//	 * @return The readable name for this class.
//	 */
//	public String getReadableName() {
//		return ClassUtils.getReadableClassName(this.innerClass);
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
//	 *
//	 * @return <jk>true</jk> if a new instance of this class can be constructed.
//	 */
//	public boolean canCreateNewInstance() {
//		if (isMemberClass)
//			return false;
//		if (noArgConstructor != null)
//			return true;
//		if (isArray() && elementType.canCreateNewInstance())
//			return true;
//		return false;
//	}
//
//	/**
//	 * Create a new instance of the main class of this declared type.
//	 *
//	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
//	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
//	 * @throws IllegalArgumentException If one of the following occurs:
//	 * 	<ul class='spaced-list'>
//	 * 		<li>The number of actual and formal parameters differ.
//	 * 		<li>An unwrapping conversion for primitive arguments fails.
//	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
//	 * 		<li>The constructor pertains to an enum type.
//	 * 	</ul>
//	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
//	 * @throws InvocationTargetException If the underlying constructor throws an exception.
//	 */
//	@SuppressWarnings("unchecked")
//	public T newInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
//		if (isArray())
//			return (T)Array.newInstance(getInnerClass().getComponentType(), 0);
//		Constructor<? extends T> con = noArgConstructor;
//		if (con != null)
//			return con.newInstance((Object[])null);
//		if (isArray())
//			return (T)Array.newInstance(this.elementType.innerClass,0);
//		return null;
//	}
//
//	/**
//	 * Same as {@link #newInstance()} except for instantiating non-static member classes.
//	 *
//	 * @param outer The instance of the owning object of the member class instance.  Can be <jk>null</jk> if instantiating a non-member or static class.
//	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
//	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
//	 * @throws IllegalArgumentException If one of the following occurs:
//	 * 	<ul class='spaced-list'>
//	 * 		<li>The number of actual and formal parameters differ.
//	 * 		<li>An unwrapping conversion for primitive arguments fails.
//	 * 		<li>A parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
//	 * 		<li>The constructor pertains to an enum type.
//	 * 	</ul>
//	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class.
//	 * @throws InvocationTargetException If the underlying constructor throws an exception.
//	 */
//	public T newInstance(Object outer) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
//		if (isMemberClass)
//			return noArgConstructor.newInstance(outer);
//		return newInstance();
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
//	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match
//	 * 	the class type of the defining class.
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @return <jk>true</jk> if a new instance of this class can be created within the context of the specified outer object.
//	 */
//	public boolean canCreateNewInstance(Object outer) {
//		if (isMemberClass)
//			return outer != null && noArgConstructor != null && noArgConstructor.getParameterTypes()[0] == outer.getClass();
//		return canCreateNewInstance();
//	}
//
//
//	/**
//	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
//	 */
//	public boolean canCreateNewInstanceFromString(Object outer) {
//		if (fromStringMethod != null)
//			return true;
//		if (stringConstructor != null) {
//			if (isMemberClass)
//				return outer != null && stringConstructor.getParameterTypes()[0] == outer.getClass();
//			return true;
//		}
//		return false;
//	}
//
//	/**
//	 * Create a new instance of the main class of this declared type from a <code>String</code> input.
//	 * <p>
//	 * In order to use this method, the class must have one of the following methods:
//	 * <ul>
//	 * 	<li><code><jk>public static</jk> T valueOf(String in);</code>
//	 * 	<li><code><jk>public static</jk> T fromString(String in);</code>
//	 * 	<li><code><jk>public</jk> T(String in);</code>
//	 * </ul>
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @param arg The input argument value.
//	 * @return A new instance of the object, or <jk>null</jk> if there is no string constructor on the object.
//	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
//	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
//	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
//	 * 	does not have one of the methods described above.
//	 * @throws InvocationTargetException If the underlying constructor throws an exception.
//	 */
//	@SuppressWarnings("unchecked")
//	public T newInstanceFromString(Object outer, String arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
//		Method m = fromStringMethod;
//		if (m != null)
//			return (T)m.invoke(null, arg);
//		Constructor<T> con = stringConstructor;
//		if (con != null) {
//			if (isMemberClass)
//				return con.newInstance(outer, arg);
//			return con.newInstance(arg);
//		}
//		throw new InstantiationError("No string constructor or valueOf(String) method found for class '"+getInnerClass().getName()+"'");
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
//	 */
//	public boolean canCreateNewInstanceFromNumber(Object outer) {
//		if (numberConstructor != null) {
//			if (isMemberClass)
//				return outer != null && numberConstructor.getParameterTypes()[0] == outer.getClass();
//			return true;
//		}
//		return false;
//	}
//
//	/**
//	 * Returns the class type of the parameter of the numeric constructor.
//	 *
//	 * @return The class type of the numeric constructor, or <jk>null</jk> if no such constructor exists.
//	 */
//	public Class<? extends Number> getNewInstanceFromNumberClass() {
//		return (Class<? extends Number>) numberConstructorType.innerClass;
//	}
//
//	/**
//	 * Create a new instance of the main class of this declared type from a <code>Number</code> input.
//	 * <p>
//	 * In order to use this method, the class must have one of the following methods:
//	 * <ul>
//	 * 	<li><code><jk>public</jk> T(Number in);</code>
//	 * </ul>
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @param arg The input argument value.
//	 * @return A new instance of the object, or <jk>null</jk> if there is no numeric constructor on the object.
//	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
//	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
//	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
//	 * 	does not have one of the methods described above.
//	 * @throws InvocationTargetException If the underlying constructor throws an exception.
//	 */
//	public T newInstanceFromNumber(Object outer, Number arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
//		Constructor<T> con = numberConstructor;
//		if (con != null) {
//			if (isMemberClass)
//				return con.newInstance(outer, arg);
//			return con.newInstance(arg);
//		}
//		throw new InstantiationError("No string constructor or valueOf(Number) method found for class '"+getInnerClass().getName()+"'");
//	}
//
//	/**
//	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
//	 */
//	public boolean canCreateNewInstanceFromObjectMap(Object outer) {
//		if (objectMapConstructor != null) {
//			if (isMemberClass)
//				return outer != null && objectMapConstructor.getParameterTypes()[0] == outer.getClass();
//			return true;
//		}
//		return false;
//	}
//
//	/**
//	 * Create a new instance of the main class of this declared type from an <code>ObjectMap</code> input.
//	 * <p>
//	 * In order to use this method, the class must have one of the following methods:
//	 * <ul>
//	 * 	<li><code><jk>public</jk> T(ObjectMap in);</code>
//	 * </ul>
//	 *
//	 * @param outer The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
//	 * @param arg The input argument value.
//	 * @return A new instance of the object.
//	 * @throws IllegalAccessException If the <code>Constructor</code> object enforces Java language access control and the underlying constructor is inaccessible.
//	 * @throws IllegalArgumentException If the parameter type on the method was invalid.
//	 * @throws InstantiationException If the class that declares the underlying constructor represents an abstract class, or
//	 * 	does not have one of the methods described above.
//	 * @throws InvocationTargetException If the underlying constructor throws an exception.
//	 */
//	public T newInstanceFromObjectMap(Object outer, ObjectMap arg) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException {
//		Constructor<T> con = objectMapConstructor;
//		if (con != null) {
//			if (isMemberClass)
//				return con.newInstance(outer, arg);
//			return con.newInstance(arg);
//		}
//		throw new InstantiationError("No map constructor method found for class '"+getInnerClass().getName()+"'");
//	}
//
//
//	@SuppressWarnings("unchecked")
//	public T convert(Object outer, Object value, ClassMetaSimple vcm) {
//
//		try {
//			// Handle the case of a null value.
//			if (value == null) {
//
//				// If it's a primitive, then use the converters to get the default value for the primitive type.
//				if (isPrimitive())
//					return getPrimitiveDefault();
//
//				// Otherwise, just return null.
//				return null;
//			}
//
//			Class<T> tc = getInnerClass();
//
//			// If no conversion needed, then just return the value.
//			// Don't include maps or collections, because child elements may need conversion.
//			if (tc.isInstance(value))
//					return (T)value;
//
//			if (isPrimitive() && value.toString().isEmpty())
//				return getPrimitiveDefault();
//
//			if (isNumber()) {
//				if (value instanceof Number) {
//					Number n = (Number)value;
//					if (tc == Integer.class)
//						return (T)Integer.valueOf(n.intValue());
//					if (tc == Short.class)
//						return (T)Short.valueOf(n.shortValue());
//					if (tc == Long.class)
//						return (T)Long.valueOf(n.longValue());
//					if (tc == Float.class)
//						return (T)Float.valueOf(n.floatValue());
//					if (tc == Double.class)
//						return (T)Double.valueOf(n.doubleValue());
//					if (tc == Byte.class)
//						return (T)Byte.valueOf(n.byteValue());
//					if (tc == Byte.class)
//						return (T)Byte.valueOf(n.byteValue());
//					if (tc == AtomicInteger.class)
//						return (T)new AtomicInteger(n.intValue());
//					if (tc == AtomicLong.class)
//						return (T)new AtomicLong(n.intValue());
//				} else {
//					if (value.toString().isEmpty())
//						return null;
//					String n = null;
//					if (value instanceof Boolean)
//						n = ((Boolean)value).booleanValue() ? "1" : "0";
//					else
//						n = value.toString();
//					if (tc == Integer.class)
//						return (T)Integer.valueOf(n);
//					if (tc == Short.class)
//						return (T)Short.valueOf(n);
//					if (tc == Long.class)
//						return (T)Long.valueOf(n);
//					if (tc == Float.class)
//						return (T)new Float(n);
//					if (tc == Double.class)
//						return (T)new Double(n);
//					if (tc == Byte.class)
//						return (T)Byte.valueOf(n);
//					if (tc == AtomicInteger.class)
//						return (T)new AtomicInteger(Integer.valueOf(n));
//					if (tc == AtomicLong.class)
//						return (T)new AtomicLong(Long.valueOf(n));
//				}
//			}
//
//			if (vcm.isNumber() && canCreateNewInstanceFromNumber(outer)) {
//				return newInstanceFromNumber(outer, (Number)numberConstructorType.convert(value, outer, null));
//			}
//
//			if (isBoolean()) {
//				if (value instanceof Number)
//					return (T)(Boolean.valueOf(((Number)value).intValue() != 0));
//				return (T)Boolean.valueOf(value.toString());
//			}
//
//			if (isChar()) {
//				String s = value.toString();
//				return (T)Character.valueOf(s.length() == 0 ? 0 : s.charAt(0));
//			}
//
//			if (isString()) {
//				if (vcm.isMap() || vcm.isCollectionOrArray()) {
//					if (JsonSerializer.DEFAULT_LAX != null)
//						return (T)JsonSerializer.DEFAULT_LAX.serialize(value);
//				} else if (vcm.isClass()) {
//					return (T)ClassUtils.getReadableClassName((Class<?>)value);
//				} else {
//					return (T)value.toString();
//				}
//			}
//
//			if (vcm.isString() && canCreateNewInstanceFromString(outer)) {
//				return newInstanceFromString(outer, value.toString());
//			}
//
//			if (isEnum()) {
//				if (canCreateNewInstanceFromString(outer))
//					return (T)newInstanceFromString(outer, value.toString());
//				return (T)Enum.valueOf((Class<? extends Enum>)tc, value.toString());
//			}
//
//			if (isCharSequence()) {
//				Class<?> c = value.getClass();
//				if (c.isArray()) {
//					if (c.getComponentType().isPrimitive()) {
//						ObjectList l = new ObjectList(this);
//						int size = Array.getLength(value);
//						for (int i = 0; i < size; i++)
//							l.add(Array.get(value, i));
//						value = l;
//					}
//					value = new ObjectList((Object[])value);
//				}
//
//				return newInstanceFromString(outer, value.toString());
//			}
//
//			// Handle setting of array properties
//			if (isArray()) {
//				if (vcm.isCollection())
//					return (T)toArray(this, outer, (Collection)value);
//				else if (vcm.isArray())
//					return (T)toArray(this, outer, Arrays.asList((Object[])value));
//				else if (StringUtils.startsWith(value.toString(), '['))
//					return (T)toArray(this, outer, new ObjectList(value.toString()));
//			}
//
//			if (swapConstructor != null && swapMethodType.isInstance(value))
//				return swapConstructor.newInstance(value);
//
//			if (vcm.swapMethodType == innerClass)
//				return (T)vcm.swapMethod.invoke(value, (Object[])null);
//
//			if (innerClass == ObjectMap.class && vcm.toObjectMapMethod != null)
//				return (T)vcm.toObjectMapMethod.invoke(value);
//
//			if (vcm.innerClass == ObjectMap.class && objectMapConstructor != null)
//				return objectMapConstructor.newInstance(value);
//
//			// Target type is some sort of Map that needs to be converted.
//			if (isMap()) {
//				try {
//					if (vcm.isMap()) {
//						Map m = canCreateNewInstance(outer) ? (Map)newInstance(outer) : new ObjectMap();
//						for (Map.Entry e : (Set<Map.Entry>)((Map)value).entrySet()) {
//							Object k = e.getKey();
//							if (keyType.isNotObject()) {
//								if (keyType.isString() && k.getClass() != Class.class)
//									k = k.toString();
//								else
//									k = keyType.convert(k, m, null);
//							}
//							Object v = e.getValue();
//							if (valueType.isNotObject())
//								v = valueType.convert(k, m, null);
//							m.put(k, v);
//						}
//						return (T)m;
//					} else if (! canCreateNewInstanceFromString(outer)) {
//						ObjectMap m = new ObjectMap(value.toString());
//						return convert(m, outer, null);
//					}
//				} catch (Exception e) {
//					throw new InvalidDataConversionException(value.getClass(), innerClass, e);
//				}
//			}
//
//			// Target type is some sort of Collection
//			if (isCollection()) {
//				try {
//					Collection l = canCreateNewInstance(outer) ? (Collection)newInstance(outer) : new ObjectList();
//
//					if (value.getClass().isArray())
//						for (Object o : (Object[])value)
//							l.add(elementType.isObject() ? o : elementType.convert(o, l, null));
//					else if (value instanceof Collection)
//						for (Object o : (Collection)value)
//							l.add(elementType.isObject() ? o : elementType.convert(l, o, null));
//					else if (value instanceof Map)
//						l.add(elementType.isObject() ? value : elementType.convert(l, value, null));
//					else if (! value.toString().isEmpty())
//						throw new InvalidDataConversionException(value.getClass(), innerClass, null);
//					return (T)l;
//				} catch (InvalidDataConversionException e) {
//					throw e;
//				} catch (Exception e) {
//					throw new InvalidDataConversionException(value.getClass(), innerClass, e);
//				}
//			}
//
//		} catch (Exception e) {
//			throw new InvalidDataConversionException(value, this.innerClass, e);
//		}
//
//		throw new InvalidDataConversionException(value, this.innerClass, null);
//
//	}
//
//	public final Object toArray(ClassMetaSimple<?> type, Object outer, Collection<?> list) {
//		if (list == null)
//			return null;
//		ClassMetaSimple<?> componentType = type.elementType;
//		Object array = Array.newInstance(componentType.getInnerClass(), list.size());
//		int i = 0;
//		for (Object o : list) {
//			if (! type.getInnerClass().isInstance(o)) {
//				if (componentType.isArray() && o instanceof Collection)
//					o = toArray(componentType, outer, (Collection<?>)o);
//				else if (o == null && componentType.isPrimitive())
//					o = componentType.getPrimitiveDefault();
//				else
//					o = componentType.convert(o, outer, elementType);
//			}
//			try {
//				Array.set(array, i++, o);
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//				throw e;
//			}
//		}
//		return array;
//	}

}
