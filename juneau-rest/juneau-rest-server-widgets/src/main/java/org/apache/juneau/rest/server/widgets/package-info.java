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
 * Reusable widget primitives for the Juneau REST server toolkit.
 *
 * <p>
 * This optional module holds the shared {@link org.apache.juneau.rest.server.widgets.Widget} marker, the
 * {@link org.apache.juneau.rest.server.widgets.ActionBar} primitive, the card-layout beans
 * ({@link org.apache.juneau.rest.server.widgets.CardGrid} / {@link org.apache.juneau.rest.server.widgets.Card} /
 * {@link org.apache.juneau.rest.server.widgets.CardBody} / {@link org.apache.juneau.rest.server.widgets.CardFieldList}
 * / {@link org.apache.juneau.rest.server.widgets.CardField}), and the bean-only definitions for the reusable
 * calendar widget ({@link org.apache.juneau.rest.server.widgets.CalendarDef},
 * {@link org.apache.juneau.rest.server.widgets.EventCategory},
 * {@link org.apache.juneau.rest.server.widgets.CalendarEvent}).  Table-specific types (row-detail defs, row actions)
 * and the concrete html5 emitters + client runtimes stay in {@code juneau-rest-server-views}; this module has
 * <b>no</b> dependency on views, so a widget bean can never import a views-module type.  A
 * {@link org.apache.juneau.rest.server.widgets.CalendarDef} carries no HTML-emitter code &mdash; the calendar's
 * emitter ({@code CalendarTable}) lives in views and composes these beans.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.Widget}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.ActionBar}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.CardGrid}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.CalendarDef}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.ServerValues}
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.widgets;
