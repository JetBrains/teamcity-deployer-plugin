package my.buildServer.deployer.agent;

import org.apache.catalina.ant.DeployTask;

/**
 * Created by Nikita.Skvortsov
 * Date: 9/27/12, 4:09 PM
 */
public class Tmp {

    public static void main(String[] args) {
        DeployTask deployTask = new DeployTask();
        deployTask.setUrl("http://localhost:8080/manager/html");  // path to manager/html is required
        deployTask.setUsername("tomcat");
        deployTask.setPassword("manager-gui");

        deployTask.setWar("C:/Users/Nikita.Skvortsov/Downloads/sample.war");
        deployTask.setPath("/deployed_sample");   // leading / is required!
        deployTask.execute();
    }
}
