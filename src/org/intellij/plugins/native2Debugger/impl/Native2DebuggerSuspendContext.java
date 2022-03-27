package org.intellij.plugins.native2Debugger.impl;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.impl.Native2DebugProcess;

import java.util.List;
import java.util.Map;

public class Native2DebuggerSuspendContext extends XSuspendContext {
    private final Native2DebugProcess myDebuggerSession;
    private final Native2ExecutionStack[] myExecutionStacks;
    private final int myActiveStackId;

    public Native2DebuggerSuspendContext(Native2DebugProcess debuggerSession, Native2ExecutionStack[] executionStacks, int activeStackId) {
        myDebuggerSession = debuggerSession;
        myExecutionStacks = executionStacks;
        myActiveStackId = activeStackId;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
        if (myActiveStackId >= 0 && myActiveStackId < myExecutionStacks.length) {
            return myExecutionStacks[myActiveStackId];
        } else {
            return null;
        }
    }

    @Override
    public XExecutionStack /*@NotNull*/[] getExecutionStacks() {
        // TODO: print execution stacks of all threads
        return myExecutionStacks;
    }
}
