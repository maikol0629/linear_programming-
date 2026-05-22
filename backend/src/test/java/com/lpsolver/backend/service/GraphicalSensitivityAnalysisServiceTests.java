package com.lpsolver.backend.service;

import com.lpsolver.backend.model.ConstraintRequest;
import com.lpsolver.backend.model.ObjectiveCoeffRange;
import com.lpsolver.backend.model.ProblemRequest;
import com.lpsolver.backend.model.RHSRange;
import com.lpsolver.backend.model.SensitivityAnalysis;
import com.lpsolver.backend.model.SolutionResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphicalSensitivityAnalysisServiceTests {

    @Test
    void analyzeSensitivityReturnsRangesForSimpleGraphicalProblem() {
        ProblemRequest request = new ProblemRequest();
        request.setObjectiveType("MAX");
        request.setObjX(3.0);
        request.setObjY(2.0);

        ConstraintRequest c1 = new ConstraintRequest();
        c1.setX(2.0);
        c1.setY(1.0);
        c1.setOperator("<=");
        c1.setValue(18.0);

        ConstraintRequest c2 = new ConstraintRequest();
        c2.setX(2.0);
        c2.setY(3.0);
        c2.setOperator("<=");
        c2.setValue(42.0);

        request.setConstraints(List.of(c1, c2));

        GraphicalSolverService solverService = new GraphicalSolverService();
        SolutionResponse solution = solverService.solve(request);
        assertEquals("OPTIMAL", solution.getStatus());
        assertNotNull(solution.getOptimalPoint());

        GraphicalSensitivityAnalysisService sensitivityService = new GraphicalSensitivityAnalysisService(solverService);
        SensitivityAnalysis analysis = sensitivityService.analyzeSensitivity(request, solution);

        assertNotNull(analysis);
        assertNotNull(analysis.getObjectiveCoefficientsRanges());
        assertEquals(2, analysis.getObjectiveCoefficientsRanges().size());
        assertNotNull(analysis.getRhsRanges());
        assertEquals(2, analysis.getRhsRanges().size());
        assertNotNull(analysis.getShadowPrices());
        assertEquals(2, analysis.getShadowPrices().size());
        assertFalse(analysis.getDegeneracyWarning().isDegenerateSolution());

        for (ObjectiveCoeffRange range : analysis.getObjectiveCoefficientsRanges()) {
            assertTrue(range.getMinRange() <= range.getMaxRange());
        }
        for (RHSRange rhsRange : analysis.getRhsRanges()) {
            assertTrue(rhsRange.getMinRange() <= rhsRange.getMaxRange());
        }
    }
}
