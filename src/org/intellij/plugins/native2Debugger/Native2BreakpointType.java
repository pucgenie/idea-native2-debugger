// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.intellij.plugins.native2Debugger.impl.Native2DebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;

public class Native2BreakpointType extends XLineBreakpointType<XBreakpointProperties> {
  private final Native2DebuggerEditorsProvider myMyEditorsProvider1 = new Native2DebuggerEditorsProvider();

  public Native2BreakpointType() {
    super("native2", Native2DebuggerBundle.message("title.native2.breakpoints"));
  }

  @Override
  public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
    final Document document = FileDocumentManager.getInstance().getDocument(file);
    if (document == null) return false;

    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (psiFile == null) {
      return false;
    }
    //final FileType fileType = psiFile.getFileType();
    return true;
  }

  @Override
  public XDebuggerEditorsProvider getEditorsProvider(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, @NotNull Project project) {
    final XSourcePosition position = breakpoint.getSourcePosition();
    if (position == null) {
      return null;
    }

    final PsiFile file = PsiManager.getInstance(project).findFile(position.getFile());
    if (file == null) {
      return null;
    }

    return myMyEditorsProvider1;
  }

  @Override
  public XBreakpointProperties createBreakpointProperties(@NotNull VirtualFile file, int line) {
    return null;
  }
}
