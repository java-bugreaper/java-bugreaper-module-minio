package io.bugreaper.modules.minio.interfaces;

import io.bugreaper.modules.minio.Minio;


/**
 * Interface for Minio config methods
 *
 * @author ambu550
 */
public interface MinioConfig {


    /**
     * Configure
     *
     * @param downloadBufferSize buffer size for file upload
     * @return this
     */
    Minio withDownloadBufferSize(int downloadBufferSize);

    /**
     * Configure
     *
     * @param awaitMs ms wait for object in asserts with await
     * @return this
     */
    Minio withAwaitMs(int awaitMs);

    /**
     * Configure
     *
     * @param maxUploadSize max size in bytes for file to upload
     * @return this
     */
    Minio withMaxUploadSize(int maxUploadSize);

    /**
     * Configure
     *
     * @param maxDownloadObjectSize max size in bytes for object to download/read
     * @return this
     */
    Minio withMaxDownloadObjectSize(int maxDownloadObjectSize);

}
