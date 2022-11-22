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
package org.apache.juneau.collections;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

import org.apache.juneau.*;

/**
 * Utility class to make it easier to work with command-line arguments pass in through a
 * <c>main(String[] args)</c> method.
 *
 * <p>
 * Used to parse command-line arguments of the form
 * <js>"[zero or more main arguments] [zero or more optional arguments]"</js>.
 *
 * <p>
 * The format of a main argument is a token that does not start with <js>'-'</js>.
 *
 * <p>
 * The format of an optional argument is <js>"-argName [zero or more tokens]"</js>.
 *
 * <h5 class='topic'>Command-line examples</h5>
 * <ul>
 * 	<li><c>java com.sample.MyClass mainArg1</c>
 * 	<li><c>java com.sample.MyClass mainArg1 mainArg2</c>
 * 	<li><c>java com.sample.MyClass mainArg1 -optArg1</c>
 * 	<li><c>java com.sample.MyClass -optArg1</c>
 * 	<li><c>java com.sample.MyClass mainArg1 -optArg1 optArg1Val</c>
 * 	<li><c>java com.sample.MyClass mainArg1 -optArg1 optArg1Val1 optArg1Val2</c>
 * 	<li><c>java com.sample.MyClass mainArg1 -optArg1 optArg1Val1 -optArg1 optArg1Val2</c>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *
 * 	<jc>// Main method with arguments</jc>
 * 	<jk>public static void</jk> <jsm>main</jsm>(String[] <jv>_args</jv>) {
 *
 * 		<jc>// Wrap in Args</jc>
 * 		Args <jv>args</jv> = <jk>new</jk> Args(<jv>_args</jv>);
 *
 * 		<jc>// One main argument</jc>
 * 		<jc>// a1</jc>
 * 		String <jv>a1</jv> = <jv>args</jv>.getArg(0); <jc>// "a1"</jc>
 * 		String <jv>a2</jv> = <jv>args</jv>.getArg(1); <jc>// null</jc>
 *
 * 		<jc>// Two main arguments</jc>
 * 		<jc>// a1 a2</jc>
 * 		String <jv>a1</jv> = <jv>args</jv>.getArg(0); <jc>// "a1"</jc>
 * 		String <jv>a2</jv> = <jv>args</jv>.getArg(1); <jc>// "a2"</jc>
 *
 * 		<jc>// One main argument and one optional argument with no value</jc>
 * 		<jc>// a1 -a2</jc>
 * 		String <jv>a1</jv> = <jv>args</jv>.getArg(0);
 * 		<jk>boolean</jk> <jv>hasA2</jv> = <jv>args</jv>.hasArg(<js>"a2"</js>); <jc>// true</jc>
 * 		<jk>boolean</jk> <jv>hasA3</jv> = <jv>args</jv>.hasArg(<js>"a3"</js>); <jc>// false</jc>
 *
 * 		<jc>// One main argument and one optional argument with one value</jc>
 * 		<jc>// a1 -a2 v2</jc>
 * 		String <jv>a1</jv> = <jv>args</jv>.getArg(0);
 * 		String <jv>a2</jv> = <jv>args</jv>.getArg(<js>"a2"</js>); <jc>// "v2"</jc>
 * 		String <jv>a3</jv> = <jv>args</jv>.getArg(<js>"a3"</js>); <jc>// null</jc>
 *
 * 		<jc>// One main argument and one optional argument with two values</jc>
 * 		<jc>// a1 -a2 v2a v2b</jc>
 * 		String a1 = a.getArg(0);
 * 		List&lt;String&gt; <jv>a2</jv> = <jv>args</jv>.getArgs(<js>"a2"</js>); <jc>// Contains ["v2a","v2b"]</jc>
 * 		List&lt;String&gt; <jv>a3</jv> = <jv>args</jv>.getArgs(<js>"a3"</js>); <jc>// Empty list</jc>
 *
 * 		<jc>// Same as previous, except specify optional argument name multiple times</jc>
 * 		<jc>// a1 -a2 v2a -a2 v2b</jc>
 * 		String <jv>a1</jv> = <jv>args</jv>.getArg(0);
 * 		List&lt;String&gt; <jv>a2</jv> = <jv>args</jv>.getArgs(<js>"a2"</js>); <jc>// Contains ["v2a","v2b"]</jc>
 * 	}
 * </p>
 *
 * <p>
 * Main arguments are available through numeric string keys (e.g. <js>"0"</js>, <js>"1"</js>, ...).
 * So you could use the {@link JsonMap} API to convert main arguments directly to POJOs, such as an <c>Enum</c>
 * <p class='bjava'>
 * 	<jc>// Get 1st main argument as an Enum</jc>
 * 	MyEnum <jv>_enum</jv> = <jv>args</jv>.get(MyEnum.<jk>class</jk>, <js>"0"</js>);
 *
 * 	<jc>// Get 1st main argument as an integer</jc>
 * 	<jk>int</jk> <jv>_int</jv> = <jv>args</jv>.get(<jk>int</jk>.<jk>class</jk>, <js>"0"</js>);
 * </p>
 *
 * <p>
 * Equivalent operations are available on optional arguments through the {@link #getArg(Class, String)} method.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public final class Args extends JsonMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param args Arguments passed in through a <c>main(String[] args)</c> method.
	 */
	public Args(String[] args) {
		List<String> argList = linkedList(args);

		// Capture the main arguments.
		int i = 0;
		while (! argList.isEmpty()) {
			String s = argList.get(0);
			if (startsWith(s,'-'))
				break;
			put(Integer.toString(i), argList.remove(0));
			i++;
		}

		// Capture the mapped arguments.
		String key = null;
		while (! argList.isEmpty()) {
			String s = argList.remove(0);
			if (startsWith(s, '-')) {
				key = s.substring(1);
				if (key.matches("\\d*"))
					throw new BasicRuntimeException("Invalid optional key name ''{0}''", key);
				if (! containsKey(key))
					put(key, new JsonList());
			} else {
				((JsonList)get(key)).add(s);
			}
		}
	}

	/**
	 * Constructor.
	 *
	 * @param args Arguments passed in as a raw command line.
	 */
	public Args(String args) {
		this(splitQuoted(args));
	}

	/**
	 * Returns main argument at the specified index, or <jk>null</jk> if the index is out of range.
	 *
	 * <p>
	 * Can be used in conjunction with {@link #hasArg(int)} to check for existence of arg.
	 * <p class='bjava'>
	 * 	<jc>// Check for no arguments</jc>
	 * 	<jk>if</jk> (! <jv>args</jv>.hasArg(0))
	 * 		<jsm>printUsageAndExit</jsm>();
	 *
	 * 	<jc>// Get the first argument</jc>
	 * 	String <jv>firstArg</jv> = <jv>args</jv>.getArg(0);
	 * </p>
	 *
	 * <p>
	 * Since main arguments are stored as numeric keys, this method is essentially equivalent to...
	 * <p class='bjava'>
	 * 	<jc>// Check for no arguments</jc>
	 * 	<jk>if</jk> (! <jv>args</jv>.containsKey(<js>"0"</js>))
	 * 		<jsm>printUsageAndExit</jsm>();
	 *
	 * 	<jc>// Get the first argument</jc>
	 * 	String <jv>firstArg</jv> = <jv>args</jv>.getString(<js>"0"</js>);
	 * </p>
	 *
	 * @param i The index position of the main argument (zero-indexed).
	 * @return The main argument value, or <js>""</js> if argument doesn't exist at that position.
	 */
	public String getArg(int i) {
		return getString(Integer.toString(i));
	}

	/**
	 * Returns <jk>true</jk> if argument exists at specified index.
	 *
	 * @param i The zero-indexed position of the argument.
	 * @return <jk>true</jk> if argument exists at specified index.
	 */
	public boolean hasArg(int i) {
		return containsKey(Integer.toString(i));
	}

	/**
	 * Returns <jk>true</jk> if the named argument exists.
	 *
	 * @param name The argument name.
	 * @return <jk>true</jk> if the named argument exists.
	 */
	public boolean hasArg(String name) {
		JsonList l = (JsonList)get(name);
		return l != null;
	}

	/**
	 * Returns the optional argument value, or blank if the optional argument was not specified.
	 *
	 * <p>
	 * If the optional arg has multiple values, returns values as a comma-delimited list.
	 *
	 * @param name The optional argument name.
	 * @return The optional argument value, or blank if the optional argument was not specified.
	 */
	public String getArg(String name) {
		JsonList l = (JsonList)get(name);
		if (l == null || l.size() == 0)
			return null;
		if (l.size() == 1)
			return l.get(0).toString();
		return Arrays.toString(l.toArray()).replaceAll("[\\[\\]]", "");
	}

	/**
	 * Returns the optional argument value converted to the specified object type.
	 *
	 * <p>
	 * If the optional arg has multiple values, returns only the first converted value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Command:  java com.sample.MyClass -verbose true -debug 5</jc>
	 * 	<jk>boolean</jk> <jv>bool</jv> = <jv>args</jv>.getArg(<jk>boolean</jk>.<jk>class</jk>, <js>"verbose"</js>);
	 * 	<jk>int</jk> <jv>_int</jv> = <jv>args</jv>.getArg(<jk>int</jk>.<jk>class</jk>, <js>"debug"</js>);
	 * </p>
	 *
	 * @param c The class type to convert the value to.
	 * @param <T> The class type to convert the value to.
	 * @param name The optional argument name.
	 * @return The optional argument value, or blank if the optional argument was not specified.
	 */
	public <T> T getArg(Class<T> c, String name) {
		JsonList l = (JsonList)get(name);
		if (l == null || l.size() == 0)
			return null;
		return l.get(0, c);
	}

	/**
	 * Returns the optional argument values as a list of strings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Command:  java com.sample.MyClass -extraArgs foo bar baz</jc>
	 * 	List&lt;String&gt; <jv>list1</jv> = <jv>args</jv>.getArgs(<js>"extraArgs"</js>); <jc>// ['foo','bar','baz']</jc>
	 * 	List&lt;String&gt; <jv>list2</jv> = <jv>args</jv>.getArgs(<js>"nonExistentArgs"</js>); <jc>// An empty list</jc>
	 * </p>
	 *
	 * @param name The optional argument name.
	 * @return The optional argument values, or an empty list if the optional argument was not specified.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<String> getArgs(String name) {
		List l = (JsonList)get(name);
		if (l == null)
			return Collections.emptyList();
		return l;
	}
}
