// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.friendly_machines.intellij.plugins.native2Debugger.impl;

import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.Breakpoint;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.friendly_machines.intellij.plugins.native2Debugger.Native2BreakpointType;
import com.friendly_machines.intellij.plugins.native2Debugger.rt.engine.BreakpointManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class Native2BreakpointHandler extends XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> {
  private final Native2DebugProcess myNative2DebugProcess;

  public Native2BreakpointHandler(Native2DebugProcess native2DebugProcess, final Class<? extends Native2BreakpointType> typeClass) {
    super(typeClass);
    myNative2DebugProcess = native2DebugProcess;
  }

  @Override
  public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
    final XSourcePosition sourcePosition = breakpoint.getSourcePosition();
    if (sourcePosition == null || !sourcePosition.getFile().exists() || !sourcePosition.getFile().isValid()) {
      // ???
      return;
    }

    final VirtualFile file = sourcePosition.getFile();
    final Project project = myNative2DebugProcess.getSession().getProject();
    final String fileURL = getFileURL(file);
    final int lineNumber = getActualLineNumber(breakpoint, project);
    if (lineNumber == -1) {
      myNative2DebugProcess.getSession().setBreakpointInvalid(breakpoint, "Unsupported breakpoint position");
      return;
    }

    ArrayList<String> options = new ArrayList<>();
    // TODO: "-h" for hardware breakpoint
    // TODO: "-f" for creating a pending breakpoint if necessary
    // TODO: "-a" for a tracepoint (see GDB page 193)
    // TODO: "-c condition" for condition
    // TODO: "-i ignore-count"
    // TODO: "-p thread-id"
    // TODO: -break-watch [-r|-a] <variable>
    // TODO: -break-passcount <tracepoint-id> <passcount>
    if (breakpoint.isTemporary())
      options.add("-t");
    if (!breakpoint.isEnabled())
      options.add("-d");
    // TODO: breakpoint.isLogStack()
    options.add(myNative2DebugProcess.fileLineReference(sourcePosition));
    if (breakpoint.isLogMessage()) {
      options.add("Breakpointhit"); // TODO: what is the message?
      myNative2DebugProcess.gdbSend("-dprintf-insert", options.toArray(new String[0]), new String[0]);
    } else {
      myNative2DebugProcess.gdbSend("-break-insert", options.toArray(new String[0]), new String[0]);
    }
    final BreakpointManager manager = myNative2DebugProcess.getBreakpointManager();
    Breakpoint bp;
    if ((bp = manager.getBreakpoint(fileURL, lineNumber)) != null) {
      bp.setEnabled(true);
    } else {
      manager.setBreakpoint(breakpoint, fileURL, lineNumber);
    }
    System.err.println("REGISTER BREAKPOINT 4");
//      final XDebugSession session = myNative2DebugProcess.getSession();
//      session.reportMessage(Native2DebuggerBundle.message("notification.content.target.vm.not.responding.breakpoint.can.not.be.set"), MessageType.ERROR);
//      session.setBreakpointInvalid(breakpoint, "Target VM is not responding. Breakpoint can not be set");
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

    final BreakpointManager manager = myNative2DebugProcess.getBreakpointManager();
    if (temporary) {
      final Breakpoint bp = manager.getBreakpoint(fileURL, lineNumber);
      if (bp != null) {
        bp.setEnabled(false);
      }
    } else {
      manager.removeBreakpoint(fileURL, lineNumber);
    }
//    myNative2DebugProcess.getSession().reportMessage(
//            Native2DebuggerBundle.message("notification.content.target.vm.not.responding.breakpoint.can.not.be.removed"), MessageType.ERROR);
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

//  @Nullable
//  public static PsiElement findContextElement(Project project, @Nullable XSourcePosition position) {
//    if (position == null) {
//      return null;
//    }
//
//    final PsiFile file = PsiManager.getInstance(project).findFile(position.getFile());
//    if (file == null) {
//      return null;
//    }
//
//    int offset = -1;
//    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
//    if (document != null && document.getLineCount() > position.getLine() && position.getLine() >= 0) {
//      offset = document.getLineStartOffset(position.getLine());
//    }
//    if (offset < 0) {
//      offset = position.getOffset();
//    }
//
//    PsiElement contextElement = file.findElementAt(offset);
//    while (contextElement != null && !(contextElement instanceof XmlElement)) {
//      contextElement = PsiTreeUtil.nextLeaf(contextElement);
//    }
//    return contextElement;
//  }
}
