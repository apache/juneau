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

import java.util.*;
import java.util.function.*;

/**
 * Functional interface for converting collection-like objects to standardized List&lt;Object&gt; format.
 *
 * <p>Listifiers enable the Bean-Centric Testing framework to treat diverse collection-like
 * objects uniformly during property access and iteration. They convert various iterable
 * structures into a common List format for consistent processing.</p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul>
 *    <li><b>Arrays:</b> Convert primitive and object arrays to lists</li>
 *    <li><b>Streams:</b> Materialize Stream objects to lists</li>
 *    <li><b>Custom collections:</b> Extract elements from domain-specific collections</li>
 *    <li><b>Map entries:</b> Convert Maps to lists of entry objects</li>
 *    <li><b>Iterables:</b> Handle any Iterable implementation</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 *    <jc>// Custom collection handling</jc>
 *    Listifier&lt;ResultSet&gt; <jv>resultSetListifier</jv> = (<jp>conv</jp>, <jp>rs</jp>) -&gt; {
 *       <jk>var</jk> <jv>results</jv> = <jk>new</jk> ArrayList&lt;&gt;();
 *       <jk>while</jk> (<jp>rs</jp>.next()) {
 *          <jv>results</jv>.add(<jp>rs</jp>.getRowData()); <jc>// Custom row extraction</jc>
 *       }
 *       <jk>return</jk> <jv>results</jv>;
 *    };
 *
 *    <jc>// Stream processing with side effects</jc>
 *    Listifier&lt;Stream&gt; <jv>streamListifier</jv> = (<jp>conv</jp>, <jp>stream</jp>) -&gt;
 *       <jp>stream</jp>.peek(<jv>logProcessor</jv>::log).collect(toList());
 *
 *    <jc>// Custom data structure</jc>
 *    Listifier&lt;Tree&gt; <jv>treeListifier</jv> = (<jp>conv</jp>, <jp>tree</jp>) -&gt;
 *       <jp>tree</jp>.traversePreOrder().collect(toList());
 * </p>
 *
 * <h5 class='section'>Registration:</h5>
 * <p class='bjava'>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addListifier(ResultSet.<jk>class</jk>, <jv>resultSetListifier</jv>)
 *       .addListifier(Stream.<jk>class</jk>, <jv>streamListifier</jv>)
 *       .addListifier(Tree.<jk>class</jk>, <jv>treeListifier</jv>)
 *       .build();
 * </p>
 *
 * <h5 class='section'>Best Practices:</h5>
 * <ul>
 *    <li><b>Preserve order</b> when the original collection has meaningful ordering</li>
 *    <li><b>Handle empty cases</b> gracefully (return empty list, not null)</li>
 *    <li><b>Consider performance</b> for large collections (avoid full materialization when possible)</li>
 *    <li><b>Use converter parameter</b> for swapping/transforming individual elements</li>
 * </ul>
 *
 * @param <T> The type of collection-like object this listifier handles
 * @see BasicBeanConverter.Builder#addListifier(Class, Listifier)
 * @see BeanConverter#listify(Object)
 */
@FunctionalInterface
public interface Listifier<T> extends BiFunction<BeanConverter,T,List<Object>> {}
