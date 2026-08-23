package io.github.metdaisy.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reports text patterns commonly produced by a Korean encoding mismatch.
 *
 * <p>This check is intentionally heuristic. It reports warnings because
 * CJK/Hangul mixtures and compatibility ideographs can be valid text.</p>
 */
public class SuspiciousKoreanEncodingCheck extends AbstractCheck {

  private static final String MESSAGE_KEY = "suspicious.korean.encoding";
  private static final int CJK_UNIFIED_START = 0x3400;
  private static final int CJK_UNIFIED_END = 0x9FFF;
  private static final int CJK_COMPATIBILITY_START = 0xF900;
  private static final int CJK_COMPATIBILITY_END = 0xFAFF;
  private static final int HANGUL_START = 0xAC00;
  private static final int HANGUL_END = 0xD7AF;
  private static final int REPLACEMENT_CHARACTER = 0xFFFD;

  private String whitelistFile;
  private List<String> whitelist = Collections.emptyList();

  /**
   * Sets the YAML file containing literal fragments that should not produce
   * warnings.
   *
   * @param value YAML whitelist path
   */
  public void setWhitelistFile(String value) {
    whitelistFile = value;
  }

  @Override
  protected void finishLocalSetup() throws CheckstyleException {
    if (whitelistFile == null || whitelistFile.isBlank()) {
      throw new CheckstyleException("whitelistFile must be configured.");
    }

    try {
      whitelist = readWhitelist(Path.of(whitelistFile));
    } catch (IOException | IllegalArgumentException exception) {
      throw new CheckstyleException(
          "Unable to read Checkstyle whitelist file: " + whitelistFile, exception);
    }
  }

  @Override
  public int[] getDefaultTokens() {
    return new int[]{TokenTypes.COMPILATION_UNIT};
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[0];
  }

  @Override
  public void beginTree(DetailAST rootAST) {
    String[] lines = getFileContents().getText().toLinesArray();

    for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      String line = lines[lineIndex];
      int tokenStart = 0;

      while (tokenStart < line.length()) {
        while (tokenStart < line.length()
            && Character.isWhitespace(line.charAt(tokenStart))) {
          tokenStart++;
        }

        if (tokenStart >= line.length()) {
          break;
        }

        int tokenEnd = tokenStart;
        while (tokenEnd < line.length()
            && !Character.isWhitespace(line.charAt(tokenEnd))) {
          tokenEnd++;
        }

        String token = line.substring(tokenStart, tokenEnd);
        if (isSuspicious(token) && !isWhitelisted(token)) {
          log(lineIndex + 1, tokenStart, MESSAGE_KEY, token);
        }

        tokenStart = tokenEnd;
      }
    }
  }

  private boolean isSuspicious(String token) {
    boolean containsCjk = false;
    boolean containsHangul = false;

    for (int offset = 0; offset < token.length();) {
      int codePoint = token.codePointAt(offset);
      offset += Character.charCount(codePoint);

      if (codePoint == REPLACEMENT_CHARACTER) {
        return false;
      }
      if (isCjk(codePoint)) {
        containsCjk = true;
      }
      if (isHangul(codePoint)) {
        containsHangul = true;
      }
    }

    return containsHangul && containsCjk;
  }

  private boolean isWhitelisted(String token) {
    return whitelist.stream().anyMatch(token::contains);
  }

  private List<String> readWhitelist(Path path) throws IOException, CheckstyleException {
    List<String> entries = new ArrayList<>();
    boolean whitelistSectionFound = false;

    for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
      String line = stripComment(rawLine).trim();
      if (line.isEmpty()) {
        continue;
      }

      if (line.equals("whitelist:")) {
        whitelistSectionFound = true;
        continue;
      }

      if (!whitelistSectionFound || !line.startsWith("-")) {
        throw new CheckstyleException(
            "Expected a whitelist YAML list under 'whitelist:' in " + path);
      }

      String entry = parseYamlString(line.substring(1).trim());
      if (!entry.isEmpty()) {
        entries.add(entry);
      }
    }

    if (!whitelistSectionFound) {
      throw new CheckstyleException("Missing 'whitelist:' section in " + path);
    }

    return List.copyOf(entries);
  }

  private String parseYamlString(String value) throws CheckstyleException {
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1)
          .replace("\\\"", "\"")
          .replace("\\\\", "\\");
    }
    if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
      return value.substring(1, value.length() - 1).replace("''", "'");
    }
    if (value.contains(":")) {
      throw new CheckstyleException("Whitelist entries must be YAML strings: " + value);
    }
    return value;
  }

  private String stripComment(String line) {
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;

    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '\'' && !inDoubleQuote) {
        inSingleQuote = !inSingleQuote;
      } else if (character == '"' && !inSingleQuote) {
        inDoubleQuote = !inDoubleQuote;
      } else if (character == '#' && !inSingleQuote && !inDoubleQuote) {
        return line.substring(0, index);
      }
    }

    return line;
  }

  private boolean isCjk(int codePoint) {
    return codePoint >= CJK_UNIFIED_START && codePoint <= CJK_UNIFIED_END
        || codePoint >= CJK_COMPATIBILITY_START && codePoint <= CJK_COMPATIBILITY_END;
  }

  private boolean isHangul(int codePoint) {
    return codePoint >= HANGUL_START && codePoint <= HANGUL_END;
  }

  @Override
  public String getMessageBundle() {
    return "io.github.metdaisy.checkstyle.messages";
  }
}
