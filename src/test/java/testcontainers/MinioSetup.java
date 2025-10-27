package testcontainers;

import io.bugreaper.modules.minio.Minio;
import org.testcontainers.containers.MinIOContainer;

public class MinioSetup {


    MinIOContainer container = new MinIOContainer("minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withExposedPorts(9000)
            .withUserName("admin")
            .withPassword("password");


    private static MinioSetup instance;


    public MinioSetup() {
        container.start();
    }

    public static MinioSetup getInstance() {
        if (instance == null) {
            instance = new MinioSetup();
        }

        return instance;
    }

    public Minio getMinio() {
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
