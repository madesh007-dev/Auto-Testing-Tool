package com.autotest.mapper;

import com.autotest.dto.TestCaseResponse;
import com.autotest.entity.TestCase;
import org.springframework.stereotype.Component;

@Component
public class TestCaseMapper {

    public TestCaseResponse toResponse(TestCase testCase) {
        if (testCase == null) {
            return null;
        }

        return new TestCaseResponse(
                testCase.getId(),
                testCase.getName(),
                testCase.getDescription(),
                testCase.getTestType(),
                testCase.getConfiguration(),
                testCase.getProject() != null ? testCase.getProject().getId() : null,
                testCase.getProject() != null ? testCase.getProject().getName() : null,
                testCase.getCreatedAt(),
                testCase.getUpdatedAt()
        );
    }
}
