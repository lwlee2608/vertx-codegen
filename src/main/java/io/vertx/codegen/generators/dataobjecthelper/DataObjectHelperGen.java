package io.vertx.codegen.generators.dataobjecthelper;

import io.vertx.codegen.DataObjectModel;
import io.vertx.codegen.Generator;
import io.vertx.codegen.PropertyInfo;
import io.vertx.codegen.PropertyKind;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.format.CamelCase;
import io.vertx.codegen.format.Case;
import io.vertx.codegen.format.KebabCase;
import io.vertx.codegen.format.LowerCamelCase;
import io.vertx.codegen.format.QualifiedCase;
import io.vertx.codegen.format.SnakeCase;
import io.vertx.codegen.type.AnnotationValueInfo;
import io.vertx.codegen.type.ClassKind;
import io.vertx.codegen.type.ClassTypeInfo;
import io.vertx.codegen.type.DataObjectInfo;
import io.vertx.codegen.type.MapperInfo;
import io.vertx.codegen.type.TypeInfo;
import io.vertx.codegen.writer.CodeWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DataObjectHelperGen extends Generator<DataObjectModel> {

  private Case formatter;

  public DataObjectHelperGen() {
    kinds = Collections.singleton("dataObject");
    name = "data_object_converters";
  }

  @Override
  public Collection<Class<? extends Annotation>> annotations() {
    return Collections.singletonList(DataObject.class);
  }

  @Override
  public String filename(DataObjectModel model) {
    if (model.isClass() && model.getGenerateConverter()) {
      if (model.getProtoConverter()) {
        return model.getFqn() + "ProtoConverter.java";
      } else {
        return model.getFqn() + "Converter.java";
      }
    }
    return null;
  }

  @Override
  public String render(DataObjectModel model, int index, int size, Map<String, Object> session) {
    if (model.getProtoConverter()) {
      return renderProto(model, index, size, session);
    } else {
      return renderJson(model, index, size, session);
    }
  }

  public String renderProto(DataObjectModel model, int index, int size, Map<String, Object> session) {
    formatter = getCase(model);

    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    CodeWriter code = new CodeWriter(writer);
    String visibility = model.isPublicConverter() ? "public" : "";
    boolean inheritConverter = model.getInheritConverter();

    writer.print("package " + model.getType().getPackageName() + ";\n");
    writer.print("\n");
    writer.print("import com.google.protobuf.CodedOutputStream;\n");
    writer.print("import com.google.protobuf.CodedInputStream;\n");
    writer.print("import java.io.IOException;\n");
    writer.print("import java.util.ArrayList;\n");
    writer.print("import java.util.List;\n");
    writer.print("\n");
    code
      .codeln("public class " + model.getType().getSimpleName() + "ProtoConverter {"
      ).newLine();

    String simpleName = model.getType().getSimpleName();

    // fromProto()
    {
      writer.print("  " + visibility + " static void fromProto(CodedInputStream input, " + simpleName + " obj) throws IOException {\n");
      writer.print("    int tag;\n");
      writer.print("    while ((tag = input.readTag()) != 0) {\n");
      writer.print("      switch (tag) {\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();

        String protoType;
        int wireType;
        if (propKind.basic) {
          protoType = getProtoDataType(prop.getType().getName());
          switch (protoType) {
            case "Bool":
            case "Int64":
            case "UInt64":
            case "Int32":
            case "UInt32":
              wireType = 0;
              break;
            case "Double":
              wireType = 1;
              break;
            case "String":
              wireType = 2;
              break;
            case "Float":
              wireType = 5;
              break;
            default:
              throw new UnsupportedOperationException("Unsupported proto-type " + protoType);
          }
        } else {
          protoType = prop.getType().getSimpleName();
          wireType = 2;
        }

        // Override wire type if property is a list
        if (prop.getKind() == PropertyKind.LIST) {
          wireType = 2;
        }

        int tag = (fieldNumber << 3) | wireType;

        writer.print("        case " + tag + ": {\n");
        if (prop.getKind() == PropertyKind.LIST) {
          writer.print("          int length = input.readRawVarint32();\n");
          writer.print("          int limit = input.pushLimit(length);\n");
          writer.print("          List<Integer> list = new ArrayList<>();\n");
          writer.print("          while (input.getBytesUntilLimit() > 0) {\n");
          writer.print("            list.add(input.read" + protoType + "());\n");
          writer.print("          }\n");
          writer.print("          obj." +  prop.getSetterMethod() + "(list);\n");
          writer.print("          input.popLimit(limit);\n");
          writer.print("          break;\n");
        } else {
          if (propKind.basic) {
            writer.print("          obj." + prop.getSetterMethod() + "(input.read" + protoType + "());\n");
          } else {
            String dataObjectName = prop.getType().getSimpleName();
            writer.print("          int length = input.readUInt32();\n");
            writer.print("          int oldLimit = input.pushLimit(length);\n");
            writer.print("          " + dataObjectName + " address = new " + dataObjectName + "();\n");
            writer.print("          AddressProtoConverter.fromProto(input, address);\n");
            writer.print("          obj." + prop.getSetterMethod() + "(address);\n");
            writer.print("          input.popLimit(oldLimit);\n");
          }
          writer.print("          break;\n");
        }
        writer.print("        }\n");
        fieldNumber++;
      }
      writer.print("      }\n");
      writer.print("    }\n");
      writer.print("  }\n");
      writer.print("\n");
    }

    // toProto()
    {
      writer.print("  " + visibility + " static void toProto(" + simpleName + " obj, CodedOutputStream output) throws IOException {\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();
        writer.print("    if (obj." + prop.getGetterMethod() + "() != null) {\n");
        if (prop.getKind() == PropertyKind.LIST) {
          String protoType = getProtoDataType(prop.getType().getName());
          writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
          writer.print("        output.writeUInt32NoTag(26);\n"); // TODO calculate tag value
          writer.print("        int dataSize = 0;\n");
          writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
          writer.print("          dataSize += CodedOutputStream.computeInt32SizeNoTag(element);\n");
          writer.print("        }\n");
          writer.print("        output.writeUInt32NoTag(dataSize);\n");
          writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
          writer.print("          output.write" + protoType + "NoTag(element);\n");
          writer.print("        }\n");
          writer.print("      }\n");
        } else {
          if (propKind.basic) {
            String protoType = getProtoDataType(prop.getType().getName());
            writer.print("      output.write" + protoType + "(" + fieldNumber + ", obj." + prop.getGetterMethod() + "());\n");
          } else {
            String dataObjectName = prop.getType().getSimpleName();
            writer.print("      output.writeTag(" + fieldNumber + ", 2);\n");
            writer.print("      output.writeUInt32NoTag(" + dataObjectName + "ProtoConverter.computeSize(obj." + prop.getGetterMethod() + "()));\n");
            writer.print("      " + dataObjectName + "ProtoConverter.toProto(obj." + prop.getGetterMethod() + "(), output);\n");
          }
        }
        writer.print("    }\n");
        fieldNumber++;
      }
      writer.print("  }\n");
      writer.print("\n");
    }

    // Compute Size
    {
      writer.print("  " + visibility + " static int computeSize(" + simpleName + " obj) {\n");
      writer.print("    int size = 0;\n");
      int fieldNumber = 1;
      for (PropertyInfo prop : model.getPropertyMap().values()) {
        ClassKind propKind = prop.getType().getKind();
        writer.print("    if (obj." + prop.getGetterMethod() + "() != null) {\n");
        if (prop.getKind() == PropertyKind.LIST) {
          String protoType = getProtoDataType(prop.getType().getName());
          writer.print("      if (obj." + prop.getGetterMethod() + "().size() > 0) {\n");
          writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(26);\n"); // TODO calculate tag value
          writer.print("        int dataSize = 0;\n");
          writer.print("        for (Integer element: obj." + prop.getGetterMethod() + "()) {\n");
          writer.print("          dataSize += CodedOutputStream.compute" + protoType + "SizeNoTag(element);\n");
          writer.print("        }\n");
          writer.print("        size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
          writer.print("        size += dataSize;\n");
          writer.print("      }\n");
        } else {
          if (propKind.basic) {
            String protoType = getProtoDataType(prop.getType().getName());
            writer.print("      size += CodedOutputStream.compute" + protoType + "Size(" + fieldNumber + ", obj." + prop.getGetterMethod() + "());\n");
          } else {
            String dataObjectName = prop.getType().getSimpleName();
            writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(10);\n"); // TODO calculate tag value
            writer.print("      int dataSize = " + dataObjectName + "ProtoConverter.computeSize(obj." + prop.getGetterMethod() + "());\n");
            writer.print("      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);\n");
            writer.print("      size += dataSize;\n");
          }
        }
        writer.print("    }\n");
        fieldNumber++;
      }
      writer.print("    return size;\n");
      writer.print("  }\n");
      writer.print("\n");
      writer.print("}\n");
    }

    return buffer.toString();
  }

  private String getProtoDataType(String dataType) {
    String protoType;
    if ("java.lang.Integer".equals(dataType)) {
      protoType = "Int32";
    } else if ("java.lang.String".equals(dataType)) {
      protoType = "String";
    } else if ("java.lang.Float".equals(dataType)) {
      protoType = "Float";
    } else {
      // TODO Support more data type
      throw new UnsupportedOperationException("Unsupported data-type " + dataType);
    }
    return  protoType;
  }

  public String renderJson(DataObjectModel model, int index, int size, Map<String, Object> session) {
    formatter = getCase(model);

    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    CodeWriter code = new CodeWriter(writer);
    String visibility = model.isPublicConverter() ? "public" : "";
    boolean inheritConverter = model.getInheritConverter();

    writer.print("package " + model.getType().getPackageName() + ";\n");
    writer.print("\n");
    writer.print("import io.vertx.core.json.JsonObject;\n");
    writer.print("import io.vertx.core.json.JsonArray;\n");
    writer.print("import io.vertx.core.json.impl.JsonUtil;\n");
    writer.print("import java.time.Instant;\n");
    writer.print("import java.time.format.DateTimeFormatter;\n");
    writer.print("import java.util.Base64;\n");
    writer.print("\n");
    writer.print("/**\n");
    writer.print(" * Converter and mapper for {@link " + model.getType() + "}.\n");
    writer.print(" * NOTE: This class has been automatically generated from the {@link " + model.getType() + "} original class using Vert.x codegen.\n");
    writer.print(" */\n");
    code
      .codeln("public class " + model.getType().getSimpleName() + "Converter {"
      ).newLine();
    if (model.getGenerateConverter()) {
      writer.print("\n");
      switch (model.getBase64Type()) {
        case "basic":
          writer.print(
            "  private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();\n" +
            "  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();\n");
          break;
        case "base64url":
          writer.print(
            "  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();\n" +
            "  private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();\n");
          break;
        default:
          writer.print(
            "  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;\n" +
            "  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;\n");
          break;
      }
      writer.print("\n");

      genFromJson(visibility, inheritConverter, model, writer);
      writer.print("\n");
      genToJson(visibility, inheritConverter, model, writer);
    }
    writer.print("}\n");
    return buffer.toString();
  }

  private void genToJson(String visibility, boolean inheritConverter, DataObjectModel model, PrintWriter writer) {
    String simpleName = model.getType().getSimpleName();
    writer.print("  " + visibility + " static void toJson(" + simpleName + " obj, JsonObject json) {\n");
    writer.print("    toJson(obj, json.getMap());\n");
    writer.print("  }\n");
    writer.print("\n");
    writer.print("  " + visibility + " static void toJson(" + simpleName + " obj, java.util.Map<String, Object> json) {\n");
    model.getPropertyMap().values().forEach(prop -> {
      if ((prop.isDeclared() || inheritConverter) && prop.getGetterMethod() != null && prop.isJsonifiable()) {
        ClassKind propKind = prop.getType().getKind();
        if (propKind.basic) {
          if (propKind == ClassKind.STRING) {
            genPropToJson("", "", prop, writer);
          } else {
            switch (prop.getType().getSimpleName()) {
              case "char":
              case "Character":
                genPropToJson("Character.toString(", ")", prop, writer);
                break;
              default:
                genPropToJson("", "", prop, writer);
            }
          }
        } else {
          DataObjectInfo dataObject = prop.getType().getDataObject();
          if (dataObject != null) {
            if (dataObject.isSerializable()) {
              String m;
              MapperInfo mapperInfo = dataObject.getSerializer();
              String match;
              switch (mapperInfo.getKind()) {
                case SELF:
                  m = "";
                  match = "." + String.join(".", mapperInfo.getSelectors()) + "()";
                  break;
                case STATIC_METHOD:
                  m = mapperInfo.getQualifiedName() + "." + String.join(".", mapperInfo.getSelectors()) + "(";
                  match = ")";
                  break;
                default:
                  throw new UnsupportedOperationException();
              }
              genPropToJson(m, match, prop, writer);
            } else {
              return;
            }
          } else {
            switch (propKind) {
              case API:
                if (prop.getType().getName().equals("io.vertx.core.buffer.Buffer")) {
                  genPropToJson("BASE64_ENCODER.encodeToString(", ".getBytes())", prop, writer);
                }
                break;
              case ENUM:
                genPropToJson("", ".name()", prop, writer);
                break;
              case JSON_OBJECT:
              case JSON_ARRAY:
              case OBJECT:
                genPropToJson("", "", prop, writer);
                break;
              case OTHER:
                if (prop.getType().getName().equals(Instant.class.getName())) {
                  genPropToJson("DateTimeFormatter.ISO_INSTANT.format(", ")", prop, writer);
                }
                break;
            }
          }
        }
      }
    });

    writer.print("  }\n");
  }

  private void genPropToJson(String before, String after, PropertyInfo prop, PrintWriter writer) {
    String jsonPropertyName = LowerCamelCase.INSTANCE.to(formatter, prop.getName());
    String indent = "    ";
    if (prop.isList() || prop.isSet()) {
      writer.print(indent + "if (obj." + prop.getGetterMethod() + "() != null) {\n");
      writer.print(indent + "  JsonArray array = new JsonArray();\n");
      writer.print(indent + "  obj." + prop.getGetterMethod() + "().forEach(item -> array.add(" + before + "item" + after + "));\n");
      writer.print(indent + "  json.put(\"" + jsonPropertyName + "\", array);\n");
      writer.print(indent + "}\n");
    } else if (prop.isMap()) {
      writer.print(indent + "if (obj." + prop.getGetterMethod() + "() != null) {\n");
      writer.print(indent + "  JsonObject map = new JsonObject();\n");
      writer.print(indent + "  obj." + prop.getGetterMethod() + "().forEach((key, value) -> map.put(key, " + before + "value" + after + "));\n");
      writer.print(indent + "  json.put(\"" + jsonPropertyName + "\", map);\n");
      writer.print(indent + "}\n");
    } else {
      String sp = "";
      if (prop.getType().getKind() != ClassKind.PRIMITIVE) {
        sp = "  ";
        writer.print(indent + "if (obj." + prop.getGetterMethod() + "() != null) {\n");
      }
      writer.print(indent + sp + "json.put(\"" + jsonPropertyName + "\", " + before + "obj." + prop.getGetterMethod() + "()" + after + ");\n");
      if (prop.getType().getKind() != ClassKind.PRIMITIVE) {
        writer.print(indent + "}\n");
      }
    }
  }

  private void genFromJson(String visibility, boolean inheritConverter, DataObjectModel model, PrintWriter writer) {
    writer.print("  " + visibility + " static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, " + model.getType().getSimpleName() + " obj) {\n");
    writer.print("    for (java.util.Map.Entry<String, Object> member : json) {\n");
    writer.print("      switch (member.getKey()) {\n");
    model.getPropertyMap().values().forEach(prop -> {
      if (prop.isDeclared() || inheritConverter) {
        ClassKind propKind = prop.getType().getKind();
        if (propKind.basic) {
          if (propKind == ClassKind.STRING) {
            genPropFromJson("String", "(String)", "", prop, writer);
          } else {
            switch (prop.getType().getSimpleName()) {
              case "boolean":
              case "Boolean":
                genPropFromJson("Boolean", "(Boolean)", "", prop, writer);
                break;
              case "byte":
              case "Byte":
                genPropFromJson("Number", "((Number)", ").byteValue()", prop, writer);
                break;
              case "short":
              case "Short":
                genPropFromJson("Number", "((Number)", ").shortValue()", prop, writer);
                break;
              case "int":
              case "Integer":
                genPropFromJson("Number", "((Number)", ").intValue()", prop, writer);
                break;
              case "long":
              case "Long":
                genPropFromJson("Number", "((Number)", ").longValue()", prop, writer);
                break;
              case "float":
              case "Float":
                genPropFromJson("Number", "((Number)", ").floatValue()", prop, writer);
                break;
              case "double":
              case "Double":
                genPropFromJson("Number", "((Number)", ").doubleValue()", prop, writer);
                break;
              case "char":
              case "Character":
                genPropFromJson("String", "((String)", ").charAt(0)", prop, writer);
                break;
            }
          }
        } else {
          TypeInfo type = prop.getType();
          DataObjectInfo dataObject = type.getDataObject();
          if (dataObject != null) {
            if (dataObject.isDeserializable()) {
              String simpleName;
              String match;
              MapperInfo mapper = dataObject.getDeserializer();
              TypeInfo jsonType = mapper.getJsonType();
              switch (mapper.getKind()) {
                case SELF:
                  match = "new " + type.getName() + "((" + mapper.getJsonType().getName() + ")";
                  simpleName = jsonType.getSimpleName();
                  break;
                case STATIC_METHOD:
                  match = mapper.getQualifiedName() + "." + String.join(".", mapper.getSelectors()) + "((" + jsonType.getSimpleName() + ")";
                  simpleName = jsonType.getSimpleName();
                  break;
                default:
                  throw new AssertionError();
              }
              genPropFromJson(
                simpleName,
                match,
                ")",
                prop,
                writer
              );

            }
          } else {
            switch (propKind) {
              case API:
                if (prop.getType().getName().equals("io.vertx.core.buffer.Buffer")) {
                  genPropFromJson("String", "io.vertx.core.buffer.Buffer.buffer(BASE64_DECODER.decode((String)", "))", prop, writer);
                }
                break;
              case JSON_OBJECT:
                genPropFromJson("JsonObject", "((JsonObject)", ").copy()", prop, writer);
                break;
              case JSON_ARRAY:
                genPropFromJson("JsonArray", "((JsonArray)", ").copy()", prop, writer);
                break;
              case ENUM:
                genPropFromJson("String", prop.getType().getName() + ".valueOf((String)", ")", prop, writer);
                break;
              case OBJECT:
                genPropFromJson("Object", "", "", prop, writer);
                break;
              case OTHER:
                if (prop.getType().getName().equals(Instant.class.getName())) {
                  genPropFromJson("String", "Instant.from(DateTimeFormatter.ISO_INSTANT.parse((String)", "))", prop, writer);
                }
                break;
              default:
            }
          }
        }
      }
    });
    writer.print("      }\n");
    writer.print("    }\n");
    writer.print("  }\n");
  }

  private void genPropFromJson(String cast, String before, String after, PropertyInfo prop, PrintWriter writer) {
    String jsonPropertyName = LowerCamelCase.INSTANCE.to(formatter, prop.getName());
    String indent = "        ";
    writer.print(indent + "case \"" + jsonPropertyName + "\":\n");
    if (prop.isList() || prop.isSet()) {
      writer.print(indent + "  if (member.getValue() instanceof JsonArray) {\n");
      if (prop.isSetter()) {
        String coll = prop.isList() ? "java.util.ArrayList" : "java.util.LinkedHashSet";
        writer.print(indent + "    " + coll + "<" + prop.getType().getName() + "> list =  new " + coll + "<>();\n");
        writer.print(indent + "    ((Iterable<Object>)member.getValue()).forEach( item -> {\n");
        writer.print(indent + "      if (item instanceof " + cast + ")\n");
        writer.print(indent + "        list.add(" + before + "item" + after + ");\n");
        writer.print(indent + "    });\n");
        writer.print(indent + "    obj." + prop.getSetterMethod() + "(list);\n");
      } else if (prop.isAdder()) {
        writer.print(indent + "    ((Iterable<Object>)member.getValue()).forEach( item -> {\n");
        writer.print(indent + "      if (item instanceof " + cast + ")\n");
        writer.print(indent + "        obj." + prop.getAdderMethod() + "(" + before + "item" + after + ");\n");
        writer.print(indent + "    });\n");
      }
      writer.print(indent + "  }\n");
    } else if (prop.isMap()) {
      writer.print(indent + "  if (member.getValue() instanceof JsonObject) {\n");
      if (prop.isAdder()) {
        writer.print(indent + "    ((Iterable<java.util.Map.Entry<String, Object>>)member.getValue()).forEach(entry -> {\n");
        writer.print(indent + "      if (entry.getValue() instanceof " + cast + ")\n");
        writer.print(indent + "        obj." + prop.getAdderMethod() + "(entry.getKey(), " + before + "entry.getValue()" + after + ");\n");
        writer.print(indent + "    });\n");
      } else if (prop.isSetter()) {
        writer.print(indent + "    java.util.Map<String, " + prop.getType().getName() + "> map = new java.util.LinkedHashMap<>();\n");
        writer.print(indent + "    ((Iterable<java.util.Map.Entry<String, Object>>)member.getValue()).forEach(entry -> {\n");
        writer.print(indent + "      if (entry.getValue() instanceof " + cast + ")\n");
        writer.print(indent + "        map.put(entry.getKey(), " + before + "entry.getValue()" + after + ");\n");
        writer.print(indent + "    });\n");
        writer.print(indent + "    obj." + prop.getSetterMethod() + "(map);\n");
      }
      writer.print(indent + "  }\n");
    } else {
      if (prop.isSetter()) {
        writer.print(indent + "  if (member.getValue() instanceof " + cast + ") {\n");
        writer.print(indent + "    obj." + prop.getSetterMethod()+ "(" + before + "member.getValue()" + after + ");\n");
        writer.print(indent + "  }\n");
      }
    }
    writer.print(indent + "  break;\n");
  }

  private Case getCase(DataObjectModel model) {
    AnnotationValueInfo abc = model
      .getAnnotations()
      .stream().filter(ann -> ann.getName().equals(DataObject.class.getName()))
      .findFirst().get();
    ClassTypeInfo cti = (ClassTypeInfo) abc.getMember("jsonPropertyNameFormatter");
    switch (cti.getName()) {
      case "io.vertx.codegen.format.CamelCase":
        return CamelCase.INSTANCE;
      case "io.vertx.codegen.format.SnakeCase":
        return SnakeCase.INSTANCE;
      case "io.vertx.codegen.format.LowerCamelCase":
        return LowerCamelCase.INSTANCE;
      case "io.vertx.codegen.format.KebabCase":
        return KebabCase.INSTANCE;
      case "io.vertx.codegen.format.QualifiedCase":
        return QualifiedCase.INSTANCE;
      default:
        throw new UnsupportedOperationException("Todo");
    }
  }
}
