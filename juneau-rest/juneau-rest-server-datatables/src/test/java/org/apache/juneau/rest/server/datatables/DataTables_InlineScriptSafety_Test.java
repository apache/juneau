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
package org.apache.juneau.rest.server.datatables;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.test.assertions.*;
import org.junit.jupiter.api.*;

/**
 * Guards every served {@code org/apache/juneau/rest/server/datatables/*.js} resource against a literal closing script tag, which would end an
 * inlining HTML {@code <script>} block early and silently truncate the file.  See
 * {@code ViewsJs_InlineScriptSafety_Test} in {@code juneau-rest-server-views}.
 */
class DataTables_InlineScriptSafety_Test extends TestBase {

	@Test void b01_scriptsWereFound() throws Exception {
		assertFalse(InlineScriptSafety.listResources(DataTablesMixin.class, DataTablesMixin.GLUE_RESOURCE, ".js").isEmpty(), "no served scripts found");
	}

	@Test void b02_noServedScriptContainsAClosingScriptTag() throws Exception {
		InlineScriptSafety.assertNoScriptCloseTag(DataTablesMixin.class, DataTablesMixin.GLUE_RESOURCE, ".js");
	}
}
