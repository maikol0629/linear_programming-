package com.lpsolver.backend.controller;

import com.lpsolver.backend.model.ProblemRequest;
import com.lpsolver.backend.model.SolutionResponse;
import com.lpsolver.backend.model.SensitivityAnalysis;
import com.lpsolver.backend.model.SimplexProblemRequest;
import com.lpsolver.backend.model.SimplexResponse;
import com.lpsolver.backend.service.GraphicalSensitivityAnalysisService;
import com.lpsolver.backend.service.GraphicalSolverService;
import com.lpsolver.backend.service.SensitivityAnalysisService;
import com.lpsolver.backend.service.SimplexSolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solve")
@CrossOrigin(origins = "*") // Allow frontend to call
public class SolverController {

    private final GraphicalSolverService graphicalSolverService;
    private final GraphicalSensitivityAnalysisService graphicalSensitivityAnalysisService;
    private final SimplexSolverService simplexSolverService;
    private final SensitivityAnalysisService sensitivityAnalysisService;

    @Autowired
    public SolverController(GraphicalSolverService graphicalSolverService, GraphicalSensitivityAnalysisService graphicalSensitivityAnalysisService,
                            SimplexSolverService simplexSolverService, SensitivityAnalysisService sensitivityAnalysisService) {
        this.graphicalSolverService = graphicalSolverService;
        this.graphicalSensitivityAnalysisService = graphicalSensitivityAnalysisService;
        this.simplexSolverService = simplexSolverService;
        this.sensitivityAnalysisService = sensitivityAnalysisService;
    }

    @PostMapping("/graphical")
    public SolutionResponse solveGraphical(@RequestBody ProblemRequest request) {
        return graphicalSolverService.solve(request);
    }

    @PostMapping("/simplex")
    public SimplexResponse solveSimplex(@RequestBody SimplexProblemRequest request) {
        return simplexSolverService.solve(request);
    }

    @PostMapping("/simplex/sensitivity")
    public ResponseEntity<?> analyzeSimplexSensitivity(@RequestBody SimplexProblemRequest request) {
        try {
            SimplexResponse simplexResponse = simplexSolverService.solve(request);
            if (!"OPTIMAL".equals(simplexResponse.getStatus())) {
                return ResponseEntity.badRequest().body("Sensitivity analysis requires an optimal solution. Current status: " + simplexResponse.getStatus());
            }
            SensitivityAnalysis analysis = sensitivityAnalysisService.analyzeSensitivity(simplexResponse, request);
            return ResponseEntity.ok(analysis);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error analyzing sensitivity: " + ex.getMessage());
        }
    }

    @PostMapping("/graphical/sensitivity")
    public ResponseEntity<?> analyzeGraphicalSensitivity(@RequestBody ProblemRequest request) {
        try {
            SolutionResponse graphicalResponse = graphicalSolverService.solve(request);
            if (!"OPTIMAL".equals(graphicalResponse.getStatus())) {
                return ResponseEntity.badRequest().body("Sensitivity analysis requires an optimal solution. Current status: " + graphicalResponse.getStatus());
            }
            SensitivityAnalysis analysis = graphicalSensitivityAnalysisService.analyzeSensitivity(request, graphicalResponse);
            return ResponseEntity.ok(analysis);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error analyzing graphical sensitivity: " + ex.getMessage());
        }
    }
}
