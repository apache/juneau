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
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static java.util.Arrays.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.URI;
import java.time.temporal.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.swap.*;

/**
 * A wrapper class around the {@link Class} object that provides cached information about that class.
 *
 * <p>
 * Instances of this class can be created through the {@link BeanContext#getClassMeta(Class)} method.
 *
 * <p>
 * The {@link BeanContext} class will cache and reuse instances of this class except for the following class types:
 * <ul>
 * 	<li>Arrays
 * 	<li>Maps with non-Object key/values.
 * 	<li>Collections with non-Object key/values.
 * </ul>
 *
 * <p>
 * This class is tied to the {@link BeanContext} class because it's that class that makes the determination of what is
 * a bean.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties="innerClass,classCategory,elementType,keyType,valueType,notABeanReason,initException,beanMeta")
public final class ClassMeta<T> implements Type {

	/** Class categories. */
	enum ClassCategory {
		MAP, COLLECTION, CLASS, METHOD, NUMBER, DECIMAL, BOOLEAN, CHAR, DATE, ARRAY, ENUM, OTHER, CHARSEQ, STR, OBJ, URI, BEANMAP, READER, INPUTSTREAM, VOID, ARGS, OPTIONAL
	}

	final Class<T> innerClass;                              // The class being wrapped.
	final ClassInfo info;

	private final Class<? extends T> implClass;             // The implementation class to use if this is an interface.
	private final ClassCategory cc;                         // The class category.
	private final Method fromStringMethod;                  // The static valueOf(String) or fromString(String) or forString(String) method (if it has one).
	private final ConstructorInfo
		noArgConstructor,                                    // The no-arg constructor for this class (if it has one).
		stringConstructor;                                   // The X(String) constructor (if it has one).
	private final Method
		exampleMethod;                                       // The example() or @Example-annotated method (if it has one).
	private final Field
		exampleField;                                        // The @Example-annotated field (if it has one).
	private final Setter
		namePropertyMethod,                                  // The method to set the name on an object (if it has one).
		parentPropertyMethod;                                // The method to set the parent on an object (if it has one).
	private final boolean
		isDelegate,                                          // True if this class extends Delegate.
		isAbstract,                                          // True if this class is abstract.
		isMemberClass;                                       // True if this is a non-static member class.
	private final Object primitiveDefault;                  // Default value for primitive type classes.
	private final Map<String,Method>
		publicMethods;                                       // All public methods, including static methods.
	private final ObjectSwap<?,?>[] childSwaps;              // Any ObjectSwaps where the normal type is a subclass of this class.
	private final ConcurrentHashMap<Class<?>,ObjectSwap<?,?>>
		childSwapMap,                                        // Maps normal subclasses to ObjectSwaps.
		childUnswapMap;                                      // Maps swap subclasses to ObjectSwaps.
	private final ObjectSwap<T,?>[] swaps;                     // The object POJO swaps associated with this bean (if it has any).
	private final BuilderSwap<T,?> builderSwap;             // The builder swap associated with this bean (if it has one).
	private final BeanContext beanContext;                  // The bean context that created this object.
	private final ClassMeta<?>
		elementType,                                         // If ARRAY or COLLECTION, the element class type.
		keyType,                                             // If MAP, the key class type.
		valueType;                                           // If MAP, the value class type.
	private final BeanMeta<T> beanMeta;                     // The bean meta for this bean class (if it's a bean).
	private final String
		typePropertyName,                                    // The property name of the _type property for this class and subclasses.
		notABeanReason,                                      // If this isn't a bean, the reason why.
		dictionaryName;                                      // The dictionary name of this class if it has one.
	private final Throwable initException;                  // Any exceptions thrown in the init() method.
	private final InvocationHandler invocationHandler;      // The invocation handler for this class (if it has one).
	private final BeanRegistry beanRegistry;                // The bean registry of this class meta (if it has one).
	private final ClassMeta<?>[] args;                      // Arg types if this is an array of args.
	private final String example;                           // Example JSON.
	private final Map<Class<?>,Mutater<?,T>> fromMutaters = new ConcurrentHashMap<>();
	private final Map<Class<?>,Mutater<T,?>> toMutaters = new ConcurrentHashMap<>();
	private final Mutater<String,T> stringMutater;
	private final Map<Class<?>,Annotation[]> annotationArrayMap = new ConcurrentHashMap<>();
	private final Map<Class<?>,Optional<?>> annotationLastMap = new ConcurrentHashMap<>();
	private final Map<String,Optional<?>> properties = new ConcurrentHashMap<>();
	private final BiMap<Object,String> enumValues;

	private final SimpleReadWriteLock lock = new SimpleReadWriteLock(false);

	/**
	 * Construct a new {@code ClassMeta} based on the specified {@link Class}.
	 *
	 * @param innerClass The class being wrapped.
	 * @param beanContext The bean context that created this object.
	 * @param implClass
	 * 	For interfaces and abstract classes, this represents the "real" class to instantiate.
	 * 	Can be <jk>null</jk>.
	 * @param swaps
	 * 	The {@link ObjectSwap} programmatically associated with this class.
	 * 	Can be <jk>null</jk>.
	 * @param childSwaps
	 * 	The child {@link ObjectSwap ObjectSwaps} programmatically associated with this class.
	 * 	These are the <c>ObjectSwaps</c> that have normal classes that are subclasses of this class.
	 * 	Can be <jk>null</jk>.
	 * @param delayedInit
	 * 	Don't call init() in constructor.
	 * 	Used for delayed initialization when the possibility of class reference loops exist.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	ClassMeta(Class<T> innerClass, BeanContext beanContext, ObjectSwap<T,?>[] swaps, ObjectSwap<?,?>[] childSwaps) {
		this.innerClass = innerClass;
		this.info = ClassInfo.of(innerClass);
		this.beanContext = beanContext;
		String notABeanReason = null;

		try (SimpleLock x = lock.write()) {
			// We always immediately add this class meta to the bean context cache so that we can resolve recursive references.
			if (beanContext != null && beanContext.cmCache != null && isCacheable(innerClass))
				beanContext.cmCache.put(innerClass, this);

			ClassMetaBuilder<T> builder = new ClassMetaBuilder(innerClass, beanContext, swaps, childSwaps);

			this.cc = builder.cc;
			this.isDelegate = builder.isDelegate;
			this.fromStringMethod = builder.fromStringMethod;
			this.parentPropertyMethod = builder.parentPropertyMethod;
			this.namePropertyMethod = builder.namePropertyMethod;
			this.noArgConstructor = builder.noArgConstructor;
			this.stringConstructor = builder.stringConstructor;
			this.primitiveDefault = builder.primitiveDefault;
			this.publicMethods = builder.publicMethods;
			this.swaps = builder.swaps.isEmpty() ? null : builder.swaps.toArray(new ObjectSwap[builder.swaps.size()]);
			this.builderSwap = builder.builderSwap;
			this.keyType = builder.keyType;
			this.valueType = builder.valueType;
			this.elementType = builder.elementType;
			notABeanReason = builder.notABeanReason;
			this.beanMeta = builder.beanMeta;
			this.initException = builder.initException;
			this.typePropertyName = builder.typePropertyName;
			this.dictionaryName = builder.dictionaryName;
			this.invocationHandler = builder.invocationHandler;
			this.beanRegistry = builder.beanRegistry;
			this.isMemberClass = builder.isMemberClass;
			this.isAbstract = builder.isAbstract;
			this.implClass = builder.implClass;
			this.childUnswapMap = builder.childUnswapMap;
			this.childSwapMap = builder.childSwapMap;
			this.childSwaps = builder.childSwaps;
			this.exampleMethod = builder.exampleMethod;
			this.exampleField = builder.exampleField;
			this.example = builder.example;
			this.args = null;
			this.stringMutater = builder.stringMutater;
			this.enumValues = builder.enumValues == null ? null : builder.enumValues.build();
		} catch (ClassMetaRuntimeException e) {
			notABeanReason = e.getMessage();
			throw e;
		} finally {
			this.notABeanReason = notABeanReason;
		}
	}

	/**
	 * Generated classes shouldn't be cacheable to prevent needlessly filling up the cache.
	 */
	private static boolean isCacheable(Class<?> c) {
		String n = c.getName();
		char x = n.charAt(n.length()-1);  // All generated classes appear to end with digits.
		if (x >= '0' && x <= '9') {
			if (n.indexOf("$$") != -1 || n.startsWith("sun") || n.startsWith("com.sun") || n.indexOf("$Proxy") != -1)
				return false;
		}
		return true;
	}

	/**
	 * Causes thread to wait until constructor has exited.
	 */
	final void waitForInit() {
		try (SimpleLock x = lock.read()) {}
	}

	/**
	 * Copy constructor.
	 *
	 * <p>
	 * Used for creating Map and Collection class metas that shouldn't be cached.
	 */
	ClassMeta(ClassMeta<T> mainType, ClassMeta<?> keyType, ClassMeta<?> valueType, ClassMeta<?> elementType) {
		this.innerClass = mainType.innerClass;
		this.info = mainType.info;
		this.implClass = mainType.implClass;
		this.childSwaps = mainType.childSwaps;
		this.childSwapMap = mainType.childSwapMap;
		this.childUnswapMap = mainType.childUnswapMap;
		this.cc = mainType.cc;
		this.fromStringMethod = mainType.fromStringMethod;
		this.noArgConstructor = mainType.noArgConstructor;
		this.stringConstructor = mainType.stringConstructor;
		this.namePropertyMethod = mainType.namePropertyMethod;
		this.parentPropertyMethod = mainType.parentPropertyMethod;
		this.isDelegate = mainType.isDelegate;
		this.isAbstract = mainType.isAbstract;
		this.isMemberClass = mainType.isMemberClass;
		this.primitiveDefault = mainType.primitiveDefault;
		this.publicMethods = mainType.publicMethods;
		this.beanContext = mainType.beanContext;
		this.elementType = elementType;
		this.keyType = keyType;
		this.valueType = valueType;
		this.invocationHandler = mainType.invocationHandler;
		this.beanMeta = mainType.beanMeta;
		this.typePropertyName = mainType.typePropertyName;
		this.dictionaryName = mainType.dictionaryName;
		this.notABeanReason = mainType.notABeanReason;
		this.swaps = mainType.swaps;
		this.builderSwap = mainType.builderSwap;
		this.initException = mainType.initException;
		this.beanRegistry = mainType.beanRegistry;
		this.exampleMethod = mainType.exampleMethod;
		this.exampleField = mainType.exampleField;
		this.example = mainType.example;
		this.args = null;
		this.stringMutater = mainType.stringMutater;
		this.enumValues = mainType.enumValues;
	}

	/**
	 * Constructor for args-arrays.
	 */
	@SuppressWarnings("unchecked")
	ClassMeta(ClassMeta<?>[] args) {
		this.innerClass = (Class<T>) Object[].class;
		this.info = ClassInfo.of(innerClass);
		this.args = args;
		this.implClass = null;
		this.childSwaps = null;
		this.childSwapMap = null;
		this.childUnswapMap = null;
		this.cc = ARGS;
		this.fromStringMethod = null;
		this.noArgConstructor = null;
		this.stringConstructor = null;
		this.namePropertyMethod = null;
		this.parentPropertyMethod = null;
		this.isDelegate = false;
		this.isAbstract = false;
		this.isMemberClass = false;
		this.primitiveDefault = null;
		this.publicMethods = null;
		this.beanContext = null;
		this.elementType = null;
		this.keyType = null;
		this.valueType = null;
		this.invocationHandler = null;
		this.beanMeta = null;
		this.typePropertyName = null;
		this.dictionaryName = null;
		this.notABeanReason = null;
		this.swaps = null;
		this.builderSwap = null;
		this.initException = null;
		this.beanRegistry = null;
		this.exampleMethod = null;
		this.exampleField = null;
		this.example = null;
		this.stringMutater = null;
		this.enumValues = null;
	}

	@SuppressWarnings({"unchecked","rawtypes","hiding"})
	private final class ClassMetaBuilder<T> {
		Class<T> innerClass;
		ClassInfo ci;
		Class<? extends T> implClass;
		BeanContext beanContext;
		ClassCategory cc = ClassCategory.OTHER;
		boolean
			isDelegate = false,
			isMemberClass = false,
			isAbstract = false;
		Method
			fromStringMethod = null;
		Setter
			parentPropertyMethod = null,
			namePropertyMethod = null;
		ConstructorInfo
			noArgConstructor = null,
			stringConstructor = null;
		Object primitiveDefault = null;
		Map<String,Method>
			publicMethods = map();
		ClassMeta<?>
			keyType = null,
			valueType = null,
			elementType = null;
		String
			typePropertyName = null,
			notABeanReason = null,
			dictionaryName = null;
		Throwable initException = null;
		BeanMeta beanMeta = null;
		List<ObjectSwap> swaps = list();
		BuilderSwap builderSwap;
		InvocationHandler invocationHandler = null;
		BeanRegistry beanRegistry = null;
		ObjectSwap<?,?>[] childSwaps;
		ConcurrentHashMap<Class<?>,ObjectSwap<?,?>>
			childSwapMap,
			childUnswapMap;
		Method exampleMethod;
		Field exampleField;
		String example;
		Mutater<String,T> stringMutater;
		BiMap.Builder<Object,String> enumValues;

		ClassMetaBuilder(Class<T> innerClass, BeanContext beanContext, ObjectSwap<T,?>[] swaps, ObjectSwap<?,?>[] childSwaps) {
			this.innerClass = innerClass;
			this.beanContext = beanContext;
			BeanContext bc = beanContext;

			this.childSwaps = childSwaps;
			if (childSwaps == null) {
				this.childSwapMap = null;
				this.childUnswapMap = null;
			} else {
				this.childSwapMap = new ConcurrentHashMap<>();
				this.childUnswapMap = new ConcurrentHashMap<>();
			}

			Class<T> c = innerClass;
			ci = ClassInfo.of(c);

			if (c.isPrimitive()) {
				if (c == Boolean.TYPE)
					cc = BOOLEAN;
				else if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE || c == Long.TYPE || c == Float.TYPE || c == Double.TYPE) {
					if (c == Float.TYPE || c == Double.TYPE)
						cc = DECIMAL;
					else
						cc = NUMBER;
				}
				else if (c == Character.TYPE)
					cc = CHAR;
				else if (c == void.class || c == Void.class)
					cc = VOID;
			} else {
				if (ci.isChildOf(Delegate.class))
					isDelegate = true;

				if (c == Object.class)
					cc = OBJ;
				else if (c.isEnum())
					cc = ENUM;
				else if (c.equals(Class.class))
					cc = ClassCategory.CLASS;
				else if (ci.isChildOf(Method.class))
					cc = METHOD;
				else if (ci.isChildOf(CharSequence.class)) {
					if (c.equals(String.class))
						cc = STR;
					else
						cc = CHARSEQ;
				}
				else if (ci.isChildOf(Number.class)) {
					if (ci.isChildOfAny(Float.class, Double.class))
						cc = DECIMAL;
					else
						cc = NUMBER;
				}
				else if (ci.isChildOf(Collection.class))
					cc = COLLECTION;
				else if (ci.isChildOf(Map.class)) {
					if (ci.isChildOf(BeanMap.class))
						cc = BEANMAP;
					else
						cc = MAP;
				}
				else if (c == Character.class)
					cc = CHAR;
				else if (c == Boolean.class)
					cc = BOOLEAN;
				else if (ci.isChildOfAny(Date.class, Calendar.class))
					cc = DATE;
				else if (c.isArray())
					cc = ARRAY;
				else if (ci.isChildOfAny(URL.class, URI.class) || ci.hasAnnotation(bc, Uri.class))
					cc = URI;
				else if (ci.isChildOf(Reader.class))
					cc = READER;
				else if (ci.isChildOf(InputStream.class))
					cc = INPUTSTREAM;
				else if (ci.is(Optional.class))
					cc = OPTIONAL;
			}

			isMemberClass = ci.isMemberClass() && ci.isNotStatic();

			// Find static fromString(String) or equivalent method.
			// fromString() must be checked before valueOf() so that Enum classes can create their own
			//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
			// valueOf() is used by enums.
			// parse() is used by the java logging Level class.
			// forName() is used by Class and Charset
			String[] fromStringMethodNames = {"fromString","fromValue","valueOf","parse","parseString","forName","forString"};
			fromStringMethod = optional(
				ci.getPublicMethod(
					x -> x.isStatic()
					&& x.isNotDeprecated()
					&& x.hasReturnType(c)
					&& x.hasParamTypes(String.class)
					&& ArrayUtils.contains(x.getName(), fromStringMethodNames))
				).map(x -> x.inner())
				.orElse(null);

			// Find example() method if present.
			exampleMethod = optional(
				ci.getPublicMethod(
					x -> x.isStatic()
					&& x.isNotDeprecated()
					&& x.hasName("example")
					&& x.hasFuzzyParamTypes(BeanSession.class))
				).map(x -> x.inner())
				.orElse(null);

			ci.forEachAllField(x -> x.hasAnnotation(bc, ParentProperty.class), x -> {
				if (x.isStatic())
					throw new ClassMetaRuntimeException(c, "@ParentProperty used on invalid field ''{0}''.  Must be static.", x);
				parentPropertyMethod = new Setter.FieldSetter(x.accessible().inner());
			});

			ci.forEachAllField(x -> x.hasAnnotation(bc, NameProperty.class), x -> {
				if (x.isStatic())
					throw new ClassMetaRuntimeException(c, "@NameProperty used on invalid field ''{0}''.  Must be static.", x);
				namePropertyMethod = new Setter.FieldSetter(x.accessible().inner());
			});

			ci.forEachDeclaredField(x -> x.hasAnnotation(bc, Example.class), x -> {
				if (! (x.isStatic() && ci.isParentOf(x.getType().inner())))
					throw new ClassMetaRuntimeException(c, "@Example used on invalid field ''{0}''.  Must be static and an instance of the type.", x);
				exampleField = x.accessible().inner();
			});

			// Find @NameProperty and @ParentProperty methods if present.
			List<MethodInfo> methods = ci.getMethods();
			for (int i = methods.size()-1; i >=0; i--) {
				MethodInfo m = methods.get(i);
				if (m.hasAnnotation(bc, ParentProperty.class)) {
					if (m.isStatic() || ! m.hasNumParams(1))
						throw new ClassMetaRuntimeException(c, "@ParentProperty used on invalid method ''{0}''.  Must not be static and have one argument.", m);
					m.setAccessible();
					parentPropertyMethod = new Setter.MethodSetter(m.inner());
				}
				if (m.hasAnnotation(bc, NameProperty.class)) {
					if (m.isStatic() || ! m.hasNumParams(1))
						throw new ClassMetaRuntimeException(c, "@NameProperty used on invalid method ''{0}''.  Must not be static and have one argument.", m);
					m.setAccessible();
					namePropertyMethod = new Setter.MethodSetter(m.inner());
				}
			}

			ci.forEachDeclaredMethod(m -> m.hasAnnotation(bc, Example.class), m -> {
				if (! (m.isStatic() && m.hasFuzzyParamTypes(BeanSession.class) && ci.isParentOf(m.getReturnType().inner())))
					throw new ClassMetaRuntimeException(c, "@Example used on invalid method ''{0}''.  Must be static and return an instance of the declaring class.", m.toString());
				m.setAccessible();
				exampleMethod = m.inner();
			});

			// Note:  Primitive types are normally abstract.
			isAbstract = ci.isAbstract() && ci.isNotPrimitive();

			// Find constructor(String) method if present.
			ci.forEachPublicConstructor(cs -> cs.isPublic() && cs.isNotDeprecated(), cs -> {
				List<ClassInfo> pt = cs.getParamTypes();
				if (pt.size() == (isMemberClass ? 1 : 0) && c != Object.class && ! isAbstract) {
					noArgConstructor = cs;
				} else if (pt.size() == (isMemberClass ? 2 : 1)) {
					ClassInfo arg = pt.get(isMemberClass ? 1 : 0);
					if (arg.is(String.class))
						stringConstructor = cs;
				}
			});

			primitiveDefault = ci.getPrimitiveDefault();

			ci.forEachPublicMethod(
				x -> x.isNotDeprecated(),
				x -> publicMethods.put(x.getSignature(), x.inner())
			);

			BeanFilter beanFilter = findBeanFilter(bc);
			MarshalledFilter marshalledFilter = findMarshalledFilter(bc);

			addAll(this.swaps, swaps);

			if (bc != null)
				this.builderSwap = BuilderSwap.findSwapFromObjectClass(bc, c, bc.getBeanConstructorVisibility(), bc.getBeanMethodVisibility());

			findSwaps(this.swaps, bc);

			if (beanFilter != null) {
				example = beanFilter.getExample();
				implClass = (Class<? extends T>) beanFilter.getImplClass();
			}

			if (marshalledFilter != null) {
				if (example == null)
					example = marshalledFilter.getExample();
				if (implClass == null)
					implClass = (Class<? extends T>) marshalledFilter.getImplClass();
			}

			if (innerClass != Object.class) {
				ClassInfo x = implClass == null ? ci : ClassInfo.of(implClass);
				noArgConstructor = x.getPublicConstructor(y -> y.hasNoParams());
			}

			try {

				// If this is an array, get the element type.
				if (cc == ARRAY)
					elementType = findClassMeta(innerClass.getComponentType());

				// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
				else if (cc == MAP) {
					ClassMeta[] parameters = findParameters();
					if (parameters != null && parameters.length == 2) {
						keyType = parameters[0];
						valueType = parameters[1];
					} else {
						keyType = findClassMeta(Object.class);
						valueType = findClassMeta(Object.class);
					}
				}

				// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
				else if (cc == COLLECTION || cc == OPTIONAL) {
					ClassMeta[] parameters = findParameters();
					if (parameters != null && parameters.length == 1) {
						elementType = parameters[0];
					} else {
						elementType = findClassMeta(Object.class);
					}
				}

				// If the category is unknown, see if it's a bean.
				// Note that this needs to be done after all other initialization has been done.
				else if (cc == OTHER) {

					BeanMeta newMeta = null;
					try {
						newMeta = new BeanMeta(ClassMeta.this, bc, beanFilter, null, implClass == null ? null : noArgConstructor);
						notABeanReason = newMeta.notABeanReason;

						// Always get these even if it's not a bean:
						beanRegistry = newMeta.beanRegistry;
						typePropertyName = newMeta.typePropertyName;

					} catch (RuntimeException e) {
						notABeanReason = e.getMessage();
						throw e;
					}
					if (notABeanReason == null)
						beanMeta = newMeta;
				}

			} catch (NoClassDefFoundError e) {
				initException = e;
			} catch (RuntimeException e) {
				initException = e;
				throw e;
			}

			if (beanMeta != null)
				dictionaryName = beanMeta.getDictionaryName();

			if (beanMeta != null && bc != null && bc.isUseInterfaceProxies() && innerClass.isInterface())
				invocationHandler = new BeanProxyInvocationHandler<T>(beanMeta);

			if (bc != null) {
				bc.forEachAnnotation(Bean.class, c, x -> true, x -> {
					if (x.dictionary().length != 0)
						beanRegistry = new BeanRegistry(bc, null, x.dictionary());
					// This could be a non-bean POJO with a type name.
					if (dictionaryName == null && ! x.typeName().isEmpty())
						dictionaryName = x.typeName();
				});
			}

			if (example == null && bc != null) {
				bc.forEachAnnotation(Example.class, c, x -> ! x.value().isEmpty(), x -> example = x.value());
			}

			if (example == null) {
				switch(cc) {
					case BOOLEAN:
						example = "true";
						break;
					case CHAR:
						example = "a";
						break;
					case CHARSEQ:
					case STR:
						example = "foo";
						break;
					case DECIMAL:
						if (isFloat())
							example = "1.0";
						else if (isDouble())
							example = "1.0";
						break;
					case ENUM:
						Iterator<? extends Enum> i = EnumSet.allOf((Class<? extends Enum>)c).iterator();
						if (i.hasNext())
							example = beanContext.isUseEnumNames() ? i.next().name() : i.next().toString();
						break;
					case NUMBER:
						if (isShort())
							example = "1";
						else if (isInteger())
							example = "1";
						else if (isLong())
							example = "1";
						break;
					case URI:
					case ARGS:
					case ARRAY:
					case BEANMAP:
					case CLASS:
					case COLLECTION:
					case DATE:
					case INPUTSTREAM:
					case MAP:
					case METHOD:
					case OBJ:
					case OTHER:
					case READER:
					case OPTIONAL:
					case VOID:
						break;
				}
			}

			this.stringMutater = Mutaters.get(String.class, c);

			if (cc == ENUM) {
				Class<? extends Enum> ec = (Class<? extends Enum<?>>)c;
				boolean useEnumNames = bc != null && bc.isUseEnumNames();
				enumValues = BiMap.create();
				enumValues.unmodifiable();
				stream(ec.getEnumConstants()).forEach(x -> enumValues.add(x, useEnumNames ? x.name() : x.toString()));
			}
		}

		private BeanFilter findBeanFilter(BeanContext bc) {
			try {
				List<Bean> ba = info.getAnnotations(bc, Bean.class);
				if (! ba.isEmpty())
					return BeanFilter.create(innerClass).applyAnnotations(ba).build();
			} catch (Exception e) {
				throw asRuntimeException(e);
			}
			return null;
		}

		private MarshalledFilter findMarshalledFilter(BeanContext bc) {
			try {
				List<Marshalled> ba = info.getAnnotations(bc, Marshalled.class);
				if (! ba.isEmpty())
					return MarshalledFilter.create(innerClass).applyAnnotations(ba).build();
			} catch (Exception e) {
				throw asRuntimeException(e);
			}
			return null;
		}

		private void findSwaps(List<ObjectSwap> l, BeanContext bc) {

			if (bc != null)
				bc.forEachAnnotation(Swap.class, innerClass, x -> true, x -> l.add(createSwap(x)));

			ObjectSwap defaultSwap = DefaultSwaps.find(ci);
			if (defaultSwap == null)
				defaultSwap = AutoObjectSwap.find(bc, ci);
			if (defaultSwap == null)
				defaultSwap = AutoNumberSwap.find(bc, ci);
			if (defaultSwap == null)
				defaultSwap = AutoMapSwap.find(bc, ci);
			if (defaultSwap == null)
				defaultSwap = AutoListSwap.find(bc, ci);
			if (defaultSwap != null)
				l.add(defaultSwap);
		}

		private ObjectSwap<T,?> createSwap(Swap s) {
			Class<?> c = s.value();
			if (ClassUtils.isVoid(c))
				c = s.impl();
			ClassInfo ci = ClassInfo.of(c);

			if (ci.isChildOf(ObjectSwap.class)) {
				ObjectSwap ps = BeanCreator.of(ObjectSwap.class).type(c).run();
				if (s.mediaTypes().length > 0)
					ps.forMediaTypes(MediaType.ofAll(s.mediaTypes()));
				if (! s.template().isEmpty())
					ps.withTemplate(s.template());
				return ps;
			}

			if (ci.isChildOf(Surrogate.class)) {
				List<SurrogateSwap<?,?>> l = SurrogateSwap.findObjectSwaps(c, beanContext);
				if (! l.isEmpty())
					return (ObjectSwap<T,?>)l.iterator().next();
			}

			throw new ClassMetaRuntimeException(c, "Invalid swap class ''{0}'' specified.  Must extend from ObjectSwap or Surrogate.", c);
		}

		private ClassMeta<?> findClassMeta(Class<?> c) {
			return beanContext.getClassMeta(c, false);
		}

		private ClassMeta<?>[] findParameters() {
			return beanContext.findParameters(innerClass, innerClass);
		}
	}

	/**
	 * Returns the {@link ClassInfo} wrapper for the underlying class.
	 *
	 * @return The {@link ClassInfo} wrapper for the underlying class, never <jk>null</jk>.
	 */
	public ClassInfo getInfo() {
		return info;
	}

	/**
	 * Returns the type property name associated with this class and subclasses.
	 *
	 * <p>
	 * If <jk>null</jk>, <js>"_type"</js> should be assumed.
	 *
	 * @return
	 * 	The type property name associated with this bean class, or <jk>null</jk> if there is no explicit type
	 * 	property name defined or this isn't a bean.
	 */
	public String getBeanTypePropertyName() {
		return typePropertyName;
	}

	/**
	 * Returns the bean dictionary name associated with this class.
	 *
	 * <p>
	 * The lexical name is defined by {@link Bean#typeName() @Bean(typeName)}.
	 *
	 * @return
	 * 	The type name associated with this bean class, or <jk>null</jk> if there is no type name defined or this
	 * 	isn't a bean.
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}

	/**
	 * Returns the bean registry for this class.
	 *
	 * <p>
	 * This bean registry contains names specified in the {@link Bean#dictionary() @Bean(dictionary)} annotation
	 * defined on the class, regardless of whether the class is an actual bean.
	 * This allows interfaces to define subclasses with type names.
	 *
	 * @return The bean registry for this class, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	/**
	 * Returns the category of this class.
	 *
	 * @return The category of this class.
	 */
	public ClassCategory getClassCategory() {
		return cc;
	}

	/**
	 * Returns <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a superclass of or the same as the specified class.
	 */
	public boolean isAssignableFrom(Class<?> c) {
		return info.isChildOf(c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 *
	 * @param c The comparison class.
	 * @return <jk>true</jk> if this class is a subclass of or the same as the specified class.
	 */
	public boolean isInstanceOf(Class<?> c) {
		return info.isParentOf(c);
	}

	/**
	 * Returns <jk>true</jk> if this class or any child classes has a {@link ObjectSwap} associated with it.
	 *
	 * <p>
	 * Used when transforming bean properties to prevent having to look up transforms if we know for certain that no
	 * transforms are associated with a bean property.
	 *
	 * @return <jk>true</jk> if this class or any child classes has a {@link ObjectSwap} associated with it.
	 */
	protected boolean hasChildSwaps() {
		return childSwaps != null;
	}

	/**
	 * Returns the {@link ObjectSwap} where the specified class is the same/subclass of the normal class of one of the
	 * child POJO swaps associated with this class.
	 *
	 * @param normalClass The normal class being resolved.
	 * @return The resolved {@link ObjectSwap} or <jk>null</jk> if none were found.
	 */
	protected ObjectSwap<?,?> getChildObjectSwapForSwap(Class<?> normalClass) {
		if (childSwapMap != null) {
			ObjectSwap<?,?> s = childSwapMap.get(normalClass);
			if (s == null) {
				for (ObjectSwap<?,?> f : childSwaps)
					if (s == null && f.getNormalClass().isParentOf(normalClass))
						s = f;
				if (s == null)
					s = ObjectSwap.NULL;
				ObjectSwap<?,?> s2 = childSwapMap.putIfAbsent(normalClass, s);
				if (s2 != null)
					s = s2;
			}
			if (s == ObjectSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Returns the {@link ObjectSwap} where the specified class is the same/subclass of the swap class of one of the child
	 * POJO swaps associated with this class.
	 *
	 * @param swapClass The swap class being resolved.
	 * @return The resolved {@link ObjectSwap} or <jk>null</jk> if none were found.
	 */
	protected ObjectSwap<?,?> getChildObjectSwapForUnswap(Class<?> swapClass) {
		if (childUnswapMap != null) {
			ObjectSwap<?,?> s = childUnswapMap.get(swapClass);
			if (s == null) {
				for (ObjectSwap<?,?> f : childSwaps)
					if (s == null && f.getSwapClass().isParentOf(swapClass))
						s = f;
				if (s == null)
					s = ObjectSwap.NULL;
				ObjectSwap<?,?> s2 = childUnswapMap.putIfAbsent(swapClass, s);
				if (s2 != null)
					s = s2;
			}
			if (s == ObjectSwap.NULL)
				return null;
			return s;
		}
		return null;
	}

	/**
	 * Locates the no-arg constructor for the specified class.
	 *
	 * <p>
	 * Constructor must match the visibility requirements specified by parameter 'v'.
	 * If class is abstract, always returns <jk>null</jk>.
	 * Note that this also returns the 1-arg constructor for non-static member classes.
	 *
	 * @param <T> The class from which to locate the no-arg constructor.
	 * @param c The class from which to locate the no-arg constructor.
	 * @param v The minimum visibility.
	 * @return The constructor, or <jk>null</jk> if no no-arg constructor exists with the required visibility.
	 */
	@SuppressWarnings({"unchecked"})
	protected static <T> Constructor<? extends T> findNoArgConstructor(Class<?> c, Visibility v) {
		ClassInfo ci = ClassInfo.of(c);
		if (ci.isAbstract())
			return null;
		boolean isMemberClass = ci.isMemberClass() && ci.isNotStatic();
		ConstructorInfo cc = ci.getPublicConstructor(
			x -> x.isVisible(v)
			&& x.isNotDeprecated()
			&& x.hasNumParams(isMemberClass ? 1 : 0)
		);
		if (cc != null)
			return (Constructor<? extends T>) v.transform(cc.inner());
		return null;
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
	 * Returns the serialized (swapped) form of this class if there is an {@link ObjectSwap} associated with it.
	 *
	 * @param session
	 * 	The bean session.
	 * 	<br>Required because the swap used may depend on the media type being serialized or parsed.
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	public ClassMeta<?> getSerializedClassMeta(BeanSession session) {
		ObjectSwap<T,?> ps = getSwap(session);
		return (ps == null ? this : ps.getSwapClassMeta(session));
	}

	/**
	 * Returns the example of this class.
	 *
	 * @param session
	 * 	The bean session.
	 * 	<br>Required because the example method may take it in as a parameter.
	 * @param jpSession The JSON parser for parsing examples into POJOs.
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public T getExample(BeanSession session, JsonParserSession jpSession) {
		try {
			if (example != null)
				return jpSession.parse(example, this);
			if (exampleMethod != null)
				return (T)MethodInfo.of(exampleMethod).invokeFuzzy(null, session);
			if (exampleField != null)
				return (T)exampleField.get(null);

			if (isCollection()) {
				Object etExample = getElementType().getExample(session, jpSession);
				if (etExample != null) {
					if (canCreateNewInstance()) {
						Collection c = (Collection)newInstance();
						c.add(etExample);
						return (T)c;
					}
					return (T)Collections.singleton(etExample);
				}
			} else if (isArray()) {
				Object etExample = getElementType().getExample(session, jpSession);
				if (etExample != null) {
					Object o = Array.newInstance(getElementType().innerClass, 1);
					Array.set(o, 0, etExample);
					return (T)o;
				}
			} else if (isMap()) {
				Object vtExample = getValueType().getExample(session, jpSession);
				Object ktExample = getKeyType().getExample(session, jpSession);
				if (ktExample != null && vtExample != null) {
					if (canCreateNewInstance()) {
						Map m = (Map)newInstance();
						m.put(ktExample, vtExample);
						return (T)m;
					}
					return (T)Collections.singletonMap(ktExample, vtExample);
				}
			}

			return null;
		} catch (Exception e) {
			throw new ClassMetaRuntimeException(e);
		}
	}

	/**
	 * For array and {@code Collection} types, returns the class type of the components of the array or
	 * {@code Collection}.
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
	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning it's a representation of some other
	 * object.
	 *
	 * @return <jk>true</jk> if this class implements {@link Delegate}.
	 */
	public boolean isDelegate() {
		return isDelegate;
	}

	/**
	 * Returns <jk>true</jk> if the specified class is an exact match for this metadata.
	 *
	 * @param value The value to check against.
	 * @return <jk>true</jk> if the specified class is an exact match for this metadata.
	 */
	public boolean is(Class<?> value) {
		return eq(innerClass, value);
	}

	/**
	 * Returns <jk>true</jk> if this metadata represents the specified type.
	 *
	 * @param c The class to test against.
	 * @return <jk>true</jk> if this metadata represents the specified type.
	 */
	public boolean isChildOf(Class<?> c) {
		return info.isChildOf(c);
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
	 */
	public boolean isMap() {
		return cc == MAP || cc == BEANMAP;
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
	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 */
	public boolean isBeanMap() {
		return cc == BEANMAP;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
	 */
	public boolean isCollection() {
		return cc == COLLECTION;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Optional}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Optional}.
	 */
	public boolean isOptional() {
		return cc == OPTIONAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array.
	 */
	public boolean isCollectionOrArray() {
		return cc == COLLECTION || cc == ARRAY;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array or {@link Optional}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array or {@link Optional}.
	 */
	public boolean isCollectionOrArrayOrOptional() {
		return cc == COLLECTION || cc == ARRAY || cc == OPTIONAL;
	}

	/**
	 * Returns <jk>true</jk> if this class extends from {@link Set}.
	 *
	 * @return <jk>true</jk> if this class extends from {@link Set}.
	 */
	public boolean isSet() {
		return cc == COLLECTION && info.isChildOf(Set.class);
	}

	/**
	 * Returns <jk>true</jk> if this class extends from {@link List}.
	 *
	 * @return <jk>true</jk> if this class extends from {@link List}.
	 */
	public boolean isList() {
		return cc == COLLECTION && info.isChildOf(List.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is <code><jk>byte</jk>[]</code>.
	 *
	 * @return <jk>true</jk> if this class is <code><jk>byte</jk>[]</code>.
	 */
	public boolean isByteArray() {
		return cc == ARRAY && this.innerClass == byte[].class;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Class}.
	 *
	 * @return <jk>true</jk> if this class is {@link Class}.
	 */
	public boolean isClass() {
		return cc == ClassCategory.CLASS;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Method}.
	 *
	 * @return <jk>true</jk> if this class is {@link Method}.
	 */
	public boolean isMethod() {
		return cc == METHOD;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link Enum}.
	 *
	 * @return <jk>true</jk> if this class is an {@link Enum}.
	 */
	public boolean isEnum() {
		return cc == ENUM;
	}

	/**
	 * Returns <jk>true</jk> if this class is an array.
	 *
	 * @return <jk>true</jk> if this class is an array.
	 */
	public boolean isArray() {
		return cc == ARRAY;
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
	 * Returns <jk>true</jk> if this class is {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is {@link Object}.
	 */
	public boolean isObject() {
		return cc == OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is not {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is not {@link Object}.
	 */
	public boolean isNotObject() {
		return cc != OBJ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
	 */
	public boolean isNumber() {
		return cc == NUMBER || cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 */
	public boolean isDecimal() {
		return cc == DECIMAL;
	}

	/**
	 * Returns <jk>true</jk> if this class is either {@link Float} or <jk>float</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Float} or <jk>float</jk>.
	 */
	public boolean isFloat() {
		return innerClass == Float.class || innerClass == float.class;
	}

	/**
	 * Returns <jk>true</jk> if this class is either {@link Double} or <jk>double</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Double} or <jk>double</jk>.
	 */
	public boolean isDouble() {
		return innerClass == Double.class || innerClass == double.class;
	}

	/**
	 * Returns <jk>true</jk> if this class is either {@link Short} or <jk>short</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Short} or <jk>short</jk>.
	 */
	public boolean isShort() {
		return innerClass == Short.class || innerClass == short.class;
	}

	/**
	 * Returns <jk>true</jk> if this class is either {@link Integer} or <jk>int</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Integer} or <jk>int</jk>.
	 */
	public boolean isInteger() {
		return innerClass == Integer.class || innerClass == int.class;
	}

	/**
	 * Returns <jk>true</jk> if this class is either {@link Long} or <jk>long</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Long} or <jk>long</jk>.
	 */
	public boolean isLong() {
		return innerClass == Long.class || innerClass == long.class;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Boolean}.
	 */
	public boolean isBoolean() {
		return cc == BOOLEAN;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 */
	public boolean isCharSequence() {
		return cc == STR || cc == CHARSEQ;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link String}.
	 *
	 * @return <jk>true</jk> if this class is a {@link String}.
	 */
	public boolean isString() {
		return cc == STR;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Character}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Character}.
	 */
	public boolean isChar() {
		return cc == CHAR;
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
	public boolean isDateOrCalendar() {
		return cc == DATE;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar} or {@link Temporal}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar} or {@link Temporal}.
	 */
	public boolean isDateOrCalendarOrTemporal() {
		return cc == DATE || info.isChildOf(Temporal.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date}.
	 */
	public boolean isDate() {
		return cc == DATE && info.isChildOf(Date.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Temporal}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Temporal}.
	 */
	public boolean isTemporal() {
		return info.isChildOf(Temporal.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Calendar}.
	 */
	public boolean isCalendar() {
		return cc == DATE && info.isChildOf(Calendar.class);
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 *
	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 */
	public boolean isUri() {
		return cc == URI;
	}

	/**
	 * Returns <jk>true</jk> if this class is a {@link Reader}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Reader}.
	 */
	public boolean isReader() {
		return cc == READER;
	}

	/**
	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
	 *
	 * @return <jk>true</jk> if this class is an {@link InputStream}.
	 */
	public boolean isInputStream() {
		return cc == INPUTSTREAM;
	}

	/**
	 * Returns <jk>true</jk> if this class is {@link Void} or <jk>void</jk>.
	 *
	 * @return <jk>true</jk> if this class is {@link Void} or <jk>void</jk>.
	 */
	public boolean isVoid() {
		return cc == VOID;
	}

	/**
	 * Returns <jk>true</jk> if this metadata represents an array of argument types.
	 *
	 * @return <jk>true</jk> if this metadata represents an array of argument types.
	 */
	public boolean isArgs() {
		return cc == ARGS;
	}

	/**
	 * Returns the argument types of this meta.
	 *
	 * @return The argument types of this meta, or <jk>null</jk> if this isn't an array of argument types.
	 */
	public ClassMeta<?>[] getArgs() {
		return args;
	}

	/**
	 * Returns the argument metadata at the specified index if this is an args metadata object.
	 *
	 * @param index The argument index.
	 * @return The The argument metadata.  Never <jk>null</jk>.
	 * @throws BeanRuntimeException If this metadata object is not a list of arguments, or the index is out of range.
	 */
	public ClassMeta<?> getArg(int index) {
		if (args != null && index >= 0 && index < args.length)
			return args[index];
		throw new BeanRuntimeException("Invalid argument index specified:  {0}.  Only {1} arguments are defined.", index, args == null ? 0 : args.length);
	}

	/**
	 * Returns <jk>true</jk> if instance of this object can be <jk>null</jk>.
	 *
	 * <p>
	 * Objects can be <jk>null</jk>, but primitives cannot, except for chars which can be represented by
	 * <code>(<jk>char</jk>)0</code>.
	 *
	 * @return <jk>true</jk> if instance of this class can be null.
	 */
	public boolean isNullable() {
		if (innerClass.isPrimitive())
			return cc == CHAR;
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class is abstract.
	 *
	 * @return <jk>true</jk> if this class is abstract.
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * Returns <jk>true</jk> if this class is an inner class.
	 *
	 * @return <jk>true</jk> if this class is an inner class.
	 */
	public boolean isMemberClass() {
		return isMemberClass;
	}

	/**
	 * All public methods on this class including static methods.
	 *
	 * <p>
	 * Keys are method signatures.
	 *
	 * @return The public methods on this class.
	 */
	public Map<String,Method> getPublicMethods() {
		return publicMethods;
	}

	/**
	 * Returns the {@link ObjectSwap} associated with this class that's the best match for the specified session.
	 *
	 * @param session
	 * 	The current bean session.
	 * 	<br>If multiple swaps are associated with a class, only the first one with a matching media type will
	 * 	be returned.
	 * @return
	 * 	The {@link ObjectSwap} associated with this class, or <jk>null</jk> if there are no POJO swaps associated with
	 * 	this class.
	 */
	public ObjectSwap<T,?> getSwap(BeanSession session) {
		if (swaps != null) {
			int matchQuant = 0, matchIndex = -1;

			for (int i = 0; i < swaps.length; i++) {
				int q = swaps[i].match(session);
				if (q > matchQuant) {
					matchQuant = q;
					matchIndex = i;
				}
			}

			if (matchIndex > -1)
				return swaps[matchIndex];
		}
		return null;
	}

	/**
	 * Returns the builder swap associated with this class.
	 *
	 * @param session The current bean session.
	 * @return The builder swap associated with this class, or <jk>null</jk> if it doesn't exist.
	 */
	public BuilderSwap<T,?> getBuilderSwap(BeanSession session) {
		return builderSwap;
	}

	/**
	 * Returns the {@link BeanMeta} associated with this class.
	 *
	 * @return
	 * 	The {@link BeanMeta} associated with this class, or <jk>null</jk> if there is no bean meta associated with
	 * 	this class.
	 */
	public BeanMeta<T> getBeanMeta() {
		return beanMeta;
	}

	/**
	 * Returns the no-arg constructor for this class.
	 *
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public ConstructorInfo getConstructor() {
		return noArgConstructor;
	}

	/**
	 * Returns the no-arg constructor for this class based on the {@link Marshalled#implClass()} value.
	 *
	 * @param conVis The constructor visibility.
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public ConstructorInfo getImplClassConstructor(Visibility conVis) {
		if (implClass != null)
			return ClassInfo.of(implClass).getNoArgConstructor(conVis);
		return null;
	}

	/**
	 * Returns the interface proxy invocation handler for this class.
	 *
	 * @return The interface proxy invocation handler, or <jk>null</jk> if it does not exist.
	 */
	public InvocationHandler getProxyInvocationHandler() {
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
		if (noArgConstructor != null || getProxyInvocationHandler() != null || (isArray() && elementType.canCreateNewInstance()))
			return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match the class type of
	 * the defining class.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return
	 * 	<jk>true</jk> if a new instance of this class can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewInstance(Object outer) {
		if (isMemberClass)
			return outer != null && noArgConstructor != null && noArgConstructor.hasParamTypes(outer.getClass());
		return canCreateNewInstance();
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated as a bean.
	 * Returns <jk>false</jk> if this is a non-static member class and the outer object does not match the class type of
	 * the defining class.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @return
	 * 	<jk>true</jk> if a new instance of this bean can be created within the context of the specified outer object.
	 */
	public boolean canCreateNewBean(Object outer) {
		if (beanMeta == null || beanMeta.constructor == null)
			return false;
		if (isMemberClass)
			return outer != null && beanMeta.constructor.hasParamTypes(outer.getClass());
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class can call the {@link #newInstanceFromString(Object, String)} method.
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.
	 * 	Can be <jk>null</jk> for non-member or static classes.
	 * @return <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 */
	public boolean canCreateNewInstanceFromString(Object outer) {
		if (fromStringMethod != null)
			return true;
		if (stringConstructor != null) {
			if (isMemberClass)
				return outer != null && stringConstructor.hasParamTypes(outer.getClass(), String.class);
			return true;
		}
		return false;
	}

	/**
	 * Returns the method or field annotated with {@link NameProperty @NameProperty}.
	 *
	 * @return
	 * 	The method or field  annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Setter getNameProperty() {
		return namePropertyMethod;
	}

	/**
	 * Returns the method or field annotated with {@link ParentProperty @ParentProperty}.
	 *
	 * @return
	 * 	The method or field annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Setter getParentProperty() {
		return parentPropertyMethod;
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
	 * Returns any exception that was throw in the <c>init()</c> method.
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
	 * If this is an {@link Optional}, returns an empty optional.
	 *
	 * <p>
	 * Note that if this is a nested optional, will recursively create empty optionals.
	 *
	 * @return An empty optional, or <jk>null</jk> if this isn't an optional.
	 */
	public Optional<?> getOptionalDefault() {
		if (isOptional())
			return optional(getElementType().getOptionalDefault());
		return null;
	}

	/**
	 * Converts the specified object to a string.
	 *
	 * @param t The object to convert.
	 * @return The object converted to a string, or <jk>null</jk> if the object was null.
	 */
	public String toString(Object t) {
		if (t == null)
			return null;
		if (isEnum() && beanContext.isUseEnumNames())
			return ((Enum<?>)t).name();
		return t.toString();
	}

	/**
	 * Create a new instance of the main class of this declared type from a <c>String</c> input.
	 *
	 * <p>
	 * In order to use this method, the class must have one of the following methods:
	 * <ul>
	 * 	<li><code><jk>public static</jk> T valueOf(String in);</code>
	 * 	<li><code><jk>public static</jk> T fromString(String in);</code>
	 * 	<li><code><jk>public</jk> T(String in);</code>
	 * </ul>
	 *
	 * @param outer
	 * 	The outer class object for non-static member classes.  Can be <jk>null</jk> for non-member or static classes.
	 * @param arg The input argument value.
	 * @return A new instance of the object, or <jk>null</jk> if there is no string constructor on the object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings({ "unchecked" })
	public T newInstanceFromString(Object outer, String arg) throws ExecutableException {

		if (isEnum()) {
			T t = (T)enumValues.getKey(arg);
			if (t == null && ! beanContext.isIgnoreUnknownEnumValues())
				throw new ExecutableException("Could not resolve enum value '"+arg+"' on class '"+getInnerClass().getName()+"'");
			return t;
		}

		Method m = fromStringMethod;
		if (m != null) {
			try {
				return (T)m.invoke(null, arg);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ExecutableException(e);
			}
		}
		ConstructorInfo c = stringConstructor;
		if (c != null) {
			if (isMemberClass)
				return c.<T>invoke(outer, arg);
			return c.<T>invoke(arg);
		}
		throw new ExecutableException("No string constructor or valueOf(String) method found for class '"+getInnerClass().getName()+"'");
	}

	/**
	 * Create a new instance of the main class of this declared type.
	 *
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@SuppressWarnings("unchecked")
	public T newInstance() throws ExecutableException {
		if (isArray())
			return (T)Array.newInstance(getInnerClass().getComponentType(), 0);
		ConstructorInfo c = getConstructor();
		if (c != null)
			return c.<T>invoke();
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
	 * @param outer
	 * 	The instance of the owning object of the member class instance.
	 * 	Can be <jk>null</jk> if instantiating a non-member or static class.
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public T newInstance(Object outer) throws ExecutableException {
		if (isMemberClass)
			return noArgConstructor.<T>invoke(outer);
		return newInstance();
	}

	/**
	 * Similar to {@link #equals(Object)} except primitive and Object types that are similar are considered the same.
	 * (e.g. <jk>boolean</jk> == <c>Boolean</c>).
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
	 * @return The passed-in string builder.
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
		if (cc == COLLECTION || cc == OPTIONAL)
			return sb.append(n).append(elementType.isObject() ? "" : "<"+elementType.toString(simple)+">");
		return sb.append(n);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is an instance of this class.
	 *
	 * <p>
	 * This is a simple comparison on the base class itself and not on any generic parameters.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is an instance of this class.
	 */
	public boolean isInstance(Object o) {
		if (o != null)
			return info.isParentOf(o.getClass()) || (isPrimitive() && info.getPrimitiveWrapper() == o.getClass());
		return false;
	}

	/**
	 * Returns a readable name for this class (e.g. <js>"java.lang.String"</js>, <js>"boolean[]"</js>).
	 *
	 * @return The readable name for this class.
	 */
	public String getFullName() {
		return info.getFullName();
	}

	/**
	 * Shortcut for calling {@link Class#getName()} on the inner class of this metadata.
	 *
	 * @return The  name of the inner class.
	 */
	public String getName() {
		return innerClass.getName();
	}

	/**
	 * Shortcut for calling {@link Class#getSimpleName()} on the inner class of this metadata.
	 *
	 * @return The simple name of the inner class.
	 */
	public String getSimpleName() {
		return innerClass.getSimpleName();
	}

	/**
	 * Returns <jk>true</jk> if this class has a transform associated with it that allows it to be created from a Reader.
	 *
	 * @return <jk>true</jk> if this class has a transform associated with it that allows it to be created from a Reader.
	 */
	public boolean hasReaderMutater() {
		return hasMutaterFrom(Reader.class);
	}

	/**
	 * Returns the transform for this class for creating instances from a Reader.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<Reader,T> getReaderMutater() {
		return getFromMutater(Reader.class);
	}

	/**
	 * Returns <jk>true</jk> if this class has a transform associated with it that allows it to be created from an InputStream.
	 *
	 * @return <jk>true</jk> if this class has a transform associated with it that allows it to be created from an InputStream.
	 */
	public boolean hasInputStreamMutater() {
		return hasMutaterFrom(InputStream.class);
	}

	/**
	 * Returns the transform for this class for creating instances from an InputStream.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<InputStream,T> getInputStreamMutater() {
		return getFromMutater(InputStream.class);
	}

	/**
	 * Returns <jk>true</jk> if this class has a transform associated with it that allows it to be created from a String.
	 *
	 * @return <jk>true</jk> if this class has a transform associated with it that allows it to be created from a String.
	 */
	public boolean hasStringMutater() {
		return stringMutater != null;
	}

	/**
	 * Returns the transform for this class for creating instances from a String.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<String,T> getStringMutater() {
		return stringMutater;
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated from the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be instantiated from the specified type.
	 */
	public boolean hasMutaterFrom(Class<?> c) {
		return getFromMutater(c) != null;
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated from the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be instantiated from the specified type.
	 */
	public boolean hasMutaterFrom(ClassMeta<?> c) {
		return getFromMutater(c.getInnerClass()) != null;
	}

	/**
	 * Returns <jk>true</jk> if this class can be transformed to the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be transformed to the specified type.
	 */
	public boolean hasMutaterTo(Class<?> c) {
		return getToMutater(c) != null;
	}

	/**
	 * Returns <jk>true</jk> if this class can be transformed to the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be transformed to the specified type.
	 */
	public boolean hasMutaterTo(ClassMeta<?> c) {
		return getToMutater(c.getInnerClass()) != null;
	}

	/**
	 * Transforms the specified object into an instance of this class.
	 *
	 * @param o The object to transform.
	 * @return The transformed object.
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public T mutateFrom(Object o) {
		Mutater t = getFromMutater(o.getClass());
		return (T)(t == null ? null : t.mutate(o));
	}

	/**
	 * Transforms the specified object into an instance of this class.
	 *
	 * @param <O> The transform-to class.
	 * @param o The object to transform.
	 * @param c The class
	 * @return The transformed object.
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public <O> O mutateTo(Object o, Class<O> c) {
		Mutater t = getToMutater(c);
		return (O)(t == null ? null : t.mutate(o));
	}

	/**
	 * Transforms the specified object into an instance of this class.
	 *
	 * @param <O> The transform-to class.
	 * @param o The object to transform.
	 * @param c The class
	 * @return The transformed object.
	 */
	public <O> O mutateTo(Object o, ClassMeta<O> c) {
		return mutateTo(o, c.getInnerClass());
	}

	/**
	 * Returns the transform for this class for creating instances from other object types.
	 *
	 * @param <I> The transform-from class.
	 * @param c The transform-from class.
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <I> Mutater<I,T> getFromMutater(Class<I> c) {
		Mutater t = fromMutaters.get(c);
		if (t == Mutaters.NULL)
			return null;
		if (t == null) {
			t = Mutaters.get(c, innerClass);
			if (t == null)
				t = Mutaters.NULL;
			fromMutaters.put(c, t);
		}
		return t == Mutaters.NULL ? null : t;
	}

	/**
	 * Returns the transform for this class for creating instances from other object types.
	 *
	 * @param <O> The transform-to class.
	 * @param c The transform-from class.
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <O> Mutater<T,O> getToMutater(Class<O> c) {
		Mutater t = toMutaters.get(c);
		if (t == Mutaters.NULL)
			return null;
		if (t == null) {
			t = Mutaters.get(innerClass, c);
			if (t == null)
				t = Mutaters.NULL;
			toMutaters.put(c, t);
		}
		return t == Mutaters.NULL ? null : t;
	}

	/**
	 * Shortcut for calling <code>getInnerClass().getAnnotation(a) != <jk>null</jk></code>.
	 *
	 * @param a The annotation to check for.
	 * @return <jk>true</jk> if the inner class has the annotation.
	 */
	public boolean hasAnnotation(Class<? extends Annotation> a) {
		return getLastAnnotation(a) != null;
	}

	/**
	 * Shortcut for calling <c>getInnerClass().getAnnotation(a)</c>.
	 *
	 * @param <A> The annotation type to look for.
	 * @param a The annotation to retrieve.
	 * @return The specified annotation, or <jk>null</jk> if the class does not have the specified annotation.
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getLastAnnotation(Class<A> a) {
		Optional<A> o = (Optional<A>)annotationLastMap.get(a);
		if (o == null) {
			if (beanContext == null)
				return info.getAnnotation(BeanContext.DEFAULT, a);
			o = optional(info.getAnnotation(beanContext, a));
			annotationLastMap.put(a, o);
		}
		return o.orElse(null);
	}

	/**
	 * Performs an action on all matching annotations of the specified type defined on this class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to search for.
	 * @param filter A predicate to apply to the entries to determine if action should be performed.  Can be <jk>null</jk>.
	 * @param action An action to perform on the entry.
	 * @return This object.
	 */
	public <A extends Annotation> ClassMeta<T> forEachAnnotation(Class<A> type, Predicate<A> filter, Consumer<A> action) {
		A[] array = annotationArray(type);
		if (array == null) {
			if (beanContext == null)
				info.forEachAnnotation(BeanContext.DEFAULT, type, filter, action);
			return this;
		}
		for (A a : array)
			consume(filter, action, a);
		return this;
	}

	/**
	 * Returns the first matching annotation on this class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to search for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be used.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> Optional<A> firstAnnotation(Class<A> type, Predicate<A> filter) {
		A[] array = annotationArray(type);
		if (array == null) {
			if (beanContext == null)
				return Optional.ofNullable(info.firstAnnotation(BeanContext.DEFAULT, type, filter));
			return Optional.empty();
		}
		for (A a : array)
			if (test(filter, a))
				return Optional.of(a);
		return Optional.empty();
	}

	/**
	 * Returns the last matching annotation on this class or parent classes/interfaces in parent-to-child order.
	 *
	 * @param <A> The annotation type to look for.
	 * @param type The annotation to search for.
	 * @param filter A predicate to apply to the entries to determine if annotation should be used.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public <A extends Annotation> Optional<A> lastAnnotation(Class<A> type, Predicate<A> filter) {
		A[] array = annotationArray(type);
		if (array == null) {
			if (beanContext == null)
				return Optional.ofNullable(info.lastAnnotation(BeanContext.DEFAULT, type, filter));
			return Optional.empty();
		}
		for (int i = array.length-1; i >= 0; i--)
			if (test(filter, array[i]))
				return Optional.of(array[i]);
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> A[] annotationArray(Class<A> type) {
		A[] array = (A[])annotationArrayMap.get(type);
		if (array == null && beanContext != null) {
			List<A> l = list();
			info.forEachAnnotation(beanContext, type, x-> true, x -> l.add(x));
			array = (A[])Array.newInstance(type, l.size());
			for (int i = 0; i < l.size(); i++)
				Array.set(array, i, l.get(i));
			annotationArrayMap.put(type, array);
		}
		return array;
	}

	/**
	 * Returns a calculated property on this context.
	 *
	 * @param <T2> The type to convert the property to.
	 * @param name The name of the property.
	 * @param function The function used to create this property.
	 * @return The property value.  Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <T2> Optional<T2> getProperty(String name, Function<ClassMeta<?>,T2> function) {
		Optional<T2> t = (Optional<T2>) properties.get(name);
		if (t == null) {
			t = optional(function.apply(this));
			properties.put(name, t);
		}
		return t;
	}

	@Override /* Object */
	public int hashCode() {
		return innerClass.hashCode();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof ClassMeta) && eq(this, (ClassMeta<?>)o, (x,y)->eq(x.innerClass, y.innerClass));
	}
}
