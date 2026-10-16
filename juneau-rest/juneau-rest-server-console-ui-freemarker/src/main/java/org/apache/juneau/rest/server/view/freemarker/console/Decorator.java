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

/**
 * A chart decorator entry: one element of a chart card's {@code decorators[]}, run in the browser by the decorator
 * registered under its {@code use} name ({@code JuneauCharts.decorator}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ChartDef.<jsm>line</jsm>(<js>"t"</js>).src(<js>"/s"</js>).x(<js>"d"</js>).series(<js>"v"</js>, <js>"V"</js>)
 * 		.decorate(Markers.<jsm>now</jsm>(<js>"Today"</js>)).decorate(Thresholds.<jsm>at</jsm>(500).label(<js>"SLO"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public interface Decorator {

	/**
	 * @return A new map {@code {"use": name, ...args}}; a string {@code topic} key is published on click, and
	 * 	{@link ChartDef#decorate(Decorator)} adds it to the card's {@code publishes}.
	 */
	JsonMap toJson();
}
