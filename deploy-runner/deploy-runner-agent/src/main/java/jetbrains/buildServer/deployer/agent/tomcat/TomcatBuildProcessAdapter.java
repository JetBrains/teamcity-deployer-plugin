package jetbrains.buildServer.deployer.agent.tomcat;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcessAdapter;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.catalina.ant.DeployTask;
import org.apache.catalina.ant.ListTask;
import org.apache.catalina.ant.UndeployTask;
import org.apache.tools.ant.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;


class TomcatBuildProcessAdapter extends BuildProcessAdapter {

    private static final String MANAGER_APP_SUFFIX = "manager";
    private static final String HTTP_PREFIX = "http://";
    private static final String CONTEXTS_LIST_PROPERTY = "temp.contexts.list";

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;
    private final BuildRunnerContext myContext;
    private final String myWarPath;

    private volatile boolean hasFinished;

    public TomcatBuildProcessAdapter(String target, String username, String password, BuildRunnerContext context, String sourcePath) {
        myTarget = target;
        myUsername = username;
        myPassword = password;
        myContext = context;
        myWarPath = sourcePath;
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
            if (!myTarget.startsWith(HTTP_PREFIX)) {
                sb.append(HTTP_PREFIX);
            }
            sb.append(myTarget);

            if (!myTarget.endsWith(MANAGER_APP_SUFFIX)) { // path to manager/html is required
                if (!myTarget.endsWith("/")) {
                    sb.append('/');
                }
                sb.append(MANAGER_APP_SUFFIX);
            }

            final File deployableWar = new File(myContext.getWorkingDirectory(), myWarPath);
            final String fileNameWithExtension = deployableWar.getName();
            final String contextName = fileNameWithExtension.substring(0, fileNameWithExtension.length() - 4);
            final String targetUrl = sb.toString();

            final Project tempProject = new Project();
            final ListTask list = new ListTask();
            list.setProject(tempProject);
            list.setUrl(targetUrl);
            list.setUsername(myUsername);
            list.setPassword(myPassword);
            list.setOutputproperty(CONTEXTS_LIST_PROPERTY);
            list.execute();

            final String contextsList = tempProject.getProperty(CONTEXTS_LIST_PROPERTY);

            if (contextsList.contains(contextName)) {
                final UndeployTask undeployTask = new UndeployTask();
                undeployTask.setUrl(targetUrl);
                undeployTask.setUsername(myUsername);
                undeployTask.setPassword(myPassword);
                undeployTask.setPath("/" + contextName);
                undeployTask.execute();
            }

            final DeployTask deployTask = new DeployTask();
            deployTask.setUrl(targetUrl);
            deployTask.setUsername(myUsername);
            deployTask.setPassword(myPassword);
            deployTask.setWar(deployableWar.getCanonicalPath());
            deployTask.setPath("/" + contextName);   // leading / is required!
            deployTask.execute();

        } catch (Exception e) {
            throw new RunBuildException(e);
        } finally {
            hasFinished = true;
        }
    }
}
