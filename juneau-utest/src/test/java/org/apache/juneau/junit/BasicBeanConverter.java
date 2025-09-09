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
package org.apache.juneau.junit;

import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.*;
import static java.time.format.DateTimeFormatter.*;
import static java.util.Collections.*;
import static java.util.Optional.*;
import static org.apache.juneau.junit.Utils.*;

import java.io.*;

import static java.util.Spliterators.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;

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
 * 	<li><b>Extensible Type Handlers:</b> Pluggable stringifiers, listifiers, and swappers for custom types</li>
 * 	<li><b>Performance Optimization:</b> ConcurrentHashMap caching for type-to-handler mappings</li>
 * 	<li><b>Comprehensive Defaults:</b> Built-in support for all common Java types and structures</li>
 * 	<li><b>Configurable Settings:</b> Customizable formatting, delimiters, and display options</li>
 * 	<li><b>Thread Safety:</b> Fully thread-safe implementation suitable for concurrent testing</li>
 * </ul>
 *
 * <h5 class='section'>Architecture Overview:</h5>
 * <p>The converter uses three types of pluggable handlers:</p>
 * <dl>
 * 	<dt><b>Stringifiers:</b></dt>
 * 	<dd>Convert objects to string representations with custom formatting rules</dd>
 *
 * 	<dt><b>Listifiers:</b></dt>
 * 	<dd>Convert collection-like objects to List&lt;Object&gt; for uniform iteration</dd>
 *
 * 	<dt><b>Swappers:</b></dt>
 * 	<dd>Pre-process objects before conversion (unwrap Optional, call Supplier, etc.)</dd>
 * </dl>
 *
 * <h5 class='section'>Default Type Support:</h5>
 * <p>Out-of-the-box stringification support includes:</p>
 * <ul>
 * 	<li><b>Collections:</b> List, Set, Queue → "[item1,item2,item3]" format</li>
 * 	<li><b>Maps:</b> Map, Properties → "{key1=value1,key2=value2}" format</li>
 * 	<li><b>Map Entries:</b> Map.Entry → "key=value" format</li>
 * 	<li><b>Arrays:</b> All array types → "[element1,element2]" format</li>
 * 	<li><b>Dates:</b> Date, Calendar → ISO-8601 format</li>
 * 	<li><b>Files/Streams:</b> File, InputStream, Reader → content as hex or text</li>
 * 	<li><b>Reflection:</b> Class, Method, Constructor → human-readable signatures</li>
 * 	<li><b>Enums:</b> Enum values → name() format</li>
 * </ul>
 *
 * <p>Default listification support includes:</p>
 * <ul>
 * 	<li><b>Collection types:</b> List, Set, Queue, and all subtypes</li>
 * 	<li><b>Iterable objects:</b> Any Iterable implementation</li>
 * 	<li><b>Iterators:</b> Iterator and Enumeration (consumed to list)</li>
 * 	<li><b>Streams:</b> Stream objects (terminated to list)</li>
 * 	<li><b>Optional:</b> Empty list or single-element list</li>
 * 	<li><b>Maps:</b> Converted to list of Map.Entry objects</li>
 * </ul>
 *
 * <p>Default swapping support includes:</p>
 * <ul>
 * 	<li><b>Optional:</b> Unwrapped to contained value or null</li>
 * 	<li><b>Supplier:</b> Called to get supplied value</li>
 * </ul>
 *
 * <h5 class='section'>Configuration Settings:</h5>
 * <p>The converter supports extensive customization via settings:</p>
 * <dl>
 * 	<dt><code>nullValue</code></dt>
 * 	<dd>String representation for null values (default: "&lt;null&gt;")</dd>
 *
 * 	<dt><code>emptyValue</code></dt>
 * 	<dd>String representation for empty collections (default: "&lt;empty&gt;")</dd>
 *
 * 	<dt><code>fieldSeparator</code></dt>
 * 	<dd>Delimiter between collection elements and map entries (default: ",")</dd>
 *
 * 	<dt><code>collectionPrefix/Suffix</code></dt>
 * 	<dd>Brackets around collection content (default: "[" and "]")</dd>
 *
 * 	<dt><code>mapPrefix/Suffix</code></dt>
 * 	<dd>Brackets around map content (default: "{" and "}")</dd>
 *
 * 	<dt><code>mapEntrySeparator</code></dt>
 * 	<dd>Separator between map keys and values (default: "=")</dd>
 *
 * 	<dt><code>calendarFormat</code></dt>
 * 	<dd>DateTimeFormatter for calendar objects (default: ISO_INSTANT)</dd>
 *
 * 	<dt><code>classNameFormat</code></dt>
 * 	<dd>Format for class names: "simple", "canonical", or "full" (default: "simple")</dd>
 * </dl>
 *
 * <h5 class='section'>Usage Examples:</h5>
 *
 * <p><b>Basic Usage with Defaults:</b></p>
 * <p class='bjava'>
 * 	<jc>// Use default converter</jc>
 * 	var converter = BasicBeanConverter.DEFAULT;
 * 	var result = converter.stringify(myObject);
 * </p>
 *
 * <p><b>Custom Configuration:</b></p>
 * <p class='bjava'>
 * 	<jc>// Build custom converter</jc>
 * 	var converter = BasicBeanConverter.create()
 * 		.addSetting(SETTING_nullValue, "NULL")
 * 		.addSetting(SETTING_fieldSeparator, " | ")
 * 		.addStringifier(MyClass.class, (obj, conv) -> "MyClass[" + obj.getName() + "]")
 * 		.addListifier(MyIterable.class, (obj, conv) -> obj.toList())
 * 		.addSwapifier(MyWrapper.class, (obj, conv) -> obj.getWrapped())
 * 		.defaultSettings()
 * 		.build();
 * </p>
 *
 * <p><b>Complex Property Access:</b></p>
 * <p class='bjava'>
 * 	<jc>// Extract nested properties</jc>
 * 	var name = converter.getEntry(user, "name");
 * 	var city = converter.getEntry(user, "address.city");
 * 	var firstOrder = converter.getEntry(user, "orders.0.id");
 * 	var orderCount = converter.getEntry(user, "orders.length");
 * </p>
 *
 * <h5 class='section'>Performance Characteristics:</h5>
 * <ul>
 * 	<li><b>Handler Lookup:</b> O(1) average case via ConcurrentHashMap caching</li>
 * 	<li><b>Type Registration:</b> Handlers checked in reverse registration order (last wins)</li>
 * 	<li><b>Inheritance Support:</b> Handlers support class inheritance and interface implementation</li>
 * 	<li><b>Thread Safety:</b> Full concurrency support with no locking overhead after initialization</li>
 * 	<li><b>Memory Efficiency:</b> Minimal object allocation during normal operation</li>
 * </ul>
 *
 * <h5 class='section'>Extension Patterns:</h5>
 *
 * <p><b>Custom Type Stringification:</b></p>
 * <p class='bjava'>
 * 	builder.addStringifier(LocalDateTime.class, (dt, conv) ->
 * 		dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
 * </p>
 *
 * <p><b>Custom Collection Handling:</b></p>
 * <p class='bjava'>
 * 	builder.addListifier(MyCustomCollection.class, (coll, conv) ->
 * 		coll.stream().map(conv::swap).toList());
 * </p>
 *
 * <p><b>Custom Object Transformation:</b></p>
 * <p class='bjava'>
 * 	builder.addSwapifier(LazyValue.class, (lazy, conv) ->
 * 		lazy.isEvaluated() ? lazy.getValue() : "&lt;unevaluated&gt;");
 * </p>
 *
 * <h5 class='section'>Integration with BCT:</h5>
 * <p>This class is used internally by all BCT assertion methods in {@link TestUtils}:</p>
 * <ul>
 * 	<li>{@link TestUtils#assertBean(Object, String, String)} - Uses getEntry() for property access</li>
 * 	<li>{@link TestUtils#assertMap(Map, String, String)} - Uses stringify() for value formatting</li>
 * 	<li>{@link TestUtils#assertList(List, Object...)} - Uses stringify() for element comparison</li>
 * 	<li>{@link TestUtils#assertBeans(Collection, String, String...)} - Uses both getEntry() and stringify()</li>
 * </ul>
 *
 * @see BeanConverter
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BasicBeanConverter implements BeanConverter {

	public static final BasicBeanConverter DEFAULT = create().defaultSettings().build();

	public static final String
		SETTING_nullValue = "nullValue",
		SETTING_emptyValue = "emptyValue",
		SETTING_fieldSeparator = "fieldSeparator",
		SETTING_collectionPrefix = "collectionPrefix",
		SETTING_collectionSuffix = "collectionSuffix",
		SETTING_mapPrefix = "mapPrefix",
		SETTING_mapSuffix = "mapSuffix",
		SETTING_mapEntrySeparator = "mapEntrySeparator",
		SETTING_calendarFormat = "calendarFormat",
		SETTING_classNameFormat = "short";

	private final List<StringifierEntry<?>> stringifiers;
	private final List<ListifierEntry<?>> listifiers;
	private final List<SwapperEntry<?>> swappers;
	private final List<PropertyExtractor> propertyExtractors;
	private final Map<String,Object> settings;

	private final ConcurrentHashMap<Class,Optional<Stringifier<?>>> stringifierMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class,Optional<Listifier<?>>> listifierMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class,Optional<Swapper<?>>> swapperMap = new ConcurrentHashMap<>();

	protected BasicBeanConverter(Builder b) {
		stringifiers = new ArrayList<>(b.stringifiers);
		listifiers = new ArrayList<>(b.listifiers);
		swappers = new ArrayList<>(b.swappers);
		propertyExtractors = new ArrayList<>(b.propertyExtractors);
		settings = new HashMap<>(b.settings);
		Collections.reverse(stringifiers);
		Collections.reverse(listifiers);
		Collections.reverse(swappers);
		Collections.reverse(propertyExtractors);
	}

	/**
	 * Creates a new builder for configuring a BasicBeanConverter instance.
	 *
	 * <p>The builder allows registration of custom stringifiers, listifiers, and swappers,
	 * as well as configuration of various formatting settings before building the converter.</p>
	 *
	 * @return A new Builder instance
	 */
	public static Builder create() {
		return new Builder();
	}

	@Override
	public String stringify(Object o) {
		o = swap(o);
		if (o == null)
			return getSetting(SETTING_nullValue, null);
		var c = o.getClass();
		var stringifier = stringifierMap.computeIfAbsent(c, this::findStringifier);
		if (stringifier.isEmpty()) {
			stringifier = of(canListify(o) ? (bc, o2) -> bc.stringify(bc.listify(o2)) : (bc, o2) -> o2.toString());
			stringifierMap.putIfAbsent(c, stringifier);
		}
		var o2 = o;
		return stringifier.map(x -> (Stringifier)x).map(x -> x.apply(this, o2)).map(Object::toString).orElse(null);
	}

	@Override
	public Object swap(Object o) {
		if (o == null) return null;
		var c = o.getClass();
		var swapper = swapperMap.computeIfAbsent(c, this::findSwapper);
		if (swapper.isPresent())
			return swap(swapper.map(x -> (Swapper)x).map(x -> x.apply(this, o)).orElse(null));
		return o;
	}

	@Override
	public List<Object> listify(Object o) {
		o = swap(o);
		if (o == null)
			return null;
		if (o instanceof List)
			return (List<Object>)o;
		var c = o.getClass();
		if (c.isArray())
			return arrayToList(o);
		var o2 = o;
		return listifierMap.computeIfAbsent(c, this::findListifier).map(x -> (Listifier)x).map(x -> (List<Object>)x.apply(this, o2)).orElse(null);
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
	public String nullValue() {
		return getSetting(SETTING_nullValue, null);
	}

	@Override
	public Object getProperty(Object object, String name) {
		var o = swap(object);
		return propertyExtractors
			.stream()
			.filter(x -> x.canExtract(this, o, name))
			.findFirst()
			.orElseThrow(()->new RuntimeException(f("Could not find extractor for object of type {0}", o.getClass().getName())))
			.extract(this, o, name);
	}

	private Optional<Stringifier<?>> findStringifier(Class<?> c) {
		if (c == null)
			return empty();
		var s = stringifiers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (s != null)
			return of(s.function);
		for (var c2 : c.getInterfaces()) {
			s = stringifiers.stream().filter(x -> x.forClass.isAssignableFrom(c2)).findFirst().orElse(null);
			if (s != null)
				return of(s.function);
		}
		return findStringifier(c.getSuperclass());
	}

	private Optional<Listifier<?>> findListifier(Class<?> c) {
		if (c == null)
			return empty();
		var l = listifiers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (l != null)
			return of(l.function);
		for (var c2 : c.getInterfaces()) {
			l = listifiers.stream().filter(x -> x.forClass.isAssignableFrom(c2)).findFirst().orElse(null);
			if (l != null)
				return of(l.function);
		}
		return findListifier(c.getSuperclass());
	}

	private Optional<Swapper<?>> findSwapper(Class<?> c) {
		if (c == null)
			return empty();
		var s = swappers.stream().filter(x -> x.forClass.isAssignableFrom(c)).findFirst().orElse(null);
		if (s != null)
			return of(s.function);
		for (var c2 : c.getInterfaces()) {
			s = swappers.stream().filter(x -> x.forClass.isAssignableFrom(c2)).findFirst().orElse(null);
			if (s != null)
				return of(s.function);
		}
		return findSwapper(c.getSuperclass());
	}

	@Override
	public <T> T getSetting(String key, T def) {
		return (T)settings.getOrDefault(key, def);
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
	 * 	<li><b>Stringifiers:</b> Custom string conversion logic for specific types</li>
	 * 	<li><b>Listifiers:</b> Custom list conversion logic for collection-like types</li>
	 * 	<li><b>Swappers:</b> Pre-processing transformation logic for wrapper types</li>
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
	 * 	<li>Exact class match</li>
	 * 	<li>Interface matches (in order of interface declaration)</li>
	 * 	<li>Superclass matches (walking up the inheritance hierarchy)</li>
	 * </ol>
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 * 	var converter = BasicBeanConverter.create()
	 * 		<jc>// Custom stringification for LocalDateTime</jc>
	 * 		.addStringifier(LocalDateTime.class, (dt, conv) ->
	 * 			dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
	 *
	 * 		<jc>// Custom collection handling for custom type</jc>
	 * 		.addListifier(MyIterable.class, (iter, conv) ->
	 * 			iter.stream().collect(toList()))
	 *
	 * 		<jc>// Custom transformation for wrapper type</jc>
	 * 		.addSwapifier(LazyValue.class, (lazy, conv) ->
	 * 			lazy.isComputed() ? lazy.get() : null)
	 *
	 * 		<jc>// Configure settings</jc>
	 * 		.addSetting(SETTING_nullValue, "NULL")
	 * 		.addSetting(SETTING_fieldSeparator, " | ")
	 *
	 * 		<jc>// Add default handlers for common types</jc>
	 * 		.defaultSettings()
	 * 		.build();
	 * </p>
	 */
	public static class Builder {
		private Map<String,Object> settings = new HashMap<>();
		private List<StringifierEntry<?>> stringifiers = new ArrayList<>();
		private List<ListifierEntry<?>> listifiers = new ArrayList<>();
		private List<SwapperEntry<?>> swappers = new ArrayList<>();
		private List<PropertyExtractor> propertyExtractors = new ArrayList<>();

		/**
		 * Adds a configuration setting to the converter.
		 *
		 * @param key The setting key (use SETTING_* constants)
		 * @param value The setting value
		 * @return This builder for method chaining
		 */
		public Builder addSetting(String key, Object value) { settings.put(key, value); return this; }

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
		public <T> Builder addStringifier(Class<T> c, Stringifier<T> s) { stringifiers.add(new StringifierEntry<>(c, s)); return this; }

		/**
		 * Registers a custom listifier for a specific type.
		 *
		 * <p>Listifiers convert collection-like objects to List&lt;Object&gt;. The BiFunction
		 * receives the object to convert and the converter instance for recursive calls.</p>
		 *
		 * @param <T> The type to handle
		 * @param c The class to register the listifier for
		 * @param s The listification function
		 * @return This builder for method chaining
		 */
		public <T> Builder addListifier(Class<T> c, Listifier<T> l) { listifiers.add(new ListifierEntry<>(c, l)); return this; }

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
		public <T> Builder addSwapper(Class<T> c, Swapper<T> s) { swappers.add(new SwapperEntry<>(c, s)); return this; }

		public <T> Builder addPropertyExtractor(PropertyExtractor e) { propertyExtractors.add(e); return this; }

		/**
		 * Adds default handlers and settings for common Java types.
		 *
		 * <p>This method registers comprehensive support for:</p>
		 * <ul>
		 * 	<li><b>Collections:</b> List, Set, Collection → bracket format</li>
		 * 	<li><b>Maps:</b> Map, Properties → brace format with key=value pairs</li>
		 * 	<li><b>Map Entries:</b> Map.Entry → "key=value" format</li>
		 * 	<li><b>Dates:</b> Date, Calendar → ISO-8601 format</li>
		 * 	<li><b>Files/Streams:</b> File, InputStream, Reader → content extraction</li>
		 * 	<li><b>Reflection:</b> Class, Method, Constructor → readable signatures</li>
		 * 	<li><b>Enums:</b> All enum types → name() format</li>
		 * 	<li><b>Iterables:</b> Iterable, Iterator, Enumeration, Stream → list conversion</li>
		 * 	<li><b>Wrappers:</b> Optional, Supplier → unwrapping/evaluation</li>
		 * </ul>
		 *
		 * <p>Default settings include:</p>
		 * <ul>
		 * 	<li><code>nullValue</code> = "&lt;null&gt;"</li>
		 * 	<li><code>emptyValue</code> = "&lt;empty&gt;"</li>
		 * 	<li><code>classNameFormat</code> = "simple"</li>
		 * </ul>
		 *
		 * <p><b>Note:</b> This should typically be called after custom handlers to avoid
		 * overriding your custom configurations, since handlers are processed in reverse order.</p>
		 *
		 * @return This builder for method chaining
		 */
		public Builder defaultSettings() {
			addSetting(SETTING_nullValue, "<null>");
			addSetting(SETTING_emptyValue, "<empty>");
			addSetting(SETTING_classNameFormat, "simple");

			addStringifier(Map.Entry.class, (bc, o) -> bc.stringify(o.getKey()) + bc.getSetting(SETTING_mapEntrySeparator, "=") + bc.stringify(o.getValue()));
			addStringifier(GregorianCalendar.class, (bc, o) ->  o.toZonedDateTime().format(bc.getSetting(SETTING_calendarFormat, ISO_INSTANT)));
			addStringifier(Date.class, (bc, o) ->  o.toInstant().toString());
			addStringifier(InputStream.class, (bc, o) ->  stringify(o));
			addStringifier(byte[].class, (bc, o) ->  stringify(o));
			addStringifier(Reader.class, (bc, o) -> stringify(o));
			addStringifier(File.class, (bc, o) -> stringify(o));
			addStringifier(Enum.class, (bc, o) -> o.name());
			addStringifier(Class.class, (bc, o) -> stringify(bc, o));
			addStringifier(Constructor.class, (bc, o) -> stringify(bc, o));
			addStringifier(Method.class, (bc, o) -> stringify(bc, o));
			addStringifier(List.class, (bc, o) -> ((List<?>)o).stream().map(bc::stringify).collect(joining(bc.getSetting(SETTING_fieldSeparator, ","), bc.getSetting(SETTING_collectionPrefix, "["), bc.getSetting(SETTING_collectionSuffix, "]"))));
			addStringifier(Map.class, (bc, o) -> ((Map<?,?>)o).entrySet().stream().map(bc::stringify).collect(joining(bc.getSetting(SETTING_fieldSeparator, ","), bc.getSetting(SETTING_mapPrefix, "{"), bc.getSetting(SETTING_mapSuffix, "}"))));

			addListifier(Collection.class, (bc, o) -> new ArrayList<>(o));
			addListifier(Iterable.class, (bc, o) -> stream(((Iterable<Object>)o).spliterator(), false).toList());
			addListifier(Iterator.class, (bc, o) -> stream(spliteratorUnknownSize(o, 0), false).toList());
			addListifier(Enumeration.class, (bc, o) -> list(o));
			addListifier(Stream.class, (bc, o) -> o.toList());
			addListifier(Optional.class, (bc, o) -> o.isEmpty() ? emptyList() : singletonList(o.get()));
			addListifier(Map.class, (bc, o) -> new ArrayList<>(((Map<?,?>)o).entrySet()));

			addSwapper(Optional.class, (bc, o) -> o.orElse(null));
			addSwapper(Supplier.class, (bc, o) -> o.get());

			addPropertyExtractor(new PropertyExtractors.ObjectPropertyExtractor());
			addPropertyExtractor(new PropertyExtractors.ListPropertyExtractor());
			addPropertyExtractor(new PropertyExtractors.MapPropertyExtractor());

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
	}

	static class StringifierEntry<T> {
		private Class<T> forClass;
		private Stringifier<T> function;

		private StringifierEntry(Class<T> forClass, Stringifier function) {
			this.forClass = forClass;
			this.function = function;
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

	static class SwapperEntry<T> {
		private Class<T> forClass;
		private Swapper<T> function;

		private SwapperEntry(Class<T> forClass, Swapper<T> function) {
			this.forClass = forClass;
			this.function = function;
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods.
	//---------------------------------------------------------------------------------------------

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	private static String stringify(byte[] o) {
		var sb = new StringBuilder(o.length * 2);
		for (var element : o) {
			var v = element & 0xFF;
			sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
		}
		return sb.toString();
	}

	private static String stringify(InputStream o) {
		return safe(() -> {
			try (var o2 = o) {
				var buff = new ByteArrayOutputStream(1024);
				var nRead = 0;
				var b = new byte[1024];
				while ((nRead = o2.read(b, 0, b.length)) != -1)
					buff.write(b, 0, nRead);
				buff.flush();
				return stringify(buff.toByteArray());
			}
		});
	}

	private static String stringify(Reader o) {
		return safe(() -> {
			try (var o2 = o) {
				var sb = new StringBuilder();
				var buf = new char[1024];
				var i = 0;
				while ((i = o2.read(buf)) != -1)
					sb.append(buf, 0, i);
				return sb.toString();
			}
		});
	}

	private static String stringify(File o) {
		return safe(()->stringify(Files.newBufferedReader(o.toPath())));
	}

	private static String stringify(BeanConverter bc, Class<?> o) {
		return switch(bc.getSetting(SETTING_classNameFormat, "default")) {
			case "simple" -> o.getSimpleName();
			case "canonical" -> o.getCanonicalName();
			default -> o.getName();
		};
	}

	private static String stringify(BeanConverter bc, Constructor o) {
		return new StringBuilder().append(stringify(bc, o.getDeclaringClass())).append('(').append(stringify(bc, o.getParameterTypes())).append(')').toString();
	}

	private static String stringify(BeanConverter bc, Method o) {
		return new StringBuilder().append(o.getName()).append('(').append(stringify(bc, o.getParameterTypes())).append(')').toString();
	}

	private static String stringify(BeanConverter bc, Class[] o) {
		return Arrays.stream(o).map(x -> stringify(bc, x)).collect(joining(","));
	}
}
