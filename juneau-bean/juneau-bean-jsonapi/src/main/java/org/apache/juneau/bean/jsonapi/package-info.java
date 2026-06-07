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
 * Juneau {@link org.apache.juneau.Marshalled @Marshalled} types for the
 * <a href="https://jsonapi.org/format/">JSON:API v1.1</a> wire format.
 *
 * <h5 class='topic'>Overview</h5>
 *
 * <p>
 * JSON:API is a media-type ({@code application/vnd.api+json}) for building APIs with shared conventions for
 * resources, relationships, links, errors, and metadata. This module provides typed Juneau beans modeling the
 * JSON:API wire format - no marshaller is needed; the existing {@link org.apache.juneau.json.JsonSerializer} /
 * {@link org.apache.juneau.json.JsonParser} already produce and consume the wire format. The matching
 * {@code ContentType} constant is {@code org.apache.juneau.http.header.ContentType#APPLICATION_VND_API_JSON}.
 *
 * <h5 class='topic'>Bean Classes</h5>
 * <ul class='spaced-list'>
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiDocument} - Top-level container with
 *     {@code data}/{@code errors}/{@code meta}/{@code jsonapi}/{@code links}/{@code included}.
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiResource} - Resource object with
 *     {@code type}/{@code id}/{@code attributes}/{@code relationships}/{@code links}/{@code meta}.
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiResourceIdentifier} - Slim form used inside relationship linkage.
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiRelationship} - Relationship object
 *     ({@code data} linkage, {@code links}, {@code meta}).
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiError} - Error object with
 *     {@code id}/{@code links}/{@code status}/{@code code}/{@code title}/{@code detail}/{@code source}/{@code meta}.
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiErrorSource} - The {@code source} sub-object
 *     ({@code pointer}/{@code parameter}/{@code header}).
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiLink} - Link object with
 *     {@code href}/{@code rel}/{@code describedby}/{@code title}/{@code type}/{@code hreflang}/{@code meta}.
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiVersion} - The {@code jsonapi} member
 *     ({@code version}/{@code meta}).
 *   <li>{@link org.apache.juneau.bean.jsonapi.JsonApiLinkOrStringSwap} -
 *     {@link org.apache.juneau.swap.ObjectSwap} for {@code links} map values; each value can be either a JSON
 *     string URL or a {@link org.apache.juneau.bean.jsonapi.JsonApiLink} object per the spec.
 * </ul>
 *
 * <h5 class='topic'>Important: {@code JsonApiResource.type} is a plain String</h5>
 *
 * <p>
 * JSON:API uses {@code type} as an open-ended string field on resource objects (the entity-type name, e.g.
 * {@code "articles"}); the value is chosen by the API and is <b>not</b> a closed Java class hierarchy. Therefore
 * {@code JsonApiResource} models {@code type} as a plain {@code String} and is <b>not</b> annotated with
 * {@code @Marshalled(typePropertyName="type", dictionary=...)}. Conflating JSON:API resource typing with Juneau
 * bean dispatch would break round-tripping of unknown {@code type} values and force every API to enumerate its
 * resource types in a Java dictionary up front.
 *
 * <h5 class='topic'>Document-level mutual exclusion</h5>
 *
 * <p>
 * Per the JSON:API spec, a document MUST contain at least one of {@code data}, {@code errors}, or {@code meta};
 * {@code data} and {@code errors} MUST NOT coexist. {@link org.apache.juneau.bean.jsonapi.JsonApiDocument}
 * documents this constraint and offers an optional {@code validate()} helper.
 *
 * <h5 class='topic'>Example</h5>
 *
 * <p class='bjava'>
 * 	<jc>// Build a compound document with included resources.</jc>
 * 	JsonApiResource <jv>article</jv> = <jk>new</jk> JsonApiResource()
 * 		.setType(<js>"articles"</js>).setId(<js>"1"</js>)
 * 		.putAttribute(<js>"title"</js>, <js>"JSON:API paints my bikeshed!"</js>);
 * 	JsonApiDocument <jv>doc</jv> = <jk>new</jk> JsonApiDocument().setData(<jv>article</jv>);
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>doc</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://jsonapi.org/format/">JSON:API v1.1</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonApi">juneau-bean-jsonapi</a>
 * </ul>
 */
package org.apache.juneau.bean.jsonapi;
