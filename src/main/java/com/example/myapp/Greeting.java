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
package com.example.myapp;

/**
 * Simple POJO returned by {@link HelloWorldResource}.
 *
 * <p>Because the resource returns this bean (not a hand-serialized String), the same endpoint
 * serves JSON, XML, and HTML via Juneau content negotiation.
 */
public class Greeting {

	/** Who is being greeted. */
	public String name;

	/** The greeting message. */
	public String message;

	/** Default constructor (required by the Juneau parser). */
	public Greeting() {}

	/**
	 * Constructor.
	 *
	 * @param name The name.
	 * @param message The message.
	 */
	public Greeting(String name, String message) {
		this.name = name;
		this.message = message;
	}
}
