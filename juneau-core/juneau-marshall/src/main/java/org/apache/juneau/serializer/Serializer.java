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

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;

/**
 * Parent class for all Juneau serializers.
 *
 * <h5 class='section'>Description:</h5>
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
public abstract class Serializer extends BeanContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "Serializer.";

	/**
	 * Configuration property:  Max serialization depth.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.maxDepth.i"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>100</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 * This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 */
	public static final String SERIALIZER_maxDepth = PREFIX + "maxDepth.i";

	/**
	 * Configuration property:  Initial depth.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.initialDepth.i"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>0</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * The initial indentation level at the root.
	 * Useful when constructing document fragments that need to be indented at a certain level.
	 */
	public static final String SERIALIZER_initialDepth = PREFIX + "initialDepth.i";

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.detectRecursions.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 *
	 * <p>
	 * Recursions can occur when serializing models that aren't true trees, but rather contain loops.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link #SERIALIZER_ignoreRecursions}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>SERIALIZER_ignoreRecursions</jsf> is <jk>true</jk>...
	 * <code>{A:{B:{C:null}}}</code>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Checking for recursion can cause a small performance penalty.
	 * </ul>
	 */
	public static final String SERIALIZER_detectRecursions = PREFIX + "detectRecursions.b";

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.ignoreRecursions.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Used in conjunction with {@link #SERIALIZER_detectRecursions}.
	 * Setting is ignored if <jsf>SERIALIZER_detectRecursions</jsf> is <jk>false</jk>.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 */
	public static final String SERIALIZER_ignoreRecursions = PREFIX + "ignoreRecursions.b";

	/**
	 * Configuration property:  Use whitespace.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.useWhitespace.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack serializer.
	 */
	public static final String SERIALIZER_useWhitespace = PREFIX + "useWhitespace.b";

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.maxIndent.i"</js>
	 * 	<li><b>Data type:</b> <code>Integer</code>
	 * 	<li><b>Default:</b> <code>100</code>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack or RDF serializers.
	 */
	public static final String SERIALIZER_maxIndent = PREFIX + "maxIndent.i";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.addBeanTypeProperties.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 */
	public static final String SERIALIZER_addBeanTypeProperties = PREFIX + "addBeanTypeProperties.b";

	/**
	 * Configuration property:  Quote character.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.quoteChar.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"\""</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <p>
	 * This setting does not apply to the MessagePack or RDF serializers.
	 */
	public static final String SERIALIZER_quoteChar = PREFIX + "quoteChar.s";

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimNullProperties.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with <jk>null</jk> values will be lost.
	 * </ul>
	 */
	public static final String SERIALIZER_trimNullProperties = PREFIX + "trimNullProperties.b";

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyLists.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Map entries with empty list values will be lost.
	 * 	<li>
	 * 		Bean properties with empty list values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyCollections = PREFIX + "trimEmptyLists.b";

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimEmptyMaps.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 *
	 * <p>
	 * Note that enabling this setting has the following effects on parsing:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Bean properties with empty map values will not be set.
	 * </ul>
	 */
	public static final String SERIALIZER_trimEmptyMaps = PREFIX + "trimEmptyMaps.b";

	/**
	 * Configuration property:  Trim strings.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.trimStrings.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 */
	public static final String SERIALIZER_trimStrings = PREFIX + "trimStrings.b";

	/**
	 * Configuration property:  URI context bean.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriContext.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code> (JSON object representing a {@link UriContext})
	 * 	<li><b>Default:</b> <js>"{}"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	<js>"{authority:'http://localhost:10000',contextRoot:'/myContext',servletPath:'/myServlet',pathInfo:'/foo'}"</js>
	 * </p>
	 */
	public static final String SERIALIZER_uriContext = PREFIX + "uriContext.s";

	/**
	 * Configuration property:  URI resolution.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriResolution.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code> ({@link UriResolution})
	 * 	<li><b>Default:</b> <js>"NONE"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Defines the resolution level for URIs when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriResolution#ABSOLUTE}
	 * 		- Resolve to an absolute URL (e.g. <js>"http://host:port/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#ROOT_RELATIVE}
	 * 		- Resolve to a root-relative URL (e.g. <js>"/context-root/servlet-path/path-info"</js>).
	 * 	<li>{@link UriResolution#NONE}
	 * 		- Don't do any URL resolution.
	 * </ul>
	 */
	public static final String SERIALIZER_uriResolution = PREFIX + "uriResolution.s";

	/**
	 * Configuration property:  URI relativity.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.uriRelativity.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code> ({@link UriRelativity})
	 * 	<li><b>Default:</b> <js>"RESOURCE"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Defines what relative URIs are relative to when serializing any of the following:
	 * <ul>
	 * 	<li>{@link java.net.URI}
	 * 	<li>{@link java.net.URL}
	 * 	<li>Properties annotated with {@link org.apache.juneau.annotation.URI @URI}
	 * </ul>
	 *
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li>{@link UriRelativity#RESOURCE}
	 * 		- Relative URIs should be considered relative to the servlet URI.
	 * 	<li>{@link UriRelativity#PATH_INFO}
	 * 		- Relative URIs should be considered relative to the request URI.
	 * </ul>
	 */
	public static final String SERIALIZER_uriRelativity = PREFIX + "uriRelativity.s";

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortCollections.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortCollections = PREFIX + "sortCollections.b";

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.sortMaps.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Note that this introduces a performance penalty.
	 */
	public static final String SERIALIZER_sortMaps = PREFIX + "sortMaps.b";

	/**
	 * Configuration property:  Abridged output.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.abridged.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <p>
	 * For example, when serializing a POJO with a {@link Bean#typeName()} value, a <js>"_type"</js> will be added when
	 * this setting is disabled, but not added when it is enabled.
	 */
	public static final String SERIALIZER_abridged = PREFIX + "abridged.b";

	/**
	 * Configuration property:  Serializer listener.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"Serializer.listener.c"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? extends SerializerListener&gt;</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 */
	public static final String SERIALIZER_listener = PREFIX + "listener.c";

	
	static final Serializer DEFAULT = new Serializer(PropertyStore.create().build(), "") {
		@Override
		public SerializerSession createSession(SerializerSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};
	
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final int maxDepth, initialDepth, maxIndent;
	final boolean
		detectRecursions,
		ignoreRecursions,
		useWhitespace,
		addBeanTypeProperties,
		trimNulls,
		trimEmptyCollections,
		trimEmptyMaps,
		trimStrings,
		sortCollections,
		sortMaps,
		abridged;
	final char quoteChar;
	final UriContext uriContext;
	final UriResolution uriResolution;
	final UriRelativity uriRelativity;
	final Class<? extends SerializerListener> listener;

	private final MediaType[] accept;
	private final MediaType produces;

	// Hidden constructors to force subclass from OuputStreamSerializer or WriterSerializer.
	Serializer(PropertyStore ps, String produces, String...accept) {
		super(ps);
		
		maxDepth = getProperty(SERIALIZER_maxDepth, Integer.class, 100);
		initialDepth = getProperty(SERIALIZER_initialDepth, Integer.class, 0);
		detectRecursions = getProperty(SERIALIZER_detectRecursions, boolean.class, false);
		ignoreRecursions = getProperty(SERIALIZER_ignoreRecursions, boolean.class, false);
		useWhitespace = getProperty(SERIALIZER_useWhitespace, boolean.class, false);
		maxIndent = getProperty(SERIALIZER_maxIndent, Integer.class, 100);
		addBeanTypeProperties = getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true);
		trimNulls = getProperty(SERIALIZER_trimNullProperties, boolean.class, true);
		trimEmptyCollections = getProperty(SERIALIZER_trimEmptyCollections, boolean.class, false);
		trimEmptyMaps = getProperty(SERIALIZER_trimEmptyMaps, boolean.class, false);
		trimStrings = getProperty(SERIALIZER_trimStrings, boolean.class, false);
		sortCollections = getProperty(SERIALIZER_sortCollections, boolean.class, false);
		sortMaps = getProperty(SERIALIZER_sortMaps, boolean.class, false);
		abridged = getProperty(SERIALIZER_abridged, boolean.class, false);
		quoteChar = getProperty(SERIALIZER_quoteChar, String.class, "\"").charAt(0);
		uriContext = getProperty(SERIALIZER_uriContext, UriContext.class, UriContext.DEFAULT);
		uriResolution = getProperty(SERIALIZER_uriResolution, UriResolution.class, UriResolution.NONE);
		uriRelativity = getProperty(SERIALIZER_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);
		listener = getClassProperty(SERIALIZER_listener, SerializerListener.class, null);

		this.produces = MediaType.forString(produces);
		if (accept.length == 0) {
			this.accept = new MediaType[]{this.produces};
		} else {
			this.accept = new MediaType[accept.length];
			for (int i = 0; i < accept.length; i++) {
				this.accept[i] = MediaType.forString(accept[i]);
			}
		}
	}

	@Override /* Context */
	public SerializerBuilder builder() {
		return null;
	}

	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

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


	//--------------------------------------------------------------------------------
	// Convenience methods
	//--------------------------------------------------------------------------------

	@Override /* Context */
	public final SerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Context */
	public final SerializerSessionArgs createDefaultSessionArgs() {
		return new SerializerSessionArgs(ObjectMap.EMPTY_MAP, null, null, null, getResponseContentType(), null);
	}

	/**
	 * Serializes a POJO to the specified output stream or writer.
	 *
	 * <p>
	 * Equivalent to calling <code>serializer.createSession().serialize(o, output);</code>
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
	 */
	public final void serialize(Object o, Object output) throws SerializeException {
		createSession().serialize(o, output);
	}

	/**
	 * Shortcut method for serializing objects directly to either a <code>String</code> or <code><jk>byte</jk>[]</code>
	 * depending on the serializer type.
	 *
	 * @param o The object to serialize.
	 * @return
	 * 	The serialized object.
	 * 	<br>Character-based serializers will return a <code>String</code>
	 * 	<br>Stream-based serializers will return a <code><jk>byte</jk>[]</code>
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Object serialize(Object o) throws SerializeException {
		return createSession().serialize(o);
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the value of the <code>accept</code> parameter passed into the constructor.
	 *
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaType[] getMediaTypes() {
		return accept;
	}

	/**
	 * Optional method that returns the response <code>Content-Type</code> for this serializer if it is different from
	 * the matched media type.
	 *
	 * <p>
	 * This method is specified to override the content type for this serializer.
	 * For example, the {@link org.apache.juneau.json.JsonSerializer.Simple} class returns that it handles media type
	 * <js>"text/json+simple"</js>, but returns <js>"text/json"</js> as the actual content type.
	 * This allows clients to request specific 'flavors' of content using specialized <code>Accept</code> header values.
	 *
	 * <p>
	 * This method is typically meaningless if the serializer is being used stand-alone (i.e. outside of a REST server
	 * or client).
	 *
	 * @return The response content type.  If <jk>null</jk>, then the matched media type is used.
	 */
	public final MediaType getResponseContentType() {
		return produces;
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("Serializer", new ObjectMap()
				.append("maxDepth", maxDepth)
				.append("initialDepth", initialDepth)
				.append("detectRecursions", detectRecursions)
				.append("ignoreRecursions", ignoreRecursions)
				.append("useWhitespace", useWhitespace)
				.append("maxIndent", maxIndent)
				.append("addBeanTypeProperties", addBeanTypeProperties)
				.append("trimNulls", trimNulls)
				.append("trimEmptyCollections", trimEmptyCollections)
				.append("trimEmptyMaps", trimEmptyMaps)
				.append("trimStrings", trimStrings)
				.append("sortCollections", sortCollections)
				.append("sortMaps", sortMaps)
				.append("parserKnowsRootTypes", abridged)
				.append("quoteChar", quoteChar)
				.append("uriContext", uriContext)
				.append("uriResolution", uriResolution)
				.append("uriRelativity", uriRelativity)
				.append("listener", listener)
			);
	}
}
