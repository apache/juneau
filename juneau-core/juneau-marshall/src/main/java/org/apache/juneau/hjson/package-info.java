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

/**
 * Hjson (Human JSON) format serializer and parser for Apache Juneau.
 *
 * <p>
 * Hjson extends JSON with human-friendly features designed for configuration files and hand-edited data:
 * </p>
 * <ul class='spaced-list'>
 * 	<li>Quoteless strings — values without quotes when unambiguous (e.g. <c>name: Alice</c>)
 * 	<li>Multiline strings — triple single-quote <c>'''</c> blocks with indentation stripping
 * 	<li>Comments — <c>#</c>, <c>//</c>, and block comments (<c>/&#42; ... &#42;/</c>)
 * 	<li>Optional commas — newlines can separate object members and array elements
 * 	<li>Trailing commas — ignored
 * 	<li>Unquoted keys — object keys need not be quoted when safe
 * 	<li>Root-braceless objects — top-level content without wrapping <c>{}</c> (parse only)
 * </ul>
 *
 * <h5 class='section'>Syntax summary</h5>
 * <p>
 * Strings: quoteless (to end of line), double-quoted, single-quoted, or multiline <c>'''...'''</c>.
 * Numbers, booleans (<c>true</c>/<c>false</c>), and <c>null</c> follow JSON rules.
 * Objects use <c>{}</c> with <c>key: value</c> pairs; arrays use <c>[]</c>.
 * </p>
 *
 * <h5 class='section'>Java mapping</h5>
 * <p>
 * Objects → <c>JsonMap</c>; arrays → <c>JsonList</c>;
 * strings → <c>String</c>; numbers → <c>Number</c> subclasses; booleans → <c>Boolean</c>; null → <jk>null</jk>.
 * Beans are created when <c>_type</c> is present or when parsing to a specific bean class.
 * </p>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	String <jv>hjson</jv> = HjsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 * 	MyBean <jv>bean</jv> = HjsonParser.<jsf>DEFAULT</jsf>.parse(<jv>hjson</jv>, MyBean.<jk>class</jk>);
 *
 * 	<jc>// Or use the marshaller:</jc>
 * 	String <jv>hjson</jv> = Hjson.<jsf>of</jsf>(<jv>myBean</jv>);
 * 	MyBean <jv>bean</jv> = Hjson.<jsf>to</jsf>(<jv>hjson</jv>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HjsonBasics">Hjson Basics</a>
 * 	<li class='link'><a class="doclink" href="https://hjson.github.io/syntax.html">Hjson Specification</a>
 * </ul>
 */
package org.apache.juneau.hjson;
