package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.LocalTimeCounter;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// FIXME:

public class Native2DebuggerEditorsProvider extends XDebuggerEditorsProvider {

  public Native2DebuggerEditorsProvider() {
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return PlainTextFileType.INSTANCE;
  }

  @NotNull
  @Override
  public Document createDocument(@NotNull Project project,
                                 @NotNull String text,
                                 @Nullable XSourcePosition sourcePosition,
                                 @NotNull EvaluationMode mode) {
    final PsiFile psiFile = PsiFileFactory.getInstance(project)
      .createFileFromText("XPathExpr." + getFileType().getDefaultExtension(), getFileType(), text, LocalTimeCounter.currentTime(), true);

//    if (sourcePosition instanceof Native2SourcePosition && ((Native2SourcePosition)sourcePosition).getLocation() instanceof Debugger.StyleFrame) {
//      final Debugger.Locatable location = ((Native2SourcePosition)sourcePosition).getLocation();
//      final EvalContextProvider context = new EvalContextProvider(((Debugger.StyleFrame)location).getVariables());
//      context.attachTo(psiFile);
//    } else {
//      final PsiElement contextElement = Native2BreakpointHandler.findContextElement(project, sourcePosition);
//      if (contextElement != null) {
//        final BreakpointContext context = new BreakpointContext(contextElement);
//        context.attachTo(psiFile);
//      }
//    }

    final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
    assert document != null;
    return document;
  }
}
