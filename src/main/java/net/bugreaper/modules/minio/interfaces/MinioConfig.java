package net.bugreaper.modules.minio.interfaces;

import net.bugreaper.modules.minio.Minio;


/**
 * Interface that defines helper configuration methods for helper operations.
 * Validates that all required methods are implemented.
 */
public interface MinioConfig {


    /**
     * Sets the buffer size for file downloads.
     *
     * @param downloadBufferSize buffer size in bytes
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the specified size is invalid
     */
    Minio setDownloadBufferSize(int downloadBufferSize);

    /**
     * Configures the global await timeout for assertions.
     *
     * @param awaitMs await timeout in milliseconds
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the provided timeout is invalid or less than 200 milliseconds
     */
    Minio setAwaitMs(int awaitMs);

    /**
     * Sets a custom await timeout for the next assertion operation.
     *
     * <p>After the operation is completed, the timeout is reset to the global value
     * configured by {@link #setAwaitMs(int)}.</p>
     *
     * @param awaitMs await timeout in milliseconds
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the timeout is invalid
     */
    Minio withAwaitMs(int awaitMs);

    /**
     * Sets the maximum size of a file that can be uploaded.
     *
     * @param maxUploadSize maximum file size in bytes
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the specified size is invalid
     */
    Minio setMaxUploadSize(int maxUploadSize);

    /**
     * Sets the maximum size of an object that can be downloaded or read.
     *
     * @param maxDownloadObjectSize maximum object size in bytes
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the specified size is invalid
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
