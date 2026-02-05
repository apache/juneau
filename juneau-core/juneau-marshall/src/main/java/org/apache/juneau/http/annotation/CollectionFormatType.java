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
 * Static strings used for Swagger parameter collection format types.
 *
 */
public class CollectionFormatType {

	/**
	 * Prevents instantiation.
	 */
	private CollectionFormatType() {}

	/** CSV collection format. */
	public static final String CSV = "csv";
	/** SSV collection format. */
	public static final String SSV = "ssv";
	/** TSV collection format. */
	public static final String TSV = "tsv";
	/** PIPES collection format. */
	public static final String PIPES = "pipes";
	/** MULTI collection format. */
	public static final String MULTI = "multi";
	/** UON collection format. */
	public static final String UON = "uon";
}