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

interface ClassNode {
  name: string;
  type: 'interface' | 'class' | 'abstract' | 'method' | 'field' | 'annotation' | 'enum' | 'private-method' | 'private-field' | 'annotation-method' |
        'java-interface' | 'java-class' | 'java-abstract-class' | 'java-method' | 'java-field' | 'java-annotation' | 'java-enum' | 'java-private-method' | 'java-private-field' | 'java-annotation-method';
  link?: string;
  description?: string;
  children?: ClassNode[];
  childrenCondensed?: boolean;
}

interface ClassHierarchyProps {
  nodes: ClassNode[];
}

const getCssClass = (type: string) => {
  switch (type) {
    case 'interface':
    case 'java-interface':
      return 'jic'; // Java interface
    case 'class':
    case 'java-class':
      return 'jc'; // Java class
    case 'abstract':
    case 'java-abstract-class':
      return 'jac'; // Java abstract class
    case 'method':
    case 'java-method':
      return 'jm'; // Java method
    case 'field':
    case 'java-field':
      return 'jf'; // Java field
    case 'annotation':
    case 'java-annotation':
      return 'ja'; // Java annotation
    case 'enum':
    case 'java-enum':
      return 'je'; // Java enum
    case 'private-method':
    case 'java-private-method':
      return 'jmp'; // Java private method
    case 'private-field':
    case 'java-private-field':
      return 'jfp'; // Java private field
    case 'annotation-method':
    case 'java-annotation-method':
      return 'jma'; // Java annotation method
    default:
      return 'jc';
  }
};

const getTypeIcon = (type: string) => {
  switch (type) {
    case 'interface':
      return '\u24d8'; // ⓘ
    case 'class':
      return '\u24d2'; // ⓒ
    case 'abstract':
      return '\u24d2'; // ⓒ (same as class but different color)
    case 'method':
      return '\u25cf'; // ●
    case 'field':
      return '\u25cf'; // ●
    case 'annotation':
      return '\u24d0'; // ⓐ
    case 'enum':
      return '\u24d4'; // ⓔ
    case 'private-method':
      return '\u25c6'; // ◆
    case 'private-field':
      return '\u25cf'; // ●
    case 'annotation-method':
      return '\u25cf'; // ●
    default:
      return '\u24d2'; // ⓒ
  }
};

const ClassTreeNode: React.FC<{ node: ClassNode; level: number }> = ({ node, level }) => {
  const typeClass = getCssClass(node.type);
  
  return (
    <li className={typeClass}>
      {node.link ? (
        <a href={node.link}>
          {node.name}
        </a>
      ) : (
        node.name
      )}
      {node.description && (
        <span> - {node.description}</span>
      )}
      {node.children && node.children.length > 0 && (
        <ul className={node.childrenCondensed ? 'condensed' : ''}>
          {node.children.map((child, index) => (
            <ClassTreeNode key={index} node={child} level={level + 1} />
          ))}
        </ul>
      )}
    </li>
  );
};

export const ClassHierarchy: React.FC<ClassHierarchyProps> = ({ nodes }) => {
  return (
    <ul className="javatreec">
      {nodes.map((node, index) => (
        <ClassTreeNode key={index} node={node} level={0} />
      ))}
    </ul>
  );
};

export default ClassHierarchy;
