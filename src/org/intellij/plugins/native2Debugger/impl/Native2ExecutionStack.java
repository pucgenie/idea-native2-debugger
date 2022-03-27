// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;

import java.util.*;

// Per thread
public class Native2ExecutionStack extends XExecutionStack {
    //private final Native2StackFrame myTopFrame;
    private final Native2DebugProcess myDebuggerSession;
    private final Native2StackFrame myTopFrame;
    private final String myThreadId;

    public Native2ExecutionStack(@NlsContexts.ListItem String name, String threadId, HashMap<String, Object> topFrame, Native2DebugProcess debuggerSession) {
        super(name);
        myDebuggerSession = debuggerSession;
        myThreadId = threadId;
//    for (Map.Entry<String, Object> frame : frames) {
//      if ("frame".equals(frame.getKey())) {
//        myFrames.add(new Native2StackFrame((HashMap<String, Object>) frame.getValue(), myDebuggerSession));
//      }
//    }
        myTopFrame = new Native2StackFrame(threadId, (HashMap<String, Object>) topFrame, myDebuggerSession);
    }

    @Override
    public XStackFrame getTopFrame() {
        return myTopFrame;
    }

    @Override
    public void computeStackFrames(int firstFrameIndex, XStackFrameContainer container) {
        // TODO: Compute the "other" frames here (using the Native2DebugProcess):
        myDebuggerSession.getFrames(myThreadId);

//    if (myDebuggerSession.getCurrentState() == Debugger.State.SUSPENDED) {
        final List<XStackFrame> frames = new ArrayList<>();
        frames.add(myTopFrame);
        if (firstFrameIndex <= frames.size()) {
            container.addStackFrames(frames.subList(firstFrameIndex, frames.size()), true);
        } else {
            container.addStackFrames(Collections.emptyList(), true);
        }
//    }
    }
}
