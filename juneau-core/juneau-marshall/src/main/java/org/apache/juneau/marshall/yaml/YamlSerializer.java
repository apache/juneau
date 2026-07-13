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
package org.apache.juneau.marshall.yaml;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.stream.*;

/**
 * Serializes POJO models to YAML.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Accept</c> types:  <bc>application/yaml, text/yaml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/yaml</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * The conversion is as follows...
 * <ul class='spaced-list'>
 * 	<li>
 * 		Maps (e.g. {@link HashMap HashMaps}, {@link TreeMap TreeMaps}) are converted to YAML mappings.
 * 	<li>
 * 		Collections (e.g. {@link HashSet HashSets}, {@link LinkedList LinkedLists}) and Java arrays are converted to
 * 		YAML sequences.
 * 	<li>
 * 		{@link String Strings} are converted to YAML scalars.
 * 	<li>
 * 		{@link Number Numbers} (e.g. {@link Integer}, {@link Long}, {@link Double}) are converted to YAML numbers.
 * 	<li>
 * 		{@link Boolean Booleans} are converted to YAML booleans.
 * 	<li>
 * 		{@code nulls} are converted to YAML nulls.
 * 	<li>
 * 		{@code arrays} are converted to YAML sequences.
 * 	<li>
 * 		{@code beans} are converted to YAML mappings.
 * </ul>
 *
 * <p>
 * The types above are considered "YAML-primitive" object types.
 * Any non-YAML-primitive object types are transformed into YAML-primitive object types through
 * {@link org.apache.juneau.marshall.swap.ObjectSwap ObjectSwaps} associated through the
 * {@link org.apache.juneau.marshall.MarshallingContext.Builder#swaps(Class...)} method.
 * Several default transforms are provided for transforming Dates, Enums, Iterators, etc...
 *
 * <p>
 * This serializer provides several serialization options.
 * Typically, one of the predefined DEFAULT serializers will be sufficient.
 * However, custom serializers can be constructed to fine-tune behavior.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Use one of the default serializers to serialize a POJO</jc>
 * 	String <jv>yaml</jv> = YamlSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>someObject</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	YamlSerializer <jv>serializer</jv> = YamlSerializer.<jsm>create</jsm>().build();
 *
 * 	<jc>// Clone an existing serializer and modify it to use whitespace</jc>
 * 	<jv>serializer</jv> = YamlSerializer.<jsf>DEFAULT</jsf>.copy().ws().build();
 *
 * 	<jc>// Serialize a POJO to YAML</jc>
 * 	String <jv>yaml</jv> = <jv>serializer</jv>.serialize(<jv>someObject</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bcode'>
 * 	name: Alice
 * 	age: 30
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bcode'>
 * 	name: Alice
 * 	age: 30
 * 	address:
 * 	  street: 123 Main St
 * 	  city: Boston
 * 	  state: MA
 * 	tags:
 * 	- a
 * 	- b
 * 	- c
 * </p>
 *
 * <h5 class='section'>Limitations compared to JSON</h5>
 * <p>
 * The YAML serializer has fewer configuration options than {@link JsonSerializer}:
 * <ul class='spaced-list'>
 * 	<li>
 * 		No compact single-line output mode; YAML is always emitted in block-style (indentation-based) format.
 * 	<li>
 * 		No equivalent to JSON's simple mode or attribute quoting style variants (single vs double quotes).
 * 	<li>
 * 		No strict vs lax output modes; YAML output follows a consistent, human-readable style.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/YamlSupport">YAML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource" // Closeable resources are owned by the caller's serializer session; Eclipse JDT @Owning warning is by design.
})
public class YamlSerializer extends WriterSerializer implements RecordWritable, ArrayRecordWritable {

	private static final String PROP_addBeanTypesYaml = "addBeanTypesYaml";

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder<Builder> {

		private static final Cache<HashKey,YamlSerializer> CACHE = Cache.of(HashKey.class, YamlSerializer.class).build();

		private boolean addBeanTypesYaml;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("application/yaml");
			accept("application/yaml,text/yaml");
			addBeanTypesYaml = env("YamlSerializer.addBeanTypes", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesYaml = copyFrom.addBeanTypesYaml;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(YamlSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			addBeanTypesYaml = copyFrom.addBeanTypesYaml;
		}

		/**
		 * Add <js>"_type"</js> properties when needed.
		 *
		 * <p>
		 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
		 * through reflection.
		 *
		 * <p>
		 * When present, this value overrides the {@link org.apache.juneau.marshall.serializer.Serializer.Builder#addBeanTypes()} setting and is
		 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
		 *
		 * @return This object.
		 */
		public Builder addBeanTypesYaml() {
			return addBeanTypesYaml(true);
		}

		/**
		 * Same as {@link #addBeanTypesYaml()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder addBeanTypesYaml(boolean value) {
			addBeanTypesYaml = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public YamlSerializer build() {
			return cache(CACHE).build(YamlSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			return HashKey.of(
				super.hashKey(),
				addBeanTypesYaml
			);
		}


	}

	/** Default serializer, with whitespace. */
	public static class Readable extends YamlSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, all default settings. */
	public static final YamlSerializer DEFAULT = new YamlSerializer(create());

	/** Default serializer, with whitespace. */
	public static final YamlSerializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	protected final boolean addBeanTypesYaml;

	private final boolean addBeanTypes2;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public YamlSerializer(Builder builder) {
		super(builder);
		addBeanTypesYaml = builder.addBeanTypesYaml;
		addBeanTypes2 = addBeanTypesYaml || super.isAddBeanTypes();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public YamlSerializerSession.Builder createSession() {
		return YamlSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public YamlSerializerSession getSession() { return createSession().build(); }

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see Builder#addBeanTypesYaml()
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() { return addBeanTypes2; }

	@Override /* Overridden from WriterSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_addBeanTypesYaml, addBeanTypesYaml);
	}

	/**
	 * Convenience delegator for the whole-value {@link RecordWriter} using <b>default session
	 * arguments</b>.  The real implementation lives on
	 * {@link YamlSerializerSession#serializeRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* RecordWritable */
	public RecordWriter serializeRecords(Object output) throws IOException {
		return getSession().serializeRecords(output);
	}

	/**
	 * Convenience delegator for the buffered array-element {@link RecordWriter} using <b>default
	 * session arguments</b>.  The real implementation lives on
	 * {@link YamlSerializerSession#serializeArrayRecords(Object)}.
	 *
	 * @param output The output.
	 * @return A new {@link RecordWriter}.
	 * @throws IOException If a problem occurred opening the underlying output.
	 */
	@Override /* ArrayRecordWritable */
	public RecordWriter serializeArrayRecords(Object output) throws IOException {
		return getSession().serializeArrayRecords(output);
	}

	/**
	 * The YAML record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* RecordWritable */
	public boolean isRecordStreaming() { return false; }

	/**
	 * The YAML array-record writer is buffered/{@link RecordAdapter}-backed, not O(1) streaming.
	 *
	 * @return Always <jk>false</jk>.
	 */
	@Override /* ArrayRecordWritable */
	public boolean isArrayRecordStreaming() { return false; }
}
