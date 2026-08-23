package io.github.metdaisy.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Reports Unicode replacement characters, which usually mean that source
 * text was decoded with the wrong character encoding.
 */
public class NoReplacementCharacterCheck extends AbstractCheck {

  private static final String MESSAGE_KEY = "replacement.character";
  private static final char REPLACEMENT_CHARACTER = '\uFFFD';

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
      int column = line.indexOf(REPLACEMENT_CHARACTER);

      while (column >= 0) {
        log(lineIndex + 1, column, MESSAGE_KEY);
        column = line.indexOf(REPLACEMENT_CHARACTER, column + 1);
      }
    }
  }

  @Override
  public String getMessageBundle() {
    return "io.github.metdaisy.checkstyle.messages";
  }
}
