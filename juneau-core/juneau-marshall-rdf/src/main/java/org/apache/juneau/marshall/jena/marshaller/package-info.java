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
 * Convenience marshallers pairing RDF serializers and parsers into single classes with fluent to/from methods.
 *
 * <p>
 * Each class combines a matching serializer and parser for a specific RDF syntax &mdash; including
 * {@link org.apache.juneau.marshall.jena.marshaller.RdfXml RDF/XML}, {@link org.apache.juneau.marshall.jena.marshaller.Turtle Turtle},
 * {@link org.apache.juneau.marshall.jena.marshaller.N3 N3}, {@link org.apache.juneau.marshall.jena.marshaller.NTriple N-Triples},
 * {@link org.apache.juneau.marshall.jena.marshaller.NQuads N-Quads}, {@link org.apache.juneau.marshall.jena.marshaller.TriG TriG},
 * {@link org.apache.juneau.marshall.jena.marshaller.TriX TriX}, and {@link org.apache.juneau.marshall.jena.marshaller.JsonLd JSON-LD} &mdash;
 * so RDF content can be read and written through a single object.
 */
package org.apache.juneau.marshall.jena.marshaller;
