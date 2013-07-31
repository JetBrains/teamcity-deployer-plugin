package jetbrains.buildServer.deployer.agent.tomcat;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.deployer.agent.SyncBuildProcessAdapter;
import jetbrains.buildServer.util.StringUtil;
import org.apache.catalina.ant.DeployTask;
import org.apache.catalina.ant.ListTask;
import org.apache.catalina.ant.UndeployTask;
import org.apache.tools.ant.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;


class TomcatBuildProcessAdapter extends SyncBuildProcessAdapter {

    private static final String MANAGER_APP_SUFFIX = "manager";
    private static final String HTTP_PREFIX = "http://";
    private static final String CONTEXTS_LIST_PROPERTY = "temp.contexts.list";

    private final String myTarget;
    private final String myUsername;
    private final String myPassword;

    private final String myWepappContext;
    private final File myWarArchive;


    public TomcatBuildProcessAdapter(final @NotNull String target,
                                     final @NotNull String username,
                                     final @NotNull String password,
                                     final @NotNull BuildRunnerContext context,
                                     final @NotNull String sourcePath,
                                     final @Nullable String contextPath) {
        super(context.getBuild().getBuildLogger());
        myTarget = target;
        myUsername = username;
        myPassword = password;
        myWarArchive = new File(context.getWorkingDirectory(), sourcePath);

        if (contextPath == null || StringUtil.isEmptyOrSpaces(contextPath)) {
        final String fileNameWithExtension = myWarArchive.getName();
            // leading / is required!
            myWepappContext = "/" + fileNameWithExtension.substring(0, fileNameWithExtension.length() - 4);
        } else {
            // leading / is required!
            myWepappContext = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        }
    }


    @Override
    public void runProcess() throws RunBuildException {

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

            final String targetUrl = sb.toString();
            myLogger.message("Using manager app located at [" + targetUrl + "]");

            final Project tempProject = new Project();
            final ListTask list = new ListTask();
            list.setProject(tempProject);
            list.setUrl(targetUrl);
            list.setUsername(myUsername);
            list.setPassword(myPassword);
            list.setOutputproperty(CONTEXTS_LIST_PROPERTY);
            list.execute();

            final String contextsList = tempProject.getProperty(CONTEXTS_LIST_PROPERTY);

            if (contextsList.contains(myWepappContext)) {
                myLogger.message("Found existing context [" + myWepappContext + "]. Trying to undeploy...");
                final UndeployTask undeployTask = new UndeployTask();
                undeployTask.setUrl(targetUrl);
                undeployTask.setUsername(myUsername);
                undeployTask.setPassword(myPassword);
                undeployTask.setPath(myWepappContext);
                undeployTask.execute();
                myLogger.message("Undeployed [" + myWepappContext + "]");
            }

            myLogger.message("Deploying [" + myWarArchive.getPath() + "] to [" + myWepappContext + "]...");
            final DeployTask deployTask = new DeployTask();
            deployTask.setUrl(targetUrl);
            deployTask.setUsername(myUsername);
            deployTask.setPassword(myPassword);
            deployTask.setWar(myWarArchive.getCanonicalPath());
            deployTask.setPath(myWepappContext);
            deployTask.execute();
            myLogger.message("Deployed [" + myWepappContext + "]");

        } catch (Exception e) {
            throw new RunBuildException(e);
        }
    }
}
