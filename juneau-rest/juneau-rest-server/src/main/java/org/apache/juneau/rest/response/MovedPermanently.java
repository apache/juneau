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
package org.apache.juneau.rest.response;

import java.net.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents an <code>HTTP 301 Moved Permanently</code> response.
 *
 * <p>
 * This and all future requests should be directed to the given URI.
 */
@Response(code=301, example="'Moved Permanently'")
public class MovedPermanently {

	/** Reusable instance. */
	public static final MovedPermanently INSTANCE = new MovedPermanently();

	private final URI location;

	/**
	 * Constructor.
	 */
	public MovedPermanently() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param location <code>Location</code> header value.
	 */
	public MovedPermanently(URI location) {
		this.location = location;
	}

	@Override /* Object */
	public String toString() {
		return "Moved Permanently";
	}

	/**
	 * @return <code>Location</code> header value.
	 */
	@Header(name="Location", description="")
	public URI getLocation() {
		return location;
	}
}