/**
 * Main public API classes of the core streaming JSON
 * processor: most importantly {@link org.codehaus.jackson.JsonFactory}
 * used for constructing
 *  Json parser ({@link org.codehaus.jackson.JsonParser})
 * and generator
 * ({@link org.codehaus.jackson.JsonParser})
 * instances.
 * <p>
 * Public API of the higher-level mapping interfaces ("Mapping API")
 * is found from
 * under {@link org.codehaus.jackson.map} and not included here,
 * except for following base interfaces:
 * <ul>
 *<li>{@link org.codehaus.jackson.JsonNode} is included
 *within Streaming API to support integration of the Tree Model
 *(which is based on <code>JsonNode</code>) with the basic
 *parsers and generators (iff using mapping-supporting factory: which
 *is part of Mapping API, not core)
 *  </li>
 *<li>{@link org.codehaus.jackson.ObjectCodec} is included so that
 *  reference to the object capable of serializing/deserializing
 *  Objects to/from JSON (usually, {@link org.codehaus.jackson.map.ObjectMapper})
 *  can be exposed, without adding direct dependency to implementation.
 *  </li>
 *</ul>
 * </ul>
 */

package org.codehaus.jackson;
