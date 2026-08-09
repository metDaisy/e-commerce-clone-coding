package io.github.metdaisy.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.regex.Pattern;

/**
 * Reports fully-qualified type names used in Java code.
 *
 * <p>Import and package declarations are excluded. A name is considered a type
 * reference when it consists of one or more lower-case package segments and a
 * type segment beginning with an upper-case character. Examples:
 * {@code java.util.UUID} and {@code org.springframework.http.MediaType}.</p>
 */
public class NoFullyQualifiedTypeCheck extends AbstractCheck {

  private static final String MESSAGE_KEY = "fully.qualified.type";
  private static final Pattern TYPE_NAME = Pattern.compile(
      "^(?:[a-z_$][\\w$]*\\.)+[A-Z_$][\\w$]*(?:\\.[A-Z_$][\\w$]*)*$");

  @Override
  public int[] getDefaultTokens() {
    return new int[]{TokenTypes.DOT};
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
  public void visitToken(DetailAST ast) {
    if (isInsideImportOrPackageDeclaration(ast)) {
      return;
    }

    FullIdent fullIdent = FullIdent.createFullIdent(ast);
    String name = fullIdent.getText();

    if (!TYPE_NAME.matcher(name).matches() || hasMatchingQualifiedParent(ast)) {
      return;
    }

    log(ast, MESSAGE_KEY, name);
  }

  @Override
  public String getMessageBundle() {
    return "io.github.metdaisy.checkstyle.messages";
  }

  private boolean isInsideImportOrPackageDeclaration(DetailAST ast) {
    DetailAST current = ast;
    while (current != null) {
      int tokenType = current.getType();
      if (tokenType == TokenTypes.IMPORT
          || tokenType == TokenTypes.STATIC_IMPORT
          || tokenType == TokenTypes.PACKAGE_DEF) {
        return true;
      }
      current = current.getParent();
    }
    return false;
  }

  private boolean hasMatchingQualifiedParent(DetailAST ast) {
    DetailAST parent = ast.getParent();
    if (parent == null || parent.getType() != TokenTypes.DOT) {
      return false;
    }

    String parentName = FullIdent.createFullIdent(parent).getText();
    return TYPE_NAME.matcher(parentName).matches();
  }
}
