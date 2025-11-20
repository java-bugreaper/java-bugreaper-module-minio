package io.bugreaper.modules.minio;

import org.junit.jupiter.api.Test;

import static io.bugreaper.core.utils.AllureStepsValidator.validateAllSteps;

class AllureStepsValidationTest {

    @Test
    void testStepsMinio() {
        validateAllSteps("io.bugreaper.modules.minio.Minio");
    }

}
