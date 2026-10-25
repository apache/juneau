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

// region resource
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.docs.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * A small task API that publishes its own Swagger 2.0 and OpenAPI 3.1 documents.
 */
@Rest(
	title="Task API",
	description="Create and list to-do items.",
	mixins={SwaggerUiMixin.class, RedocMixin.class}
)
public class TaskResource extends RestServlet implements BasicUniversalConfig {

	private static final long serialVersionUID = 1L;

	private final Map<Integer,Task> tasks = new ConcurrentSkipListMap<>();
	private final AtomicInteger nextId = new AtomicInteger(1);

	@RestGet(path="/tasks", summary="List tasks", description="Returns every task, oldest first.")
	public Collection<Task> list() {
		return tasks.values();
	}

	@RestGet(path="/tasks/{id}", summary="Get a task")
	public Task get(@Path("id") @Schema(description="Task identifier.") int id) {
		var t = tasks.get(id);
		if (t == null)
			throw new NotFound("No task with id {0}", id);
		return t;
	}

	@RestPost(path="/tasks", summary="Create a task")
	public Task create(@Content Task task) {
		task.id = nextId.getAndIncrement();
		tasks.put(task.id, task);
		return task;
	}
}
// endregion resource
