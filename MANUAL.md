# Jackson: the Missing Reference Manual

## 1. Intro

- Read, write JSON, from File, into File, as text, byte[], convert to Map
- Read JSON as JsonNode, use JsonPointer
- Write XML
- Read Avro, convert to Map

## 2. Processing Models

### 2.1 Tree Model (JsonNode)
### 2.2 POJOs (data-binding / mapping)
### 2.3 Streaming
### 2.4 "Untyped" Java objects (Lists, Maps)

## 3. Main API Objects

### 3.1 Databinding: ObjectMapper and Friends

#### 3.1.1 ObjectMapper

- PropertyNamingStrategy?

#### 3.1.2 ObjectReader, related

- MappingIterator

#### 3.1.3 ObjectWriter, related

- SequenceWriter

#### 3.1.4 JavaType

- TypeFactory
- TypeReference

### 3.2 Streaming API: JsonFactory and Friends

#### 3.2.1 JsonFactory
#### 3.2.2 JsonParser
#### 3.2.3 JsonGenerator
#### 3.2.4 Delegates: JsonParserDelegate, JsonGeneratorDelegate
#### 3.2.5 FormatSchema
#### 3.2.6 PrettyPrinter
#### 3.2.7 JsonPointer

### 3.3 Context objects

#### 3.3.1 DeserializationContext

#### 3.3.2 SerializerProvider

#### 3.3.3 JsonStreamContext

Access to current type, parent context, current mapped value

#### 3.3.4 ObjectCodec

Barebones interface for callbacks to ObjectMapper, -Reader and -Writer

### 3.4 Misc other

- TokenBuffer for efficient buffering, used internally for buffering, conversions

## 4. Annotation-based Configuration

Mostly for databinding.


## 5. Configuration: on/off Features

### 5.1 Data-binding features

#### 5.1.1 MapperFeature
#### 5.1.2 DeserializationFeature
#### 5.1.3 SerializationFeature

### 5.2 Streaming features

#### 5.2.1 JsonFactory.Feature
#### 5.2.2 JsonParser.Feature
#### 5.2.3 JsonGenerator.Feature
#### 5.2.4 FormatFeature

## 6. Configuration: Other

### 6.1 ObjectMapper configuration
### 6.2 ObjectReader configuration
### 6.3 ObjectWriter configuration
### 6.4 Configuration by Module
### 6.5 FormatSchema

- Forward ref to data formats

### 7. Advanced Features

#### 7.1 Polymorphic type handling

- Annotations (@JsonTypeInfo)
- Default typing

#### 7.2 Object identity handling (Cyclic data structures)


## 8. Custom Handlers

### 8.1 Databinding, common

- PropertyNamingStrategy (back-ref?)
- Mix-Ins
- Defaults:
   - InjectableValues
   - JsonInclude
   - DateFormat
   - TimeZone
   - Visibility

### 8.2 Deserialization

#### 8.2.1 Custom deserializers
#### 8.2.2 Value instantiators
#### 8.2.3 Value injections

### 8.3 Serialization

#### 8.3.1 Custom Serializers
#### 8.3.2 Value instantiators
#### 8.3.3 Filtering

### 8.4 Streaming parsing

NOTE: most configurable via ObjectMapper, ObjectReader

- FormatFeatures too

### 8.5 Streaming generation

NOTE: most configurable via ObjectMapper, ObjectWriter

- CharacterEscapes
- PrettyPrinter
- FormatFeatures too

### 8.5 Tree handling

- JsonNodeFactory
- Comparators?

### 9. Extensions

#### 9.1 Datatype modules

#### 9.2 Dataformat extensions

#### 9.3 JAX-RS providers

#### 9.4 JVM Language Support

- Scala, Kotlin

#### 9.5 Other extensions

- Afterburner, Mr. Bean, JAXB Annotations
