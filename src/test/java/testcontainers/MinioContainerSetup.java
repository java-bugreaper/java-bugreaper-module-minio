package testcontainers;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import net.bugreaper.modules.minio.Minio;
import org.testcontainers.containers.MinIOContainer;

import java.util.Objects;

public class MinioContainerSetup {


    static MinIOContainer container = new MinIOContainer("minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withExposedPorts(9000)
            .withCreateContainerCmdModifier(cmd -> Objects.requireNonNull(cmd.getHostConfig()).withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(29000), new ExposedPort(9000))
            ))
            .withUserName("admin")
            .withPassword("password");


    static {
        container.start();
    }

    public static Minio getMinio() {
        return new Minio(
                "http://" + container.getHost(),
                container.getMappedPort(9000),
                "admin",
                "password");
    }

    public Minio getMinio(String user, String pass) {
        return new Minio(
                "http://" + container.getHost(),
                container.getMappedPort(9000),
                user,
                pass);
    }

}
