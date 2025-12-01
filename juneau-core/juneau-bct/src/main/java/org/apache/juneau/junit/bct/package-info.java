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
 * Bean-Centric Testing Framework.
 *
 * <p>This package provides a comprehensive testing framework that extends JUnit with streamlined assertion methods
 * for Java objects. The Bean-Centric Testing (BCT) framework eliminates verbose test code while providing
 * comprehensive object introspection and comparison capabilities.</p>
 *
 * <h5 class='section'>Key Features:</h5>
 * <ul>
 *    <li><b>Concise Assertions:</b> Replace multiple lines of manual property extraction with single assertion calls</li>
 *    <li><b>Powerful Property Access:</b> Nested objects, collections, arrays, and maps with unified syntax</li>
 *    <li><b>Flexible Comparison:</b> Support for custom converters, formatters, and comparison logic</li>
 *    <li><b>Type Safety:</b> Comprehensive error messages with clear property paths</li>
 *    <li><b>Extensible:</b> Custom property extractors, stringifiers, and conversion logic</li>
 * </ul>
 *
 * <h5 class='section'>Core Classes:</h5>
 * <ul>
 *    <li><b>{@link org.apache.juneau.junit.bct.BctAssertions}:</b> Main assertion methods for BCT</li>
 *    <li><b>{@link org.apache.juneau.junit.bct.BeanConverter}:</b> Interface for object conversion and property access</li>
 *    <li><b>{@link org.apache.juneau.junit.bct.BasicBeanConverter}:</b> Default implementation with extensible type handlers</li>
 * </ul>
 *
 * <h5 class='section'>Quick Start:</h5>
 * <p class='bjava'>
 *    <jk>import static</jk> org.apache.juneau.junit.bct.BctAssertions.*;
 *
 *    <ja>@Test</ja>
 *    <jk>void</jk> testUser() {
 *       User <jv>user</jv> = <jk>new</jk> User(<js>"Alice"</js>, 25, <jk>true</jk>);
 *
 *       <jc>// Test multiple properties at once</jc>
 *       <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name,age,active"</js>, <js>"Alice,25,true"</js>);
 *
 *       <jc>// Test nested objects</jc>
 *       <jsm>assertBean</jsm>(<jv>user</jv>, <js>"address{street,city}"</js>, <js>"{123 Main St,Springfield}"</js>);
 *    }
 * </p>
 *
 * <h5 class='section'>Assertion Method Examples:</h5>
 *
 * <h6 class='figure'>1. {@link org.apache.juneau.junit.bct.BctAssertions#assertBean(Object,String,String) assertBean()}</h6>
 * <p>Tests object properties with support for nested syntax and collection iteration.</p>
 * <p class='bjava'>
 *    User <jv>user</jv> = <jk>new</jk> User(<js>"Bob"</js>, 30);
 *    <jv>user</jv>.setAddress(<jk>new</jk> Address(<js>"456 Oak Ave"</js>, <js>"Denver"</js>));
 *
 *    <jc>// Test simple properties</jc>
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name"</js>, <js>"Bob"</js>);
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name,age"</js>, <js>"Bob,30"</js>);
 *
 *    <jc>// Test nested properties</jc>
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"address.street"</js>, <js>"456 Oak Ave"</js>);
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"address{street,city}"</js>, <js>"{456 Oak Ave,Denver}"</js>);
 * </p>
 *
 * <h6 class='figure'>2. {@link org.apache.juneau.junit.bct.BctAssertions#assertBeans(Object,String,String...) assertBeans()}</h6>
 * <p>Tests collections of objects by extracting and comparing specific fields.</p>
 * <p class='bjava'>
 *    List&lt;User&gt; <jv>users</jv> = Arrays.<jsm>asList</jsm>(
 *       <jk>new</jk> User(<js>"Alice"</js>, 25),
 *       <jk>new</jk> User(<js>"Bob"</js>, 30),
 *       <jk>new</jk> User(<js>"Carol"</js>, 35)
 *    );
 *
 *    <jc>// Test single field across collection</jc>
 *    <jsm>assertBeans</jsm>(<jv>users</jv>, <js>"name"</js>, <js>"Alice"</js>, <js>"Bob"</js>, <js>"Carol"</js>);
 *
 *    <jc>// Test multiple fields</jc>
 *    <jsm>assertBeans</jsm>(<jv>users</jv>, <js>"name,age"</js>, <js>"Alice,25"</js>, <js>"Bob,30"</js>, <js>"Carol,35"</js>);
 * </p>
 *
 * <h6 class='figure'>3. {@link org.apache.juneau.junit.bct.BctAssertions#assertMapped(Object,java.util.function.BiFunction,String,String) assertMapped()}</h6>
 * <p>Tests custom property access using BiFunction for non-standard objects.</p>
 * <p class='bjava'>
 *    Map&lt;String,Object&gt; <jv>data</jv> = <jk>new</jk> HashMap&lt;&gt;();
 *    <jv>data</jv>.put(<js>"name"</js>, <js>"Alice"</js>);
 *    <jv>data</jv>.put(<js>"score"</js>, 95);
 *
 *    <jc>// Custom property extractor for Map objects</jc>
 *    <jsm>assertMapped</jsm>(<jv>data</jv>, (obj, key) -&gt; obj.get(key), <js>"name,score"</js>, <js>"Alice,95"</js>);
 * </p>
 *
 * <h6 class='figure'>4. {@link org.apache.juneau.junit.bct.BctAssertions#assertList(Object,Object...) assertList()}</h6>
 * <p>Tests list/collection elements with varargs for expected values.</p>
 * <p class='bjava'>
 *    List&lt;String&gt; <jv>names</jv> = Arrays.<jsm>asList</jsm>(<js>"Alice"</js>, <js>"Bob"</js>, <js>"Carol"</js>);
 *    String[] <jv>colors</jv> = {<js>"red"</js>, <js>"green"</js>, <js>"blue"</js>};
 *
 *    <jc>// Test list contents</jc>
 *    <jsm>assertList</jsm>(<jv>names</jv>, <js>"Alice"</js>, <js>"Bob"</js>, <js>"Carol"</js>);
 *    <jsm>assertList</jsm>(<jv>colors</jv>, <js>"red"</js>, <js>"green"</js>, <js>"blue"</js>);
 * </p>
 *
 * <h6 class='figure'>5. {@link org.apache.juneau.junit.bct.BctAssertions#assertContains(String,Object) assertContains()}</h6>
 * <p>Tests that a string appears somewhere within the stringified object.</p>
 * <p class='bjava'>
 *    User <jv>user</jv> = <jk>new</jk> User(<js>"Alice Smith"</js>, 25);
 *    List&lt;String&gt; <jv>items</jv> = Arrays.<jsm>asList</jsm>(<js>"apple"</js>, <js>"banana"</js>, <js>"cherry"</js>);
 *
 *    <jc>// Test substring presence</jc>
 *    <jsm>assertContains</jsm>(<js>"Alice"</js>, <jv>user</jv>);
 *    <jsm>assertContains</jsm>(<js>"banana"</js>, <jv>items</jv>);
 * </p>
 *
 * <h6 class='figure'>6. {@link org.apache.juneau.junit.bct.BctAssertions#assertContainsAll(Object,String...) assertContainsAll()}</h6>
 * <p>Tests that all specified strings appear within the stringified object.</p>
 * <p class='bjava'>
 *    User <jv>user</jv> = <jk>new</jk> User(<js>"Alice Smith"</js>, 25);
 *    <jv>user</jv>.setEmail(<js>"alice@example.com"</js>);
 *
 *    <jc>// Test multiple substrings</jc>
 *    <jsm>assertContainsAll</jsm>(<jv>user</jv>, <js>"Alice"</js>, <js>"Smith"</js>, <js>"25"</js>);
 *    <jsm>assertContainsAll</jsm>(<jv>user</jv>, <js>"alice"</js>, <js>"example.com"</js>);
 * </p>
 *
 * <h6 class='figure'>7. {@link org.apache.juneau.junit.bct.BctAssertions#assertEmpty(Object) assertEmpty()}</h6>
 * <p>Tests that collections, arrays, maps, or strings are empty.</p>
 * <p class='bjava'>
 *    List&lt;String&gt; <jv>emptyList</jv> = <jk>new</jk> ArrayList&lt;&gt;();
 *    String[] <jv>emptyArray</jv> = {};
 *    Map&lt;String,String&gt; <jv>emptyMap</jv> = <jk>new</jk> HashMap&lt;&gt;();
 *    String <jv>emptyString</jv> = <js>""</js>;
 *
 *    <jc>// Test empty collections</jc>
 *    <jsm>assertEmpty</jsm>(<jv>emptyList</jv>);
 *    <jsm>assertEmpty</jsm>(<jv>emptyArray</jv>);
 *    <jsm>assertEmpty</jsm>(<jv>emptyMap</jv>);
 *    <jsm>assertEmpty</jsm>(<jv>emptyString</jv>);
 * </p>
 *
 * <h6 class='figure'>8. {@link org.apache.juneau.junit.bct.BctAssertions#assertNotEmpty(Object) assertNotEmpty()}</h6>
 * <p>Tests that collections, arrays, maps, or strings are not empty.</p>
 * <p class='bjava'>
 *    List&lt;String&gt; <jv>names</jv> = Arrays.<jsm>asList</jsm>(<js>"Alice"</js>);
 *    String[] <jv>colors</jv> = {<js>"red"</js>};
 *    Map&lt;String,String&gt; <jv>config</jv> = Map.<jsm>of</jsm>(<js>"key"</js>, <js>"value"</js>);
 *    String <jv>message</jv> = <js>"Hello"</js>;
 *
 *    <jc>// Test non-empty collections</jc>
 *    <jsm>assertNotEmpty</jsm>(<jv>names</jv>);
 *    <jsm>assertNotEmpty</jsm>(<jv>colors</jv>);
 *    <jsm>assertNotEmpty</jsm>(<jv>config</jv>);
 *    <jsm>assertNotEmpty</jsm>(<jv>message</jv>);
 * </p>
 *
 * <h6 class='figure'>9. {@link org.apache.juneau.junit.bct.BctAssertions#assertSize(int,Object) assertSize()}</h6>
 * <p>Tests the size/length of collections, arrays, maps, or strings.</p>
 * <p class='bjava'>
 *    List&lt;String&gt; <jv>names</jv> = Arrays.<jsm>asList</jsm>(<js>"Alice"</js>, <js>"Bob"</js>, <js>"Carol"</js>);
 *    String[] <jv>colors</jv> = {<js>"red"</js>, <js>"green"</js>};
 *    Map&lt;String,Integer&gt; <jv>scores</jv> = Map.<jsm>of</jsm>(<js>"Alice"</js>, 95, <js>"Bob"</js>, 87);
 *    String <jv>message</jv> = <js>"Hello"</js>;
 *
 *    <jc>// Test collection sizes</jc>
 *    <jsm>assertSize</jsm>(3, <jv>names</jv>);
 *    <jsm>assertSize</jsm>(2, <jv>colors</jv>);
 *    <jsm>assertSize</jsm>(2, <jv>scores</jv>);
 *    <jsm>assertSize</jsm>(5, <jv>message</jv>);
 * </p>
 *
 * <h6 class='figure'>10. {@link org.apache.juneau.junit.bct.BctAssertions#assertString(String,Object) assertString()}</h6>
 * <p>Tests the string representation of an object using the configured converter.</p>
 * <p class='bjava'>
 *    User <jv>user</jv> = <jk>new</jk> User(<js>"Alice"</js>, 25);
 *    List&lt;Integer&gt; <jv>numbers</jv> = Arrays.<jsm>asList</jsm>(1, 2, 3);
 *    Date <jv>date</jv> = <jk>new</jk> Date(1609459200000L); <jc>// 2021-01-01</jc>
 *
 *    <jc>// Test string representations</jc>
 *    <jsm>assertString</jsm>(<js>"User(name=Alice, age=25)"</js>, <jv>user</jv>);
 *    <jsm>assertString</jsm>(<js>"[1, 2, 3]"</js>, <jv>numbers</jv>);
 *    <jsm>assertString</jsm>(<js>"2021-01-01"</js>, <jv>date</jv>);
 * </p>
 *
 * <h6 class='figure'>11. {@link org.apache.juneau.junit.bct.BctAssertions#assertMatchesGlob(String,Object) assertMatchesGlob()}</h6>
 * <p>Tests that the stringified object matches a glob-style pattern (* and ? wildcards).</p>
 * <p class='bjava'>
 *    User <jv>user</jv> = <jk>new</jk> User(<js>"Alice Smith"</js>, 25);
 *    String <jv>filename</jv> = <js>"report.pdf"</js>;
 *    String <jv>email</jv> = <js>"alice@company.com"</js>;
 *
 *    <jc>// Test pattern matching</jc>
 *    <jsm>assertMatchesGlob</jsm>(<js>"*Alice*"</js>, <jv>user</jv>);
 *    <jsm>assertMatchesGlob</jsm>(<js>"*.pdf"</js>, <jv>filename</jv>);
 *    <jsm>assertMatchesGlob</jsm>(<js>"*@*.com"</js>, <jv>email</jv>);
 *    <jsm>assertMatchesGlob</jsm>(<js>"User(name=Alice*, age=25)"</js>, <jv>user</jv>);
 * </p>
 *
 * <h5 class='section'>Custom Error Messages:</h5>
 * <p>All assertion methods support custom error messages via a <code>Supplier&lt;String&gt;</code> parameter:</p>
 * <p class='bjava'>
 *    <jc>// Simple custom message</jc>
 *    <jsm>assertBean</jsm>(() -> <js>"User validation failed"</js>, <jv>user</jv>, <js>"name,age"</js>, <js>"Alice,25"</js>);
 *
 *    <jc>// Formatted message using Utils.fs() for convenient message suppliers with arguments</jc>
 *    <jsm>assertBean</jsm>(<jsm>fs</jsm>(<js>"User {0} validation failed"</js>, <js>"Alice"</js>), <jv>user</jv>, <js>"name,age"</js>, <js>"Alice,25"</js>);
 * </p>
 *
 * <h5 class='section'>Customizing the Default Converter:</h5>
 * <p>The default bean converter can be customized on a per-thread basis:</p>
 * <p class='bjava'>
 *    <jc>// Set custom converter in @BeforeEach method</jc>
 *    <ja>@BeforeEach</ja>
 *    <jk>void</jk> <jsm>setUp</jsm>() {
 *       <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *          .defaultSettings()
 *          .addStringifier(LocalDate.<jk>class</jk>, <jp>date</jp> -> <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>))
 *          .build();
 *       BctAssertions.<jsm>setConverter</jsm>(<jv>converter</jv>);
 *    }
 *
 *    <jc>// All assertions now use the custom converter</jc>
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"birthDate"</js>, <js>"2023-12-01"</js>);
 *
 *    <jc>// Reset in @AfterEach method</jc>
 *    <ja>@AfterEach</ja>
 *    <jk>void</jk> <jsm>tearDown</jsm>() {
 *       BctAssertions.<jsm>resetConverter</jsm>();
 *    }
 * </p>
 *
 * @see org.apache.juneau.junit.bct.BctAssertions
 * @see org.apache.juneau.junit.bct.BeanConverter
 * @see org.apache.juneau.junit.bct.BasicBeanConverter
 * @see org.apache.juneau.common.utils.Utils#fs(String, Object...)
 */
package org.apache.juneau.junit.bct;
