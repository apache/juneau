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
 * A typed, declarative "rich DataTables view" toolkit that serializes to the frozen {@code VIEW_META} JSON wire
 * contract consumed by the first-party client runtime.
 *
 * <p>
 * This optional module builds on {@code juneau-rest-server-datatables} (the TODO-355 server-side query protocol):
 * an app author declares one {@link org.apache.juneau.rest.server.views.ViewDef} &mdash; columns, ribbon actions,
 * row-decorator rules, and named cell renderers &mdash; as ordinary Juneau beans, and the model serializes to the
 * {@code VIEW_META} JSON contract that the shipped {@code juneau-views.js}/{@code juneau-ribbon.js}/
 * {@code juneau-renders.js} runtime consumes to wire up a fully-featured DataTable.
 *
 * <p>
 * The module owns a dependency-free base {@code .tag} chip stylesheet and takes <b>no</b> hard dependency on
 * {@code juneau-rest-server-console-ui}: the base chip keeps client-rendered views legible standalone, while
 * console-ui's palette themes the shared {@code .tag.<domain>.<value>} class-name contract when present.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.DataTablesQueryProtocol}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.DataTablesColumns}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/manual/server-side">DataTables Server-Side Processing</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.views;
