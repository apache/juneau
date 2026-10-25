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
package org.apache.juneau.examples.guides.openapidocs;

// region demo
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;

/**
 * Starts {@link TaskResource} in-process, fetches /openapi.json and prints the task operations it documents.
 */
public class TaskDocsDemo {

	public static void main(String[] args) throws Exception {
		System.out.println(run());
	}

	public static String run() throws Exception {
		try (var client = MockRestClient.buildJson(TaskResource.class)) {
			var json = client.get("/openapi.json").run().assertStatus(200).getContent().asString();
			var doc = JsonMap.ofString(json);

			var lines = new ArrayList<String>();
			lines.add("openapi: " + doc.getString("openapi"));
			lines.add("title:   " + doc.getMap("info").getString("title"));
			var paths = doc.getMap("paths");
			for (var path : new TreeSet<>(paths.keySet())) {
				if (! path.startsWith("/tasks"))
					continue;  // Skip the documentation endpoints, which document themselves too.
				var ops = paths.getMap(path);
				for (var method : new TreeSet<>(ops.keySet()))
					lines.add(method.toUpperCase() + " " + path + " - " + ops.getMap(method).getString("summary"));
			}
			return String.join("\n", lines);
		}
	}
}
// endregion demo
