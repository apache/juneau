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
package org.apache.juneau.marshall.jena;

import java.net.*;

import com.apicatalog.jsonld.*;
import com.apicatalog.jsonld.document.*;
import com.apicatalog.jsonld.loader.*;

/**
 * JSON-LD document loader that refuses to load any remote document or context.
 *
 * <p>
 * Used by the RDF parsers unless remote JSON-LD contexts have been explicitly enabled.
 */
final class RdfNoNetworkDocumentLoader implements DocumentLoader {

	static final RdfNoNetworkDocumentLoader INSTANCE = new RdfNoNetworkDocumentLoader();

	private RdfNoNetworkDocumentLoader() {}

	@Override /* Overridden from DocumentLoader */
	public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
		throw new JsonLdError(JsonLdErrorCode.LOADING_REMOTE_CONTEXT_FAILED, "Remote JSON-LD contexts are not enabled.");
	}
}
