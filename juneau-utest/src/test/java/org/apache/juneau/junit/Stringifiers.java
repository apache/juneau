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

import static java.time.format.DateTimeFormatter.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.junit.Utils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/**
 * Collection of standard stringifier implementations for the Bean-Centric Testing framework.
 *
 * <p>This class provides built-in string conversion strategies that handle common Java types
 * and objects. These stringifiers are automatically registered when using
 * {@link BasicBeanConverter.Builder#defaultSettings()}.</p>
 *
 * <h5 class='section'>Purpose:</h5>
 * <p>Stringifiers convert objects to human-readable string representations for use in BCT
 * assertions and test output. They provide consistent, meaningful string formats across
 * different object types while supporting customization for specific testing needs.</p>
 *
 * <h5 class='section'>Built-in Stringifiers:</h5>
 * <ul>
 * 	<li><b>{@link #mapEntryStringifier()}</b> - Converts {@link Map.Entry} to <js>"key=value"</js> format</li>
 * 	<li><b>{@link #calendarStringifier()}</b> - Converts {@link GregorianCalendar} to ISO-8601 format</li>
 * 	<li><b>{@link #dateStringifier()}</b> - Converts {@link Date} to ISO instant format</li>
 * 	<li><b>{@link #inputStreamStringifier()}</b> - Converts {@link InputStream} content to hex strings</li>
 * 	<li><b>{@link #byteArrayStringifier()}</b> - Converts byte arrays to hex strings</li>
 * 	<li><b>{@link #readerStringifier()}</b> - Converts {@link Reader} content to strings</li>
 * 	<li><b>{@link #fileStringifier()}</b> - Converts {@link File} content to strings</li>
 * 	<li><b>{@link #enumStringifier()}</b> - Converts {@link Enum} values to name format</li>
 * 	<li><b>{@link #classStringifier()}</b> - Converts {@link Class} objects to name format</li>
 * 	<li><b>{@link #constructorStringifier()}</b> - Converts {@link Constructor} to signature format</li>
 * 	<li><b>{@link #methodStringifier()}</b> - Converts {@link Method} to signature format</li>
 * 	<li><b>{@link #listStringifier()}</b> - Converts {@link List} to bracket-delimited format</li>
 * 	<li><b>{@link #mapStringifier()}</b> - Converts {@link Map} to brace-delimited format</li>
 * </ul>
 *
 * <h5 class='section'>Usage Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Register stringifiers using builder</jc>
 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 * 		.defaultSettings()
 * 		.addStringifier(Date.<jk>class</jk>, Stringifiers.<jsm>dateStringifier</jsm>())
 * 		.addStringifier(File.<jk>class</jk>, Stringifiers.<jsm>fileStringifier</jsm>())
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Resource Handling:</h5>
 * <p><b>Warning:</b> Some stringifiers consume or close their input resources:</p>
 * <ul>
 * 	<li><b>{@link InputStream}:</b> Stream is consumed and closed during stringification</li>
 * 	<li><b>{@link Reader}:</b> Reader is consumed and closed during stringification</li>
 * 	<li><b>{@link File}:</b> File content is read completely during stringification</li>
 * </ul>
 *
 * <h5 class='section'>Custom Stringifier Development:</h5>
 * <p>When creating custom stringifiers, follow these patterns:</p>
 * <ul>
 * 	<li><b>Null Safety:</b> Handle <jk>null</jk> inputs gracefully</li>
 * 	<li><b>Resource Management:</b> Properly close resources after use</li>
 * 	<li><b>Exception Handling:</b> Convert exceptions to meaningful error messages</li>
 * 	<li><b>Performance:</b> Consider string building efficiency for complex objects</li>
 * 	<li><b>Readability:</b> Ensure output is useful for debugging and assertions</li>
 * </ul>
 *
 * @see Stringifier
 * @see BasicBeanConverter.Builder#addStringifier(Class, Stringifier)
 * @see BasicBeanConverter.Builder#defaultSettings()
 */
@SuppressWarnings({"rawtypes"})
public class Stringifiers {

	private static final char[] HEX = "0123456789ABCDEF".toCharArray();

	/**
	 * Constructor.
	 */
	private Stringifiers() {}

	/**
	 * Returns a stringifier for {@link Map.Entry} objects that formats them as <js>"key=value"</js>.
	 *
	 * <p>This stringifier creates a human-readable representation of map entries by converting
	 * both the key and value to strings and joining them with the configured entry separator.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> Uses the pattern <js>"{key}{separator}{value}"</js></li>
	 * 	<li><b>Separator:</b> Uses the {@code mapEntrySeparator} setting (default: <js>"="</js>)</li>
	 * 	<li><b>Recursive conversion:</b> Both key and value are converted using the same converter</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test map entry stringification</jc>
	 * 	<jk>var</jk> <jv>entry</jv> = Map.<jsm>entry</jsm>(<js>"name"</js>, <js>"John"</js>);
	 * 	<jsm>assertBean</jsm>(<jv>entry</jv>, <js>"&lt;self&gt;"</js>, <js>"name=John"</js>);
	 *
	 * 	<jc>// Test with custom separator</jc>
	 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 * 		.defaultSettings()
	 * 		.addSetting(<jsf>SETTING_mapEntrySeparator</jsf>, <js>": "</js>)
	 * 		.build();
	 * 	<jsm>assertBean</jsm>(<jv>entry</jv>, <js>"&lt;self&gt;"</js>, <js>"name: John"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link Map.Entry} objects
	 * @see Map.Entry
	 */
	public static Stringifier<Map.Entry> mapEntryStringifier() {
		return (bc, entry) -> bc.stringify(entry.getKey()) + bc.getSetting("mapEntrySeparator", "=") + bc.stringify(entry.getValue());
	}

	/**
	 * Returns a stringifier for {@link GregorianCalendar} objects that formats them as ISO-8601 strings.
	 *
	 * <p>This stringifier converts calendar objects to standardized ISO-8601 timestamp format,
	 * which provides consistent, sortable, and internationally recognized date representations.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> Uses the {@code calendarFormat} setting (default: {@link java.time.format.DateTimeFormatter#ISO_INSTANT})</li>
	 * 	<li><b>Timezone:</b> Respects the calendar's timezone information</li>
	 * 	<li><b>Precision:</b> Includes full precision available in the calendar</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test calendar stringification</jc>
	 * 	<jk>var</jk> <jv>calendar</jv> = <jk>new</jk> GregorianCalendar(<jv>2023</jv>, Calendar.<jsf>JANUARY</jsf>, <jv>15</jv>);
	 * 	<jsm>assertMatchesGlob</jsm>(<js>"2023-01-*"</js>, <jv>calendar</jv>);
	 *
	 * 	<jc>// Test with custom format</jc>
	 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 * 		.defaultSettings()
	 * 		.addSetting(<jsf>SETTING_calendarFormat</jsf>, DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>)
	 * 		.build();
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link GregorianCalendar} objects
	 * @see GregorianCalendar
	 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
	 */
	public static Stringifier<GregorianCalendar> calendarStringifier() {
		return (bc, calendar) -> calendar.toZonedDateTime().format(bc.getSetting("calendarFormat", ISO_INSTANT));
	}

	/**
	 * Returns a stringifier for {@link Date} objects that formats them as ISO instant strings.
	 *
	 * <p>This stringifier converts Date objects to ISO-8601 instant format, providing
	 * standardized timestamp representations suitable for logging and comparison.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> ISO-8601 instant format (e.g., <js>"2023-01-15T10:30:00Z"</js>)</li>
	 * 	<li><b>Timezone:</b> Always represents time in UTC (Z timezone)</li>
	 * 	<li><b>Precision:</b> Millisecond precision as available in Date objects</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test date stringification</jc>
	 * 	<jk>var</jk> <jv>date</jv> = <jk>new</jk> Date(<jv>1673780400000L</jv>); <jc>// 2023-01-15T10:00:00Z</jc>
	 * 	<jsm>assertBean</jsm>(<jv>date</jv>, <js>"&lt;self&gt;"</js>, <js>"2023-01-15T10:00:00Z"</js>);
	 *
	 * 	<jc>// Test in object property</jc>
	 * 	<jk>var</jk> <jv>event</jv> = <jk>new</jk> Event().setTimestamp(<jv>date</jv>);
	 * 	<jsm>assertBean</jsm>(<jv>event</jv>, <js>"timestamp"</js>, <js>"2023-01-15T10:00:00Z"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link Date} objects
	 * @see Date
	 */
	public static Stringifier<Date> dateStringifier() {
		return (bc, date) -> date.toInstant().toString();
	}

	/**
	 * Returns a stringifier for {@link InputStream} objects that converts content to hex strings.
	 *
	 * <p><b>Warning:</b> This stringifier consumes and closes the input stream during conversion.
	 * After stringification, the stream cannot be used again.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Content reading:</b> Reads all available bytes from the stream</li>
	 * 	<li><b>Hex conversion:</b> Converts bytes to uppercase hexadecimal representation</li>
	 * 	<li><b>Resource management:</b> Automatically closes the stream after reading</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test with byte content</jc>
	 * 	<jk>var</jk> <jv>stream</jv> = <jk>new</jk> ByteArrayInputStream(<jk>new</jk> <jk>byte</jk>[]{<jv>0x48</jv>, <jv>0x65</jv>, <jv>0x6C</jv>, <jv>0x6C</jv>, <jv>0x6F</jv>});
	 * 	<jsm>assertBean</jsm>(<jv>stream</jv>, <js>"&lt;self&gt;"</js>, <js>"48656C6C6F"</js>); <jc>// "Hello" in hex</jc>
	 *
	 * 	<jc>// Test empty stream</jc>
	 * 	<jk>var</jk> <jv>empty</jv> = <jk>new</jk> ByteArrayInputStream(<jk>new</jk> <jk>byte</jk>[<jv>0</jv>]);
	 * 	<jsm>assertBean</jsm>(<jv>empty</jv>, <js>"&lt;self&gt;"</js>, <js>""</js>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 * 	<li><b>One-time use:</b> The stream is consumed and closed during conversion</li>
	 * 	<li><b>Memory usage:</b> All content is loaded into memory for conversion</li>
	 * 	<li><b>Exception handling:</b> IO exceptions are wrapped in RuntimeException</li>
	 * </ul>
	 *
	 * @return A {@link Stringifier} for {@link InputStream} objects
	 * @see InputStream
	 */
	public static Stringifier<InputStream> inputStreamStringifier() {
		return (bc, stream) -> stringifyInputStream(stream);
	}

	/**
	 * Returns a stringifier for byte arrays that converts them to hex strings.
	 *
	 * <p>This stringifier provides a consistent way to represent binary data as readable
	 * hexadecimal strings, useful for testing and debugging binary content.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Hex format:</b> Each byte is represented as two uppercase hex digits</li>
	 * 	<li><b>No separators:</b> Bytes are concatenated without spaces or delimiters</li>
	 * 	<li><b>Empty arrays:</b> Returns empty string for zero-length arrays</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test byte array stringification</jc>
	 * 	<jk>byte</jk>[] <jv>data</jv> = {<jv>0x48</jv>, <jv>0x65</jv>, <jv>0x6C</jv>, <jv>0x6C</jv>, <jv>0x6F</jv>};
	 * 	<jsm>assertBean</jsm>(<jv>data</jv>, <js>"&lt;self&gt;"</js>, <js>"48656C6C6F"</js>); <jc>// "Hello" in hex</jc>
	 *
	 * 	<jc>// Test with zeros and high values</jc>
	 * 	<jk>byte</jk>[] <jv>mixed</jv> = {<jv>0x00</jv>, <jv>0xFF</jv>, <jv>0x7F</jv>};
	 * 	<jsm>assertBean</jsm>(<jv>mixed</jv>, <js>"&lt;self&gt;"</js>, <js>"00FF7F"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for byte arrays
	 */
	public static Stringifier<byte[]> byteArrayStringifier() {
		return (bc, bytes) -> {
			var sb = new StringBuilder(bytes.length * 2);
			for (var element : bytes) {
				var v = element & 0xFF;
				sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
			}
			return sb.toString();
		};
	}

	/**
	 * Returns a stringifier for {@link Reader} objects that converts content to strings.
	 *
	 * <p><b>Warning:</b> This stringifier consumes and closes the reader during conversion.
	 * After stringification, the reader cannot be used again.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Content reading:</b> Reads all available characters from the reader</li>
	 * 	<li><b>String conversion:</b> Converts characters directly to string format</li>
	 * 	<li><b>Resource management:</b> Automatically closes the reader after reading</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test with string content</jc>
	 * 	<jk>var</jk> <jv>reader</jv> = <jk>new</jk> StringReader(<js>"Hello World"</js>);
	 * 	<jsm>assertBean</jsm>(<jv>reader</jv>, <js>"&lt;self&gt;"</js>, <js>"Hello World"</js>);
	 *
	 * 	<jc>// Test with file reader</jc>
	 * 	<jk>var</jk> <jv>fileReader</jv> = Files.<jsm>newBufferedReader</jsm>(path);
	 * 	<jsm>assertMatchesGlob</jsm>(<js>"*expected content*"</js>, <jv>fileReader</jv>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 * 	<li><b>One-time use:</b> The reader is consumed and closed during conversion</li>
	 * 	<li><b>Memory usage:</b> All content is loaded into memory for conversion</li>
	 * 	<li><b>Exception handling:</b> IO exceptions are wrapped in RuntimeException</li>
	 * </ul>
	 *
	 * @return A {@link Stringifier} for {@link Reader} objects
	 * @see Reader
	 */
	public static Stringifier<Reader> readerStringifier() {
		return (bc, reader) -> stringifyReader(reader);
	}

	/**
	 * Returns a stringifier for {@link File} objects that converts file content to strings.
	 *
	 * <p>This stringifier reads the entire file content and returns it as a string,
	 * making it useful for testing file-based operations and content verification.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Content reading:</b> Reads the entire file content into memory</li>
	 * 	<li><b>Encoding:</b> Uses the default platform encoding for text files</li>
	 * 	<li><b>Resource management:</b> Properly closes file resources after reading</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test file content</jc>
	 * 	<jk>var</jk> <jv>configFile</jv> = <jk>new</jk> File(<js>"config.properties"</js>);
	 * 	<jsm>assertMatchesGlob</jsm>(<js>"*database.url=*"</js>, <jv>configFile</jv>);
	 *
	 * 	<jc>// Test empty file</jc>
	 * 	<jk>var</jk> <jv>emptyFile</jv> = <jk>new</jk> File(<js>"empty.txt"</js>);
	 * 	<jsm>assertBean</jsm>(<jv>emptyFile</jv>, <js>"&lt;self&gt;"</js>, <js>""</js>);
	 * </p>
	 *
	 * <h5 class='section'>Important Notes:</h5>
	 * <ul>
	 * 	<li><b>Memory usage:</b> Large files will consume significant memory</li>
	 * 	<li><b>File existence:</b> Non-existent files will cause exceptions</li>
	 * 	<li><b>Binary files:</b> May produce unexpected results with binary content</li>
	 * 	<li><b>Exception handling:</b> IO exceptions are wrapped in RuntimeException</li>
	 * </ul>
	 *
	 * @return A {@link Stringifier} for {@link File} objects
	 * @see File
	 */
	public static Stringifier<File> fileStringifier() {
		return (bc, file) -> safe(() -> stringifyReader(Files.newBufferedReader(file.toPath())));
	}

	/**
	 * Returns a stringifier for {@link Enum} objects that converts them to name format.
	 *
	 * <p>This stringifier provides a consistent way to represent enum values as their
	 * declared constant names, which is typically the most useful format for testing.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Name format:</b> Uses {@link Enum#name()} method for string representation</li>
	 * 	<li><b>Case preservation:</b> Maintains the exact case as declared in enum</li>
	 * 	<li><b>All enum types:</b> Works with any enum implementation</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test enum stringification</jc>
	 * 	<jsm>assertBean</jsm>(Color.<jsf>RED</jsf>, <js>"&lt;self&gt;"</js>, <js>"RED"</js>);
	 * 	<jsm>assertBean</jsm>(Status.<jsf>IN_PROGRESS</jsf>, <js>"&lt;self&gt;"</js>, <js>"IN_PROGRESS"</js>);
	 *
	 * 	<jc>// Test in object property</jc>
	 * 	<jk>var</jk> <jv>task</jv> = <jk>new</jk> Task().setStatus(Status.<jsf>COMPLETED</jsf>);
	 * 	<jsm>assertBean</jsm>(<jv>task</jv>, <js>"status"</js>, <js>"COMPLETED"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Alternative Formats:</h5>
	 * <p>If you need different enum string representations (like {@link Enum#toString()}
	 * or custom formatting), register a custom stringifier for specific enum types.</p>
	 *
	 * @return A {@link Stringifier} for {@link Enum} objects
	 * @see Enum
	 * @see Enum#name()
	 */
	public static Stringifier<Enum> enumStringifier() {
		return (bc, enumValue) -> enumValue.name();
	}

	/**
	 * Returns a stringifier for {@link Class} objects that formats them according to configured settings.
	 *
	 * <p>This stringifier provides flexible class name formatting, supporting different
	 * levels of detail from simple names to fully qualified class names.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format options:</b> Controlled by {@code classNameFormat} setting</li>
	 * 	<li><b>Simple format:</b> Class simple name (default)</li>
	 * 	<li><b>Canonical format:</b> Fully qualified canonical name</li>
	 * 	<li><b>Full format:</b> Complete class name including package</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test with default simple format</jc>
	 * 	<jsm>assertBean</jsm>(String.<jk>class</jk>, <js>"&lt;self&gt;"</js>, <js>"String"</js>);
	 * 	<jsm>assertBean</jsm>(ArrayList.<jk>class</jk>, <js>"&lt;self&gt;"</js>, <js>"ArrayList"</js>);
	 *
	 * 	<jc>// Test with canonical format</jc>
	 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 * 		.defaultSettings()
	 * 		.addSetting(<jsf>SETTING_classNameFormat</jsf>, <js>"canonical"</js>)
	 * 		.build();
	 * 	<jsm>assertBean</jsm>(String.<jk>class</jk>, <js>"&lt;self&gt;"</js>, <js>"java.lang.String"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link Class} objects
	 * @see Class
	 */
	public static Stringifier<Class> classStringifier() {
		return (bc, clazz) -> stringifyClass(bc, clazz);
	}

	/**
	 * Returns a stringifier for {@link Constructor} objects that formats them as readable signatures.
	 *
	 * <p>This stringifier creates human-readable constructor signatures including the
	 * declaring class name and parameter types, useful for reflection-based testing.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> <js>"{ClassName}({paramType1},{paramType2},...)"</js></li>
	 * 	<li><b>Class names:</b> Uses the configured class name format</li>
	 * 	<li><b>Parameter types:</b> Includes all parameter types in declaration order</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test constructor stringification</jc>
	 * 	<jk>var</jk> <jv>constructor</jv> = String.<jk>class</jk>.getConstructor(<jk>char</jk>[].<jk>class</jk>);
	 * 	<jsm>assertBean</jsm>(<jv>constructor</jv>, <js>"&lt;self&gt;"</js>, <js>"String(char[])"</js>);
	 *
	 * 	<jc>// Test no-arg constructor</jc>
	 * 	<jk>var</jk> <jv>defaultConstructor</jv> = ArrayList.<jk>class</jk>.getConstructor();
	 * 	<jsm>assertBean</jsm>(<jv>defaultConstructor</jv>, <js>"&lt;self&gt;"</js>, <js>"ArrayList()"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link Constructor} objects
	 * @see Constructor
	 */
	public static Stringifier<Constructor> constructorStringifier() {
		return (bc, constructor) -> {
			return new StringBuilder()
				.append(stringifyClass(bc, ((Constructor<?>) constructor).getDeclaringClass()))
				.append('(')
				.append(Arrays.stream((constructor).getParameterTypes())
				.map(x -> stringifyClass(bc, x))
				.collect(joining(",")))
				.append(')')
				.toString();
		};
	}

	/**
	 * Returns a stringifier for {@link Method} objects that formats them as readable signatures.
	 *
	 * <p>This stringifier creates human-readable method signatures including the method
	 * name and parameter types, useful for reflection-based testing and debugging.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> <js>"{methodName}({paramType1},{paramType2},...)"</js></li>
	 * 	<li><b>Method name:</b> Uses the declared method name</li>
	 * 	<li><b>Parameter types:</b> Includes all parameter types in declaration order</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test method stringification</jc>
	 * 	<jk>var</jk> <jv>method</jv> = String.<jk>class</jk>.getMethod(<js>"substring"</js>, <jk>int</jk>.<jk>class</jk>, <jk>int</jk>.<jk>class</jk>);
	 * 	<jsm>assertBean</jsm>(<jv>method</jv>, <js>"&lt;self&gt;"</js>, <js>"substring(int,int)"</js>);
	 *
	 * 	<jc>// Test no-arg method</jc>
	 * 	<jk>var</jk> <jv>toString</jv> = Object.<jk>class</jk>.getMethod(<js>"toString"</js>);
	 * 	<jsm>assertBean</jsm>(<jv>toString</jv>, <js>"&lt;self&gt;"</js>, <js>"toString()"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link Method} objects
	 * @see Method
	 */
	public static Stringifier<Method> methodStringifier() {
		return (bc, method) -> {
			return new StringBuilder()
				.append(method.getName())
				.append('(')
				.append(Arrays.stream(method.getParameterTypes())
				.map(x -> stringifyClass(bc, x))
				.collect(joining(",")))
				.append(')')
				.toString();
		};
	}

	/**
	 * Returns a stringifier for {@link List} objects that formats them with configurable delimiters.
	 *
	 * <p>This stringifier converts lists to bracket-delimited strings with customizable
	 * separators and prefixes/suffixes, providing consistent list representation across tests.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> <js>"{prefix}{element1}{separator}{element2}...{suffix}"</js></li>
	 * 	<li><b>Separator:</b> Uses {@code fieldSeparator} setting (default: <js>","</js>)</li>
	 * 	<li><b>Prefix:</b> Uses {@code collectionPrefix} setting (default: <js>"["</js>)</li>
	 * 	<li><b>Suffix:</b> Uses {@code collectionSuffix} setting (default: <js>"]"</js>)</li>
	 * 	<li><b>Recursive:</b> Elements are converted using the same converter</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test list stringification</jc>
	 * 	<jk>var</jk> <jv>list</jv> = List.<jsm>of</jsm>(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>);
	 * 	<jsm>assertBean</jsm>(<jv>list</jv>, <js>"&lt;self&gt;"</js>, <js>"[apple,banana,cherry]"</js>);
	 *
	 * 	<jc>// Test with custom formatting</jc>
	 * 	<jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 * 		.defaultSettings()
	 * 		.addSetting(<jsf>SETTING_fieldSeparator</jsf>, <js>"; "</js>)
	 * 		.addSetting(<jsf>SETTING_collectionPrefix</jsf>, <js>"("</js>)
	 * 		.addSetting(<jsf>SETTING_collectionSuffix</jsf>, <js>")"</js>)
	 * 		.build();
	 * 	<jsm>assertBean</jsm>(<jv>list</jv>, <js>"&lt;self&gt;"</js>, <js>"(apple; banana; cherry)"</js>);
	 * </p>
	 *
	 * @return A {@link Stringifier} for {@link List} objects
	 * @see List
	 */
	public static Stringifier<List> listStringifier() {
		return (bc, list) -> ((List<?>)list).stream()
			.map(bc::stringify)
			.collect(joining(
				bc.getSetting("fieldSeparator", ","),
				bc.getSetting("collectionPrefix", "["),
				bc.getSetting("collectionSuffix", "]")
			));
	}

	/**
	 * Returns a stringifier for {@link Map} objects that formats them with configurable delimiters.
	 *
	 * <p>This stringifier converts maps to brace-delimited strings by first converting the
	 * map to a list of entries and then stringifying each entry, providing consistent
	 * map representation across tests.</p>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul>
	 * 	<li><b>Format:</b> <js>"{prefix}{entry1}{separator}{entry2}...{suffix}"</js></li>
	 * 	<li><b>Separator:</b> Uses {@code fieldSeparator} setting (default: <js>","</js>)</li>
	 * 	<li><b>Prefix:</b> Uses {@code mapPrefix} setting (default: <js>"{"</js>)</li>
	 * 	<li><b>Suffix:</b> Uses {@code mapSuffix} setting (default: <js>"}"</js>)</li>
	 * 	<li><b>Entry format:</b> Each entry uses the map entry stringifier</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test map stringification</jc>
	 * 	<jk>var</jk> <jv>map</jv> = Map.<jsm>of</jsm>(<js>"name"</js>, <js>"John"</js>, <js>"age"</js>, <jv>25</jv>);
	 * 	<jsm>assertMatchesGlob</jsm>(<js>"{*name=John*age=25*}"</js>, <jv>map</jv>);
	 *
	 * 	<jc>// Test empty map</jc>
	 * 	<jk>var</jk> <jv>emptyMap</jv> = Map.<jsm>of</jsm>();
	 * 	<jsm>assertBean</jsm>(<jv>emptyMap</jv>, <js>"&lt;self&gt;"</js>, <js>"{}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Order Considerations:</h5>
	 * <p>The order of entries in the string depends on the map implementation's iteration
	 * order. Use order-independent assertions (like {@code assertMatchesGlob}) for maps where
	 * order is not guaranteed.</p>
	 *
	 * @return A {@link Stringifier} for {@link Map} objects
	 * @see Map
	 * @see Map.Entry
	 */
	public static Stringifier<Map> mapStringifier() {
		return (bc, map) -> ((Map<?,?>)map).entrySet().stream()
			.map(bc::stringify)
			.collect(joining(
				bc.getSetting("fieldSeparator", ","),
				bc.getSetting("mapPrefix", "{"),
				bc.getSetting("mapSuffix", "}")
			));
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods for internal stringification
	//---------------------------------------------------------------------------------------------

	/**
	 * Converts an InputStream to a hexadecimal string representation.
	 *
	 * @param stream The InputStream to convert
	 * @return Hexadecimal string representation of the stream content
	 */
	private static String stringifyInputStream(InputStream stream) {
		return safe(() -> {
			try (var o2 = stream) {
				var buff = new ByteArrayOutputStream(1024);
				var nRead = 0;
				var b = new byte[1024];
				while ((nRead = o2.read(b, 0, b.length)) != -1)
					buff.write(b, 0, nRead);
				buff.flush();
				byte[] bytes = buff.toByteArray();
				var sb = new StringBuilder(bytes.length * 2);
				for (var element : bytes) {
					var v = element & 0xFF;
					sb.append(HEX[v >>> 4]).append(HEX[v & 0x0F]);
				}
				return sb.toString();
			}
		});
	}

	/**
	 * Converts a Reader to a string representation.
	 *
	 * @param reader The Reader to convert
	 * @return String content from the reader
	 */
	private static String stringifyReader(Reader reader) {
		return safe(() -> {
			try (var o2 = reader) {
				var sb = new StringBuilder();
				var buf = new char[1024];
				var i = 0;
				while ((i = o2.read(buf)) != -1)
					sb.append(buf, 0, i);
				return sb.toString();
			}
		});
	}

	/**
	 * Converts a Class to a string representation based on converter settings.
	 *
	 * @param bc The bean converter for accessing settings
	 * @param clazz The Class to convert
	 * @return String representation of the class
	 */
	private static String stringifyClass(BeanConverter bc, Class<?> clazz) {
		return switch(bc.getSetting("classNameFormat", "default")) {
			case "simple" -> clazz.getSimpleName();
			case "canonical" -> clazz.getCanonicalName();
			default -> clazz.getName();
		};
	}
}
