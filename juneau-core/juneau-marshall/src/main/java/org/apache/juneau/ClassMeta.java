/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under  the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau;

import static org.apache.juneau.ClassMeta.Category.*;
import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.time.temporal.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.cp.*;
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
 *
 * @param <T> The class type of the wrapped class.
 */
@Bean(properties = "innerClass,elementType,keyType,valueType,notABeanReason,initException,beanMeta")
public class ClassMeta<T> extends ClassInfoTyped<T> {

	private static class Categories {
		int bits;

		public boolean same(Categories cat) {
			return cat.bits == bits;
		}

		boolean is(Category c) {
			return (bits & c.mask) != 0;
		}

		boolean isUnknown() {
			return bits == 0;
		}

		Categories set(Category c) {
			bits |= c.mask;
			return this;
		}
	}

	enum Category {
		MAP(0),
		COLLECTION(1),
		NUMBER(2),
		DECIMAL(3),
		DATE(4),
		ARRAY(5),
		ENUM(6),
		CHARSEQ(8),
		STR(9),
		URI(10),
		BEANMAP(11),
		READER(12),
		INPUTSTREAM(13),
		ARGS(14),
		CALENDAR(15),
		TEMPORAL(16),
		LIST(17),
		SET(18),
		DELEGATE(19),
		BEAN(20);

		private final int mask;

		Category(int bitPosition) {
			this.mask = 1 << bitPosition;
		}
	}

	/**
	 * Checks if the specified category is set in the bitmap.
	 *
	 * @param category The category to check.
	 * @return {@code true} if the category is set, {@code false} otherwise.
	 */

	/**
	 * Generated classes shouldn't be cacheable to prevent needlessly filling up the cache.
	 */
	private static boolean isCacheable(Class<?> c) {
		var n = c.getName();
		var x = n.charAt(n.length() - 1);  // All generated classes appear to end with digits.
		if (x >= '0' && x <= '9') {
			if (n.indexOf("$$") != -1 || n.startsWith("sun") || n.startsWith("com.sun") || n.indexOf("$Proxy") != -1)
				return false;
		}
		return true;
	}

	private final List<ClassMeta<?>> args;                                     // Arg types if this is an array of args.
	private final BeanContext beanContext;                                     // The bean context that created this object.
	private final Supplier<BuilderSwap<T,?>> builderSwap;                      // The builder swap associated with this bean (if it has one).
	private final Categories cat;                                              // The class category.
	private final Cache<Class<?>,ObjectSwap<?,?>> childSwapMap;                // Maps normal subclasses to ObjectSwaps.
	private final Supplier<List<ObjectSwap<?,?>>> childSwaps;                  // Any ObjectSwaps where the normal type is a subclass of this class.
	private final Cache<Class<?>,ObjectSwap<?,?>> childUnswapMap;              // Maps swap subclasses to ObjectSwaps.
	private final Supplier<String> beanDictionaryName;                             // The dictionary name of this class if it has one.
	private final Supplier<ClassMeta<?>> elementType;                          // If ARRAY or COLLECTION, the element class type.
	private final OptionalSupplier<String> example;                            // Example JSON.
	private final OptionalSupplier<FieldInfo> exampleField;                    // The @Example-annotated field (if it has one).
	private final OptionalSupplier<MethodInfo> exampleMethod;                  // The example() or @Example-annotated method (if it has one).
	private final Supplier<BidiMap<Object,String>> enumValues;
	private final Map<Class<?>,Mutater<?,T>> fromMutaters = new ConcurrentHashMap<>();
	private final OptionalSupplier<MethodInfo> fromStringMethod;               // Static fromString(String) or equivalent method
	private final OptionalSupplier<ClassInfoTyped<? extends T>> implClass;     // The implementation class to use if this is an interface.
	private final Supplier<KeyValueTypes> keyValueTypes;                        // Key and value types for MAP types.
	private final OptionalSupplier<MarshalledFilter> marshalledFilter;
	private final Supplier<Property<T,Object>> nameProperty;                   // The method to set the name on an object (if it has one).
	private final OptionalSupplier<ConstructorInfo> noArgConstructor;          // The no-arg constructor for this class (if it has one).
	private final Supplier<Property<T,Object>> parentProperty;                 // The method to set the parent on an object (if it has one).
	private final Map<String,Optional<?>> properties = new ConcurrentHashMap<>();
	private final Mutater<String,T> stringMutater;
	private final OptionalSupplier<ConstructorInfo> stringConstructor;         // The X(String) constructor (if it has one).
	private final Supplier<List<ObjectSwap<T,?>>> swaps;                       // The object POJO swaps associated with this bean (if it has any).
	private final Map<Class<?>,Mutater<T,?>> toMutaters = new ConcurrentHashMap<>();
	private final OptionalSupplier<BeanMeta.BeanMetaValue<T>> beanMeta;

	private record KeyValueTypes(ClassMeta<?> keyType, ClassMeta<?> valueType) {
		Optional<ClassMeta<?>> optKeyType() { return opt(keyType()); }
		Optional<ClassMeta<?>> optValueType() { return opt(valueType()); }
	}

	/**
	 * Construct a new {@code ClassMeta} based on the specified {@link Class}.
	 *
	 * @param innerClass The class being wrapped.
	 * @param beanContext The bean context that created this object.
	 * @param delayedInit
	 * 	Don't call init() in constructor.
	 * 	Used for delayed initialization when the possibility of class reference loops exist.
	 */
	ClassMeta(Class<T> innerClass, BeanContext beanContext) {
		super(innerClass);
		this.beanContext = beanContext;
		this.cat = new Categories();

		// We always immediately add this class meta to the bean context cache so that we can resolve recursive references.
		if (nn(beanContext) && nn(beanContext.cmCache) && isCacheable(innerClass))
			beanContext.cmCache.put(innerClass, this);

		var ap = beanContext.getAnnotationProvider();

		if (isChildOf(Delegate.class)) {
			cat.set(DELEGATE);
		}
		if (isEnum()) {
			cat.set(ENUM);
		} else if (isChildOf(CharSequence.class)) {
			cat.set(CHARSEQ);
			if (is(String.class)) {
				cat.set(STR);
			}
		} else if (isChildOf(Number.class) || isAny(byte.class, short.class, int.class, long.class, float.class, double.class)) {
			cat.set(NUMBER);
			if (isChildOfAny(Float.class, Double.class) || isAny(float.class, double.class)) {
				cat.set(DECIMAL);
			}
		} else if (isChildOf(Collection.class)) {
			cat.set(COLLECTION);
			if (isChildOf(Set.class)) {
				cat.set(SET);
			} else if (isChildOf(List.class)) {
				cat.set(LIST);
			}
		} else if (isChildOf(Map.class)) {
			cat.set(MAP);
			if (isChildOf(BeanMap.class)) {
				cat.set(BEANMAP);
			}
		} else if (isChildOfAny(Date.class, Calendar.class)) {
			if (isChildOf(Date.class)) {
				cat.set(DATE);
			} else if (isChildOf(Calendar.class)) {
				cat.set(CALENDAR);
			}
		} else if (isChildOf(Temporal.class)) {
			cat.set(TEMPORAL);
		} else if (inner().isArray()) {
			cat.set(ARRAY);
		} else if (isChildOfAny(URL.class, URI.class) || ap.has(Uri.class, this)) {
			cat.set(URI);
		} else if (isChildOf(Reader.class)) {
			cat.set(READER);
		} else if (isChildOf(InputStream.class)) {
			cat.set(INPUTSTREAM);
		}

		beanMeta = memoize(()->findBeanMeta());
		builderSwap = memoize(()->findBuilderSwap());
		childSwapMap = Cache.<Class<?>,ObjectSwap<?,?>>create().supplier(x -> findSwap(x)).build();
		childSwaps = memoize(()->findChildSwaps());
		childUnswapMap = Cache.<Class<?>,ObjectSwap<?,?>>create().supplier(x -> findUnswap(x)).build();
		beanDictionaryName = memoize(()->findBeanDictionaryName());
		elementType = memoize(()->findElementType());
		enumValues = memoize(()->findEnumValues());
		example = memoize(()->findExample());
		exampleField = memoize(()->findExampleField());
		exampleMethod = memoize(()->findExampleMethod());
		fromStringMethod = memoize(()->findFromStringMethod());
		implClass = memoize(()->findImplClass());
		keyValueTypes = memoize(()->findKeyValueTypes());
		marshalledFilter = memoize(()->findMarshalledFilter());
		nameProperty = memoize(()->findNameProperty());
		noArgConstructor = memoize(()->findNoArgConstructor());
		parentProperty = memoize(()->findParentProperty());
		stringConstructor = memoize(()->findStringConstructor());
		swaps = memoize(()->findSwaps());

		this.args = null;
		this.stringMutater = Mutaters.get(String.class, inner());
	}

	protected ObjectSwap<?,?> findSwap(Class<?> c) {
		return childSwaps.get().stream().filter(x -> x.getNormalClass().isParentOf(c)).findFirst().orElse(null);
	}

	protected ObjectSwap<?,?> findUnswap(Class<?> c) {
		return childSwaps.get().stream().filter(x -> x.getSwapClass().isParentOf(c)).findFirst().orElse(null);
	}


	/**
	 * Constructor for args-arrays.
	 */
	@SuppressWarnings("unchecked")
	ClassMeta(List<ClassMeta<?>> args) {
		super((Class<T>)Object[].class);
		this.args = args;
		this.childSwaps = memoize(()->findChildSwaps());
		this.childSwapMap = null;
		this.childUnswapMap = null;
		this.cat = new Categories().set(ARGS);
		this.beanContext = null;
		this.elementType = memoize(()->findElementType());
		this.keyValueTypes = memoize(()->findKeyValueTypes());
		this.beanMeta = memoize(()->findBeanMeta());
		this.swaps = memoize(()->findSwaps());
		this.stringMutater = null;
		this.fromStringMethod = memoize(()->findFromStringMethod());
		this.exampleMethod = memoize(()->findExampleMethod());
		this.parentProperty = memoize(()->findParentProperty());
		this.nameProperty = memoize(()->findNameProperty());
		this.exampleField = memoize(()->findExampleField());
		this.noArgConstructor = memoize(()->findNoArgConstructor());
		this.stringConstructor = memoize(()->findStringConstructor());
		this.marshalledFilter = memoize(()->findMarshalledFilter());
		this.builderSwap = memoize(()->findBuilderSwap());
		this.example = memoize(()->findExample());
		this.implClass = memoize(()->findImplClass());
		this.enumValues = memoize(()->findEnumValues());
		this.beanDictionaryName = memoize(()->findBeanDictionaryName());
	}

	/**
	 * Copy constructor.
	 *
	 * <p>
	 * Used for creating Map and Collection class metas that shouldn't be cached.
	 */
	ClassMeta(ClassMeta<T> mainType, ClassMeta<?> keyType, ClassMeta<?> valueType, ClassMeta<?> elementType) {
		super(mainType.inner());
		this.childSwaps = mainType.childSwaps;
		this.childSwapMap = mainType.childSwapMap;
		this.childUnswapMap = mainType.childUnswapMap;
		this.cat = mainType.cat;
		this.fromStringMethod = mainType.fromStringMethod;
		this.beanContext = mainType.beanContext;
		this.elementType = elementType != null ? memoize(()->elementType) : mainType.elementType;
		this.keyValueTypes = (keyType != null || valueType != null) ? memoize(()->new KeyValueTypes(keyType, valueType)) : mainType.keyValueTypes;
		this.beanMeta = mainType.beanMeta;
		this.swaps = mainType.swaps;
		this.exampleMethod = mainType.exampleMethod;
		this.args = null;
		this.stringMutater = mainType.stringMutater;
		this.parentProperty = mainType.parentProperty;
		this.nameProperty = mainType.nameProperty;
		this.exampleField = mainType.exampleField;
		this.noArgConstructor = mainType.noArgConstructor;
		this.stringConstructor = mainType.stringConstructor;
		this.marshalledFilter = mainType.marshalledFilter;
		this.builderSwap = mainType.builderSwap;
		this.example = mainType.example;
		this.implClass = mainType.implClass;
		this.enumValues = mainType.enumValues;
		this.beanDictionaryName = mainType.beanDictionaryName;
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
		var bm = getBeanMeta();
		if (bm == null || ! bm.hasConstructor())
			return false;
		if (isMemberClass() && isNotStatic())
			return nn(outer) && bm.getConstructor().hasParameterTypes(outer.getClass());
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class has a no-arg constructor or invocation handler.
	 *
	 * @return <jk>true</jk> if a new instance of this class can be constructed.
	 */
	public boolean canCreateNewInstance() {
		if (isMemberClass() && isNotStatic())
			return false;
		var bm = getBeanMeta();
		if (noArgConstructor.isPresent() || (bm != null && bm.getBeanProxyInvocationHandler() != null) || (isArray() && elementType.get().canCreateNewInstance()))
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
		if (isMemberClass() && isNotStatic())
			return nn(outer) && noArgConstructor.map(x -> x.hasParameterTypes(outer.getClass())).orElse(false);
		return canCreateNewInstance();
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
		if (fromStringMethod.isPresent())
			return true;
		if (stringConstructor.isPresent()) {
			if (isMemberClass() && isNotStatic())
				return nn(outer) && stringConstructor.map(x -> x.hasParameterTypes(outer.getClass(), String.class)).orElse(false);
			return true;
		}
		return false;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return (o instanceof ClassMeta<?>) && super.equals(o);
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
		if (beanContext != null) {
			beanContext.getAnnotationProvider().find(type, this).stream().map(AnnotationInfo::inner).filter(x -> filter == null || filter.test(x)).forEach(x -> action.accept(x));
		}
		return this;
	}

	/**
	 * Returns the argument metadata at the specified index if this is an args metadata object.
	 *
	 * @param index The argument index.
	 * @return The The argument metadata.  Never <jk>null</jk>.
	 * @throws BeanRuntimeException If this metadata object is not a list of arguments, or the index is out of range.
	 */
	public ClassMeta<?> getArg(int index) {
		if (nn(args) && index >= 0 && index < args.size())
			return args.get(index);
		throw bex("Invalid argument index specified:  {0}.  Only {1} arguments are defined.", index, args == null ? 0 : args.size());
	}

	/**
	 * Returns the argument types of this meta.
	 *
	 * @return The argument types of this meta, or <jk>null</jk> if this isn't an array of argument types.
	 */
	public List<ClassMeta<?>> getArgs() { return args; }

	/**
	 * Returns the {@link BeanContext} that created this object.
	 *
	 * @return The bean context.
	 */
	public BeanContext getBeanContext() { return beanContext; }

	/**
	 * Returns the {@link BeanMeta} associated with this class.
	 *
	 * @return
	 * 	The {@link BeanMeta} associated with this class, or <jk>null</jk> if there is no bean meta associated with
	 * 	this class.
	 */
	public BeanMeta<T> getBeanMeta() {
		return beanMeta.get().beanMeta();
	}

	/**
	 * Returns the bean registry for this class.
	 *
	 * <p>
	 * This bean registry contains names specified in the {@link Bean#dictionary() @Bean(dictionary)} annotation
	 * defined on the class, regardless of whether the class is an actual bean.
	 * This allows interfaces to define subclasses with type names.
	 *
	 * <p>
	 * This is a shortcut for calling getBeanMeta().getBeanRegistry().
	 *
	 * @return The bean registry for this class, or <jk>null</jk> if no bean registry is associated with it.
	 */
	public BeanRegistry getBeanRegistry() {
		return beanMeta.get().optBeanMeta().map(x -> x.getBeanRegistry()).orElse(null);
	}

	/**
	 * Returns the builder swap associated with this class.
	 *
	 * @param session The current bean session.
	 * @return The builder swap associated with this class, or <jk>null</jk> if it doesn't exist.
	 */
	public BuilderSwap<T,?> getBuilderSwap(BeanSession session) {
		return builderSwap.get();
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
	public String getBeanDictionaryName() {
		return beanDictionaryName.get();
	}

	/**
	 * For array and {@code Collection} types, returns the class type of the components of the array or
	 * {@code Collection}.
	 *
	 * @return The element class type, or <jk>null</jk> if this class is not an array or Collection.
	 */
	public ClassMeta<?> getElementType() { return elementType.get(); }

	/**
	 * Returns the example of this class.
	 *
	 * @param session
	 * 	The bean session.
	 * 	<br>Required because the example method may take it in as a parameter.
	 * @param jpSession The JSON parser for parsing examples into POJOs.
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	@SuppressWarnings({ "unchecked" })
	public T getExample(BeanSession session, JsonParserSession jpSession) {
		try {
			if (example.isPresent())
				return jpSession.parse(example.get(), this);
			if (exampleMethod.isPresent())
				return (T)exampleMethod.get().invokeLenient(null, session);
			if (exampleField.isPresent())
				return (T)exampleField.get().get(null);

			if (isCollection()) {
				var etExample = getElementType().getExample(session, jpSession);
				if (nn(etExample)) {
					if (canCreateNewInstance()) {
						var c = (Collection<Object>)newInstance();
						c.add(etExample);
						return (T)c;
					}
					return (T)Collections.singleton(etExample);
				}
			} else if (super.isArray()) {
				var etExample = getElementType().getExample(session, jpSession);
				if (nn(etExample)) {
					var o = Array.newInstance(getElementType().inner(), 1);
					Array.set(o, 0, etExample);
					return (T)o;
				}
			} else if (isMap()) {
				var vtExample = getValueType().getExample(session, jpSession);
				var ktExample = getKeyType().getExample(session, jpSession);
				if (nn(ktExample) && nn(vtExample)) {
					if (canCreateNewInstance()) {
						var m = (Map<Object,Object>)newInstance();
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
			t = Mutaters.get(c, inner());
			if (t == null)
				t = Mutaters.NULL;
			fromMutaters.put(c, t);
		}
		return t == Mutaters.NULL ? null : t;
	}

	/**
	 * Returns the no-arg constructor for this class based on the {@link Marshalled#implClass()} value.
	 *
	 * @param conVis The constructor visibility.
	 * @return The no-arg constructor for this class, or <jk>null</jk> if it does not exist.
	 */
	public ConstructorInfo getImplClassConstructor(Visibility conVis) {
		return implClass.map(x -> x.getNoArgConstructor(conVis).orElse(null)).orElse(null);
	}

	/**
	 * Returns the transform for this class for creating instances from an InputStream.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<InputStream,T> getInputStreamMutater() { return getFromMutater(InputStream.class); }

	/**
	 * For {@code Map} types, returns the class type of the keys of the {@code Map}.
	 *
	 * @return The key class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getKeyType() {
		return keyValueTypes.get().keyType();
	}

	/**
	 * Returns the method or field annotated with {@link NameProperty @NameProperty}.
	 *
	 * @return
	 * 	The method or field  annotated with {@link NameProperty @NameProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Property<T,Object> getNameProperty() { return nameProperty.get(); }

	/**
	 * Returns the reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 *
	 * @return The reason why this class is not a bean, or <jk>null</jk> if it is a bean.
	 */
	public synchronized String getNotABeanReason() {
		return beanMeta.get().notABeanReason();
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
			return opt(getElementType().getOptionalDefault());
		return null;
	}

	/**
	 * Returns the method or field annotated with {@link ParentProperty @ParentProperty}.
	 *
	 * @return
	 * 	The method or field annotated with {@link ParentProperty @ParentProperty} or <jk>null</jk> if method does not
	 * 	exist.
	 */
	public Property<T,Object> getParentProperty() { return parentProperty.get(); }

	/**
	 * Returns a lazily-computed, cached property value for this {@link ClassMeta} instance.
	 *
	 * <p>
	 * This method provides a memoization mechanism for expensive computations. The property value is computed
	 * on the first call using the provided function, then cached for subsequent calls with the same property name.
	 *
	 * <p>
	 * The function is only invoked once per property name per {@link ClassMeta} instance. Subsequent calls
	 * with the same name will return the cached value without re-invoking the function.
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * This method is thread-safe. If multiple threads call this method simultaneously with the same property name,
	 * the function may be invoked multiple times, but only one result will be cached and returned.
	 *
	 * <h5 class='section'>Usage:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Compute and cache an expensive property</jc>
	 * 	Optional&lt;String&gt; <jv>computedValue</jv> = classMeta.<jsm>getProperty</jsm>(<js>"expensiveProperty"</js>, cm -&gt; {
	 * 		<jc>// Expensive computation that only runs once</jc>
	 * 		<jk>return</jk> performExpensiveComputation(cm);
	 * 	});
	 *
	 * 	<jc>// Subsequent calls return cached value</jc>
	 * 	Optional&lt;String&gt; <jv>cached</jv> = classMeta.<jsm>getProperty</jsm>(<js>"expensiveProperty"</js>, cm -&gt; {
	 * 		<jc>// This function is NOT called again</jc>
	 * 		<jk>return</jk> performExpensiveComputation(cm);
	 * 	});
	 * </p>
	 *
	 * @param <T2> The type of the property value.
	 * @param name The unique name identifying this property. Used as the cache key.
	 * @param function The function that computes the property value. Receives this {@link ClassMeta} instance as input.
	 * 	Only invoked if the property hasn't been computed yet. Can return <jk>null</jk>.
	 * @return An {@link Optional} containing the property value if the function returned a non-null value,
	 * 	otherwise an empty {@link Optional}. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <T2> Optional<T2> getProperty(String name, Function<ClassMeta<?>,T2> function) {
		var t = properties.get(name);
		if (t == null) {
			t = opt(function.apply(this));
			properties.put(name, t);
		}
		return (Optional<T2>)t;
	}

	/**
	 * Returns the interface proxy invocation handler for this class.
	 *
	 * @return The interface proxy invocation handler, or <jk>null</jk> if it does not exist.
	 */
	public InvocationHandler getProxyInvocationHandler() {
		return beanMeta.get().optBeanMeta().map(x -> x.getBeanProxyInvocationHandler()).orElse(null);
	}

	/**
	 * Returns the transform for this class for creating instances from a Reader.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<Reader,T> getReaderMutater() { return getFromMutater(Reader.class); }

	/**
	 * Returns the serialized (swapped) form of this class if there is an {@link ObjectSwap} associated with it.
	 *
	 * @param session
	 * 	The bean session.
	 * 	<br>Required because the swap used may depend on the media type being serialized or parsed.
	 * @return The serialized class type, or this object if no swap is associated with the class.
	 */
	public ClassMeta<?> getSerializedClassMeta(BeanSession session) {
		var ps = getSwap(session);
		return (ps == null ? this : ps.getSwapClassMeta(session));
	}

	/**
	 * Returns the transform for this class for creating instances from a String.
	 *
	 * @return The transform, or <jk>null</jk> if no such transform exists.
	 */
	public Mutater<String,T> getStringMutater() { return stringMutater; }

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
		var swapsList = swaps.get();
		if (! swapsList.isEmpty()) {
			var matchQuant = 0;
			ObjectSwap<T,?> matchSwap = null;

			for (var swap : swapsList) {
				var q = swap.match(session);
				if (q > matchQuant) {
					matchQuant = q;
					matchSwap = swap;
				}
			}

			if (matchSwap != null)
				return matchSwap;
		}
		return null;
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
			t = Mutaters.get(inner(), c);
			if (t == null)
				t = Mutaters.NULL;
			toMutaters.put(c, t);
		}
		return t == Mutaters.NULL ? null : t;
	}

	/**
	 * For {@code Map} types, returns the class type of the values of the {@code Map}.
	 *
	 * @return The value class type, or <jk>null</jk> if this class is not a Map.
	 */
	public ClassMeta<?> getValueType() {
		return keyValueTypes.get().valueType();
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
	 * Returns <jk>true</jk> if this class can be instantiated from the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be instantiated from the specified type.
	 */
	public boolean hasMutaterFrom(Class<?> c) {
		return nn(getFromMutater(c));
	}

	/**
	 * Returns <jk>true</jk> if this class can be instantiated from the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be instantiated from the specified type.
	 */
	public boolean hasMutaterFrom(ClassMeta<?> c) {
		return nn(getFromMutater(c.inner()));
	}

	/**
	 * Returns <jk>true</jk> if this class can be transformed to the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be transformed to the specified type.
	 */
	public boolean hasMutaterTo(Class<?> c) {
		return nn(getToMutater(c));
	}

	/**
	 * Returns <jk>true</jk> if this class can be transformed to the specified type.
	 *
	 * @param c The class type to convert from.
	 * @return <jk>true</jk> if this class can be transformed to the specified type.
	 */
	public boolean hasMutaterTo(ClassMeta<?> c) {
		return nn(getToMutater(c.inner()));
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
	 * Returns <jk>true</jk> if this class has a transform associated with it that allows it to be created from a String.
	 *
	 * @return <jk>true</jk> if this class has a transform associated with it that allows it to be created from a String.
	 */
	public boolean hasStringMutater() {
		return nn(stringMutater);
	}

	/**
	 * Returns <jk>true</jk> if this metadata represents an array of argument types.
	 *
	 * @return <jk>true</jk> if this metadata represents an array of argument types.
	 */
	public boolean isArgs() { return cat.is(ARGS); }

	/**
	 * Returns <jk>true</jk> if this class is a bean.
	 *
	 * @return <jk>true</jk> if this class is a bean.
	 */
	public boolean isBean() { return nn(getBeanMeta()); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link BeanMap}.
	 */
	public boolean isBeanMap() { return cat.is(BEANMAP); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Boolean}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Boolean}.
	 */
	public boolean isBoolean() { return isAny(boolean.class, Boolean.class); }

	/**
	 * Returns <jk>true</jk> if this class is <code><jk>byte</jk>[]</code>.
	 *
	 * @return <jk>true</jk> if this class is <code><jk>byte</jk>[]</code>.
	 */
	public boolean isByteArray() { return is(byte[].class); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Calendar}.
	 */
	public boolean isCalendar() { return cat.is(CALENDAR); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Character}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Character}.
	 */
	public boolean isChar() { return isAny(char.class, Character.class); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link CharSequence}.
	 */
	public boolean isCharSequence() { return cat.is(CHARSEQ); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection}.
	 */
	public boolean isCollection() { return cat != null && cat.is(COLLECTION); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Collection} or is an array or {@link Optional}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Collection} or is an array or {@link Optional}.
	 */
	public boolean isCollectionOrArrayOrOptional() { return cat.is(ARRAY) || is(Optional.class) || cat.is(COLLECTION); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date}.
	 */
	public boolean isDate() { return cat.is(DATE); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar}.
	 */
	public boolean isDateOrCalendar() { return cat.is(DATE) || cat.is(CALENDAR); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Date} or {@link Calendar} or {@link Temporal}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Date} or {@link Calendar} or {@link Temporal}.
	 */
	public boolean isDateOrCalendarOrTemporal() { return cat.is(DATE) || cat.is(CALENDAR) || cat.is(TEMPORAL); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Float} or {@link Double}.
	 */
	public boolean isDecimal() { return cat.is(DECIMAL); }

	/**
	 * Returns <jk>true</jk> if this class implements {@link Delegate}, meaning it's a representation of some other
	 * object.
	 *
	 * @return <jk>true</jk> if this class implements {@link Delegate}.
	 */
	public boolean isDelegate() { return cat.is(DELEGATE); }

	/**
	 * Returns <jk>true</jk> if this class is either {@link Double} or <jk>double</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Double} or <jk>double</jk>.
	 */
	public boolean isDouble() { return isAny(Double.class, double.class); }

	/**
	 * Returns <jk>true</jk> if this class is either {@link Float} or <jk>float</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Float} or <jk>float</jk>.
	 */
	public boolean isFloat() { return isAny(Float.class, float.class); }

	/**
	 * Returns <jk>true</jk> if this class is an {@link InputStream}.
	 *
	 * @return <jk>true</jk> if this class is an {@link InputStream}.
	 */
	public boolean isInputStream() { return cat.is(INPUTSTREAM); }

	/**
	 * Returns <jk>true</jk> if this class is either {@link Integer} or <jk>int</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Integer} or <jk>int</jk>.
	 */
	public boolean isInteger() { return isAny(Integer.class, int.class); }

	/**
	 * Returns <jk>true</jk> if this class extends from {@link List}.
	 *
	 * @return <jk>true</jk> if this class extends from {@link List}.
	 */
	public boolean isList() { return cat.is(LIST); }

	/**
	 * Returns <jk>true</jk> if this class is either {@link Long} or <jk>long</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Long} or <jk>long</jk>.
	 */
	public boolean isLong() { return isAny(Long.class, long.class); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map}.
	 */
	public boolean isMap() {
		// TODO - Figure out how cat can be null.
		return cat != null && cat.is(MAP);
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Map} or it's a bean.
	 */
	public boolean isMapOrBean() { return cat.is(MAP) || nn(getBeanMeta()); }

	/**
	 * Returns <jk>true</jk> if this class is {@link Method}.
	 *
	 * @return <jk>true</jk> if this class is {@link Method}.
	 */
	public boolean isMethod() { return is(Method.class); }

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
		if (isPrimitive())
			return is(char.class);
		return true;
	}

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Number}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Number}.
	 */
	public boolean isNumber() { return cat.is(NUMBER); }

	/**
	 * Returns <jk>true</jk> if this class is {@link Object}.
	 *
	 * @return <jk>true</jk> if this class is {@link Object}.
	 */
	public boolean isObject() { return is(Object.class); }

	/**
	 * Returns <jk>true</jk> if this class is a subclass of {@link Optional}.
	 *
	 * @return <jk>true</jk> if this class is a subclass of {@link Optional}.
	 */
	public boolean isOptional() { return is(Optional.class); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Reader}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Reader}.
	 */
	public boolean isReader() { return cat.is(READER); }

	/**
	 * Returns <jk>true</jk> if this class extends from {@link Set}.
	 *
	 * @return <jk>true</jk> if this class extends from {@link Set}.
	 */
	public boolean isSet() { return cat.is(SET); }

	/**
	 * Returns <jk>true</jk> if this class is either {@link Short} or <jk>short</jk>.
	 *
	 * @return <jk>true</jk> if this class is either {@link Short} or <jk>short</jk>.
	 */
	public boolean isShort() { return isAny(Short.class, short.class); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link String}.
	 *
	 * @return <jk>true</jk> if this class is a {@link String}.
	 */
	public boolean isString() { return is(String.class); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link Temporal}.
	 *
	 * @return <jk>true</jk> if this class is a {@link Temporal}.
	 */
	public boolean isTemporal() { return cat.is(TEMPORAL); }

	/**
	 * Returns <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 *
	 * @return <jk>true</jk> if this class is a {@link URI} or {@link URL}.
	 */
	public boolean isUri() { return cat != null && cat.is(URI); }

	/**
	 * Transforms the specified object into an instance of this class.
	 *
	 * @param o The object to transform.
	 * @return The transformed object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
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
	@SuppressWarnings({ "unchecked" })
	public <O> O mutateTo(Object o, Class<O> c) {
		Mutater<Object,O> t = (Mutater<Object,O>)getToMutater(c);
		return t == null ? null : t.mutate(o);
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
		return mutateTo(o, c.inner());
	}

	/**
	 * Create a new instance of the main class of this declared type.
	 *
	 * @return A new instance of the object, or <jk>null</jk> if there is no no-arg constructor on the object.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T newInstance() throws ExecutableException {
		if (super.isArray())
			return (T)Array.newInstance(inner().getComponentType(), 0);
		if (noArgConstructor.isPresent())
			return noArgConstructor.get().newInstance();
		var h = getProxyInvocationHandler();
		if (nn(h))
			return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), a(inner(), java.io.Serializable.class), h);
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
		if (isMemberClass() && isNotStatic() && noArgConstructor.isPresent())
			return noArgConstructor.get().<T>newInstance(outer);
		return newInstance();
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
			var t = (T)enumValues.get().getKey(arg);
			if (t == null && ! beanContext.isIgnoreUnknownEnumValues())
				throw new ExecutableException("Could not resolve enum value ''{0}'' on class ''{1}''", arg, inner().getName());
			return t;
		}

		if (fromStringMethod.isPresent())
			return (T)fromStringMethod.get().invoke(null, arg);

		if (stringConstructor.isPresent()) {
			if (isMemberClass() && isNotStatic())
				return stringConstructor.get().<T>newInstance(outer, arg);
			return stringConstructor.get().<T>newInstance(arg);
		}
		throw new ExecutableException("No string constructor or valueOf(String) method found for class '" + inner().getName() + "'");
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
		return (isPrimitive() && cat.same(cm.cat));
	}

	@Override /* Overridden from Object */
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

	@SuppressWarnings("unchecked")
	private ObjectSwap<T,?> createSwap(Swap s) {
		var c = s.value();
		if (ClassUtils.isVoid(c))
			c = s.impl();
		var ci = info(c);

		if (ci.isChildOf(ObjectSwap.class)) {
			var ps = BeanCreator.of(ObjectSwap.class).type(c).run();
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

	private String findBeanDictionaryName() {
		if (beanContext == null)
			return null;

		var d = beanMeta.get().optBeanMeta().map(x -> x.getDictionaryName()).orElse(null);
		if (nn(d))
			return d;

		// Note that @Bean(typeName) can be defined on non-bean types, so
		// we have to check again.
		return beanContext.getAnnotationProvider().find(Bean.class, this)
			.stream()
			.map(AnnotationInfo::inner)
			.filter(x -> ! x.typeName().isEmpty())
			.map(x -> x.typeName())
			.findFirst()
			.orElse(null);
	}

	private BeanMeta.BeanMetaValue<T> findBeanMeta() {
		if (! cat.isUnknown())
			return new BeanMeta.BeanMetaValue<>(null, "Known non-bean type");
		return BeanMeta.create(this, null, implClass.map(x -> x.getPublicConstructor(x2 -> x2.hasNumParameters(0)).orElse(null)).orElse(null));
	}

	private KeyValueTypes findKeyValueTypes() {
		if (cat.is(MAP) && ! cat.is(BEANMAP)) {
			// If this is a MAP, see if it's parameterized (e.g. AddressBook extends HashMap<String,Person>)
			var parameters = beanContext.findParameters(inner(), inner());
			if (nn(parameters) && parameters.length == 2) {
				return new KeyValueTypes(parameters[0], parameters[1]);
			}
			return new KeyValueTypes(beanContext.getClassMeta(Object.class), beanContext.getClassMeta(Object.class));
		}
		return new KeyValueTypes(null, null);
	}

	private ClassMeta<?> findElementType() {
		if (beanContext == null)
			return null;
		if (cat.is(ARRAY)) {
			return beanContext.getClassMeta(inner().getComponentType(), false);
		} else if (cat.is(COLLECTION) || is(Optional.class)) {
			// If this is a COLLECTION, see if it's parameterized (e.g. AddressBook extends LinkedList<Person>)
			var parameters = beanContext.findParameters(inner(), inner());
			if (nn(parameters) && parameters.length == 1) {
				return parameters[0];
			}
			return beanContext.getClassMeta(Object.class);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private BuilderSwap<T,?> findBuilderSwap() {
		var bc = beanContext;
		if (bc == null)
			return null;
		return (BuilderSwap<T,?>)BuilderSwap.findSwapFromObjectClass(bc, inner(), bc.getBeanConstructorVisibility(), bc.getBeanMethodVisibility());
	}

	@SuppressWarnings("unchecked")
	private List<ObjectSwap<T,?>> findSwaps() {
		if (beanContext == null)
			return l();

		var list = new ArrayList<ObjectSwap<T,?>>();
		var swapArray = beanContext.getSwaps();
		if (swapArray != null && swapArray.length > 0) {
			var innerClass = inner();
			for (var f : swapArray)
				if (f.getNormalClass().isParentOf(innerClass))
					list.add((ObjectSwap<T,?>)f);
		}

		var ap = beanContext.getAnnotationProvider();
		ap.find(Swap.class, this).stream().map(AnnotationInfo::inner).forEach(x -> list.add(createSwap(x)));
		var ds = DefaultSwaps.find(this);
		if (ds == null)
			ds = AutoObjectSwap.find(beanContext, this);
		if (ds == null)
			ds = AutoNumberSwap.find(beanContext, this);
		if (ds == null)
			ds = AutoMapSwap.find(beanContext, this);
		if (ds == null)
			ds = AutoListSwap.find(beanContext, this);

		if (nn(ds))
			list.add((ObjectSwap<T,?>)ds);

		return u(list);
	}

	private List<ObjectSwap<?,?>> findChildSwaps() {
		if (beanContext == null)
			return l();
		var swapArray = beanContext.getSwaps();
		if (swapArray == null || swapArray.length == 0)
			return l();
		var list = new ArrayList<ObjectSwap<?,?>>();
		var innerClass = inner();
		for (var f : swapArray)
			if (f.getNormalClass().isChildOf(innerClass))
				list.add(f);
		return u(list);
	}

	private BidiMap<Object,String> findEnumValues() {
		if (! isEnum())
			return null;

		var bc = beanContext;
		var useEnumNames = nn(bc) && bc.isUseEnumNames();

		var m = BidiMap.<Object,String>create().unmodifiable();
		var c = inner().asSubclass(Enum.class);
		stream(c.getEnumConstants()).forEach(x -> m.add(x, useEnumNames ? x.name() : x.toString()));
		return m.build();
	}

	@SuppressWarnings("unchecked")
	private String findExample() {

		var example = beanMeta.get().optBeanMeta().map(x -> x.getBeanFilter()).map(x -> x.getExample()).orElse(null);

		if (example == null)
			example = marshalledFilter.map(x -> x.getExample()).orElse(null);

		if (example == null && nn(beanContext))
			example = beanContext.getAnnotationProvider().find(Example.class, this).stream().map(x -> x.inner().value()).filter(Utils::isNotEmpty).findFirst().orElse(null);

		if (example == null) {
			if (isAny(boolean.class, Boolean.class)) {
				example = "true";
			} else if (isAny(char.class, Character.class)) {
				example = "a";
			} else if (cat.is(CHARSEQ)) {
				example = "foo";
			} else if (cat.is(ENUM)) {
				Iterator<? extends Enum<?>> i = EnumSet.allOf(inner().asSubclass(Enum.class)).iterator();
				example = i.hasNext() ? (beanContext.isUseEnumNames() ? i.next().name() : i.next().toString()) : null;
			} else if (isAny(float.class, Float.class, double.class, Double.class)) {
				example = "1.0";
			} else if (isAny(short.class, Short.class, int.class, Integer.class, long.class, Long.class)) {
				example = "1";
			}
		}

		return example;
	}

	private FieldInfo findExampleField() {
		var ap = beanContext.getAnnotationProvider();

		return getDeclaredFields()
			.stream()
			.filter(x -> x.isStatic() && isParentOf(x.getFieldType()) && ap.has(Example.class, x))
			.map(x -> x.accessible())
			.findFirst()
			.orElse(null);
	}

	private MethodInfo findExampleMethod() {
		// @formatter:off
		var ap = beanContext.getAnnotationProvider();

		// Option 1:  Public example() or @Example method.
		var m = getPublicMethod(
			x -> x.isStatic() && x.isNotDeprecated() && (x.hasName("example") || ap.has(Example.class, x)) && x.hasParameterTypesLenient(BeanSession.class)
		);
		if (m.isPresent()) return m.get();

		// Option 2:  Non-public @Example method.
		return getDeclaredMethods()
			.stream()
			.flatMap(x -> x.getMatchingMethods().stream())
			.filter(x -> x.isStatic() && ap.has(Example.class, x))
			.map(x -> x.accessible())
			.findFirst()
			.orElse(null);
		// @formatter:on
	}

	private MethodInfo findFromStringMethod() {
		// Find static fromString(String) or equivalent method.
		// fromString() must be checked before valueOf() so that Enum classes can create their own
		//		specialized fromString() methods to override the behavior of Enum.valueOf(String).
		// valueOf() is used by enums.
		// parse() is used by the java logging Level class.
		// forName() is used by Class and Charset
		// @formatter:off
		var names = a("fromString", "fromValue", "valueOf", "parse", "parseString", "forName", "forString");
		return getPublicMethod(
			x -> x.isStatic() && x.isNotDeprecated() && x.hasReturnType(this) && x.hasParameterTypes(String.class) && contains(x.getName(), names)
		).orElse(null);
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private ClassInfoTyped<? extends T> findImplClass() {

		if (is(Object.class))
			return null;

		var v = beanContext.getAnnotationProvider().find(Bean.class, this).stream().map(x -> x.inner()).filter(x -> ne(x.implClass(), void.class)).map(x -> ClassInfo.of(x)).findFirst().orElse(null);

		if (v == null)
			v = marshalledFilter.map(x -> x.getImplClass()).map(ReflectionUtils::info).orElse(null);

		return (ClassInfoTyped<? extends T>)v;
	}

	private MarshalledFilter findMarshalledFilter() {
		var ap = beanContext.getAnnotationProvider();
		var l = ap.find(Marshalled.class, this);
		if (l.isEmpty())
			return null;
		return MarshalledFilter.create(inner()).applyAnnotations(reverse(l.stream().map(AnnotationInfo::inner).toList())).build();
	}

	private Property<T,Object> findNameProperty() {
		var ap = beanContext.getAnnotationProvider();

		var s = getAllFields()
			.stream()
			.filter(x -> ap.has(NameProperty.class, x))
			.map(x -> x.accessible())
			.map(x -> Property.<T,Object>create().field(x).build())
			.findFirst();

		if (s.isPresent()) return s.get();

		var builder = Property.<T,Object>create();

		// Look for setter method (1 parameter) with @NameProperty
		var setterMethod = getAllMethods()
			.stream()
			.filter(x -> ap.has(NameProperty.class, x) && x.hasNumParameters(1))
			.findFirst();

		if (setterMethod.isPresent()) {
			builder.setter(setterMethod.get().accessible());

			// Try to find a corresponding getter method (even if not annotated)
			// If setter is "setName", look for "getName" or "isName"
			var setterName = setterMethod.get().getSimpleName();
			if (setterName.startsWith("set") && setterName.length() > 3) {
				var propertyName = setterName.substring(3);
				var getterName1 = "get" + propertyName;
				var getterName2 = "is" + propertyName;

				var getter = getAllMethods()
					.stream()
					.filter(x -> !x.isStatic() && x.hasNumParameters(0) &&
						(x.hasName(getterName1) || x.hasName(getterName2)) &&
						!x.getReturnType().is(Void.TYPE))
					.findFirst();

				if (getter.isPresent()) {
					builder.getter(getter.get().accessible());
				} else {
					// Try to find a field with the property name (lowercase first letter)
					var fieldName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
					var field = getAllFields()
						.stream()
						.filter(x -> !x.isStatic() && x.hasName(fieldName))
						.findFirst();

					if (field.isPresent()) {
						var f = field.get().accessible();
						builder.getter(obj -> f.get(obj));
					}
				}
			}
		}

		// Look for getter method (0 parameters, non-void return) with @NameProperty
		var getterMethod = getAllMethods()
			.stream()
			.filter(x -> ap.has(NameProperty.class, x) && x.hasNumParameters(0) && !x.getReturnType().is(Void.TYPE))
			.findFirst();

		if (getterMethod.isPresent()) {
			builder.getter(getterMethod.get().accessible());
		}

		// Return null if neither setter nor getter was found
		if (setterMethod.isEmpty() && getterMethod.isEmpty())
			return null;

		return builder.build();
	}

	private ConstructorInfo findNoArgConstructor() {

		if (is(Object.class))
			return null;

		if (implClass.isPresent())
			return implClass.get().getPublicConstructor(x -> x.hasNumParameters(0)).orElse(null);

		if (isAbstract())
			return null;

		var numParams = isMemberClass() && isNotStatic() ? 1 : 0;
		return getPublicConstructors()
			.stream()
			.filter(x -> x.isPublic() && x.isNotDeprecated() && x.hasNumParameters(numParams))
			.findFirst()
			.orElse(null);
	}

	private Property<T,Object> findParentProperty() {
		var ap = beanContext.getAnnotationProvider();

		var s = getAllFields()
			.stream()
			.filter(x -> ap.has(ParentProperty.class, x))
			.map(x -> x.accessible())
			.map(x -> Property.<T,Object>create().field(x).build())
			.findFirst();

		if (s.isPresent()) return s.get();

		var builder = Property.<T,Object>create();

		// Look for setter method (1 parameter) with @ParentProperty
		var setterMethod = getAllMethods()
			.stream()
			.filter(x -> ap.has(ParentProperty.class, x) && x.hasNumParameters(1))
			.findFirst();

		if (setterMethod.isPresent()) {
			builder.setter(setterMethod.get().accessible());

			// Try to find a corresponding getter method (even if not annotated)
			// If setter is "setParent", look for "getParent" or "isParent"
			var setterName = setterMethod.get().getSimpleName();
			if (setterName.startsWith("set") && setterName.length() > 3) {
				var propertyName = setterName.substring(3);
				var getterName1 = "get" + propertyName;
				var getterName2 = "is" + propertyName;

				var getter = getAllMethods()
					.stream()
					.filter(x -> !x.isStatic() && x.hasNumParameters(0) &&
						(x.hasName(getterName1) || x.hasName(getterName2)) &&
						!x.getReturnType().is(Void.TYPE))
					.findFirst();

				if (getter.isPresent()) {
					builder.getter(getter.get().accessible());
				} else {
					// Try to find a field with the property name (lowercase first letter)
					var fieldName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
					var field = getAllFields()
						.stream()
						.filter(x -> !x.isStatic() && x.hasName(fieldName))
						.findFirst();

					if (field.isPresent()) {
						var f = field.get().accessible();
						builder.getter(obj -> f.get(obj));
					}
				}
			}
		}

		// Look for getter method (0 parameters, non-void return) with @ParentProperty
		var getterMethod = getAllMethods()
			.stream()
			.filter(x -> ap.has(ParentProperty.class, x) && x.hasNumParameters(0) && !x.getReturnType().is(Void.TYPE))
			.findFirst();

		if (getterMethod.isPresent()) {
			builder.getter(getterMethod.get().accessible());
		}

		// Return null if neither setter nor getter was found
		if (setterMethod.isEmpty() && getterMethod.isEmpty())
			return null;

		return builder.build();
	}

	private ConstructorInfo findStringConstructor() {

		if (is(Object.class) || isAbstract())
			return null;

		if (implClass.isPresent())
			return implClass.get().getPublicConstructor(x -> x.hasParameterTypes(String.class)).orElse(null);

		if (isAbstract())
			return null;

		var numParams = isMemberClass() && isNotStatic() ? 2 : 1;
		return getPublicConstructors()
			.stream()
			.filter(x -> x.isPublic() && x.isNotDeprecated() && x.hasNumParameters(numParams))
			.filter(x -> x.getParameter(numParams == 2 ? 1 : 0).isType(String.class))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns the {@link ObjectSwap} where the specified class is the same/subclass of the normal class of one of the
	 * child POJO swaps associated with this class.
	 *
	 * @param normalClass The normal class being resolved.
	 * @return The resolved {@link ObjectSwap} or <jk>null</jk> if none were found.
	 */
	protected ObjectSwap<?,?> getChildObjectSwapForSwap(Class<?> normalClass) {
		return childSwapMap.get(normalClass);
	}

	/**
	 * Returns the {@link ObjectSwap} where the specified class is the same/subclass of the swap class of one of the child
	 * POJO swaps associated with this class.
	 *
	 * @param swapClass The swap class being resolved.
	 * @return The resolved {@link ObjectSwap} or <jk>null</jk> if none were found.
	 */
	protected ObjectSwap<?,?> getChildObjectSwapForUnswap(Class<?> swapClass) {
		return childUnswapMap.get(swapClass);
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
		return ! childSwaps.get().isEmpty();
	}

	/**
	 * Appends this object as a readable string to the specified string builder.
	 *
	 * @param sb The string builder to append this object to.
	 * @param simple Print simple class names only (no package).
	 * @return The passed-in string builder.
	 */
	protected StringBuilder toString(StringBuilder sb, boolean simple) {
		var n = inner().getName();
		if (simple) {
			var i = n.lastIndexOf('.');
			n = n.substring(i == -1 ? 0 : i + 1).replace('$', '.');
		}
		if (cat.is(ARRAY))
			return elementType.get().toString(sb, simple).append('[').append(']');
		if (cat.is(BEANMAP))
			return sb.append(cn(BeanMap.class)).append('<').append(n).append('>');
		if (cat.is(MAP)) {
			var kvTypes = keyValueTypes.get();
			var kt = kvTypes.optKeyType();
			var vt = kvTypes.optValueType();
			if (kt.isPresent() && vt.isPresent() && kt.get().isObject() && vt.get().isObject())
				return sb.append(n);
			return sb.append(n).append('<').append(kt.map(x -> x.toString(simple)).orElse("?")).append(',').append(vt.map(x -> x.toString(simple)).orElse("?")).append('>');
		}
		if (cat.is(COLLECTION) || is(Optional.class)) {
			var et = elementType.get();
			return sb.append(n).append(et != null && et.isObject() ? "" : "<" + (et == null ? "?" : et.toString(simple)) + ">");
		}
		return sb.append(n);
	}
}