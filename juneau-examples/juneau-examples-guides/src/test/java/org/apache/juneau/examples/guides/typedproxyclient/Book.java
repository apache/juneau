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
package org.apache.juneau.examples.guides.typedproxyclient;

// region book
/**
 * A book, as the books API sends it: {@code {"id":1,"title":"Dune","author":"Frank Herbert"}}.
 */
public class Book {

	/** The id the API assigned. */
	public int id;

	/** The title. */
	public String title;

	/** The author. */
	public String author;

	/** Used by the parser. */
	public Book() {}

	/**
	 * Creates a book that has no id yet.
	 *
	 * @param title The title.
	 * @param author The author.
	 */
	public Book(String title, String author) {
		this.title = title;
		this.author = author;
	}
}
// endregion book
