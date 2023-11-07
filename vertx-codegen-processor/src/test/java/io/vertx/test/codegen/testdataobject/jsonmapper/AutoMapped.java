package io.vertx.test.codegen.testdataobject.jsonmapper;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public interface AutoMapped {

  static AutoMapped fromJson(JsonObject json) {
    int port = json.getInteger("port", -1);
    String host = json.getString("host");
    return new AutoMapped() {
      @Override
      public int port() {
        return port;
      }
      @Override
      public String host() {
        return host;
      }
    };
  }

  static AutoMapped of(String host, int port) {
    return new AutoMapped() {
      @Override
      public int port() {
        return port;
      }
      @Override
      public String host() {
        return host;
      }
    };
  }

  int port();

  String host();

  default JsonObject toJson() {
    return new JsonObject().put("port", port()).put("host", host());
  }
}
