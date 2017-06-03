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
package org.apache.juneau.utils;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.juneau.*;

/**
 * Utility class to make it easier to work with command-line arguments pass in through a <code>main(String[] args)</code> method.
 * <p>
 * Used to parse command-line arguments of the form <js>"[zero or more main arguments] [zero or more optional arguments]"</js>.
 * <p>
 * The format of a main argument is a token that does not start with <js>'-'</js>.
 * <p>
 * The format of an optional argument is <js>"-argName [zero or more tokens]"</js>.
 * <p>
 * <h6 class='topic'>Command-line examples</h6>
 * <ul>
 * 	<li><code>java com.sample.MyClass mainArg1</code>
 * 	<li><code>java com.sample.MyClass mainArg1 mainArg2</code>
 * 	<li><code>java com.sample.MyClass mainArg1 -optArg1</code>
 * 	<li><code>java com.sample.MyClass -optArg1</code>
 * 	<li><code>java com.sample.MyClass mainArg1 -optArg1 optArg1Val</code>
 * 	<li><code>java com.sample.MyClass mainArg1 -optArg1 optArg1Val1 optArg1Val2</code>
 * 	<li><code>java com.sample.MyClass mainArg1 -optArg1 optArg1Val1 -optArg1 optArg1Val2</code>
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode'>
 *
 * 	<jc>// Main method with arguments</jc>
 * 	<jk>public static void</jk> <jsm>main</jsm>(String[] args) {
 *
 * 		<jc>// Wrap in Args</jc>
 * 		Args a = new Args(args);
 *
 * 		<jc>// One main argument</jc>
 * 		<jc>// a1</jc>
 * 		String a1 = a.getArg(0); <jc>// "a1"</jc>
 * 		String a2 = a.getArg(1); <jc>// null</jc>
 *
 * 		<jc>// Two main arguments</jc>
 * 		<jc>// a1 a2</jc>
 * 		String a1 = a.getArg(0); <jc>// "a1"</jc>
 * 		String a2 = a.getArg(1); <jc>// "a2"</jc>
 *
 * 		<jc>// One main argument and one optional argument with no value</jc>
 * 		<jc>// a1 -a2</jc>
 * 		String a1 = a.getArg(0);
 * 		<jk>boolean</jk> hasA2 = a.hasArg(<js>"a2"</js>); <jc>// true</jc>
 * 		<jk>boolean</jk> hasA3 = a.hasArg(<js>"a3"</js>); <jc>// false</jc>
 *
 * 		<jc>// One main argument and one optional argument with one value</jc>
 * 		<jc>// a1 -a2 v2</jc>
 * 		String a1 = a.getArg(0);
 * 		String a2 = a.getArg(<js>"a2"</js>); <jc>// "v2"</jc>
 * 		String a3 = a.getArg(<js>"a3"</js>); <jc>// null</jc>
 *
 * 		<jc>// One main argument and one optional argument with two values</jc>
 * 		<jc>// a1 -a2 v2a v2b</jc>
 * 		String a1 = a.getArg(0);
 * 		List&lt;String&gt; a2 = a.getArgs(<js>"a2"</js>); <jc>// Contains ["v2a","v2b"]</jc>
 * 		List&lt;String&gt; a3 = a.getArgs(<js>"a3"</js>); <jc>// Empty list</jc>
 *
 * 		<jc>// Same as previous, except specify optional argument name multiple times</jc>
 * 		<jc>// a1 -a2 v2a -a2 v2b</jc>
 * 		String a1 = a.getArg(0);
 * 		List&lt;String&gt; a2 = a.getArgs(<js>"a2"</js>); <jc>// Contains ["v2a","v2b"]</jc>
 * 	}
 * </p>
 * <p>
 * Main arguments are available through numeric string keys (e.g. <js>"0"</js>, <js>"1"</js>, ...).
 * So you could use the {@link ObjectMap} API to convert main arguments directly to POJOs, such as an <code>Enum</code>
 * <p class='bcode'>
 * 	<jc>// Get 1st main argument as an Enum</jc>
 * 	MyEnum e = a.get(MyEnum.<jk>class</jk>, <js>"0"</js>);
 *
 * 	<jc>// Get 1st main argument as an integer</jc>
 * 	int i = a.get(<jk>int</jk>.<jk>class</jk>, <js>"0"</js>);
 * </p>
 * <p>
 * Equivalent operations are available on optional arguments through the {@link #getArg(Class, String)} method.
 */
public final class Args extends ObjectMap {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param args Arguments passed in through a <code>main(String[] args)</code> method.
	 */
	public Args(String[] args) {
		List<String> argList = new LinkedList<String>(Arrays.asList(args));

		// Capture the main arguments.
		Integer i = 0;
		while (! argList.isEmpty()) {
			String s = argList.get(0);
			if (startsWith(s,'-'))
				break;
			put(i.toString(), argList.remove(0));
			i++;
		}

		// Capture the mapped arguments.
		String key = null;
		while (! argList.isEmpty()) {
			String s = argList.remove(0);
			if (startsWith(s, '-')) {
				key = s.substring(1);
				if (key.matches("\\d*"))
					throw new RuntimeException("Invalid optional key name '"+key+"'");
				if (! containsKey(key))
					put(key, new ObjectList());
			} else {
				((ObjectList)get(key)).add(s);
			}
		}
	}

	/**
	 * Returns main argument at the specified index, or <jk>null</jk> if the index is out of range.
	 * <p>
	 * Can be used in conjuction with {@link #hasArg(int)} to check for existence of arg.
	 * <p class='bcode'>
	 * 	<jc>// Check for no arguments</jc>
	 * 	<jk>if</jk> (! args.hasArg(0))
	 * 		printUsageAndExit();
	 *
	 * 	<jc>// Get the first argument</jc>
	 * 	String firstArg = args.getArg(0);
	 * </p>
	 * <p>
	 * Since main arguments are stored as numeric keys, this method is essentially equivalent to...
	 * <p class='bcode'>
	 * 	<jc>// Check for no arguments</jc>
	 * 	<jk>if</jk> (! args.containsKey(<js>"0"</js>))
	 * 		printUsageAndExit();
	 *
	 * 	<jc>// Get the first argument</jc>
	 * 	String firstArg = args.getString("0");
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
	 * Returns the optional argument value, or blank if the optional argument was not specified.
	 * <p>
	 * If the optional arg has multiple values, returns values as a comma-delimited list.
	 *
	 * @param name The optional argument name.
	 * @return The optional argument value, or blank if the optional argument was not specified.
	 */
	public String getArg(String name) {
		ObjectList l = (ObjectList)get(name);
		if (l == null || l.size() == 0)
			return null;
		if (l.size() == 1)
			return l.get(0).toString();
		return Arrays.toString(l.toArray()).replaceAll("[\\[\\]]", "");
	}

	/**
	 * Returns the optional argument value converted to the specified object type.
	 * <p>
	 * If the optional arg has multiple values, returns only the first converted value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Command:  java com.sample.MyClass -verbose true -debug 5</jc>
	 * 	<jk>boolean</jk> b = args.getArg(<jk>boolean</jk>.<jk>class</jk>, <js>"verbose"</js>);
	 * 	<jk>int</jk> i = args.getArg(<jk>int</jk>.<jk>class</jk>, <js>"debug"</js>);
	 * </p>
	 *
	 * @param c The class type to convert the value to.
	 * @param <T> The class type to convert the value to.
	 * @param name The optional argument name.
	 * @return The optional argument value, or blank if the optional argument was not specified.
	 */
	public <T> T getArg(Class<T> c, String name) {
		ObjectList l = (ObjectList)get(name);
		if (l == null || l.size() == 0)
			return null;
		return l.get(c, 0);
	}

	/**
	 * Returns the optional argument values as a list of strings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Command:  java com.sample.MyClass -extraArgs foo bar baz</jc>
	 * 	List&lt;String&gt; l1 = args.getArgs(<js>"extraArgs"</js>); <jc>// ['foo','bar','baz']</jc>
	 * 	List&lt;String&gt; l2 = args.getArgs(<js>"nonExistentArgs"</js>); <jc>// An empty list</jc>
	 * </p>
	 *
	 * @param name The optional argument name.
	 * @return The optional argument values, or an empty list if the optional argument was not specified.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<String> getArgs(String name) {
		List l = (ObjectList)get(name);
		if (l == null)
			return Collections.emptyList();
		return l;
	}
}
