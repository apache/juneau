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
package org.apache.juneau.commons.http;

import static org.apache.juneau.commons.utils.Utils.*;

/**
 * A simple immutable implementation of {@link NameValuePair}.
 */
public class BasicNameValuePair implements NameValuePair {

	private final String name;
	private final String value;

	/**
	 * Constructor.
	 *
	 * @param name The name.
	 * @param value The value, may be <jk>null</jk>.
	 */
	public BasicNameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof BasicNameValuePair o2) && eq(name, o2.name) && eq(value, o2.value);
	}

	@Override
	public int hashCode() {
		return h(name, value);
	}

	@Override
	public String toString() {
		return name + "=" + value;
	}
}
