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
package org.apache.juneau.common.collections;

import java.util.*;

/**
 * A specialized set for storing and efficiently looking up language keywords.
 *
 * <p>
 * This class provides a lightweight, immutable container optimized for fast keyword lookups using binary search.
 * Keywords are stored in a sorted array, making lookups O(log n) efficient. This is particularly useful for
 * parsers, compilers, and syntax highlighters that need to frequently check if identifiers are reserved keywords.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Immutable after construction - thread-safe for concurrent reads
 * 	<li>Efficient O(log n) lookups using binary search
 * 	<li>Compact memory footprint using sorted array
 * 	<li>Automatic rejection of null and single-character strings
 * </ul>
 *
 * <h5 class='section'>Implementation Details:</h5>
 * <p>
 * Keywords are sorted lexicographically during construction and stored in an internal array. The {@link #contains(String)}
 * method uses {@link Arrays#binarySearch(Object[], Object)} for efficient lookups. Strings shorter than 2 characters
 * are automatically rejected without performing a search, as most programming languages don't have single-character
 * keywords.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a keyword set for Java reserved words</jc>
 * 	KeywordSet <jv>javaKeywords</jv> = <jk>new</jk> KeywordSet(
 * 		<js>"abstract"</js>, <js>"assert"</js>, <js>"boolean"</js>, <js>"break"</js>, <js>"byte"</js>,
 * 		<js>"case"</js>, <js>"catch"</js>, <js>"char"</js>, <js>"class"</js>, <js>"const"</js>
 * 		<jc>// ... more keywords</jc>
 * 	);
 *
 * 	<jc>// Check if identifiers are keywords</jc>
 * 	<jk>if</jk> (<jv>javaKeywords</jv>.contains(<js>"class"</js>)) {
 * 		<jc>// Handle keyword</jc>
 * 	}
 *
 * 	<jc>// Safe handling of edge cases</jc>
 * 	<jsm>assertFalse</jsm>(<jv>javaKeywords</jv>.contains(<jk>null</jk>));     <jc>// Returns false</jc>
 * 	<jsm>assertFalse</jsm>(<jv>javaKeywords</jv>.contains(<js>"a"</js>));      <jc>// Single char - returns false</jc>
 * 	<jsm>assertFalse</jsm>(<jv>javaKeywords</jv>.contains(<js>"myVar"</js>));  <jc>// Not a keyword</jc>
 *
 * 	<jc>// Use in a parser/lexer</jc>
 * 	<jk>class</jk> Lexer {
 * 		<jk>private static final</jk> KeywordSet KEYWORDS = <jk>new</jk> KeywordSet(
 * 			<js>"if"</js>, <js>"else"</js>, <js>"while"</js>, <js>"for"</js>, <js>"return"</js>
 * 		);
 *
 * 		<jk>boolean</jk> isKeyword(String token) {
 * 			<jk>return</jk> KEYWORDS.contains(token);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Programming language parsers - checking if tokens are reserved words
 * 	<li>Syntax highlighters - identifying keywords for special formatting
 * 	<li>Code analyzers - distinguishing keywords from identifiers
 * 	<li>Template engines - recognizing template keywords
 * 	<li>Query languages - validating reserved words
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>immutable and thread-safe</b> after construction. Multiple threads can safely call
 * 		{@link #contains(String)} concurrently.
 * 	<li class='note'>
 * 		Keywords are compared using exact string matching (case-sensitive). For case-insensitive matching,
 * 		normalize your keywords and input strings to the same case.
 * 	<li class='note'>
 * 		The minimum keyword length is 2 characters. Single-character strings are automatically rejected.
 * 	<li class='note'>
 * 		Consider creating {@link KeywordSet} instances as static final constants to avoid repeated construction.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class KeywordSet {
	final String[] store;

	/**
	 * Creates a new keyword set with the specified keywords.
	 *
	 * <p>
	 * Keywords are automatically sorted during construction. Duplicate keywords are allowed but provide
	 * no benefit. For best performance, pass unique keywords.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a keyword set for SQL keywords</jc>
	 * 	KeywordSet <jv>sql</jv> = <jk>new</jk> KeywordSet(
	 * 		<js>"SELECT"</js>, <js>"FROM"</js>, <js>"WHERE"</js>, <js>"INSERT"</js>, <js>"UPDATE"</js>,
	 * 		<js>"DELETE"</js>, <js>"CREATE"</js>, <js>"DROP"</js>, <js>"TABLE"</js>, <js>"INDEX"</js>
	 * 	);
	 *
	 * 	<jc>// Keywords can be passed in any order</jc>
	 * 	KeywordSet <jv>keywords</jv> = <jk>new</jk> KeywordSet(<js>"zebra"</js>, <js>"apple"</js>, <js>"banana"</js>);
	 * 	<jsm>assertTrue</jsm>(<jv>keywords</jv>.contains(<js>"apple"</js>));  <jc>// Sorted internally</jc>
	 * </p>
	 *
	 * @param keywords The keywords to store. Can be empty but not <jk>null</jk>. Individual keywords can be any non-null string.
	 */
	public KeywordSet(String...keywords) {
		this.store = keywords;
		Arrays.sort(store);
	}

	/**
	 * Checks if the specified string is a keyword in this set.
	 *
	 * <p>
	 * This method performs an O(log n) binary search on the sorted keyword array. Null strings and
	 * strings with fewer than 2 characters are automatically rejected without performing a search.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	KeywordSet <jv>keywords</jv> = <jk>new</jk> KeywordSet(<js>"class"</js>, <js>"interface"</js>, <js>"enum"</js>);
	 *
	 * 	<jc>// Standard checks</jc>
	 * 	<jsm>assertTrue</jsm>(<jv>keywords</jv>.contains(<js>"class"</js>));      <jc>// Keyword exists</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>keywords</jv>.contains(<js>"MyClass"</js>));   <jc>// Not a keyword</jc>
	 *
	 * 	<jc>// Edge cases handled gracefully</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>keywords</jv>.contains(<jk>null</jk>));        <jc>// null returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>keywords</jv>.contains(<js>""</js>));          <jc>// Empty string returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>keywords</jv>.contains(<js>"a"</js>));         <jc>// Single char returns false</jc>
	 *
	 * 	<jc>// Case-sensitive matching</jc>
	 * 	<jsm>assertTrue</jsm>(<jv>keywords</jv>.contains(<js>"class"</js>));
	 * 	<jsm>assertFalse</jsm>(<jv>keywords</jv>.contains(<js>"CLASS"</js>));     <jc>// Different case</jc>
	 * </p>
	 *
	 * <h5 class='section'>Performance:</h5>
	 * <ul>
	 * 	<li>Time complexity: O(log n) using binary search
	 * 	<li>Space complexity: O(1) - no additional memory allocated
	 * 	<li>Short-circuit: Strings with length &lt; 2 return immediately without searching
	 * </ul>
	 *
	 * @param s The string to check. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the string exists in this keyword set, <jk>false</jk> if it doesn't exist,
	 *         is <jk>null</jk>, or has fewer than 2 characters.
	 */
	public boolean contains(String s) {
		if (s == null || s.length() < 2)
			return false;
		return Arrays.binarySearch(store, s) >= 0;
	}
}