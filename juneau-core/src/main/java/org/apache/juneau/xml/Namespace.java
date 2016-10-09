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

import org.apache.juneau.annotation.*;

/**
 * Represents a simple namespace mapping between a simple name and URI.
 * <p>
 * 	In general, the simple name will be used as the XML prefix mapping unless
 * 	there are conflicts or prefix remappings in the serializer.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Bean(sort=true)
public final class Namespace implements Comparable<Namespace> {
	final String name, uri;
	private final int hashCode;

	/**
	 * Constructor.
	 * <p>
	 * 	Use this constructor when the long name and short name are the same value.
	 *
	 * @param name The long and short name of this schema.
	 * @param uri The URI of this schema.
	 */
	@BeanConstructor(properties="name,uri")
	public Namespace(String name, String uri) {
		this.name = name;
		this.uri = uri;
		this.hashCode = (name == null ? 0 : name.hashCode()) + uri.hashCode();
	}

	/**
	 * Returns the namespace name.
	 *
	 * @return The namespace name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the namespace URI.
	 *
	 * @return The namespace URI.
	 */
	public String getUri() {
		return uri;
	}

	@Override /* Comparable */
	public int compareTo(Namespace o) {
		int i = name.compareTo(o.name);
		if (i == 0)
			i = uri.compareTo(o.uri);
		return i;
	}

	/**
	 * For performance reasons, equality is always based on identity, since
	 * the {@link NamespaceFactory} class ensures no duplicate name+uri pairs.
	 */
	@Override /* Object */
	public boolean equals(Object o) {
		return this == o;
	}

	@Override /* Object */
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public String toString() {
		return "{name:'"+name+"',uri:'"+uri+"'}";
	}
}
