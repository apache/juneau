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
package org.apache.juneau.petstore.console.dev;

import java.util.*;

import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Charts: the chart-core demo page, built with {@link DevPageBuilders#charts()} only (it has no template twin).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	GET /console/dev/charts
 * 	GET /console/dev/charts/daily
 * </p>
 */
@Rest(path="/charts", title="Charts")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class ChartsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	private static final int[] PLACED = {3, 5, 4, 7, 6, 2, 1};
	private static final int[] DELIVERED = {1, 2, 4, 5, 6, 2, 1};

	/**
	 * Renders the Charts page.
	 *
	 * @param req The request.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return twin(req, DevPageBuilders.charts());
	}

	/**
	 * The week's order counts the chart and the table draw.
	 *
	 * @return One row per day.
	 */
	@RestGet(path="/daily")
	public List<Map<String,Object>> daily() {
		var l = new ArrayList<Map<String,Object>>();
		for (var i = 0; i < DAYS.length; i++)
			l.add(Map.of("day", DAYS[i], "placed", PLACED[i], "delivered", DELIVERED[i]));
		return l;
	}
}
