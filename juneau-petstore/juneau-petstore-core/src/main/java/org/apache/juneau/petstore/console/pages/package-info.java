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
 * Builder twins of the petstore console pages: each page's {@code .ftlh} file rebuilt with {@code PageSpec}, served
 * when the page is requested with {@code ?builder}.
 *
 * <p>
 * Each method mirrors its template card for card; {@code PetstoreTwins_Test} checks the two render the same page
 * contract.  The base chrome ({@code base.ftlh}: title, nav, badge, facts, scripts, footer) is not rebuilt: a spec with
 * no page template renders inside the console mixin's chrome template, the same as an FTL page.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/"</js>)
 * 	<jk>public</jk> View page(RestRequest <jv>req</jv>) {
 * 		<jk>return</jk> <jsm>builder</jsm>(<jv>req</jv>) ? <jsm>twin</jsm>(<jv>req</jv>, OpsPageBuilders.<jsm>audit</jsm>()) : FreemarkerView.<jsm>of</jsm>(<js>"audit.ftlh"</js>);
 * 	}
 * </p>
 */
package org.apache.juneau.petstore.console.pages;
