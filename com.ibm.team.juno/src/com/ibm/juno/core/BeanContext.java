/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core;

import static com.ibm.juno.core.utils.ClassUtils.*;
import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.filter.Filter;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.ClassUtils.ClassComparator;
import com.ibm.juno.core.utils.*;

/**
 * Core class of the Juno architecture.
 * <p>
 * 	This class servers multiple purposes:
 * 	<ul>
 * 		<li>Provides the ability to wrap beans inside {@link Map} interfaces.
 * 		<li>Serves as a repository for metadata on POJOs, such as associated {@link Filter filters}, {@link PropertyNamer property namers}, etc...
 * 			which are used to tailor how POJOs are serialized and parsed.
 * 		<li>Serves as a common utility class for all {@link Serializer Serializers} and {@link Parser Parsers}
 * 				for serializing and parsing Java beans.
 * 	</ul>
 *
 *
 * <h5 class='topic'>Bean Contexts</h5>
 * <p>
 * 	Typically, it will be sufficient to use the existing {@link #DEFAULT} contexts for creating
 * 	bean maps.  However, if you want to tweak any of the settings on the context, you must
 * 	either clone the default context or create a new one from scratch (whichever is simpler for you).
 * 	You'll notice that this context class uses a fluent interface for defining settings.
 * <p>
 * 	Bean context factories can be locked using the {@link BeanContextFactory#lock()} method.  This makes the context settings
 * 	read-only.  Attempting to change a setting on a locked context will cause a {@link LockedException}
 * 	to be thrown.  The default context is locked by default.  Cloning a locked context using the
 * 	{@link #clone()} method produces a new unlocked context.  Locking a context is optional, although
 * 	it can prevent errors where bean contexts, parsers, or serializers start behaving differently
 * 	because a setting was changed.  As a general rule, if you want to change a setting on an existing
 * 	context, you should clone it, modify the setting(s), and then lock the new context.
 *
 *
 * <h5 class='topic'>BeanContext settings</h5>
 * 	<code>BeanContexts</code> have several settings that can be used to tweak behavior on how beans are handled.
 * <p>
 * 	Some settings (e.g. {@link BeanContextProperties#BEAN_beansRequireDefaultConstructor}) are used to differentiate between bean and non-bean classes.
 * 	Attempting to create a bean map around one of these objects will throw a {@link BeanRuntimeException}.
 * 	The purpose for this behavior is so that the serializers can identify these non-bean classes and convert them to plain strings using the {@link Object#toString()} method.
 * <p>
 * 	Some settings (e.g. {@link BeanContextProperties#BEAN_beanFieldVisibility}) are used to determine what kinds of properties are detected on beans.
 * <p>
 * 	Some settings (e.g. {@link BeanContextProperties#BEAN_beanMapPutReturnsOldValue}) change the runtime behavior of bean maps.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * 	This class has configurable properties that can be set through the {@link BeanContextFactory#setProperty(String, Object)} method.
 * <p>
 * 	See {@link BeanContextProperties} for settings applicable to this class.
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Construct a context from scratch.</jc>
 * 	BeanContext beanContext = <jk>new</jk> BeanContextFactory()
 * 		.setProperty(BeanContextProperties.<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
 * 		.addNotBeanClasses(Foo.<jk>class</jk>)
 * 		.getBeanContext();
 *
 * 	<jc>// Clone an existing context factory.</jc>
 * 	BeanContext beanContext = otherBeanContextFactory.clone()
 * 		.setProperty(BeanContextProperties.<jsf>BEAN_beansRequireDefaultConstructor</jsf>, <jk>true</jk>)
 * 		.addNotBeanClasses(Foo.<jk>class</jk>)
 * 		.getBeanContext();
 * </p>
 *
 *
 * <h5 class='topic'>Bean Maps</h5>
 * <p>
 * 	{@link BeanMap BeanMaps} are wrappers around Java beans that allow properties to be retrieved and
 * 	set using the common {@link Map#put(Object,Object)} and {@link Map#get(Object)} methods.<br>
 * 	<br>
 * 	Bean maps are created in two ways...
 * 	<ol>
 * 		<li> {@link BeanContext#forBean(Object) BeanContext.forBean()} - Wraps an existing bean inside a {@code Map} wrapper.
 * 		<li> {@link BeanContext#newBeanMap(Class) BeanContext.newInstance()} - Create a new bean instance wrapped in a {@code Map} wrapper.
 * 	</ol>
 *
 *
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// A sample bean class</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String name);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> age);
 * 	}
 *
 * 	<jc>// Wrap an existing bean in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; m1 = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> Person());
 * 	m1.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	m1.put(<js>"age"</js>, 45);
 *
 * 	<jc>// Create a new bean instance wrapped in a new bean map</jc>
 * 	BeanMap&lt;Person&gt; m2 = BeanContext.<jsf>DEFAULT</jsf>.newInstance(Person.<jk>class</jk>);
 * 	m2.put(<js>"name"</js>, <js>"John Smith"</js>);
 * 	m2.put(<js>"age"</js>, 45);
 * 	Person p = m2.getBean();  <jc>// Get the bean instance that was created.</jc>
 * </p>
 *
 *
 * <h5 class='topic'>Bean Annotations</h5>
 * <p>
 * 	This package contains annotations that can be applied to
 * 	class definitions to override what properties are detected on a bean.
 * <h6 class='topic'>Examples</h6>
 * <p class='bcode'>
 * 	<jc>// Bean class definition where only property 'name' is detected.</jc>
 * 	<ja>&#64;Bean</ja>(properties={<js>"name"</js>})
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> String getName();
 * 		<jk>public void</jk> setName(String name);
 * 		<jk>public int</jk> getAge();
 * 		<jk>public void</jk> setAge(<jk>int</jk> age);
 * 	}
 * <p>
 * 	See {@link Bean @Bean} and {@link BeanProperty @BeanProperty} for more information.
 *
 *
 * <h5 class='topic'>Beans with read-only properties</h5>
 * <p>
 * 	Bean maps can also be defined on top of beans with read-only properties by adding a
 * 	{@link BeanConstructor @BeanConstructor} annotation to one of the constructors on the
 * 	bean class.  This will allow read-only properties to be set through constructor arguments.
 * <p>
 * 	When the <code>@BeanConstructor</code> annotation is present, bean instantiation is delayed until the call to {@link BeanMap#getBean()}.
 * 	Until then, bean property values are stored in a local cache until <code>getBean()</code> is called.
 * 	Because of this additional caching step, parsing into read-only beans tends to be slower and use
 * 	more memory than parsing into beans with writable properties.
 * <p>
 * 	Attempting to call {@link BeanMap#put(String,Object)} on a read-only property after calling {@link BeanMap#getBean()}
 * 	will result in a {@link BeanRuntimeException} being thrown.
 * 	Multiple calls to {@link BeanMap#getBean()} will return the same bean instance.
 * <p>
 * 	Beans can be defined with a combination of read-only and read-write properties.
 * <p>
 * 	See {@link BeanConstructor @BeanConstructor} for more information.
 *
 *
 * <h5 class='topic'>Filters</h5>
 * <p>
 * 	{@link Filter Filters} are used to tailor how beans and non-beans are handled.<br>
 * 	There are two subclasses of filters:
 * 	<ol>
 * 		<li>{@link BeanFilter} - Allows you to tailor handling of bean classes.
 * 			This class can be considered a programmatic equivalent to the {@link Bean} annotation when
 * 			annotating classes are not possible (e.g. you don't have access to the source).
 * 		<li>{@link PojoFilter} - Allows you to convert objects to serializable forms.
 * 	</ol>
 * <p>
 * 	See {@link com.ibm.juno.core.filter} for more information.
 *
 *
 * <h5 class='topic'>ClassMetas</h5>
 * <p>
 * 	The {@link ClassMeta} class is a wrapper around {@link Class} object that provides cached information
 * 	about that class (e.g. whether it's a {@link Map} or {@link Collection} or bean).
 * <p>
 * 	As a general rule, it's best to reuse bean contexts (and therefore serializers and parsers too)
 * 	whenever possible since it takes some time to populate the internal {@code ClassMeta} object cache.
 * 	By reusing bean contexts, the class type metadata only needs to be calculated once which significantly
 * 	improves performance.
 * <p>
 * 	See {@link ClassMeta} for more information.
 *
 * @author Barry M. Caceres
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings({"unchecked","rawtypes"})
public final class BeanContext {

	// Cache of all instances of classMetaCache for all bean contexts keyed by
	// the hashcode of the bean context during creation.
	// TODO - Convert these to soft references.
	private static final ConcurrentHashMap<BeanContext,Map<Class,ClassMeta>> cmCacheCache = new ConcurrentHashMap<BeanContext,Map<Class,ClassMeta>>();

	/** Default context.  All default settings. */
	public static final BeanContext DEFAULT = new BeanContextFactory().getBeanContext();

	final boolean
		beansRequireDefaultConstructor,
		beansRequireSerializable,
		beansRequireSettersForGetters,
		beansRequireSomeProperties,
		beanMapPutReturnsOldValue,
		useInterfaceProxies,
		ignoreUnknownBeanProperties,
		ignoreUnknownNullBeanProperties,
		ignorePropertiesWithoutSetters,
		ignoreInvocationExceptionsOnGetters,
		ignoreInvocationExceptionsOnSetters,
		useJavaBeanIntrospector;

	final Visibility
		beanConstructorVisibility,
		beanClassVisibility,
		beanMethodVisibility,
		beanFieldVisibility;

	final int b;
	final BeanFilter[] beanFilters;
	final PojoFilter[] pojoFilters;
	final Class[] notBeanClasses;
	final String[] notBeanPackages;
	final Map<Class<?>,Class<?>> implClasses = new TreeMap<Class<?>, Class<?>>(new ClassComparator());
	final Map<String,String> uriVars = new TreeMap<String,String>();
	final Class[] implKeyClasses, implValueClasses;
	final String[] uriKeys, uriVals;
	final Map<Class,ClassMeta> cmCache;
	final ClassMeta<Object> cmObject;  // Reusable ClassMeta that represents general Objects.
	final ClassMeta<String> cmString;  // Reusable ClassMeta that represents general Strings.
	final String[] notBeanPackageNames, notBeanPackagePrefixes;
	final int hashCode;
	final ClassLoader classLoader;

	// Optional default parser set by setDefaultParser().
	final ReaderParser defaultParser;

	// Holds pending ClassMetas (created, but not yet initialized).
	final Deque<ClassMeta> pendingClassMetas = new LinkedList<ClassMeta>();


	BeanContext(BeanContextFactory bc) {
		beansRequireDefaultConstructor = bc.beansRequireDefaultConstructor;
		beansRequireSerializable = bc.beansRequireSerializable;
		beansRequireSettersForGetters = bc.beansRequireSettersForGetters;
		beansRequireSomeProperties = bc.beansRequireSomeProperties;
		beanMapPutReturnsOldValue = bc.beanMapPutReturnsOldValue;
		useInterfaceProxies = bc.useInterfaceProxies;
		ignoreUnknownBeanProperties = bc.ignoreUnknownBeanProperties;
		ignoreUnknownNullBeanProperties = bc.ignoreUnknownNullBeanProperties;
		ignorePropertiesWithoutSetters = bc.ignorePropertiesWithoutSetters;
		ignoreInvocationExceptionsOnGetters = bc.ignoreInvocationExceptionsOnGetters;
		ignoreInvocationExceptionsOnSetters = bc.ignoreInvocationExceptionsOnSetters;
		useJavaBeanIntrospector = bc.useJavaBeanIntrospector;
		beanConstructorVisibility = bc.beanConstructorVisibility;
		beanClassVisibility = bc.beanClassVisibility;
		beanMethodVisibility = bc.beanMethodVisibility;
		beanFieldVisibility = bc.beanFieldVisibility;

		b =
			(beansRequireDefaultConstructor ? 1<<1 : 0)
				+ (beansRequireSerializable ? 1<<2 : 0)
				+ (beansRequireSettersForGetters ? 1<<3 : 0)
				+ (beansRequireSomeProperties ? 1<<4 : 0)
				+ (beanMapPutReturnsOldValue ? 1<<5 : 0)
				+ (useJavaBeanIntrospector ? 1<<6 : 0)
				+ (useInterfaceProxies ? 1<<8 : 0)
				+ (ignoreUnknownBeanProperties ? 1<<9 : 0)
				+ (ignoreUnknownNullBeanProperties ? 1<<10 : 0)
				+ (ignorePropertiesWithoutSetters ? 1<<11 : 0)
				+ (ignoreInvocationExceptionsOnGetters ? 1<<12 : 0)
				+ (ignoreInvocationExceptionsOnSetters ? 1<<13 : 0);

		LinkedList<BeanFilter> lbf = new LinkedList<BeanFilter>();
		LinkedList<PojoFilter> lpf = new LinkedList<PojoFilter>();
 		for (Class<?> c : bc.filters) {
			if (isParentClass(Filter.class, c)) {
				try {
					if (isParentClass(BeanFilter.class, c)) {
						BeanFilter f = (BeanFilter)c.newInstance();
						f.setBeanContext(this);
						lbf.add(f);
					} else if (isParentClass(PojoFilter.class, c)) {
						PojoFilter f = (PojoFilter)c.newInstance();
						f.setBeanContext(this);
						lpf.add(f);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				if (! c.getClass().isInterface()) {
					List<SurrogateFilter<?,?>> l = SurrogateFilter.findFilters(c);
					if (! l.isEmpty()) {
						for (SurrogateFilter<?,?> f : l) {
							f.setBeanContext(this);
							lpf.add(f);
						}
						continue;
					}
				}
				BeanFilter f = new InterfaceBeanFilter(c);
				f.setBeanContext(this);
				lbf.add(f);
			}
		}
 		beanFilters = lbf.toArray(new BeanFilter[0]);
 		pojoFilters = lpf.toArray(new PojoFilter[0]);
		notBeanClasses = bc.notBeanClasses.toArray(new Class[0]);
		notBeanPackages = bc.notBeanPackages.toArray(new String[0]);
		implClasses.putAll(bc.implClasses);
		uriVars.putAll(bc.uriVars);
		implKeyClasses = implClasses.keySet().toArray(new Class[0]);
		implValueClasses = implClasses.values().toArray(new Class[0]);
		uriKeys = uriVars.keySet().toArray(new String[0]);
		uriVals = uriVars.values().toArray(new String[0]);

		List<String> l1 = new LinkedList<String>();
		List<String> l2 = new LinkedList<String>();
		for (String s : notBeanPackages) {
			if (s.endsWith(".*"))
				l2.add(s.substring(0, s.length()-2));
			else
				l1.add(s);
		}
		notBeanPackageNames = l1.toArray(new String[l1.size()]);
		notBeanPackagePrefixes = l2.toArray(new String[l2.size()]);

		defaultParser = bc.defaultParser;

		int h = b;
		for (BeanFilter f : beanFilters)
			h = hash(h, f.hashCode());
		for (PojoFilter f : pojoFilters)
			h = hash(h, f.hashCode());
		h = hash(h, beanConstructorVisibility.hashCode());
		h = hash(h, beanClassVisibility.hashCode());
		h = hash(h, beanMethodVisibility.hashCode());
		h = hash(h, beanFieldVisibility.hashCode());
		for (Class c : notBeanClasses)
			h += c.hashCode();
		for (String s : notBeanPackages)
			h += s.hashCode();
		for (Map.Entry<Class<?>,Class<?>> e : implClasses.entrySet()) {
			h += e.getKey().hashCode();
			h += e.getValue().hashCode();
		}
		for (Map.Entry<String,String> e : uriVars.entrySet()) {
			h += e.getKey().hashCode();
			h += e.getValue().hashCode();
		}
		hashCode = h;

		Map<Class,ClassMeta> cmc = cmCacheCache.get(this);
		if (cmc == null) {
			cmc = new ConcurrentHashMap<Class,ClassMeta>();
			cmc.put(Object.class, new ClassMeta(Object.class, this));
			cmc.put(String.class, new ClassMeta(String.class, this));
			Map<Class,ClassMeta> cmc2 = cmCacheCache.putIfAbsent(this, cmc);
			cmc = (cmc2 == null ? cmc : cmc2);
		}
		cmCache = cmc;
		cmObject = cmc.get(Object.class);
		cmString = cmc.get(String.class);

		classLoader = bc.classLoader;
	}

	/**
	 * Returns <jk>true</jk> if the specified bean context shares the same cache as this bean context.
	 * Useful for testing purposes.
	 *
	 * @param bc The bean context to compare to.
	 * @return <jk>true</jk> if the bean contexts have equivalent settings and thus share caches.
	 */
	public boolean hasSameCache(BeanContext bc) {
		return bc.cmCache == this.cmCache;
	}


	/**
	 * Bean property getter:  <property>ignoreUnknownBeanProperties</property>.
	 * See {@link BeanContextProperties#BEAN_ignoreUnknownBeanProperties}.
	 *
	 * @return The value of the <property>ignoreUnknownBeanProperties</property> property on this bean.
	 */
	public boolean isIgnoreUnknownBeanProperties() {
		return ignoreUnknownBeanProperties;
	}


	/**
	 * Determines whether the specified class is ignored as a bean class based on the various
	 * 	exclusion parameters specified on this context class.
	 *
	 * @param c The class type being tested.
	 * @return <jk>true</jk> if the specified class matches any of the exclusion parameters.
	 */
	protected boolean isNotABean(Class<?> c) {
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
		for (Class exclude : notBeanClasses)
			if (isParentClass(exclude, c))
				return true;
		return false;
	}

	/**
	 * Prints meta cache statistics to <code>System.out</code>.
	 */
	protected static void dumpCacheStats() {
		try {
			int ctCount = 0;
			for (Map<Class,ClassMeta> cm : cmCacheCache.values())
				ctCount += cm.size();
			System.out.println(MessageFormat.format("ClassMeta cache: {0} instances in {1} caches", ctCount, cmCacheCache.size()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (i.e. a modifiable {@link Map}).
	 * <p>
	 * 	If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> Person());
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @return The wrapped object.
	 */
	public <T> BeanMap<T> forBean(T o) {
		return this.forBean(o, (Class<T>)o.getClass());
	}

	/**
	 * Determines whether the specified object matches the requirements on this context of being a bean.
	 *
	 * @param o The object being tested.
	 * @return <jk>true</jk> if the specified object is considered a bean.
	 */
	public boolean isBean(Object o) {
		if (o == null)
			return false;
		return isBean(o.getClass());
	}

	/**
	 * Determines whether the specified class matches the requirements on this context of being a bean.
	 *
	 * @param c The class being tested.
	 * @return <jk>true</jk> if the specified class is considered a bean.
	 */
	public boolean isBean(Class<?> c) {
		return getBeanMeta(c) != null;
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (i.e.: a modifiable {@link Map})
	 * defined as a bean for one of its class, a super class, or an implemented interface.
	 * <p>
	 * 	If object is not a true bean, throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a bean map for new bean using only properties defined in a superclass</jc>
	 * 	BeanMap&lt;MySubBean&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> MySubBean(), MySuperBean.<jk>class</jk>);
	 *
	 * 	<jc>// Construct a bean map for new bean using only properties defined in an interface</jc>
	 * 	BeanMap&lt;MySubBean&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.forBean(<jk>new</jk> MySubBean(), MySuperInterface.<jk>class</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a bean interface.  Must not be null.
	 * @param c The superclass to narrow the bean properties to.  Must not be null.
	 * @return The bean representation, or <jk>null</jk> if the object is not a true bean.
	 * @throws NullPointerException If either parameter is null.
	 * @throws IllegalArgumentException If the specified object is not an an instance of the specified class.
	 * @throws BeanRuntimeException If specified object is not a bean according to the bean rules
	 * 		specified in this context class.
	 */
	public <T> BeanMap<T> forBean(T o, Class<? super T> c) throws BeanRuntimeException {
		assertFieldNotNull(o, "o");
		assertFieldNotNull(c, "c");

		if (! c.isInstance(o))
			illegalArg("The specified object is not an instance of the specified class.  class=''{0}'', objectClass=''{1}'', object=''{2}''", c.getName(), o.getClass().getName(), 0);

		ClassMeta cm = getClassMeta(c);

		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			throw new BeanRuntimeException(c, "Class is not a bean.  Reason=''{0}''", cm.getNotABeanReason());
		return new BeanMap<T>(o, m);
	}

	/**
	 * Creates a new {@link BeanMap} object (i.e. a modifiable {@link Map}) of the given class with uninitialized property values.
	 * <p>
	 * 	If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a new bean map wrapped around a new Person object</jc>
	 * 	BeanMap&lt;Person&gt; bm = BeanContext.<jsf>DEFAULT</jsf>.newBeanMap(Person.<jk>class</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @return A new instance of the class.
	 */
	public <T> BeanMap<T> newBeanMap(Class<T> c) {
		return newBeanMap(null, c);
	}

	/**
	 * Same as {@link #newBeanMap(Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new instance of the class.
	 */
	public <T> BeanMap<T> newBeanMap(Object outer, Class<T> c) {
		BeanMeta m = getBeanMeta(c);
		if (m == null)
			return null;
		T bean = null;
		if (m.constructorArgs.length == 0) {
			bean = newBean(outer, c);
			// Beans with subtypes won't be instantiated until the sub type property is specified.
			if (bean == null && ! m.getClassMeta().hasSubTypes())
				return null;
		}
		return new BeanMap<T>(bean, m);
	}

	/**
	 * Creates a new empty bean of the specified type, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p class='bcode'>
	 * 	<jc>// Construct a new instance of the specified bean class</jc>
	 * 	Person p = BeanContext.<jsf>DEFAULT</jsf>.newBean(Person.<jk>class</jk>);
	 * </p>
	 * 	</dd>
	 * </dl>
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public <T> T newBean(Class<T> c) throws BeanRuntimeException {
		return newBean(null, c);
	}

	/**
	 * Same as {@link #newBean(Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public <T> T newBean(Object outer, Class<T> c) throws BeanRuntimeException {
		ClassMeta<T> cm = getClassMeta(c);
		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			return null;
		try {
			T o = (T)m.newBean(outer);
			if (o == null) {
				// Beans with subtypes won't be instantiated until the sub type property is specified.
				if (cm.beanFilter != null && cm.beanFilter.getSubTypeProperty() != null)
					return null;
				throw new BeanRuntimeException(c, "Class does not have a no-arg constructor.");
			}
			return o;
		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new BeanRuntimeException(e);
		}
	}

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class
	 * 	is not a bean per the settings on this context.
	 */
	public <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Returns the class type bound to this bean context if the specified class type
	 * 	is from another bean context.
	 * <p>
	 * For example, this method allows you to pass in an object from <code>BeanContext.<jsf>DEFAULT</jsf>.getMapClassMeta(...)</code>
	 * 	to any of the <code>ReaderParser.parse(Reader, ClassMeta, ParserContext)</code> methods, and the parsers
	 * 	will use this method to replace the class type with the one registered with the parser.
	 * This ensures that registered filters are applied correctly.
	 *
	 * @param <T> The class type.
	 * @param cm The class type.
	 * @return The class type bound by this bean context.
	 */
	public <T> ClassMeta<T> normalizeClassMeta(ClassMeta<T> cm) {
		if (cm == null)
			return (ClassMeta<T>)object();
		if (cm.beanContext == this || cm.beanContext.equals(this))
			return cm;
		if (cm.isMap()) {
			ClassMeta<Map> cm2 = (ClassMeta<Map>)cm;
			cm2 = getMapClassMeta(cm2.getInnerClass(), cm2.getKeyType().getClass(), cm2.getValueType().getClass());
			return (ClassMeta<T>)cm2;
		}
		if (cm.isCollection()) {
			ClassMeta<Collection> cm2 = (ClassMeta<Collection>)cm;
			cm2 = getCollectionClassMeta(cm2.getInnerClass(), cm2.getElementType().getClass());
		}
		return getClassMeta(cm.getInnerClass());
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param c The class being wrapped.
	 * 	of type {@link Class} or {@link ClassMeta}.
	 * @return If the class is not an array, returns a cached {@link ClassMeta} object.
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.<br>
	 */
	public <T> ClassMeta<T> getClassMeta(Class<T> c) {

		// If this is an array, then we want it wrapped in an uncached ClassMeta object.
		// Note that if it has a pojo filter, we still want to cache it so that
		// we can cache something like byte[] with ByteArrayBase64Filter.
		if (c.isArray() && findPojoFilter(c) == null)
			return new ClassMeta(c, this);

		// This can happen if we have filters defined against String or Object.
		if (cmCache == null)
			return null;

		ClassMeta<T> cm = cmCache.get(c);
		if (cm == null) {

			synchronized (this) {

				// Make sure someone didn't already set it while this thread was blocked.
				cm = cmCache.get(c);
				if (cm == null) {

					// Note:  Bean properties add the possibility that class reference loops exist.
					// To handle this possibility, we create a set of pending ClassMetas, and
					// call init (which finds the bean properties) after it's been added to the pending set.
					for (ClassMeta pcm : pendingClassMetas)
						if (pcm.innerClass == c)
							return pcm;

					cm = new ClassMeta<T>(c, this, true);
					pendingClassMetas.addLast(cm);
					try {
						cm.init();
					} finally {
						pendingClassMetas.removeLast();
					}
					cmCache.put(c, cm);
				}
			}
		}
		return cm;
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <K> The map key class type.
	 * @param <V> The map value class type.
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are OBJECT, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <K,V,T extends Map<K,V>> ClassMeta<T> getMapClassMeta(Class<T> c, ClassMeta<K> keyType, ClassMeta<V> valueType) {
		if (keyType.isObject() && valueType.isObject())
			return getClassMeta(c);
		return new ClassMeta(c, this).setKeyType(keyType).setValueType(valueType);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <K> The map key class type.
	 * @param <V> The map value class type.
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are Object, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <K,V,T extends Map<K,V>> ClassMeta<T> getMapClassMeta(Class<T> c, Class<K> keyType, Class<V> valueType) {
		return getMapClassMeta(c, getClassMeta(keyType), getClassMeta(valueType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Map} object.
	 *
	 * @param <T> The map class type.
	 * @param c The map class type.
	 * @param keyType The map key class type.
	 * @param valueType The map value class type.
	 * @return If the key and value types are Object, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <T extends Map> ClassMeta<T> getMapClassMeta(Class<T> c, Type keyType, Type valueType) {
		return getMapClassMeta(c, getClassMeta(keyType), getClassMeta(valueType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <E> The collection element class type.
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <E,T extends Collection<E>> ClassMeta<T> getCollectionClassMeta(Class<T> c, ClassMeta<E> elementType) {
		if (elementType.isObject())
			return getClassMeta(c);
		return new ClassMeta(c, this).setElementType(elementType);
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <E> The collection element class type.
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <E,T extends Collection<E>> ClassMeta<T> getCollectionClassMeta(Class<T> c, Class<E> elementType) {
		return getCollectionClassMeta(c, getClassMeta(elementType));
	}

	/**
	 * Construct a {@code ClassMeta} wrapper around a {@link Collection} object.
	 *
	 * @param <T> The collection class type.
	 * @param c The collection class type.
	 * @param elementType The collection element class type.
	 * @return If the element type is <code>OBJECT</code>, returns a cached {@link ClassMeta} object.<br>
	 * 	Otherwise, returns a new {@link ClassMeta} object every time.
	 */
	public <T extends Collection> ClassMeta<T> getCollectionClassMeta(Class<T> c, Type elementType) {
		return getCollectionClassMeta(c, getClassMeta(elementType));
	}

	/**
	 * Constructs a ClassMeta object given the specified object and parameters.
	 *
	 * @param o The parent class type.
	 * 	Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link ClassMeta} object, which just returns the same object.
	 * 		<li>{@link Class} object (e.g. <code>String.<jk>class</jk></code>).
	 * 		<li>{@link Type} object (e.g. {@link ParameterizedType} or {@link GenericArrayType}.
	 * 		<li>Anything else is interpreted as {@code getClassMeta(o.getClass(), parameters);}
	 * 	</ul>
	 * @return A ClassMeta object, or <jk>null</jk> if the object is null.
	 */
	public ClassMeta getClassMeta(Type o) {
		return getClassMeta(o, null);
	}

	ClassMeta getClassMeta(Type o, Map<Class<?>,Class<?>[]> typeVarImpls) {
		if (o == null)
			return null;

		if (o instanceof ClassMeta)
			return (ClassMeta)o;

		Class c = null;
		if (o instanceof Class) {
			c = (Class)o;
		} else if (o instanceof ParameterizedType) {
			// A parameter (e.g. <String>.
			c = (Class<?>)((ParameterizedType)o).getRawType();
		} else if (o instanceof GenericArrayType) {
			// An array parameter (e.g. <byte[]>.
			GenericArrayType gat = (GenericArrayType)o;
			Type gatct = gat.getGenericComponentType();
			if (gatct instanceof Class) {
				Class gatctc = (Class)gatct;
				c = Array.newInstance(gatctc, 0).getClass();
			} else if (gatct instanceof ParameterizedType) {
				Class gatctc = (Class<?>)((ParameterizedType)gatct).getRawType();
				c = Array.newInstance(gatctc, 0).getClass();
			} else {
				return null;
			}
		} else if (o instanceof TypeVariable) {
			if (typeVarImpls != null) {
				TypeVariable t = (TypeVariable) o;
				String varName = t.getName();
				int varIndex = -1;
				Class gc = (Class)t.getGenericDeclaration();
				TypeVariable[] tv = gc.getTypeParameters();
				for (int i = 0; i < tv.length; i++) {
					if (tv[i].getName().equals(varName)) {
						varIndex = i;
					}
				}
				if (varIndex != -1) {

					// If we couldn't find a type variable implementation, that means
					// the type was defined at runtime (e.g. Bean b = new Bean<Foo>();)
					// in which case the type is lost through erasure.
					// Assume java.lang.Object as the type.
					if (! typeVarImpls.containsKey(gc))
						return object();

					return getClassMeta(typeVarImpls.get(gc)[varIndex]);
				}
			}
			// We don't know the bounded type, so just resolve to Object.
			return object();
		} else {
			// This can happen when trying to resolve the "E getFirst()" method on LinkedList, whose type is a TypeVariable
			// These should just resolve to Object.
			return object();
		}

		ClassMeta rawType = getClassMeta(c);

		// If this is a Map or Collection, and the parameter types aren't part
		// of the class definition itself (e.g. class AddressBook extends List<Person>),
		// then we need to figure out the parameters.
		if (rawType.isMap() || rawType.isCollection()) {
			ClassMeta[] params = findParameters(o, c);
			if (params == null)
				return rawType;
			if (rawType.isMap()) {
				if (params.length != 2)
					return rawType;
				if (params[0].isObject() && params[1].isObject())
					return rawType;
				return new ClassMeta(rawType.innerClass, this).setKeyType(params[0]).setValueType(params[1]);
			}
			if (rawType.isCollection()) {
				if (params.length != 1)
					return rawType;
				if (params[0].isObject())
					return rawType;
				return new ClassMeta(rawType.innerClass, this).setElementType(params[0]);
			}
		}

		return rawType;
	}

	/**
	 * Given an array of {@link Class} objects, returns an array of corresponding {@link ClassMeta} objects.
	 * Constructs a new array on each call.
	 *
	 * @param classes The array of classes to get class metas for.
	 * @return An array of {@link ClassMeta} objects corresponding to the classes.  Never <jk>null</jk>.
	 */
	public ClassMeta<?>[] getClassMetas(Class<?>[] classes) {
		assertFieldNotNull(classes, "classes");
		ClassMeta<?>[] cm = new ClassMeta<?>[classes.length];
		for (int i = 0; i < classes.length; i++)
			cm[i] = getClassMeta(classes[i]);
		return cm;
	}

	ClassMeta[] findParameters(Type o, Class c) {
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
				List<ClassMeta<?>> l = new LinkedList<ClassMeta<?>>();
				for (Type pt2 : pt.getActualTypeArguments()) {
					if (pt2 instanceof WildcardType || pt2 instanceof TypeVariable)
						return null;
					l.add(getClassMeta(pt2, null));
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
	public <T> ClassMeta<T> getClassMetaForObject(T o) {
		if (o == null)
			return null;
		return (ClassMeta<T>)getClassMeta(o.getClass());
	}


	/**
	 * Used for determining the class type on a method or field where a {@code @BeanProperty} annotation
	 * 	may be present.
	 *
	 * @param <T> The class type we're wrapping.
	 * @param p The property annotation on the type if there is one.
	 * @param t The type.
	 * @param typeVarImpls Contains known resolved type parameters on the specified class so
	 * 	that we can result {@code ParameterizedTypes} and {@code TypeVariables}.<br>
	 * 	Can be <jk>null</jk> if the information is not known.
	 * @return The new {@code ClassMeta} object wrapped around the {@code Type} object.
	 */
	protected <T> ClassMeta<T> getClassMeta(BeanProperty p, Type t, Map<Class<?>,Class<?>[]> typeVarImpls) {
		ClassMeta<T> cm = getClassMeta(t, typeVarImpls);
		ClassMeta<T> cm2 = cm;
		if (p != null) {

			if (p.type() != Object.class)
				cm2 = getClassMeta(p.type(), typeVarImpls);

			if (cm2.isMap()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class, Object.class} : p.params());
				if (pParams.length != 2)
					throw new RuntimeException("Invalid number of parameters specified for Map (must be 2): " + pParams.length);
				ClassMeta<?> keyType = resolveType(pParams[0], cm2.getKeyType(), cm.getKeyType());
				ClassMeta<?> valueType = resolveType(pParams[1], cm2.getValueType(), cm.getValueType());
				if (keyType.isObject() && valueType.isObject())
					return cm2;
				return new ClassMeta<T>(cm2.innerClass, this).setKeyType(keyType).setValueType(valueType);
			}

			if (cm2.isCollection()) {
				Class<?>[] pParams = (p.params().length == 0 ? new Class[]{Object.class} : p.params());
				if (pParams.length != 1)
					throw new RuntimeException("Invalid number of parameters specified for Collection (must be 1): " + pParams.length);
				ClassMeta<?> elementType = resolveType(pParams[0], cm2.getElementType(), cm.getElementType());
				if (elementType.isObject())
					return cm2;
				return new ClassMeta<T>(cm2.innerClass, this).setElementType(elementType);
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
	 * Converts class name strings to ClassMeta objects.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <ul>
	 * 	<li>{@code java.lang.String}
	 * 	<li>{@code com.ibm.sample.MyBean[]}
	 * 	<li>{@code java.util.HashMap<java.lang.String,java.lang.Integer>}
	 * 	<li>{@code [Ljava.lang.String;} (i.e. the value of <code>String[].<jk>class</jk>.getName()</code>
	 * </ul>
	 * 	</dd>
	 * </dl>
	 *
	 * @param s The class name.
	 * @return The ClassMeta corresponding to the class name string.
	 */
	public ClassMeta<?> getClassMetaFromString(String s) {
		int d = 0;
		if (s == null || s.isEmpty())
			return null;

		// Check for Class.getName() on array class types.
		if (s.charAt(0) == '[') {
			try {
				return getClassMeta(findClass(s));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		int i1 = 0;
		int i2 = 0;
		int dim = 0;
		List<ClassMeta<?>> p = null;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '<') {
				if (d == 0) {
					i1 = i;
					i2 = i+1;
					p = new LinkedList<ClassMeta<?>>();
				}
				d++;
			} else if (c == '>') {
				d--;
				if (d == 0 && p != null)
					p.add(getClassMetaFromString(s.substring(i2, i)));
			} else if (c == ',' && d == 1) {
				if (p != null)
					p.add(getClassMetaFromString(s.substring(i2, i)));
				i2 = i+1;
			}
			if (c == '[') {
				if (i1 == 0)
					i1 = i;
				dim++;
			}
		}
		if (i1 == 0)
			i1 = s.length();
		try {
			String name = s.substring(0, i1).trim();
			char x = name.charAt(0);
			Class<?> c = null;
			if (x >= 'b' && x <= 's') {
				if (x == 'b' && name.equals("boolean"))
					c = boolean.class;
				else if (x == 'b' && name.equals("byte"))
					c = byte.class;
				else if (x == 'c' && name.equals("char"))
					c = char.class;
				else if (x == 'd' && name.equals("double"))
					c = double.class;
				else if (x == 'i' && name.equals("int"))
					c = int.class;
				else if (x == 'l' && name.equals("long"))
					c = long.class;
				else if (x == 's' && name.equals("short"))
					c = short.class;
				else
					c = findClass(name);
			} else {
				c = findClass(name);
			}

			ClassMeta<?> cm = getClassMeta(c);

			if (p != null) {
				if (cm.isMap())
					cm = new ClassMeta(c, this).setKeyType(p.get(0)).setValueType(p.get(1));
				if (cm.isCollection())
					cm = new ClassMeta(c, this).setElementType(p.get(0));
			}

			while (dim > 0) {
				cm = new ClassMeta(Array.newInstance(cm.getInnerClass(), 0).getClass(), this);
				dim--;
			}

			return cm;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Class<?> findClass(String name) throws ClassNotFoundException {
		return classLoader == null ? Class.forName(name) : Class.forName(name, true, classLoader);
	}

	/**
	 * Returns the {@link PojoFilter} associated with the specified class, or <jk>null</jk> if there is no
	 * pojo filter associated with the class.
	 *
	 * @param <T> The class associated with the filter.
	 * @param c The class associated with the filter.
	 * @return The filter associated with the class, or null if there is no association.
	 */
	protected <T> PojoFilter findPojoFilter(Class<T> c) {
		// Note:  On first
		if (c != null)
			for (PojoFilter f : pojoFilters)
				if (isParentClass(f.forClass(), c))
					return f;
		return null;
	}

	/**
	 * Checks whether a class has a {@link PojoFilter} associated with it in this bean context.
	 * @param c The class to check.
	 * @return <jk>true</jk> if the specified class or one of its subclasses has a {@link PojoFilter} associated with it.
	 */
	protected boolean hasChildPojoFilters(Class<?> c) {
		if (c != null)
			for (PojoFilter f : pojoFilters)
				if (isParentClass(c, f.forClass()))
					return true;
		return false;
	}

	/**
	 * Returns the {@link BeanFilter} associated with the specified class, or <jk>null</jk> if there is no
	 * bean filter associated with the class.
	 *
	 * @param <T> The class associated with the filter.
	 * @param c The class associated with the filter.
	 * @return The filter associated with the class, or null if there is no association.
	 */
	protected <T> BeanFilter findBeanFilter(Class<T> c) {
		if (c != null)
			for (BeanFilter f : beanFilters)
				if (isParentClass(f.forClass(), c))
					return f;
		return null;
	}

	/**
	 * Gets the no-arg constructor for the specified class.
	 *
	 * @param <T> The class to check.
	 * @param c The class to check.
	 * @param v The minimum visibility for the constructor.
	 * @return The no arg constructor, or <jk>null</jk> if the class has no no-arg constructor.
	 */
	protected <T> Constructor<? extends T> getImplClassConstructor(Class<T> c, Visibility v) {
		if (implClasses.isEmpty())
			return null;
		Class cc = c;
		while (cc != null) {
			Class implClass = implClasses.get(cc);
			if (implClass != null)
				return ClassMeta.findNoArgConstructor(implClass, v);
			for (Class ic : cc.getInterfaces()) {
				implClass = implClasses.get(ic);
				if (implClass != null)
					return ClassMeta.findNoArgConstructor(implClass, v);
			}
			cc = cc.getSuperclass();
		}
		return null;
	}

	/**
	 * Converts the specified value to the specified class type.
	 * <p>
	 * 	See {@link #convertToType(Object, ClassMeta)} for the list of valid conversions.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public <T> T convertToType(Object value, Class<T> type) throws InvalidDataConversionException {
		// Shortcut for most common case.
		if (value != null && value.getClass() == type)
			return (T)value;
		return convertToType(null, value, getClassMeta(type));
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public <T> T convertToType(Object outer, Object value, Class<T> type) throws InvalidDataConversionException {
		return convertToType(outer, value, getClassMeta(type));
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>Object</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent "any object type" when an object type
	 * 	is not known.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(Object.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>Object</code> class.
	 */
	public ClassMeta<Object> object() {
		return cmObject;
	}

	/**
	 * Returns a reusable {@link ClassMeta} representation for the class <code>String</code>.
	 * <p>
	 * This <code>ClassMeta</code> is often used to represent key types in maps.
	 * <p>
	 * This method is identical to calling <code>getClassMeta(String.<jk>class</jk>)</code> but uses
	 * 	a cached copy to avoid a hashmap lookup.
	 *
	 * @return The {@link ClassMeta} object associated with the <code>String</code> class.
	 */
	public ClassMeta<String> string() {
		return cmString;
	}

	/**
	 * Casts the specified value into the specified type.
	 * <p>
	 * 	If the value isn't an instance of the specified type, then converts
	 * 	the value if possible.<br>
	 * <p>
	 * 	The following conversions are valid:
	 * 	<table class='styled'>
	 * 		<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
	 * 		<tr>
	 * 			<td>
	 * 				A class that is the normal type of a registered {@link PojoFilter}.
	 * 			</td>
	 * 			<td>
	 * 				A value whose class matches the filtered type of that registered {@link PojoFilter}.
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				A class that is the filtered type of a registered {@link PojoFilter}.
	 * 			</td>
	 * 			<td>
	 * 				A value whose class matches the normal type of that registered {@link PojoFilter}.
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)<br>
	 * 				<code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>, <code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
	 * 			</td>
	 * 			<td>
	 * 				{@code Number}, {@code String}, <jk>null</jk>
	 * 			</td>
	 * 			<td>
	 * 				For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code ObjectMap})
	 * 			</td>
	 * 			<td>
	 * 				{@code Map}
	 * 			</td>
	 * 			<td>
	 * 				If {@code Map} is not constructible, a {@code ObjectMap} is created.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 			{@code Collection} (e.g. {@code List}, {@code LinkedList}, {@code HashSet}, {@code ObjectList})
	 * 			</td>
	 * 			<td>
	 * 				{@code Collection<Object>}<br>
	 * 				{@code Object[]}
	 * 			</td>
	 * 			<td>
	 * 				If {@code Collection} is not constructible, a {@code ObjectList} is created.
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code X[]} (array of any type X)<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code List<X>}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code X[][]} (multi-dimensional arrays)<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code List<List<X>>}<br>
	 * 				{@code List<X[]>}<br>
	 * 				{@code List[]<X>}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code Enum}<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code String}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				Bean<br>
	 * 			</td>
	 * 			<td>
	 * 				{@code Map}<br>
	 * 			</td>
	 * 			<td>&nbsp;</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				{@code String}<br>
	 * 			</td>
	 * 			<td>
	 * 				Anything<br>
	 * 			</td>
	 * 			<td>
	 * 				Arrays are converted to JSON arrays<br>
	 * 			</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>
	 * 				Anything with one of the following methods:<br>
	 * 				<code><jk>public static</jk> T fromString(String)</code><br>
	 * 				<code><jk>public static</jk> T valueOf(String)</code><br>
	 * 				<code><jk>public<jk> T(String)</code><br>
	 * 			</td>
	 * 			<td>
	 * 				<code>String</code><br>
	 * 			</td>
	 * 			<td>
	 * 				<br>
	 * 			</td>
	 * 		</tr>
	 * 	</table>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public <T> T convertToType(Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		return convertToType(null, value, type);
	}

	/**
	 * Same as {@link #convertToType(Object, ClassMeta)}, except used for instantiating inner member classes that must
	 * 	be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public <T> T convertToType(Object outer, Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		if (type == null)
			type = (ClassMeta<T>)object();

		try {
			// Handle the case of a null value.
			if (value == null) {

				// If it's a primitive, then use the converters to get the default value for the primitive type.
				if (type.isPrimitive())
					return type.getPrimitiveDefault();

				// Otherwise, just return null.
				return null;
			}

			Class<T> tc = type.getInnerClass();

			// If no conversion needed, then just return the value.
			// Don't include maps or collections, because child elements may need conversion.
			if (tc.isInstance(value))
				if (! ((type.isMap() && type.getValueType().isNotObject()) || (type.isCollection() && type.getElementType().isNotObject())))
					return (T)value;

			if (type.getPojoFilter() != null) {
				PojoFilter f = type.getPojoFilter();
				Class<?> nc = f.getNormalClass(), fc = f.getFilteredClass();
				if (isParentClass(nc, tc) && isParentClass(fc, value.getClass()))
					return (T)f.unfilter(value, type);
			}

			ClassMeta<?> vt = getClassMetaForObject(value);
			if (vt.getPojoFilter() != null) {
				PojoFilter f = vt.getPojoFilter();
				Class<?> nc = f.getNormalClass(), fc = f.getFilteredClass();
				if (isParentClass(nc, vt.getInnerClass()) && isParentClass(fc, tc))
					return (T)f.filter(value);
			}

			if (type.isPrimitive()) {
				if (value.toString().isEmpty())
					return type.getPrimitiveDefault();

				if (type.isNumber()) {
					if (value instanceof Number) {
						Number n = (Number)value;
						if (tc == Integer.TYPE)
							return (T)Integer.valueOf(n.intValue());
						if (tc == Short.TYPE)
							return (T)Short.valueOf(n.shortValue());
						if (tc == Long.TYPE)
							return (T)Long.valueOf(n.longValue());
						if (tc == Float.TYPE)
							return (T)Float.valueOf(n.floatValue());
						if (tc == Double.TYPE)
							return (T)Double.valueOf(n.doubleValue());
						if (tc == Byte.TYPE)
							return (T)Byte.valueOf(n.byteValue());
					} else {
						String n = null;
						if (value instanceof Boolean)
							n = ((Boolean)value).booleanValue() ? "1" : "0";
						else
							n = value.toString();
						if (tc == Integer.TYPE)
							return (T)Integer.valueOf(n);
						if (tc == Short.TYPE)
							return (T)Short.valueOf(n);
						if (tc == Long.TYPE)
							return (T)Long.valueOf(n);
						if (tc == Float.TYPE)
							return (T)new Float(n);
						if (tc == Double.TYPE)
							return (T)new Double(n);
						if (tc == Byte.TYPE)
							return (T)Byte.valueOf(n);
					}
				} else if (type.isChar()) {
					String s = value.toString();
					return (T)Character.valueOf(s.length() == 0 ? 0 : s.charAt(0));
				} else if (type.isBoolean()) {
					if (value instanceof Number) {
						int i = ((Number)value).intValue();
						return (T)(i == 0 ? Boolean.FALSE : Boolean.TRUE);
					}
					return (T)Boolean.valueOf(value.toString());
				}
			}

			if (type.isNumber()) {
				if (value instanceof Number) {
				Number n = (Number)value;
				if (tc == Integer.class)
					return (T)Integer.valueOf(n.intValue());
				if (tc == Short.class)
					return (T)Short.valueOf(n.shortValue());
				if (tc == Long.class)
					return (T)Long.valueOf(n.longValue());
				if (tc == Float.class)
					return (T)Float.valueOf(n.floatValue());
				if (tc == Double.class)
					return (T)Double.valueOf(n.doubleValue());
				if (tc == Byte.class)
					return (T)Byte.valueOf(n.byteValue());
				} else {
					if (value.toString().isEmpty())
						return null;
					String n = null;
					if (value instanceof Boolean)
						n = ((Boolean)value).booleanValue() ? "1" : "0";
					else
						n = value.toString();
					if (tc == Integer.class)
						return (T)Integer.valueOf(n);
					if (tc == Short.class)
						return (T)Short.valueOf(n);
					if (tc == Long.class)
						return (T)Long.valueOf(n);
					if (tc == Float.class)
						return (T)new Float(n);
					if (tc == Double.class)
						return (T)new Double(n);
					if (tc == Byte.class)
						return (T)Byte.valueOf(n);
				}
			}

			if (type.isChar()) {
				String s = value.toString();
				return (T)Character.valueOf(s.length() == 0 ? 0 : s.charAt(0));
			}

			// Handle setting of array properties
			if (type.isArray()) {
				if (value instanceof List)
					return (T)toArray(type, (List)value);
				else if (value.getClass().isArray())
					return (T)toArray(type, Arrays.asList((Object[])value));
				else if (StringUtils.startsWith(value.toString(), '['))
					return (T)toArray(type, new ObjectList(value.toString()).setBeanContext(this));
			}

			// Target type is some sort of Map that needs to be converted.
			if (type.isMap()) {
				try {
					if (value instanceof Map) {
						Map m = type.canCreateNewInstance(outer) ? (Map)type.newInstance(outer) : new ObjectMap(this);
						ClassMeta keyType = type.getKeyType(), valueType = type.getValueType();
						for (Map.Entry e : (Set<Map.Entry>)((Map)value).entrySet()) {
							Object k = e.getKey();
							if (keyType.isNotObject()) {
								if (keyType.isString())
									k = k.toString();
								else
									k = convertToType(m, k, keyType);
							}
							Object v = e.getValue();
							if (valueType.isNotObject())
								v = convertToType(m, v, valueType);
							m.put(k, v);
						}
						return (T)m;
					} else if (!type.canCreateNewInstanceFromString(outer)) {
						ObjectMap m = new ObjectMap(value.toString(), defaultParser);
						return convertToType(outer, m, type);
					}
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), tc, e);
				}
			}

			// Target type is some sort of Collection
			if (type.isCollection()) {
				try {
					Collection l = type.canCreateNewInstance(outer) ? (Collection)type.newInstance(outer) : new ObjectList(this);
					ClassMeta elementType = type.getElementType();

					if (value.getClass().isArray())
						for (Object o : (Object[])value)
							l.add(elementType.isObject() ? o : convertToType(l, o, elementType));
					else if (value instanceof Collection)
						for (Object o : (Collection)value)
							l.add(elementType.isObject() ? o : convertToType(l, o, elementType));
					else if (value instanceof Map)
						l.add(elementType.isObject() ? value : convertToType(l, value, elementType));
					else if (! value.toString().isEmpty())
						throw new InvalidDataConversionException(value.getClass(), tc, null);
					return (T)l;
				} catch (InvalidDataConversionException e) {
					throw e;
				} catch (Exception e) {
					throw new InvalidDataConversionException(value.getClass(), tc, e);
				}
			}

			if (type.isString()) {
				Class<?> c = value.getClass();
				if (c.isArray()) {
					if (c.getComponentType().isPrimitive()) {
						ObjectList l = new ObjectList(this);
						int size = Array.getLength(value);
						for (int i = 0; i < size; i++)
							l.add(Array.get(value, i));
						return (T)l.toString();
					}
					return (T)new ObjectList((Object[])value).setBeanContext(this).toString();
				}
				return (T)value.toString();
			}

			if (type.isCharSequence()) {
				Class<?> c = value.getClass();
				if (c.isArray()) {
					if (c.getComponentType().isPrimitive()) {
						ObjectList l = new ObjectList(this);
						int size = Array.getLength(value);
						for (int i = 0; i < size; i++)
							l.add(Array.get(value, i));
						value = l;
					}
					value = new ObjectList((Object[])value).setBeanContext(this);
				}

				return type.newInstanceFromString(outer, value.toString());
			}

			if (type.isBoolean()) {
				if (value instanceof Number)
					return (T)(Boolean.valueOf(((Number)value).intValue() != 0));
				return (T)Boolean.valueOf(value.toString());
			}

			// It's a bean being initialized with a Map
			if (type.isBean() && value instanceof Map)
				return newBeanMap(tc).load((Map<?,?>) value).getBean();

			if (type.canCreateNewInstanceFromObjectMap(outer) && value instanceof ObjectMap)
				return type.newInstanceFromObjectMap(outer, (ObjectMap)value);

			if (type.canCreateNewInstanceFromString(outer))
				return type.newInstanceFromString(outer, value.toString());

			if (type.isBean())
				return newBeanMap(type.getInnerClass()).load(value.toString()).getBean();

		} catch (Exception e) {
			throw new InvalidDataConversionException(value, type.getInnerClass(), e);
		}

		throw new InvalidDataConversionException(value, type.getInnerClass(), null);
	}

	/**
	 * Converts the contents of the specified list into an array.
	 * <p>
	 * 	Works on both object and primitive arrays.
	 * <p>
	 * 	In the case of multi-dimensional arrays, the incoming list must
	 * 	contain elements of type n-1 dimension.  i.e. if {@code type} is <code><jk>int</jk>[][]</code>
	 * 	then {@code list} must have entries of type <code><jk>int</jk>[]</code>.
	 *
	 * @param type The type to convert to.  Must be an array type.
	 * @param list The contents to populate the array with.
	 * @return A new object or primitive array.
	 */
	public Object toArray(ClassMeta<?> type, Collection<?> list) {
		if (list == null)
			return null;
		ClassMeta<?> componentType = type.getElementType();
		Object array = Array.newInstance(componentType.getInnerClass(), list.size());
		int i = 0;
		for (Object o : list) {
			if (! type.getInnerClass().isInstance(o)) {
				if (componentType.isArray() && o instanceof Collection)
					o = toArray(componentType, (Collection<?>)o);
				else if (o == null && componentType.isPrimitive())
					o = componentType.getPrimitiveDefault();
				else
					o = convertToType(null, o, componentType);
			}
			try {
				Array.set(array, i++, o);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw e;
			}
		}
		return array;
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	private int hash(int h1, int h2) {
		return Integer.rotateLeft(h1, 1) + h2;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (o instanceof BeanContext) {
			BeanContext bcs = (BeanContext)o;
			if (hashCode != bcs.hashCode
					|| b != bcs.b
					|| beanFilters.length != bcs.beanFilters.length
					|| pojoFilters.length != bcs.pojoFilters.length
					|| notBeanClasses.length != bcs.notBeanClasses.length
					|| implKeyClasses.length != bcs.implKeyClasses.length
					|| implValueClasses.length != bcs.implValueClasses.length
					|| uriKeys.length != bcs.uriKeys.length
					|| uriVals.length != bcs.uriVals.length
					|| beanConstructorVisibility != bcs.beanConstructorVisibility
					|| beanClassVisibility != bcs.beanClassVisibility
					|| beanMethodVisibility != bcs.beanMethodVisibility
					|| beanFieldVisibility != bcs.beanFieldVisibility)
				return false;
			for (int i = 0; i < beanFilters.length; i++)
				if (! beanFilters[i].isSameAs(bcs.beanFilters[i]))
					return false;
			for (int i = 0; i < pojoFilters.length; i++)
				if (! pojoFilters[i].isSameAs(bcs.pojoFilters[i]))
					return false;
			for (int i = 0; i < notBeanClasses.length; i++)
				if (! notBeanClasses[i].equals(bcs.notBeanClasses[i]))
					return false;
			for (int i = 0; i < notBeanPackages.length; i++)
				if (! notBeanPackages[i].equals(bcs.notBeanPackages[i]))
					return false;
			for (int i = 0; i < implKeyClasses.length; i++)
				if (! implKeyClasses[i].equals(bcs.implKeyClasses[i]))
					return false;
			for (int i = 0; i < implValueClasses.length; i++)
				if (! implValueClasses[i].equals(bcs.implValueClasses[i]))
					return false;
			for (int i = 0; i < uriKeys.length; i++)
				if (! uriKeys[i].equals(bcs.uriKeys[i]))
					return false;
			for (int i = 0; i < uriVals.length; i++)
				if (! uriVals[i].equals(bcs.uriVals[i]))
					return false;
			return true;
		}
		return false;
	}

	@Override /* Object */
	public String toString() {
		ObjectMap m = new ObjectMap()
			.append("id", System.identityHashCode(this))
			.append("beansRequireDefaultConstructor", beansRequireDefaultConstructor)
			.append("beansRequireSerializable", beansRequireSerializable)
			.append("beansRequireSettersForGetters", beansRequireSettersForGetters)
			.append("beansRequireSomeProperties", beansRequireSomeProperties)
			.append("beanMapPutReturnsOldValue", beanMapPutReturnsOldValue)
			.append("useInterfaceProxies", useInterfaceProxies)
			.append("ignoreUnknownBeanProperties", ignoreUnknownBeanProperties)
			.append("ignoreUnknownNullBeanProperties", ignoreUnknownNullBeanProperties)
			.append("ignorePropertiesWithoutSetters", ignorePropertiesWithoutSetters)
			.append("ignoreInvocationExceptionsOnGetters", ignoreInvocationExceptionsOnGetters)
			.append("ignoreInvocationExceptionsOnSetters", ignoreInvocationExceptionsOnSetters)
			.append("useJavaBeanIntrospector", useJavaBeanIntrospector)
			.append("beanFilters", beanFilters)
			.append("pojoFilters", pojoFilters)
			.append("notBeanPackages", notBeanPackages)
			.append("notBeanClasses", notBeanClasses)
			.append("implClasses", implClasses)
			.append("uriVars", uriVars);
		try {
			return m.toString(JsonSerializer.DEFAULT_LAX_READABLE);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		}
	}
}
