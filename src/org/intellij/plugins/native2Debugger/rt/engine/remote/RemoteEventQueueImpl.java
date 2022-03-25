/*
 * Copyright 2002-2007 Sascha Weinreuter
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

package org.intellij.plugins.native2Debugger.rt.engine.remote;

import org.intellij.plugins.native2Debugger.rt.engine.OutputEventQueue;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class RemoteEventQueueImpl extends UnicastRemoteObject implements RemoteDebugger.EventQueue {
  private final OutputEventQueue myQueue;

  public RemoteEventQueueImpl(OutputEventQueue queue) throws RemoteException {
    myQueue = queue;
  }

  @Override
  public List<OutputEventQueue.NodeEvent> getEvents() throws RemoteException {
    return myQueue.getEvents();
  }

  @Override
  public void setEnabled(boolean b) throws RemoteException {
    myQueue.setEnabled(b);
  }
}
