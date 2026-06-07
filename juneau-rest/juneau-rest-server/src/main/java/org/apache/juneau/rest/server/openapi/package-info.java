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
 * Server-side OpenAPI 3.1 emission — the {@link org.apache.juneau.rest.server.openapi.OpenApiProvider} SPI
 * and the {@link org.apache.juneau.rest.server.openapi.BasicOpenApiProvider} default implementation that
 * walks an {@link org.apache.juneau.rest.server.RestContext} and produces an
 * {@link org.apache.juneau.bean.openapi3.OpenApi} document.
 *
 * <p>
 * Sibling of the {@code org.apache.juneau.rest.server.swagger} package which emits Swagger 2.0; both
 * providers coexist and a resource may select either or both. The default OpenAPI version emitted
 * is {@code "3.1.0"}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerOpenApi">OpenAPI 3.1 Server Emission</a>
 * </ul>
 */
package org.apache.juneau.rest.server.openapi;
