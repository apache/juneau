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
 * Test
 */
@XmlSchema(
	prefix="p1",
	xmlNs={
		@XmlNs(prefix="p1",namespaceURI="http://p1"),
		@XmlNs(prefix="p2",namespaceURI="http://p2"),
		@XmlNs(prefix="p3",namespaceURI="http://p3(unused)"),
		@XmlNs(prefix="c1",namespaceURI="http://c1"),
		@XmlNs(prefix="f1",namespaceURI="http://f1")
	}
)
package org.apache.juneau.xml.xml1c;
import org.apache.juneau.xml.annotation.*;

