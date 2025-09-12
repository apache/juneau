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
 * Functional interface for pre-processing and transforming objects before conversion.
 *
 * <p>Swappers provide object transformation logic that runs before stringification or
 * listification. They enable unwrapping, preprocessing, and value extraction from
 * wrapper objects, containers, and lazy evaluation constructs.</p>
 *
 * <h5 class='section'>Key Features:</h5>
 * <ul>
 *    <li><b>Pre-processing:</b> Transform objects before string/list conversion</li>
 *    <li><b>Wrapper unwrapping:</b> Extract values from Optional, Supplier, Future, etc.</li>
 *    <li><b>Lazy evaluation:</b> Trigger computation of lazy values</li>
 *    <li><b>Type transformation:</b> Convert between related types for better testing</li>
 * </ul>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul>
 *    <li><b>Optional unwrapping:</b> Extract values from Optional containers</li>
 *    <li><b>Supplier evaluation:</b> Call get() on Supplier objects</li>
 *    <li><b>Future resolution:</b> Extract completed values from Future objects</li>
 *    <li><b>Proxy unwrapping:</b> Extract underlying objects from proxies</li>
 *    <li><b>Value extraction:</b> Pull relevant data from complex wrapper objects</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 *    <jc>// Future value extraction</jc>
 *    Swapper&lt;CompletableFuture&gt; <jv>futureSwapper</jv> = (<jp>conv</jp>, <jp>future</jp>) -&gt; {
 *       <jk>try</jk> {
 *          <jk>return</jk> <jp>future</jp>.isDone() ? <jp>future</jp>.get() : <js>"&lt;pending&gt;"</js>;
 *       } <jk>catch</jk> (Exception <jv>e</jv>) {
 *          <jk>return</jk> <js>"&lt;error: "</js> + <jv>e</jv>.getMessage() + <js>"&gt;"</js>;
 *       }
 *    };
 *
 *    <jc>// Custom wrapper unwrapping</jc>
 *    Swapper&lt;LazyValue&gt; <jv>lazySwapper</jv> = (<jp>conv</jp>, <jp>lazy</jp>) -&gt;
 *       <jp>lazy</jp>.isEvaluated() ? <jp>lazy</jp>.getValue() : <js>"&lt;unevaluated&gt;"</js>;
 *
 *    <jc>// Entity to DTO conversion</jc>
 *    Swapper&lt;UserEntity&gt; <jv>entitySwapper</jv> = (<jp>conv</jp>, <jp>entity</jp>) -&gt;
 *       <jk>new</jk> UserDTO(<jp>entity</jp>.getId(), <jp>entity</jp>.getName(), <jp>entity</jp>.getEmail());
 * </p>
 *
 * <h5 class='section'>Registration:</h5>
 * <p class='bjava'>
 *    <jk>var</jk> <jv>converter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
 *       .defaultSettings()
 *       .addSwapper(CompletableFuture.<jk>class</jk>, <jv>futureSwapper</jv>)
 *       .addSwapper(LazyValue.<jk>class</jk>, <jv>lazySwapper</jv>)
 *       .addSwapper(UserEntity.<jk>class</jk>, <jv>entitySwapper</jv>)
 *       .build();
 * </p>
 *
 * <h5 class='section'>Execution Flow:</h5>
 * <p>Swappers are applied early in the conversion process:</p>
 * <ol>
 *    <li><b>Object received</b> for conversion</li>
 *    <li><b>Swapper applied</b> if one is registered for the object's type</li>
 *    <li><b>Result processed</b> through normal stringification/listification</li>
 *    <li><b>Chain continues</b> recursively for nested objects</li>
 * </ol>
 *
 * <h5 class='section'>Best Practices:</h5>
 * <ul>
 *    <li><b>Handle exceptions</b> gracefully, returning error indicators rather than throwing</li>
 *    <li><b>Preserve semantics</b> - the swapped object should represent the same logical value</li>
 *    <li><b>Consider performance</b> - swappers are called frequently during conversion</li>
 *    <li><b>Return meaningful values</b> for null or invalid states</li>
 *    <li><b>Avoid recursion</b> - ensure swappers don't create circular transformations that could 
 *        lead to {@link StackOverflowError}. The framework does not detect recursion, so developers
 *        must ensure swapped objects don't trigger the same or related swappers in an endless cycle</li>
 * </ul>
 *
 * @param <T> The type of object this swapper handles
 * @see BasicBeanConverter.Builder#addSwapper(Class, Swapper)
 * @see BeanConverter#swap(Object)
 */
@FunctionalInterface
public interface Swapper<T> extends BiFunction<BeanConverter,T,Object> {}
