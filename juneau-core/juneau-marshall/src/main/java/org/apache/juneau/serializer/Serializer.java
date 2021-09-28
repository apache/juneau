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

import static java.util.Optional.*;

import java.io.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Parent class for all Juneau serializers.
 * {@review}
 *
 * <h5 class='topic'>Description</h5>
 *
 * Base serializer class that serves as the parent class for all serializers.
 *
 * <p>
 * The purpose of this class is:
 * <ul>
 * 	<li>Maintain a read-only configuration state of a serializer.
 * 	<li>Create session objects used for serializing POJOs (i.e. {@link SerializerSession}).
 * 	<li>Provide convenience methods for serializing POJOs without having to construct session objects.
 * </ul>
 *
 * <p>
 * Subclasses should extend directly from {@link OutputStreamSerializer} or {@link WriterSerializer} depending on
 * whether it's a stream or character based serializer.
 */
@ConfigurableContext
public abstract class Serializer extends BeanTraverseContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Represents no Serializer.
	 */
	public static abstract class Null extends Serializer {
		private Null(SerializerBuilder builder) {
			super(builder);
		}
	}

	/**
	 * Instantiates a builder of the specified serializer class.
	 *
	 * <p>
	 * Looks for a public static method called <c>create</c> that returns an object that can be passed into a public
	 * or protected constructor of the class.
	 *
	 * @param c The builder to create.
	 * @return A new builder.
	 */
	public static SerializerBuilder createSerializerBuilder(Class<? extends Serializer> c) {
		return (SerializerBuilder)Context.createBuilder(c);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String produces, accept;
	final boolean
		addBeanTypes,
		keepNullProperties,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		addRootType;
	final UriContext uriContext;
	final UriResolution uriResolution;
	final UriRelativity uriRelativity;
	final Class<? extends SerializerListener> listener;

	private final MediaRanges acceptRanges;
	private final MediaType[] acceptMediaTypes;
	private final MediaType producesMediaType;

	/**
	 * Constructor
	 *
	 * @param builder The builder this object.
	 */
	protected Serializer(SerializerBuilder builder) {
		super(builder);

		produces = builder.produces;
		accept = builder.accept;
		addBeanTypes = builder.addBeanTypes;
		keepNullProperties = builder.keepNullProperties;
		trimEmptyCollections = builder.trimEmptyCollections;
		trimEmptyMaps = builder.trimEmptyMaps;
		trimStrings = builder.trimStrings;
		sortCollections = builder.sortCollections;
		sortMaps = builder.sortMaps;
		addRootType = builder.addRootType;
		uriContext = builder.uriContext;
		uriResolution = builder.uriResolution;
		uriRelativity = builder.uriRelativity;
		listener = builder.listener;

		this.producesMediaType = MediaType.of(produces);
		this.acceptRanges = ofNullable(accept).map(MediaRanges::of).orElseGet(()->MediaRanges.of(produces));
		this.acceptMediaTypes = ofNullable(builder.accept).map(x -> StringUtils.split(x, ',')).map(MediaType::ofAll).orElseGet(()->new MediaType[] {this.producesMediaType});
	}

	@Override
	public abstract SerializerBuilder copy();

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 *
	 * @return <jk>true</jk> if this serializer subclasses from {@link WriterSerializer}.
	 */
	public boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Create the session object used for actual serialization of objects.
	 *
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and serializer
	 * 	contexts.
	 * @return
	 * 	The new session object.
	 */
	public abstract SerializerSession createSession(SerializerSessionArgs args);


	//-----------------------------------------------------------------------------------------------------------------
	// Convenience methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public SerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Context */
	public final SerializerSessionArgs createDefaultSessionArgs() {
		return new SerializerSessionArgs().mediaType(getResponseContentType());
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * Equivalent to calling <c>serializer.createSession().serialize(o, output);</c>
	 *
	 * @param o The object to serialize.
	 * @param output
	 * 	The output object.
	 * 	<br>Character-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link Writer}
	 * 		<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 		<li>{@link File} - Output will be written as system-default encoded stream.
	 * 		<li>{@link StringBuilder} - Output will be written to the specified string builder.
	 * 	</ul>
	 * 	<br>Stream-based serializers can handle the following output class types:
	 * 	<ul>
	 * 		<li>{@link OutputStream}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 * @throws IOException Thrown by the underlying stream.
	 */
	public final void serialize(Object o, Object output) throws SerializeException, IOException {
		createSession().serialize(o, output);
	}

	/**
	 * Shortcut method for serializing objects directly to either a <c>String</c> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <c>String</c>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Object serialize(Object o) throws SerializeException {
		return createSession().serialize(o);
	}

	/**
	 * Convenience method for serializing an object to a String.
	 *
	 * <p>
	 * For writer-based serializers, this is identical to calling {@link #serialize(Object)}.
	 * <br>For stream-based serializers, this converts the returned byte array to a string based on
	 * the {@link OutputStreamSerializerBuilder#binaryFormat(BinaryFormat)} setting.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public final String serializeToString(Object o) throws SerializeException {
		return createSession().serializeToString(o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * Note that the order of these ranges are from high to low q-value.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaRanges getMediaTypeRanges() {
		return acceptRanges;
	}

	/**
	 * Returns the first entry in the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * This signifies the 'primary' media type for this serializer.
	 *
	 * @return The media type.  Never <jk>null</jk>.
	 */
	public final MediaType getPrimaryMediaType() {
		return acceptMediaTypes[0];
	}

	/**
	 * Returns the media types handled based on the value of the <c>accept</c> parameter passed into the constructor.
	 *
	 * <p>
	 * The order of the media types are the same as those in the <c>accept</c> parameter.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaType[] getAcceptMediaTypes() {
		return acceptMediaTypes;
	}

	/**
	 * Optional method that returns the response <c>Content-Type</c> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.SimpleJsonSerializer} class returns that it handles media type
	 * <js>"text/json+simple"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <c>Accept</c> header values.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public final MediaType getResponseContentType() {
		return producesMediaType;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see SerializerBuilder#addBeanTypes()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	protected boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add type attribute to root nodes.
	 *
	 * @see SerializerBuilder#addRootType()
	 * @return
	 * 	<jk>true</jk> if type property should be added to root node.
	 */
	protected final boolean isAddRootType() {
		return addRootType;
	}

	/**
	 * Serializer listener.
	 *
	 * @see SerializerBuilder#listener(Class)
	 * @return
	 * 	Class used to listen for errors and warnings that occur during serialization.
	 */
	protected final Class<? extends SerializerListener> getListener() {
		return listener;
	}

	/**
	 * Sort arrays and collections alphabetically.
	 *
	 * @see SerializerBuilder#sortCollections()
	 * @return
	 * 	<jk>true</jk> if arrays and collections are copied and sorted before serialization.
	 */
	protected final boolean isSortCollections() {
		return sortCollections;
	}

	/**
	 * Sort maps alphabetically.
	 *
	 * @see SerializerBuilder#sortMaps()
	 * @return
	 * 	<jk>true</jk> if maps are copied and sorted before serialization.
	 */
	protected final boolean isSortMaps() {
		return sortMaps;
	}

	/**
	 * Trim empty lists and arrays.
	 *
	 * @see SerializerBuilder#trimEmptyCollections()
	 * @return
	 * 	<jk>true</jk> if empty lists and arrays are not serialized to the output.
	 */
	protected final boolean isTrimEmptyCollections() {
		return trimEmptyCollections;
	}

	/**
	 * Trim empty maps.
	 *
	 * @see SerializerBuilder#trimEmptyMaps()
	 * @return
	 * 	<jk>true</jk> if empty map values are not serialized to the output.
	 */
	protected final boolean isTrimEmptyMaps() {
		return trimEmptyMaps;
	}

	/**
	 * Don't trim null bean property values.
	 *
	 * @see SerializerBuilder#keepNullProperties()
	 * @return
	 * 	<jk>true</jk> if null bean values are serialized to the output.
	 */
	protected final boolean isKeepNullProperties() {
		return keepNullProperties;
	}

	/**
	 * Trim strings.
	 *
	 * @see SerializerBuilder#trimStrings()
	 * @return
	 * 	<jk>true</jk> if string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	protected final boolean isTrimStrings() {
		return trimStrings;
	}

	/**
	 * URI context bean.
	 *
	 * @see SerializerBuilder#uriContext(UriContext)
	 * @return
	 * 	Bean used for resolution of URIs to absolute or root-relative form.
	 */
	protected final UriContext getUriContext() {
		return uriContext;
	}

	/**
	 * URI relativity.
	 *
	 * @see SerializerBuilder#uriRelativity(UriRelativity)
	 * @return
	 * 	Defines what relative URIs are relative to when serializing any of the following:
	 */
	protected final UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * URI resolution.
	 *
	 * @see SerializerBuilder#uriResolution(UriResolution)
	 * @return
	 * 	Defines the resolution level for URIs when serializing URIs.
	 */
	protected final UriResolution getUriResolution() {
		return uriResolution;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"Serializer",
				OMap
					.create()
					.filtered()
					.a("addBeanTypes", addBeanTypes)
					.a("keepNullProperties", keepNullProperties)
					.a("trimEmptyCollections", trimEmptyCollections)
					.a("trimEmptyMaps", trimEmptyMaps)
					.a("trimStrings", trimStrings)
					.a("sortCollections", sortCollections)
					.a("sortMaps", sortMaps)
					.a("addRootType", addRootType)
					.a("uriContext", uriContext)
					.a("uriResolution", uriResolution)
					.a("uriRelativity", uriRelativity)
					.a("listener", listener)
			);
	}
}
