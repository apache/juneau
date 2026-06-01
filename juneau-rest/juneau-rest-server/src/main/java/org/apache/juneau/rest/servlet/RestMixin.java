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
package org.apache.juneau.rest.servlet;

import org.apache.juneau.rest.annotation.*;

/**
 * Optional base class for <c>@Rest</c> mixin classes &mdash; the third sibling of {@link RestServlet}
 * (servlet flavor) and {@link RestResource} (child-resource flavor), completing the
 * servlet / resource / mixin naming triad.
 *
 * <p>
 * A <i>mixin</i> is a plain {@link Rest @Rest}-annotated POJO whose {@code @RestOp} methods are pulled
 * into a host servlet/resource context when the host declares
 * {@link Rest#mixins() @Rest(mixins=ThatMixin.class)}. Mixins compose through the existing sub-context
 * model; {@code RestMixin} does <b>not</b> change that composition mechanism.
 *
 * <h5 class='section'>Opt-in, not mandatory:</h5>
 *
 * <p>
 * Extending {@code RestMixin} is <b>optional</b>. A bare {@code @Rest} POJO with no base class remains a
 * fully-supported mixin (annotation-only configuration), exactly as before &mdash; the framework reads
 * such mixins reflectively. Capability mixins that ship without state (e.g. the api-docs pack) stay
 * base-less; the new single-responsibility op-mixins extend this base to make the triad membership
 * explicit.
 *
 * <h5 class='section'>Builder support (deferred):</h5>
 *
 * <p>
 * The fluent programmatic-builder surface ({@code RestBuilder}/{@code RestMixin.Builder}) that would let
 * a mixin be configured programmatically rather than by annotation is <b>not</b> part of this base yet
 * &mdash; it is deferred along with the resource/servlet builder work. Until then, {@code RestMixin}
 * carries no builder or stashed-builder state and mixins are configured purely by their {@code @Rest} /
 * {@code @RestOp} annotations.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RestServlet}
 * 	<li class='jc'>{@link RestResource}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
public abstract class RestMixin {}
