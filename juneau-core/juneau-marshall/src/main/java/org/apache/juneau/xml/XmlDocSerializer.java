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
package org.apache.juneau.xml;

/**
 * Serializes POJOs to HTTP responses as XML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/xml</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/xml</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Same as {@link XmlSerializer}, except prepends <code><xt>&lt;?xml</xt> <xa>version</xa>=<xs>'1.0'</xs>
 * <xa>encoding</xa>=<xs>'UTF-8'</xs><xt>?&gt;</xt></code> to the response to make it a valid XML document.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
public class XmlDocSerializer extends XmlSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder().type(XmlDocSerializer.class);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer without namespaces. */
	public static class Ns extends XmlDocSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Ns(XmlSerializer.Builder builder) {
			super(builder.enableNamespaces());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public XmlDocSerializer(XmlSerializer.Builder builder) {
		super(builder);
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Context */
	public XmlDocSerializerSession.Builder createSession() {
		return XmlDocSerializerSession.create(this);
	}

	@Override /* Context */
	public XmlDocSerializerSession getSession() {
		return createSession().build();
	}
}