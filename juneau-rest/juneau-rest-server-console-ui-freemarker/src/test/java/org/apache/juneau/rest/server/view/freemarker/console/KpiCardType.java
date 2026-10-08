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
package org.apache.juneau.rest.server.view.freemarker.console;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/** Test-only open card type, registered through {@code ConsoleFreemarkerMixin.Builder.cardType}. */
public final class KpiCardType implements CardTypeHandler {
	@Override public String type() { return "kpi"; }
	@Override public JsonMap toFragment(CardSource source) {
		var body = source.json();
		if (! body.containsKey("value"))
			throw source.error("type='kpi' requires 'value'.");
		return body;
	}
}
