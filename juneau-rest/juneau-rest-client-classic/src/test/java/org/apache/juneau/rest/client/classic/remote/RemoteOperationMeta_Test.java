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
package org.apache.juneau.rest.client.classic.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

/**
 * Exercises the method-level {@code @FormData}/{@code @Header}/{@code @Path}/{@code @Query} default-processing
 * branches of {@link RemoteOperationMeta.Builder}. Each {@code filter(x -> isAnyNotEmpty(x.name(), x.value())
 * && <i>defCheck</i>(x.def()))} predicate has two short-circuiting conditions, so covering it fully needs three
 * combinations: neither {@code name} nor {@code value} set (first condition false, short-circuits), one of them
 * set with {@code def()} left at its default (first condition true, second false), and both set (exercised by
 * the existing remote-proxy tests elsewhere). This class covers the first two, which no other test reaches.
 */
class RemoteOperationMeta_Test {

	interface Proxy {
		@FormData(name = "foo") void formDataMethod();
		@Header(name = "foo") void headerMethod();
		@Path(name = "foo") void pathMethod();
		@Query(name = "foo") void queryMethod();

		@FormData void bareFormDataMethod();
		@Header void bareHeaderMethod();
		@Path void barePathMethod();
		@Query void bareQueryMethod();
	}

	@Test void a01_formData_nameOnly_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("formDataMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getFormDataDefault("foo"));
	}

	@Test void a02_header_nameOnly_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("headerMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getHeaderDefault("foo"));
	}

	@Test void a03_path_nameOnly_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("pathMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getPathDefault("foo"));
	}

	@Test void a04_query_nameOnly_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("queryMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getQueryDefault("foo"));
	}

	@Test void b01_bareFormData_neitherNameNorValue_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("bareFormDataMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getFormDataDefault(""));
	}

	@Test void b02_bareHeader_neitherNameNorValue_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("bareHeaderMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getHeaderDefault(""));
	}

	@Test void b03_barePath_neitherNameNorValue_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("barePathMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getPathDefault(""));
	}

	@Test void b04_bareQuery_neitherNameNorValue_noDefaultRecorded() throws Exception {
		var m = Proxy.class.getMethod("bareQueryMethod");
		var meta = new RemoteOperationMeta("", m, "GET");
		assertNull(meta.getQueryDefault(""));
	}
}
