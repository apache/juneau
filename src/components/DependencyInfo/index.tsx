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

import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import styles from './styles.module.css';

type ArtifactSpec = {
  /** Maven artifactId. */
  artifact: string;
  /** OSGi bundle symbolic-name base (optional — omitted for artifacts with no OSGi bundle). */
  bundle?: string;
  /**
   * Override for the "Javadocs" link target: a package path like
   * "org/apache/juneau/commons" (no leading/trailing slash). When omitted, the target
   * is auto-derived from the generated artifact->package map, falling back to the
   * aggregate javadoc index.
   */
  javadocPackage?: string;
};

type Props = {
  artifact?: string;
  bundle?: string;
  artifacts?: ArtifactSpec[];
  groupId?: string;
  note?: string;
  mavenOnly?: boolean;
  bazel?: boolean;
  /** Override the "Javadocs" link target for the single-artifact form (see ArtifactSpec.javadocPackage). */
  javadocPackage?: string;
};

const DEFAULT_GROUP_ID = 'org.apache.juneau';
// Only used if customFields.juneauVersion is somehow absent (config always sets it).
const FALLBACK_VERSION = '10.0.0';

function mavenXml(groupId: string, artifact: string): string {
  // The Maven <dependency> keeps the literal ${juneau.version} property on purpose:
  // it mirrors the property the user defines in their own POM.
  return [
    '<dependency>',
    `    <groupId>${groupId}</groupId>`,
    `    <artifactId>${artifact}</artifactId>`,
    '    <version>${juneau.version}</version>',
    '</dependency>',
  ].join('\n');
}

function bazelCoordinate(groupId: string, artifact: string, version: string): string {
  return [
    '# rules_jvm_external (MODULE.bazel / WORKSPACE maven.install)',
    'maven.install(',
    '    artifacts = [',
    `        "${groupId}:${artifact}:${version}",`,
    '    ],',
    ')',
  ].join('\n');
}

function ArtifactRows(props: {
  spec: ArtifactSpec;
  groupId: string;
  groupPath: string;
  version: string;
  modulePaths: Record<string, string>;
  javadocPackages: Record<string, string>;
  mavenOnly: boolean;
  bazel: boolean;
  showHeading: boolean;
}): React.JSX.Element {
  const {spec, groupId, groupPath, version, modulePaths, javadocPackages, mavenOnly, bazel, showHeading} = props;
  const {artifact, bundle} = spec;

  const jarName = `${artifact}-${version}.jar`;
  const jarUrl = `https://repo1.maven.org/maven2/${groupPath}/${artifact}/${version}/${jarName}`;
  const bundleName = bundle ? `${bundle}_${version}.jar` : undefined;
  const centralUrl = `https://central.sonatype.com/artifact/${groupId}/${artifact}/${version}`;
  // Prefer an explicit per-artifact override, then the auto-derived base package for
  // this artifact, else fall back to the aggregate javadoc index.
  const javadocPackage = (spec.javadocPackage || javadocPackages[artifact] || '')
    .replace(/^\/+|\/+$/g, '');
  const javadocUrl = javadocPackage
    ? `pathname:///site/apidocs/${javadocPackage}/package-summary.html`
    : `pathname:///site/apidocs/index.html`;
  const modulePath = modulePaths[artifact];
  const sourceUrl = modulePath !== undefined
    ? `https://github.com/apache/juneau/tree/master/${modulePath}`
    : undefined;

  return (
    <table className={styles.depTable}>
      <tbody>
        {showHeading && (
          <tr>
            <th className={styles.artifactHeading} colSpan={2}>
              <code>{artifact}</code>
            </th>
          </tr>
        )}
        <tr>
          <th className={styles.rowLabel}>Maven</th>
          <td className={styles.mavenCell}>
            <CodeBlock language="xml">{mavenXml(groupId, artifact)}</CodeBlock>
          </td>
        </tr>
        {bazel && (
          <tr>
            <th className={styles.rowLabel}>Bazel</th>
            <td className={styles.mavenCell}>
              <CodeBlock language="python">{bazelCoordinate(groupId, artifact, version)}</CodeBlock>
            </td>
          </tr>
        )}
        {!mavenOnly && (
          <tr>
            <th className={styles.rowLabel}>Jar</th>
            <td>
              <Link href={jarUrl}>{jarName}</Link>
            </td>
          </tr>
        )}
        {!mavenOnly && bundleName && (
          <tr>
            <th className={styles.rowLabel}>OSGi</th>
            <td>
              <Link href={jarUrl}>{bundleName}</Link>
            </td>
          </tr>
        )}
        {!mavenOnly && (
          <tr>
            <th className={styles.rowLabel}>Links</th>
            <td className={styles.linksCell}>
              <Link href={centralUrl}>Maven Central</Link>
              {' · '}
              <Link to={javadocUrl}>Javadocs</Link>
              {sourceUrl && (
                <>
                  {' · '}
                  <Link href={sourceUrl}>Source</Link>
                </>
              )}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}

export default function DependencyInfo(props: Props): React.JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  const version = (siteConfig.customFields?.juneauVersion as string) || FALLBACK_VERSION;
  const modulePaths = (siteConfig.customFields?.juneauModulePaths as Record<string, string>) || {};
  const javadocPackages = (siteConfig.customFields?.juneauJavadocPackages as Record<string, string>) || {};

  const groupId = props.groupId || DEFAULT_GROUP_ID;
  const groupPath = groupId.replace(/\./g, '/');

  const specs: ArtifactSpec[] = props.artifacts && props.artifacts.length
    ? props.artifacts
    : props.artifact
      ? [{artifact: props.artifact, bundle: props.bundle, javadocPackage: props.javadocPackage}]
      : [];

  const multi = specs.length > 1;

  return (
    <div className={styles.depInfo}>
      {props.note && <p className={styles.note}>{props.note}</p>}
      {specs.map((spec) => (
        <ArtifactRows
          key={spec.artifact}
          spec={spec}
          groupId={groupId}
          groupPath={groupPath}
          version={version}
          modulePaths={modulePaths}
          javadocPackages={javadocPackages}
          mavenOnly={!!props.mavenOnly}
          bazel={!!props.bazel}
          showHeading={multi}
        />
      ))}
    </div>
  );
}
