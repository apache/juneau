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

// region client
import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.client.*;

/**
 * Calls the books API through {@link BookApi} and prints what comes back.
 */
public class BookClient {

	/**
	 * Runs against the API root given as the first argument, or {@code http://localhost:8080}.
	 *
	 * @param args The API root URL, optional.
	 * @throws IOException If the API cannot be reached.
	 */
	public static void main(String[] args) throws IOException {
		run(args.length > 0 ? args[0] : "http://localhost:8080", System.out);
	}

	/**
	 * Makes the calls and prints the results.
	 *
	 * @param rootUrl The API root URL.
	 * @param out Where to print.
	 * @throws IOException If the API cannot be reached.
	 */
	public static void run(String rootUrl, PrintStream out) throws IOException {
		try (var client = RestClient.builder()
				.rootUrl(rootUrl)
				.defaultSerializer(JsonSerializer.DEFAULT)
				.defaultParser(JsonParser.DEFAULT)
				.allowPrivateUrls(true)  // Only because the stand-in API runs on localhost.
				.build()) {

			BookApi books = client.remote(BookApi.class);

			for (var b : books.listBooks())
				out.println(b.id + ": " + b.title + " by " + b.author);

			var added = books.addBook(new Book("Kindred", "Octavia E. Butler"));
			out.println("Added " + added.title + " as id " + added.id);

			out.println("Book 1 is " + books.getBook(1).title);

			try {
				books.getBook(99);
			} catch (NotFound e) {
				out.println("Book 99: 404 Not Found");
			}
		}
	}
}
// endregion client
