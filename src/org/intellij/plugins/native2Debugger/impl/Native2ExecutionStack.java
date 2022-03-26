// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.Native2DebuggerSession;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Native2ExecutionStack extends XExecutionStack {
  private final Native2StackFrame myTopFrame;
  private final Native2DebuggerSession myDebuggerSession;

  public Native2ExecutionStack(@NlsContexts.ListItem String name, HashMap<String, Object> topFrame, Native2DebuggerSession debuggerSession) {
    super(name);
    myDebuggerSession = debuggerSession;
    myTopFrame = new Native2StackFrame(topFrame, myDebuggerSession);
  }

  @Override
  public XStackFrame getTopFrame() {
    return myTopFrame;
  }

  @Override
  public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
    if (myDebuggerSession.getCurrentState() == Debugger.State.SUSPENDED) {
      final List<XStackFrame> frames = new ArrayList<>();
      frames.add(myTopFrame);
      // TODO: add the other frames!
      if (firstFrameIndex <= frames.size()) {
        container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
      } else {
        container.addStackFrames(Collections.emptyList(), true);
      }
    }
  }
}
