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
 * ATOM Data Transfer Objects
 *
 * <p>
 * This package contains predefined DTOs for working with ATOM feeds and entries.
 * These classes provide a convenient way to serialize and parse ATOM XML content
 * using Juneau's marshalling framework.
 * </p>
 */
// @formatter:off
@XmlSchema(
	prefix="atom",
	xmlNs={
		@XmlNs(prefix="atom", namespaceURI="http://www.w3.org/2005/Atom/"),
		@XmlNs(prefix="xml", namespaceURI="http://www.w3.org/XML/1998/namespace")
	}
)
// @formatter:on
package org.apache.juneau.bean.atom;

import org.apache.juneau.xml.annotation.*;
