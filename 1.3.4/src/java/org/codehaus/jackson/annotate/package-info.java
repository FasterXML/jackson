/**
 * Public core annotations, most of which are used to configure how
 * Data Mapping/Binding works, excluding annotations that directly
 * depend on Mapper classes.
 * Also contains parameter types (mostly enums) needed by annotations, and
 * a dummy marker class {@link org.codehaus.jackson.annotate.NoClass},
 * which is needed to
 * work around the problem of 'null' not being valid value for
 * annotation properties.
 */
package org.codehaus.jackson.annotate;
