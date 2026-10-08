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
package org.apache.juneau.rest.server.auth.saml;

import javax.xml.parsers.*;

import org.apache.commons.xml.secure.SecureDocumentBuilderFactory;
import org.xml.sax.*;

/**
 * Shared JAXP setup for SAML response and metadata parsing.
 */
final class SamlXml {

	private SamlXml() {}

	/**
	 * Returns a namespace-aware document builder factory that rejects DTDs and entity expansion at parse.
	 *
	 * <p>
	 * {@link SecureDocumentBuilderFactory} enables secure processing and ignores external entity
	 * resolution.  That floor does not reject an internal DTD or a billion-laughs entity graph, so
	 * SAML documents still apply the stricter fail-closed features here.
	 * </p>
	 *
	 * @return A factory ready for {@link DocumentBuilderFactory#newDocumentBuilder()}.
	 * @throws ParserConfigurationException If the JDK parser cannot apply the required features.
	 */
	static DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
		var dbf = SecureDocumentBuilderFactory.newNSInstance();
		dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
		dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		dbf.setXIncludeAware(false);
		dbf.setExpandEntityReferences(false);
		return dbf;
	}

	/**
	 * Creates a document builder from {@link #documentBuilderFactory()} whose parse errors are rethrown.
	 *
	 * <p>
	 * The JDK default error handler prints {@code [Fatal Error]} to stderr before throwing.  Rethrowing
	 * from the handler yields the same {@link SAXParseException} without the stderr output.
	 *
	 * @return A new document builder.
	 * @throws ParserConfigurationException If the JDK parser cannot apply the required features.
	 */
	static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
		var db = documentBuilderFactory().newDocumentBuilder();
		db.setErrorHandler(new ErrorHandler() {
			@Override /* ErrorHandler */
			public void warning(SAXParseException e) {
				// Non-fatal; ignored as with the JDK default handler (which only prints).
			}

			@Override /* ErrorHandler */
			public void error(SAXParseException e) throws SAXException {
				throw e;
			}

			@Override /* ErrorHandler */
			public void fatalError(SAXParseException e) throws SAXException {
				throw e;
			}
		});
		return db;
	}
}
