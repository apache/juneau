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

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Serializes POJO models to UON (a notation for URL-encoded query parameter values).
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>text/uon</code>
 *
 * <p>
 * Produces <code>Content-Type</code> types: <code>text/uon</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * The following shows a sample object defined in Javascript:
 * <p class='bcode'>
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
 * <p class='bcode'>
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
 * <p class='bcode'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map m = <jk>new</jk> ObjectMap(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
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
public class UonSerializer extends WriterSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "UonSerializer.";

	/**
	 * <b>Configuration property:</b>  Encode non-valid URI characters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.encodeChars"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk> for {@link UonSerializer}, <jk>true</jk> for {@link UrlEncodingSerializer}
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Encode non-valid URI characters with <js>"%xx"</js> constructs.
	 *
	 * <p>
	 * If <jk>true</jk>, non-valid URI characters will be converted to <js>"%xx"</js> sequences.
	 * Set to <jk>false</jk> if parameter value is being passed to some other code that will already perform
	 * URL-encoding of non-valid URI characters.
	 */
	public static final String UON_encodeChars = PREFIX + "encodeChars";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UonSerializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined from
	 * the value type.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String UON_addBeanTypeProperties = PREFIX + "addBeanTypeProperties";

	/**
	 * <b>Configuration property:</b>  Format to use for query/form-data/header values.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"UrlEncodingSerializer.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>ParamFormat</code>
	 * 	<li><b>Default:</b> <jsf>UON</jsf>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 *
	 * <p>
	 * Specifies the format to use for URL GET parameter keys and values.
	 */
	public static final String UON_paramFormat = PREFIX + "paramFormat";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UonSerializer}, all default settings. */
	public static final UonSerializer DEFAULT = new UonSerializer(PropertyStore.create());

	/** Reusable instance of {@link UonSerializer.Readable}. */
	public static final UonSerializer DEFAULT_READABLE = new Readable(PropertyStore.create());

	/** Reusable instance of {@link UonSerializer.Encoding}. */
	public static final UonSerializer DEFAULT_ENCODING = new Encoding(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializerBuilder().ws().build();</code>.
	 */
	public static class Readable extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore propertyStore) {
			super(propertyStore.copy().append(SERIALIZER_useWhitespace, true));
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UonSerializerBuilder().encoding().build();</code>.
	 */
	public static class Encoding extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Encoding(PropertyStore propertyStore) {
			super(propertyStore.copy().append(UON_encodeChars, true));
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final UonSerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 */
	public UonSerializer(PropertyStore propertyStore) {
		this(propertyStore, "text/uon");
	}

	/**
	 * Constructor.
	 *
	 * @param propertyStore
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	public UonSerializer(PropertyStore propertyStore, String produces, String...accept) {
		super(propertyStore, produces, accept);
		this.ctx = createContext(UonSerializerContext.class);
	}

	@Override /* CoreObject */
	public UonSerializerBuilder builder() {
		return new UonSerializerBuilder(propertyStore);
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


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new UonSerializerSession(ctx, null, args);
	}
}
