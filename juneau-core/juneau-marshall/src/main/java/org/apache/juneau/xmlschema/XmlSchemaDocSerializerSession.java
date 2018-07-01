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
package org.apache.juneau.xmlschema;

import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlSchemaDocSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class XmlSchemaDocSerializerSession extends XmlSchemaSerializerSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected XmlSchemaDocSerializerSession(XmlSchemaDocSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
	}

	@Override /* SerializerSession */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {
		try (XmlWriter w = getXmlWriter(out)) {
			w.append("<?xml")
				.attr("version", "1.0")
				.attr("encoding", "UTF-8")
				.appendln("?>");
			w.flush();
			super.doSerialize(out, o);
		}
	}
}
