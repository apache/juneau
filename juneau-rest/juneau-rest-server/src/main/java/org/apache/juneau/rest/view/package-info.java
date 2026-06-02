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
 * Engine-agnostic server-side-rendered view extension point.
 *
 * <p>
 * Hosts the {@link org.apache.juneau.rest.view.View View} interface &mdash; the stable contract
 * that per-engine bridge modules ({@code juneau-rest-server-view-jsp},
 * {@code -thymeleaf}, {@code -mustache}, {@code -freemarker}) implement and pair with a
 * {@link org.apache.juneau.rest.view.ViewRenderer ViewRenderer} that detects their own
 * {@code View} subtype on {@code @RestOp}-method return values and dispatches to the underlying
 * templating engine.
 *
 * <p>
 * Keeping the interface in {@code juneau-rest-server}'s core (rather than in any one engine
 * bridge) lets a {@code @RestOp} method declare a {@code View} return type without forcing a
 * compile-time dependency on a specific engine, and lets host code interoperate with multiple
 * bridge modules in the same application.
 *
 * <h5 class='topic'>Renderer auto-prepend</h5>
 *
 * <p>
 * Any {@link org.apache.juneau.rest.processor.ResponseProcessor ResponseProcessor} implementing
 * {@link org.apache.juneau.rest.view.ViewRenderer} is automatically repositioned to run before the first
 * {@link org.apache.juneau.rest.processor.CatchAllResponseProcessor CatchAllResponseProcessor}
 * (i.e. {@link org.apache.juneau.rest.processor.SerializedPojoProcessor SerializedPojoProcessor})
 * during {@link org.apache.juneau.rest.processor.ResponseProcessorList ResponseProcessorList}
 * construction.  This ensures that a {@code @RestOp} method returning a typed {@code View}
 * subclass ({@link org.apache.juneau.rest.view.jsp.JspView JspView},
 * {@link org.apache.juneau.rest.view.thymeleaf.ThymeleafView ThymeleafView}, etc.) reaches the
 * matching renderer without the user having to enumerate a full custom processor chain.
 *
 * <p>
 * Third-party renderers gain the same ordering guarantee by implementing {@link org.apache.juneau.rest.view.ViewRenderer}.
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.view;
