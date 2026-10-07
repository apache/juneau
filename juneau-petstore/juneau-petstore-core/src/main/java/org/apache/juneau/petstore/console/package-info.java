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
 * Petstore admin console: the {@code /console} page tree.
 *
 * <p>
 * Every page is a FreeMarker template rendered inside the {@code base.ftlh} console chrome, mounted beside the
 * unchanged {@code /petstore} API and sharing its {@link org.apache.juneau.petstore.service.PetStore}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(children={PetStoreResource.<jk>class</jk>, PetstoreConsoleResource.<jk>class</jk>})
 * 	<jk>public class</jk> RootResources <jk>extends</jk> BasicRestServletGroup {}
 * </p>
 */
package org.apache.juneau.petstore.console;
