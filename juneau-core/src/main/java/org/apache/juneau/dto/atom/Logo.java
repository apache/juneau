// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              * 
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.dto.atom;

import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.URI;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomLogo</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomLogo = element atom:logo {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(typeName="logo")
public class Logo extends Common {

	private URI uri;


	/**
	 * Normal constructor.
	 *
	 * @param uri The URI of the logo.
	 */
	public Logo(URI uri) {
		this.uri = uri;
	}

	/** Bean constructor. */
	public Logo() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the URI of the logo.
	 *
	 * @return The URI of the logo.
	 */
	@Xml(format=CONTENT)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the logo.
	 *
	 * @param uri The URI of the logo.
	 * @return This object (for method chaining).
	 */
	public Logo setUri(URI uri) {
		this.uri = uri;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Logo setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Logo setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
