package com.lpsolver.backend.controller;

import com.lpsolver.backend.model.ProblemRequest;
import com.lpsolver.backend.model.SolutionResponse;
import com.lpsolver.backend.model.SimplexProblemRequest;
import com.lpsolver.backend.model.SimplexResponse;
import com.lpsolver.backend.service.GraphicalSolverService;
import com.lpsolver.backend.service.SimplexSolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solve")
@CrossOrigin(origins = "*") // Allow frontend to call
public class SolverController {

    private final GraphicalSolverService graphicalSolverService;
    private final SimplexSolverService simplexSolverService;

    @Autowired
    public SolverController(GraphicalSolverService graphicalSolverService, SimplexSolverService simplexSolverService) {
        this.graphicalSolverService = graphicalSolverService;
        this.simplexSolverService = simplexSolverService;
    }

    @PostMapping("/graphical")
    public SolutionResponse solveGraphical(@RequestBody ProblemRequest request) {
        return graphicalSolverService.solve(request);
    }

    @PostMapping("/simplex")
    public SimplexResponse solveSimplex(@RequestBody SimplexProblemRequest request) {
        return simplexSolverService.solve(request);
    }
}
