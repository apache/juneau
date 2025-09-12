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

import java.util.function.*;

/**
 * Functional interface for converting objects of a specific type to their string representation.
 *
 * <p>Stringifiers provide custom string conversion logic for specific object types within the
 * Bean-Centric Testing framework. They enable precise control over how objects are displayed
 * in test assertions and error messages, going beyond simple {@code toString()} calls.</p>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 *    <jc>// Simple custom format</jc>
 *    Stringifier&lt;LocalDate&gt; <jv>dateStringifier</jv> = (<jp>conv</jp>, <jp>date</jp>) -&gt;
 *       <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>);
 *
 *    <jc>// Complex nested object formatting</jc>
 *    Stringifier&lt;Person&gt; <jv>personStringifier</jv> = (<jp>conv</jp>, <jp>person</jp>) -&gt;
 *       <jp>person</jp>.getName() + <js>" ("</js> + <jp>conv</jp>.stringify(<jp>person</jp>.getAddress()) + <js>")"</js>;
 *
 *    <jc>// Conditional formatting</jc>
 *    Stringifier&lt;Order&gt; <jv>orderStringifier</jv> = (<jp>conv</jp>, <jp>order</jp>) -&gt; {
 *       <jk>var</jk> <jv>status</jv> = <jp>order</jp>.getStatus();
 *       <jk>return</jk> <jv>status</jv> == OrderStatus.<jsf>COMPLETED</jsf> ?
 *          <js>"Order #"</js> + <jp>order</jp>.getId() + <js>" [DONE]"</js> :
 *          <js>"Order #"</js> + <jp>order</jp>.getId() + <js>" ["</js> + <jv>status</jv> + <js>"]"</js>;
 *    };
 * </p>
 *
 * <h5 class='section'>Registration:</h5>
 * <p class='bjava'>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addStringifier(LocalDate.<jk>class</jk>, <jv>dateStringifier</jv>)
 *       .addStringifier(Person.<jk>class</jk>, <jv>personStringifier</jv>)
 *       .addStringifier(Order.<jk>class</jk>, <jv>orderStringifier</jv>)
 *       .build();
 * </p>
 *
 * <h5 class='section'>Best Practices:</h5>
 * <ul>
 *    <li><b>Use the converter parameter</b> for recursive conversion of nested objects</li>
 *    <li><b>Keep output concise</b> but informative for test readability</li>
 *    <li><b>Handle edge cases</b> gracefully (empty collections, special values)</li>
 *    <li><b>Consider test context</b> - focus on properties relevant to testing</li>
 * </ul>
 *
 * @param <T> The type of object this stringifier handles
 * @see BasicBeanConverter.Builder#addStringifier(Class, Stringifier)
 * @see BeanConverter#stringify(Object)
 */
@FunctionalInterface
public interface Stringifier<T> extends BiFunction<BeanConverter,T,String> {}
