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
package org.apache.juneau.uon;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to UON (a notation for URL-encoded query parameter values).
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/uon</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/uon</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * The following shows a sample object defined in Javascript:
 * <p class='bcode w800'>
 * 	{
 * 		id: 1,
 * 		name: <js>'John Smith'</js>,
 * 		uri: <js>'http://sample/addressBook/person/1'</js>,
 * 		addressBookUri: <js>'http://sample/addressBook'</js>,
 * 		birthDate: <js>'1946-08-12T00:00:00Z'</js>,
 * 		otherIds: <jk>null</jk>,
 * 		addresses: [
 * 			{
 * 				uri: <js>'http://sample/addressBook/address/1'</js>,
 * 				personUri: <js>'http://sample/addressBook/person/1'</js>,
 * 				id: 1,
 * 				street: <js>'100 Main Street'</js>,
 * 				city: <js>'Anywhereville'</js>,
 * 				state: <js>'NY'</js>,
 * 				zip: 12345,
 * 				isCurrent: <jk>true</jk>,
 * 			}
 * 		]
 * 	}
 * </p>
 *
 * <p>
 * Using the "strict" syntax defined in this document, the equivalent UON notation would be as follows:
 * <p class='bcode w800'>
 * 	(
 * 		<ua>id</ua>=<un>1</un>,
 * 		<ua>name</ua>=<us>'John+Smith'</us>,
 * 		<ua>uri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 		<ua>addressBookUri</ua>=<us>http://sample/addressBook</us>,
 * 		<ua>birthDate</ua>=<us>1946-08-12T00:00:00Z</us>,
 * 		<ua>otherIds</ua>=<uk>null</uk>,
 * 		<ua>addresses</ua>=@(
 * 			(
 * 				<ua>uri</ua>=<us>http://sample/addressBook/address/1</us>,
 * 				<ua>personUri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 				<ua>id</ua>=<un>1</un>,
 * 				<ua>street</ua>=<us>'100+Main+Street'</us>,
 * 				<ua>city</ua>=<us>Anywhereville</us>,
 * 				<ua>state</ua>=<us>NY</us>,
 * 				<ua>zip</ua>=<un>12345</un>,
 * 				<ua>isCurrent</ua>=<uk>true</uk>
 * 			)
 * 		)
 * 	)
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = OMap.<jsm>ofJson</jsm>(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "(a=b,c=1,d=false,e=@(f,1,false),g=(h=i))"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 *
 * 	<jc>// Serialize a bean</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(String s);
 * 		<jk>public</jk> String getName();
 * 		<jk>public int</jk> getAge();
 * 		<jk>public</jk> Address getAddress();
 * 		<jk>public boolean</jk> deceased;
 * 	}
 *
 * 	<jk>public class</jk> Address {
 * 		<jk>public</jk> String getStreet();
 * 		<jk>public</jk> String getCity();
 * 		<jk>public</jk> String getState();
 * 		<jk>public int</jk> getZip();
 * 	}
 *
 * 	Person p = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>,
 * 		<js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "(name='John Doe',age=23,address=(street='123 Main St',city=Anywhere,state=NY,zip=12345),deceased=false)"</jc>
 * 	String s = UonSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 * </p>
 */
@ConfigurableContext
public class UonSerializer extends WriterSerializer implements HttpPartSerializer, UonMetaProvider, UonCommon {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "UonSerializer";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.uon.UonSerializer#UON_addBeanTypes UON_addBeanTypes}
	 * 	<li><b>Name:</b>  <js>"UonSerializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>UonSerializer.addBeanTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>UONSERIALIZER_ADDBEANTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.uon.annotation.UonConfig#addBeanTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.uon.UonSerializerBuilder#addBeanTypes()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String UON_addBeanTypes = PREFIX + ".addBeanTypes.b";

	/**
	 * Configuration property:  Encode non-valid URI characters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.uon.UonSerializer#UON_encoding UON_encoding}
	 * 	<li><b>Name:</b>  <js>"UonSerializer.encoding.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>UonSerializer.encoding</c>
	 * 	<li><b>Environment variable:</b>  <c>UONSERIALIZER_ENCODING</c>
	 * 	<li><b>Default:</b>  <jk>false</jk> for {@link org.apache.juneau.uon.UonSerializer}, <jk>true</jk> for {@link org.apache.juneau.urlencoding.UrlEncodingSerializer}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.uon.annotation.UonConfig#encoding()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.uon.UonSerializerBuilder#encoding(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.uon.UonSerializerBuilder#encoding()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 *
	 * <p>
	 * If <jk>true</jk>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * <br>Set to <jk>false</jk> if parameter value is being passed to some other code that will already perform
	 * URL-encoding of non-valid URI characters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a non-encoding UON serializer.</jc>
	 * 	UonSerializer s1 = UonSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.build();
	 *
	 * 	<jc>// Create an encoding UON serializer.</jc>
	 * 	UonSerializer s2 = UonSerializer.
	 * 		.<jsm>create</jsm>()
	 * 		.encoding()
	 * 		.build();
	 *
	 * 	OMap m = OMap.<jsm>of</jsm>(<js>"foo"</js>, <js>"foo bar"</js>);
	 *
	 * 	<jc>// Produces: "(foo=foo bar)"</jc>
	 * 	String uon1 = s1.serialize(m)
	 *
	 * 	<jc>// Produces: "(foo=foo%20bar)"</jc>
	 * 	String uon2 = s2.serialize(m)
	 * </p>
	 */
	public static final String UON_encoding = PREFIX + ".encoding.b";

	/**
	 * Configuration property:  Format to use for query/form-data/header values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.uon.UonSerializer#UON_paramFormat UON_paramFormat}
	 * 	<li><b>Name:</b>  <js>"UonSerializer.paramFormat.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.uon.ParamFormat}
	 * 	<li><b>System property:</b>  <c>UonSerializer.paramFormat</c>
	 * 	<li><b>Environment variable:</b>  <c>UONSERIALIZER_PARAMFORMAT</c>
	 * 	<li><b>Default:</b>  <js>"UON"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.uon.annotation.UonConfig#paramFormat()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.uon.UonSerializerBuilder#paramFormat(ParamFormat)}
	 * 			<li class='jm'>{@link org.apache.juneau.uon.UonSerializerBuilder#paramFormatPlain()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 *
	 * <p>
	 * Possible values:
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link ParamFormat#UON} (default) - Use UON notation for parameters.
	 * 	<li class='jf'>{@link ParamFormat#PLAINTEXT} - Use plain text for parameters.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a normal UON serializer.</jc>
	 * 	UonSerializer s1 = UonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.build();
	 *
	 * 	<jc>// Create a plain-text UON serializer.</jc>
	 * 	UonSerializer s2 = UonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.paramFormat(<jsf>PLAIN_TEXT</jsf>)
	 * 		.build();
	 *
	 * 	OMap m = OMap.<jsm>of</jsm>(
	 * 		<js>"foo"</js>, <js>"bar"</js>,
	 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
	 * 	);
	 *
	 * 	<jc>// Produces: "(foo=bar,baz=@(qux,'true','123'))"</jc>
	 * 	String uon1 = s1.serialize(m)
	 *
	 * 	<jc>// Produces: "foo=bar,baz=qux,true,123"</jc>
	 * 	String uon2 = s2.serialize(m)
	 * </p>
	 */
	public static final String UON_paramFormat = PREFIX + ".paramFormat.s";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonSerializer}, all default settings. */
	public static final UonSerializer DEFAULT = new UonSerializer(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UonSerializer.Readable}. */
	public static final UonSerializer DEFAULT_READABLE = new Readable(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UonSerializer.Encoding}. */
	public static final UonSerializer DEFAULT_ENCODING = new Encoding(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Equivalent to <code>UonSerializer.<jsm>create</jsm>().ws().build();</code>.
	 */
	public static class Readable extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore ps) {
			super(ps.builder().setDefault(WSERIALIZER_useWhitespace, true).build());
		}
	}

	/**
	 * Equivalent to <code>UonSerializer.<jsm>create</jsm>().encoding().build();</code>.
	 */
	public static class Encoding extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Encoding(PropertyStore ps) {
			super(ps.builder().setDefault(UON_encoding, true).build());
		}
	}

	/**
	 * Converts the specified value to a string that can be used as an HTTP header value, query parameter value,
	 * form-data parameter, or URI path variable.
	 *
	 * <p>
	 * Returned values should NOT be URL-encoded.
	 *
	 * @param partType The category of value being serialized.
	 * @param schema
	 * 	Schema information about the part.
	 * 	<br>May be <jk>null</jk>.
	 * 	<br>Not all part serializers use the schema information.
	 * @param value The value being serialized.
	 * @return The serialized value.
	 * @throws SerializeException If a problem occurred while trying to parse the input.
	 * @throws SchemaValidationException If the output fails schema validation.
	 */
	public String serialize(HttpPartType partType, HttpPartSchema schema, Object value) throws SchemaValidationException, SerializeException {
		return createPartSession(null).serialize(partType, schema, value);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean
		encoding,
		addBeanTypes;

	private final char
		quoteChar;

	private final ParamFormat
		paramFormat;

	private final Map<ClassMeta<?>,UonClassMeta> uonClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UonBeanPropertyMeta> uonBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public UonSerializer(PropertyStore ps) {
		this(ps, "text/uon", (String)null);
	}

	/**
	 * No-arg constructor.
	 */
	public UonSerializer() {
		this(PropertyStore.DEFAULT, "text/uon", (String)null);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc RFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public UonSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
		encoding = getBooleanProperty(UON_encoding, false);
		addBeanTypes = getBooleanProperty(UON_addBeanTypes, getBooleanProperty(SERIALIZER_addBeanTypes, false));
		paramFormat = getProperty(UON_paramFormat, ParamFormat.class, ParamFormat.UON);
		quoteChar = getStringProperty(WSERIALIZER_quoteChar, "'").charAt(0);
	}

	@Override /* Context */
	public UonSerializerBuilder builder() {
		return new UonSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UonSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UonSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link UonSerializerBuilder} object.
	 */
	public static UonSerializerBuilder create() {
		return new UonSerializerBuilder();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Entry point methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public  UonSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public UonSerializerSession createSession(SerializerSessionArgs args) {
		return new UonSerializerSession(this, null, args);
	}

	@Override /* HttpPartSerializer */
	public UonSerializerSession createPartSession(SerializerSessionArgs args) {
		return new UonSerializerSession(this, null, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* UonMetaProvider */
	public UonClassMeta getUonClassMeta(ClassMeta<?> cm) {
		UonClassMeta m = uonClassMetas.get(cm);
		if (m == null) {
			m = new UonClassMeta(cm, this);
			uonClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* UonMetaProvider */
	public UonBeanPropertyMeta getUonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UonBeanPropertyMeta.DEFAULT;
		UonBeanPropertyMeta m = uonBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new UonBeanPropertyMeta(bpm.getDelegateFor(), this);
			uonBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see #UON_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Encode non-valid URI characters.
	 *
	 * @see #UON_encoding
	 * @return
	 * 	<jk>true</jk> if non-valid URI characters should be encoded with <js>"%xx"</js> constructs.
	 */
	protected final boolean isEncoding() {
		return encoding;
	}

	/**
	 * Format to use for query/form-data/header values.
	 *
	 * @see #UON_paramFormat
	 * @return
	 * 	Specifies the format to use for URL GET parameter keys and values.
	 */
	protected final ParamFormat getParamFormat() {
		return paramFormat;
	}

	/**
	 * Quote character.
	 *
	 * @see WriterSerializer#WSERIALIZER_quoteChar
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	@Override
	protected final char getQuoteChar() {
		return quoteChar;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("UonSerializer", new DefaultFilteringOMap()
				.a("encoding", encoding)
				.a("addBeanTypes", addBeanTypes)
				.a("paramFormat", paramFormat)
			);
	}
}
