/*
 * Copyright 2007 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.friendly_machines.intellij.plugins.native2Debugger.rt.engine;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.DebugProcess;
import com.friendly_machines.intellij.plugins.native2Debugger.impl.GdbMiOperationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

// Note: one line can map to multiple actual addrs! (but that's GDB's business)
public class BreakpointManager {
    private final List<Breakpoint> myBreakpoints = new ArrayList<Breakpoint>();
    private final DebugProcess myDebugProcess;

    public BreakpointManager(DebugProcess debugProcess) {
        myDebugProcess = debugProcess;
    }

    public static String fileLineReference(XSourcePosition position) {
        return position.getFile().getPath() + ":" + (position.getLine() + 1);
    }


    public boolean addBreakpoint(XLineBreakpoint<XBreakpointProperties> key) {
        final XSourcePosition sourcePosition = key.getSourcePosition();
        if (sourcePosition == null || !sourcePosition.getFile().exists() || !sourcePosition.getFile().isValid()) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported file for breakpoint");
            return false;
        }

        final VirtualFile file = sourcePosition.getFile();
        final Project project = myDebugProcess.getSession().getProject();
        final int lineNumber = sourcePosition.getLine();
        if (lineNumber == -1) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint position");
            return false;
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
        if (key.isTemporary())
            options.add("-t");
        if (!key.isEnabled())
            options.add("-d");
        // TODO: breakpoint.isLogStack()
        options.add(fileLineReference(key.getSourcePosition()));
        Object response;
        try {
            HashMap<String, Object> gdbResponse;
            if (key.isLogMessage()) {
                options.add("Breakpointhit"); // TODO: what is the message?
                gdbResponse = myDebugProcess.dprintfInsert(options.toArray(new String[0]));
            } else {
                gdbResponse = myDebugProcess.breakInsert(options.toArray(new String[0]));
            }
            String number = (String) gdbResponse.get("number");
            myBreakpoints.add(new Breakpoint(myDebugProcess, key, (HashMap<String, Object>) gdbResponse.get("bkpt")));
            return true;
        } catch (GdbMiOperationException e) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint position");
            return false;
        } catch (ClassCastException e) {
            myDebugProcess.getSession().setBreakpointInvalid(key, "Unsupported breakpoint position");
            return false;
        }
    }

    List<Breakpoint> getBreakpoints() {
        return myBreakpoints;
    }

    public Optional<Breakpoint> getBreakpointByGdbNumber(String key) {
        for (Breakpoint breakpoint : myBreakpoints) {
            String number = breakpoint.getNumber();
            if (number.equals(key)) {
                return Optional.of(breakpoint);
            }
        }
        return Optional.empty();
    }

    public Optional<Breakpoint> getBreakpoint(XBreakpoint key) {
        for (Breakpoint breakpoint : myBreakpoints) {
            if (breakpoint.getXBreakpoint() == key) {
                return Optional.of(breakpoint);
            }
        }
        return Optional.empty();
    }

    public boolean deleteBreakpoint(XBreakpoint key) {
        Optional<Breakpoint> breakpointo = getBreakpoint(key);
        if (breakpointo.isPresent()) {
            Breakpoint breakpoint = breakpointo.get();
            String number = breakpoint.getNumber();
            try {
                myDebugProcess.breakDelete(number);
                myBreakpoints.remove(breakpoint);
                return true;
            } catch (GdbMiOperationException e) {
                myDebugProcess.getSession().reportError("Breakpoint could not be deleted in GDB");
                return false;
            }
        } else
            return false;
    }

    public boolean deleteBreakpoint1(String number) {
        Optional<Breakpoint> breakpointo = getBreakpointByGdbNumber(number);
        if (breakpointo.isPresent()) {
            Breakpoint breakpoint = breakpointo.get();
            // not myDebugProcess.breakDelete(number);
            myBreakpoints.remove(breakpoint);
            return true;
        } else
            return false;
    }
}
