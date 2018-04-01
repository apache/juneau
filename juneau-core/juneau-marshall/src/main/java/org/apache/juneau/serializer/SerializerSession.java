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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.serializer.Serializer.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.transform.*;

/**
 * Serializer session that lives for the duration of a single use of {@link Serializer}.
 * 
 * <p>
 * Used by serializers for the following purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Keeping track of how deep it is in a model for indentation purposes.
 * 	<li>
 * 		Ensuring infinite loops don't occur by setting a limit on how deep to traverse a model.
 * 	<li>
 * 		Ensuring infinite loops don't occur from loops in the model (when detectRecursions is enabled.
 * 	<li>
 * 		Allowing serializer properties to be overridden on method calls.
 * </ul>
 * 
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public abstract class SerializerSession extends BeanSession {

	private final int maxDepth, initialDepth;
	private final boolean
		detectRecursions,
		ignoreRecursions,
		addBeanTypes,
		trimNulls,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		addRootType;
	private final UriResolver uriResolver;

	private final Map<Object,Object> set;                                           // Contains the current objects in the current branch of the model.
	private final LinkedList<StackElement> stack = new LinkedList<>();              // Contains the current objects in the current branch of the model.
	private final Method javaMethod;                                                // Java method that invoked this serializer.

	// Writable properties
	private boolean isBottom;                                                       // If 'true', then we're at a leaf in the model (i.e. a String, Number, Boolean, or null).
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;
	private final SerializerListener listener;

	/** The current indentation depth into the model. */
	public int indent;


	/**
	 * Create a new session using properties specified in the context.
	 * 
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * 	Can be <jk>null</jk>.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected SerializerSession(Serializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
		this.javaMethod = args.javaMethod;
		UriResolution uriResolution;
		UriRelativity uriRelativity;
		Class<?> listenerClass;
		
		maxDepth = getProperty(SERIALIZER_maxDepth, int.class, ctx.maxDepth);
		initialDepth = getProperty(SERIALIZER_initialDepth, int.class, ctx.initialDepth);
		detectRecursions = getProperty(SERIALIZER_detectRecursions, boolean.class, ctx.detectRecursions);
		ignoreRecursions = getProperty(SERIALIZER_ignoreRecursions, boolean.class, ctx.ignoreRecursions);
		addBeanTypes = getProperty(SERIALIZER_addBeanTypes, boolean.class, ctx.addBeanTypes);
		trimNulls = getProperty(SERIALIZER_trimNullProperties, boolean.class, ctx.trimNulls);
		trimEmptyCollections = getProperty(SERIALIZER_trimEmptyCollections, boolean.class, ctx.trimEmptyCollections);
		trimEmptyMaps = getProperty(SERIALIZER_trimEmptyMaps, boolean.class, ctx.trimEmptyMaps);
		trimStrings = getProperty(SERIALIZER_trimStrings, boolean.class, ctx.trimStrings);
		sortCollections = getProperty(SERIALIZER_sortCollections, boolean.class, ctx.sortMaps);
		sortMaps = getProperty(SERIALIZER_sortMaps, boolean.class, ctx.sortMaps);
		addRootType = getProperty(SERIALIZER_addRootType, boolean.class, ctx.addRootType);
		uriResolution = getInstanceProperty(SERIALIZER_uriResolution, UriResolution.class, ctx.uriResolution);
		uriRelativity = getInstanceProperty(SERIALIZER_uriRelativity, UriRelativity.class, ctx.uriRelativity);
		listenerClass = getProperty(SERIALIZER_listener, Class.class, ctx.listener);

		uriResolver = new UriResolver(uriResolution, uriRelativity, args.uriContext == null ? ctx.uriContext : args.uriContext);

		listener = newInstance(SerializerListener.class, listenerClass);

		this.indent = initialDepth;
		if (detectRecursions || isDebug()) {
			set = new IdentityHashMap<>();
		} else {
			set = Collections.emptyMap();
		}
	}

	/**
	 * Default constructor.
	 * 
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected SerializerSession(SerializerSessionArgs args) {
		this(Serializer.DEFAULT, args);
	}
	
	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("SerializerSession", new ObjectMap()
				.append("maxDepth", maxDepth)
				.append("initialDepth", initialDepth)
				.append("detectRecursions", detectRecursions)
				.append("ignoreRecursions", ignoreRecursions)
				.append("addBeanTypes", addBeanTypes)
				.append("trimNulls", trimNulls)
				.append("trimEmptyCollections", trimEmptyCollections)
				.append("trimEmptyMaps", trimEmptyMaps)
				.append("trimStrings", trimStrings)
				.append("sortCollections", sortCollections)
				.append("sortMaps", sortMaps)
				.append("addRootType", addRootType)
				.append("uriResolver", uriResolver)
			);
	}

	/**
	 * Wraps the specified input object into a {@link ParserPipe} object so that it can be easily converted into
	 * a stream or reader.
	 * 
	 * @param output
	 * 	The output location.
	 * 	<br>For character-based serializers, this can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 		<li>{@link StringBuilder}
	 * 	</ul>
	 * 	<br>For byte-based serializers, this can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @return
	 * 	A new {@link ParserPipe} wrapper around the specified input object.
	 */
	protected SerializerPipe createPipe(Object output) {
		return new SerializerPipe(output);
	}


	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 * 
	 * <p>
	 * This method should NOT close the context object.
	 * 
	 * @param pipe Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws Exception If thrown from underlying stream, or if the input contains a syntax error or is malformed.
	 */
	protected abstract void doSerialize(SerializerPipe pipe, Object o) throws Exception;

	/**
	 * Shortcut method for serializing objects directly to either a <code>String</code> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 * 
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <code>String</code>.
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public abstract Object serialize(Object o) throws SerializeException;

	/**
	 * Shortcut method for serializing an object to a String.
	 * 
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <code>String</code>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code> converted to a string based on the {@link OutputStreamSerializer#OSSERIALIZER_binaryFormat} setting.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public abstract String serializeToString(Object o) throws SerializeException;

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 * 
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public abstract boolean isWriterSerializer();


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Serialize the specified object using the specified session.
	 * 
	 * @param out Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final void serialize(Object o, Object out) throws SerializeException {
		try (SerializerPipe pipe = createPipe(out)) {
			doSerialize(pipe, o);
		} catch (SerializeException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new SerializeException(this,
				"Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the Serializer.SERIALIZER_detectRecursions setting to help locate the loop.").initCause(e);
		} catch (Exception e) {
			throw new SerializeException(this, e);
		} finally {
			checkForWarnings();
		}
	}

	/**
	 * Sets the current bean property being serialized for proper error messages.
	 * 
	 * @param currentProperty The current property being serialized.
	 */
	protected final void setCurrentProperty(BeanPropertyMeta currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being serialized for proper error messages.
	 * 
	 * @param currentClass The current class being serialized.
	 */
	protected final void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Returns the Java method that invoked this serializer.
	 * 
	 * <p>
	 * When using the REST API, this is the Java method invoked by the REST call.
	 * Can be used to access annotations defined on the method or class.
	 * 
	 * @return The Java method that invoked this serializer.
	*/
	protected final Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the URI resolver.
	 * 
	 * @return The URI resolver.
	 */
	protected final UriResolver getUriResolver() {
		return uriResolver;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_maxDepth} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_maxDepth} setting value for this session.
	 */
	protected final int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_initialDepth} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_initialDepth} setting value for this session.
	 */
	protected final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_detectRecursions} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_detectRecursions} setting value for this session.
	 */
	protected final boolean isDetectRecursions() {
		return detectRecursions;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_ignoreRecursions} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_ignoreRecursions} setting value for this session.
	 */
	protected final boolean isIgnoreRecursions() {
		return ignoreRecursions;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_addBeanTypes} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_addBeanTypes} setting value for this session.
	 */
	protected boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_addRootType} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_addRootType} setting value for this session.
	 */
	protected boolean isAddRootType() {
		return addRootType;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_trimNullProperties} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_trimNullProperties} setting value for this session.
	 */
	protected final boolean isTrimNulls() {
		return trimNulls;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_trimEmptyCollections} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_trimEmptyCollections} setting value for this session.
	 */
	protected final boolean isTrimEmptyCollections() {
		return trimEmptyCollections;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_trimEmptyMaps} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_trimEmptyMaps} setting value for this session.
	 */
	protected final boolean isTrimEmptyMaps() {
		return trimEmptyMaps;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_trimStrings} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_trimStrings} setting value for this session.
	 */
	protected boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_sortCollections} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_sortCollections} setting value for this session.
	 */
	protected final boolean isSortCollections() {
		return sortCollections;
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_sortMaps} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_sortMaps} setting value for this session.
	 */
	protected final boolean isSortMaps() {
		return sortMaps;
	}

	/**
	 * Push the specified object onto the stack.
	 * 
	 * @param attrName The attribute name.
	 * @param o The current object being serialized.
	 * @param eType The expected class type.
	 * @return
	 * 	The {@link ClassMeta} of the object so that <code>instanceof</code> operations only need to be performed
	 * 	once (since they can be expensive).
	 * @throws SerializeException If recursion occurred.
	 */
	protected final ClassMeta<?> push(String attrName, Object o, ClassMeta<?> eType) throws SerializeException {
		indent++;
		isBottom = true;
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ClassMeta<?> cm = (eType != null && c == eType.getInnerClass()) ? eType : getClassMeta(c);
		if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
			return cm;
		if (detectRecursions || isDebug()) {
			if (stack.size() > maxDepth)
				return null;
			if (willRecurse(attrName, o, cm))
				return null;
			isBottom = false;
			stack.add(new StackElement(stack.size(), attrName, o, cm));
			if (isDebug())
				getLogger().info(getStack(false));
			set.put(o, o);
		}
		return cm;
	}

	/**
	 * Returns <jk>true</jk> if {@link Serializer#SERIALIZER_detectRecursions} is enabled, and the specified
	 * object is already higher up in the serialization chain.
	 * 
	 * @param attrName The bean property attribute name, or some other identifier.
	 * @param o The object to check for recursion.
	 * @param cm The metadata on the object class.
	 * @return <jk>true</jk> if recursion detected.
	 * @throws SerializeException If recursion occurred.
	 */
	protected final boolean willRecurse(String attrName, Object o, ClassMeta<?> cm) throws SerializeException {
		if (! (detectRecursions || isDebug()))
			return false;
		if (! set.containsKey(o))
			return false;
		if (ignoreRecursions && ! isDebug())
			return true;

		stack.add(new StackElement(stack.size(), attrName, o, cm));
		throw new SerializeException("Recursion occurred, stack={0}", getStack(true));
	}

	/**
	 * Pop an object off the stack.
	 */
	protected final void pop() {
		indent--;
		if ((detectRecursions || isDebug()) && ! isBottom)  {
			Object o = stack.removeLast().o;
			Object o2 = set.remove(o);
			if (o2 == null)
				onError(null, "Couldn't remove object of type ''{0}'' on attribute ''{1}'' from object stack.",
					o.getClass().getName(), stack);
		}
		isBottom = false;
	}

	/**
	 * Specialized warning when an exception is thrown while executing a bean getter.
	 * 
	 * @param p The bean map entry representing the bean property.
	 * @param t The throwable that the bean getter threw.
	 */
	protected final void onBeanGetterException(BeanPropertyMeta p, Throwable t) {
		if (listener != null)
			listener.onBeanGetterException(this, t, p);
		String prefix = (isDebug() ? getStack(false) + ": " : "");
		addWarning("{0}Could not call getValue() on property ''{1}'' of class ''{2}'', exception = {3}", prefix,
			p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage());
	}

	/**
	 * Logs a warning message.
	 * 
	 * @param t The throwable that was thrown (if there was one).
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected final void onError(Throwable t, String msg, Object... args) {
		if (listener != null)
			listener.onError(this, t, format(msg, args));
		super.addWarning(msg, args);
	}

	/**
	 * Trims the specified string if {@link SerializerSession#isTrimStrings()} returns <jk>true</jk>.
	 * 
	 * @param o The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	protected final String trim(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (trimStrings)
			s = s.trim();
		return s;
	}

	/**
	 * Generalize the specified object if a POJO swap is associated with it.
	 * 
	 * @param o The object to generalize.
	 * @param type The type of object.
	 * @return The generalized object, or <jk>null</jk> if the object is <jk>null</jk>.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final Object generalize(Object o, ClassMeta<?> type) throws SerializeException {
		try {
			if (o == null)
				return null;
			PojoSwap f = (type == null || type.isObject() ? getClassMeta(o.getClass()).getPojoSwap(this) : type.getPojoSwap(this));
			if (f == null)
				return o;
			return f.swap(this, o);
		} catch (SerializeException e) {
			throw e;
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified value should not be serialized.
	 * 
	 * @param cm The class type of the object being serialized.
	 * @param attrName The bean attribute name, or <jk>null</jk> if this isn't a bean attribute.
	 * @param value The object being serialized.
	 * @return <jk>true</jk> if the specified value should not be serialized.
	 * @throws SerializeException If recursion occurred.
	 */
	protected final boolean canIgnoreValue(ClassMeta<?> cm, String attrName, Object value) throws SerializeException {

		if (trimNulls && value == null)
			return true;

		if (value == null)
			return false;

		if (cm == null)
			cm = object();

		if (trimEmptyCollections) {
			if (cm.isArray() || (cm.isObject() && value.getClass().isArray())) {
				if (((Object[])value).length == 0)
					return true;
			}
			if (cm.isCollection() || (cm.isObject() && isParentClass(Collection.class, value.getClass()))) {
				if (((Collection<?>)value).isEmpty())
					return true;
			}
		}

		if (trimEmptyMaps) {
			if (cm.isMap() || (cm.isObject() && isParentClass(Map.class, value.getClass()))) {
				if (((Map<?,?>)value).isEmpty())
					return true;
			}
		}

		if (trimNulls && willRecurse(attrName, value, cm))
			return true;

		return false;
	}

	/**
	 * Sorts the specified map if {@link SerializerSession#isSortMaps()} returns <jk>true</jk>.
	 * 
	 * @param m The map being sorted.
	 * @return A new sorted {@link TreeMap}.
	 */
	protected final <K,V> Map<K,V> sort(Map<K,V> m) {
		if (sortMaps && m != null && (! m.isEmpty()) && m.keySet().iterator().next() instanceof Comparable<?>)
			return new TreeMap<>(m);
		return m;
	}

	/**
	 * Sorts the specified collection if {@link SerializerSession#isSortCollections()} returns <jk>true</jk>.
	 * 
	 * @param c The collection being sorted.
	 * @return A new sorted {@link TreeSet}.
	 */
	protected final <E> Collection<E> sort(Collection<E> c) {
		if (sortCollections && c != null && (! c.isEmpty()) && c.iterator().next() instanceof Comparable<?>)
			return new TreeSet<>(c);
		return c;
	}

	/**
	 * Converts the contents of the specified object array to a list.
	 * 
	 * <p>
	 * Works on both object and primitive arrays.
	 * 
	 * <p>
	 * In the case of multi-dimensional arrays, the outgoing list will contain elements of type n-1 dimension.
	 * i.e. if {@code type} is <code><jk>int</jk>[][]</code> then {@code list} will have entries of type
	 * <code><jk>int</jk>[]</code>.
	 * 
	 * @param type The type of array.
	 * @param array The array being converted.
	 * @return The array as a list.
	 */
	protected static final List<Object> toList(Class<?> type, Object array) {
		Class<?> componentType = type.getComponentType();
		if (componentType.isPrimitive()) {
			int l = Array.getLength(array);
			List<Object> list = new ArrayList<>(l);
			for (int i = 0; i < l; i++)
				list.add(Array.get(array, i));
			return list;
		}
		return Arrays.asList((Object[])array);
	}

	/**
	 * Converts a String to an absolute URI based on the {@link UriContext} on this session.
	 * 
	 * @param uri
	 * 	The input URI.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link java.net.URI}
	 * 		<li>{@link java.net.URL}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	URI can be any of the following forms:
	 * 	<ul>
	 * 		<li><js>"foo://foo"</js> - Absolute URI.
	 * 		<li><js>"/foo"</js> - Root-relative URI.
	 * 		<li><js>"/"</js> - Root URI.
	 * 		<li><js>"context:/foo"</js> - Context-root-relative URI.
	 * 		<li><js>"context:/"</js> - Context-root URI.
	 * 		<li><js>"servlet:/foo"</js> - Servlet-path-relative URI.
	 * 		<li><js>"servlet:/"</js> - Servlet-path URI.
	 * 		<li><js>"request:/foo"</js> - Request-path-relative URI.
	 * 		<li><js>"request:/"</js> - Request-path URI.
	 * 		<li><js>"foo"</js> - Path-info-relative URI.
	 * 		<li><js>""</js> - Path-info URI.
	 * 	</ul>
	 * @return The resolved URI.
	 */
	public final String resolveUri(Object uri) {
		return uriResolver.resolve(uri);
	}

	/**
	 * Opposite of {@link #resolveUri(Object)}.
	 * 
	 * <p>
	 * Converts the URI to a value relative to the specified <code>relativeTo</code> parameter.
	 * 
	 * <p>
	 * Both parameters can be any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>{@link CharSequence}
	 * </ul>
	 * 
	 * <p>
	 * Both URIs can be any of the following forms:
	 * <ul>
	 * 	<li><js>"foo://foo"</js> - Absolute URI.
	 * 	<li><js>"/foo"</js> - Root-relative URI.
	 * 	<li><js>"/"</js> - Root URI.
	 * 	<li><js>"context:/foo"</js> - Context-root-relative URI.
	 * 	<li><js>"context:/"</js> - Context-root URI.
	 * 	<li><js>"servlet:/foo"</js> - Servlet-path-relative URI.
	 * 	<li><js>"servlet:/"</js> - Servlet-path URI.
	 * 	<li><js>"request:/foo"</js> - Request-path-relative URI.
	 * 	<li><js>"request:/"</js> - Request-path URI.
	 * 	<li><js>"foo"</js> - Path-info-relative URI.
	 * 	<li><js>""</js> - Path-info URI.
	 * </ul>
	 * 
	 * @param relativeTo The URI to relativize against.
	 * @param uri The URI to relativize.
	 * @return The relativized URI.
	 */
	protected final String relativizeUri(Object relativeTo, Object uri) {
		return uriResolver.relativize(relativeTo, uri);
	}

	/**
	 * Converts the specified object to a <code>String</code>.
	 * 
	 * <p>
	 * Also has the following effects:
	 * <ul>
	 * 	<li><code>Class</code> object is converted to a readable name.  See {@link ClassUtils#getReadableClassName(Class)}.
	 * 	<li>Whitespace is trimmed if the trim-strings setting is enabled.
	 * </ul>
	 * 
	 * @param o The object to convert to a <code>String</code>.
	 * @return The object converted to a String, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String toString(Object o) {
		if (o == null)
			return null;
		if (o.getClass() == Class.class)
			return getReadableClassName((Class<?>)o);
		if (o.getClass().isEnum()) 
			return getClassMetaForObject(o).toString(o);
		String s = o.toString();
		if (trimStrings)
			s = s.trim();
		return s;
	}

	private static final class StackElement {
		final int depth;
		final String name;
		final Object o;
		final ClassMeta<?> aType;

		StackElement(int depth, String name, Object o, ClassMeta<?> aType) {
			this.depth = depth;
			this.name = name;
			this.o = o;
			this.aType = aType;
		}

		String toString(boolean simple) {
			StringBuilder sb = new StringBuilder().append('[').append(depth).append(']');
			sb.append(isEmpty(name) ? "<noname>" : name).append(':');
			sb.append(aType.toString(simple));
			if (aType != aType.getSerializedClassMeta(null))
				sb.append('/').append(aType.getSerializedClassMeta(null).toString(simple));
			return sb.toString();
		}
	}

	private String getStack(boolean full) {
		StringBuilder sb = new StringBuilder();
		for (StackElement e : stack) {
			if (full) {
				sb.append("\n\t");
				for (int i = 1; i < e.depth; i++)
					sb.append("  ");
				if (e.depth > 0)
					sb.append("->");
				sb.append(e.toString(false));
			} else {
				sb.append(" > ").append(e.toString(true));
			}
		}
		return sb.toString();
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 * 
	 * @return A map, typically containing something like <code>{line:123,column:456,currentProperty:"foobar"}</code>
	 */
	protected final ObjectMap getLastLocation() {
		ObjectMap m = new ObjectMap();
		if (currentClass != null)
			m.put("currentClass", currentClass);
		if (currentProperty != null)
			m.put("currentProperty", currentProperty);
		if (stack != null && ! stack.isEmpty())
			m.put("stack", stack);
		return m;
	}

	/**
	 * Create a "_type" property that contains the dictionary name of the bean.
	 * 
	 * @param m The bean map to create a class property on.
	 * @param typeName The type name of the bean.
	 * @return A new bean property value.
	 */
	protected static final BeanPropertyValue createBeanTypeNameProperty(BeanMap<?> m, String typeName) {
		BeanMeta<?> bm = m.getMeta();
		return new BeanPropertyValue(bm.getTypeProperty(), bm.getTypeProperty().getName(), typeName, null);
	}

	/**
	 * Resolves the dictionary name for the actual type.
	 * 
	 * @param eType The expected type of the bean property.
	 * @param aType The actual type of the bean property.
	 * @param pMeta The current bean property being serialized.
	 * @return The bean dictionary name, or <jk>null</jk> if a name could not be found.
	 */
	protected final String getBeanTypeName(ClassMeta<?> eType, ClassMeta<?> aType, BeanPropertyMeta pMeta) {
		if (eType == aType)
			return null;

		if (! isAddBeanTypes())
			return null;

		String eTypeTn = eType.getDictionaryName();

		// First see if it's defined on the actual type.
		String tn = aType.getDictionaryName();
		if (tn != null && ! tn.equals(eTypeTn)) {
			return tn;
		}

		// Then see if it's defined on the expected type.
		// The expected type might be an interface with mappings for implementation classes.
		BeanRegistry br = eType.getBeanRegistry();
		if (br != null) {
			tn = br.getTypeName(aType);
			if (tn != null && ! tn.equals(eTypeTn))
				return tn;
		}

		// Then look on the bean property.
		br = pMeta == null ? null : pMeta.getBeanRegistry();
		if (br != null) {
			tn = br.getTypeName(aType);
			if (tn != null && ! tn.equals(eTypeTn))
				return tn;
		}

		// Finally look in the session.
		br = getBeanRegistry();
		if (br != null) {
			tn = br.getTypeName(aType);
			if (tn != null && ! tn.equals(eTypeTn))
				return tn;
		}

		return null;
	}

	/**
	 * Returns the parser-side expected type for the object.
	 * 
	 * <p>
	 * The return value depends on the {@link Serializer#SERIALIZER_addRootType} setting.
	 * When disabled, the parser already knows the Java POJO type being parsed, so there is
	 * no reason to add <js>"_type"</js> attributes to the root-level object.
	 * 
	 * @param o The object to get the expected type on.
	 * @return The expected type.
	 */
	protected final ClassMeta<?> getExpectedRootType(Object o) {
		return addRootType ? object() : getClassMetaForObject(o);
	}

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 * 
	 * <p>
	 * For example, {@link SoapXmlSerializer} needs to set a <code>SOAPAction</code> header.
	 * 
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 * 
	 * @return
	 * 	The HTTP headers to set on HTTP requests.
	 * 	Never <jk>null</jk>.
	 */
	public Map<String,String> getResponseHeaders() {
		return Collections.emptyMap();
	}
	
	
	/**
	 * Returns the listener associated with this session.
	 * 
	 * @return The listener associated with this session, or <jk>null</jk> if there is no listener.
	 */
	public SerializerListener getListener() {
		return listener;
	}

	/**
	 * Returns the listener associated with this session.
	 * 
	 * @param c The listener class to cast to. 
	 * @return The listener associated with this session, or <jk>null</jk> if there is no listener.
	 */
	@SuppressWarnings("unchecked")
	public <T extends SerializerListener> T getListener(Class<T> c) {
		return (T)listener;
	}
}
