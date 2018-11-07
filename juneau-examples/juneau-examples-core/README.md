<!--
 ***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
 * with the License.  You may obtain a copy of the License at                                                              *
 *                                                                                                                         *
 *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
 *                                                                                                                         *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
 * specific language governing permissions and limitations under the License.                                              *
 ***************************************************************************************************************************
-->

# Juneau Core Examples

Juneau Core Examples provide some insight on how to leverage Juneau within your applications.

Juneau Core is focused on serialization and deserialization, these examples are focused on how to use
Juneau to serialize and deserialize your POJOs.

To use any of the examples, you need to add Apache Juneau as a dependency

```xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>juneau-core</artifactId>
    <version>${juneau.version}</version>
</dependency>
```

## JSON Examples

Juneau provides out of the box JSON support, reading and writing JSON structures into Plain Old Java Objects (POJOs)

- `JsonSimpleExample` - How to use the JsonSerializer and JsonParser to convert POJOs to String and then strings back to POJOs
- `JsonConfigurationExample` - How to create JsonParser and Seralizer with different properties set.

## XML Examples

Juneau provides out of the box XML support, reading and writing XML structures into Plain Old Java Objects (POJOs)

- `XmlComplexExample` - How to use the XmlSerializer and XmlParser to convert complex POJOs to String and then strings back to complex POJOs

## RDF Examples

Juneau provides RDF support assuming Apache Jena is on the classpath.  First, you need to add Jena to the classpath.

```xml
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-core</artifactId>
    <version>${jena.version}</version> // Juneau is tested against 2.7.1
</dependency>
```

- `RDFExample` - An example on how to serialize a POJO into RDF format
