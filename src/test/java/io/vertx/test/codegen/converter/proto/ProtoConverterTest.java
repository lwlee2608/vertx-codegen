package io.vertx.test.codegen.converter.proto;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import io.vertx.test.codegen.converter.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class ProtoConverterTest {
  @Test
  public void testPrimitiveFields() throws IOException {
    Address address = new Address();
    address.setLatitude(3.333F);
    address.setLongitude(4.444F);
    User user = new User();
    user.setUserName("jviet");
    user.setAge(21);
    user.setAddress(address);
    user.setIntegerListField(Collections.unmodifiableList(Arrays.asList(100, 101)));
    user.setDoubleField(5.5);
    user.setLongField(1000L);
    user.setBoolField(true);
    user.setShortField((short) 10);
    user.setCharField((char) 1);
    Map<String, String> stringValueMap = new HashMap<>();
    stringValueMap.put("key1", "value1");
    stringValueMap.put("key2", "value2");
    user.setStringValueMap(stringValueMap);
    Map<String, Integer> integerValueMap = new HashMap<>();
    integerValueMap.put("key1", 1);
    integerValueMap.put("key2", 2);
    user.setIntegerValueMap(integerValueMap);

    // Encode
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CodedOutputStream output = CodedOutputStream.newInstance(baos);
    UserProtoConverter.toProto(user, output);
    output.flush();
    byte[] encoded = baos.toByteArray();
    // Decode
    User decoded = new User();
    CodedInputStream input = CodedInputStream.newInstance(encoded);
    UserProtoConverter.fromProto(input, decoded);

    // Assert decoded is same with original
    assertEquals(user.getUserName(), decoded.getUserName());
    assertEquals(user.getAge(), decoded.getAge());
    //Assert.assertEquals(user.getAddress(), decoded.getAddress()); // need equal and hash
    assertEquals(user.getAddress().getLatitude(), decoded.getAddress().getLatitude());
    assertEquals(user.getAddress().getLongitude(), decoded.getAddress().getLongitude());
    Assert.assertArrayEquals(user.getIntegerListField().toArray(), decoded.getIntegerListField().toArray());
    assertEquals(user.getDoubleField(), decoded.getDoubleField());
    assertEquals(user.getLongField(), decoded.getLongField());
    assertEquals(user.getBoolField(), decoded.getBoolField());
    assertEquals(user.getShortField(), decoded.getShortField());
    assertEquals(user.getCharField(), decoded.getCharField());
    assertEquals(user.getStringValueMap(), decoded.getStringValueMap());
    assertEquals(user.getIntegerValueMap(), decoded.getIntegerValueMap());

    // Assert total size is equal to computed size
    assertEquals(encoded.length, UserProtoConverter.computeSize(user));
  }

  @Test
  public void testCachedComputeSize() throws IOException {
    int[] cache = new int[11];
    RecursiveItem root = new RecursiveItem("root");
    RecursiveItem a = new RecursiveItem("a");
    RecursiveItem a_a = new RecursiveItem("a_a");
    RecursiveItem a_b = new RecursiveItem("a_b");
    RecursiveItem a_c = new RecursiveItem("a_c");
    RecursiveItem b = new RecursiveItem("b");
    RecursiveItem b_a = new RecursiveItem("b_a");
    RecursiveItem b_b = new RecursiveItem("b_b");
    RecursiveItem c = new RecursiveItem("c");
    RecursiveItem c_b = new RecursiveItem("c_b");
    RecursiveItem c_c = new RecursiveItem("c_c");

    root.setChildA(a);

    a.setChildA(a_a);
    a.setChildB(a_b);
    a.setChildC(a_c);

    root.setChildB(b);
    b.setChildA(b_a);
    b.setChildB(b_b);

    root.setChildC(c);
    c.setChildB(c_b);
    c.setChildC(c_c);
    int val = RecursiveItemProtoConverter.computeSize2(root, cache, 0);
    assertEquals(11, val);
    System.out.println("abc");
  }
}
