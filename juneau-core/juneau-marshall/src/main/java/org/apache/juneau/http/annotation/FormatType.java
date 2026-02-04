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
package org.apache.juneau.http.annotation;

/**
 * Static strings used for Swagger parameter format types.
 *
 */
public class FormatType {

	/**
	 * Prevents instantiation.
	 */
	private FormatType() {}

	@SuppressWarnings("javadoc")
	public static final String INT32 = "int32";
	@SuppressWarnings("javadoc")
	public static final String INT64 = "int64";
	@SuppressWarnings("javadoc")
	public static final String FLOAT = "float";
	@SuppressWarnings("javadoc")
	public static final String DOUBLE = "double";
	@SuppressWarnings("javadoc")
	public static final String BYTE = "byte";
	@SuppressWarnings("javadoc")
	public static final String BINARY = "binary";
	@SuppressWarnings("javadoc")
	public static final String DATE = "date";
	@SuppressWarnings("javadoc")
	public static final String DATE_TIME = "date-time";
	@SuppressWarnings("javadoc")
	public static final String PASSWORD = "password";
	@SuppressWarnings("javadoc")
	public static final String UON = "uon";
}