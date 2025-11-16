package net.minestom.codegen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record BlockPropertyGenerator(
        String packageName, String blockPropertyClassName,
        InputStream entriesFile,
        Path outputFolder
) implements MinestomCodeGenerator {

    public BlockPropertyGenerator {
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(blockPropertyClassName, "blockPropertyClassName cannot be null");
        Objects.requireNonNull(entriesFile, "entriesFile cannot be null");
        Objects.requireNonNull(outputFolder, "outputFolder cannot be null");
    }

    @Override
    public void generate() {
        ensureDirectory(outputFolder);

        JsonObject root;
        try (var reader = new InputStreamReader(entriesFile, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read block property definitions", e);
        }
        if (root == null) throw new IllegalStateException("Block property definition file is empty");

        ClassName interfaceCN = ClassName.get(packageName, blockPropertyClassName);
        ClassName blockPropertyCN = ClassName.get("net.minestom.server.instance.block", "BlockProperty");
        ClassName blockPropertyImplCN = ClassName.get("net.minestom.server.instance.block", "BlockPropertyImpl");

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceCN)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(generateJavadoc(blockPropertyCN));

        List<PropertyDefinition> definitions = root.entrySet().stream()
                .map(entry -> parseDefinition(entry.getKey(), entry.getValue().getAsJsonObject()))
                .sorted(Comparator.comparing(PropertyDefinition::constantName))
                .toList();

        for (PropertyDefinition definition : definitions) {
            interfaceBuilder.addField(buildField(definition, blockPropertyCN, blockPropertyImplCN, interfaceCN));
        }

        definitions.stream()
                .map(PropertyDefinition::enumDefinition)
                .filter(Objects::nonNull)
                .map(this::buildEnum)
                .forEach(interfaceBuilder::addType);

        writeFiles(JavaFile.builder(packageName, interfaceBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build());
    }

    private FieldSpec buildField(PropertyDefinition definition, ClassName blockPropertyCN,
                                 ClassName blockPropertyImplCN, ClassName interfaceCN) {
        return switch (definition.type()) {
            case BOOLEAN -> FieldSpec.builder(blockPropertyCN.nestedClass("Boolean"), definition.constantName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T.BooleanImpl($S)", blockPropertyImplCN, definition.stateName())
                    .build();
            case INTEGER -> FieldSpec.builder(blockPropertyCN.nestedClass("Integer"), definition.constantName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T.IntImpl($S, $L, $L)", blockPropertyImplCN, definition.stateName(), definition.min(), definition.max())
                    .build();
            case ENUM -> {
                EnumDefinition enumDefinition = definition.enumDefinition();
                if (enumDefinition == null) {
                    throw new IllegalStateException("Enum property without enum definition: " + definition.constantName());
                }
                ClassName enumType = interfaceCN.nestedClass(enumDefinition.typeName());
                ParameterizedTypeName typeName = ParameterizedTypeName.get(blockPropertyCN.nestedClass("Enum"), enumType);
                yield FieldSpec.builder(typeName, definition.constantName(), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T.EnumImpl<>($S, $T.class)", blockPropertyImplCN, definition.stateName(), enumType)
                        .build();
            }
        };
    }

    private TypeSpec buildEnum(EnumDefinition enumDefinition) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumDefinition.typeName())
                .addModifiers(Modifier.PUBLIC)
                .addField(FieldSpec.builder(String.class, "serialized", Modifier.PRIVATE, Modifier.FINAL).build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "serialized")
                        .addStatement("this.serialized = serialized")
                        .build())
                .addMethod(MethodSpec.methodBuilder("serialized")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addStatement("return this.serialized")
                        .build());

        for (EnumValue value : enumDefinition.values()) {
            enumBuilder.addEnumConstant(value.enumName(), TypeSpec.anonymousClassBuilder("$S", value.serializedName()).build());
        }
        return enumBuilder.build();
    }

    private PropertyDefinition parseDefinition(String key, JsonObject definition) {
        String constantName = toConstant(key);
        String stateName = getString(definition, "name");
        PropertyType type = PropertyType.valueOf(getString(definition, "type").toUpperCase(Locale.ROOT));
        return switch (type) {
            case BOOLEAN -> new PropertyDefinition(constantName, stateName, type, 0, 0, null);
            case INTEGER -> new PropertyDefinition(constantName, stateName, type,
                    getInt(definition, "min"), getInt(definition, "max"), null);
            case ENUM -> new PropertyDefinition(constantName, stateName, type, 0, 0, parseEnum(definition));
        };
    }

    private EnumDefinition parseEnum(JsonObject definition) {
        String typeName = getString(definition, "mojangName");
        List<EnumValue> values = new ArrayList<>();
        for (JsonElement element : definition.getAsJsonArray("values")) {
            JsonObject valueObject = element.getAsJsonObject();
            String enumName = getString(valueObject, "enumName");
            String serializedName = getString(valueObject, "serializedName");
            values.add(new EnumValue(toConstant(enumName), serializedName));
        }
        return new EnumDefinition(typeName, values);
    }

    private static String getString(JsonObject object, String name) {
        if (object.has(name)) return object.get(name).getAsString();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().getAsString();
            }
        }
        throw new IllegalStateException("Missing string field '%s'".formatted(name));
    }

    private static int getInt(JsonObject object, String name) {
        if (object.has(name)) return object.get(name).getAsInt();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().getAsInt();
            }
        }
        throw new IllegalStateException("Missing int field '%s'".formatted(name));
    }

    private enum PropertyType {
        BOOLEAN, INTEGER, ENUM
    }

    private record PropertyDefinition(String constantName, String stateName, PropertyType type,
                                      int min, int max, EnumDefinition enumDefinition) {
    }

    private record EnumDefinition(String typeName, List<EnumValue> values) {
    }

    private record EnumValue(String enumName, String serializedName) {
    }
}
