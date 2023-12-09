# Jackson Coding Style Guide

This document serves as the coding conventions for Java source code in the Jackson project.
Some parts of this document only apply to Java source code, while others apply to all source code in the project.

## Table of Contents

1. [Source Formatting](#source-formatting)
    1. [Indentation](#indentation)
    2. [Import Statements](#import-statements)
        1. [Wildcard Imports](#wildcard-imports)
        2. [Ordering](#ordering)
2. [Naming Conventions](#naming-conventions)
    1. [Field Naming](#field-naming)
    2. [Method Naming](#method-naming)

## Source Formatting

### Indentation

- **Rule**: Use spaces instead of tabs.
- **Amount**: 4 spaces per indentation level.

### Import Statements

#### Wildcard Imports

- **Usage**: Wildcards are permissible.
- **Threshold**: Employ wildcards for 5 or more imports from the same package.
- **Exceptions**: For frequently used packages like `java.util`, `java.io`, and main Jackson packages (
  e.g., `com.fasterxml.jackson.databind`), wildcards may be used for fewer imports.

#### Ordering

1. **JDK Imports**: Begin with standard Java (JDK) imports.
2. **Jackson Imports**: Follow with Jackson imports, starting from base types (annotations, core, databind), ordered
   from foundational to higher-level.

## Naming Conventions

### Field Naming

- **Non-Public Fields**: Prefix non-public member fields with an underscore (e.g., `_fieldName`).

### Method Naming

- **Public Methods**: Adhere to standard Java naming conventions.
- **Internal Methods**: Prefix methods used within the class with an underscore.
- **Sub-Class Methods**: Optionally, use a leading underscore for methods intended for subclass use, but not for
  external class calls.
