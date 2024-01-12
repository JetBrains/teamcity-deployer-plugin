

package jetbrains.buildServer.deployer.server.cargo;

/**
 * Created by Nikita.Skvortsov
 * date: 26.06.2014.
 */
public class ContainerBean {
  private final String id;
  private final String name;

  public ContainerBean(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }
}