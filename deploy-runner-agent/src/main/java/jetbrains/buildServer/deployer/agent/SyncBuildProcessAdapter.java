package jetbrains.buildServer.deployer.agent;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;

public abstract class SyncBuildProcessAdapter extends BuildProcessAdapter {
  protected final BuildProgressLogger myLogger;
  private volatile boolean hasFinished;
  private volatile BuildFinishedStatus statusCode;
  private volatile boolean isInterrupted;


  public SyncBuildProcessAdapter(@NotNull final BuildProgressLogger logger) {
    myLogger = logger;
    hasFinished = false;
    statusCode = null;
  }


  @Override
  public void interrupt() {
    isInterrupted = true;
  }

  @Override
  public boolean isInterrupted() {
    return isInterrupted;
  }

  @Override
  public boolean isFinished() {
    return hasFinished;
  }

  @NotNull
  @Override
  public BuildFinishedStatus waitFor() throws RunBuildException {
    while (!isInterrupted() && !hasFinished) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RunBuildException(e);
      }
    }
    return hasFinished ? statusCode : BuildFinishedStatus.INTERRUPTED;
  }

  @Override
  public void start() throws RunBuildException {
    try {
      statusCode = runProcess();
      hasFinished = true;
    } catch (UploadInterruptedException e) {
      hasFinished = false;
    }
  }

  /**
   * @return true is process finished successfully
   */
  protected abstract BuildFinishedStatus runProcess();

  protected void checkIsInterrupted() throws UploadInterruptedException {
    if (isInterrupted()) throw new UploadInterruptedException();
  }

}
