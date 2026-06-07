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
package org.apache.juneau.uon;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to UON (a notation for URL-encoded query parameter values).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>text/uon</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/uon</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
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
 * Using the "strict" syntax defined in this document, the equivalent UON notation would be as follows:
 * <p class='buon'>
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
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30)
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	(name=Alice,age=30,address=(street=123+Main+St,city=Boston,state=MA),tags=@(a,b,c))
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a Map</jc>
 * 	Map <jv>map</jv> = JsonMap.<jsm>ofText</jsm>(<js>"{a:'b',c:1,d:false,e:['f',1,false],g:{h:'i'}}"</js>);
 *
 * 	<jc>// Serialize to value equivalent to JSON.</jc>
 * 	<jc>// Produces "(a=b,c=1,d=false,e=@(f,1,false),g=(h=i))"</jc>
 * 	String <jv>uon</jv> = UonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>map</jv>);
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
 * 	Person <jv>person</jv> = <jk>new</jk> Person(<js>"John Doe"</js>, 23, <js>"123 Main St"</js>, <js>"Anywhere"</js>,
 * 		<js>"NY"</js>, 12345, <jk>false</jk>);
 *
 * 	<jc>// Produces "(name='John Doe',age=23,address=(street='123 Main St',city=Anywhere,state=NY,zip=12345),deceased=false)"</jc>
 * 	String <jv>uon</jv> = UonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>person</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UonBasics">UON Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class UonSerializer extends WriterSerializer implements HttpPartSerializer, UonMetaProvider {

	// Property name constants
	private static final String PROP_addBeanTypes = "addBeanTypes";
	private static final String PROP_encoding = "encoding";
	private static final String PROP_paramFormat = "paramFormat";

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends WriterSerializer.Builder<SELF> {

		private static final Cache<HashKey,UonSerializer> CACHE = Cache.of(HashKey.class, UonSerializer.class).build();

		private boolean addBeanTypesUon;
		private boolean encoding;
		private ParamFormat paramFormat;
		private Character quoteCharUon;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/uon");
			addBeanTypesUon = env("UonSerializer.addBeanTypesUon", false);
			encoding = env("UonSerializer.encoding", false);
			paramFormat = env("UonSerializer.paramFormat", ParamFormat.UON);
			quoteCharUon = null;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesUon = copyFrom.addBeanTypesUon;
			encoding = copyFrom.encoding;
			paramFormat = copyFrom.paramFormat;
			quoteCharUon = copyFrom.quoteCharUon;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(UonSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesUon = copyFrom.addBeanTypesUon;
			encoding = copyFrom.encoding;
			paramFormat = copyFrom.paramFormat;
			quoteCharUon = copyFrom.quoteCharUon;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public SELF addBeanTypesUon() {
			return addBeanTypesUon(true);
		}

		/**
		 * Same as {@link #addBeanTypesUon()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF addBeanTypesUon(boolean value) {
			addBeanTypesUon = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public UonSerializer build() {
			return cache(CACHE).build(UonSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		/**
		 * Encode non-valid URI characters.
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
		 * <p class='bjava'>
		 * 	<jc>// Create a non-encoding UON serializer.</jc>
		 * 	UonSerializer <jv>serializer1</jv> = UonSerializer.
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create an encoding UON serializer.</jc>
		 * 	UonSerializer <jv>serializer2</jv> = UonSerializer.
		 * 		.<jsm>create</jsm>()
		 * 		.encoding()
		 * 		.build();
		 *
		 * 	JsonMap <jv>map</jv> = JsonMap.<jsm>of</jsm>(<js>"foo"</js>, <js>"foo bar"</js>);
		 *
		 * 	<jc>// Produces: "(foo=foo bar)"</jc>
		 * 	String <jv>uon1</jv> = <jv>serializer1</jv>.serialize(<jv>map</jv>)
		 *
		 * 	<jc>// Produces: "(foo=foo%20bar)"</jc>
		 * 	String <jv>uon2</jv> = <jv>serializer2</jv>.serialize(<jv>map</jv>)
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF encoding() {
			return encoding(true);
		}

		/**
		 * Same as {@link #encoding()} but allows you to disable the previous setting.
		 *
		 * @param value The new value for this setting.
		 * @return This object.
		 */
		public SELF encoding(boolean value) {
			encoding = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				addBeanTypesUon,
				encoding,
				paramFormat,
				quoteCharUon
			);
			// @formatter:on
		}

		/**
		 * Format to use for query/form-data/header values.
		 *
		 * <p>
		 * Specifies the format to use for URL GET parameter keys and values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a normal UON serializer.</jc>
		 * 	UonSerializer <jv>serializer1</jv> = UonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create a plain-text UON serializer.</jc>
		 * 	UonSerializer <jv>serializer2</jv> = UonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.paramFormat(<jsf>PLAIN_TEXT</jsf>)
		 * 		.build();
		 *
		 * 	JsonMap <jv>map</jv> = JsonMap.<jsm>of</jsm>(
		 * 		<js>"foo"</js>, <js>"bar"</js>,
		 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
		 * 	);
		 *
		 * 	<jc>// Produces: "(foo=bar,baz=@(qux,'true','123'))"</jc>
		 * 	String <jv>uon1</jv> = <jv>serializer1</jv>.serialize(<jv>map</jv>)
		 *
		 * 	<jc>// Produces: "foo=bar,baz=qux,true,123"</jc>
		 * 	String <jv>uon2</jv> = <jv>serializer2</jv>.serialize(<jv>map</jv>)
		 * </p>
		 *
		 * <p>
		 * <ul class='values javatree'>
		 * 	<li class='jf'>{@link ParamFormat#UON} (default) - Use UON notation for parameters.
		 * 	<li class='jf'>{@link ParamFormat#PLAINTEXT} - Use plain text for parameters.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default value is {@link ParamFormat#UON}.
		 * 	<br>Cannot be <jk>null</jk>.
		 * @return This object.
		 */
		public SELF paramFormat(ParamFormat value) {
			paramFormat = assertArgNotNull(ARG_value, value);
			return self();
		}

		/**
		 * Format to use for query/form-data/header values.
		 *
		 * <p>
		 * Specifies plain-text for the format to use for URL GET parameter keys and values.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a plain-text UON serializer.</jc>
		 * 	UonSerializer <jv>serializer</jv> = UonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.paramFormatPlain()
		 * 		.build();
		 *
		 * 	JsonMap <jv>map</jv> = JsonMap.<jsm>of</jsm>(
		 * 		<js>"foo"</js>, <js>"bar"</js>,
		 * 		<js>"baz"</js>, <jk>new</jk> String[]{<js>"qux"</js>, <js>"true"</js>, <js>"123"</js>}
		 * 	);
		 *
		 * 	<jc>// Produces: "foo=bar,baz=qux,true,123"</jc>
		 * 	String <jv>uon</jv> = <jv>serializer</jv>.serialize(<jv>map</jv>)
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF paramFormatPlain() {
			return paramFormat(ParamFormat.PLAINTEXT);
		}

		/**
		 * Specifies the quote character.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF quoteCharUon(char value) {
			quoteCharUon = value;
			return self();
		}


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link UonSerializer#create()} / {@link UonSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(UonSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Equivalent to <code>UonSerializer.<jsm>create</jsm>().encoding().build();</code>.
	 */
	public static class Encoding extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Encoding(Builder<?> builder) {
			super(builder.encoding());
		}
	}

	/**
	 * Equivalent to <code>UonSerializer.<jsm>create</jsm>().ws().build();</code>.
	 */
	public static class Readable extends UonSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder<?> builder) {
			super(builder.useWhitespace());
		}
	}

	/** Reusable instance of {@link UonSerializer}, all default settings. */
	public static final UonSerializer DEFAULT = new UonSerializer(create());
	/** Reusable instance of {@link UonSerializer.Readable}. */
	public static final UonSerializer DEFAULT_READABLE = new Readable(create());

	/** Reusable instance of {@link UonSerializer.Encoding}. */
	public static final UonSerializer DEFAULT_ENCODING = new Encoding(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	protected final boolean addBeanTypesUon;
	protected final boolean encoding;
	protected final Character quoteCharUon;
	protected final ParamFormat paramFormat;
	private final boolean addBeanTypes2;

	private final char quoteChar2;
	private final Map<BeanPropertyMeta,UonBeanPropertyMeta> uonBeanPropertyMetas = new ConcurrentHashMap<>();
	private final Map<ClassMeta<?>,UonClassMeta> uonClassMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	public UonSerializer(Builder<?> builder) {
		super(builder);

		addBeanTypesUon = builder.addBeanTypesUon;
		encoding = builder.encoding;
		paramFormat = builder.paramFormat;
		quoteCharUon = builder.quoteCharUon;

		addBeanTypes2 = addBeanTypesUon || super.isAddBeanTypes();
		if (nn(quoteCharUon)) {
			quoteChar2 = quoteCharUon;
		} else if (nn(super.quoteChar())) {
			quoteChar2 = super.quoteChar();
		} else {
			quoteChar2 = '\'';
		}
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public UonSerializerSession.Builder<?> createSession() {
		return UonSerializerSession.create(this);
	}

	@Override /* Overridden from HttpPartSerializer */
	public UonSerializerSession getPartSession() { return UonSerializerSession.create(this).build(); }

	@Override /* Overridden from Context */
	public UonSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from UonMetaProvider */
	public UonBeanPropertyMeta getUonBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UonBeanPropertyMeta.DEFAULT;
		return uonBeanPropertyMetas.computeIfAbsent(bpm, k -> new UonBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from UonMetaProvider */
	public UonClassMeta getUonClassMeta(ClassMeta<?> cm) {
		return uonClassMetas.computeIfAbsent(cm, k -> new UonClassMeta(k, this));
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
		return getPartSession().serialize(partType, schema, value);
	}

	/**
	 * Format to use for query/form-data/header values.
	 *
	 * @see Builder#paramFormat(ParamFormat)
	 * @return
	 * 	Specifies the format to use for URL GET parameter keys and values.
	 */
	protected final ParamFormat getParamFormat() { return paramFormat; }

	/**
	 * Quote character.
	 *
	 * @see Builder#quoteCharUon(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	@Override
	protected final char getQuoteChar() { return quoteChar2; }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesUon()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypes2; }

	/**
	 * Encode non-valid URI characters.
	 *
	 * @see Builder#encoding()
	 * @return
	 * 	<jk>true</jk> if non-valid URI characters should be encoded with <js>"%xx"</js> constructs.
	 */
	protected final boolean isEncoding() { return encoding; }

	@Override /* Overridden from WriterSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypes, addBeanTypes2)
			.a(PROP_encoding, encoding)
			.a(PROP_paramFormat, paramFormat);
	}
}