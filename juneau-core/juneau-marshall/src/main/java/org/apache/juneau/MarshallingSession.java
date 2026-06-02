/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import org.apache.juneau.commons.http.MediaType;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.FluentMap;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.json.JsonParserSession;
import org.apache.juneau.json5.*;
import org.apache.juneau.swap.*;

/**
 * Session object that lives for the duration of a single use of {@link MarshallingContext}.
 *
 * <ul class='spaced-list'>
 * 	<li class='info'>Typically session objects are not thread safe nor reusable.  However, bean sessions do not maintain any state and
 * 		thus can be safely cached and reused.
 * </ul>
 *
 */
@SuppressWarnings({
	"unchecked", // Type erasure requires unchecked casts
	"rawtypes", // Raw types necessary for generic type handling
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"java:S1452"  // Wildcard required - ClassMeta<?>, ObjectSwap<?,?>, etc. for bean metadata
})
public class MarshallingSession extends ContextSession implements ConverterSession, BeanSession {

	// Property name constants
	private static final String PROP_locale = "locale";
	private static final String PROP_mediaType = "mediaType";
	private static final String PROP_timeZone = "timeZone";
	private static final String PROP_BeanSession_locale = "MarshallingSession.locale";
	private static final String PROP_BeanSession_mediaType = "MarshallingSession.mediaType";
	private static final String PROP_BeanSession_timeZone = "MarshallingSession.timeZone";

	// Argument name constants for assertArgNotNull
	private static final String ARG_ctx = "ctx";
	private static final String ARG_o = "o";
	private static final String ARG_c = "c";
	private static final String ARG_classes = "classes";

	/**
	 * Builder class.
	 */
	public abstract static class Builder<SELF extends Builder<SELF>> extends ContextSession.Builder<SELF> {

		private MarshallingContext ctx;
		private Locale locale;
		private MediaType mediaType;
		private TimeZone timeZone;

		/**
		 * Constructor
		 *
		 * @param ctx The context creating this session.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(MarshallingContext ctx) {
			super(assertArgNotNull(ARG_ctx, ctx));
			this.ctx = ctx;
			mediaType = ctx.getMediaType();
			timeZone = ctx.getTimeZone();
		}

		/**
		 * Build the object.
		 *
		 * @return The built object.
		 */
		@Override
		public MarshallingSession build() {
			return new MarshallingSession(this);
		}

		/**
		 * The session locale.
		 *
		 * <p>
		 * Specifies the default locale for serializer and parser sessions.
		 *
		 * <p>
		 * If not specified, defaults to {@link MarshallingContext.Builder<?>#locale(Locale)}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link MarshalledConfig#locale()}
		 * 	<li class='jm'>{@link MarshallingContext.Builder<?>#locale(Locale)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk> defaults to {@link MarshallingContext#getLocale()}
		 * @return This object.
		 */
		public SELF locale(Locale value) {
			locale = value;
			return self();
		}

		/**
		 * The session media type.
		 *
		 * <p>
		 * Specifies the default media type value for serializer and parser sessions.
		 *
		 * <p>
		 * If not specified, defaults to {@link MarshallingContext.Builder<?>#mediaType(MediaType)}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link MarshalledConfig#mediaType()}
		 * 	<li class='jm'>{@link MarshallingContext.Builder<?>#mediaType(MediaType)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk> defaults to {@link MarshallingContext#getMediaType()}.
		 * @return This object.
		 */
		public SELF mediaType(MediaType value) {
			mediaType = value;
			return self();
		}

		/**
		 * Same as {@link #mediaType(MediaType)} but doesn't overwrite the value if it is already set.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
		 * @return This object.
		 */
		public SELF mediaTypeDefault(MediaType value) {
			if (mediaType == null)
				mediaType = value;
			return self();
		}

		@Override /* Overridden from Builder */
		public SELF property(String key, Object value) {
			if (key == null) {
				super.property(key, value);  // delegates null-key validation to base class
				return self();
			}
			switch (key) {
				case PROP_locale, PROP_BeanSession_locale:
					return locale(cvt(value, Locale.class));
				case PROP_mediaType, PROP_BeanSession_mediaType:
					return mediaType(cvt(value, MediaType.class));
				case PROP_timeZone, PROP_BeanSession_timeZone:
					return timeZone(cvt(value, TimeZone.class));
				default:
					super.property(key, value);
					return self();
			}
		}

		/**
		 * The session timezone.
		 *
		 * <p>
		 * Specifies the default timezone for serializer and parser sessions.
		 *
		 * <p>
		 * If not specified, defaults to {@link MarshallingContext.Builder<?>#timeZone(TimeZone)}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link MarshalledConfig#timeZone()}
		 * 	<li class='jm'>{@link MarshallingContext.Builder<?>#timeZone(TimeZone)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk> defaults to {@link MarshallingContext#getTimeZone()}.
		 * @return This object.
		 */
		public SELF timeZone(TimeZone value) {
			timeZone = value;
			return self();
		}

		/**
		 * Same as {@link #timeZone(TimeZone)} but doesn't overwrite the value if it is already set.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>If <jk>null</jk>, then the locale defined on the context is used.
		 * @return This object.
		 */
		public SELF timeZoneDefault(TimeZone value) {
			if (timeZone == null)
				timeZone = value;
			return self();
		}

	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@code create()} path (CRTP terminal).
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder(MarshallingContext ctx) {
			super(ctx);
		}
	}

	private static final Logger LOG = Logger.getLogger(cn(MarshallingSession.class));

	/**
	 * Creates a builder of this object.
	 *
	 * @param ctx The context creating this builder.
	 * 	<br>Cannot be <jk>null</jk>.
	 * @return A new builder.
	 */
	public static Builder<?> create(MarshallingContext ctx) {
		return new DefaultBuilder(assertArgNotNull(ARG_ctx, ctx));
	}

	/**
	 * The name property name.
	 *
	 * <p>
	 * Currently this always returns <js>"_name"</js>.
	 */
	public static final String NAME_PROPERTY_NAME = "_name";

	private final MarshallingContext ctx;
	private final Locale locale;
	private final MediaType mediaType;

	private final TimeZone timeZone;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	protected MarshallingSession(Builder<?> builder) {
		super(builder);
		ctx = builder.ctx;
		locale = opt(builder.locale).orElse(ctx.getLocale());
		mediaType = opt(builder.mediaType).orElse(builder.mediaType);
		timeZone = opt(builder.timeZone).orElse(builder.timeZone);
	}

	@Override /* ConverterSession */
	public <T> Optional<T> get(Class<T> c) {
		if (c == TimeZone.class) return opt(c.cast(timeZone));
		if (c == Locale.class) return opt(c.cast(locale));
		if (c == MediaType.class) return opt(c.cast(mediaType));
		return opte();
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	@Override
	public void addWarning(String msg, Object...args) {
		if (isDebug())
			LOG.log(Level.WARNING, () -> args.length == 0 ? msg : f(msg, args));
		super.addWarning(msg, args);
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToMemberType(Object outer, Object value, Class<T> type) throws InvalidDataConversionException {
		return convertToMemberType(outer, value, getClassMeta(type));
	}

	/**
	 * Converts the specified value to the specified class type.
	 *
	 * <p>
	 * See {@link #convertToType(Object, ClassMeta)} for the list of valid conversions.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public final <T> T convertToType(Object value, Class<T> type) throws InvalidDataConversionException {
		// Shortcut for most common case.
		if (nn(value) && value.getClass() == type)
			return (T)value;
		return convertToMemberType(null, value, getClassMeta(type));
	}

	/**
	 * Casts the specified value into the specified type.
	 *
	 * <p>
	 * If the value isn't an instance of the specified type, then converts the value if possible.
	 *
	 * <p>
	 * The following conversions are valid:
	 * <table class='styled'>
	 * 	<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
	 * 	<tr>
	 * 		<td>
	 * 			A class that is the normal type of a registered {@link ObjectSwap}.
	 * 		</td>
	 * 		<td>
	 * 			A value whose class matches the transformed type of that registered {@link ObjectSwap}.
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			A class that is the transformed type of a registered {@link ObjectSwap}.
	 * 		</td>
	 * 		<td>
	 * 			A value whose class matches the normal type of that registered {@link ObjectSwap}.
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)
	 * 			<br><code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>,
	 * 			<code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
	 * 		</td>
	 * 		<td>
	 * 			{@code Number}, {@code String}, <jk>null</jk>
	 * 		</td>
	 * 		<td>
	 * 			For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code JsonMap})
	 * 		</td>
	 * 		<td>
	 * 			{@code Map}
	 * 		</td>
	 * 		<td>
	 * 			If {@code Map} is not constructible, an {@code JsonMap} is created.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 		{@code Collection} (e.g. {@code List}, {@code LinkedList}, {@code HashSet}, {@code JsonList})
	 * 		</td>
	 * 		<td>
	 * 			{@code Collection<Object>}
	 * 			<br>{@code Object[]}
	 * 		</td>
	 * 		<td>
	 * 			If {@code Collection} is not constructible, an {@code JsonList} is created.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code X[]} (array of any type X)
	 * 		</td>
	 * 		<td>
	 * 			{@code List<X>}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code X[][]} (multi-dimensional arrays)
	 * 		</td>
	 * 		<td>
	 * 			{@code List<List<X>>}
	 * 			<br>{@code List<X[]>}
	 * 			<br>{@code List[]<X>}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code Enum}
	 * 		</td>
	 * 		<td>
	 * 			{@code String}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			Bean
	 * 		</td>
	 * 		<td>
	 * 			{@code Map}
	 * 		</td>
	 * 		<td>&nbsp;</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			{@code String}
	 * 		</td>
	 * 		<td>
	 * 			Anything
	 * 		</td>
	 * 		<td>
	 * 			Arrays are converted to JSON arrays
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			Anything with one of the following methods:
	 * 			<br><code><jk>public static</jk> T fromString(String)</code>
	 * 			<br><code><jk>public static</jk> T valueOf(String)</code>
	 * 			<br><code><jk>public</jk> T(String)</code>
	 * 		</td>
	 * 		<td>
	 * 			<c>String</c>
	 * 		</td>
	 * 		<td>
	 * 			<br>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public final <T> T convertToType(Object value, ClassMeta<T> type) throws InvalidDataConversionException {
		return convertToMemberType(null, value, type);
	}

	/**
	 * Same as {@link #convertToType(Object, Class)}, but allows for complex data types consisting of collections or maps.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to be converted.
	 * @param type The target object type.
	 * @param args The target object parameter types.
	 * @return The converted type.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 */
	public final <T> T convertToType(Object value, Type type, Type...args) throws InvalidDataConversionException {
		return (T)convertToMemberType(null, value, getClassMeta(type, args));
	}

	/**
	 * Returns the annotation provider for this session.
	 *
	 * @return The annotation provider for this session.
	 */
	public AnnotationProvider getAnnotationProvider() {
		return ctx.getAnnotationProvider();
	}

	/**
	 * Returns the {@link MarshallingContext} backing this session.
	 *
	 * @return The marshalling context.
	 */
	public final MarshallingContext getMarshallingContext() {
		return ctx;
	}

	/**
	 * Minimum bean class visibility.
	 *
	 * @see MarshallingContext.Builder<?>#beanClassVisibility(Visibility)
	 * @return
	 * 	Classes are not considered beans unless they meet the minimum visibility requirements.
	 */
	public final Visibility getBeanClassVisibility() { return ctx.getBeanClassVisibility(); }

	/**
	 * Minimum bean constructor visibility.
	 *
	 * @see MarshallingContext.Builder<?>#beanConstructorVisibility(Visibility)
	 * @return
	 * 	Only look for constructors with this specified minimum visibility.
	 */
	public final Visibility getBeanConstructorVisibility() { return ctx.getBeanConstructorVisibility(); }

	/**
	 * Bean dictionary.
	 *
	 * @see MarshallingContext.Builder<?>#beanDictionary(ClassInfo...)
	 * @return
	 * 	The list of classes that make up the bean dictionary in this bean context.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>List is unmodifiable.
	 */
	public final List<ClassInfo> getBeanDictionary() { return ctx.getBeanDictionary(); }

	/**
	 * Minimum bean field visibility.
	 *
	 *
	 * @see MarshallingContext.Builder<?>#beanFieldVisibility(Visibility)
	 * @return
	 * 	Only look for bean fields with this specified minimum visibility.
	 */
	public final Visibility getBeanFieldVisibility() { return ctx.getBeanFieldVisibility(); }

	/**
	 * Returns the {@link BeanMeta} class for the specified class.
	 *
	 * @param <T> The class type to get the meta-data on.
	 * @param c The class to get the meta-data on.
	 * @return
	 * 	The {@link BeanMeta} for the specified class, or <jk>null</jk> if the class
	 * 	is not a bean per the settings on this context.
	 */
	public final <T> BeanMeta<T> getBeanMeta(Class<T> c) {
		if (c == null)
			return null;
		return getClassMeta(c).getBeanMeta();
	}

	/**
	 * Minimum bean method visibility.
	 *
	 * @see MarshallingContext.Builder<?>#beanMethodVisibility(Visibility)
	 * @return
	 * 	Only look for bean methods with this specified minimum visibility.
	 */
	public final Visibility getBeanMethodVisibility() { return ctx.getBeanMethodVisibility(); }

	/**
	 * Returns the bean registry defined in this bean context defined by {@link MarshallingContext.Builder<?>#beanDictionary(ClassInfo...)}.
	 *
	 * @return The bean registry defined in this bean context.  Never <jk>null</jk>.
	 */
	public final BeanRegistry getBeanRegistry() { return ctx.getBeanRegistry(); }

	/**
	 * Bean type property name.
	 *
	 * @see MarshallingContext.Builder<?>#typePropertyName(String)
	 * @return
	 * 	The name of the bean property used to store the dictionary name of a bean type so that the parser knows the data type to reconstruct.
	 */
	public final String getBeanTypePropertyName() { return ctx.getBeanTypePropertyName(); }

	/**
	 * Returns the type property name as defined by {@link MarshallingContext.Builder<?>#typePropertyName(String)}.
	 *
	 * @param cm
	 * 	The class meta of the type we're trying to resolve the type name for.
	 * 	Can be <jk>null</jk>.
	 * @return The type property name.  Never <jk>null</jk>.
	 */
	public final String getBeanTypePropertyName(ClassMeta cm) {
		if (cm == null)
			return getBeanTypePropertyName();
		var beanMeta = cm.getBeanMeta();
		if (beanMeta == null)
			return getBeanTypePropertyName();
		var s = beanMeta.getTypePropertyName();
		return s == null ? getBeanTypePropertyName() : s;
	}

	/**
	 * Returns a {@code ClassMeta} wrapper around a {@link Class} object.
	 *
	 * @param <T> The class type being wrapped.
	 * @param c The class being wrapped.
	 * @return The class meta object containing information about the class.
	 */
	public final <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return ctx.getClassMeta(c);
	}

	/**
	 * Used to resolve <c>ClassMetas</c> of type <c>Collection</c> and <c>Map</c> that have
	 * <c>ClassMeta</c> values that themselves could be collections or maps.
	 *
	 * <p>
	 * <c>Collection</c> meta objects are assumed to be followed by zero or one meta objects indicating the
	 * element type.
	 *
	 * <p>
	 * <c>Map</c> meta objects are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
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
	 * @param <T>
	 * 	The class to resolve.
	 * @param type
	 * 	The class to resolve.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The class meta.
	 */
	public final <T> ClassMeta<T> getClassMeta(Type type, Type...args) {
		return ctx.getClassMeta(type, args);
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())}.
	 *
	 * @param <T> The class of the object being passed in.
	 * @param o The class to find the class type for.
	 * @return The ClassMeta object, or <jk>null</jk> if {@code o} is <jk>null</jk>.
	 */
	public final <T> ClassMeta<T> getClassMetaForObject(T o) {
		return (ClassMeta<T>)getClassMetaForObject(o, null);
	}

	/**
	 * Locale.
	 *
	 * <p>
	 * The locale is determined in the following order:
	 * <ol>
	 * 	<li><c>locale</c> parameter passed in through constructor.
	 * 	<li>{@link MarshallingContext.Builder<?>#locale(Locale)} setting on bean context.
	 * 	<li>Locale returned by {@link Locale#getDefault()}.
	 * </ol>
	 *
	 * @see MarshallingContext.Builder<?>#locale(Locale)
	 * @return The session locale.
	 */
	public Locale getLocale() { return locale; }

	/**
	 * Media type.
	 *
	 * <p>
	 * For example, <js>"application/json"</js>.
	 *
	 * @see MarshallingContext.Builder<?>#mediaType(MediaType)
	 * @return The media type for this session, or <jk>null</jk> if not specified.
	 */
	public final MediaType getMediaType() { return mediaType; }

	/**
	 * Bean class exclusions.
	 *
	 * @see MarshallingContext.Builder<?>#notBeanClasses(ClassInfo...)
	 * @return
	 * 	The list of classes that are explicitly not beans.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>List is unmodifiable.
	 */
	public final List<ClassInfo> getNotBeanClasses() { return ctx.getNotBeanClasses(); }

	/**
	 * Bean package exclusions.
	 *
	 * @see MarshallingContext.Builder<?>#notBeanPackages(String...)
	 * @return
	 * 	The set of fully-qualified package names to exclude from being classified as beans.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>Set is unmodifiable.
	 */
	public final Set<String> getNotBeanPackagesNames() { return ctx.getNotBeanPackagesNames(); }

	/**
	 * Bean property namer.
	 *
	 * @see MarshallingContext.Builder<?>#propertyNamer(Class)
	 * @return
	 * 	The interface used to calculate bean property names.
	 */
	public final PropertyNamer getPropertyNamer() { return ctx.getPropertyNamer(); }

	/**
	 * Session classloader.
	 *
	 * <p>
	 * Returns the classloader explicitly configured via {@link MarshallingContext.Builder<?>#classLoader(ClassLoader)},
	 * or <jk>null</jk> if none was set (callers should fall back to the thread-context classloader).
	 *
	 * @return The session classloader, or <jk>null</jk> if not set.
	 */
	public final ClassLoader getClassLoader() { return ctx.getClassLoader(); }

	/**
	 * Java object swaps.
	 *
	 * @see MarshallingContext.Builder<?>#swaps(Class...)
	 * @return
	 * 	The list POJO swaps defined.
	 * 	<br>Never <jk>null</jk>.
	 * 	<br>List is unmodifiable.
	 */
	public final List<ObjectSwap<?,?>> getSwaps() { return ctx.getSwaps(); }

	/**
	 * Time zone.
	 *
	 * <p>
	 * The timezone is determined in the following order:
	 * <ol>
	 * 	<li><c>timeZone</c> parameter passed in through constructor.
	 * 	<li>{@link MarshallingContext.Builder<?>#timeZone(TimeZone)} setting on bean context.
	 * </ol>
	 *
	 * @see MarshallingContext.Builder<?>#timeZone(TimeZone)
	 * @return The session timezone, or <jk>null</jk> if timezone not specified.
	 */
	public final TimeZone getTimeZone() { return timeZone; }

	/**
	 * Time zone.
	 *
	 * <p>
	 * The timezone is determined in the following order:
	 * <ol>
	 * 	<li><c>timeZone</c> parameter passed in through constructor.
	 * 	<li>{@link MarshallingContext.Builder<?>#timeZone(TimeZone)} setting on bean context.
	 * </ol>
	 *
	 * @see MarshallingContext.Builder<?>#timeZone(TimeZone)
	 * @return The session timezone, or the system timezone if not specified.  Never <jk>null</jk>.
	 */
	public final ZoneId getTimeZoneId() { return timeZone == null ? ZoneId.systemDefault() : timeZone.toZoneId(); }

	/**
	 * Determines whether the specified class matches the requirements on this context of being a bean.
	 *
	 * @param c The class being tested.
	 * @return <jk>true</jk> if the specified class is considered a bean.
	 */
	public final boolean isBean(Class<?> c) {
		return nn(getBeanMeta(c));
	}

	/**
	 * Determines whether the specified object matches the requirements on this context of being a bean.
	 *
	 * @param o The object being tested.
	 * @return <jk>true</jk> if the specified object is considered a bean.
	 */
	public final boolean isBean(Object o) {
		if (o == null)
			return false;
		return isBean(o.getClass());
	}

	/**
	 * BeanMap.put() returns old property value.
	 *
	 * @see MarshallingContext.Builder<?>#beanMapPutReturnsOldValue()
	 * @return
	 * 	<jk>true</jk> if the {@link BeanMap#put(String,Object) BeanMap.put()} method will return old property values.
	 * 	<br>Otherwise, it returns <jk>null</jk>.
	 */
	public final boolean isBeanMapPutReturnsOldValue() { return ctx.isBeanMapPutReturnsOldValue(); }

	/**
	 * Beans require no-arg constructors.
	 *
	 * @see MarshallingContext.Builder<?>#beansRequireDefaultConstructor()
	 * @return
	 * 	<jk>true</jk> if a Java class must implement a default no-arg constructor to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireDefaultConstructor() { return ctx.isBeansRequireDefaultConstructor(); }

	/**
	 * Beans require Serializable interface.
	 *
	 * @see MarshallingContext.Builder<?>#beansRequireSerializable()
	 * @return
	 * 	<jk>true</jk> if a Java class must implement the {@link Serializable} interface to be considered a bean.
	 * 	<br>Otherwise, the bean will be serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSerializable() { return ctx.isBeansRequireSerializable(); }

	/**
	 * Beans require setters for getters.
	 *
	 * @see MarshallingContext.Builder<?>#beansRequireSettersForGetters()
	 * @return
	 * 	<jk>true</jk> if only getters that have equivalent setters will be considered as properties on a bean.
	 * 	<br>Otherwise, they are ignored.
	 */
	public final boolean isBeansRequireSettersForGetters() { return ctx.isBeansRequireSettersForGetters(); }

	/**
	 * Beans require at least one property.
	 *
	 * @see MarshallingContext.Builder<?>#disableBeansRequireSomeProperties()
	 * @return
	 * 	<jk>true</jk> if a Java class doesn't need to contain at least 1 property to be considered a bean.
	 * 	<br>Otherwise, the bean is serialized as a string using the {@link Object#toString()} method.
	 */
	public final boolean isBeansRequireSomeProperties() { return ctx.isBeansRequireSomeProperties(); }

	/**
	 * Find fluent setters.
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 *
	 * @see MarshallingContext.Builder<?>#findFluentSetters()
	 * @return
	 * 	<jk>true</jk> if fluent setters are detected on beans.
	 */
	public final boolean isFindFluentSetters() { return ctx.isFindFluentSetters(); }

	/**
	 * Ignore invocation errors on getters.
	 *
	 * @see MarshallingContext.Builder<?>#ignoreInvocationExceptionsOnGetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean getter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnGetters() { return ctx.isIgnoreInvocationExceptionsOnGetters(); }

	/**
	 * Ignore invocation errors on setters.
	 *
	 * @see MarshallingContext.Builder<?>#ignoreInvocationExceptionsOnSetters()
	 * @return
	 * 	<jk>true</jk> if errors thrown when calling bean setter methods are silently ignored.
	 */
	public final boolean isIgnoreInvocationExceptionsOnSetters() { return ctx.isIgnoreInvocationExceptionsOnSetters(); }

	/**
	 * Silently ignore missing setters.
	 *
	 * @see MarshallingContext.Builder<?>#disableIgnoreMissingSetters()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a bean property without a setter should throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreMissingSetters() { return ctx.isIgnoreMissingSetters(); }

	/**
	 * Ignore unknown properties.
	 *
	 * @see MarshallingContext.Builder<?>#ignoreUnknownBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a value on a non-existent bean property is silently ignored.
	 * 	<br>Otherwise, a {@code RuntimeException} is thrown.
	 */
	public final boolean isIgnoreUnknownBeanProperties() { return ctx.isIgnoreUnknownBeanProperties(); }

	/**
	 * Schema validation mode.
	 *
	 * @see MarshallingContext.Builder<?>#validateSchema()
	 * @return
	 * 	<jk>true</jk> if bean property values should be validated against their {@code @Schema}-declared constraints
	 * 	during parsing and serialization.
	 * @since 9.5.0
	 */
	public final boolean isValidateSchema() { return ctx.isValidateSchema(); }

	/**
	 * Ignore unknown properties with null values.
	 *
	 * @see MarshallingContext.Builder<?>#disableIgnoreUnknownNullBeanProperties()
	 * @return
	 * 	<jk>true</jk> if trying to set a <jk>null</jk> value on a non-existent bean property should not throw a {@link BeanRuntimeException}.
	 */
	public final boolean isIgnoreUnknownNullBeanProperties() { return ctx.isIgnoreUnknownNullBeanProperties(); }

	/**
	 * Unsorted properties.
	 *
	 * @see MarshallingContext.Builder<?>#unsortedProperties()
	 * @return
	 * 	<jk>true</jk> if bean properties are serialized in natural JVM-dependent order instead of alphabetically.
	 */
	public final boolean isUnsortedProperties() { return ctx.isUnsortedProperties(); }

	/**
	 * Use interface proxies.
	 *
	 * @see MarshallingContext.Builder<?>#disableInterfaceProxies()
	 * @return
	 * 	<jk>true</jk> if interfaces will be instantiated as proxy classes through the use of an
	 * 	{@link InvocationHandler} if there is no other way of instantiating them.
	 */
	public final boolean isUseInterfaceProxies() { return ctx.isUseInterfaceProxies(); }

	/**
	 * Use Java Introspector.
	 *
	 * @see MarshallingContext.Builder<?>#useJavaBeanIntrospector()
	 * @return
	 * 	<jk>true</jk> if the built-in Java bean introspector should be used for bean introspection.
	 */
	public final boolean isUseJavaBeanIntrospector() { return ctx.isUseJavaBeanIntrospector(); }

	/**
	 * Creates a new empty bean of the specified type, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a new instance of the specified bean class</jc>
	 * 	Person <jv>person</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.newBean(Person.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public final <T> T newBean(Class<T> c) throws BeanRuntimeException {
		return newBean(null, c);
	}

	/**
	 * Same as {@link #newBean(Class)}, except used for instantiating inner member classes that must be instantiated
	 * within another class instance.
	 *
	 * @param <T> The class type of the bean being created.
	 * @param c The class type of the bean being created.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new bean object.
	 * @throws BeanRuntimeException If the specified class is not a valid bean.
	 */
	public final <T> T newBean(Object outer, Class<T> c) throws BeanRuntimeException {
		var cm = getClassMeta(c);
		var m = cm.getBeanMeta();
		if (m == null)
			return null;
		try {
			var o = m.newBean(outer);
			if (o == null)
				throw bex(c, "Class does not have a no-arg constructor.");
			return o;
		} catch (BeanRuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw bex(e);
		}
	}

	/**
	 * Creates a new {@link BeanMap} object (a modifiable {@link Map}) of the given class with uninitialized
	 * property values.
	 *
	 * <p>
	 * If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a
	 * bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a new bean map wrapped around a new Person object</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.newBeanMap(Person.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @return A new instance of the class.
	 */
	public final <T> BeanMap<T> newBeanMap(Class<T> c) {
		return newBeanMap(null, c);
	}

	/**
	 * Same as {@link #newBeanMap(Class)}, except used for instantiating inner member classes that must be instantiated
	 * within another class instance.
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param c The name of the class to create a new instance of.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @return A new instance of the class.
	 */
	@SuppressWarnings({
		"java:S1168",   // null when BeanMeta not found. Consider empty BeanMap.
		"java:S1135"    // TODO comment - deferred design consideration
	})
	public final <T> BeanMap<T> newBeanMap(Object outer, Class<T> c) {
		var m = getBeanMeta(c);
		if (m == null)
			return null;
		T bean = null;
		if (e(m.getConstructorArgs()))
			bean = newBean(outer, c);
		var bm = new BeanMap<>(bean, m);
		bm.setBeanSession(this);
		return bm;
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
	public final ClassMeta<Object> object() {
		return ctx.object();
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
	public final ClassMeta<String> string() {
		return ctx.string();
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (a modifiable {@link Map}).
	 *
	 * <p>
	 * If object is not a true bean, then throws a {@link BeanRuntimeException} with an explanation of why it's not a
	 * bean.
	 *
	 * <p>
	 * If object is already a {@link BeanMap}, simply returns the same object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> Person());
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @return The wrapped object.
	 */
	@Override
	public final <T> BeanMap<T> toBeanMap(T o) {
		if (o instanceof BeanMap o2)
			return o2;
		return this.toBeanMap(o, (Class<T>)o.getClass());
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (i.e.: a modifiable {@link Map}) defined as a bean for one of its
	 * class, a super class, or an implemented interface.
	 *
	 * <p>
	 * If object is not a true bean, throws a {@link BeanRuntimeException} with an explanation of why it's not a bean.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean map for new bean using only properties defined in a superclass</jc>
	 * 	BeanMap&lt;MySubBean&gt; <jv>beanMap</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> MySubBean(), MySuperBean.<jk>class</jk>);
	 *
	 * 	<jc>// Construct a bean map for new bean using only properties defined in an interface</jc>
	 * 	BeanMap&lt;MySubBean&gt; <jv>beanMap</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> MySubBean(), MySuperInterface.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a bean interface.  Must not be null.
	 * @param c The superclass to narrow the bean properties to.  Must not be null.
	 * @return The bean representation, or <jk>null</jk> if the object is not a true bean.
	 * @throws NullPointerException If either parameter is null.
	 * @throws IllegalArgumentException If the specified object is not an an instance of the specified class.
	 * @throws
	 * 	BeanRuntimeException If specified object is not a bean according to the bean rules specified in this context
	 * class.
	 */
	public final <T> BeanMap<T> toBeanMap(T o, Class<? super T> c) throws BeanRuntimeException {
		assertArgNotNull(ARG_o, o);
		assertArgNotNull(ARG_c, c);
		assertArg(c.isInstance(o), "The specified object is not an instance of the specified class.  class=''{0}'', objectClass=''{1}'', object=''{2}''", cn(c), cn(o), 0);

		var cm = getClassMeta(c);

		BeanMeta m = cm.getBeanMeta();
		if (m == null)
			throw bex(c, "Class is not a bean.  Reason=''{0}''", cm.getNotABeanReason());
		var bm = new BeanMap<>(o, m);
		bm.setBeanSession(this);
		return bm;
	}

	/**
	 * Wraps an object inside a {@link BeanMap} object (a modifiable {@link Map}).
	 *
	 * <p>
	 * Same as {@link #toBeanMap(Object)} but allows you to specify a property namer instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Construct a bean map around a bean instance</jc>
	 * 	BeanMap&lt;Person&gt; <jv>beanMap</jv> = MarshallingContext.<jsf>DEFAULT</jsf>.toBeanMap(<jk>new</jk> Person(), PropertyNamerDLC.<jsf>INSTANCE</jsf>);
	 * </p>
	 *
	 * @param <T> The class of the object being wrapped.
	 * @param o The object to wrap in a map interface.  Must not be null.
	 * @param propertyNamer The property namer to use.
	 * @return The wrapped object.
	 */
	@SuppressWarnings({
		"java:S1172" // Parameter reserved for future property naming strategy support
	})
	public final <T> BeanMap<T> toBeanMap(T o, PropertyNamer propertyNamer) {
		return this.toBeanMap(o, (Class<T>)o.getClass());
	}

	/**
	 * Same as {@link #convertToType(Object, ClassMeta)}, except used for instantiating inner member classes that must
	 * be instantiated within another class instance.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param outer
	 * 	If class is a member class, this is the instance of the containing class.
	 * 	Should be <jk>null</jk> if not a member class.
	 * @param value The value to convert.
	 * @param to The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	protected final <T> T convertToMemberType(Object outer, Object value, ClassMeta<T> to) throws InvalidDataConversionException {
		if (to == null)
			to = (ClassMeta<T>)object();
		try {
			// If the value is already an instance of the target type and no element/value conversion is needed, return as-is.
			// Skip this shortcut for typed maps/collections whose elements may need conversion.
			if (value != null && to.inner().isInstance(value)
					&& !((to.isMap() && !to.getValueType().is(Object.class)) || ((to.isCollection() || to.isOptional()) && !to.getElementType().isObject())))
				return (T) value;
			return ctx.getConverter().to(value, outer, this, to.innerType(), to.getParameters());
		} catch (InvalidDataConversionException e) {
			throw e;
		} catch (Exception e) {
			throw new InvalidDataConversionException(value, to, e);
		}
	}

	/**
	 * Given an array of {@link Type} objects, returns a {@link ClassMeta} representing those arguments.
	 *
	 * <p>
	 * Constructs a new meta on each call.
	 *
	 * @param classes The array of classes to get class metas for.
	 * @return The args {@link ClassMeta} object corresponding to the classes.  Never <jk>null</jk>.
	 */
	protected final ClassMeta<Object[]> getArgsClassMeta(Type[] classes) {
		assertArgNotNull(ARG_classes, classes);
		return new ClassMeta(Arrays.stream(classes).map(this::getClassMeta).toList());
	}

	/**
	 * Shortcut for calling {@code getClassMeta(o.getClass())} but returns a default value if object is <jk>null</jk>.
	 *
	 * @param o The class to find the class type for.
	 * @param def The default {@link ClassMeta} if the object is null.
	 * @return The ClassMeta object, or the default value if {@code o} is <jk>null</jk>.
	 */
	protected final ClassMeta<?> getClassMetaForObject(Object o, ClassMeta<?> def) {
		if (o == null)
			return def;
		return getClassMeta(o.getClass());
	}

	/**
	 * Creates a generic {@link Map} for parser-driven map population, choosing the map type based on the key type.
	 *
	 * <p>
	 * Returns the flavored {@link MarshalledMap} subclass when the key type is String (via {@link #newGenericMap()}),
	 * otherwise returns a plain {@link LinkedHashMap} (since {@link MarshalledMap} is keyed by String).
	 *
	 * <p>
	 * Per-marshaller parser sessions override {@link #newGenericMap()} (not this method) to return their flavored
	 * {@code XMap} variant.
	 *
	 * @param mapMeta The metadata of the map to create.
	 * @return A new map.
	 */
	protected Map newGenericMap(ClassMeta mapMeta) {
		var k = mapMeta.getKeyType();
		return (k == null || k.isString()) ? newGenericMap() : map();
	}

	/**
	 * Creates a string-keyed generic map for parser-driven map population.
	 *
	 * <p>
	 * This is the main factory hook overridden by per-marshaller parser sessions to return their flavored
	 * {@code XMap} variant (for example, {@link JsonParserSession} returns
	 * {@link JsonMap}; {@link Json5ParserSession} returns
	 * {@link Json5Map}).
	 *
	 * @return A new map.
	 */
	protected MarshalledMap newGenericMap() {
		return new MarshalledMap(this);
	}

	/**
	 * Creates a generic list for parser-driven list population.
	 *
	 * <p>
	 * Per-marshaller parser sessions override this hook to return their flavored {@code XList} variant
	 * (for example, {@link JsonParserSession} returns {@link JsonList};
	 * {@link Json5ParserSession} returns
	 * {@link Json5List}).
	 *
	 * @return A new list.
	 */
	protected MarshalledList newGenericList() {
		return new MarshalledList(this);
	}

	@Override /* Overridden from ContextSession */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_locale, locale)
			.a(PROP_mediaType, mediaType)
			.a(PROP_timeZone, timeZone);
	}

	/**
	 * Converts the contents of the specified list into an array.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * <p>
	 * In the case of multi-dimensional arrays, the incoming list must contain elements of type n-1 dimension.
	 * i.e. if {@code type} is <code><jk>int</jk>[][]</code> then {@code list} must have entries of type
	 * <code><jk>int</jk>[]</code>.
	 *
	 * @param type The type to convert to.  Must be an array type.
	 * @param list The contents to populate the array with.
	 * @return A new object or primitive array.
	 */
	protected final Object toArray(ClassMeta<?> type, Collection<?> list) {
		if (list == null)
			return null;
		var componentType = type.isArgs() ? object() : type.getElementType();
		var array = Array.newInstance(componentType.inner(), list.size());
		var i = IntegerHolder.create();
		list.forEach(x -> {
			var x2 = x;
			if (! type.isInstance(x)) {
				if (componentType.isArray() && x instanceof Collection<?> c)
					x2 = toArray(componentType, c);
				else if (x == null && componentType.isPrimitive())
					x2 = componentType.getPrimitiveDefault();
				else
					x2 = convertToType(x, componentType);
			}
			Array.set(array, i.getAndIncrement(), x2);
		});
		return array;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanSession bridge methods — SPI seam to commons.bean.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bridge implementation of {@link BeanSession#convertToType(Object, Object)} that dispatches to the
	 * {@link ClassMeta}-typed overload when the supplied {@code targetType} is a {@link ClassMeta}, or to the
	 * {@link Class}-typed overload when it is a {@link Class}.
	 *
	 * <p>
	 * The bean-modeling layer calls this method through the {@link BeanSession} interface so it can request type
	 * conversion without referencing the marshalling-side {@link ClassMeta} concrete type.
	 *
	 * @param value The value to convert.  May be <jk>null</jk>.
	 * @param targetType A {@link ClassMeta} or {@link Class} describing the target type.  Must not be <jk>null</jk>.
	 * @return The converted value.
	 * @throws IllegalArgumentException If {@code targetType} is neither a {@link ClassMeta} nor a {@link Class}.
	 */
	@Override /* BeanSession */
	public final Object convertToType(Object value, Object targetType) {
		if (targetType == null)
			return convertToType(value, (ClassMeta<?>) null);
		if (targetType instanceof ClassMeta<?> cm)
			return convertToType(value, cm);
		if (targetType instanceof Class<?> c)
			return convertToType(value, c);
		throw illegalArg("Unsupported targetType for convertToType: {0}", targetType.getClass().getName());
	}

	/**
	 * Bridge implementation of {@link BeanSession#convertToMemberType(Object, Object, Object)} that dispatches to the
	 * {@link ClassMeta}-typed overload when the supplied {@code targetType} is a {@link ClassMeta}, or to the
	 * {@link Class}-typed overload when it is a {@link Class}.
	 *
	 * @param outer The outer-class instance for non-static inner classes.  May be <jk>null</jk>.
	 * @param value The value to convert.  May be <jk>null</jk>.
	 * @param targetType A {@link ClassMeta} or {@link Class} describing the target type.  Must not be <jk>null</jk>.
	 * @return The converted value.
	 * @throws IllegalArgumentException If {@code targetType} is neither a {@link ClassMeta} nor a {@link Class}.
	 */
	@Override /* BeanSession */
	public final Object convertToMemberType(Object outer, Object value, Object targetType) {
		if (targetType == null)
			return convertToMemberType(outer, value, (ClassMeta<?>) null);
		if (targetType instanceof ClassMeta<?> cm)
			return convertToMemberType(outer, value, cm);
		if (targetType instanceof Class<?> c)
			return convertToMemberType(outer, value, c);
		throw illegalArg("Unsupported targetType for convertToMemberType: {0}", targetType.getClass().getName());
	}

	/**
	 * Bridge implementation of {@link BeanSession#parseToMap(CharSequence)} that delegates to
	 * {@link Json5Map#ofString(CharSequence)} paired with this session.
	 *
	 * <p>
	 * Used by {@link BeanPropertyMeta#setPropertyValue} to parse a {@link CharSequence} into a {@link Map} when
	 * the property is map-typed.  Lifted out of {@link BeanPropertyMeta} so the bean-modeling layer no longer
	 * references the marshalling-side JSON parser.
	 *
	 * <p>
	 * Uses {@link Json5Map} so that historic JSON5-form inputs (e.g. annotation default
	 * values written as <c>{a:'b'}</c>) continue to parse — the strict-JSON {@link JsonMap} retargeting in v9.5
	 * would otherwise break those call sites.
	 *
	 * @param value The JSON-formatted character sequence to parse.
	 * @return The parsed {@link Json5Map} (empty if {@code value} is <jk>null</jk>), never <jk>null</jk>.
	 */
	@Override /* BeanSession */
	public final Map<?,?> parseToMap(CharSequence value) {
		return Json5Map.ofString(value).session(this);
	}

	/**
	 * Bridge implementation of {@link BeanSession#parseToList(CharSequence)} that delegates to
	 * {@link Json5List#Json5List(CharSequence)} paired with this session.
	 *
	 * <p>
	 * Used by {@link BeanPropertyMeta#setPropertyValue} to parse a {@link CharSequence} into a {@link Collection}
	 * when the property is collection-typed.  Lifted out of {@link BeanPropertyMeta} so the bean-modeling layer
	 * no longer references the marshalling-side JSON parser.
	 *
	 * <p>
	 * Uses {@link Json5List} for the same back-compat reasons as
	 * {@link #parseToMap(CharSequence)}.
	 *
	 * @param value The JSON-formatted character sequence to parse.  Must not be <jk>null</jk>.
	 * @return The parsed {@link Json5List}.
	 */
	@Override /* BeanSession */
	public final Collection<?> parseToList(CharSequence value) {
		return new Json5List(value).setBeanSession(this);
	}

}
