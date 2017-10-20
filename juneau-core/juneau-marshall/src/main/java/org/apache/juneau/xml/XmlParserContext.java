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

import static org.apache.juneau.xml.XmlParser.*;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Contains a snapshot-in-time read-only copy of the settings on the {@link XmlParser} class.
 */
public class XmlParserContext extends ParserContext {

	final boolean
		validating,
		preserveRootElement;
	final XMLReporter reporter;
	final XMLResolver resolver;
	final XMLEventAllocator eventAllocator;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public XmlParserContext(PropertyStore ps) {
		super(ps);
		validating = ps.getProperty(XML_validating, boolean.class, false);
		preserveRootElement = ps.getProperty(XML_preserveRootElement, boolean.class, false);
		reporter = ps.getProperty(XML_reporter, XMLReporter.class, null);
		resolver = ps.getProperty(XML_resolver, XMLResolver.class, null);
		eventAllocator = ps.getProperty(XML_eventAllocator, XMLEventAllocator.class, null);
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("XmlParserContext", new ObjectMap()
				.append("validating", validating)
				.append("preserveRootElement", preserveRootElement)
				.append("reporter", reporter)
				.append("resolver", resolver)
				.append("eventAllocator", eventAllocator)
			);
	}
}
