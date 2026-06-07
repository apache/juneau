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
 * Juneau {@link org.apache.juneau.marshall.Marshalled @Marshalled} types for the
 * <a href="https://stateless.group/hal_specification.html">HAL (Hypertext Application Language)</a>
 * hypermedia format, also covered by
 * <a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08">draft-kelly-json-hal-08</a>.
 *
 * <h5 class='topic'>Overview</h5>
 *
 * <p>
 * HAL is a JSON-based hypermedia format used by RESTful APIs to express links and embedded resources alongside the
 * payload itself. This module provides typed Juneau beans modeling the HAL wire format — no marshaller is needed; the
 * existing {@link org.apache.juneau.marshall.json.JsonSerializer} / {@link org.apache.juneau.marshall.json.JsonParser} already produce
 * and consume {@code application/hal+json}. The matching {@code ContentType} constant is
 * {@code org.apache.juneau.http.header.ContentType#APPLICATION_HAL_JSON} (in {@code juneau-rest-common}).
 *
 * <h5 class='topic'>Bean Classes</h5>
 * <ul class='spaced-list'>
 *   <li>{@link org.apache.juneau.bean.hal.HalResource} - Top-level HAL resource. Carries
 *     {@code _links}, {@code _embedded}, and arbitrary payload fields.
 *   <li>{@link org.apache.juneau.bean.hal.HalLink} - HAL Link Object with the 8 spec fields.
 *   <li>{@link org.apache.juneau.bean.hal.HalLinkArray} - {@code LinkedList&lt;HalLink&gt;} used as
 *     the multi-link branch inside an {@code _links} map.
 *   <li>{@link org.apache.juneau.bean.hal.HalResourceArray} - {@code LinkedList&lt;HalResource&gt;}
 *     used as the multi-resource branch inside an {@code _embedded} map.
 *   <li>{@link org.apache.juneau.bean.hal.HalLinkOrArraySwap} - {@link org.apache.juneau.marshall.swap.ObjectSwap}
 *     that materializes each {@code _links} value as either a single {@link org.apache.juneau.bean.hal.HalLink}
 *     (JSON object) or a {@link org.apache.juneau.bean.hal.HalLinkArray} (JSON array) per spec.
 *   <li>{@link org.apache.juneau.bean.hal.HalResourceOrArraySwap} - The {@code _embedded} analogue.
 * </ul>
 *
 * <h5 class='topic'>Example</h5>
 *
 * <p class='bjava'>
 * 	<jc>// Build a HAL resource with a self link, a curies array, and embedded items.</jc>
 * 	HalResource <jv>order</jv> = <jk>new</jk> HalResource()
 * 		.addLink(<js>"self"</js>, <jk>new</jk> HalLink().setHref(<js>"/orders/123"</js>))
 * 		.addLinks(<js>"curies"</js>,
 * 			<jk>new</jk> HalLink().setName(<js>"acme"</js>).setHref(<js>"https://acme.example/{rel}"</js>).setTemplated(<jk>true</jk>))
 * 		.set(<js>"total"</js>, 99.50);
 *
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>order</jv>);
 * 	HalResource <jv>back</jv> = JsonParser.<jsf>DEFAULT</jsf>.parse(<jv>json</jv>, HalResource.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://stateless.group/hal_specification.html">HAL Specification</a>
 *   <li class='link'><a href="https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08">draft-kelly-json-hal-08</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanHal">juneau-bean-hal</a>
 * </ul>
 */
package org.apache.juneau.bean.hal;
