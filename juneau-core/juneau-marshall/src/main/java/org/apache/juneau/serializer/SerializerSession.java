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
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.svl.*;
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
public abstract class SerializerSession extends BeanTraverseSession {

	private final Serializer ctx;
	private final UriResolver uriResolver;
	private VarResolverSession vrs;

	private final Method javaMethod;                                                // Java method that invoked this serializer.

	// Writable properties
	private final SerializerListener listener;

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
		super(ctx, args == null ? SerializerSessionArgs.DEFAULT : args);
		this.ctx = ctx;
		args = args == null ? SerializerSessionArgs.DEFAULT : args;
		this.javaMethod = args.javaMethod;
		this.uriResolver = new UriResolver(ctx.getUriResolution(), ctx.getUriRelativity(), getProperty(SERIALIZER_uriContext, UriContext.class, ctx.getUriContext()));
		this.listener = castOrCreate(SerializerListener.class, ctx.getListener());
		this.vrs = args.resolver;
	}

	/**
	 * Adds a session object to the {@link VarResolverSession} in this session.
	 *
	 * @param name The session object key.
	 * @param value The session object.
	 * @return This object (for method chaining).
	 */
	public SerializerSession varSessionObject(String name, Object value) {
		getVarResolver().sessionObject(name, value);
		return this;
	}

	/**
	 * Adds a session object to the {@link VarResolverSession} in this session.
	 *
	 * @return This object (for method chaining).
	 */
	protected VarResolverSession createDefaultVarResolverSession() {
		return VarResolver.DEFAULT.createSession();
	}

	/**
	 * Returns the variable resolver session.
	 *
	 * @return The variable resolver session.
	 */
	public VarResolverSession getVarResolver() {
		if (vrs == null)
			vrs = createDefaultVarResolverSession();
		return vrs;
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

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * This method should NOT close the context object.
	 *
	 * @param pipe Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException Problem occurred trying to serialize object.
	 */
	protected abstract void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException;

	/**
	 * Shortcut method for serializing objects directly to either a <c>String</c> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <c>String</c>.
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
	 * 	<br>Character-based serializers will return a <c>String</c>
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
	protected abstract SerializerPipe createPipe(Object output);

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serialize the specified object using the specified session.
	 *
	 * @param out Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final void serialize(Object o, Object out) throws SerializeException, IOException {
		try (SerializerPipe pipe = createPipe(out)) {
			doSerialize(pipe, o);
		} catch (SerializeException | IOException e) {
			throw e;
		} catch (StackOverflowError e) {
			throw new SerializeException(this,
				"Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.").initCause(e);
		} catch (Exception e) {
			throw new SerializeException(this, e);
		} finally {
			checkForWarnings();
		}
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
	@Override
	protected void onError(Throwable t, String msg, Object... args) {
		if (listener != null)
			listener.onError(this, t, format(msg, args));
		super.onError(t, msg, args);
	}

	/**
	 * Trims the specified string if {@link SerializerSession#isTrimStrings()} returns <jk>true</jk>.
	 *
	 * @param o The input string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String trim(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (isTrimStrings())
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
	public final boolean canIgnoreValue(ClassMeta<?> cm, String attrName, Object value) throws SerializeException {

		if (isTrimNullProperties() && value == null)
			return true;

		if (value == null)
			return false;

		if (cm == null)
			cm = object();

		if (isTrimEmptyCollections()) {
			if (cm.isArray() || (cm.isObject() && value.getClass().isArray())) {
				if (((Object[])value).length == 0)
					return true;
			}
			if (cm.isCollection() || (cm.isObject() && ClassInfo.of(value).isChildOf(Collection.class))) {
				if (((Collection<?>)value).isEmpty())
					return true;
			}
		}

		if (isTrimEmptyMaps()) {
			if (cm.isMap() || (cm.isObject() && ClassInfo.of(value).isChildOf(Map.class))) {
				if (((Map<?,?>)value).isEmpty())
					return true;
			}
		}

		try {
			if (isTrimNullProperties() && willRecurse(attrName, value, cm))
				return true;
		} catch (BeanRecursionException e) {
			throw new SerializeException(e);
		}

		return false;
	}

	/**
	 * Sorts the specified map if {@link SerializerSession#isSortMaps()} returns <jk>true</jk>.
	 *
	 * @param m The map being sorted.
	 * @return A new sorted {@link TreeMap}.
	 */
	public final <K,V> Map<K,V> sort(Map<K,V> m) {
		if (isSortMaps() && m != null && (! m.isEmpty()) && m.keySet().iterator().next() instanceof Comparable<?>)
			return new TreeMap<>(m);
		return m;
	}

	/**
	 * Sorts the specified collection if {@link SerializerSession#isSortCollections()} returns <jk>true</jk>.
	 *
	 * @param c The collection being sorted.
	 * @return A new sorted {@link TreeSet}.
	 */
	public final <E> Collection<E> sort(Collection<E> c) {
		if (isSortCollections() && c != null && (! c.isEmpty()) && c.iterator().next() instanceof Comparable<?>)
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
	 * Converts the URI to a value relative to the specified <c>relativeTo</c> parameter.
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
	 * Converts the specified object to a <c>String</c>.
	 *
	 * <p>
	 * Also has the following effects:
	 * <ul>
	 * 	<li><c>Class</c> object is converted to a readable name.  See {@link ClassInfo#getFullName()}.
	 * 	<li>Whitespace is trimmed if the trim-strings setting is enabled.
	 * </ul>
	 *
	 * @param o The object to convert to a <c>String</c>.
	 * @return The object converted to a String, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String toString(Object o) {
		if (o == null)
			return null;
		if (o.getClass() == Class.class)
			return ClassInfo.of((Class<?>)o).getFullName();
		if (o.getClass() == ClassInfo.class)
			return ((ClassInfo)o).getFullName();
		if (o.getClass().isEnum())
			return getClassMetaForObject(o).toString(o);
		String s = o.toString();
		if (isTrimStrings())
			s = s.trim();
		return s;
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
		if (isAddRootType())
			return object();
		ClassMeta<?> cm = getClassMetaForObject(o);
		if (cm != null && cm.isOptional())
			return cm.getElementType();
		return cm;
	}

	/**
	 * Optional method that specifies HTTP request headers for this serializer.
	 *
	 * <p>
	 * For example, {@link SoapXmlSerializer} needs to set a <c>SOAPAction</c> header.
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
	 * @param c The listener class to cast to.
	 * @return The listener associated with this session, or <jk>null</jk> if there is no listener.
	 */
	@SuppressWarnings("unchecked")
	public <T extends SerializerListener> T getListener(Class<T> c) {
		return (T)listener;
	}

	/**
	 * Resolves any variables in the specified string.
	 *
	 * @param string The string to resolve values in.
	 * @return The string with variables resolved.
	 */
	public String resolve(String string) {
		return getVarResolver().resolve(string);
	}

	/**
	 * Same as {@link #push(String, Object, ClassMeta)} but wraps {@link BeanRecursionException} inside {@link SerializeException}.
	 *
	 * @param attrName The attribute name.
	 * @param o The current object being traversed.
	 * @param eType The expected class type.
	 * @return
	 * 	The {@link ClassMeta} of the object so that <c>instanceof</c> operations only need to be performed
	 * 	once (since they can be expensive).
	 * @throws SerializeException If recursion occurred.
	 */
	protected final ClassMeta<?> push2(String attrName, Object o, ClassMeta<?> eType) throws SerializeException {
		try {
			return super.push(attrName, o, eType);
		} catch (BeanRecursionException e) {
			throw new SerializeException(e);
		}
	}

	/**
	 * Invokes the specified swap on the specified object if the swap is not null.
	 *
	 * @param swap The swap to invoke.  Can be <jk>null</jk>.
	 * @param o The input object.
	 * @return The swapped object.
	 * @throws SerializeException If swap method threw an exception.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object swap(PojoSwap swap, Object o) throws SerializeException {
		try {
			if (swap == null)
				return o;
			return swap.swap(this, o);
		} catch (Exception e) {
			throw new SerializeException(e);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * @see Serializer#SERIALIZER_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	protected boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * @see Serializer#SERIALIZER_addRootType
	 * @return
	 * 	<jk>true</jk> if type property should be added to root node.
	 */
	protected final boolean isAddRootType() {
		return ctx.isAddRootType();
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
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * @see Serializer#SERIALIZER_sortCollections
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() {
		return ctx.isSortCollections();
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * @see Serializer#SERIALIZER_sortMaps
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() {
		return ctx.isSortMaps();
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * @see Serializer#SERIALIZER_trimEmptyCollections
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() {
		return ctx.isTrimEmptyCollections();
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * @see Serializer#SERIALIZER_trimEmptyMaps
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() {
		return ctx.isTrimEmptyMaps();
	}

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * @see Serializer#SERIALIZER_trimNullProperties
	 * @return
	 * 	<jk>true</jk> if null bean values are not serialized to the output.
	 */
	protected final boolean isTrimNullProperties() {
		return ctx.isTrimNullProperties();
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * @see Serializer#SERIALIZER_trimStrings
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected boolean isTrimStrings() {
		return ctx.isTrimStrings();
	}

	/**
	 * Configuration property:  URI context bean.
	 *
	 * @see Serializer#SERIALIZER_uriContext
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() {
		return ctx.getUriContext();
	}

	/**
	 * Configuration property:  URI relativity.
	 *
	 * @see Serializer#SERIALIZER_uriRelativity
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() {
		return ctx.getUriRelativity();
	}

	/**
	 * Configuration property:  URI resolution.
	 *
	 * @see Serializer#SERIALIZER_uriResolution
	 * @return
	 * 	Defines the resolution level for URIs when serializing URIs.
	 */
	protected final UriResolution getUriResolution() {
		return ctx.getUriResolution();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("SerializerSession", new DefaultFilteringOMap()
				.a("uriResolver", uriResolver)
			);
	}
}
