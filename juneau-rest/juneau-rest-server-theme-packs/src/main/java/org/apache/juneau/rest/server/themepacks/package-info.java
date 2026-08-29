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

/**
 * Built-in console-ui theme-pack constants, plus any views-side asset resolution they need.
 *
 * <p>
 * The {@code ThemePack} <b>types</b> ({@code ThemePack}, {@code ThemePack.Builder}, {@code Density},
 * {@code FontRef}, {@code ThemePackSettings}) live in {@code juneau-rest-server-console-ui}, beside
 * {@code Theme} &mdash; a type in this leaf could not be {@code console-ui}'s own parameter type, since
 * {@code console-ui} does not depend on this module. This module holds only the built-in pack
 * <b>constants</b> (values of those types), because a constant may depend on both halves of the split
 * that a shared type cannot.
 *
 * <p>
 * This module is currently empty: it exists so that the moment the pack types land, the built-in
 * constants have a home and are not blocked on a packaging decision.
 *
 * @since 10.0.0
 */
package org.apache.juneau.rest.server.themepacks;
