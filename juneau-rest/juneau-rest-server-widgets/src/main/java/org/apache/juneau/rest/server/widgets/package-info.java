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
 * This optional module holds the shared {@link org.apache.juneau.rest.server.widgets.Widget} marker and the
 * bean-only definitions for the reusable calendar widget ({@link org.apache.juneau.rest.server.widgets.CalendarDef},
 * {@link org.apache.juneau.rest.server.widgets.EventCategory},
 * {@link org.apache.juneau.rest.server.widgets.CalendarEvent}).  Table-specific types (row actions, bulk mutate)
 * stay in {@code juneau-rest-server-views}; this module has
 * <b>no</b> dependency on views, so a widget bean can never import a views-module type.  A
 * {@link org.apache.juneau.rest.server.widgets.CalendarDef} carries no HTML-emitter code &mdash; pages mount an
 * empty {@code data-juneau-calendar} marker and {@code juneau-calendar.js} hydrates it.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.Widget}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.BarSlot}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.CalendarDef}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.widgets.ServerValues}
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.widgets;
