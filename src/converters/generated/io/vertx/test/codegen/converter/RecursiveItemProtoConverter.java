package io.vertx.test.codegen.converter;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.CodedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class RecursiveItemProtoConverter {

  public static void fromProto(CodedInputStream input, RecursiveItem obj) throws IOException {
    int tag;
    while ((tag = input.readTag()) != 0) {
      switch (tag) {
        case 10: {
          int length = input.readUInt32();
          int oldLimit = input.pushLimit(length);
          RecursiveItem tmp_ = new RecursiveItem();
          RecursiveItemProtoConverter.fromProto(input, tmp_);
          obj.setChildA(tmp_);
          input.popLimit(oldLimit);
          break;
        }
        case 18: {
          int length = input.readUInt32();
          int oldLimit = input.pushLimit(length);
          RecursiveItem tmp_ = new RecursiveItem();
          RecursiveItemProtoConverter.fromProto(input, tmp_);
          obj.setChildB(tmp_);
          input.popLimit(oldLimit);
          break;
        }
        case 26: {
          int length = input.readUInt32();
          int oldLimit = input.pushLimit(length);
          RecursiveItem tmp_ = new RecursiveItem();
          RecursiveItemProtoConverter.fromProto(input, tmp_);
          obj.setChildC(tmp_);
          input.popLimit(oldLimit);
          break;
        }
        case 34: {
          obj.setId(input.readString());
          break;
        }
      }
    }
  }

  public static void toProto(RecursiveItem obj, CodedOutputStream output) throws IOException {
    if (obj.getChildA() != null) {
      output.writeUInt32NoTag(10);
      output.writeUInt32NoTag(RecursiveItemProtoConverter.computeSize(obj.getChildA()));
      RecursiveItemProtoConverter.toProto(obj.getChildA(), output);
    }
    if (obj.getChildB() != null) {
      output.writeUInt32NoTag(18);
      output.writeUInt32NoTag(RecursiveItemProtoConverter.computeSize(obj.getChildB()));
      RecursiveItemProtoConverter.toProto(obj.getChildB(), output);
    }
    if (obj.getChildC() != null) {
      output.writeUInt32NoTag(26);
      output.writeUInt32NoTag(RecursiveItemProtoConverter.computeSize(obj.getChildC()));
      RecursiveItemProtoConverter.toProto(obj.getChildC(), output);
    }
    if (obj.getId() != null) {
      output.writeString(4, obj.getId());
    }
  }

  public static int computeSize(RecursiveItem obj) {
    int size = 0;
    if (obj.getChildA() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(10);
      int dataSize = RecursiveItemProtoConverter.computeSize(obj.getChildA());
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getChildB() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(18);
      int dataSize = RecursiveItemProtoConverter.computeSize(obj.getChildB());
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getChildC() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(26);
      int dataSize = RecursiveItemProtoConverter.computeSize(obj.getChildC());
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getId() != null) {
      size += CodedOutputStream.computeStringSize(4, obj.getId());
    }
    return size;
  }

  public static int computeSize2(RecursiveItem obj, int[] cache, final int baseIndex) {
    int size = 0;
    int index = baseIndex + 1;
    if (obj.getChildA() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(10);
      int savedIndex = index;
      index = RecursiveItemProtoConverter.computeSize2(obj.getChildA(), cache, index);
      int dataSize = cache[savedIndex];
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getChildB() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(18);
      int savedIndex = index;
      index = RecursiveItemProtoConverter.computeSize2(obj.getChildB(), cache, index);
      int dataSize = cache[savedIndex];
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getChildC() != null) {
      size += CodedOutputStream.computeUInt32SizeNoTag(26);
      int savedIndex = index;
      index = RecursiveItemProtoConverter.computeSize2(obj.getChildC(), cache, index);
      int dataSize = cache[savedIndex];
      size += CodedOutputStream.computeUInt32SizeNoTag(dataSize);
      size += dataSize;
    }
    if (obj.getId() != null) {
      size += CodedOutputStream.computeStringSize(4, obj.getId());
    }
    cache[baseIndex] = size;
    return index;
  }

}
