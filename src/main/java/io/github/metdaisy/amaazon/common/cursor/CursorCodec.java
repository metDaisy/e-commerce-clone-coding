package io.github.metdaisy.amaazon.common.cursor;

public interface CursorCodec {

  String encode(Object payload);

  <T> T decode(String cursor, Class<T> payloadType);
}
