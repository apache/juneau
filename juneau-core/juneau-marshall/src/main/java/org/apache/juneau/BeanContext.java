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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static java.util.Optional.*;
import static java.util.Arrays.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Core class of the Juneau architecture.
 * {@review}
 *
 * <p class='w800'>
 * This class servers multiple purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Provides the ability to wrap beans inside {@link Map} interfaces.
 * 	<li>
 * 		Serves as a repository for metadata on POJOs, such as associated {@link Bean @Bean} annotations,
 * 		{@link PropertyNamer PropertyNamers}, etc...  which are used to tailor how POJOs are serialized and parsed.
 * </ul>
 *
 * <p class='w800'>
 * All serializers and parsers extend from this context so that they can handle POJOs using a common framework.
 *
 * <h5 class='topic'>Bean Contexts</h5>
 *
 * <p class='w800'>
 * Bean contexts are created through the {@link BeanContext#create() BeanContext.create()} and {@link BeanContextBuilder#build()} methods.
 * <br>These context objects are read-only, reusable, and thread-safe.
 *
 * <p class='w800'>
 * Each bean context maintains a cache of {@link ClassMeta} objects that describe information about classes encountered.
 * These <c>ClassMeta</c> objects are time-consuming to construct.
 * Therefore, instances of {@link BeanContext} that share the same <js>"BeanContext.*"</js> property values share
 * the same cache.  This allows for efficient reuse of <c>ClassMeta</c> objects so that the information about
 * classes only needs to be calculated once.
 * Because of this, many of the properties defined on the {@link BeanContext} class cannot be overridden on the session.
 *
 * <h5 class='topic'>Bean Sessions</h5>
 *
 * <p class='w800'>
 * Whereas <c>BeanContext</c> objects are permanent, unchangeable, cached, and thread-safe,
 * {@link BeanSession} objects are ephemeral and not thread-safe.
 * They are meant to be used as quickly-constructed scratchpads for creating bean maps.
 * {@link BeanMap} objects can only be created through the session.
 *
 * <h5 class='topic'>BeanContext configuration properties</h5>
 *
 * <p class='w800'>
 * <c>BeanContexts</c> have several configuration properties that can be used to tweak behavior on how beans are
 * handled.  These are denoted as the static <jsf>BEAN_*</jsf> fields on this class.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link BeanContextBuilder#beansRequireDefaultConstructor()}) are used to differentiate between bean
 * and non-bean classes.
 * Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to
 * plain strings using the {@link Object#toString()} method.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link BeanContextBuilder#beanFieldVisibility(Visibility)}) are used to determine what kinds of properties are
 * detected on beans.
 *
 * <p class='w800'>
 * Some settings (e.g. {@link BeanContextBuilder#beanMapPutReturnsOldValue()}) change the runtime behavior of bean maps.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode w800'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext <jv>beanContext</jv> = BeanContext
 * 		.<jsm>create</jsm>()
 * 		.beansRequireDefaultConstructor()
 * 		.notBeanClasses(Foo.<jk>class</jk>)
 * 		.build();
 * </p>
 *
 * <h5 class='topic'>Bean Maps</h5>
 *
 * <p class='w800'>
 * {@link BeanMap BeanMaps} are wrappers around Java beans that allow properties to be retrieved and
 * set using the common {@link Map#put(Object,Object)} and {@link Map#get(Object)} methods.
 *
 * <p class='w800'>
 * Bean maps are created in two ways...
 * <ol>
 * 	<li>{@link BeanSession#toBeanMap(Object) BeanSession.toBeanMap()} - Wraps an existing bean inside a {@code Map}
 * 		wrapper.
 * 	<li>{@link BeanSession#newBeanMap(Class) BeanSession.newBeanMap()} - Create a new bean instance wrapped in a
 * 		{@code Map} wrapper.
 * </ol>
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bcode w800'>
 * 	<jc>// A sample bean class</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String <jv>name</jv>);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> <jv>age</jv>);
 * 	}
 *
 * 	<jc>// Create a new bean session</jc>
 * 	BeanSession <jv>session</jv> = BeanContext.<jsf>DEFAULT</jsf>.createSession();
 *
 * 	<jc>// Wrap an existing bean in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; <jv>m1</jv> = <jv>session</jv>.toBeanMap(<jk>new</jk> Person());
 * 	<jv>m1</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>m1</jv>.put(<js>"age"</js>, 45);
 *
 * 	<jc>// Create a new bean instance wrapped in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; <jv>m2</jv> = <jv>session</jv>.newBeanMap(Person.<jk>class</jk>);
 * 	<jv>m2</jv>.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	<jv>m2</jv>.put(<js>"age"</js>, 45);
 * 	Person <jv>p</jv> = <jv>m2</jv>.getBean();  <jc>// Get the bean instance that was created.</jc>
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc BeanContext}
 * </ul>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BeanContext extends Context {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/*
	 * The default package pattern exclusion list.
	 * Any beans in packages in this list will not be considered beans.
	 */
	private static final String[] DEFAULT_NOTBEAN_PACKAGES = {
		"java.lang",
		"java.lang.annotation",
		"java.lang.ref",
		"java.lang.reflect",
		"java.io",
		"java.net",
		"java.nio.*",
		"java.util.*"
	};

	/*
	 * The default bean class exclusion list.
	 * Anything in this list will not be considered beans.
	 */
	private static final Class<?>[] DEFAULT_NOTBEAN_CLASSES = {
		Map.class,
		Collection.class,
		Reader.class,
		Writer.class,
		InputStream.class,
		OutputStream.class,
		Throwable.class
	};


	/** Default config.  All default settings. */
	public static final BeanContext DEFAULT = BeanContext.create().build();

	/** Default config.  All default settings except sort bean properties. */
	public static final BeanContext DEFAULT_SORTED = BeanContext.create().sortProperties().build();

	/** Default reusable unmodifiable session.  Can be used to avoid overhead of creating a session (for creating BeanMaps for example).*/
	public  static final BeanSession DEFAULT_SESSION = new BeanSession(DEFAULT, DEFAULT.createDefaultBeanSessionArgs().unmodifiable());

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignoreMissingSetters,
		ignoreTransientFields,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector,
		useEnumNames,
		sortProperties,
		findFluentSetters;
	final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;
	final String typePropertyName;
	final Locale locale;
	final TimeZone timeZone;
	final MediaType mediaType;
	final Class<? extends PropertyNamer> propertyNamer;
	final List<Class<?>> beanDictionary, swaps, notBeanClasses;
	final List<String> notBeanPackages;

	final Map<Class,ClassMeta> cmCache;

	private final String[] notBeanPackageNames, notBeanPackagePrefixes;
	private final BeanRegistry beanRegistry;
	private final PropertyNamer propertyNamerBean;
	private final PojoSwap[] swapArray;
	private final Class<?>[] notBeanClassesArray;
	private final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	private final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	private final ClassMeta<Class> cmClass;  // Reusable ClassMeta that represents general Classes.

	private volatile WriterSerializer beanToStringSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public BeanContext(BeanContextBuilder builder) {
		super(builder);

		beanConstructorVisibility = builder.beanConstructorVisibility;
		beanClassVisibility = builder.beanClassVisibility;
		beanMethodVisibility = builder.beanMethodVisibility;
		beanFieldVisibility = builder.beanFieldVisibility;
		beansRequireDefaultConstructor = builder.beansRequireDefaultConstructor;
		beansRequireSerializable = builder.beansRequireSerializable;
		beansRequireSettersForGetters = builder.beansRequireSettersForGetters;
		beansRequireSomeProperties = ! builder.disableBeansRequireSomeProperties;
		beanMapPutReturnsOldValue = builder.beanMapPutReturnsOldValue;
		useEnumNames = builder.useEnumNames;
		useInterfaceProxies = ! builder.disableInterfaceProxies;
		ignoreUnknownBeanProperties = builder.ignoreUnknownBeanProperties;
		ignoreUnknownNullBeanProperties = ! builder.disableIgnoreUnknownNullBeanProperties;
		ignoreMissingSetters = ! builder.disableIgnoreMissingSetters;
		ignoreTransientFields = ! builder.disableIgnoreTransientFields;
		ignoreInvocationExceptionsOnGetters = builder.ignoreInvocationExceptionsOnGetters;
		ignoreInvocationExceptionsOnSetters = builder.ignoreInvocationExceptionsOnSetters;
		useJavaBeanIntrospector = builder.useJavaBeanIntrospector;
		sortProperties = builder.sortProperties;
		findFluentSetters = builder.findFluentSetters;
		typePropertyName = ofNullable(builder.typePropertyName).orElse("_type");
		locale = ofNullable(builder.locale).orElseGet(()->Locale.getDefault());
		timeZone = builder.timeZone;
		mediaType = builder.mediaType;
		beanDictionary = ofNullable(builder.beanDictionary).map(Collections::unmodifiableList).orElse(emptyList());
		swaps = ofNullable(builder.swaps).map(Collections::unmodifiableList).orElse(emptyList());
		notBeanClasses = ofNullable(builder.notBeanClasses).map(ArrayList::new).map(Collections::unmodifiableList).orElse(emptyList());
		notBeanPackages = ofNullable(builder.notBeanPackages).map(ArrayList::new).map(Collections::unmodifiableList).orElse(emptyList());
		propertyNamer = builder.propertyNamer != null ? builder.propertyNamer : BasicPropertyNamer.class;

		notBeanClassesArray = notBeanClasses.isEmpty() ? DEFAULT_NOTBEAN_CLASSES : Stream.of(notBeanClasses, asList(DEFAULT_NOTBEAN_CLASSES)).flatMap(Collection::stream).toArray(Class[]::new);

		String[] _notBeanPackages = notBeanPackages.isEmpty() ? DEFAULT_NOTBEAN_PACKAGES : Stream.of(notBeanPackages, asList(DEFAULT_NOTBEAN_PACKAGES)).flatMap(Collection::stream).toArray(String[]::new);
		notBeanPackageNames = Stream.of(_notBeanPackages).filter(x -> ! x.endsWith(".*")).toArray(String[]::new);
		notBeanPackagePrefixes = Stream.of(_notBeanPackages).filter(x -> x.endsWith(".*")).map(x -> x.substring(0, x.length()-2)).toArray(String[]::new);

		try {
			propertyNamerBean = propertyNamer.newInstance();
		} catch (Exception e) {
			throw runtimeException(e);
		}

		LinkedList<PojoSwap<?,?>> _swaps = new LinkedList<>();
		for (Object o : ofNullable(swaps).orElse(emptyList())) {
			ClassInfo ci = ClassInfo.of((Class<?>)o);
			if (ci.isChildOf(PojoSwap.class))
				_swaps.add(castOrCreate(PojoSwap.class, ci.inner()));
			else if (ci.isChildOf(Surrogate.class))
				_swaps.addAll(SurrogateSwap.findPojoSwaps(ci.inner(), this));
			else
				throw runtimeException("Invalid class {0} specified in BeanContext.swaps property.  Must be a subclass of PojoSwap or Surrogate.", ci.inner());
		}
		swapArray = _swaps.toArray(new PojoSwap[_swaps.size()]);

		cmCache = new ConcurrentHashMap<>();
		cmCache.put(String.class, new ClassMeta(String.class, this, findPojoSwaps(String.class), findChildPojoSwaps(String.class)));
		cmCache.put(Object.class, new ClassMeta(Object.class, this, findPojoSwaps(Object.class), findChildPojoSwaps(Object.class)));
		cmString = cmCache.get(String.class);
		cmObject = cmCache.get(Object.class);
		cmClass = cmCache.get(Class.class);

		beanRegistry = new BeanRegistry(this, null);
	}

	@Override /* Context */
	public BeanContextBuilder copy() {
		return new BeanContextBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link BeanContextBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> BeanContextBuilder()</code>.
	 *
	 * @return A new {@link JsonSerializerBuilder} object.
	 */
	public static BeanContextBuilder create() {
		return new BeanContextBuilder();
	}

	/**
	 * Create a new bean session based on the properties defined on this context.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @return A new session object.
	 */
	@Override /* Context */
	public BeanSession createSession() {
		return createBeanSession(defaultArgs());
	}

	/**
	 * Create a new bean session based on the properties defined on this context combined with the specified
	 * runtime args.
	 *
	 * <p>
	 * Use this method for creating sessions if you don't need to override any
	 * properties or locale/timezone currently set on this context.
	 *
	 * @param args
	 * 	The session arguments.
	 * @return A new session object.
	 */
	public BeanSession createSession(BeanSessionArgs args) {
		return createBeanSession(args);
	}

	@Override /* Context */
	public final ContextSession createSession(Context.Args args) {
		throw new NoSuchMethodError();
	}

	/**
	 * Same as {@link #createSession(BeanSessionArgs)} except always returns a {@link BeanSession} object unlike {@link #createSession(BeanSessionArgs)}
	 * which is meant to be overridden by subclasses.
	 *
	 * @param args The session arguments.
	 * @return A new session object.
	 */
	public final BeanSession createBeanSession(BeanSessionArgs args) {
		return new BeanSession(this, args);
	}

	/**
	 * Same as {@link #createSession()} except always returns a {@link BeanSession} object unlike {@link #createSession()}
	 * which is meant to be overridden by subclasses.
	 *
	 * @return A new session object.
	 */
 	public final BeanSession createBeanSession() {
		return new BeanSession(this, createDefaultBeanSessionArgs());
	}

 	@Override /* Context */
	public BeanSessionArgs defaultArgs() {
 		return createDefaultBeanSessionArgs();
	}

	/**
	 * Same as {@link #defaultArgs()} except always returns a {@link BeanSessionArgs} unlike
	 * {@link #createDefaultBeanSessionArgs()} which is meant to be overridden by subclasses.
	 *
	 * @return A new session arguments object.
	 */
	public final BeanSessionArgs createDefaultBeanSessionArgs() {
		return new BeanSessionArgs();
	}

	/**
	 * Returns <jk>true</jk> if the specified bean context shares the same cache as this bean context.
	 *
	 * <p>
	 * Useful for testing purposes.
	 *
	 * @param bc The bean context to compare to.
	 * @return <jk>true</jk> if the bean contexts have equivalent settings and thus share caches.
	 */
	public final boolean hasSameCache(BeanContext bc) {
		return bc.cmCache == this.cmCache;
	}

	/**
	 * Determines whether the specified class is ignored as a bean class based on the various exclusion parameters
	 * specified on this context class.
	 *
	 * @param c The class type being tested.
	 * @return <jk>true</jk> if the specified class matches any of the exclusion parameters.
	 */
	protected final boolean isNotABean(Class<?> c) {
		if (c.isArray() || c.isPrimitive() || c.isEnum() || c.isAnnotation())
			return true;
		Package p = c.getPackage();
		if (p != null) {
			for (String p2 : notBeanPackageNames)
				if (p.getName().equals(p2))
					return true;
			for (String p2 : notBeanPackagePrefixes)
				if (p.getName().startsWith(p2))
					return true;
		}
		ClassInfo ci = ClassInfo.of(c);
		for (Class exclude : notBeanClassesArray)
			if (ci.isChildOf(exclude))
				return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a bean.
	 *
	 * @param o The object to test.
	 * @return <jk>true</jk> if the specified object is a bean.  <jk>false</jk> if the bean is <jk>null</jk>.
	 */
	public boolean isBean(Object o) {
		if (o == null)
			return false;
		return getClassMetaForObject(o).isBean();
	}

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return
	 * 	The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class is not a bean per the settings on
	 * 	this context.
	 */
	public final <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param type The class to resolve.
	 * @return
	 * 	If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> type) {
		return getClassMeta(type, true);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param type The class to resolve.
	 * @param waitForInit
	 * 	When enabled, wait for the ClassMeta constructor to finish before returning.
	 * @return
	 * 	If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	final <T> ClassMeta<T> getClassMeta(Class<T> type, boolean waitForInit) {

		// This can happen if we have transforms defined against String or Object.
		if (cmCache == null)
			return null;

		ClassMeta<T> cm = cmCache.get(type);
		if (cm == null) {

			synchronized (this) {
				// Make sure someone didn't already set it while this thread was blocked.
				cm = cmCache.get(type);
				if (cm == null)
					cm = new ClassMeta<>(type, this, findPojoSwaps(type), findChildPojoSwaps(type));
			}
		}
		if (waitForInit)
			cm.waitForInit();
		return cm;
	}

	/**
	 * Used to resolve <c>ClassMetas</c> of type <c>Collection</c> and <c>Map</c> that have
	 * <c>ClassMeta</c> values that themselves could be collections or maps.
	 *
	 * <p>
	 * <c>Collection</c> meta objects are assumed to be followed by zero or one meta objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> meta objects are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><code>getClassMeta(String.<jk>class</jk>);</code> - A normal type.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>);</code> - A list containing objects.
	 * 	<li><code>getClassMeta(List.<jk>class</jk>, String.<jk>class</jk>);</code> - A list containing strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> - A linked-list containing
	 * 		strings.
	 * 	<li><code>getClassMeta(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);</code> -
	 * 		A linked-list containing linked-lists of strings.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>);</code> - A map containing object keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);</code> - A map
	 * 		containing string keys/values.
	 * 	<li><code>getClassMeta(Map.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);</code> -
	 * 		A map containing string keys and values of lists containing beans.
	 * </ul>
	 *
	 * @param type
	 * 	The class to resolve.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The resolved class meta.
	 */
	public final <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		if (type == null)
			return null;
		ClassMeta<T> cm = type instanceof Class ? getClassMeta((Class)type) : resolveClassMeta(type, null);
		if (args.length == 0)
			return cm;
		ClassMeta<?>[] cma = new ClassMeta[args.length+1];
		cma[0] = cm;
		for (int i = 0; i < Array.getLength(args); i++) {
			Type arg = (Type)Array.get(args, i);
			cma[i+1] = arg instanceof Class ? getClassMeta((Class)arg) : resolveClassMeta(arg, null);
		}
		return (ClassMeta<T>) getTypedClassMeta(cma, 0);
	}

	/*
	 * Resolves the 'genericized' class meta at the specified position in the ClassMeta array.
	 */
	private ClassMeta<?> getTypedClassMeta(ClassMeta<?>[] c, int pos) {
		ClassMeta<?> cm = c[pos++];
		if (cm.isCollection() || cm.isOptional()) {
			ClassMeta<?> ce = c.length == pos ? object() : getTypedClassMeta(c, pos);
			return (ce.isObject() ? cm : new ClassMeta(cm, null, null, ce));
		} else if (cm.isMap()) {
			ClassMeta<?> ck = c.length == pos ? object() : c[pos++];
			ClassMeta<?> cv = c.length == pos ? object() : getTypedClassMeta(c, pos);
			return (ck.isObject() && cv.isObject() ? cm : new ClassMeta(cm, ck, cv, null));
		}
		return cm;
	}

	final ClassMeta resolveClassMeta(Type o, Map<Class<?>,Class<?>[]> typeVarImpls) {
		if (o == null)
			return null;

		if (o instanceof ClassMeta) {
			ClassMeta<?> cm = (ClassMeta)o;

			// This classmeta could have been created by a different context.
			// Need to re-resolve it to pick up PojoSwaps and stuff on this context.
			if (cm.getBeanContext() == this)
				return cm;
			if (cm.isMap())
				return getClassMeta(cm.innerClass, cm.getKeyType(), cm.getValueType());
			if (cm.isCollection() || cm.isOptional())
				return getClassMeta(cm.innerClass, cm.getElementType());
			return getClassMeta(cm.innerClass);
		}

		Class c = resolve(o, typeVarImpls);

		// This can happen when trying to resolve the "E getFirst()" method on LinkedList, whose type is a TypeVariable
		// These should just resolve to Object.
		if (c == null)
			return object();

		ClassMeta rawType = getClassMeta(c);

		// If this is a Map or Collection, and the parameter types aren't part
		// of the class definition itself (e.g. class AddressBook extends List<Person>),
		// then we need to figure out the parameters.
		if (rawType.isMap() || rawType.isCollection() || rawType.isOptional()) {
			ClassMeta[] params = findParameters(o, c);
			if (params == null)
				return rawType;
			if (rawType.isMap()) {
				if (params.length != 2)
					return rawType;
				if (params[0].isObject() && params[1].isObject())
					return rawType;
				return new ClassMeta(rawType, params[0], params[1], null);
			}
			if (rawType.isCollection() || rawType.isOptional()) {
				if (params.length != 1)
					return rawType;
				if (params[0].isObject())
					return rawType;
				return new ClassMeta(rawType, null, null, params[0]);
			}
		}

		if (rawType.isArray()) {
			if (o instanceof GenericArrayType) {
				GenericArrayType gat = (GenericArrayType)o;
				ClassMeta elementType = resolveClassMeta(gat.getGenericComponentType(), typeVarImpls);
				return new ClassMeta(rawType, null, null, elementType);
			}
		}

		return rawType;
	}

	/**
	 * Convert a Type to a Class if possible.
	 * Return null if not possible.
	 */
	final Class resolve(Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {

		if (t instanceof Class)
			return (Class)t;

		if (t instanceof ParameterizedType)
			// A parameter (e.g. <String>.
			return (Class)((ParameterizedType)t).getRawType();

		if (t instanceof GenericArrayType) {
			// An array parameter (e.g. <byte[]>).
			Type gatct = ((GenericArrayType)t).getGenericComponentType();

			if (gatct instanceof Class)
				return Array.newInstance((Class)gatct, 0).getClass();

			if (gatct instanceof ParameterizedType)
				return Array.newInstance((Class)((ParameterizedType)gatct).getRawType(), 0).getClass();

			if (gatct instanceof GenericArrayType)
				return Array.newInstance(resolve(gatct, typeVarImpls), 0).getClass();

			return null;

		} else if (t instanceof TypeVariable) {
			if (typeVarImpls != null) {
				TypeVariable tv = (TypeVariable)t;
				String varName = tv.getName();
				int varIndex = -1;
				Class gc = (Class)tv.getGenericDeclaration();
				TypeVariable[] tvv = gc.getTypeParameters();
				for (int i = 0; i < tvv.length; i++) {
					if (tvv[i].getName().equals(varName)) {
						varIndex = i;
					}
				}
				if (varIndex != -1) {

					// If we couldn't find a type variable implementation, that means
					// the type was defined at runtime (e.g. Bean b = new Bean<Foo>();)
					// in which case the type is lost through erasure.
					// Assume java.lang.Object as the type.
					if (! typeVarImpls.containsKey(gc))
						return null;

					return typeVarImpls.get(gc)[varIndex];
				}
			}
		}
		return null;
	}

	final ClassMeta[] findParameters(Type o, Class c) {
		if (o == null)
			o = c;

		// Loop until we find a ParameterizedType
		if (! (o instanceof ParameterizedType)) {
			loop: do {
				o = c.getGenericSuperclass();
				if (o instanceof ParameterizedType)
					break loop;
				for (Type t : c.getGenericInterfaces()) {
					o = t;
					if (o instanceof ParameterizedType)
						break loop;
				}
				c = c.getSuperclass();
			} while (c != null);
		}

		if (o instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)o;
			if (! pt.getRawType().equals(Enum.class)) {
				List<ClassMeta<?>> l = new LinkedList<>();
				for (Type pt2 : pt.getActualTypeArguments()) {
					if (pt2 instanceof WildcardType || pt2 instanceof TypeVariable)
						return null;
					l.add(resolveClassMeta(pt2, null));
				}
				if (l.isEmpty())
					return null;
				return l.toArray(new ClassMeta[l.size()]);
			}
		}

		return null;
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())}.
	 *
	 * @param <T> The class of the object being passed in.
	 * @param o The class to find the class type for.
	 * @return The ClassMeta object, or <jk>null</jk> if {@code o} is <jk>null</jk>.
	 */
	public final <T> ClassMeta<T> getClassMetaForObject(T o) {
		if (o == null)
			return null;
		return (ClassMeta<T>)getClassMeta(o.getClass());
	}


	/**
	 * Used for determining the class type on a method or field where a {@code @Beanp} annotation may be present.
	 *
	 * @param <T> The class type we're wrapping.
	 * @param p The property annotation on the type if there is one.
	 * @param t The type.
	 * @param typeVarImpls
	 * 	Contains known resolved type parameters on the specified class so that we can result
	 * 	{@code ParameterizedTypes} and {@code TypeVariables}.
	 * 	Can be <jk>null</jk> if the information is not known.
	 * @return The new {@code ClassMeta} object wrapped around the {@code Type} object.
	 */
	protected final <T> ClassMeta<T> resolveClassMeta(Beanp p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = resolveClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;

		if (p != null) {

			if (p.type() != Null.class)
				cm2 = resolveClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw runtimeException("Invalid number of parameters specified for Map (must be 2): {0}", pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, keyType, valueType, null);
			}

			if (cm2.isCollection() || cm2.isOptional()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw runtimeException("Invalid number of parameters specified for {1} (must be 1): {0}", pParams.length, (cm2.isCollection() ? "Collection" : cm2.isOptional() ? "Optional" : "Array"));
				ClassMeta<?> elementType = resolveType(pParams[0], cm2.getElementType(), cm.getElementType());
				if (elementType.isObject())
					return cm2;
				return new ClassMeta<>(cm2, null, null, elementType);
			}

			return cm2;
		}

		return cm;
	}

	private ClassMeta<?> resolveType(Type...t) {
		for (Type tt : t) {
			if (tt != null) {
				ClassMeta<?> cm = getClassMeta(tt);
				if (tt != cmObject)
					return cm;
			}
		}
		return cmObject;
	}

	/**
	 * Returns the {@link PojoSwap} associated with the specified class, or <jk>null</jk> if there is no POJO swap
	 * associated with the class.
	 *
	 * @param <T> The class associated with the swap.
	 * @param c The class associated with the swap.
	 * @return The swap associated with the class, or null if there is no association.
	 */
	private final <T> PojoSwap[] findPojoSwaps(Class<T> c) {
		// Note:  On first
		if (c != null) {
			List<PojoSwap> l = new ArrayList<>();
			for (PojoSwap f : swapArray)
				if (f.getNormalClass().isParentOf(c))
					l.add(f);
			return l.size() == 0 ? null : l.toArray(new PojoSwap[l.size()]);
		}
		return null;
	}

	/**
	 * Checks whether a class has a {@link PojoSwap} associated with it in this bean context.
	 *
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specified class or one of its subclasses has a {@link PojoSwap} associated with it.
	 */
	private final PojoSwap[] findChildPojoSwaps(Class<?> c) {
		if (c == null || swapArray.length == 0)
			return null;
		List<PojoSwap> l = null;
		for (PojoSwap f : swapArray) {
			if (f.getNormalClass().isChildOf(c)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(f);
			}
		}
		return l == null ? null : l.toArray(new PojoSwap[l.size()]);
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Object</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent "any object type" when an object type is not known.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>Object</c> class.
	 */
	protected final ClassMeta<Object> object() {
		return cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>String</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	protected final ClassMeta<String> string() {
		return cmString;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <c>Class</c>.
	 *
	 * <p>
	 * This <c>ClassMeta</c> is often used to represent key types in maps.
	 *
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Class.<jk>class</jk>)</code> but uses a cached copy to
	 * avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <c>String</c> class.
	 */
	protected final ClassMeta<Class> _class() {
		return cmClass;
	}

	/**
	 * Returns the lookup table for resolving bean types by name.
	 *
	 * @return The lookup table for resolving bean types by name.
	 */
	protected final BeanRegistry getBeanRegistry() {
		return beanRegistry;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Minimum bean class visibility.
	 *
	 * @see BeanContextBuilder#beanClassVisibility(Visibility)
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() {
		return beanClassVisibility;
	}

	/**
	 * Minimum bean constructor visibility.
	 *
	 * @see BeanContextBuilder#beanConstructorVisibility(Visibility)
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() {
		return beanConstructorVisibility;
	}

	/**
	 * Bean dictionary.
	 *
	 * @see BeanContextBuilder#beanDictionary()
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 */
	public final List<Class<?>> getBeanDictionary() {
		return beanDictionary;
	}

	/**
	 * Minimum bean field visibility.
	 *
	 *
	 * @see BeanContextBuilder#beanFieldVisibility(Visibility)
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	public final Visibility getBeanFieldVisibility() {
		return beanFieldVisibility;
	}

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * @see BeanContextBuilder#beanMapPutReturnsOldValue()
	 * @return
	 * 	<jk>true</jk> if the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * 	<br>Otherwise, it returns <jk>null</jk>.
	 */
	public final boolean isBeanMapPutReturnsOldValue() {
		return beanMapPutReturnsOldValue;
	}

	/**
	 * Minimum bean method visibility.
	 *
	 * @see BeanContextBuilder#beanMethodVisibility(Visibility)
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() {
		return beanMethodVisibility;
	}

	/**
	 * Beans require no-arg constructors.
	 *
	 * @see BeanContextBuilder#beansRequireDefaultConstructor()
	 * @return
	 * 	<jk>true</jk> if a Java class must implement a default no-arg constructor to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireDefaultConstructor() {
		return beansRequireDefaultConstructor;
	}

	/**
	 * Beans require Serializable interface.
	 *
	 * @see BeanContextBuilder#beansRequireSerializable()
	 * @return
	 * 	<jk>true</jk> if a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSerializable() {
		return beansRequireSerializable;
	}

	/**
	 * Beans require setters for getters.
	 *
	 * @see BeanContextBuilder#beansRequireSettersForGetters()
	 * @return
	 * 	<jk>true</jk> if only getters that have equivalent setters will be considered as properties on a bean.
	 * 	<br>Otherwise, they are ignored.
	 */
	public final boolean isBeansRequireSettersForGetters() {
		return beansRequireSettersForGetters;
	}

	/**
	 * Beans require at least one property.
	 *
	 * @see BeanContextBuilder#disableBeansRequireSomeProperties()
	 * @return
	 * 	<jk>true</jk> if a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * 	<br>Otherwise, the bean is serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSomeProperties() {
		return beansRequireSomeProperties;
	}

	/**
	 * Bean type property name.
	 *
	 * @see BeanContextBuilder#typePropertyName(String)
	 * @return
	 * The name of the bean property used to store the dictionary name of a bean type so that the parser knows the data type to reconstruct.
	 */
	public final String getBeanTypePropertyName() {
		return typePropertyName;
	}

	/**
	 * Find fluent setters.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 *
	 * @see BeanContextBuilder#findFluentSetters()
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	public final boolean isFindFluentSetters() {
		return findFluentSetters;
	}

	/**
	 * Ignore invocation errors on getters.
	 *
	 * @see BeanContextBuilder#ignoreInvocationExceptionsOnGetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnGetters() {
		return ignoreInvocationExceptionsOnGetters;
	}

	/**
	 * Ignore invocation errors on setters.
	 *
	 * @see BeanContextBuilder#ignoreInvocationExceptionsOnSetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnSetters() {
		return ignoreInvocationExceptionsOnSetters;
	}

	/**
	 * Silently ignore missing setters.
	 *
	 * @see BeanContextBuilder#disableIgnoreMissingSetters()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreMissingSetters() {
		return ignoreMissingSetters;
	}

	/**
	 * Ignore transient fields.
	 *
	 * @see BeanContextBuilder#disableIgnoreTransientFields()
	 * @return
	 * 	<jk>true</jk> if fields and methods marked as transient should not be ignored.
	 */
	protected final boolean isIgnoreTransientFields() {
		return ignoreTransientFields;
	}

	/**
	 * Ignore unknown properties.
	 *
	 * @see BeanContextBuilder#ignoreUnknownBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	public final boolean isIgnoreUnknownBeanProperties() {
		return ignoreUnknownBeanProperties;
	}

	/**
	 * Ignore unknown properties with null values.
	 *
	 * @see BeanContextBuilder#disableIgnoreUnknownNullBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreUnknownNullBeanProperties() {
		return ignoreUnknownNullBeanProperties;
	}

	/**
	 * Bean class exclusions.
	 *
	 * @see BeanContextBuilder#notBeanClasses(Class...)
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 */
	protected final Class<?>[] getNotBeanClasses() {
		return notBeanClassesArray;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see BeanContextBuilder#notBeanPackages(String...)
	 * @return
	 * 	The list of fully-qualified package names to exclude from being classified as beans.
	 */
	public final String[] getNotBeanPackagesNames() {
		return notBeanPackageNames;
	}

	/**
	 * Bean package exclusions.
	 *
	 * @see BeanContextBuilder#notBeanPackages(String...)
	 * @return
	 * 	The list of package name prefixes to exclude from being classified as beans.
	 */
	protected final String[] getNotBeanPackagesPrefixes() {
		return notBeanPackagePrefixes;
	}

	/**
	 * Java object swaps.
	 *
	 * @see BeanContextBuilder#swaps(Class...)
	 * @return
	 * 	The list POJO swaps defined.
	 */
	public final PojoSwap<?,?>[] getSwaps() {
		return swapArray;
	}

	/**
	 * Bean property namer.
	 *
	 * @see BeanContextBuilder#propertyNamer(Class)
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	public final PropertyNamer getPropertyNamer() {
		return propertyNamerBean;
	}

	/**
	 * Sort bean properties.
	 *
	 * @see BeanContextBuilder#sortProperties()
	 * @return
	 * 	<jk>true</jk> if all bean properties will be serialized and access in alphabetical order.
	 */
	public final boolean isSortProperties() {
		return sortProperties;
	}

	/**
	 * Use enum names.
	 *
	 * @see BeanContextBuilder#useEnumNames()
	 * @return
	 * 	<jk>true</jk> if enums are always serialized by name, not using {@link Object#toString()}.
	 */
	public final boolean isUseEnumNames() {
		return useEnumNames;
	}

	/**
	 * Use interface proxies.
	 *
	 * @see BeanContextBuilder#disableInterfaceProxies()
	 * @return
	 * 	<jk>true</jk> if interfaces will be instantiated as proxy classes through the use of an
	 * 	{@link InvocationHandler} if there is no other way of instantiating them.
	 */
	public final boolean isUseInterfaceProxies() {
		return useInterfaceProxies;
	}

	/**
	 * Use Java Introspector.
	 *
	 * @see BeanContextBuilder#useJavaBeanIntrospector()
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	public final boolean isUseJavaBeanIntrospector() {
		return useJavaBeanIntrospector;
	}

	/**
	 * Locale.
	 *
	 * @see BeanContextBuilder#locale(Locale)
	 * @return
	 * 	The default locale for serializer and parser sessions.
	 */
	public final Locale getDefaultLocale() {
		return locale;
	}

	/**
	 * Media type.
	 *
	 * @see BeanContextBuilder#mediaType(MediaType)
	 * @return
	 * 	The default media type value for serializer and parser sessions.
	 */
	public final MediaType getDefaultMediaType() {
		return mediaType;
	}

	/**
	 * Time zone.
	 *
	 * @see BeanContextBuilder#timeZone(TimeZone)
	 * @return
	 * 	The default timezone for serializer and parser sessions.
	 */
	public final TimeZone getDefaultTimeZone() {
		return timeZone;
	}

	/**
	 * Returns the serializer to use for serializing beans when using the {@link BeanSession#convertToType(Object, Class)}
	 * and related methods.
	 *
	 * @return The serializer.  May be <jk>null</jk> if all initialization has occurred.
	 */
	protected WriterSerializer getBeanToStringSerializer() {
		if (beanToStringSerializer == null) {
			if (JsonSerializer.DEFAULT == null)
				return null;
			this.beanToStringSerializer = JsonSerializer.create().beanContext(this).sq().simpleMode().build();
		}
		return beanToStringSerializer;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"BeanContext",
				OMap
					.create()
					.filtered()
					.a("id", System.identityHashCode(this))
					.a("beanClassVisibility", beanClassVisibility)
					.a("beanConstructorVisibility", beanConstructorVisibility)
					.a("beanDictionary", beanDictionary)
					.a("beanFieldVisibility", beanFieldVisibility)
					.a("beanMethodVisibility", beanMethodVisibility)
					.a("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
					.a("beansRequireSerializable", beansRequireSerializable)
					.a("beansRequireSettersForGetters", beansRequireSettersForGetters)
					.a("beansRequireSomeProperties", beansRequireSomeProperties)
					.a("ignoreTransientFields", ignoreTransientFields)
					.a("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
					.a("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
					.a("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
					.a("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
					.a("notBeanClasses", notBeanClasses)
					.a("notBeanPackageNames", notBeanPackageNames)
					.a("notBeanPackagePrefixes", notBeanPackagePrefixes)
					.a("pojoSwaps", swaps)
					.a("sortProperties", sortProperties)
					.a("useEnumNames", useEnumNames)
					.a("useInterfaceProxies", useInterfaceProxies)
					.a("useJavaBeanIntrospector", useJavaBeanIntrospector)
			);
	}
}