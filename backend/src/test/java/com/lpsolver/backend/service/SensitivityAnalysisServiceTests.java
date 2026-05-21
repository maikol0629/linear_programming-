package com.lpsolver.backend.service;

import com.lpsolver.backend.model.SimplexProblemRequest;
import com.lpsolver.backend.model.SimplexConstraintRequest;
import com.lpsolver.backend.model.SimplexResponse;
import com.lpsolver.backend.model.SensitivityAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SensitivityAnalysisServiceTests {

    @Test
    void analyzeSensitivity_returnsValidRangesForOptimalSimplexResponse() {
        SimplexProblemRequest request = new SimplexProblemRequest();
        request.setObjectiveType("MAX");
        request.setObjectiveCoefficients(List.of(3.0, 5.0));

        SimplexConstraintRequest c1 = new SimplexConstraintRequest();
        c1.setCoefficients(List.of(1.0, 0.0));
        c1.setOperator("<=");
        c1.setValue(4.0);

        SimplexConstraintRequest c2 = new SimplexConstraintRequest();
        c2.setCoefficients(List.of(0.0, 2.0));
        c2.setOperator("<=");
        c2.setValue(12.0);

        request.setConstraints(List.of(c1, c2));

        SimplexResponse response = new SimplexResponse();
        response.setStatus("OPTIMAL");
        response.setOptimalSolution(List.of(4.0, 6.0));
        response.setOptimalValue(42.0);
        response.setFinalRowHeaders(List.of("s1", "s2", "Z"));
        response.setFinalColumnHeaders(List.of("x1", "x2", "s1", "s2", "RHS"));
        response.setFinalTableau(new double[][]{
                {1.0, 0.0, 1.0, 0.0, 4.0},
                {0.0, 1.0, 0.0, 2.0, 12.0},
                {-3.0, -5.0, 0.0, 0.0, 42.0}
        });

        SensitivityAnalysisService service = new SensitivityAnalysisService();
        SensitivityAnalysis analysis = service.analyzeSensitivity(response, request);

        assertNotNull(analysis);
        assertNotNull(analysis.getObjectiveCoefficientsRanges());
        assertEquals(2, analysis.getObjectiveCoefficientsRanges().size());
        assertNotNull(analysis.getShadowPrices());
        assertEquals(2, analysis.getShadowPrices().size());
        assertNotNull(analysis.getReducedCosts());
        assertEquals(2, analysis.getReducedCosts().size());
        assertNotNull(analysis.getRhsRanges());
        assertEquals(2, analysis.getRhsRanges().size());

        assertTrue(analysis.getObjectiveCoefficientsRanges().stream().allMatch(range -> range.getMinRange() <= range.getMaxRange()));
        assertTrue(analysis.getRhsRanges().stream().allMatch(range -> range.getMinRange() <= range.getMaxRange()));
    }
}
