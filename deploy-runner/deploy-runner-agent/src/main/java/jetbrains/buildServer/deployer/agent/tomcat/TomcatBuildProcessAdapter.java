package jetbrains.buildServer.deployer.agent.tomcat;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.catalina.ant.DeployTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;


class TomcatBuildProcessAdapter extends BuildProcessAdapter {

    private static final String MANAGER_APP_SUFFIX = "manager/html";
    private static final String HTTP_PREFIX = "http://";
    private final String target;
    private final String username;
    private final String password;
    private final BuildRunnerContext context;
    private final String sourcePath;

    private volatile boolean hasFinished;

    public TomcatBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
        this.target = target;
        this.username = username;
        this.password = password;
        this.context = context;
        this.sourcePath = sourcePath;
        hasFinished = false;
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
        return hasFinished ? BuildFinishedStatus.FINISHED_SUCCESS :
                BuildFinishedStatus.INTERRUPTED;
    }

    @Override
    public void start() throws RunBuildException {

        try {
            final StringBuilder sb = new StringBuilder();
            if (!target.startsWith(HTTP_PREFIX)) {
                sb.append(HTTP_PREFIX);
            }
            sb.append(target);

            if (!target.endsWith(MANAGER_APP_SUFFIX)) { // path to manager/html is required
                if (!target.endsWith("/")) {
                    sb.append('/');
                }
                sb.append(MANAGER_APP_SUFFIX);
            }

            final String targetUrl = sb.toString();
            DeployTask deployTask = new DeployTask();
            deployTask.setUrl(targetUrl);
            deployTask.setUsername(username);
            deployTask.setPassword(password);

            final File deployableWar = new File(context.getWorkingDirectory(), sourcePath);
            deployTask.setWar(deployableWar.getCanonicalPath());

            final String fileNameWithExtension = deployableWar.getName();
            final String contextName = fileNameWithExtension.substring(0, fileNameWithExtension.length() - 4);

            deployTask.setPath("/" + contextName);   // leading / is required!
            deployTask.execute();
        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            hasFinished = true;
        }
    }
}
