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
 * Juneau {@link org.apache.juneau.annotation.Marshalled @Marshalled} types for
 * <a href="https://datatracker.ietf.org/doc/html/rfc6902">JSON Patch (RFC 6902)</a>.
 *
 * <h5 class='topic'>Overview</h5>
 *
 * <p>
 * A JSON Patch document is a JSON array of operation objects describing changes to apply to a target JSON
 * document. Each operation object has an {@code op} member naming one of the six operation types
 * ({@code add}, {@code remove}, {@code replace}, {@code move}, {@code copy}, {@code test}), a {@code path}
 * member (JSON Pointer per RFC 6901), and op-specific additional members. The matching {@code ContentType}
 * constant is {@code org.apache.juneau.http.header.ContentType#APPLICATION_JSON_PATCH}.
 *
 * <h5 class='topic'>Bean Classes</h5>
 * <ul class='spaced-list'>
 *   <li>{@link org.apache.juneau.bean.jsonpatch.JsonPatchOperation} - Abstract base. Polymorphic dispatch
 *     wired via {@code @Marshalled(typePropertyName="op", dictionary={...})}, so the parser materializes the
 *     correct concrete subclass keyed off the {@code op} member.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.AddOp} - {@code op:"add"} + {@code value}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.RemoveOp} - {@code op:"remove"}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.ReplaceOp} - {@code op:"replace"} + {@code value}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.MoveOp} - {@code op:"move"} + {@code from}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.CopyOp} - {@code op:"copy"} + {@code from}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.TestOp} - {@code op:"test"} + {@code value}.
 *   <li>{@link org.apache.juneau.bean.jsonpatch.JsonPatch} - {@code LinkedList&lt;JsonPatchOperation&gt;};
 *     the JSON Patch <i>document</i> serializes as a top-level JSON array (mirrors {@code JsonSchemaArray}).
 * </ul>
 *
 * <h5 class='topic'>Example</h5>
 *
 * <p class='bjava'>
 * 	JsonPatch <jv>patch</jv> = <jk>new</jk> JsonPatch()
 * 		.append(<jk>new</jk> AddOp(<js>"/a/b/c"</js>, <js>"foo"</js>))
 * 		.append(<jk>new</jk> RemoveOp(<js>"/a/b/c"</js>))
 * 		.append(<jk>new</jk> ReplaceOp(<js>"/a/b/c"</js>, 42))
 * 		.append(<jk>new</jk> MoveOp(<js>"/a/b/c"</js>, <js>"/a/b/d"</js>))
 * 		.append(<jk>new</jk> CopyOp(<js>"/a/b/c"</js>, <js>"/a/b/e"</js>))
 * 		.append(<jk>new</jk> TestOp(<js>"/a/b/c"</js>, <js>"foo"</js>));
 *
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>patch</jv>);
 * 	JsonPatch <jv>back</jv> = JsonParser.<jsf>DEFAULT</jsf>.parse(<jv>json</jv>, JsonPatch.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 *   <li class='link'><a href="https://datatracker.ietf.org/doc/html/rfc6902">RFC 6902 - JSON Patch</a>
 *   <li class='link'><a href="https://datatracker.ietf.org/doc/html/rfc6901">RFC 6901 - JSON Pointer</a>
 *   <li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanJsonPatch">juneau-bean-jsonpatch</a>
 * </ul>
 */
package org.apache.juneau.bean.jsonpatch;
