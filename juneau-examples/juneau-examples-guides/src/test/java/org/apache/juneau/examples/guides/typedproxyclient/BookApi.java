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

// region api
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;

/**
 * The books API, described as a Java interface.  {@code RestClient.remote(BookApi.class)} implements it.
 */
@Remote(path = "/books")
public interface BookApi {

	/**
	 * Calls {@code GET /books}.
	 *
	 * @return Every book.
	 */
	@RemoteGet
	List<Book> listBooks();

	/**
	 * Calls {@code GET /books/{id}}.
	 *
	 * @param id The book id.
	 * @return The book.
	 * @throws NotFound If the API answers 404.
	 */
	@RemoteGet("/{id}")
	Book getBook(@Path("id") int id) throws NotFound;

	/**
	 * Calls {@code POST /books} with the book as a JSON body.
	 *
	 * @param book The book to add.
	 * @return The stored book, with its id.
	 */
	@RemotePost
	Book addBook(@Content Book book);
}
// endregion api
