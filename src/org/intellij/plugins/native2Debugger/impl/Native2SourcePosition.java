package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.XSourcePositionWrapper;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public class Native2SourcePosition extends XSourcePositionWrapper {
  private final Debugger.Locatable myLocation;

  Native2SourcePosition(Debugger.Locatable location, XSourcePosition position) {
    super(position);

    myLocation = location;
  }

  @Nullable
  public static XSourcePosition create(Debugger.Locatable location) {
    final VirtualFile file;
    try {
      file = VfsUtil.findFileByURL(new URI(location.getURI()).toURL());
    } catch (Exception e) {
      // TODO log
      return null;
    }

    final int line = location.getLineNumber() - 1;
    final XSourcePosition position = XDebuggerUtil.getInstance().createPosition(file, line);
    return line >= 0 && position != null ? new Native2SourcePosition(location, position) : null;
  }

  public Debugger.Locatable getLocation() {
    return myLocation;
  }
}
