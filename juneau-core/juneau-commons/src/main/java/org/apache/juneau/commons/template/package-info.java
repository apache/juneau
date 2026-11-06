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
 * Safe, structured templates for template authors who are less trusted than the application.
 *
 * <p>
 * A {@link org.apache.juneau.commons.template.TemplateResolver} wraps a {@link org.apache.juneau.commons.svl.VarResolver}
 * with a locked-down configuration: no system vars, a default-deny function allowlist, and a render scope supplied per
 * render as a map.  Templates use the SVL grammar: <c>${name}</c> reads the scope, and <c>#{...}</c> expressions can read
 * scope names as bare identifiers, project over collections (<c>rows.![id]</c>) and build lists and tables
 * (<c>ul(...)</c>, <c>table(...)</c>).
 *
 * <p>
 * {@link org.apache.juneau.commons.template.Destinations} provides encoder and renderer presets (HTML, Markdown, plain
 * text, JSON string, URL component), and {@link org.apache.juneau.commons.template.TemplateResolver#check(String, org.apache.juneau.commons.template.TemplateCatalog)}
 * validates a template against the names an application offers before it is stored.
 *
 * <h2 class='section'>Encoding rule:</h2>
 * <p>
 * Escaping is applied to resolved values only, never to template text.  The value encoder runs on the output of each
 * top-level <c>${...}</c> or <c>#{...}</c> segment, after any function inside it has run, so author literals inside an
 * expression are encoded together with the data.  A list or table result bypasses the encoder: the
 * {@link org.apache.juneau.commons.template.StructureRenderer} encodes every item and cell itself and emits its own
 * markup.  Put markup outside <c>#{...}</c>, or use the structure functions.
 *
 * <p>
 * Values are never re-parsed: a scope value that contains <c>${x}</c> or <c>#{1+1}</c> is rendered as that text.
 */
package org.apache.juneau.commons.template;
