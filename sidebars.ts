/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
	// Main Juneau sidebar
	mainSidebar: [
		// Documentation section (topics from /docs folder)
		{
			type: 'category',
			label: 'Documentation',
			collapsed: false,
			items: [
				{
					type: 'category',
					label: '1. Juneau Ecosystem',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'topics/01.01.JuneauEcosystemOverview',
							label: '1.1. Juneau Ecosystem Overview',
						},
						{
							type: 'doc',
							id: 'topics/01.02.Marshalling',
							label: '1.2. Marshalling',
						},
						{
							type: 'doc',
							id: 'topics/01.03.EndToEndRest',
							label: '1.3. End-to-End REST',
						},
						{
							type: 'doc',
							id: 'topics/01.04.RestServer',
							label: '1.4. REST Server',
						},
						{
							type: 'doc',
							id: 'topics/01.05.RestClient',
							label: '1.5. REST Client',
						},
						{
							type: 'doc',
							id: 'topics/01.06.DtoBeans',
							label: '1.6. DTO Beans',
						},
						{
							type: 'doc',
							id: 'topics/01.07.ConfigFiles',
							label: '1.7. Config Files',
						},
						{
							type: 'doc',
							id: 'topics/01.08.FluentAssertions',
							label: '1.8. Fluent Assertions',
						},
						{
							type: 'doc',
							id: 'topics/01.09.GeneralDesign',
							label: '1.9. General Design',
						},
						{
							type: 'doc',
							id: 'topics/01.10.FrameworkComparisons',
							label: '1.10. Framework Comparisons',
						},
						{
							type: 'doc',
							id: 'topics/01.11.WhyJuneau',
							label: '1.11. Why Choose Juneau?',
						},
					],
				},
				{
					type: 'category',
					label: '2. juneau-marshall',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/02.01.JuneauMarshallBasics',
							label: '2.1. juneau-marshall Basics',
						},
						{
							type: 'doc',
							id: 'topics/02.02.Marshallers',
							label: '2.2. Marshallers',
						},
						{
							type: 'doc',
							id: 'topics/02.03.SerializersAndParsers',
							label: '2.3. Serializers and Parsers',
						},
						{
							type: 'category',
							label: '2.4. Bean Contexts',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.04.01.BeanContextBasics',
									label: '2.4.1. Marshalling Context Basics',
								},
							{
								type: 'doc',
								id: 'topics/02.04.02.JavaBeansSupport',
								label: '2.4.2. Java Beans Support',
							},
							{
								type: 'doc',
								id: 'topics/02.04.03.JavaRecordsSupport',
								label: '2.4.3. Java Records Support',
							},
							{
								type: 'doc',
								id: 'topics/02.04.04.BeanTypeAnnotation',
								label: '2.4.4. @BeanType Annotation',
							},
								{
									type: 'doc',
									id: 'topics/02.04.05.BeanPropAnnotation',
									label: '2.4.5. @BeanProp Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.04.06.BeanCtorAnnotation',
									label: '2.4.6. @BeanCtor Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.04.07.BeanIgnoreAnnotation',
									label: '2.4.7. @MarshalledIgnore Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.04.08.NamePropertyAnnotation',
									label: '2.4.8. @NameProperty Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.04.09.ParentPropertyAnnotation',
									label: '2.4.9. @ParentProperty Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.04.10.PojoBuilders',
									label: '2.4.10. POJO Builders',
								},
							{
								type: 'doc',
								id: 'topics/02.04.11.BypassSerialization',
								label: '2.4.11. Bypass Serialization using `Readers` and `InputStreams`',
							},
							{
								type: 'doc',
								id: 'topics/02.04.12.ViewProjection',
								label: '2.4.12. View-Based Projection',
							},
						],
					},
						{
							type: 'doc',
							id: 'topics/02.05.HttpPartSerializersParsers',
							label: '2.5. HTTP Part Serializers and Parsers',
						},
						{
							type: 'doc',
							id: 'topics/02.06.ContextSettings',
							label: '2.6. Context Settings',
						},
						{
							type: 'doc',
							id: 'topics/02.06.01.NullAndInclusionPolicies',
							label: '2.6.1. Null & Inclusion Policies',
						},
						{
							type: 'doc',
							id: 'topics/02.07.ContextAnnotations',
							label: '2.7. Context Annotations',
						},
						{
							type: 'doc',
							id: 'topics/02.08.JsonMap',
							label: '2.8. JsonMap and JsonList',
						},
						{
							type: 'doc',
							id: 'topics/02.08.01.MarshalledNodeAndJsonPointer',
							label: '2.8.1. Tree Model & RFC 6901 JSON-Pointer',
						},
						{
							type: 'doc',
							id: 'topics/02.09.ComplexDataTypes',
							label: '2.9. Complex Data Types',
						},
						{
							type: 'doc',
							id: 'topics/02.09.01.SupportedJdkDatatypes',
							label: '2.9.1. Supported JDK Datatypes',
						},
						{
							type: 'doc',
							id: 'topics/02.10.SerializerSetsParserSets',
							label: '2.10. SerializerSets and ParserSets',
						},
						{
							type: 'category',
							label: '2.11. Swaps',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.11.01.SwapBasics',
									label: '2.11.1. Swap Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.11.02.DefaultSwaps',
									label: '2.11.2. Default Swaps',
								},
								{
									type: 'doc',
									id: 'topics/02.11.03.AutoSwaps',
									label: '2.11.3. Auto-detected swaps',
								},
								{
									type: 'doc',
									id: 'topics/02.11.04.PerMediaTypeSwaps',
									label: '2.11.4. Per-media-type Swaps',
								},
								{
									type: 'doc',
									id: 'topics/02.11.05.OneWaySwaps',
									label: '2.11.5. One-way Swaps',
								},
								{
									type: 'doc',
									id: 'topics/02.11.06.SwapAnnotation',
									label: '2.11.6. @Swap Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.11.07.TemplatedSwaps',
									label: '2.11.7. Templated Swaps',
								},
								{
									type: 'doc',
									id: 'topics/02.11.08.SurrogateClasses',
									label: '2.11.8. Surrogate Classes',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.12.DynamicallyAppliedAnnotations',
							label: '2.12. Dynamically Applied Annotations',
						},
						{
							type: 'category',
							label: '2.13. Bean Dictionaries',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.13.01.BeanDictionaryBasics',
									label: '2.13.1. Bean Dictionary Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.13.02.BeanSubTypes',
									label: '2.13.2. Bean Subtypes',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.14.VirtualBeans',
							label: '2.14. Virtual Beans',
						},
						{
							type: 'doc',
							id: 'topics/02.15.Recursion',
							label: '2.15. Non-Tree Models and Recursion Detection',
						},
						{
							type: 'doc',
							id: 'topics/02.16.ParsingIntoGenericModels',
							label: '2.16. Parsing into Generic Models',
						},
						{
							type: 'doc',
							id: 'topics/02.17.ReadingContinuousStreams',
							label: '2.17. Reading Continuous Streams',
						},
						{
							type: 'doc',
							id: 'topics/02.18.LargeDatasetStreaming',
							label: '2.18. Large-Dataset Streaming',
						},
						{
							type: 'doc',
							id: 'topics/02.19.MarshallingUris',
							label: '2.19. URIs',
						},
						{
							type: 'doc',
							id: 'topics/02.20.JacksonComparison',
							label: '2.20. Comparison with Jackson',
						},
						{
							type: 'doc',
							id: 'topics/02.21.PojoCategories',
							label: '2.21. POJO Categories',
						},
						{
							type: 'category',
							label: '2.22. Simple Variable Language',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.22.01.SimpleVariableLanguageBasics',
									label: '2.22.1. Simple Variable Language Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.22.02.SvlVariables',
									label: '2.22.2. SVL Variables',
								},
								{
									type: 'doc',
									id: 'topics/02.22.03.VarResolvers',
									label: '2.22.3. VarResolvers and VarResolverSessions',
								},
								{
									type: 'doc',
									id: 'topics/02.22.04.DefaultVarResolver',
									label: '2.22.4. VarResolver.DEFAULT',
								},
								{
									type: 'doc',
									id: 'topics/02.22.05.SvlOtherNotes',
									label: '2.22.5. Other Notes',
								},
								{
									type: 'doc',
									id: 'topics/02.22.06.ValueAnnotationBasics',
									label: '2.22.6. @Value Annotation Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.22.07.ValueFrameworkInternal',
									label: '2.22.7. @Value Framework-Internal Adoption',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.23.Encoders',
							label: '2.23. Encoders',
						},
						{
							type: 'doc',
							id: 'topics/02.24.ObjectTools',
							label: '2.24. Object Tools',
						},
						{
							type: 'category',
							label: '2.25. JSON Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.25.01.JsonBasics',
									label: '2.25.1. JSON Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.25.02.JsonMethodology',
									label: '2.25.2. JSON Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.25.03.JsonSerializers',
									label: '2.25.3. JSON Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.25.04.Json5',
									label: '2.25.4. JSON 5',
								},
								{
									type: 'doc',
									id: 'topics/02.25.05.JsonParsers',
									label: '2.25.5. JSON Parsers',
								},
								{
									type: 'doc',
									id: 'topics/02.25.06.JsonAnnotation',
									label: '2.25.6. @Json Annotation',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.26.JsonSchemaDetails',
							label: '2.26. JSON-Schema Support',
						},
						{
							type: 'category',
							label: '2.27. XML Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.27.01.XmlBasics',
									label: '2.27.1. XML Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.27.02.XmlMethodology',
									label: '2.27.2. XML Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.27.03.XmlSerializers',
									label: '2.27.3. XML Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.27.04.XmlParsers',
									label: '2.27.4. XML Parsers',
								},
								{
									type: 'doc',
									id: 'topics/02.27.05.XmlBeanTypeNameAnnotation',
									label: '2.27.5. @Marshalled(typeName) Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.27.06.XmlChildNameAnnotation',
									label: '2.27.6. @Xml(childName) Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.27.07.XmlFormatAnnotation',
									label: '2.27.7. @Xml(format) Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.27.08.XmlNamespaces',
									label: '2.27.8. Namespaces',
								},
							],
						},
						{
							type: 'category',
							label: '2.28. HTML Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.28.01.HtmlBasics',
									label: '2.28.1. HTML Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.28.02.HtmlMethodology',
									label: '2.28.2. HTML Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.28.03.HtmlSerializers',
									label: '2.28.3. HTML Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.28.04.HtmlParsers',
									label: '2.28.4. HTML Parsers',
								},
								{
									type: 'doc',
									id: 'topics/02.28.05.HtmlAnnotation',
									label: '2.28.5. @Html Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.28.06.HtmlRenderAnnotation',
									label: '2.28.6. @Html(render) Annotation',
								},
								{
									type: 'doc',
									id: 'topics/02.28.07.HtmlDocSerializer',
									label: '2.28.7. HtmlDocSerializer',
								},
								{
									type: 'doc',
									id: 'topics/02.28.08.BasicHtmlDocTemplate',
									label: '2.28.8. BasicHtmlDocTemplate',
								},
								{
									type: 'doc',
									id: 'topics/02.28.09.HtmlCustomTemplates',
									label: '2.28.9. Custom Templates',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/02.29.HtmlSchemaSupport',
							label: '2.29. HTML-Schema Support',
						},
						{
							type: 'category',
							label: '2.30. UON Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.30.01.UonBasics',
									label: '2.30.1. UON Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.30.02.UonMethodology',
									label: '2.30.2. UON Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.30.03.UonSerializers',
									label: '2.30.3. UON Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.30.04.UonParsers',
									label: '2.30.4. UON Parsers',
								},
							],
						},
						{
							type: 'category',
							label: '2.31. URL-Encoding Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.31.01.UrlEncodingBasics',
									label: '2.31.1. URL-Encoding Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.31.02.UrlEncMethodology',
									label: '2.31.2. URL-Encoding Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.31.03.UrlEncSerializers',
									label: '2.31.3. URL-Encoding Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.31.04.UrlEncParsers',
									label: '2.31.4. URL-Encoding Parsers',
								},
								{
									type: 'doc',
									id: 'topics/02.31.05.UrlEncodingAnnotation',
									label: '2.31.5. @UrlEncoding Annotation',
								},
							],
						},
						{
							type: 'category',
							label: '2.32. MessagePack Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.32.01.MessagePackBasics',
									label: '2.32.1. MessagePack Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.32.02.MsgPackSerializers',
									label: '2.32.2. MessagePack Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.32.03.MsgPackParsers',
									label: '2.32.3. MessagePack Parsers',
								},
							],
						},
						{
							type: 'category',
							label: '2.33. OpenApi Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.33.01.OpenApiBasics',
									label: '2.33.1. OpenApi Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.33.02.OpenApiMethodology',
									label: '2.33.2. OpenAPI Methodology',
								},
								{
									type: 'doc',
									id: 'topics/02.33.03.OpenApiSerializers',
									label: '2.33.3. OpenAPI Serializers',
								},
								{
									type: 'doc',
									id: 'topics/02.33.04.OpenApiParsers',
									label: '2.33.4. OpenAPI Parsers',
								},
							],
						},
						{
							type: 'category',
							label: '2.34. TOML Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.34.01.TomlBasics',
									label: '2.34.1. TOML Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.34.5. Protobuf Text Format',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.34.05.ProtobufBasics',
									label: '2.34.5.1. Protobuf Text Format Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.34.07.ProtobufBinaryBasics',
									label: '2.34.5.2. Protobuf Binary Format Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.34.06.ParquetBasics',
									label: '2.34.6. Parquet Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.38. JSONL Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.38.01.JsonlBasics',
									label: '2.38.1. JSONL Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.39. Hjson Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.39.01.HjsonBasics',
									label: '2.39.1. Hjson Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.40. JCS Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.40.01.JcsBasics',
									label: '2.40.1. JCS Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.41. BSON Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.41.01.BsonBasics',
									label: '2.41.1. BSON Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.42. CBOR Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.42.01.CborBasics',
									label: '2.42.1. CBOR Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.43. HOCON Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.43.01.HoconBasics',
									label: '2.43.1. HOCON Basics',
								},
								{
									type: 'doc',
									id: 'topics/02.43.02.IniBasics',
									label: '2.43.2. INI Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.44. SSE Support',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/02.44.01.SseBasics',
									label: '2.44.1. SSE Basics',
								},
							],
						},
						{
							type: 'category',
							label: '2.35. YAML Support',
							collapsed: true,
							items: [
								{ type: 'doc', id: 'topics/02.35.01.YamlBasics', label: '2.35.1. YAML Basics' },
								{ type: 'doc', id: 'topics/02.35.02.YamlMethodology', label: '2.35.2. YAML Methodology' },
								{ type: 'doc', id: 'topics/02.35.03.YamlSerializers', label: '2.35.3. YAML Serializers' },
								{ type: 'doc', id: 'topics/02.35.04.YamlParsers', label: '2.35.4. YAML Parsers' },
								{ type: 'doc', id: 'topics/02.35.05.YamlAnnotation', label: '2.35.5. @YamlConfig Annotation' },
							],
						},
						{
							type: 'category',
							label: '2.36. CSV Support',
							collapsed: true,
							items: [
								{ type: 'doc', id: 'topics/02.36.01.CsvBasics', label: '2.36.1. CSV Basics' },
								{ type: 'doc', id: 'topics/02.36.02.CsvSerializers', label: '2.36.2. CSV Serializers' },
								{ type: 'doc', id: 'topics/02.36.03.CsvParsers', label: '2.36.3. CSV Parsers' },
							],
						},
						{
							type: 'category',
							label: '2.37. Markdown Support',
							collapsed: true,
							items: [
								{ type: 'doc', id: 'topics/02.37.01.MarkdownBasics', label: '2.37.1. Markdown Basics' },
							],
						},
						{
							type: 'category',
							label: '2.50. Token / Record Streaming',
							collapsed: true,
							items: [
								{ type: 'doc', id: 'topics/02.50.01.TokenStreamingBasics', label: '2.50.1. Token-Streaming Basics' },
								{ type: 'doc', id: 'topics/02.50.02.RecordStreaming', label: '2.50.2. Record Streaming' },
								{ type: 'doc', id: 'topics/02.50.03.TokenStreaming', label: '2.50.3. Token Streaming' },
								{ type: 'doc', id: 'topics/02.50.04.ArrayRecordStreaming', label: '2.50.4. Array-Record Streaming' },
								{ type: 'doc', id: 'topics/02.50.05.RestStreamingIntegration', label: '2.50.5. REST Streaming Integration' },
							],
						},
						{
							type: 'doc',
							id: 'topics/02.51.BestPractices',
							label: '2.51. Best Practices',
						},
					],
				},
				{
					type: 'category',
					label: '3. juneau-marshall-rdf',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/03.01.JuneauMarshallRdfOverview',
							label: '3.1. juneau-marshall-rdf',
						},
						{
							type: 'doc',
							id: 'topics/03.02.RdfBasics',
							label: '3.2. RDF Basics',
						},
						{
							type: 'doc',
							id: 'topics/03.03.RdfSerializers',
							label: '3.3. RDF Serializers',
						},
						{
							type: 'doc',
							id: 'topics/03.04.RdfParsers',
							label: '3.4. RDF Parsers',
						},
					],
				},
				{
					type: 'category',
					label: '4. juneau-bean',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/04.01.JuneauBeanBasics',
							label: '4.1. juneau-bean Basics',
						},
						{
							type: 'doc',
							id: 'topics/04.02.JuneauBeanHtml5',
							label: '4.2. juneau-bean-html5',
						},
						{
							type: 'doc',
							id: 'topics/04.03.JuneauBeanAtom',
							label: '4.3. juneau-bean-atom',
						},
						{
							type: 'doc',
							id: 'topics/04.04.JuneauBeanJsonSchema',
							label: '4.4. juneau-bean-jsonschema',
						},
						{
							type: 'doc',
							id: 'topics/04.05.JuneauBeanOpenApi3',
							label: '4.5. juneau-bean-openapi-v3',
						},
						{
							type: 'doc',
							id: 'topics/04.06.JuneauBeanCommon',
							label: '4.6. juneau-bean-common',
						},
						{
							type: 'doc',
							id: 'topics/04.07.JuneauBeanSwagger2',
							label: '4.7. juneau-bean-swagger-v2',
						},
						{
							type: 'doc',
							id: 'topics/04.08.JuneauBeanMcp',
							label: '4.8. juneau-bean-mcp',
						},
						{
							type: 'doc',
							id: 'topics/04.09.JuneauBeanRfc7807',
							label: '4.9. juneau-bean-rfc7807',
						},
						{
							type: 'doc',
							id: 'topics/04.10.JuneauBeanHal',
							label: '4.10. juneau-bean-hal',
						},
						{
							type: 'doc',
							id: 'topics/04.11.JuneauBeanJsonApi',
							label: '4.11. juneau-bean-jsonapi',
						},
						{
							type: 'doc',
							id: 'topics/04.12.JuneauBeanJsonPatch',
							label: '4.12. juneau-bean-jsonpatch',
						},
					],
				},
				{
					type: 'category',
					label: '5. juneau-config',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/05.01.JuneauConfigBasics',
							label: '5.1. juneau-config Basics',
						},
						{
							type: 'category',
							label: '5.2. Overview',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.02.01.ConfigOverviewBasics',
									label: '5.2.1. Config Overview Basics',
								},
								{
									type: 'doc',
									id: 'topics/05.02.02.SyntaxRules',
									label: '5.2.2. Syntax Rules',
								},
							],
						},
						{
							type: 'category',
							label: '5.3. Reading Entries',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.03.01.ReadingEntriesBasics',
									label: '5.3.1. Reading Entries Basics',
								},
								{
									type: 'doc',
									id: 'topics/05.03.02.Pojos',
									label: '5.3.2. POJOs',
								},
								{
									type: 'doc',
									id: 'topics/05.03.03.Arrays',
									label: '5.3.3. Arrays',
								},
								{
									type: 'doc',
									id: 'topics/05.03.04.JCFObjects',
									label: '5.3.4. Java Collection Framework Objects',
								},
								{
									type: 'doc',
									id: 'topics/05.03.05.BinaryData',
									label: '5.3.5. Binary Data',
								},
							],
						},
						{
							type: 'category',
							label: '5.4. Variables',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.04.01.VariableBasics',
									label: '5.4.1. Variable Basics',
								},
								{
									type: 'doc',
									id: 'topics/05.04.02.LogicVariables',
									label: '5.4.2. Logic Variables',
								},
								{
									type: 'doc',
									id: 'topics/05.04.03.PropertySources',
									label: '5.4.3. Property Sources',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/05.05.ModdedEntries',
							label: '5.5. Modded/Encoded Entries',
						},
						{
							type: 'doc',
							id: 'topics/05.06.Sections',
							label: '5.6. Sections',
						},
						{
							type: 'category',
							label: '5.7. Setting Values',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.07.01.SettingValuesBasics',
									label: '5.7.1. Setting Values Basics',
								},
								{
									type: 'doc',
									id: 'topics/05.07.02.FileSystemChanges',
									label: '5.7.2. File System Changes',
								},
								{
									type: 'doc',
									id: 'topics/05.07.03.CustomEntrySerialization',
									label: '5.7.3. Custom Entry Serialization',
								},
								{
									type: 'doc',
									id: 'topics/05.07.04.BulkSettingValues',
									label: '5.7.4. Setting Values in Bulk',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/05.08.Listeners',
							label: '5.8. Listeners',
						},
						{
							type: 'doc',
							id: 'topics/05.09.SerializingConfigs',
							label: '5.9. Serializing',
						},
						{
							type: 'doc',
							id: 'topics/05.10.Imports',
							label: '5.10. Imports',
						},
						{
							type: 'category',
							label: '5.11. Config Stores',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/05.11.01.ConfigStoresBasics',
									label: '5.11.1. Config Stores Basics',
								},
								{
									type: 'doc',
									id: 'topics/05.11.02.MemoryStore',
									label: '5.11.2. MemoryStore',
								},
								{
									type: 'doc',
									id: 'topics/05.11.03.FileStore',
									label: '5.11.3. FileStore',
								},
								{
									type: 'doc',
									id: 'topics/05.11.04.CustomStores',
									label: '5.11.4. Custom ConfigStores',
								},
								{
									type: 'doc',
									id: 'topics/05.11.05.StoreListeners',
									label: '5.11.5. ConfigStore Listeners',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/05.12.ReadOnlyConfigs',
							label: '5.12. Read-only Configs',
						},
						{
							type: 'doc',
							id: 'topics/05.13.ClosingConfigs',
							label: '5.13. Closing Configs',
						},
						{
							type: 'doc',
							id: 'topics/05.14.SystemDefaultConfig',
							label: '5.14. System Default Config',
						},
						{
							type: 'doc',
							id: 'topics/05.15.YamlConfigFiles',
							label: '5.15. YAML Config Files',
						},
					],
				},
					{
						type: 'category',
						label: '6. juneau-commons',
						collapsed: true,
						items: [
							{
								type: 'doc',
								id: 'topics/06.01.JuneauCommonsBasics',
								label: '6.1. juneau-commons Basics',
							},
							{
								type: 'category',
								label: '6.2. Packages',
								collapsed: true,
								items: [
									{
										type: 'doc',
										id: 'topics/06.02.01.JuneauCommonsCollections',
										label: '6.2.1. Collections Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.02.JuneauCommonsUtils',
										label: '6.2.2. Utils Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.03.JuneauCommonsReflection',
										label: '6.2.3. Reflection Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.04.JuneauCommonsSettings',
										label: '6.2.4. Settings Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.05.JuneauCommonsLang',
										label: '6.2.5. Lang Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.06.JuneauCommonsIO',
										label: '6.2.6. I/O Package',
									},
									{
										type: 'doc',
										id: 'topics/06.02.07.JuneauCommonsInject',
										label: '6.2.7. Inject Package',
									},
								],
							},
						],
					},
				{
					type: 'category',
					label: '7. juneau-assertions',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/07.01.JuneauAssertionBasics',
							label: '7.1. juneau-assertions Basics',
						},
						{
							type: 'doc',
							id: 'topics/07.02.AssertionsOverview',
							label: '7.2. Assertions Overview',
						},
					],
				},
				{
					type: 'category',
					label: '8. juneau-bct',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/08.01.JuneauBctBasics',
							label: '8.1. juneau-bct Basics',
						},
						{
							type: 'doc',
							id: 'topics/08.02.CustomErrorMessages',
							label: '8.2. Custom Error Messages',
						},
						{
							type: 'category',
							label: '8.3. Customization',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/08.03.01.Customization',
									label: '8.3.1. Customization',
								},
								{
									type: 'doc',
									id: 'topics/08.03.02.Stringifiers',
									label: '8.3.2. Stringifiers',
								},
								{
									type: 'doc',
									id: 'topics/08.03.03.Listifiers',
									label: '8.3.3. Listifiers',
								},
								{
									type: 'doc',
									id: 'topics/08.03.04.Swappers',
									label: '8.3.4. Swappers',
								},
								{
									type: 'doc',
									id: 'topics/08.03.05.PropertyExtractors',
									label: '8.3.5. Property Extractors',
								},
							],
						},
					],
				},
				{
					type: 'category',
					label: '9. juneau-rest-common',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/09.01.JuneauRestCommonBasics',
							label: '9.1. juneau-rest-common Basics',
						},
						{
							type: 'doc',
							id: 'topics/09.02.HelperClasses',
							label: '9.2. Helper Classes',
						},
						{
							type: 'doc',
							id: 'topics/09.03.Annotations',
							label: '9.3. Annotations',
						},
						{
							type: 'doc',
							id: 'topics/09.04.HttpHeaders',
							label: '9.4. HTTP Headers',
						},
						{
							type: 'doc',
							id: 'topics/09.05.HttpParts',
							label: '9.5. HTTP Parts',
						},
						{
							type: 'doc',
							id: 'topics/09.06.HttpEntitiesAndResources',
							label: '9.6. HTTP Entities and Resources',
						},
						{
							type: 'doc',
							id: 'topics/09.07.HttpResponses',
							label: '9.7. HTTP Responses',
						},
						{
							type: 'doc',
							id: 'topics/09.08.RemoteProxyInterfaces',
							label: '9.8. Remote Proxy Interfaces',
						},
					],
				},
				{
					type: 'category',
					label: '10. juneau-rest-server',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/10.01.JuneauRestServerBasics',
							label: '10.1. juneau-rest-server Basics',
						},
						{
							type: 'doc',
							id: 'topics/10.02.RestServerOverview',
							label: '10.2. REST Server Overview',
						},
						{
							type: 'category',
							label: '10.3. @Rest-Annotated Classes',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/10.03.01.RestAnnotatedClassBasics',
									label: '10.3.1. @Rest-Annotated Class Basics',
								},
								{
									type: 'doc',
									id: 'topics/10.03.02.PredefinedClasses',
									label: '10.3.2. Predefined Classes',
								},
								{
									type: 'doc',
									id: 'topics/10.03.03.ChildResources',
									label: '10.3.3. Child Resources',
								},
								{
									type: 'doc',
									id: 'topics/10.03.04.PathVariables',
									label: '10.3.4. Path Variables',
								},
								{
									type: 'doc',
									id: 'topics/10.03.05.Deployment',
									label: '10.3.5. Deployment',
								},
								{
									type: 'doc',
									id: 'topics/10.03.06.LifecycleHooks',
									label: '10.3.6. Lifecycle Hooks',
								},
							],
						},
						{
							type: 'category',
							label: '10.4. @RestOp-Annotated Methods',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/10.04.01.RestOpAnnotatedMethodBasics',
									label: '10.4.1. @RestOp-Annotated Method Basics',
								},
								{
									type: 'doc',
									id: 'topics/10.04.02.InferredHttpMethodsAndPaths',
									label: '10.4.2. Inferred HTTP Methods and Paths',
								},
								{
									type: 'doc',
									id: 'topics/10.04.03.JavaMethodParameters',
									label: '10.4.3. Java Method Parameters',
								},
								{
									type: 'doc',
									id: 'topics/10.04.04.JavaMethodReturnTypes',
									label: '10.4.4. Java Method Return Types',
								},
								{
									type: 'doc',
									id: 'topics/10.04.05.JavaMethodThrowableTypes',
									label: '10.4.5. Java Method Throwable Types',
								},
								{
									type: 'doc',
									id: 'topics/10.04.06.PathPatterns',
									label: '10.4.6. Path Patterns',
								},
								{
									type: 'doc',
									id: 'topics/10.04.07.Matchers',
									label: '10.4.7. Matchers',
								},
								{
									type: 'doc',
									id: 'topics/10.04.08.OverloadingHttpMethods',
									label: '10.4.8. Overloading HTTP Methods',
								},
								{
									type: 'doc',
									id: 'topics/10.04.09.AdditionalInformation',
									label: '10.4.9. Additional Information',
								},
							],
						},
						{
							type: 'category',
							label: '10.5. HTTP Parts',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/10.05.01.HttpPartBasics',
									label: '10.5.1. HTTP Part Basics',
								},
								{
									type: 'doc',
									id: 'topics/10.05.02.PartMarshallers',
									label: '10.5.2. Part Marshallers',
								},
								{
									type: 'doc',
									id: 'topics/10.05.03.HttpPartAnnotations',
									label: '10.5.3. HTTP Part Annotations',
								},
								{
									type: 'doc',
									id: 'topics/10.05.04.DefaultParts',
									label: '10.5.4. Default Parts',
								},
								{
									type: 'doc',
									id: 'topics/10.05.05.RequestBeans',
									label: '10.5.5. Request Beans',
								},
								{
									type: 'doc',
									id: 'topics/10.05.06.ResponseBeans',
									label: '10.5.6. @Response Beans',
								},
								{
									type: 'doc',
									id: 'topics/10.05.07.HttpPartApis',
									label: '10.5.7. HTTP Part APIs',
								},
								{
									type: 'doc',
									id: 'topics/10.05.08.HttpPartValidation',
									label: '10.5.8. HTTP Part Validation',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.06.Marshalling',
							label: '10.6. Marshalling',
						},
						{
							type: 'doc',
							id: 'topics/10.07.HandlingFormPosts',
							label: '10.7. Form Posts',
						},
						{
							type: 'doc',
							id: 'topics/10.08.RestServerComposition',
							label: '10.8. REST Server — Mixins and Multi-Mount Paths',
						},
						{
							type: 'doc',
							id: 'topics/10.09.RestServerMixinSubContexts',
							label: '10.9. REST Server — Mixin Sub-Contexts',
						},
						{
							type: 'doc',
							id: 'topics/10.10.RestServerStandaloneVsMixin',
							label: '10.10. REST Server — Standalone vs Mixin Resources',
						},
						{
							type: 'doc',
							id: 'topics/10.11.RestServerSelfRegistration',
							label: '10.11. REST Server — Self-Registration',
						},
						{
							type: 'doc',
							id: 'topics/10.12.RestServerProgrammaticBuilder',
							label: '10.12. REST Server — Programmatic Fluent Builder',
						},
						{
							type: 'doc',
							id: 'topics/10.14.RestServerSse',
							label: '10.14. Server-Sent Events',
						},
						{
							type: 'doc',
							id: 'topics/10.13.Guards',
							label: '10.13. Guards',
						},
						{
							type: 'doc',
							id: 'topics/10.15.Converters',
							label: '10.15. Converters',
						},
						{
							type: 'doc',
							id: 'topics/10.16.LocalizedMessages',
							label: '10.16. Localized Messages',
						},
						{
							type: 'doc',
							id: 'topics/10.17.Encoders',
							label: '10.17. Encoders',
						},
						{
							type: 'doc',
							id: 'topics/10.18.ConfigurationFiles',
							label: '10.18. Configuration Files',
						},
						{
							type: 'doc',
							id: 'topics/10.19.SvlVariables',
							label: '10.19. SVL Variables',
						},
						{
							type: 'doc',
							id: 'topics/10.20.StaticFiles',
							label: '10.20. Static files',
						},
						{
							type: 'doc',
							id: 'topics/10.21.StaticFilesMixin',
							label: '10.21. Static-Files Mixin',
						},
						{
							type: 'doc',
							id: 'topics/10.22.ConventionEndpointsMixins',
							label: '10.22. Convention-Endpoints Mixin Pack',
						},
						{
							type: 'doc',
							id: 'topics/10.23.OpsIntrospectionMixins',
							label: '10.23. Ops / Introspection Mixin Pack',
						},
						{
							type: 'doc',
							id: 'topics/10.24.JspViewSupport',
							label: '10.24. JSP View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.25.ThymeleafViewSupport',
							label: '10.25. Thymeleaf View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.26.MustacheViewSupport',
							label: '10.26. Mustache View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.27.FreemarkerViewSupport',
							label: '10.27. FreeMarker View Support',
						},
						{
							type: 'doc',
							id: 'topics/10.28.ClientVersioning',
							label: '10.28. Client Versioning',
						},
						{
							type: 'category',
							label: '10.29. Swagger',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/10.29.01.SwaggerBasics',
									label: '10.29.1. Swagger Basics',
								},
							{
								type: 'doc',
								id: 'topics/10.29.02.BasicRestServletSwagger',
								label: '10.29.2. BasicRestServlet/BasicRestObject Swagger and OpenAPI 3.1',
							},
							{
								type: 'doc',
								id: 'topics/10.29.03.ApiDocsMixins',
								label: '10.29.3. API-Docs Mixin Pack',
							},
							{
								type: 'doc',
								id: 'topics/10.29.04.BasicSwaggerInfo',
								label: '10.29.4. Basic Swagger Info',
							},
								{
									type: 'doc',
									id: 'topics/10.29.05.SwaggerTags',
									label: '10.29.5. Swagger Tags',
								},
								{
									type: 'doc',
									id: 'topics/10.29.06.SwaggerOperations',
									label: '10.29.6. Swagger Operations',
								},
								{
									type: 'doc',
									id: 'topics/10.29.07.SwaggerParameters',
									label: '10.29.7. Swagger Parameters',
								},
								{
									type: 'doc',
									id: 'topics/10.29.08.SwaggerResponses',
									label: '10.29.8. Swagger Responses',
								},
								{
									type: 'doc',
									id: 'topics/10.29.09.SwaggerModels',
									label: '10.29.9. Swagger Models',
								},
								{
									type: 'doc',
									id: 'topics/10.29.10.SwaggerStylesheet',
									label: '10.29.10. SwaggerUI.css',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.30.ExecutionStatistics',
							label: '10.30. REST method execution statistics',
						},
						{
							type: 'category',
							label: '10.31. @HtmlDocConfig Annotation',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/10.31.01.HtmlDocConfigAnnotationBasics',
									label: '10.31.1. @HtmlDocConfig Annotation Basics',
								},
								{
									type: 'doc',
									id: 'topics/10.31.02.HtmlUIvsDI',
									label: '10.31.2. User Interfaces (UI) vs. Developer Interfaces (DI)',
								},
								{
									type: 'doc',
									id: 'topics/10.31.03.HtmlWidgets',
									label: '10.31.3. Widgets',
								},
								{
									type: 'doc',
									id: 'topics/10.31.04.HtmlPredefinedWidgets',
									label: '10.31.4. Predefined Widgets',
								},
								{
									type: 'doc',
									id: 'topics/10.31.05.HtmlUiCustomization',
									label: '10.31.5. UI Customization',
								},
								{
									type: 'doc',
									id: 'topics/10.31.06.HtmlStylesheets',
									label: '10.31.6. Stylesheets',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/10.32.LoggingAndDebugging',
							label: '10.32. Logging / Debugging',
						},
						{
							type: 'doc',
							id: 'topics/10.33.HttpStatusCodes',
							label: '10.33. HTTP Status Codes',
						},
						{
							type: 'doc',
							id: 'topics/10.34.RestServerProblemDetails',
							label: '10.34. RFC 7807 / 9457 Problem Details',
						},
						{
							type: 'doc',
							id: 'topics/10.35.RestServerConditionalGet',
							label: '10.35. Conditional-GET / ETag Helpers',
						},
						{
							type: 'doc',
							id: 'topics/10.36.RestServerRateLimitAndRequestId',
							label: '10.36. Rate-Limiting and Request-Id Propagation',
						},
						{
							type: 'doc',
							id: 'topics/10.37.RestServerTestBeanInjection',
							label: '10.37. Test-time Bean Injection',
						},
						{
							type: 'doc',
							id: 'topics/10.38.RestServerAuthGuards',
							label: '10.38. AuthN Guards — Bearer / API-Key / JWT',
						},
						{
							type: 'doc',
							id: 'topics/10.39.RestServerValidation',
							label: '10.39. Jakarta Bean Validation',
						},
						{
							type: 'doc',
							id: 'topics/10.40.RestServerObservability',
							label: '10.40. Observability — Micrometer + OpenTelemetry',
						},
						{
							type: 'doc',
							id: 'topics/10.41.RestServerAsyncDispatch',
							label: '10.41. Async Returns + Virtual-Thread Dispatch',
						},
						{
							type: 'doc',
							id: 'topics/10.42.AuthFilterFramework',
							label: '10.42. AuthN Filter Framework — Servlet-Layer Authentication',
						},
						{
							type: 'doc',
							id: 'topics/10.43.SamlAuthSupport',
							label: '10.43. SAML 2.0 AuthN Support (juneau-rest-server-auth-saml)',
						},
						{
							type: 'doc',
							id: 'topics/10.44.OAuthAuthSupport',
							label: '10.44. OAuth 2.0 / OIDC AuthN Support (juneau-rest-server-auth-oauth)',
						},
						{
							type: 'doc',
							id: 'topics/10.45.OidcRelyingParty',
							label: '10.45. OIDC Relying Party Login (juneau-rest-server-auth-oidc-rp)',
						},
						{
							type: 'doc',
							id: 'topics/10.46.RestServerReactive',
							label: '10.46. Reactive-Streams Returns',
						},
						{
							type: 'doc',
							id: 'topics/10.47.BuiltInParameters',
							label: '10.47. Built-in Parameters',
						},
						{
							type: 'doc',
							id: 'topics/10.48.SessionOptions',
							label: '10.48. Session Options via HTTP',
						},
						{
							type: 'doc',
							id: 'topics/10.49.UsingWithOsgi',
							label: '10.49. Using with OSGi',
						},
						{
							type: 'doc',
							id: 'topics/10.50.RestContext',
							label: '10.50. RestContext',
						},
						{
							type: 'doc',
							id: 'topics/10.51.RestOpContext',
							label: '10.51. RestOpContext',
						},
						{
							type: 'doc',
							id: 'topics/10.52.ResponseProcessors',
							label: '10.52. Response Processors',
						},
						{
							type: 'doc',
							id: 'topics/10.53.RestRpc',
							label: '10.53. REST/RPC',
						},
						{
							type: 'doc',
							id: 'topics/10.54.SerializingUris',
							label: '10.54. Serializing URIs',
						},
						{
							type: 'doc',
							id: 'topics/10.55.UtilityBeans',
							label: '10.55. Utility Beans',
						},
						{
							type: 'doc',
							id: 'topics/10.56.HtmlBeans',
							label: '10.56. Using with HTML Beans',
						},
						{
							type: 'doc',
							id: 'topics/10.57.OtherNotes',
							label: '10.57. Other Notes',
						},
						{
							type: 'doc',
							id: 'topics/10.58.Log4j',
							label: '10.58. Using LOG4J for logging',
						},
					],
				},
				{
					type: 'category',
					label: '11. juneau-rest-server-mcp',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/11.01.JuneauRestServerMcpBasics',
							label: '11.1. juneau-rest-server-mcp Basics',
						},
					],
				},
				{
					type: 'category',
					label: '12. juneau-rest-server-springboot',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/12.01.JuneauRestServerSpringbootBasics',
							label: '12.1. juneau-rest-server-springboot Basics',
						},
						{
							type: 'doc',
							id: 'topics/12.02.SpringBootOverview',
							label: '12.2. Spring Boot Overview',
						},
					],
				},
				{
					type: 'category',
					label: '13. juneau-rest-client',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/13.01.JuneauRestClientBasics',
							label: '13.1. juneau-rest-client Basics',
						},
						{
							type: 'doc',
							id: 'topics/13.02.PojoMarshalling',
							label: '13.2. POJO Marshalling',
						},
						{
							type: 'doc',
							id: 'topics/13.03.RequestParts',
							label: '13.3. Request Parts',
						},
						{
							type: 'doc',
							id: 'topics/13.04.RequestContent',
							label: '13.4. Request Content',
						},
						{
							type: 'doc',
							id: 'topics/13.05.ResponseStatus',
							label: '13.5. Response Status',
						},
						{
							type: 'doc',
							id: 'topics/13.06.ResponseHeaders',
							label: '13.6. Response Headers',
						},
						{
							type: 'doc',
							id: 'topics/13.07.ResponseContent',
							label: '13.7. Response Content',
						},
						{
							type: 'doc',
							id: 'topics/13.08.CustomCallHandlers',
							label: '13.8. Custom Call Handlers',
						},
						{
							type: 'doc',
							id: 'topics/13.09.Interceptors',
							label: '13.9. Interceptors',
						},
						{
							type: 'category',
							label: '13.10. REST Proxies',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/13.10.01.RestProxyBasics',
									label: '13.10.1. REST Proxy Basics',
								},
								{
									type: 'doc',
									id: 'topics/13.10.02.Remote',
									label: '13.10.2. @Remote',
								},
								{
									type: 'doc',
									id: 'topics/13.10.03.RemoteMethod',
									label: '13.10.3. @RemoteOp',
								},
								{
									type: 'doc',
									id: 'topics/13.10.04.Content',
									label: '13.10.4. @Content',
								},
								{
									type: 'doc',
									id: 'topics/13.10.05.FormData',
									label: '13.10.5. @FormData',
								},
								{
									type: 'doc',
									id: 'topics/13.10.06.Query',
									label: '13.10.6. @Query',
								},
								{
									type: 'doc',
									id: 'topics/13.10.07.Header',
									label: '13.10.7. @Header',
								},
								{
									type: 'doc',
									id: 'topics/13.10.08.Path',
									label: '13.10.8. @Path',
								},
								{
									type: 'doc',
									id: 'topics/13.10.09.Request',
									label: '13.10.9. @Request',
								},
								{
									type: 'doc',
									id: 'topics/13.10.10.Response',
									label: '13.10.10. @Response',
								},
								{
									type: 'doc',
									id: 'topics/13.10.11.DualPurposeInterfaces',
									label: '13.10.11. Dual-purpose (end-to-end) interfaces',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/13.11.LoggingAndDebugging',
							label: '13.11. Logging and Debugging',
						},
						{
							type: 'doc',
							id: 'topics/13.12.CustomizingHttpClient',
							label: '13.12. Customizing HttpClient',
						},
						{
							type: 'doc',
							id: 'topics/13.13.ExtendingRestClient',
							label: '13.13. Extending RestClient',
						},
						{
							type: 'category',
							label: '13.14. Authentication',
							collapsed: true,
							items: [
								{
									type: 'doc',
									id: 'topics/13.14.01.AuthenticationBasics',
									label: '13.14.1. Authentication Basics',
								},
								{
									type: 'doc',
									id: 'topics/13.14.02.AuthenticationBASIC',
									label: '13.14.2. BASIC Authentication',
								},
								{
									type: 'doc',
									id: 'topics/13.14.03.AuthenticationForm',
									label: '13.14.3. FORM-based Authentication',
								},
								{
									type: 'doc',
									id: 'topics/13.14.04.AuthenticationOIDC',
									label: '13.14.4. OIDC Authentication',
								},
							],
						},
						{
							type: 'doc',
							id: 'topics/13.15.NextGenRestClient',
							label: '13.15. Next-Generation REST Client (Beta)',
						},
					],
				},
				{
					type: 'category',
					label: '14. juneau-rest-mock',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/14.01.JuneauRestMockBasics',
							label: '14.1. juneau-rest-mock Basics',
						},
						{
							type: 'doc',
							id: 'topics/14.02.MockRestClientOverview',
							label: '14.2. Mock REST Client Overview',
						},
					],
				},
				{
					type: 'category',
					label: '15. juneau-microservice',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/15.01.JuneauMicroserviceBasics',
							label: '15.1. juneau-microservice Basics',
						},
						{
							type: 'doc',
							id: 'topics/15.02.MicroserviceCoreOverview',
							label: '15.2. Microservice Core Overview',
						},
						{
							type: 'doc',
							id: 'topics/15.03.Args',
							label: '15.3. Args',
						},
						{
							type: 'doc',
							id: 'topics/15.04.Manifest',
							label: '15.4. Manifest',
						},
						{
							type: 'doc',
							id: 'topics/15.05.SystemProperties',
							label: '15.5. System properties',
						},
						{
							type: 'doc',
							id: 'topics/15.06.VarResolver',
							label: '15.6. VarResolver',
						},
						{
							type: 'doc',
							id: 'topics/15.07.ConsoleCommands',
							label: '15.7. Console Commands',
						},
						{
							type: 'doc',
							id: 'topics/15.08.Listeners',
							label: '15.8. Listeners',
						},
						{
							type: 'doc',
							id: 'topics/15.09.InjectAwareMicroservice',
							label: '15.9. Inject-Aware Microservice',
						},
					],
				},
				{
					type: 'category',
					label: '16. juneau-microservice-jetty',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/16.01.JuneauMicroserviceJettyBasics',
							label: '16.1. juneau-microservice-jetty Basics',
						},
						{
							type: 'doc',
							id: 'topics/16.02.MicroserviceJettyOverview',
							label: '16.2. Microservice Jetty Overview',
						},
						{
							type: 'doc',
							id: 'topics/16.03.LifecycleMethods',
							label: '16.3. Lifecycle Methods',
						},
						{
							type: 'doc',
							id: 'topics/16.04.ResourceClasses',
							label: '16.4. Resource Classes',
						},
						{
							type: 'doc',
							id: 'topics/16.05.PredefinedResourceClasses',
							label: '16.5. Predefined Resource Classes',
						},
						{
							type: 'doc',
							id: 'topics/16.06.Config',
							label: '16.6. Config',
						},
						{
							type: 'doc',
							id: 'topics/16.07.JettyXml',
							label: '16.7. Jetty.xml file',
						},
						{
							type: 'doc',
							id: 'topics/16.08.UiCustomization',
							label: '16.8. UI Customization',
						},
						{
							type: 'doc',
							id: 'topics/16.09.Extending',
							label: '16.9. Customizing via @Bean',
						},
						{
							type: 'doc',
							id: 'topics/16.10.HealthProbes',
							label: '16.10. Health / Readiness / Liveness Probes',
						},
						{
							type: 'doc',
							id: 'topics/16.11.GracefulShutdown',
							label: '16.11. Graceful Shutdown & Readiness Gating',
						},
					],
				},
				{
					type: 'category',
					label: '18. Starter Projects',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/18.01.StarterProjects',
							label: '18.1. Starter Projects',
						},
					],
				},
				{
					type: 'category',
					label: '19. juneau-petstore',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/19.01.JuneauPetstoreOverview',
							label: '19.1. juneau-petstore Overview',
						},
					],
				},
				{
					type: 'category',
					label: '20. juneau-examples',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/20.01.JuneauExamplesCore',
							label: '20.1. juneau-examples-core',
						},
					],
				},
				{
					type: 'category',
					label: '21. juneau-shaded',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/21.01.JuneauShadedOverview',
							label: '21.1. Juneau Shaded Artifacts',
						},
						{
							type: 'doc',
							id: 'topics/21.02.JuneauShadedCore',
							label: '21.2. juneau-shaded-core',
						},
						{
							type: 'doc',
							id: 'topics/21.03.JuneauShadedRestClient',
							label: '21.3. juneau-shaded-rest-client',
						},
						{
							type: 'doc',
							id: 'topics/21.04.JuneauShadedRestServer',
							label: '21.4. juneau-shaded-rest-server',
						},
						{
							type: 'doc',
							id: 'topics/21.05.JuneauShadedRestServerSpringboot',
							label: '21.5. juneau-shaded-rest-server-springboot',
						},
						{
							type: 'doc',
							id: 'topics/21.06.JuneauShadedAll',
							label: '21.6. juneau-shaded-all',
						},
					],
				},
				{
					type: 'category',
					label: '22. Security',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/22.01.SecurityBasics',
							label: '22.1. Security Basics',
						},
						{
							type: 'doc',
							id: 'topics/22.02.MarshallingSecurity',
							label: '22.2. Marshalling Security',
						},
						{
							type: 'doc',
							id: 'topics/22.03.SvlSecurity',
							label: '22.3. SVL Security',
						},
						{
							type: 'doc',
							id: 'topics/22.04.RestSecurity',
							label: '22.4. REST Security',
						},
						{
							type: 'doc',
							id: 'topics/22.05.ContentSecurityPolicy',
							label: '22.5. Content Security Policy',
						},
					],
				},
				{
					type: 'category',
					label: '23. V9.0 Migration Guide',
					collapsed: true,
					items: [
						{
							type: 'doc',
							id: 'topics/23.01.V9MigrationGuide',
							label: '23.1. v9.0 Migration Guide',
						},
					],
				},
		{
			type: 'category',
			label: '24. V10.0 Migration Guide',
			collapsed: true,
			items: [
				{
					type: 'doc',
					id: 'topics/24.01.V10MigrationGuide',
					label: '24.1. v10.0 Migration Guide',
				},
			],
		},
		],
	},
	// Developer Info section
		{
			type: 'category',
			label: 'Developer Info',
			collapsed: false,
			items: [
				{
					type: 'doc',
					id: 'developer-info/01.DeveloperLinks',
					label: '1. Developer Links',
				},
				{
					type: 'category',
					label: '2. How-to Articles',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/02.01.BecomingAContributor',
							label: '2.1. Becoming a contributor',
						},
						{
							type: 'doc',
							id: 'developer-info/02.02.NonCommittersContribute',
							label: '2.2. How non-committers can contribute code',
						},
						{
							type: 'doc',
							id: 'developer-info/02.03.NewMemberGuidelines',
							label: '2.3. New member guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.04.NewReleaseGuidelines',
							label: '2.4. New release guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.05.VersioningGuidelines',
							label: '2.5. Versioning guidelines',
						},
						{
							type: 'doc',
							id: 'developer-info/02.06.CodeFormattingStylesheet',
							label: '2.6. Using the code formatting stylesheet',
						},
						{
							type: 'doc',
							id: 'developer-info/02.08.DevelopmentWishList',
							label: '2.7. Development wish list',
						},
						{
							type: 'doc',
							id: 'developer-info/02.09.AiKnowledgeAuthoring',
							label: '2.8. AI Knowledge Authoring Guide',
						},
					],
				},
				{
					type: 'doc',
					id: 'developer-info/03.CurrentMembers',
					label: '3. Current Members',
				},
				{
					type: 'category',
					label: '4. Scripts',
					collapsed: false,
					items: [
						{
							type: 'doc',
							id: 'developer-info/04.01.Scripts',
							label: '4.1. Scripts',
						},
						{
							type: 'category',
							label: '4.2. Code Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.02.01.ScriptTest',
									label: '4.2.1. test.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.02.ScriptPush',
									label: '4.2.2. push.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.03.ScriptRelease',
									label: '4.2.3. release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.04.ScriptPromptPgpPassphrase',
									label: '4.2.4. prompt-pgp-passphrase.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.05.ScriptCurrentRelease',
									label: '4.2.5. current-release.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.06.ScriptMavenVersion',
									label: '4.2.6. maven-version.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.07.ScriptApplyEclipsePrefs',
									label: '4.2.7. apply-eclipse-prefs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.08.ScriptCleanupWhitespace',
									label: '4.2.8. cleanup-whitespace.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.09.ScriptCheckFluentSetterOverrides',
									label: '4.2.9. check-fluent-setter-overrides.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.10.ScriptRevertStaged',
									label: '4.2.10. revert-staged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.11.ScriptRevertUnstaged',
									label: '4.2.11. revert-unstaged.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.12.ScriptStartPetstoreJetty',
									label: '4.2.12. start-petstore-jetty.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.02.13.ScriptStartPetstoreSpringboot',
									label: '4.2.13. start-petstore-springboot.py',
								},
							],
						},
						{
							type: 'category',
							label: '4.3. Doc Scripts',
							collapsed: false,
							items: [
								{
									type: 'doc',
									id: 'developer-info/04.03.01.ScriptReleaseDocsStage',
									label: '4.3.1. release-docs-stage.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.02.ScriptReleaseDocs',
									label: '4.3.2. release-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.03.ScriptBuildDocs',
									label: '4.3.3. build-docs.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.04.ScriptCreateMvnSite',
									label: '4.3.4. create-mvn-site.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.05.ScriptStartDocusaurus',
									label: '4.3.5. start-docusaurus.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.06.ScriptCheckTopicLinks',
									label: '4.3.6. check-topic-links.py',
								},
								{
									type: 'doc',
									id: 'developer-info/04.03.07.ScriptAiArtifacts',
									label: '4.3.7. generate-ai-artifacts.py / check-ai-artifacts.py',
								},
							],
						},
					],
				},
				{
					type: 'doc',
					id: 'developer-info/05.ArticlesIndex',
					label: '5. Articles',
				},
			],
		},
		// Release Notes section 
		{
			type: 'category',
			label: 'Release Notes',
			collapsed: true,
			items: [
				{
					type: 'category',
					label: 'Version 10.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/10.0.0',
							label: '10.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 9.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/9.5.0',
							label: '9.5.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.2.0',
							label: '9.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.1.0',
							label: '9.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/9.0.1',
							label: '9.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/9.0.0',
							label: '9.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 8.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/8.2.0',
							label: '8.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.3',
							label: '8.1.3',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.2',
							label: '8.1.2',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.1',
							label: '8.1.1',
						},
						{
							type: 'doc',
							id: 'release-notes/8.1.0',
							label: '8.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/8.0.0',
							label: '8.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 7.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/7.2.2',
							label: '7.2.2',
						},
						{
							type: 'doc',
							id: 'release-notes/7.2.1',
							label: '7.2.1',
						},
						{
							type: 'doc',
							id: 'release-notes/7.2.0',
							label: '7.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/7.1.0',
							label: '7.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/7.0.1',
							label: '7.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/7.0.0',
							label: '7.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 6.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/6.4.0',
							label: '6.4.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.3.1',
							label: '6.3.1',
						},
						{
							type: 'doc',
							id: 'release-notes/6.3.0',
							label: '6.3.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.2.0',
							label: '6.2.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.1.0',
							label: '6.1.0',
						},
						{
							type: 'doc',
							id: 'release-notes/6.0.1',
							label: '6.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/6.0.0',
							label: '6.0.0',
						},
					],
				},
				{
					type: 'category',
					label: 'Version 5.x',
					items: [
						{
							type: 'doc',
							id: 'release-notes/5.2.0.1',
							label: '5.2.0.1',
						},
						{
							type: 'doc',
							id: 'release-notes/5.2.0.0',
							label: '5.2.0.0',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.20',
							label: '5.1.0.20',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.19',
							label: '5.1.0.19',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.18',
							label: '5.1.0.18',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.17',
							label: '5.1.0.17',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.16',
							label: '5.1.0.16',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.15',
							label: '5.1.0.15',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.14',
							label: '5.1.0.14',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.13',
							label: '5.1.0.13',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.12',
							label: '5.1.0.12',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.11',
							label: '5.1.0.11',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.10',
							label: '5.1.0.10',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.09',
							label: '5.1.0.09',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.08',
							label: '5.1.0.08',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.07',
							label: '5.1.0.07',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.06',
							label: '5.1.0.06',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.05',
							label: '5.1.0.05',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.04',
							label: '5.1.0.04',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.03',
							label: '5.1.0.03',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.02',
							label: '5.1.0.02',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.01',
							label: '5.1.0.01',
						},
						{
							type: 'doc',
							id: 'release-notes/5.1.0.00',
							label: '5.1.0.00',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.36',
							label: '5.0.0.36',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.35',
							label: '5.0.0.35',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.34',
							label: '5.0.0.34',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.33',
							label: '5.0.0.33',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.32',
							label: '5.0.0.32',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.31',
							label: '5.0.0.31',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.30',
							label: '5.0.0.30',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.29',
							label: '5.0.0.29',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.28',
							label: '5.0.0.28',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.27',
							label: '5.0.0.27',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.26',
							label: '5.0.0.26',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.25',
							label: '5.0.0.25',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.24',
							label: '5.0.0.24',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.23',
							label: '5.0.0.23',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.22',
							label: '5.0.0.22',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.21',
							label: '5.0.0.21',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.20',
							label: '5.0.0.20',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.19',
							label: '5.0.0.19',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.18',
							label: '5.0.0.18',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.17',
							label: '5.0.0.17',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.16',
							label: '5.0.0.16',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.15',
							label: '5.0.0.15',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.14',
							label: '5.0.0.14',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.13',
							label: '5.0.0.13',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.12',
							label: '5.0.0.12',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.11',
							label: '5.0.0.11',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.10',
							label: '5.0.0.10',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.09',
							label: '5.0.0.09',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.08',
							label: '5.0.0.08',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.07',
							label: '5.0.0.07',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.06',
							label: '5.0.0.06',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.05',
							label: '5.0.0.05',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.04',
							label: '5.0.0.04',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.03',
							label: '5.0.0.03',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.02',
							label: '5.0.0.02',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.01',
							label: '5.0.0.01',
						},
						{
							type: 'doc',
							id: 'release-notes/5.0.0.00',
							label: '5.0.0.00',
						},
					],
				},
			],
		},
	],
};

export default sidebars;
