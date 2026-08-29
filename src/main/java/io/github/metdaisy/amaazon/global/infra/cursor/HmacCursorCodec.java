package io.github.metdaisy.amaazon.global.infra.cursor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.common.cursor.CursorCodec;
import io.github.metdaisy.amaazon.common.cursor.CursorCodecException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HmacCursorCodec implements CursorCodec {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final ObjectMapper objectMapper;
  private final byte[] secretKey;

  public HmacCursorCodec(ObjectMapper objectMapper,
      @Value("${amaazon.cursor.secret-key}") String secretKey) {
    this.objectMapper = objectMapper;
    this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String encode(Object payload) {
    try {
      byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
      return encodeBase64(payloadBytes) + "." + encodeBase64(sign(payloadBytes));
    } catch (JsonProcessingException exception) {
      throw new CursorCodecException("Cursor 생성에 실패했습니다.", exception);
    }
  }

  @Override
  public <T> T decode(String cursor, Class<T> payloadType) {
    try {
      String[] parts = cursor == null ? new String[0] : cursor.split("\\.", -1);
      if (parts.length != 2) {
        throw invalidCursor();
      }
      byte[] payload = decodeBase64(parts[0]);
      byte[] actualSignature = decodeBase64(parts[1]);
      if (!MessageDigest.isEqual(sign(payload), actualSignature)) {
        throw invalidCursor();
      }
      return objectMapper.readValue(payload, payloadType);
    } catch (CursorCodecException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalidCursor();
    }
  }

  private byte[] sign(byte[] payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
      return mac.doFinal(payload);
    } catch (GeneralSecurityException exception) {
      throw new CursorCodecException("Cursor 서명에 실패했습니다.", exception);
    }
  }

  private String encodeBase64(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private byte[] decodeBase64(String value) {
    return Base64.getUrlDecoder().decode(value);
  }

  private CursorCodecException invalidCursor() {
    return new CursorCodecException("Cursor가 유효하지 않습니다.", null);
  }
}
