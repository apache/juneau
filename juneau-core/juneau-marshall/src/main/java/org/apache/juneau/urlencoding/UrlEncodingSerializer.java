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
package org.apache.juneau.urlencoding;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * Serializes POJO models to URL-encoded notation with UON-encoded values (a notation for URL-encoded query paramter values).
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/x-www-form-urlencoded</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/x-www-form-urlencoded</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * This serializer provides several serialization options.
 * <br>Typically, one of the predefined DEFAULT serializers will be sufficient.
 * <br>However, custom serializers can be constructed to fine-tune behavior.
 *
 * <p>
 * The following shows a sample object defined in Javascript:
 * <p class='bjson'>
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
 * <p class='burlenc'>
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
 * <p class='bjava'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofText</jsm>(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "a=b&amp;c=1&amp;d=false&amp;e=@(f,1,false)&amp;g=(h=i)"</jc>
 * 	String <jv>uenc</jv> = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>map</jv>);
 *
 * 	<jc>// Serialize a bean</jc>
 * 	<jk>public class</jk> Person {
 * 		<jk>public</jk> Person(String <jv>name</jv>);
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
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>, <js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "name=John+Doe&amp;age=23&amp;address=(street='123+Main+St',city=Anywhere,state=NY,zip=12345)&amp;deceased=false"</jc>
 * 	String <jv>uenc</jv> = UrlEncodingSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingBasics">URL-Encoding Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class UrlEncodingSerializer extends UonSerializer implements UrlEncodingMetaProvider {

	// Property name constants
	private static final String PROP_expandedParams = "expandedParams";

	/**
	 * Builder class.
	 */
	public static class Builder extends UonSerializer.Builder<Builder> {

		private static final Cache<HashKey,UrlEncodingSerializer> CACHE = Cache.of(HashKey.class, UrlEncodingSerializer.class).build();

		boolean expandedParams;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/x-www-form-urlencoded");
			expandedParams = env("UrlEncoding.expandedParams", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			expandedParams = copyFrom.expandedParams;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(UrlEncodingSerializer copyFrom) {
			super(copyFrom);
			expandedParams = copyFrom.expandedParams;
		}

		@Override /* Overridden from Context.Builder<?> */
		public UrlEncodingSerializer build() {
			return cache(CACHE).build(UrlEncodingSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Serialize bean property collections/arrays as separate key/value pairs.
		 *
		 * <p>
		 * By default, serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
		 * <br>When enabled, serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
		 *
		 * <p>
		 * This option only applies to beans.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		If parsing multi-part parameters, it's highly recommended to use <c>Collections</c> or <c>Lists</c>
		 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
		 * 		is added to it.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A sample bean.</jc>
		 * 	<jk>public class</jk> A {
		 * 		<jk>public</jk> String[] <jf>f1</jf> = {<js>"a"</js>,<js>"b"</js>};
		 * 		<jk>public</jk> List&lt;String&gt; <jf>f2</jf> = Arrays.<jsm>asList</jsm>(<jk>new</jk> String[]{<js>"c"</js>,<js>"d"</js>});
		 * 	}
		 *
		 * 	<jc>// Normal serializer.</jc>
		 * 	WriterSerializer <jv>serializer1</jv> = UrlEncodingSerializer.<jsf>DEFAULT</jsf>;
		 *
		 * 	<jc>// Expanded-params serializer.</jc>
		 * 	WriterSerializer <jv>serializer2</jv> = UrlEncodingSerializer.<jsm>create</jsm>().expandedParams().build();
		 *
		 *  <jc>// Produces "f1=(a,b)&amp;f2=(c,d)"</jc>
		 * 	String <jv>out1</jv> = <jv>serializer1</jv>.serialize(<jk>new</jk> A());
		 *
		 * 	<jc>// Produces "f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</jc>
		 * 	String <jv>out2</jv> = <jv>serializer2</jv>.serialize(<jk>new</jk> A());
		 * </p>
		 *
		 * @return This object.
		 */
		public Builder expandedParams() {
			return expandedParams(true);
		}

		/**
		 * Same as {@link #expandedParams()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder expandedParams(boolean value) {
			expandedParams = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				expandedParams
			);
			// @formatter:on
		}


	}

	/**
	 * Equivalent to <code>UrlEncodingSerializer.<jsm>create</jsm>().expandedParams().build();</code>.
	 */
	public static class Expanded extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Expanded(Builder builder) {
			super(builder.expandedParams());
		}
	}

	/**
	 * Equivalent to <code>UrlEncodingSerializer.<jsm>create</jsm>().plainTextParts().build();</code>.
	 */
	public static class PlainText extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public PlainText(Builder builder) {
			super(builder.paramFormatPlain());
		}
	}

	/**
	 * Equivalent to <code>UrlEncodingSerializer.<jsm>create</jsm>().useWhitespace().build();</code>.
	 */
	public static class Readable extends UrlEncodingSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Reusable instance of {@link UrlEncodingSerializer}, all default settings. */
	public static final UrlEncodingSerializer DEFAULT = new UrlEncodingSerializer(create());
	/** Reusable instance of {@link UrlEncodingSerializer.PlainText}. */
	public static final UrlEncodingSerializer DEFAULT_PLAINTEXT = new PlainText(create());

	/** Reusable instance of {@link UrlEncodingSerializer.Expanded}. */
	public static final UrlEncodingSerializer DEFAULT_EXPANDED = new Expanded(create());

	/** Reusable instance of {@link UrlEncodingSerializer.Readable}. */
	public static final UrlEncodingSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final boolean expandedParams;

	private final Map<ClassMeta<?>,UrlEncodingClassMeta> urlEncodingClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UrlEncodingBeanPropertyMeta> urlEncodingBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UrlEncodingSerializer(Builder builder) {
		super(builder.encoding());
		expandedParams = builder.expandedParams;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public UrlEncodingSerializerSession.Builder createSession() {
		return UrlEncodingSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public UrlEncodingSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from UrlEncodingMetaProvider */
	public UrlEncodingBeanPropertyMeta getUrlEncodingBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UrlEncodingBeanPropertyMeta.DEFAULT;
		return urlEncodingBeanPropertyMetas.computeIfAbsent(bpm, k -> new UrlEncodingBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from UrlEncodingMetaProvider */
	public UrlEncodingClassMeta getUrlEncodingClassMeta(ClassMeta<?> cm) {
		return urlEncodingClassMetas.computeIfAbsent(cm, k -> new UrlEncodingClassMeta(k, this));
	}

	/**
	 * Serialize bean property collections/arrays as separate key/value pairs.
	 *
	 * @see Builder#expandedParams()
	 * @return
	 * 	<jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * 	<br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() { return expandedParams; }

	@Override /* Overridden from UonSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_expandedParams, expandedParams);
	}
}