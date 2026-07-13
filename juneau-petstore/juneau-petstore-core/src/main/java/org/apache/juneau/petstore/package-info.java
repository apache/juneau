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
 * Apache Juneau Petstore - shared domain model and REST resources.
 *
 * <p>
 * The {@code juneau-petstore-core} module owns the petstore's deployment-agnostic surface:
 * domain DTOs ({@code dto}), the in-memory store ({@code service}), and the REST resources
 * ({@code rest}).  It is consumed verbatim by both {@code juneau-petstore-jetty} and
 * {@code juneau-petstore-springboot} so the two deployments demonstrate an identical REST
 * surface by construction.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
package org.apache.juneau.petstore;
