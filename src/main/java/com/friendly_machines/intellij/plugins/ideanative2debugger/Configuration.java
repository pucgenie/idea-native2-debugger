// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the COPYING file.

package com.friendly_machines.intellij.plugins.ideanative2debugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RemoteRunProfile;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import javax.swing.*;

public class Configuration extends LocatableConfigurationBase
        implements RunConfigurationWithSuppressedDefaultRunAction, RemoteRunProfile /* TODO: Maybe remove RemoteRunProfile */ {


    protected Configuration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(project, factory);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        /* FIXME: extends SettingsEditor<XsltRunConfiguration>
  implements CheckableRunConfigurationEditor<XsltRunConfiguration>  */
        SettingsEditorGroup<Configuration> group = new SettingsEditorGroup<>();
        // or just: return new Native2DebuggerSettingsEditor(getProject());
        //group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new RemoteConfigurable(getProject())); FIXME
        //group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<>());
        return group;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public @Nullable
    com.intellij.execution.configurations.RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        /*final GenericDebuggerRunnerSettings debuggerSettings = (GenericDebuggerRunnerSettings)env.getRunnerSettings();
        if (debuggerSettings != null) {
            // sync self state with execution environment's state if available
            debuggerSettings.LOCAL = false;
            debuggerSettings.setDebugPort(USE_SOCKET_TRANSPORT ? PORT : SHMEM_ADDRESS);
            debuggerSettings.setTransport(USE_SOCKET_TRANSPORT ? DebuggerSettings.SOCKET_TRANSPORT : DebuggerSettings.SHMEM_TRANSPORT);
        }*/
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(getProject());
        return new RunProfileState(this, env, builder);
    }
}
