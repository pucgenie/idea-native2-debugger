// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.native2Debugger.impl;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.*;
import org.intellij.plugins.native2Debugger.Native2DebuggerBundle;
import org.intellij.plugins.native2Debugger.Native2DebuggerSession;
import org.intellij.plugins.native2Debugger.VMPausedException;
import org.intellij.plugins.native2Debugger.rt.engine.DebuggerStoppedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.intellij.plugins.native2Debugger.rt.engine.Debugger;
import org.intellij.plugins.native2Debugger.rt.engine.Value;

import javax.swing.*;
import java.net.URI;
import java.util.List;

public class Native2StackFrame extends XStackFrame {
  private final Debugger.Frame myFrame;
  private final Native2DebuggerSession myDebuggerSession;
  private final XSourcePosition myPosition;

  public Native2StackFrame(Debugger.Frame frame, Native2DebuggerSession debuggerSession) {
    myFrame = frame;
    myDebuggerSession = debuggerSession;
    myPosition = Native2SourcePosition.create(frame);
  }

  @Override
  public Object getEqualityObject() {
    return Native2StackFrame.class;
  }

  @Override
  public XDebuggerEvaluator getEvaluator() {
    return myFrame instanceof Debugger.StyleFrame ? new MyEvaluator((Debugger.StyleFrame)myFrame) : null;
  }

  @Override
  public XSourcePosition getSourcePosition() {
    return myPosition;
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    if (myDebuggerSession.getCurrentState() == Debugger.State.SUSPENDED) {
      try {
        _customizePresentation(component);
      }
      catch (VMPausedException | DebuggerStoppedException ignore) {
      }
    }
  }

  private void _customizePresentation(ColoredTextContainer component) {
    final Debugger.Frame frame = myFrame;
    if (frame instanceof Debugger.StyleFrame) {
      component.append(((Debugger.StyleFrame)frame).getInstruction(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES); //NON-NLS
    } else if (frame instanceof Debugger.SourceFrame) {
      component.append(((Debugger.SourceFrame)frame).getXPath(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES); //NON-NLS
    }
    component.append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES);

    try {
      final VirtualFile file = VfsUtil.findFileByURL(new URI(frame.getURI()).toURL());
      if (file != null) {
        component.append(file.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        if (frame.getLineNumber() > 0) {
          component.append(":" + frame.getLineNumber(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        component.setToolTipText(file.getPresentableUrl());
      } else {
        component.append(frame.getURI() + ":" + frame.getLineNumber(), SimpleTextAttributes.REGULAR_ATTRIBUTES); //NON-NLS
      }
    } catch (Exception ignored) {
      component.append(frame.getURI() + ":" + frame.getLineNumber(), SimpleTextAttributes.REGULAR_ATTRIBUTES); //NON-NLS
    }
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    try {
      if (myFrame instanceof Debugger.StyleFrame) {
        final List<Debugger.Variable> variables = ((Debugger.StyleFrame)myFrame).getVariables();
        final XValueChildrenList list = new XValueChildrenList();
        for (final Debugger.Variable variable : variables) {
          list.add(variable.getName(), new MyValue(variable));
        }
        node.addChildren(list, true);
      } else {
        super.computeChildren(node);
      }
    } catch (VMPausedException ignored) {
      node.setErrorMessage(Native2DebuggerBundle.message("dialog.message.target.vm.not.responding"));
    }
  }

  public Debugger.Frame getFrame() {
    return myFrame;
  }

  private static class MyValue extends XValue {
    private final Debugger.Variable myVariable;

    MyValue(Debugger.Variable variable) {
      myVariable = variable;
    }

    @Override
    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
      final Debugger.Variable.Kind kind = myVariable.getKind();
      Icon icon = null;
      if (myVariable.isGlobal()) {
        if (kind == Debugger.Variable.Kind.VARIABLE) {
          icon = PlatformIcons.FIELD_ICON;
        } else {
          icon = PlatformIcons.PROPERTY_ICON;
        }
      } else if (kind == Debugger.Variable.Kind.VARIABLE) {
        icon = PlatformIcons.VARIABLE_ICON;
      } else if (kind == Debugger.Variable.Kind.PARAMETER) {
        icon = PlatformIcons.PARAMETER_ICON;
      }

      final Value v = myVariable.getValue();
      if (v.getType() == Value.XPathType.STRING) {
        node.setPresentation(icon, v.getType().getName(), "'" + v.getValue() + "'", false);
      } else {
        final boolean hasChildren = myVariable.getValue().getValue() instanceof Value.NodeSet;
        node.setPresentation(icon, v.getType().getName(), String.valueOf(v.getValue()), hasChildren);
      }
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
      if (myVariable.getValue().getValue() instanceof Value.NodeSet) {
        final Value.NodeSet set = (Value.NodeSet)myVariable.getValue().getValue();
        final XValueChildrenList list = new XValueChildrenList();
        for (final Value.Node n : set.getNodes()) {
          list.add(n.myXPath, new NodeValue(n));
        }
        node.addChildren(list, false);
      }
      super.computeChildren(node);
    }

    @Override
    public String getEvaluationExpression() {
      return "$" + myVariable.getName();
    }

    @Override
    public void computeSourcePosition(@NotNull XNavigatable navigatable) {
      navigatable.setSourcePosition(Native2SourcePosition.create(myVariable));
    }

    static String clipValue(String stringValue) {
      return stringValue.length() < 100 ? stringValue : stringValue.substring(0, 100) + "...";
    }

    private static class NodeValue extends XValue {
      private final Value.Node myNode;

      NodeValue(Value.Node n) {
        myNode = n;
      }

      @Override
      public void computeSourcePosition(@NotNull XNavigatable navigatable) {
        navigatable.setSourcePosition(Native2SourcePosition.create(myNode));
      }

      @Override
      public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        node.setPresentation(null, "node", myNode.myStringValue, false);
      }
    }
  }

  private static class MyEvaluator extends XDebuggerEvaluator {
    private final Debugger.StyleFrame myFrame;

    MyEvaluator(Debugger.StyleFrame frame) {
      myFrame = frame;
    }

    @Override
    public boolean isCodeFragmentEvaluationSupported() {
      return false;
    }

    @Override
    public void evaluate(@NotNull String expression, @NotNull XEvaluationCallback callback, @Nullable XSourcePosition expressionPosition) {
      try {
        final Value eval = myFrame.eval(expression);
        callback.evaluated(new MyValue(new ExpressionResult(eval)));
      } catch (VMPausedException ignored) {
        callback.errorOccurred(Native2DebuggerBundle.message("dialog.message.target.vm.not.responding"));
      } catch (Debugger.EvaluationException e) {
        callback.errorOccurred(e.getMessage() != null ? e.getMessage() : e.toString());
      }
    }

    private static class ExpressionResult implements Debugger.Variable {
      private final Value myValue;

      ExpressionResult(Value value) {
        myValue = value;
      }

      @Override
      public String getURI() {
        return null;
      }

      @Override
      public int getLineNumber() {
        return -1;
      }

      @Override
      public boolean isGlobal() {
        return false;
      }

      @Override
      public Kind getKind() {
        return Kind.EXPRESSION;
      }

      @Override
      public String getName() {
        return "result";
      }

      @Override
      public Value getValue() {
        return myValue;
      }
    }
  }
}
