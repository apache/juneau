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
package org.apache.juneau.urlencoding;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;

/**
 * Serializes POJO models to URL-encoded notation with UON-encoded values (a notation for URL-encoded query paramter values).
 *
 * <h5 class='section'>Media types:</h5>
 *
 * Handles <code>Accept</code> types: <code>application/x-www-form-urlencoded</code>
 * <p>
 * Produces <code>Content-Type</code> types: <code>application/x-www-form-urlencoded</code>
 *
 * <h5 class='section'>Description:</h5>
 *
 * This serializer provides several serialization options.  Typically, one of the predefined DEFAULT serializers will be sufficient.
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
 * Using the "strict" syntax defined in this document, the equivalent URL-encoded notation would be as follows:
 * <p class='bcode'>
 * 	<ua>id</ua>=<un>1</un>
 * 	&amp;<ua>name</ua>=<us>'John+Smith'</us>,
 * 	&amp;<ua>uri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 	&amp;<ua>addressBookUri</ua>=<us>http://sample/addressBook</us>,
 * 	&amp;<ua>birthDate</ua>=<us>1946-08-12T00:00:00Z</us>,
 * 	&amp;<ua>otherIds</ua>=<uk>null</uk>,
 * 	&amp;<ua>addresses</ua>=@(
 * 		(
 * 			<ua>uri</ua>=<us>http://sample/addressBook/address/1</us>,
 * 			<ua>personUri</ua>=<us>http://sample/addressBook/person/1</us>,
 * 			<ua>id</ua>=<un>1</un>,
 * 			<ua>street</ua>=<us>'100+Main+Street'</us>,
 * 			<ua>city</ua>=<us>Anywhereville</us>,
 * 			<ua>state</ua>=<us>NY</us>,
 * 			<ua>zip</ua>=<un>12345</un>,
 * 			<ua>isCurrent</ua>=<uk>true</uk>
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
 * 	<jc>// Produces "a=b&amp;c=1&amp;d=false&amp;e=@(f,1,false)&amp;g=(h=i)"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
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
 * 	Person p = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>, <js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=(street='123+Main+St',city=Anywhere,state=NY,zip=12345)&amp;deceased=false"</jc>
 * 	String s = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(s);
 * </p>
 */
public class UrlEncodingSerializer extends UonSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "UrlEncodingSerializer.";

	/**
	 * Configuration property:  Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"UrlEncodingSerializer.expandedParams.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk> 
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>false</jk>, serializing the array <code>[1,2,3]</code> results in <code>?key=$a(1,2,3)</code>.
	 * If <jk>true</jk>, serializing the same array results in <code>?key=1&amp;key=2&amp;key=3</code>.
	 *
	 * <p>
	 * Example:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> String[] f1 = {<js>"a"</js>,<js>"b"</js>};
	 * 		<jk>public</jk> List&lt;String&gt; f2 = <jk>new</jk> LinkedList&lt;String&gt;(Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>}));
	 * 	}
	 *
	 * 	UrlEncodingSerializer s1 = UrlEncodingSerializer.<jsf>DEFAULT</jsf>;
	 * 	UrlEncodingSerializer s2 = <jk>new</jk> UrlEncodingSerializerBuilder().expandedParams(<jk>true</jk>).build();
	 *
	 * 	String ss1 = s1.serialize(<jk>new</jk> A()); <jc>// Produces "f1=(a,b)&amp;f2=(c,d)"</jc>
	 * 	String ss2 = s2.serialize(<jk>new</jk> A()); <jc>// Produces "f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</jc>
	 * </p>
	 *
	 * <p>
	 * This option only applies to beans.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>If parsing multi-part parameters, it's highly recommended to use <code>Collections</code> or <code>Lists</code>
	 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
	 * 		is added to it.
	 * </ul>
	 */
	public static final String URLENC_expandedParams = PREFIX + "expandedParams.b";
	

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UrlEncodingSerializer.PlainText}. */
	public static final UrlEncodingSerializer DEFAULT_PLAINTEXT = new PlainText(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UrlEncodingSerializer.Expanded}. */
	public static final UrlEncodingSerializer DEFAULT_EXPANDED = new Expanded(PropertyStore.DEFAULT);

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializerBuilder().expandedParams(<jk>true</jk>).build();</code>.
	 */
	public static class Expanded extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Expanded(PropertyStore ps) {
			super(ps.builder().set(URLENC_expandedParams, true).build());
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializerBuilder().useWhitespace(<jk>true</jk>).build();</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Readable(PropertyStore ps) {
			super(ps.builder().set(SERIALIZER_useWhitespace, true).build());
		}
	}

	/**
	 * Equivalent to <code><jk>new</jk> UrlEncodingSerializerBuilder().plainTextParts().build();</code>.
	 */
	public static class PlainText extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public PlainText(PropertyStore ps) {
			super(ps.builder().set(UON_paramFormat, "PLAINTEXT").build());
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		expandedParams;

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public UrlEncodingSerializer(PropertyStore ps) {
		this(ps, "application/x-www-form-urlencoded");
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
	public UrlEncodingSerializer(PropertyStore ps, String produces, String...accept) {
		super(
			ps.builder()
				.set(UON_encodeChars, true)
				.build(), 
			produces, 
			accept
		);
		expandedParams = getProperty(URLENC_expandedParams, boolean.class, false);
	}

	@Override /* Context */
	public UrlEncodingSerializerBuilder builder() {
		return new UrlEncodingSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link UrlEncodingSerializerBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> UrlEncodingSerializerBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link UrlEncodingSerializerBuilder} object.
	 */
	public static UrlEncodingSerializerBuilder create() {
		return new UrlEncodingSerializerBuilder();
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new UrlEncodingSerializerSession(this, null, args);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("UrlEncodingSerializer", new ObjectMap()
				.append("expandedParams", expandedParams)
			);
	}
}
