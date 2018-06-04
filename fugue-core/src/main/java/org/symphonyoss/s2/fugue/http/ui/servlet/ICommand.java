package org.symphonyoss.s2.fugue.http.ui.servlet;

import java.util.EnumSet;

import org.symphonyoss.s2.fugue.FugueLifecycleState;

public interface ICommand
{

  String getName();

  String getPath();

  EnumSet<FugueLifecycleState> getValidStates();

  ICommandHandler getHandler();

}