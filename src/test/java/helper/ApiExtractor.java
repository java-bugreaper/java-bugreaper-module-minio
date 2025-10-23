package helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ApiExtractor {

    private static final Logger logger = LoggerFactory.getLogger("api-extractor");

    public static String extractBody(String url) {

        String curlCommand = "curl -s -S " + url;

        try {
            Process process = Runtime.getRuntime().exec(curlCommand);

            String response = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));


            String error = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines()
                    .collect(Collectors.joining("\n"));


            process.waitFor();

            logger.info("cURL Command: {}", curlCommand);

            logger.info("Response Body:\n{}", response);

            if (!error.isEmpty()) {
                throw new RuntimeException("Error Stream:\n" + error);
            }

            return response;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("API extractor error:\n" + e);
        }
    }

}
