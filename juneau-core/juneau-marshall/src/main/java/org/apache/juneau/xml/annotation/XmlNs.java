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
package org.apache.juneau.xml.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Namespace name/URL mapping pair.
 *
 * <p>
 * Used to identify a namespace/URI pair on a {@link XmlSchema#xmlNs() @XmlSchema(xmlNs)} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.XmlDetails">XML Details</a>
 * </ul>
 */
@Documented
@Target({})
@Retention(RUNTIME)
@Inherited
public @interface XmlNs {

	/**
	 * XML namespace URL.
	 *
	 * @return The annotation value.
	 */
	String namespaceURI();

	/**
	 * XML namespace prefix.
	 *
	 * @return The annotation value.
	 */
	String prefix();
}
