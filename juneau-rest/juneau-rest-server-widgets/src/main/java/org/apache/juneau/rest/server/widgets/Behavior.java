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
package org.apache.juneau.rest.server.widgets;

/**
 * How a {@link HeaderAction} behaves when the user activates it.
 *
 * <ul>
 * 	<li class='jc'>{@link #LINK} &mdash; navigate to a same-origin {@code href}.
 * 	<li class='jc'>{@link #SAFE} &mdash; dispatch a format-validated client event token; the toolkit interprets none
 * 		(the host app listens and acts).
 * 	<li class='jc'>{@link #MENU} &mdash; open an attached single-level menu.  The menu manager has not shipped
 * 		yet; until it lands, the trigger is disabled and the list is omitted (no fake disclosure).
 * </ul>
 *
 * @since 10.0.0
 */
public enum Behavior {

	/** Navigate to a same-origin {@code href}. */
	LINK,

	/** Dispatch a format-validated client-safe event token (host-interpreted). */
	SAFE,

	/** Open an attached single-level menu (menu manager has not shipped yet). */
	MENU
}
