package testcontainers;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import net.bugreaper.modules.minio.Minio;
import org.testcontainers.containers.MinIOContainer;

import java.util.Objects;

public class MinioContainerSetup {


    private static final String STABLE_VERSION = "minio/minio:RELEASE.2022-11-29T23-40-49Z";
    private static final String LATEST_VERSION = "minio/minio:RELEASE.2025-09-07T16-13-09Z";

    private static final String DOCKER_IMAGE = resolveDockerImage();

    static MinIOContainer container = new MinIOContainer(DOCKER_IMAGE)
            .withExposedPorts(9000)
            .withCreateContainerCmdModifier(cmd -> Objects.requireNonNull(cmd.getHostConfig()).withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(29000), new ExposedPort(9000))
            ))
            .withUserName("admin")
            .withPassword("password");


    static {
        System.out.printf("""
                \u001B[32m
                ============================================
                >>> TESTS RUNNING ON ON DOCKER IMAGE: %s <<<
                ============================================
                \u001B[0m
                %n""", DOCKER_IMAGE);

        container.start();
    }

    private static String resolveDockerImage() {
        String dockerVersion = System.getProperty("dockerTestVersion");

        if ("latest".equalsIgnoreCase(dockerVersion)) {
            return LATEST_VERSION;
        }

        return STABLE_VERSION;
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
