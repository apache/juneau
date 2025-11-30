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
package org.apache.juneau.junit.bct;

import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.reflect.ReflectionUtils.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Default implementation of {@link BeanConverter} for Bean-Centric Test (BCT) object conversion.
 *
 * <p>This class provides a comprehensive, extensible framework for converting Java objects to strings
 * and lists, with sophisticated property access capabilities. It's the core engine behind BCT testing
 * assertions, handling complex object introspection and value extraction with high performance through
 * intelligent caching and optimized lookup strategies.</p>
 *
 * <h5 class='section'>Key Features:</h5>
 * <ul>
 *    <li><b>Extensible Type Handlers:</b> Pluggable stringifiers, listifiers, and swappers for custom types</li>
 *    <li><b>Performance Optimization:</b> ConcurrentHashMap caching for type-to-handler mappings</li>
 *    <li><b>Comprehensive Defaults:</b> Built-in support for all common Java types and structures</li>
 *    <li><b>Configurable Settings:</b> Customizable formatting, delimiters, and display options</li>
 *    <li><b>Thread Safety:</b> Fully thread-safe implementation suitable for concurrent testing</li>
 * </ul>
 *
 * <h5 class='section'>Architecture Overview:</h5>
 * <p>The converter uses four types of pluggable handlers:</p>
 * <dl>
 *    <dt><b>Stringifiers:</b></dt>
 *    <dd>Convert objects to string representations with custom formatting rules</dd>
 *
 *    <dt><b>Listifiers:</b></dt>
 *    <dd>Convert collection-like objects to List&lt;Object&gt; for uniform iteration</dd>
 *
 *    <dt><b>Swappers:</b></dt>
 *    <dd>Pre-process objects before conversion (unwrap Optional, call Supplier, etc.)</dd>
 *
 *    <dt><b>PropertyExtractors:</b></dt>
 *    <dd>Define custom property access strategies for nested field navigation (e.g., <js>"user.address.city"</js>)</dd>
 * </dl>
 *
 * <p>PropertyExtractors use a chain-of-responsibility pattern, where each extractor in the chain
 * is tried until one can handle the property access. The framework includes built-in extractors for:</p>
 * <ul>
 *    <li><b>JavaBean properties:</b> Standard getter methods and public fields</li>
 *    <li><b>Collection/Array access:</b> Numeric indices and size/length properties</li>
 *    <li><b>Map access:</b> Key-based property retrieval and size property</li>
 * </ul>
 *
 * <h5 class='section'>Default Type Support:</h5>
 * <p>Out-of-the-box stringification support includes:</p>
 * <ul>
 *    <li><b>Collections:</b> List, Set, Queue → <js>"[item1,item2,item3]"</js> format</li>
 *    <li><b>Maps:</b> Map, Properties → <js>"{key1=value1,key2=value2}"</js> format</li>
 *    <li><b>Map Entries:</b> Map.Entry → <js>"key=value"</js> format</li>
 *    <li><b>Arrays:</b> All array types → <js>"[element1,element2]"</js> format</li>
 *    <li><b>Dates:</b> Date, Calendar → ISO-8601 format</li>
 *    <li><b>Files/Streams:</b> File, InputStream, Reader → content as hex or text</li>
 *    <li><b>Reflection:</b> Class, Method, Constructor → human-readable signatures</li>
 *    <li><b>Enums:</b> Enum values → name() format</li>
 * </ul>
 *
 * <p>Default listification support includes:</p>
 * <ul>
 *    <li><b>Collection types:</b> List, Set, Queue, and all subtypes</li>
 *    <li><b>Iterable objects:</b> Any Iterable implementation</li>
 *    <li><b>Iterators:</b> Iterator and Enumeration (consumed to list)</li>
 *    <li><b>Streams:</b> Stream objects (terminated to list)</li>
 *    <li><b>Optional:</b> Empty list or single-element list</li>
 *    <li><b>Maps:</b> Converted to list of Map.Entry objects</li>
 * </ul>
 *
 * <p>Default swapping support includes:</p>
 * <ul>
 *    <li><b>Optional:</b> Unwrapped to contained value or <jk>null</jk></li>
 *    <li><b>Supplier:</b> Called to get supplied value</li>
 *    <li><b>Future:</b> Extracts completed result or returns <js>"&lt;pending&gt;"</js> for incomplete futures (via {@link Swappers#futureSwapper()})</li>
 * </ul>
 *
 * <h5 class='section'>Configuration Settings:</h5>
 * <p>The converter supports extensive customization via settings:</p>
 * <dl>
 *    <dt><code>nullValue</code></dt>
 *    <dd>String representation for null values (default: <js>"&lt;null&gt;"</js>)</dd>
 *
 *    <dt><code>selfValue</code></dt>
 *    <dd>Special property name that returns the object itself (default: <js>"&lt;self&gt;"</js>)</dd>
 *
 *    <dt><code>emptyValue</code></dt>
 *    <dd>String representation for empty collections (default: <js>"&lt;empty&gt;"</js>)</dd>
 *
 *    <dt><code>fieldSeparator</code></dt>
 *    <dd>Delimiter between collection elements and map entries (default: <js>","</js>)</dd>
 *
 *    <dt><code>collectionPrefix/Suffix</code></dt>
 *    <dd>Brackets around collection content (default: <js>"["</js> and <js>"]"</js>)</dd>
 *
 *    <dt><code>mapPrefix/Suffix</code></dt>
 *    <dd>Brackets around map content (default: <js>"{"</js> and <js>"}"</js>)</dd>
 *
 *    <dt><code>mapEntrySeparator</code></dt>
 *    <dd>Separator between map keys and values (default: <js>"="</js>)</dd>
 *
 *    <dt><code>calendarFormat</code></dt>
 *    <dd>DateTimeFormatter for calendar objects (default: <jsf>ISO_INSTANT</jsf>)</dd>
 *
 *    <dt><code>classNameFormat</code></dt>
 *    <dd>Format for class names: <js>"simple"</js>, <js>"canonical"</js>, or <js>"full"</js> (default: <js>"simple"</js>)</dd>
 * </dl>
 *
 * <h5 class='section'>Usage Examples:</h5>
 *
 * <p><b>Basic Usage with Defaults:</b></p>
 * <p class='bjava'>
 *    <jc>// Use default converter</jc>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsf>DEFAULT</jsf>;
 *    <jk>var</jk> <jv>result</jv> = <jv>converter</jv>.stringify(<jv>myObject</jv>);
 * </p>
 *
 * <p><b>Custom Configuration:</b></p>
 * <p class='bjava'>
 *    <jc>// Build custom converter</jc>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addSetting(<jsf>SETTING_nullValue</jsf>, <js>"&lt;null&gt;"</js>)
 *       .addSetting(<jsf>SETTING_fieldSeparator</jsf>, <js>" | "</js>)
 *       .addStringifier(MyClass.<jk>class</jk>, (<jp>obj</jp>, <jp>conv</jp>) -> <js>"MyClass["</js> + <jp>obj</jp>.getName() + <js>"]"</js>)
 *       .addListifier(MyIterable.<jk>class</jk>, (<jp>obj</jp>, <jp>conv</jp>) -> <jp>obj</jp>.toList())
 *       .addSwapper(MyWrapper.<jk>class</jk>, (<jp>obj</jp>, <jp>conv</jp>) -> <jp>obj</jp>.getWrapped())
 *       .build();
 * </p>
 *
 * <p><b>Complex Property Access:</b></p>
 * <p class='bjava'>
 *    <jc>// Extract nested properties</jc>
 *    <jk>var</jk> <jv>name</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"name"</js>);
 *    <jk>var</jk> <jv>city</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"address.city"</js>);
 *    <jk>var</jk> <jv>firstOrder</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"orders.0.id"</js>);
 *    <jk>var</jk> <jv>orderCount</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"orders.length"</js>);
 * </p>
 *
 * <p><b>Special Property Values:</b></p>
 * <p class='bjava'>
 *    <jc>// Use special property names</jc>
 *    <jk>var</jk> <jv>userObj</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"&lt;self&gt;"</js>); <jc>// Returns the user object itself</jc>
 *    <jk>var</jk> <jv>nullValue</jv> = <jv>converter</jv>.getEntry(<jv>user</jv>, <js>"&lt;null&gt;"</js>); <jc>// Returns null</jc>
 *
 *    <jc>// Custom self value</jc>
 *    <jk>var</jk> <jv>customConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addSetting(<jsf>SETTING_selfValue</jsf>, <js>"this"</js>)
 *       .build();
 *    <jk>var</jk> <jv>selfRef</jv> = <jv>customConverter</jv>.getEntry(<jv>user</jv>, <js>"this"</js>); <jc>// Returns user object</jc>
 * </p>
 *
 * <h5 class='section'>Performance Characteristics:</h5>
 * <ul>
 *    <li><b>Handler Lookup:</b> O(1) average case via ConcurrentHashMap caching</li>
 *    <li><b>Type Registration:</b> Handlers checked in reverse registration order (last wins)</li>
 *    <li><b>Inheritance Support:</b> Handlers support class inheritance and interface implementation</li>
 *    <li><b>Thread Safety:</b> Full concurrency support with no locking overhead after initialization</li>
 *    <li><b>Memory Efficiency:</b> Minimal object allocation during normal operation</li>
 * </ul>
 *
 * <h5 class='section'>Extension Patterns:</h5>
 *
 * <p><b>Custom Type Stringification:</b></p>
 * <p class='bjava'>
 *    <jv>builder</jv>.addStringifier(LocalDateTime.<jk>class</jk>, (<jp>dt</jp>, <jp>conv</jp>) ->
 *       <jp>dt</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE_TIME</jsf>));
 * </p>
 *
 * <p><b>Custom Collection Handling:</b></p>
 * <p class='bjava'>
 *    <jv>builder</jv>.addListifier(MyCustomCollection.<jk>class</jk>, (<jp>coll</jp>, <jp>conv</jp>) ->
 *       <jp>coll</jp>.stream().map(<jp>conv</jp>::swap).toList());
 * </p>
 *
 * <p><b>Custom Object Transformation:</b></p>
 * <p class='bjava'>
 *    <jv>builder</jv>.addSwapper(LazyValue.<jk>class</jk>, (<jp>lazy</jp>, <jp>conv</jp>) ->
 *       <jp>lazy</jp>.isEvaluated() ? <jp>lazy</jp>.getValue() : "&lt;unevaluated&gt;");
 * </p>
 *
 * <h5 class='section'>Integration with BCT:</h5>
 * <p>This class is used internally by all BCT assertion methods in {@link BctAssertions}:</p>
 * <ul>
 *    <li>{@link BctAssertions#assertBean(Object, String, String)} - Uses getEntry() for property access</li>
 *    <li>{@link BctAssertions#assertList(List, Object...)} - Uses stringify() for element comparison</li>
 *    <li>{@link BctAssertions#assertBeans(Collection, String, String...)} - Uses both getEntry() and stringify()</li>
 * </ul>
 *
 * @see BeanConverter
 */
@SuppressWarnings("rawtypes")
public class BasicBeanConverter implements BeanConverter {

	/**
	 * Builder for creating customized BasicBeanConverter instances.
	 *
	 * <p>This builder provides a fluent API for configuring custom type handlers, settings,
	 * and property extraction logic. The builder supports registration of four main types
	 * of customizations:</p>
	 *
	 * <h5 class='section'>Type Handlers:</h5>
	 * <ul>
	 *    <li><b>{@link #addStringifier(Class, Stringifier)}</b> - Custom string conversion logic</li>
	 *    <li><b>{@link #addListifier(Class, Listifier)}</b> - Custom list conversion for collection-like objects</li>
	 *    <li><b>{@link #addSwapper(Class, Swapper)}</b> - Pre-processing and object transformation</li>
	 *    <li><b>{@link #addPropertyExtractor(PropertyExtractor)}</b> - Custom property access strategies</li>
	 * </ul>
	 *
	 * <h5 class='section'>PropertyExtractors:</h5>
	 * <p>Property extractors define how the converter accesses object properties during nested
	 * field access (e.g., {@code "user.address.city"}). The converter uses a chain-of-responsibility
	 * pattern, trying each registered extractor until one succeeds:</p>
	 *
	 * <ul>
	 *    <li><b>{@link PropertyExtractors.ObjectPropertyExtractor}</b> - JavaBean-style properties via reflection</li>
	 *    <li><b>{@link PropertyExtractors.ListPropertyExtractor}</b> - Numeric indices and size/length for arrays/collections</li>
	 *    <li><b>{@link PropertyExtractors.MapPropertyExtractor}</b> - Key-based access for Map objects</li>
	 * </ul>
	 *
	 * <p>Custom extractors can be added to handle specialized property access patterns:</p>
	 * <p class='bjava'>
	 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *       .defaultSettings()
	 *       .addPropertyExtractor(<jk>new</jk> MyCustomExtractor())
	 *       .addPropertyExtractor((<jp>obj</jp>, <jp>prop</jp>) -&gt; {
	 *          <jk>if</jk> (<jp>obj</jp> <jk>instanceof</jk> MySpecialType <jv>special</jv>) {
	 *             <jk>return</jk> <jv>special</jv>.getCustomProperty(<jp>prop</jp>);
	 *          }
	 *          <jk>return</jk> <jk>null</jk>; <jc>// Try next extractor</jc>
	 *       })
	 *       .build();
	 * </p>
	 *
	 * <h5 class='section'>Default Configuration:</h5>
	 * <p>The {@link #defaultSettings()} method pre-registers comprehensive type handlers
	 * and property extractors for common Java types, providing out-of-the-box functionality
	 * for most use cases while still allowing full customization.</p>
	 *
	 * @see PropertyExtractors
	 * @see PropertyExtractor
	 */
	public static class Builder {
		private Map<String,Object> settings = map();
		private List<StringifierEntry<?>> stringifiers = list();
		private List<ListifierEntry<?>> listifiers = list();
		private List<SizerEntry<?>> sizers = list();
		private List<SwapperEntry<?>> swappers = list();
		private List<PropertyExtractor> propertyExtractors = list();

		/**
		 * Registers a custom listifier for a specific type.
		 *
		 * <p>Listifiers convert collection-like objects to List&lt;Object&gt;. The BiFunction
		 * receives the object to convert and the converter instance for recursive calls.</p>
		 *
		 * @param <T> The type to handle
		 * @param c The class to register the listifier for
		 * @param l The listification function
		 * @return This builder for method chaining
		 */
		public <T> Builder addListifier(Class<T> c, Listifier<T> l) {
			listifiers.add(new ListifierEntry<>(c, l));
			return this;
		}

		/**
		 * Registers a custom sizer for a specific type.
		 *
		 * <p>Sizers compute the size of collection-like objects for test assertions.
		 * The function receives the object to size and the converter instance for accessing
		 * additional utilities if needed.</p>
		 *
		 * @param <T> The type to handle
		 * @param c The class to register the sizer for
		 * @param s The sizing function
		 * @return This builder for method chaining
		 */
		public <T> Builder addSizer(Class<T> c, Sizer<T> s) {
			sizers.add(new SizerEntry<>(c, s));
			return this;
		}

		/**
		 * Registers a custom property extractor for specialized property access logic.
		 *
		 * <p>Property extractors enable custom property access patterns beyond standard JavaBean
		 * conventions. The converter tries extractors in registration order until one returns
		 * a non-<jk>null</jk> value. This allows for:</p>
		 * <ul>
		 *    <li><b>Custom data structures:</b> Special property access for non-standard objects</li>
		 *    <li><b>Database entities:</b> Property access via entity-specific methods</li>
		 *    <li><b>Dynamic properties:</b> Computed or cached property values</li>
		 *    <li><b>Legacy objects:</b> Bridging older APIs with modern property access</li>
		 * </ul>
		 *
		 * <h5 class='section'>Implementation Example:</h5>
		 * <p class='bjava'>
		 *    <jc>// Custom extractor for a specialized data class</jc>
		 *    PropertyExtractor <jv>customExtractor</jv> = (<jp>obj</jp>, <jp>property</jp>) -&gt; {
		 *       <jk>if</jk> (<jp>obj</jp> <jk>instanceof</jk> DatabaseEntity <jv>entity</jv>) {
		 *          <jk>switch</jk> (<jp>property</jp>) {
		 *             <jk>case</jk> <js>"id"</js>: <jk>return</jk> <jv>entity</jv>.getPrimaryKey();
		 *             <jk>case</jk> <js>"displayName"</js>: <jk>return</jk> <jv>entity</jv>.computeDisplayName();
		 *             <jk>case</jk> <js>"metadata"</js>: <jk>return</jk> <jv>entity</jv>.getMetadataAsMap();
		 *          }
		 *       }
		 *       <jk>return</jk> <jk>null</jk>; <jc>// Let next extractor try</jc>
		 *    };
		 *
		 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
		 *       .addPropertyExtractor(<jv>customExtractor</jv>)
		 *       .defaultSettings() <jc>// Adds standard extractors</jc>
		 *       .build();
		 * </p>
		 *
		 * <p><b>Execution Order:</b> Custom extractors are tried before the default extractors
		 * added by {@link #defaultSettings()}, allowing overrides of standard behavior.</p>
		 *
		 * @param e The property extractor to register
		 * @return This builder for method chaining
		 * @see PropertyExtractor
		 * @see PropertyExtractors
		 */
		public Builder addPropertyExtractor(PropertyExtractor e) {
			propertyExtractors.add(e);
			return this;
		}

		/**
		 * Adds a configuration setting to the converter.
		 *
		 * @param key The setting key (use SETTING_* constants)
		 * @param value The setting value
		 * @return This builder for method chaining
		 */
		public Builder addSetting(String key, Object value) {
			settings.put(key, value);
			return this;
		}

		/**
		 * Registers a custom stringifier for a specific type.
		 *
		 * <p>Stringifiers convert objects to their string representations. The BiFunction
		 * receives the object to convert and the converter instance for recursive calls.</p>
		 *
		 * @param <T> The type to handle
		 * @param c The class to register the stringifier for
		 * @param s The stringification function
		 * @return This builder for method chaining
		 */
		public <T> Builder addStringifier(Class<T> c, Stringifier<T> s) {
			stringifiers.add(new StringifierEntry<>(c, s));
			return this;
		}

		/**
		 * Registers a custom swapper for a specific type.
		 *
		 * <p>Swappers pre-process objects before conversion. Common uses include
		 * unwrapping Optional values, calling Supplier methods, or extracting values
		 * from wrapper objects.</p>
		 *
		 * @param <T> The type to handle
		 * @param c The class to register the swapper for
		 * @param s The swapping function
		 * @return This builder for method chaining
		 */
		public <T> Builder addSwapper(Class<T> c, Swapper<T> s) {
			swappers.add(new SwapperEntry<>(c, s));
			return this;
		}

		/**
		 * Builds the configured BasicBeanConverter instance.
		 *
		 * <p>This method creates a new BasicBeanConverter with all registered handlers
		 * and settings. The builder can be reused to create multiple converters with
		 * the same configuration.</p>
		 *
		 * @return A new BasicBeanConverter instance
		 */
		public BasicBeanConverter build() {
			return new BasicBeanConverter(this);
		}

		/**
		 * Adds default handlers and settings for common Java types.
		 *
		 * <p>This method registers comprehensive support for:</p>
		 * <ul>
		 *    <li><b>Collections:</b> List, Set, Collection → bracket format</li>
		 *    <li><b>Maps:</b> Map, Properties → brace format with key=value pairs</li>
		 *    <li><b>Map Entries:</b> Map.Entry → <js>"key=value"</js> format</li>
		 *    <li><b>Dates:</b> Date, Calendar → ISO-8601 format</li>
		 *    <li><b>Files/Streams:</b> File, InputStream, Reader → content extraction</li>
		 *    <li><b>Arrays:</b> byte[], char[] → hex strings and direct string conversion</li>
		 *    <li><b>Reflection:</b> Class, Method, Constructor → readable signatures</li>
		 *    <li><b>Enums:</b> All enum types → name() format</li>
		 *    <li><b>Iterables:</b> Iterable, Iterator, Enumeration, Stream → list conversion</li>
		 *    <li><b>Wrappers:</b> Optional, Supplier → unwrapping/evaluation</li>
		 * </ul>
		 *
		 * <p>Default settings configured by this method:</p>
		 * <ul>
		 *    <li><code>{@link #SETTING_nullValue}</code> = <js>"&lt;null&gt;"</js> - String representation for null values</li>
		 *    <li><code>{@link #SETTING_selfValue}</code> = <js>"&lt;self&gt;"</js> - Special property name for self-reference</li>
		 *    <li><code>{@link #SETTING_classNameFormat}</code> = <js>"simple"</js> - Simple class name format</li>
		 * </ul>
		 *
		 * <p>Additional settings that can be configured (not set by default):</p>
		 * <ul>
		 *    <li><code>{@link #SETTING_fieldSeparator}</code> - Delimiter between collection elements (default: <js>","</js>)</li>
		 *    <li><code>{@link #SETTING_collectionPrefix}</code> - Prefix for collection content (default: <js>"["</js>)</li>
		 *    <li><code>{@link #SETTING_collectionSuffix}</code> - Suffix for collection content (default: <js>"]"</js>)</li>
		 *    <li><code>{@link #SETTING_mapPrefix}</code> - Prefix for map content (default: <js>"{"</js>)</li>
		 *    <li><code>{@link #SETTING_mapSuffix}</code> - Suffix for map content (default: <js>"}"</js>)</li>
		 *    <li><code>{@link #SETTING_mapEntrySeparator}</code> - Separator between map keys and values (default: <js>"="</js>)</li>
		 *    <li><code>{@link #SETTING_calendarFormat}</code> - DateTimeFormatter for calendar objects (default: <jsf>ISO_INSTANT</jsf>)</li>
		 * </ul>
		 *
		 * <p><b>Note:</b> This should typically be called after custom handlers to avoid
		 * overriding your custom configurations, since handlers are processed in reverse order.</p>
		 *
		 * @return This builder for method chaining
		 */
		public Builder defaultSettings() {
			addSetting(SETTING_nullValue, "<null>");
			addSetting(SETTING_selfValue, "<self>");
			addSetting(SETTING_classNameFormat, "simple");

			addStringifier(Map.Entry.class, Stringifiers.mapEntryStringifier());
			addStringifier(GregorianCalendar.class, Stringifiers.calendarStringifier());
			addStringifier(Date.class, Stringifiers.dateStringifier());
			addStringifier(InputStream.class, Stringifiers.inputStreamStringifier());
			addStringifier(byte[].class, Stringifiers.byteArrayStringifier());
			addStringifier(char[].class, Stringifiers.charArrayStringifier());
			addStringifier(Reader.class, Stringifiers.readerStringifier());
			addStringifier(File.class, Stringifiers.fileStringifier());
			addStringifier(Enum.class, Stringifiers.enumStringifier());
			addStringifier(Class.class, Stringifiers.classStringifier());
			addStringifier(Constructor.class, Stringifiers.constructorStringifier());
			addStringifier(Method.class, Stringifiers.methodStringifier());
			addStringifier(List.class, Stringifiers.listStringifier());
			addStringifier(Map.class, Stringifiers.mapStringifier());

			// Note: Listifiers are processed in reverse registration order (last registered wins).
			// Collection must be registered after Iterable so it takes precedence for Sets,
			// ensuring TreeSet conversion for deterministic ordering.
			addListifier(Iterable.class, Listifiers.iterableListifier());
			addListifier(Collection.class, Listifiers.collectionListifier());
			addListifier(Iterator.class, Listifiers.iteratorListifier());
			addListifier(Enumeration.class, Listifiers.enumerationListifier());
			addListifier(Stream.class, Listifiers.streamListifier());
			addListifier(Map.class, Listifiers.mapListifier());

			addSwapper(Optional.class, Swappers.optionalSwapper());
			addSwapper(Supplier.class, Swappers.supplierSwapper());
			addSwapper(Future.class, Swappers.futureSwapper());

			addPropertyExtractor(new PropertyExtractors.ObjectPropertyExtractor());
			addPropertyExtractor(new PropertyExtractors.ListPropertyExtractor());
			addPropertyExtractor(new PropertyExtractors.MapPropertyExtractor());

			return this;
		}
	}

	static class ListifierEntry<T> {
		private Class<T> forClass;
		private Listifier<T> function;

		private ListifierEntry(Class<T> forClass, Listifier<T> function) {
			this.forClass = forClass;
			this.function = function;
		}
	}

	static class StringifierEntry<T> {
		private Class<T> forClass;
		private Stringifier<T> function;

		@SuppressWarnings("unchecked")
		private StringifierEntry(Class<T> forClass, Stringifier function) {
			this.forClass = forClass;
			this.function = function;
		}
	}

	static class SwapperEntry<T> {
		private Class<T> forClass;
		private Swapper<T> function;

		private SwapperEntry(Class<T> forClass, Swapper<T> function) {
			this.forClass = forClass;
			this.function = function;
		}
	}

	static class SizerEntry<T> {
		private Class<T> forClass;
		private Sizer<T> function;

		private SizerEntry(Class<T> forClass, Sizer<T> function) {
			this.forClass = forClass;
			this.function = function;
		}
	}

	/**
	 * Default converter instance with standard settings and handlers.
	 *
	 * <p>This pre-configured instance provides comprehensive support for all common Java types
	 * with sensible default settings. It's suitable for most BCT testing scenarios and can be
	 * used directly without building a custom converter.</p>
	 *
	 * <h5 class='section'>Included Support:</h5>
	 * <ul>
	 *    <li><b>Collections:</b> List, Set, Queue with bracket formatting</li>
	 *    <li><b>Maps:</b> Map, Properties with brace formatting</li>
	 *    <li><b>Arrays:</b> All array types with bracket formatting</li>
	 *    <li><b>Dates:</b> Date, Calendar with ISO-8601 formatting</li>
	 *    <li><b>Files/Streams:</b> File, InputStream, Reader content extraction</li>
	 *    <li><b>Reflection:</b> Class, Method, Constructor readable signatures</li>
	 *    <li><b>Enums:</b> name() representation</li>
	 *    <li><b>Wrappers:</b> Optional, Supplier, Future unwrapping</li>
	 * </ul>
	 *
	 * <h5 class='section'>Default Settings:</h5>
	 * <ul>
	 *    <li><code>nullValue</code> = <js>"&lt;null&gt;"</js></li>
	 *    <li><code>selfValue</code> = <js>"&lt;self&gt;"</js></li>
	 *    <li><code>classNameFormat</code> = <js>"simple"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 *    <jc>// Use default converter for assertions</jc>
	 *    <jk>var</jk> <jv>result</jv> = BasicBeanConverter.<jsf>DEFAULT</jsf>.stringify(<jv>myObject</jv>);
	 *    <jk>var</jk> <jv>city</jv> = BasicBeanConverter.<jsf>DEFAULT</jsf>.getProperty(<jv>user</jv>, <js>"address.city"</js>);
	 * </p>
	 *
	 * @see #builder()
	 * @see Builder#defaultSettings()
	 */
	public static final BasicBeanConverter DEFAULT = builder().defaultSettings().build();

	/**
	 * Setting key for the string representation of null values.
	 *
	 * <p>Default value: <js>"&lt;null&gt;"</js></p>
	 *
	 * <p>This setting controls how null values are displayed when stringified.
	 * Used by the converter when encountering null objects during conversion.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 */
	public static final String SETTING_nullValue = "nullValue";

	/**
	 * Setting key for the special property name that returns the object itself.
	 *
	 * <p>Default value: <js>"&lt;self&gt;"</js></p>
	 *
	 * <p>This setting defines the property name that, when accessed, returns the
	 * object itself rather than a property of the object. Useful for self-referential
	 * property access in nested object navigation.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 */
	public static final String SETTING_selfValue = "selfValue";

	/**
	 * Setting key for the delimiter between collection elements and map entries.
	 *
	 * <p>Default value: <js>","</js></p>
	 *
	 * <p>This setting controls the separator used between elements when converting
	 * collections, arrays, and maps to string representations.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 */
	public static final String SETTING_fieldSeparator = "fieldSeparator";

	/**
	 * Setting key for the prefix character(s) used around collection content.
	 *
	 * <p>Default value: <js>"["</js></p>
	 *
	 * <p>This setting defines the opening bracket or prefix used when displaying
	 * collection and array contents in string format.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 * @see #SETTING_collectionSuffix
	 */
	public static final String SETTING_collectionPrefix = "collectionPrefix";

	/**
	 * Setting key for the suffix character(s) used around collection content.
	 *
	 * <p>Default value: <js>"]"</js></p>
	 *
	 * <p>This setting defines the closing bracket or suffix used when displaying
	 * collection and array contents in string format.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 * @see #SETTING_collectionPrefix
	 */
	public static final String SETTING_collectionSuffix = "collectionSuffix";

	/**
	 * Setting key for the prefix character(s) used around map content.
	 *
	 * <p>Default value: <js>"{"</js></p>
	 *
	 * <p>This setting defines the opening brace or prefix used when displaying
	 * map contents in string format.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 * @see #SETTING_mapSuffix
	 */
	public static final String SETTING_mapPrefix = "mapPrefix";

	/**
	 * Setting key for the suffix character(s) used around map content.
	 *
	 * <p>Default value: <js>"}"</js></p>
	 *
	 * <p>This setting defines the closing brace or suffix used when displaying
	 * map contents in string format.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 * @see #SETTING_mapPrefix
	 */
	public static final String SETTING_mapSuffix = "mapSuffix";

	/**
	 * Setting key for the separator between map keys and values.
	 *
	 * <p>Default value: <js>"="</js></p>
	 *
	 * <p>This setting controls the separator used between keys and values when
	 * converting map entries to string representations (e.g., "key=value").</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 */
	public static final String SETTING_mapEntrySeparator = "mapEntrySeparator";

	/**
	 * Setting key for the DateTimeFormatter used for calendar objects.
	 *
	 * <p>Default value: <jsf>ISO_INSTANT</jsf></p>
	 *
	 * <p>This setting defines the date/time format used when converting Calendar
	 * objects to string representations. Must be a valid DateTimeFormatter instance.</p>
	 *
	 * @see Builder#addSetting(String, Object)
	 * @see java.time.format.DateTimeFormatter
	 */
	public static final String SETTING_calendarFormat = "calendarFormat";

	/**
	 * Setting key for the format used when displaying class names.
	 *
	 * <p>Default value: <js>"simple"</js></p>
	 *
	 * <p>This setting controls how class names are displayed in string representations.
	 * Valid values are:</p>
	 * <ul>
	 *    <li><js>"simple"</js> - Simple class name only (e.g., "String")</li>
	 *    <li><js>"canonical"</js> - Canonical class name (e.g., "java.lang.String")</li>
	 *    <li><js>"full"</js> - Full class name with package (e.g., "java.lang.String")</li>
	 * </ul>
	 *
	 * @see Builder#addSetting(String, Object)
	 */
	public static final String SETTING_classNameFormat = "classNameFormat";

	/**
	 * Creates a new builder for configuring a BasicBeanConverter instance.
	 *
	 * <p>The builder allows registration of custom stringifiers, listifiers, and swappers,
	 * as well as configuration of various formatting settings before building the converter.</p>
	 *
	 * @return A new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	private final List<StringifierEntry<?>> stringifiers;
	private final List<ListifierEntry<?>> listifiers;
	private final List<SizerEntry<?>> sizers;
	private final List<SwapperEntry<?>> swappers;

	private final List<PropertyExtractor> propertyExtractors;

	private final Map<String,Object> settings;

	private final ConcurrentHashMap<Class,Optional<Stringifier<?>>> stringifierMap = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Class,Optional<Listifier<?>>> listifierMap = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Class,Optional<Sizer<?>>> sizerMap = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<Class,Optional<Swapper<?>>> swapperMap = new ConcurrentHashMap<>();

	protected BasicBeanConverter(Builder b) {
		stringifiers = copyOf(b.stringifiers);
		listifiers = copyOf(b.listifiers);
		sizers = copyOf(b.sizers);
		swappers = copyOf(b.swappers);
		propertyExtractors = copyOf(b.propertyExtractors);
		settings = copyOf(b.settings);
		Collections.reverse(stringifiers);
		Collections.reverse(listifiers);
		Collections.reverse(swappers);
		Collections.reverse(propertyExtractors);
	}

	@Override
	public boolean canListify(Object o) {
		o = swap(o);
		if (o == null)
			return false;
		var c = o.getClass();
		return o instanceof List || c.isArray() || listifierMap.computeIfAbsent(c, this::findListifier).isPresent();
	}

	@Override
	public String getNested(Object o, NestedTokenizer.Token token) {
		assertArgNotNull("token", token);

		if (o == null)
			return getSetting(SETTING_nullValue, null);

		// Handle #{...} syntax for iterating over collections/arrays
		if (eq("#", token.getValue()) && canListify(o)) {
			return listify(o).stream().map(x -> token.getNested().stream().map(x2 -> getNested(x, x2)).collect(joining(",", "{", "}"))).collect(joining(",", "[", "]"));
		}

		// Original logic for regular property access
		var pn = token.getValue();
		var selfValue = getSetting(SETTING_selfValue, "<self>");

		// Handle special values
		Object e;
		if (pn.equals(selfValue)) {
			e = o; // Return the object itself
		} else {
			e = opt(getProperty(o, pn)).orElse(null);
		}
		if (e == null || ! token.hasNested())
			return stringify(e);
		return token.getNested().stream().map(x -> getNested(e, x)).map(this::stringify).collect(joining(",", "{", "}"));
	}

	@Override
	public Object getProperty(Object object, String name) {
		var o = swap(object);
		// @formatter:off
		return propertyExtractors
			.stream()
			.filter(x -> x.canExtract(this, o, name))
			.findFirst()
			.orElseThrow(() -> rex("Could not find extractor for object of type {0}", cn(o))).extract(this, o, name);
		// @formatter:on
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getSetting(String key, T def) {
		return (T)settings.getOrDefault(key, def);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> listify(Object o) {
		assertArgNotNull("o", o);

		o = swap(o);

		if (o instanceof List)
			return (List<Object>)o;
		if (o.getClass().isArray())
			return arrayToList(o);

		var c = o.getClass();
		var o2 = o;
		// @formatter:off
		return listifierMap
			.computeIfAbsent(c, this::findListifier)
			.map(x -> (Listifier)x)
			.map(x -> (List<Object>)x.apply(this, o2))
			.orElseThrow(() -> illegalArg("Object of type {0} could not be converted to a list.", cns(o2)));
		// @formatter:on
	}

	@Override
	@SuppressWarnings("unchecked")
	public int size(Object o) {
		assertArgNotNull("o", o);

		// Checks for Optional before unpacking.
		if (o instanceof Optional o2)
			return o2.isEmpty() ? 0 : 1;

		o = swap(o);

		// Check standard object types.
		if (o == null)
			return 0;
		if (o instanceof Collection o2)
			return o2.size();
		if (o instanceof Map o3)
			return o3.size();
		if (o.getClass().isArray())
			return Array.getLength(o);
		if (o instanceof String o2)
			return o2.length();

		// Check for registered custom Sizer
		var c = o.getClass();
		var o2 = o;
		var sizer = sizerMap.computeIfAbsent(c, this::findSizer);
		if (sizer.isPresent())
			return ((Sizer)sizer.get()).size(o2, this);

		// Try to find size() or length() method via reflection
		// @formatter:off
		var sizeResult = info(c).getPublicMethods().stream()
			.filter(m -> ! m.hasParameters())
			.filter(m -> m.hasAnyName("size", "length"))
			.filter(m -> m.getReturnType().isAny(int.class, Integer.class))
			.findFirst()
			.map(m -> safe(() -> (int) m.invoke(o2)))
			.filter(Objects::nonNull);
		// @formatter:on
		if (sizeResult.isPresent()) return sizeResult.get();

		// Fall back to listify
		if (canListify(o))
			return listify(o).size();

		// Try to find isEmpty() method via reflection
		// @formatter:off
		var isEmpty = info(o).getPublicMethods().stream()
			.filter(m -> ! m.hasParameters())
			.filter(m -> m.hasName("isEmpty"))
			.filter(m -> m.getReturnType().isAny(boolean.class, Boolean.class))
			.map(m -> safe(() -> (Boolean)m.invoke(o2)))
			.findFirst();
		// @formatter:on
		if (isEmpty.isPresent()) return isEmpty.get() ? 0 : 1;

		throw illegalArg("Object of type {0} does not have a determinable size.", cns(o));
	}

	@Override
	@SuppressWarnings("unchecked")
	public String stringify(Object o) {

		o = swap(o);

		if (o == null)
			return getSetting(SETTING_nullValue, null);

		var c = o.getClass();
		var stringifier = stringifierMap.computeIfAbsent(c, this::findStringifier);
		if (stringifier.isEmpty()) {
			stringifier = of(canListify(o) ? (bc, o2) -> bc.stringify(bc.listify(o2)) : (bc, o2) -> this.safeToString(o2));
			stringifierMap.putIfAbsent(c, stringifier);
		}
		var o2 = o;
		return stringifier.map(x -> (Stringifier)x).map(x -> x.apply(this, o2)).map(this::safeToString).orElse(null);
	}

	/**
	 * Safely converts an object to a string, catching any exceptions thrown by toString().
	 *
	 * <p>
	 * This helper method ensures that exceptions thrown by problematic {@code toString()} implementations
	 * don't propagate up the call stack. Instead, it returns a descriptive error message containing
	 * the exception type and message.
	 *
	 * @param o The object to convert to a string. May be any object including <jk>null</jk>.
	 * @return The string representation of the object, or a formatted error message if toString() throws an exception.
	 *    Returns <js>"null"</js> if the object is <jk>null</jk>.
	 */
	private String safeToString(Object o) {
		try {
			return o.toString();
		} catch (Throwable t) { // NOSONAR
			return cns(t) + ": " + t.getMessage();
		}
	}

	/**
	 * Builder class for configuring BasicBeanConverter instances.
	 *
	 * <p>This builder provides a fluent interface for registering custom type handlers
	 * and configuring conversion settings. All registration methods support method chaining
	 * for convenient configuration.</p>
	 *
	 * <h5 class='section'>Handler Registration:</h5>
	 * <ul>
	 *    <li><b>Stringifiers:</b> Custom string conversion logic for specific types</li>
	 *    <li><b>Listifiers:</b> Custom list conversion logic for collection-like types</li>
	 *    <li><b>Swappers:</b> Pre-processing transformation logic for wrapper types</li>
	 * </ul>
	 *
	 * <h5 class='section'>Registration Order:</h5>
	 * <p>Handlers are checked in reverse registration order (last registered wins).
	 * This allows overriding default handlers by registering more specific ones later.</p>
	 *
	 * <h5 class='section'>Inheritance Support:</h5>
	 * <p>All handlers support class inheritance and interface implementation.
	 * When looking up a handler, the system checks:</p>
	 * <ol>
	 *    <li>Exact class match</li>
	 *    <li>Interface matches (in order of interface declaration)</li>
	 *    <li>Superclass matches (walking up the inheritance hierarchy)</li>
	 * </ol>
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *       .defaultSettings()
	 *       <jc>// Custom stringification for LocalDateTime</jc>
	 *       .addStringifier(LocalDateTime.<jk>class</jk>, (<jp>dt</jp>, <jp>conv</jp>) ->
	 *          <jp>dt</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE_TIME</jsf>))
	 *
	 *       <jc>// Custom collection handling for custom type</jc>
	 *       .addListifier(MyIterable.<jk>class</jk>, (<jp>iter</jp>, <jp>conv</jp>) ->
	 *          <jp>iter</jp>.stream().collect(toList()))
	 *
	 *       <jc>// Custom transformation for wrapper type</jc>
	 *       .addSwapper(LazyValue.<jk>class</jk>, (<jp>lazy</jp>, <jp>conv</jp>) ->
	 *          <jp>lazy</jp>.isComputed() ? <jp>lazy</jp>.get() : <jk>null</jk>)
	 *
	 *       <jc>// Configure settings</jc>
	 *       .addSetting(<jsf>SETTING_nullValue</jsf>, <js>"&lt;null&gt;"</js>)
	 *       .addSetting(<jsf>SETTING_fieldSeparator</jsf>, <js>" | "</js>)
	 *
	 *       <jc>// Add default handlers for common types</jc>
	 *       .defaultSettings()
	 *       .build();
	 * </p>
	 */

	@Override
	@SuppressWarnings("unchecked")
	public Object swap(Object o) {
		if (o == null)
			return null;
		var c = o.getClass();
		var swapper = swapperMap.computeIfAbsent(c, this::findSwapper);
		if (swapper.isPresent())
			return swap(swapper.map(x -> (Swapper)x).map(x -> x.apply(this, o)).orElse(null));
		return o;
	}

	private Optional<Listifier<?>> findListifier(Class<?> c) {
		if (c == null)
			return empty();
		var l = listifiers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (nn(l))
			return of(l.function);
		return findListifier(c.getSuperclass());
	}

	private Optional<Sizer<?>> findSizer(Class<?> c) {
		if (c == null)
			return empty();
		var s = sizers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (nn(s))
			return of(s.function);
		return findSizer(c.getSuperclass());
	}

	private Optional<Stringifier<?>> findStringifier(Class<?> c) {
		if (c == null)
			return empty();
		var s = stringifiers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (nn(s))
			return of(s.function);
		return findStringifier(c.getSuperclass());
	}

	private Optional<Swapper<?>> findSwapper(Class<?> c) {
		if (c == null)
			return empty();
		var s = swappers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (nn(s))
			return of(s.function);
		return findSwapper(c.getSuperclass());
	}
}