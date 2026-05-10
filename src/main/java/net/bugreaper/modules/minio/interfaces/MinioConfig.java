package net.bugreaper.modules.minio.interfaces;

import net.bugreaper.modules.minio.Minio;


/**
 * Interface that defines helper configuration methods for helper operations.
 * Validates that all required methods are implemented.
 */
public interface MinioConfig {


    /**
     * Configure download buffer size
     *
     * @param downloadBufferSize buffer size for file upload
     * @return this instance for method chaining
     * @throws IllegalArgumentException on invalid setup
     */
    Minio setDownloadBufferSize(int downloadBufferSize);

    /**
     * Configure global await for asserts with await
     *
     * @param awaitMs ms await
     * @return this instance for method chaining
     * @throws IllegalArgumentException on invalid setup
     */
    Minio setAwaitMs(int awaitMs);

    /**
     * Configure await for next assert with await (than await rollback to global)
     * global {@link #setAwaitMs(int)} will be ignored
     *
     * @param awaitMs ms await
     * @return this instance for method chaining
     * @throws IllegalArgumentException on invalid setup
     */
    Minio withAwaitMs(int awaitMs);

    /**
     * Configure max size for file to upload
     *
     * @param maxUploadSize max size in bytes for file to upload
     * @return this instance for method chaining
     * @throws IllegalArgumentException on invalid setup
     */
    Minio setMaxUploadSize(int maxUploadSize);

    /**
     * Configure max size for file to download/read
     *
     * @param maxDownloadObjectSize max size in bytes for object to download/read
     * @return this instance for method chaining
     * @throws IllegalArgumentException on invalid setup
     */
    Minio setMaxDownloadObjectSize(int maxDownloadObjectSize);

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
