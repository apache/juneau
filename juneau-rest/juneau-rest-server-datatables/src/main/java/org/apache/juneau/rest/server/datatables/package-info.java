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
 * The DataTables server-side-processing JSON beans implementing the
 * <a class="doclink" href="https://datatables.net/manual/server-side">DataTables server-side processing</a> wire
 * contract.
 *
 * <p>
 * This package holds <b>only</b> the DataTables JSON <b>input</b> ({@link org.apache.juneau.rest.server.datatables.DataTablesRequest})
 * and <b>output</b> ({@link org.apache.juneau.rest.server.datatables.DataTablesResults}) beans, plus the
 * client-helper mixin.  It carries <b>no</b> query-engine types (design §5.3): the beans know nothing about
 * {@link org.apache.juneau.commons.beanquery.BeanQuery}.
 *
 * <p>
 * The bridge between the two worlds lives in the sibling package
 * {@link org.apache.juneau.rest.server.datatables.adapter}: {@code DataTablesQuery} maps a {@code DataTablesRequest}
 * onto a {@code BeanQuery}, runs it against a {@link org.apache.juneau.commons.beanquery.BeanQuerySession}, and
 * populates a {@code DataTablesResults} envelope (mapping {@code total}&rarr;{@code recordsTotal} and
 * {@code matched}&rarr;{@code recordsFiltered}).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.adapter.DataTablesQuery}
 * 	<li class='jc'>{@link org.apache.juneau.commons.beanquery.BeanQuerySession}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/manual/server-side">DataTables Server-Side Processing</a>
 * </ul>
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.datatables;
