package dev.studyforge.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The records, their JSON schemas and the example payloads must agree. Producer and consumer both
 * compile against these records, and both run a test that parses the shared example, so a breaking
 * change fails the build on either side before it reaches Kafka.
 */
class EventSchemaTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Map<String, Class<? extends Record>> EVENTS =
      Map.of("review-recorded", ReviewRecorded.class, "item-completed", ItemCompleted.class);

  @ParameterizedTest
  @ValueSource(strings = {"review-recorded", "item-completed"})
  void everyRecordComponentIsARequiredSchemaPropertyOfTheMatchingType(String name) {
    var schema = read(name + ".schema.json");
    var components = EVENTS.get(name).getRecordComponents();

    var required = new HashSet<String>();
    schema.get("required").forEach(n -> required.add(n.asString()));
    assertThat(required)
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(components).map(RecordComponent::getName).toList());

    for (var component : components) {
      var property = schema.get("properties").get(component.getName());
      assertThat(property).as(component.getName()).isNotNull();
      assertThat(schemaType(property)).as(component.getName()).isEqualTo(javaType(component));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"review-recorded", "item-completed"})
  void theExamplePayloadRoundTripsThroughTheRecord(String name) {
    var example = read(name + ".example.json");
    var event = JSON.treeToValue(example, EVENTS.get(name));
    JsonNode written = JSON.valueToTree(event);
    assertThat(written).isEqualTo(normalised(example));
  }

  @ParameterizedTest
  @ValueSource(strings = {"review-recorded", "item-completed"})
  void unknownFieldsAreIgnoredSoProducersCanAddFields(String name) {
    var example = (ObjectNode) read(name + ".example.json");
    example.put("addedInAFutureVersion", true);
    var lenient =
        JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    assertThat(lenient.treeToValue(example, EVENTS.get(name))).isNotNull();
  }

  private static JsonNode normalised(JsonNode example) {
    return JSON.readTree(JSON.writeValueAsString(example));
  }

  private static String schemaType(JsonNode property) {
    var type = property.get("type").asString();
    var format = property.has("format") ? property.get("format").asString() : "";
    return format.isEmpty() ? type : type + ":" + format;
  }

  private static String javaType(RecordComponent component) {
    Class<?> type = component.getType();
    if (type == UUID.class) return "string:uuid";
    if (type == Instant.class) return "string:date-time";
    if (type == LocalDate.class) return "string:date";
    if (type == String.class) return "string";
    if (type == int.class || type == long.class) return "integer";
    if (type == double.class) return "number";
    throw new AssertionError("unmapped type " + type + " on " + component.getName());
  }

  private static JsonNode read(String file) {
    try (InputStream in = EventSchemaTest.class.getResourceAsStream("/contracts/" + file)) {
      assertThat(in).as(file).isNotNull();
      return JSON.readTree(in);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
