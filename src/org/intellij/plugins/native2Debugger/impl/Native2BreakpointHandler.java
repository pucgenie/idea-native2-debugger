// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.intellij.plugins.native2Debugger.Native2BreakpointType;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.VMPausedException;
import org.intellij.plugins.native2Debugger.rt.engine.Breakpoint;
import org.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import org.intellij.plugins.native2Debugger.rt.engine.DebuggerStoppedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Native2BreakpointHandler extends XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> {
  private final Native2DebugProcess myNative2DebugProcess;

  public Native2BreakpointHandler(Native2DebugProcess native2DebugProcess, final Class<? extends Native2BreakpointType> typeClass) {
    super(typeClass);
    myNative2DebugProcess = native2DebugProcess;
  }

  @Override
  public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
    System.err.println("REGISTER BREAKPOINT");
    final XSourcePosition sourcePosition = breakpoint.getSourcePosition();
    if (sourcePosition == null || !sourcePosition.getFile().exists() || !sourcePosition.getFile().isValid()) {
      // ???
      return;
    }
    System.err.println("REGISTER BREAKPOINT 0");

    final VirtualFile file = sourcePosition.getFile();
    final Project project = myNative2DebugProcess.getSession().getProject();
    final String fileURL = getFileURL(file);
    final int lineNumber = getActualLineNumber(breakpoint, project);
    System.err.println("REGISTER BREAKPOINT 7"); // ok
    if (lineNumber == -1) {
      myNative2DebugProcess.getSession().setBreakpointInvalid(breakpoint, "Unsupported breakpoint position");
      return;
    }
    System.err.println("REGISTER BREAKPOINT 2");

    myNative2DebugProcess.send("-break-insert", new String[] { myNative2DebugProcess.fileLineReference(sourcePosition) }, new String[0]);
    try {
      final BreakpointManager manager = myNative2DebugProcess.getBreakpointManager();
      Breakpoint bp;
      if ((bp = manager.getBreakpoint(fileURL, lineNumber)) != null) {
        bp.setEnabled(true);
      } else {
        manager.setBreakpoint(fileURL, lineNumber);
      }
      System.err.println("REGISTER BREAKPOINT 4");
    } catch (DebuggerStoppedException ignore) {
    } catch (VMPausedException e) {
      final XDebugSession session = myNative2DebugProcess.getSession();
      session.reportMessage(Native2DebuggerBundle.message("notification.content.target.vm.not.responding.breakpoint.can.not.be.set"), MessageType.ERROR);
      session.setBreakpointInvalid(breakpoint, "Target VM is not responding. Breakpoint can not be set");
    }
    System.err.println("REGISTER BREAKPOINT 3");
  }

  public static String getFileURL(VirtualFile file) {
    return VfsUtil.virtualToIoFile(file).toURI().toASCIIString();
  }

  @Override
  public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, final boolean temporary) {
    final XSourcePosition sourcePosition = breakpoint.getSourcePosition();
    if (sourcePosition == null || !sourcePosition.getFile().exists() || !sourcePosition.getFile().isValid()) {
      // ???
      return;
    }

    final VirtualFile file = sourcePosition.getFile();
    final Project project = myNative2DebugProcess.getSession().getProject();
    final String fileURL = getFileURL(file);
    final int lineNumber = getActualLineNumber(breakpoint, project);

    try {
      final BreakpointManager manager = myNative2DebugProcess.getBreakpointManager();
      if (temporary) {
        final Breakpoint bp = manager.getBreakpoint(fileURL, lineNumber);
        if (bp != null) {
          bp.setEnabled(false);
        }
      } else {
        manager.removeBreakpoint(fileURL, lineNumber);
      }
    } catch (DebuggerStoppedException ignore) {
    } catch (VMPausedException e) {
      myNative2DebugProcess.getSession().reportMessage(
        Native2DebuggerBundle.message("notification.content.target.vm.not.responding.breakpoint.can.not.be.removed"), MessageType.ERROR);
    }
  }

  public static int getActualLineNumber(XLineBreakpoint breakpoint, Project project) {
    return getActualLineNumber(project, breakpoint.getSourcePosition());
  }

  public static int getActualLineNumber(Project project, @Nullable XSourcePosition position) {
    if (position == null) {
      return -1;
    }
    return position.getLine();
//    final PsiElement element = findContextElement(project, position);
//    if (element == null) {
//      return -1;
//    }
//
//    if (element instanceof XmlToken) {
//      final IElementType tokenType = ((XmlToken)element).getTokenType();
//      if (tokenType == XmlTokenType.XML_START_TAG_START || tokenType == XmlTokenType.XML_NAME) {
//        final PsiManager psiManager = PsiManager.getInstance(project);
//        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
//        final PsiFile psiFile = psiManager.findFile(position.getFile());
//        if (psiFile == null) {
//          return -1;
//        }
//
//        final Document document = documentManager.getDocument(psiFile);
//        if (document == null) {
//          return -1;
//        }
//
//        if (document.getLineNumber(element.getTextRange().getStartOffset()) == position.getLine()) {
//          final XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
//          if (tag != null) {
//            final ASTNode node = tag.getNode();
//            assert node != null;
//            // TODO: re-check if/when Xalan is supported
//            final ASTNode end = XmlChildRole.START_TAG_END_FINDER.findChild(node);
//            if (end != null) {
//              return document.getLineNumber(end.getTextRange().getEndOffset()) + 1;
//            } else {
//              final ASTNode end2 = XmlChildRole.EMPTY_TAG_END_FINDER.findChild(node);
//              if (end2 != null) {
//                return document.getLineNumber(end2.getTextRange().getEndOffset()) + 1;
//              }
//            }
//          }
//        }
//      }
//    }
//    return -1;
  }

  @Nullable
  public static PsiElement findContextElement(Project project, @Nullable XSourcePosition position) {
    if (position == null) {
      return null;
    }

    final PsiFile file = PsiManager.getInstance(project).findFile(position.getFile());
    if (file == null) {
      return null;
    }

    int offset = -1;
    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document != null && document.getLineCount() > position.getLine() && position.getLine() >= 0) {
      offset = document.getLineStartOffset(position.getLine());
    }
    if (offset < 0) {
      offset = position.getOffset();
    }

    PsiElement contextElement = file.findElementAt(offset);
    while (contextElement != null && !(contextElement instanceof XmlElement)) {
      contextElement = PsiTreeUtil.nextLeaf(contextElement);
    }
    return contextElement;
  }
}
