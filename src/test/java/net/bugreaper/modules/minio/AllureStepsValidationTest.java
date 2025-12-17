package net.bugreaper.modules.minio;

import org.junit.jupiter.api.Test;

import static net.bugreaper.core.utils.AllureStepsValidator.validateAllSteps;

class AllureStepsValidationTest {

    @Test
    void testStepsMinio() {
        validateAllSteps("net.bugreaper.modules.minio.Minio");
    }

}
