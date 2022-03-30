// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.
package com.friendly_machines.intellij.plugins.native2Debugger;

import com.friendly_machines.intellij.plugins.native2Debugger.impl.DebugProcess;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.configurations.RunnerSettings;

import java.io.IOException;
import java.util.Objects;

public class Runner implements ProgramRunner<RunnerSettings> {
    private static final String RUNNER_ID = "Native2DebuggerRunner";
    static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();

    public Runner() {
        super();
    }

    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }
    /**
     * This makes sure the Debug mode is executed and not run mode
     *
     * @param executorId
     * @param profile
     * @return
     */
    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return (DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof DebuggerConfiguration);
    }

    @Nullable
    protected RunContentDescriptor createContentDescriptor(com.intellij.execution.configurations.RunProfileState runProfileState, ExecutionEnvironment environment)
            throws ExecutionException {
        Executor executor = environment.getExecutor();
        // could check STATE's stuff (like getParameters() etc)
        //String debuggerPort = DebuggerUtils.getInstance().findAvailableDebugAddress(true);
        //String remotePort = JDWP + debuggerPort;
        //javaParameters.getVMParametersList().addParametersString(remotePort);
        //RemoteConnection connection = new RemoteConnection(true, LOCALHOST, debuggerPort, false);
        // Attaches the remote configuration to the VM and then starts it up
        XDebugSession debugSession =
                XDebuggerManager.getInstance(environment.getProject()).startSession(environment, new XDebugProcessStarter() {
                    @Override
                    public @NotNull
                    XDebugProcess start(final @NotNull XDebugSession session) throws ExecutionException {
                        ACTIVE.set(Boolean.TRUE);
                        try {
                            final RunProfileState c = (RunProfileState)runProfileState;
                            return new DebugProcess(c, environment, Runner.this, session);
                        } catch (IOException e) {
                            e.printStackTrace();
                            throw new ExecutionException(e);
                        } finally {
                            ACTIVE.remove();
                        }
                    }
                });
        return debugSession.getRunContentDescriptor();
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, Objects.requireNonNull(environment.getState()), state -> {
            FileDocumentManager.getInstance().saveAllDocuments();
            return createContentDescriptor(state, environment);
        });
    }
}