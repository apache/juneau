/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import static com.ibm.juno.core.BeanContextProperties.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.ClassUtils.ClassComparator;

/**
 * Factory class for creating instances of {@link BeanContext}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class BeanContextFactory extends Lockable {

	//--------------------------------------------------------------------------------
	// Static constants
	//--------------------------------------------------------------------------------

	/**
	 * The default package pattern exclusion list.
	 * Any beans in packages in this list will not be considered beans.
	 */
	private static final String DEFAULT_NOTBEAN_PACKAGES =
		"java.lang,java.lang.annotation,java.lang.ref,java.lang.reflect,java.io,java.net,java.nio.*,java.util.*";

	/**
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

	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	boolean
		beansRequireDefaultConstructor = false,
		beansRequireSerializable = false,
		beansRequireSettersForGetters = false,
		beansRequireSomeProperties = true,
		beanMapPutReturnsOldValue = false,
		useInterfaceProxies = true,
		ignoreUnknownBeanProperties = false,
		ignoreUnknownNullBeanProperties = true,
		ignorePropertiesWithoutSetters = true,
		ignoreInvocationExceptionsOnGetters = false,
		ignoreInvocationExceptionsOnSetters = false,
		useJavaBeanIntrospector = false;

	Set<String> notBeanPackages = new TreeSet<String>();
	Set<Class<?>> notBeanClasses = newClassTreeSet();
	Map<Class<?>,Class<?>> implClasses = newClassTreeMap();
	Map<String,String> uriVars = new TreeMap<String,String>();
	LinkedList<Class<?>> filters = new LinkedList<Class<?>>();
	ClassLoader classLoader = null;

	Visibility
		beanConstructorVisibility = Visibility.PUBLIC,
		beanClassVisibility = Visibility.PUBLIC,
		beanFieldVisibility = Visibility.PUBLIC,
		beanMethodVisibility = Visibility.PUBLIC;

	// Optional default parser set by setDefaultParser().
	ReaderParser defaultParser = null;

	// Read-write lock for preventing acess to the getBeanContext() method while this factory is being modified.
	private ReadWriteLock lock = new ReentrantReadWriteLock();

	// Current BeanContext instance.
	private BeanContext beanContext;

	//--------------------------------------------------------------------------------
	// Methods
	//--------------------------------------------------------------------------------

	/**
	 * Default constructor.
	 */
	public BeanContextFactory() {
		addNotBeanClasses(DEFAULT_NOTBEAN_CLASSES);
		setProperty(BEAN_addNotBeanPackages, DEFAULT_NOTBEAN_PACKAGES);
	}

	/**
	 * Creates and returns a {@link BeanContext} with settings currently specified on this factory class.
	 * This method will return the same object until the factory settings are modified at which point
	 * a new {@link BeanContext} will be constructed.
	 *
	 * @return The bean context object.
	 */
	public BeanContext getBeanContext() {
		readLock();
		try {
			if (beanContext == null)
				beanContext = new BeanContext(this);
			return beanContext;
		} finally {
			readUnlock();
		}
	}

	/**
	 * Sets the default parser for this bean context.
	 * <p>
	 * The default parser is used in the following methods:
	 * <ul>
	 * 	<code>beanContext.newBeanMap(Bean.<jk>class</jk>).load(String)</code> - Used for parsing init properties.
	 * 	<li>{@link BeanContext#convertToType(Object, ClassMeta)} - Used for converting strings to beans.
	 * </ul>
	 *
	 * @param defaultParser The new default parser.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory setDefaultParser(ReaderParser defaultParser) {
		writeLock();
		try {
			this.defaultParser = defaultParser;
			return this;
		} finally {
			writeUnlock();
		}
	}


	//--------------------------------------------------------------------------------
	// Configuration property methods
	//--------------------------------------------------------------------------------

	/**
	 * Sets a property on this context.
	 * <p>
	 * 	Refer to {@link BeanContextProperties} for a description of available properties.
	 *
	 * @param property The property whose value is getting changed.
	 * @param value The new value.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory setProperty(String property, Object value) throws LockedException {
		writeLock();
		try {
			// Note:  Have to use the default bean context to set these properties since calling
			// convertToType will cause the cache object to be initialized.
			BeanContext bc = BeanContext.DEFAULT;

			if (property.equals(BEAN_beansRequireDefaultConstructor))
				beansRequireDefaultConstructor = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_beansRequireSerializable))
				beansRequireSerializable = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_beansRequireSettersForGetters))
				beansRequireSettersForGetters = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_beansRequireSomeProperties))
				beansRequireSomeProperties = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_beanMapPutReturnsOldValue))
				beanMapPutReturnsOldValue = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_beanConstructorVisibility))
				beanConstructorVisibility = Visibility.valueOf(value.toString());
			else if (property.equals(BEAN_beanClassVisibility))
				beanClassVisibility = Visibility.valueOf(value.toString());
			else if (property.equals(BEAN_beanFieldVisibility))
				beanFieldVisibility = Visibility.valueOf(value.toString());
			else if (property.equals(BEAN_methodVisibility))
				beanMethodVisibility = Visibility.valueOf(value.toString());
			else if (property.equals(BEAN_useJavaBeanIntrospector))
				useJavaBeanIntrospector = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_useInterfaceProxies))
				useInterfaceProxies = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_ignoreUnknownBeanProperties))
				ignoreUnknownBeanProperties = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_ignoreUnknownNullBeanProperties))
				ignoreUnknownNullBeanProperties = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_ignorePropertiesWithoutSetters))
				ignorePropertiesWithoutSetters = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_ignoreInvocationExceptionsOnGetters))
				ignoreInvocationExceptionsOnGetters = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_ignoreInvocationExceptionsOnSetters))
				ignoreInvocationExceptionsOnSetters = bc.convertToType(value, Boolean.class);
			else if (property.equals(BEAN_addNotBeanPackages)) {
				Set<String> set = new TreeSet<String>(notBeanPackages);
				for (String s : value.toString().split(","))
					set.add(s.trim());
				notBeanPackages = set;
			} else if (property.equals(BEAN_removeNotBeanPackages)) {
				Set<String> set = new TreeSet<String>(notBeanPackages);
				for (String s : value.toString().split(","))
					set.remove(s.trim());
				notBeanPackages = set;
			}
		} finally {
			writeUnlock();
		}
		return this;
	}

	/**
	 * Sets multiple properties on this context.
	 * <p>
	 * 	Refer to {@link BeanContextProperties} for a description of available properties.
	 *
	 * @param properties The properties to set.  Ignored if <jk>null</jk>.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory setProperties(ObjectMap properties) throws LockedException {
		writeLock();
		try {
			if (properties != null)
				for (Map.Entry<String,Object> e : properties.entrySet())
					setProperty(e.getKey(), e.getValue());
			return this;
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Adds an explicit list of Java classes to be excluded from consideration as being beans.
	 *
	 * @param classes One or more fully-qualified Java class names.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory addNotBeanClasses(Class<?>...classes) {
		writeLock();
		try {
			this.notBeanClasses.addAll(Arrays.asList(classes));
			return this;
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Add filters to this context.
	 * <p>
	 * 	There are two category of classes that can be passed in through this method:
	 * <ul>
	 * 	<li>Subclasses of {@link PojoFilter} and {@link BeanFilter}.
	 * 	<li>Any other class.
	 * </ul>
	 * <p>
	 * 	When <code>IFilter</code> classes are specified, they identify objects that need to be
	 * 		transformed into some other type during serialization (and optionally the reverse during parsing).
	 * <p>
	 * 	When non-<code>IFilter</code> classes are specified, they are wrapped inside {@link BeanFilter BeanFilters}.
	 * 	For example, if you have an interface <code>IFoo</code> and a subclass <code>Foo</code>, and you
	 * 		only want properties defined on <code>IFoo</code> to be visible as bean properties for <code>Foo</code> objects,
	 * 		you can simply pass in <code>IFoo.<jk>class</jk></code> to this method.
	 * <p>
	 * 	The following code shows the order in which filters are applied:
	 * <p class='bcode'>
	 * 	<jc>// F3,F4,F1,F2</jc>
	 * 	beanContext.addFilters(F1.<jk>class</jk>,F2.<jk>class</jk>);
	 * 	beanContext.addFilters(F3.<jk>class</jk>,F4.<jk>class</jk>);
	 * </p>
	 *
	 * @param classes One or more classes to add as filters to this context.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory addFilters(Class<?>...classes) throws LockedException {
		writeLock();
		try {
			classes = Arrays.copyOf(classes, classes.length); // Copy array to prevent modification!
			Collections.reverse(Arrays.asList(classes));
			for (Class<?> c : classes)
				filters.addFirst(c);
			return this;
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Specifies an implementation class for an interface or abstract class.
	 * <p>
	 * 	For interfaces and abstract classes this method can be used to specify an implementation
	 * 	class for the interface/abstract class so that instances of the implementation
	 * 	class are used when instantiated (e.g. during a parse).
	 *
	 * @param <T> The interface class.
	 * @param interfaceClass The interface class.
	 * @param implClass The implementation of the interface class.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public <T> BeanContextFactory addImplClass(Class<T> interfaceClass, Class<? extends T> implClass) throws LockedException {
		writeLock();
		try {
			this.implClasses.put(interfaceClass, implClass);
			return this;
		} finally {
			writeUnlock();
		}
	}

	/**
	 * Specifies the classloader to use when resolving classes, usually <js>"_class"</js> attributes.
	 * <p>
	 * 	Can be used for resolving class names when the classes being created are in a different
	 * 	classloader from the Juno code.
	 * <p>
	 * 	If <jk>null</jk>, <code>Class.forName(String)</code> will be used to resolve classes.
	 *
	 * @param classLoader The new classloader.
	 * @throws LockedException If {@link #lock()} was called on this object.
	 * @return This object (for method chaining).
	 */
	public BeanContextFactory setClassLoader(ClassLoader classLoader) throws LockedException {
		writeLock();
		try {
			this.classLoader = classLoader;
			return this;
		} finally {
			writeUnlock();
		}
	}

	//--------------------------------------------------------------------------------
	// Overridden methods on Lockable
	//--------------------------------------------------------------------------------

	@Override /* Lockable */
	public BeanContextFactory lock() {
		if (! isLocked()) {
			writeLock();
			super.lock();
			try {
				notBeanPackages = Collections.unmodifiableSet(notBeanPackages);
				notBeanClasses = Collections.unmodifiableSet(notBeanClasses);
				implClasses = Collections.unmodifiableMap(implClasses);
				uriVars = Collections.unmodifiableMap(uriVars);
			} finally {
				writeUnlock();
			}
		}
		return this;
	}

	private void writeLock() {
		checkLock();
		lock.writeLock().lock();
		beanContext = null;
	}

	private void writeUnlock() {
		lock.writeLock().unlock();
	}

	private void readLock() {
		lock.readLock().lock();
	}

	private void readUnlock() {
		lock.readLock().unlock();
	}

	@Override /* Lockable */
	public synchronized BeanContextFactory clone() {
		try {
			return (BeanContextFactory)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}

	@Override /* Lockable */
	public void onUnclone() {
		readLock();
		try {
			notBeanPackages = new LinkedHashSet<String>(notBeanPackages);
			filters = new LinkedList<Class<?>>(filters);
			notBeanClasses = newClassTreeSet(notBeanClasses);
			implClasses = newClassTreeMap(implClasses);
			uriVars = new TreeMap<String,String>(uriVars);
			beanContext = null;
		} finally {
			readUnlock();
		}
	}

	@Override /* Object */
	public String toString() {
		readLock();
		try {
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
				.append("filters", filters)
				.append("notBeanPackages", notBeanPackages)
				.append("notBeanClasses", notBeanClasses)
				.append("implClasses", implClasses)
				.append("uriVars", uriVars);
			return m.toString(JsonSerializer.DEFAULT_LAX_READABLE);
		} catch (SerializeException e) {
			return e.getLocalizedMessage();
		} finally {
			readUnlock();
		}
	}

	private TreeMap<Class<?>,Class<?>> newClassTreeMap(Map<Class<?>,Class<?>> m) {
		TreeMap<Class<?>,Class<?>> tm = newClassTreeMap();
		tm.putAll(m);
		return tm;
	}

	private TreeMap<Class<?>,Class<?>> newClassTreeMap() {
		return new TreeMap<Class<?>,Class<?>>(new ClassComparator());
	}

	private TreeSet<Class<?>> newClassTreeSet(Set<Class<?>> s) {
		TreeSet<Class<?>> ts = newClassTreeSet();
		ts.addAll(s);
		return ts;
	}

	private TreeSet<Class<?>> newClassTreeSet() {
		return new TreeSet<Class<?>>(new ClassComparator());
	}
}
