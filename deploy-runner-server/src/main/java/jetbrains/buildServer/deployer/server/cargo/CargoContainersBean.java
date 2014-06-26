package jetbrains.buildServer.deployer.server.cargo;

import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by Nikita.Skvortsov
 * date: 26.06.2014.
 */
public class CargoContainersBean {
    final List<ContainerBean> containerIds = new ArrayList<ContainerBean>();

    public CargoContainersBean() {
        final ConfigurationFactory cfgFactory = new DefaultConfigurationFactory();
        final ContainerFactory factory = new DefaultContainerFactory();
        for (Map.Entry<String, Set<ContainerType>> idTypeEntry : factory.getContainerIds().entrySet()) {
            if (idTypeEntry.getValue().contains(ContainerType.REMOTE)) {
                final String id = idTypeEntry.getKey();
                final String name = getNameOrId(factory, cfgFactory, id);
                if (!name.contains("JBoss")) {
                    containerIds.add(new ContainerBean(id, name));
                }
            }
        }
        Collections.sort(containerIds, new Comparator<ContainerBean>() {
            @Override
            public int compare(ContainerBean o1, ContainerBean o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    private static String getNameOrId(@NotNull ContainerFactory factory,
                                      @NotNull ConfigurationFactory cfgFactory,
                                      @NotNull String id) {
        final Configuration tempCfg = cfgFactory.createConfiguration(id, ContainerType.REMOTE, ConfigurationType.RUNTIME);
        final Container container = factory.createContainer(id, ContainerType.REMOTE, tempCfg);
        final String name = container.getName();
        if (name.endsWith(" Remote")) {
            return name.substring(0, name.length() - " Remote".length());
        }
        return name;
    }

    public List<ContainerBean> getContainerIds() {
        return containerIds;
    }
}
