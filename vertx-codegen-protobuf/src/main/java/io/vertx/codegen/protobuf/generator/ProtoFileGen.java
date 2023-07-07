package io.vertx.codegen.protobuf.generator;

import io.vertx.codegen.DataObjectModel;
import io.vertx.codegen.Generator;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.ModuleGen;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ProtoFileGen extends Generator<DataObjectModel> {

  public ProtoFileGen() {
    name = "protobuf";
    kinds = Collections.singleton("dataObject");
    incremental = true;
  }

  @Override
  public Collection<Class<? extends Annotation>> annotations() {
    return Arrays.asList(DataObject.class, ModuleGen.class);
  }

  @Override
  public String filename(DataObjectModel model) {
    return "resources/dataobjects.proto";
  }

  @Override
  public String render(DataObjectModel model, int index, int size, Map<String, Object> session) {
    return "Generated " + model.getFqn() + "\n";
  }
}
