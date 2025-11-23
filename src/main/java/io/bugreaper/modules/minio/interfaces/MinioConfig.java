package io.bugreaper.modules.minio.interfaces;

import io.bugreaper.modules.minio.Minio;


/**
 * Interface for Minio config methods
 *
 * @author ambu550
 */
public interface MinioConfig {


    /**
     * Configure download buffer size
     *
     * @param downloadBufferSize buffer size for file upload
     * @return this instance {@link Minio}
     * @throws IllegalArgumentException on invalid setup
     */
    Minio withDownloadBufferSize(int downloadBufferSize);

    /**
     * Configure await in asserts with await
     *
     * @param awaitMs ms await
     * @return this instance {@link Minio}
     * @throws IllegalArgumentException on invalid setup
     */
    Minio withAwaitMs(int awaitMs);

    /**
     * Configure max size for file to upload
     *
     * @param maxUploadSize max size in bytes for file to upload
     * @return this instance {@link Minio}
     * @throws IllegalArgumentException on invalid setup
     */
    Minio withMaxUploadSize(int maxUploadSize);

    /**
     * Configure max size for file to download/read
     *
     * @param maxDownloadObjectSize max size in bytes for object to download/read
     * @return this instance {@link Minio}
     * @throws IllegalArgumentException on invalid setup
     */
    Minio withMaxDownloadObjectSize(int maxDownloadObjectSize);

    /**
     * Returns and logs (at INFO level) a human-readable summary of all resolved
     * configuration values.
     * <p>
     * The summary includes values loaded from the YAML configuration file as well as
     * any fields overridden programmatically after construction. Optional fields that
     * were not present in the configuration and resolved via default values may also
     * be included.
     *
     * @return String with summary
     */
    String getConfigSummary();

}
