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

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.swap.*;

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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public class SerializerSession extends BeanTraverseSession {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @param ctx The context creating this session.
	 * @return A new builder.
	 */
	public static Builder create(Serializer ctx) {
		return new Builder(ctx);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanTraverseSession.Builder {

		Serializer ctx;
		Method javaMethod;
		VarResolverSession resolver;
		UriContext uriContext;
		HttpPartSchema schema;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Serializer ctx) {
			super(ctx);
			this.ctx = ctx;
			uriContext = ctx.uriContext;
			mediaTypeDefault(ctx.getResponseContentType());
		}

		@Override
		public SerializerSession build() {
			return new SerializerSession(this);
		}

		/**
		 * The java method that called this serializer, usually the method in a REST servlet.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder javaMethod(Method value) {
			if (value != null)
				javaMethod = value;
			return this;
		}

		/**
		 * String variable resolver.
		 *
		 * <p>
		 * If not specified, defaults to session created by {@link VarResolver#DEFAULT}.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder resolver(VarResolverSession value) {
			if (value != null)
				resolver = value;
			return this;
		}

		/**
		 * URI context bean.
		 *
		 * <p>
		 * Bean used for resolution of URIs to absolute or root-relative form.
		 *
		 * <p>
		 * If not specified, defaults to {@link Serializer.Builder#uriContext(UriContext)}.
		 *
		 * @param value
		 * 	The new property value.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder uriContext(UriContext value) {
			if (value != null)
				uriContext = value;
			return this;
		}

		/**
		 * HTTP-part schema.
		 *
		 * <p>
		 * Used for schema-based serializers and parsers to define additional formatting.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder schema(HttpPartSchema value) {
			if (value != null)
				this.schema = value;
			return this;
		}

		/**
		 * Same as {@link #schema(HttpPartSchema)} but doesn't overwrite the value if it is already set.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
		 * @return This object.
		 */
		@FluentSetter
		public Builder schemaDefault(HttpPartSchema value) {
			if (value != null && schema == null)
				this.schema = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			super.apply(type, apply);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder debug(Boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder properties(Map<String,Object> value) {
			super.properties(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder property(String key, Object value) {
			super.property(key, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.ContextSession.Builder */
		public Builder unmodifiable() {
			super.unmodifiable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder localeDefault(Locale value) {
			super.localeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder mediaTypeDefault(MediaType value) {
			super.mediaTypeDefault(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanSession.Builder */
		public Builder timeZoneDefault(TimeZone value) {
			super.timeZoneDefault(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Serializer ctx;
	private final UriResolver uriResolver;
	private final HttpPartSchema schema;
	private VarResolverSession vrs;

	private final Method javaMethod;                                                // Java method that invoked this serializer.

	// Writable properties
	private final SerializerListener listener;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected SerializerSession(Builder builder) {
		super(builder);
		ctx = builder.ctx;
		javaMethod = builder.javaMethod;
		UriContext uriContext = builder.uriContext;
		uriResolver = UriResolver.of(ctx.getUriResolution(), ctx.getUriRelativity(), uriContext);
		listener = BeanCreator.of(SerializerListener.class).type(ctx.getListener()).orElse(null);
		vrs = builder.resolver;
		schema = builder.schema;
	}

	/**
	 * Adds a session object to the {@link VarResolverSession} in this session.
	 *
	 * @param <T> The bean type.
	 * @param c The bean type being added.
	 * @param value The bean being added.
	 * @return This object.
	 */
	public <T> SerializerSession addVarBean(Class<T> c, T value) {
		getVarResolver().bean(c, value);
		return this;
	}

	/**
	 * Adds a session object to the {@link VarResolverSession} in this session.
	 *
	 * @return This object.
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
	 * HTTP part schema of object being serialized.
	 *
	 * @return HTTP part schema of object being serialized, or <jk>null</jk> if not specified.
	 */
	public final HttpPartSchema getSchema() {
		return schema;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Serializes a POJO to the specified pipe.
	 *
	 * <p>
	 * This method should NOT close the context object.
	 *
	 * <p>
	 * The default implementation of this method simply calls {@link Serializer#doSerialize(SerializerSession,SerializerPipe,Object)}.
	 *
	 * @param pipe Where to send the output from the serializer.
	 * @param o The object to serialize.
	 * @throws IOException Thrown by underlying stream.
	 * @throws SerializeException Problem occurred trying to serialize object.
	 */
	protected void doSerialize(SerializerPipe pipe, Object o) throws IOException, SerializeException {
		ctx.doSerialize(this, pipe, o);
	}

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
	public Object serialize(Object o) throws SerializeException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Shortcut method for serializing an object to a String.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <c>String</c>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code> converted to a string based on the {@link OutputStreamSerializer.Builder#binaryFormat(BinaryFormat)} setting.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public String serializeToString(Object o) throws SerializeException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public boolean isWriterSerializer() {
		return false;
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
				"Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.");
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
	 * @throws SerializeException Thrown if ignoreInvocationExceptionOnGetters is false.
	 */
	protected final void onBeanGetterException(BeanPropertyMeta p, Throwable t) throws SerializeException {
		if (listener != null)
			listener.onBeanGetterException(this, t, p);
		String prefix = (isDebug() ? getStack(false) + ": " : "");
		addWarning("{0}Could not call getValue() on property ''{1}'' of class ''{2}'', exception = {3}", prefix,
			p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage());
		if (! isIgnoreInvocationExceptionsOnGetters())
			throw new SerializeException(this, "{0}Could not call getValue() on property ''{1}'' of class ''{2}'', exception = {3}", prefix,
				p.getName(), p.getBeanMeta().getClassMeta(), t.getLocalizedMessage()).initCause(t);
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
			ObjectSwap f = (type == null || type.isObject() || type.isString() ? getClassMeta(o.getClass()).getSwap(this) : type.getSwap(this));
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

		if (value == null && ! isKeepNullProperties())
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
			if ((! isKeepNullProperties()) && (willRecurse(attrName, value, cm) || willExceedDepth()))
				return true;
		} catch (BeanRecursionException e) {
			throw new SerializeException(e);
		}

		return false;
	}

	/**
	 * Sorts the specified map if {@link SerializerSession#isSortMaps()} returns <jk>true</jk>.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map being sorted.
	 * @return A new sorted {@link TreeMap}.
	 */
	public final <K,V> Map<K,V> sort(Map<K,V> m) {
		if (m == null || m.isEmpty() || SortedMap.class.isInstance(m))
			return m;
		if (isSortMaps() && isSortable(m.keySet()))
			return new TreeMap<>(m);
		return m;
	}

	/**
	 * Consumes each map entry in the map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map being consumed.
	 * @param consumer The map entry consumer.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final <K,V> void forEachEntry(Map<K,V> m, Consumer<Map.Entry<K,V>> consumer) {
		if (m == null || m.isEmpty())
			return;
		if (isSortMaps() && ! SortedMap.class.isInstance(m) && isSortable(m.keySet()))
			((Map)m).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(x -> consumer.accept((Map.Entry<K,V>) x));
		else
			m.entrySet().forEach(consumer);
	}

	/**
	 * Sorts the specified collection if {@link SerializerSession#isSortCollections()} returns <jk>true</jk>.
	 *
	 * @param <E> The element type.
	 * @param c The collection being sorted.
	 * @return A new sorted {@link TreeSet}.
	 */
	public final <E> Collection<E> sort(Collection<E> c) {
		if (c == null || c.isEmpty() || SortedSet.class.isInstance(c))
			return c;
		if (isSortCollections() && isSortable(c))
			return c.stream().sorted().collect(Collectors.toList());
		return c;
	}

	/**
	 * Consumes each entry in the list.
	 *
	 * @param <E> The element type.
	 * @param c The collection being sorted.
	 * @param consumer The entry consumer.
	 */
	public final <E> void forEachEntry(Collection<E> c, Consumer<E> consumer) {
		if (c == null || c.isEmpty())
			return;
		if (isSortCollections() && ! SortedSet.class.isInstance(c) && isSortable(c))
			c.stream().sorted().forEach(consumer);
		else
			c.forEach(consumer);
	}

	/**
	 * Sorts the specified collection if {@link SerializerSession#isSortCollections()} returns <jk>true</jk>.
	 *
	 * @param <E> The element type.
	 * @param c The collection being sorted.
	 * @return A new sorted {@link TreeSet}.
	 */
	public final <E> List<E> sort(List<E> c) {
		if (c == null || c.isEmpty())
			return c;
		if (isSortCollections() && isSortable(c))
			return c.stream().sorted().collect(Collectors.toList());
		return c;
	}

	private boolean isSortable(Collection<?> c) {
		if (c == null)
			return false;
		for (Object o : c)
			if (! (o instanceof Comparable))
				return false;
		return true;
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
		return alist((Object[])array);
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
	 * @param session The current serializer session.
	 * @param eType The expected type of the bean property.
	 * @param aType The actual type of the bean property.
	 * @param pMeta The current bean property being serialized.
	 * @return The bean dictionary name, or <jk>null</jk> if a name could not be found.
	 */
	protected final String getBeanTypeName(SerializerSession session, ClassMeta<?> eType, ClassMeta<?> aType, BeanPropertyMeta pMeta) {
		if (eType == aType || ! (isAddBeanTypes() || (session.isRoot() && isAddRootType())))
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
	 * The return value depends on the {@link Serializer.Builder#addRootType()} setting.
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
	 * <p>
	 * The default implementation of this method simply calls {@link Serializer#getResponseHeaders(SerializerSession)}.
	 *
	 * @return
	 * 	The HTTP headers to set on HTTP requests.
	 * 	Never <jk>null</jk>.
	 */
	public Map<String,String> getResponseHeaders() {
		return ctx.getResponseHeaders(this);
	}

	/**
	 * Returns the listener associated with this session.
	 *
	 * @param <T> The listener type.
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
	protected Object swap(ObjectSwap swap, Object o) throws SerializeException {
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
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Serializer.Builder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	protected boolean isAddBeanTypes() {
		return ctx.isAddBeanTypes();
	}

	/**
	 * Add type attribute to root nodes.
	 *
	 * @see Serializer.Builder#addRootType()
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
	 * Sort arrays and collections alphabetically.
	 *
	 * @see Serializer.Builder#sortCollections()
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() {
		return ctx.isSortCollections();
	}

	/**
	 * Sort maps alphabetically.
	 *
	 * @see Serializer.Builder#sortMaps()
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() {
		return ctx.isSortMaps();
	}

	/**
	 * Trim empty lists and arrays.
	 *
	 * @see Serializer.Builder#trimEmptyCollections()
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() {
		return ctx.isTrimEmptyCollections();
	}

	/**
	 * Trim empty maps.
	 *
	 * @see Serializer.Builder#trimEmptyMaps()
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() {
		return ctx.isTrimEmptyMaps();
	}

	/**
	 * Don't trim null bean property values.
	 *
	 * @see Serializer.Builder#keepNullProperties()
	 * @return
	 * 	<jk>true</jk> if null bean values are serialized to the output.
	 */
	protected final boolean isKeepNullProperties() {
		return ctx.isKeepNullProperties();
	}

	/**
	 * Trim strings.
	 *
	 * @see Serializer.Builder#trimStrings()
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected boolean isTrimStrings() {
		return ctx.isTrimStrings();
	}

	/**
	 * URI context bean.
	 *
	 * @see Serializer.Builder#uriContext(UriContext)
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() {
		return ctx.getUriContext();
	}

	/**
	 * URI relativity.
	 *
	 * @see Serializer.Builder#uriRelativity(UriRelativity)
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() {
		return ctx.getUriRelativity();
	}

	/**
	 * URI resolution.
	 *
	 * @see Serializer.Builder#uriResolution(UriResolution)
	 * @return
	 * 	Defines the resolution level for URIs when serializing URIs.
	 */
	protected final UriResolution getUriResolution() {
		return ctx.getUriResolution();
	}

	/**
	 * Converts the specified throwable to either a {@link RuntimeException} or {@link SerializeException}.
	 *
	 * @param <T> The throwable type.
	 * @param causedBy The exception to cast or wrap.
	 */
	protected static <T extends Throwable> void handleThrown(T causedBy) {
		if (causedBy instanceof Error)
			throw (Error)causedBy;
		if (causedBy instanceof RuntimeException)
			throw (RuntimeException)causedBy;
		if (causedBy instanceof StackOverflowError)
			throw new SerializeException("Stack overflow occurred.  This can occur when trying to serialize models containing loops.  It's recommended you use the BeanTraverseContext.BEANTRAVERSE_detectRecursions setting to help locate the loop.");
		if (causedBy instanceof SerializeException)
			throw (SerializeException)causedBy;
		throw new SerializeException(causedBy);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ContextSession */
	protected JsonMap properties() {
		return filteredMap("uriResolver", uriResolver);
	}
}
