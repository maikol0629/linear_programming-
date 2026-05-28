import { Fragment, useState } from 'react';
import SensitivityReport from './SensitivityReport';
import type { SensitivityAnalysis } from '../types/sensitivity';

interface SimplexConstraint {
  id: string;
  coefficients: number[];
  operator: '<=' | '>=' | '=';
  value: number;
}

interface SimplexStep {
  iteration: number;
  description: string;
  tableau: number[][];
  rowHeaders: string[];
  columnHeaders: string[];
  pivotRow: number | null;
  pivotCol: number | null;
  enteringVariable: string | null;
  leavingVariable: string | null;
}

interface SimplexResponse {
  status: string;
  optimalSolution: number[];
  optimalValue: number;
  steps: SimplexStep[];
}

export default function SimplexSolver() {
  const [numVars, setNumVars] = useState(2);
  const [sensitivity, setSensitivity] = useState<SensitivityAnalysis | null>(null);
  const [sensitivityLoading, setSensitivityLoading] = useState(false);
  const [sensitivityError, setSensitivityError] = useState<string | null>(null);
  const [objectiveType, setObjectiveType] = useState<'MAX' | 'MIN'>('MAX');
  const [objectiveCoefficients, setObjectiveCoefficients] = useState<number[]>([3, 2]);
  const [constraints, setConstraints] = useState<SimplexConstraint[]>([
    { id: '1', coefficients: [2, 1], operator: '<=', value: 18 },
    { id: '2', coefficients: [2, 3], operator: '<=', value: 42 },
    { id: '3', coefficients: [3, 1], operator: '<=', value: 24 }
  ]);
  
  const [solverMethod, setSolverMethod] = useState<'TWO_PHASE' | 'BIG_M'>('TWO_PHASE');
  
  const [solution, setSolution] = useState<SimplexResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const handleNumVarsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseInt(e.target.value);
    if (val < 2 || val > 10 || isNaN(val)) return;
    setNumVars(val);
    
    // adjust obj coeffs
    const newObj = [...objectiveCoefficients];
    while (newObj.length < val) newObj.push(0);
    setObjectiveCoefficients(newObj.slice(0, val));
    
    // adjust constraints
    const newConstraints = constraints.map(c => {
      const newCoeffs = [...c.coefficients];
      while (newCoeffs.length < val) newCoeffs.push(0);
      return { ...c, coefficients: newCoeffs.slice(0, val) };
    });
    setConstraints(newConstraints);
  };

  const addConstraint = () => {
    setConstraints([
      ...constraints,
      { id: Date.now().toString(), coefficients: Array(numVars).fill(1), operator: '<=', value: 10 }
    ]);
  };

  const removeConstraint = (id: string) => {
    setConstraints(constraints.filter(c => c.id !== id));
  };

  const solveProblem = async () => {
    setLoading(true);
    setCurrentStepIndex(0);
    setSolution(null);
    setSensitivity(null);
    setSensitivityError(null);
    try {
      const response = await fetch('/api/solve/simplex', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          objectiveType,
          objectiveCoefficients,
          solverMethod,
          constraints: constraints.map(c => ({
            coefficients: c.coefficients,
            operator: c.operator,
            value: c.value
          }))
        }),
      });
      const data: SimplexResponse = await response.json();
      setSolution(data);
    } catch (error) {
      console.error('Error solving simplex problem:', error);
      alert('Error al conectar con el servidor.');
    } finally {
      setLoading(false);
    }
  };

  const analyzeSensitivity = async () => {
    if (!solution) return;
    setSensitivityLoading(true);
    setSensitivityError(null);
    setSensitivity(null);
    try {
      const response = await fetch('/api/solve/simplex/sensitivity', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          objectiveType,
          objectiveCoefficients,
          solverMethod,
          constraints: constraints.map(c => ({
            coefficients: c.coefficients,
            operator: c.operator,
            value: c.value
          }))
        }),
      });
      if (!response.ok) {
        const text = await response.text();
        setSensitivityError(text || 'El análisis de sensibilidad falló.');
      } else {
        const data: SensitivityAnalysis = await response.json();
        setSensitivity(data);
      }
    } catch (error) {
      console.error('Error running sensitivity analysis:', error);
      setSensitivityError('Error al conectar con el servidor para el análisis de sensibilidad.');
    } finally {
      setSensitivityLoading(false);
    }
  };

  const renderTableau = (step: SimplexStep) => {
    if (!step || !step.tableau) return null;
    return (
      <div className="overflow-x-auto custom-scrollbar mt-4">
        <table className="w-full text-sm text-left text-white/80 border-collapse">
          <thead className="text-xs uppercase bg-black/40 text-purple-300">
            <tr>
              <th className="px-4 py-3 border border-white/10">Base</th>
              {step.columnHeaders.map((header, idx) => (
                <th key={idx} className={`px-4 py-3 border border-white/10 text-center ${step.pivotCol === idx ? 'bg-pink-500/20 text-pink-300' : ''}`}>{header}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {step.tableau.map((row, rIdx) => (
              <tr key={rIdx} className={`bg-black/20 border border-white/10 ${step.pivotRow === rIdx ? 'bg-indigo-500/20' : ''}`}>
                <td className="px-4 py-2 border border-white/10 font-bold text-indigo-300">
                  {step.rowHeaders[rIdx]}
                </td>
                {row.map((cell, cIdx) => {
                  const isPivot = step.pivotRow === rIdx && step.pivotCol === cIdx;
                  return (
                    <td key={cIdx} className={`px-4 py-2 border border-white/10 text-center font-mono ${isPivot ? 'bg-pink-500/40 text-white font-bold ring-2 ring-pink-400' : ''}`}>
                      {Number(cell.toFixed(3))}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 text-white font-sans p-8">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="text-center space-y-4">
          <h1 className="text-5xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-indigo-400">
            Método Simplex
          </h1>
          <p className="text-xl text-indigo-200/80 font-light">
            Solucionador Paso a Paso con Tableros
          </p>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Input Panel */}
          <div className="lg:col-span-5 space-y-6 bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl">
            <h2 className="text-2xl font-semibold mb-4 text-emerald-300">Configuración del Problema</h2>
            
            <div className="flex flex-col sm:flex-row items-center gap-4 bg-black/20 p-4 rounded-xl border border-white/5 justify-between">
              <div className="flex items-center gap-4">
                <span className="text-indigo-300 font-semibold whitespace-nowrap">Variables (n):</span>
                <input 
                  type="number" min="2" max="10"
                  className="w-16 bg-indigo-900/50 border border-indigo-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-emerald-500 outline-none transition-all"
                  value={numVars}
                  onChange={handleNumVarsChange}
                />
              </div>
              <div className="flex items-center bg-indigo-950/80 p-1 rounded-xl border border-indigo-500/20 shadow-inner">
                <button
                  onClick={() => setSolverMethod('TWO_PHASE')}
                  className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all duration-300 ease-out ${
                    solverMethod === 'TWO_PHASE' 
                      ? 'bg-emerald-500 text-white shadow-[0_0_10px_rgba(16,185,129,0.3)]' 
                      : 'text-indigo-300/60 hover:text-indigo-200'
                  }`}
                >
                    Dos Fases
                </button>
                <button
                  onClick={() => setSolverMethod('BIG_M')}
                  className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all duration-300 ease-out ${
                    solverMethod === 'BIG_M' 
                      ? 'bg-pink-500 text-white shadow-[0_0_10px_rgba(236,72,153,0.3)]' 
                      : 'text-indigo-300/60 hover:text-indigo-200'
                  }`}
                >
                  Gran M
                </button>
              </div>
            </div>

            {/* Objective Function */}
            <div className="space-y-4 bg-black/20 p-5 rounded-2xl border border-white/5 overflow-x-auto">
              <h3 className="text-sm uppercase tracking-wider text-indigo-300 font-semibold">Función Objetivo</h3>
              <div className="flex items-center gap-3 min-w-max">
                <select 
                  className="bg-indigo-900/50 border border-indigo-500/30 text-white rounded-xl px-3 py-2 focus:ring-2 focus:ring-emerald-500 outline-none"
                  value={objectiveType}
                  onChange={(e) => setObjectiveType(e.target.value as 'MAX' | 'MIN')}
                >
                  <option value="MAX">Max</option>
                  <option value="MIN">Min</option>
                </select>
                <span className="text-lg font-bold">Z =</span>
                {objectiveCoefficients.map((c, i) => (
                  <Fragment key={i}>
                    <input 
                      type="number" 
                      className="w-16 bg-indigo-900/50 border border-indigo-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-emerald-500 outline-none"
                      value={c}
                      onChange={(e) => {
                        const newC = [...objectiveCoefficients];
                        newC[i] = Number(e.target.value);
                        setObjectiveCoefficients(newC);
                      }}
                    />
                    <span>x<sub>{i+1}</sub> {i < numVars - 1 ? '+' : ''}</span>
                  </Fragment>
                ))}
              </div>
            </div>

            {/* Constraints */}
            <div className="space-y-4 bg-black/20 p-5 rounded-2xl border border-white/5">
              <div className="flex justify-between items-center">
                <h3 className="text-sm uppercase tracking-wider text-indigo-300 font-semibold">Restricciones</h3>
                <button 
                  onClick={addConstraint}
                  className="bg-emerald-500/20 hover:bg-emerald-500/40 text-emerald-300 border border-emerald-500/30 rounded-full px-3 py-1 text-sm transition-all"
                >
                  + Agregar
                </button>
              </div>
              
              <div className="space-y-3 overflow-x-auto">
                {constraints.map((c, index) => (
                  <div key={c.id} className="flex items-center gap-3 group min-w-max">
                    <span className="text-white/50 w-6">#{index + 1}</span>
                    {c.coefficients.map((coeff, i) => (
                      <Fragment key={i}>
                        <input 
                          type="number" 
                          className="w-14 bg-indigo-900/50 border border-indigo-500/30 text-center text-white rounded-lg px-1 py-1.5 focus:ring-2 focus:ring-emerald-500 outline-none"
                          value={coeff}
                          onChange={(e) => {
                            const newC = [...constraints];
                            newC[index].coefficients[i] = Number(e.target.value);
                            setConstraints(newC);
                          }}
                        />
                        <span>x<sub>{i+1}</sub> {i < numVars - 1 ? '+' : ''}</span>
                      </Fragment>
                    ))}
                    <select 
                      className="bg-indigo-900/50 border border-indigo-500/30 text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-emerald-500 outline-none"
                      value={c.operator}
                      onChange={(e) => {
                        const newC = [...constraints];
                        newC[index].operator = e.target.value as any;
                        setConstraints(newC);
                      }}
                    >
                      <option value="<=">&le;</option>
                      <option value=">=">&ge;</option>
                      <option value="=">=</option>
                    </select>
                    <input 
                      type="number" 
                      className="w-16 bg-indigo-900/50 border border-indigo-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-emerald-500 outline-none"
                      value={c.value}
                      onChange={(e) => {
                        const newC = [...constraints];
                        newC[index].value = Number(e.target.value);
                        setConstraints(newC);
                      }}
                    />
                    <button onClick={() => removeConstraint(c.id)} className="text-red-400 opacity-0 group-hover:opacity-100 transition-opacity ml-2">✕</button>
                  </div>
                ))}
              </div>
            </div>

            <button 
              onClick={solveProblem}
              disabled={loading}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-emerald-500 to-indigo-500 text-white font-bold text-lg hover:from-emerald-400 hover:to-indigo-400 transition-all shadow-[0_0_30px_-5px_rgba(16,185,129,0.5)] disabled:opacity-50"
            >
              {loading ? 'Resolviendo...' : 'Resolver con Simplex ✨'}
            </button>
          </div>

          {/* Visualization Panel */}
          <div className="lg:col-span-7 space-y-6">
            {solution && solution.steps.length > 0 ? (
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl animate-fade-in-up">
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-bold text-indigo-300">
                    Iteration {solution.steps[currentStepIndex].iteration}
                  </h2>
                  <div className="flex gap-2">
                    <button 
                      onClick={() => setCurrentStepIndex(Math.max(0, currentStepIndex - 1))}
                      disabled={currentStepIndex === 0}
                      className="px-4 py-2 rounded-lg bg-indigo-500/20 text-indigo-300 disabled:opacity-30 hover:bg-indigo-500/40 transition-colors"
                    >
                       Anterior
                    </button>
                    <button 
                      onClick={() => setCurrentStepIndex(Math.min(solution.steps.length - 1, currentStepIndex + 1))}
                      disabled={currentStepIndex === solution.steps.length - 1}
                      className="px-4 py-2 rounded-lg bg-emerald-500/20 text-emerald-300 disabled:opacity-30 hover:bg-emerald-500/40 transition-colors"
                    >
                       Siguiente
                    </button>
                  </div>
                </div>

                <div className="bg-black/30 p-4 rounded-xl border-l-4 border-emerald-500 mb-6">
                  <p className="text-white/90 text-lg">{solution.steps[currentStepIndex].description}</p>
                </div>

                {renderTableau(solution.steps[currentStepIndex])}

                {currentStepIndex === solution.steps.length - 1 && (
                  <div className="mt-8 p-6 bg-gradient-to-r from-emerald-500/20 to-indigo-500/20 rounded-2xl border border-emerald-500/30">
                    <h3 className="text-xl font-bold text-emerald-400 mb-2">Solución Final ({solution.status === 'OPTIMAL' ? 'ÓPTIMO' : solution.status === 'UNBOUNDED' ? 'NO ACOTADO' : solution.status})</h3>
                    {solution.status === 'OPTIMAL' ? (
                      <div className="space-y-6">
                        <div className="grid grid-cols-2 gap-4">
                          <div className="text-lg">
                            <span className="text-white/60">Z Óptimo = </span>
                            <span className="font-bold text-emerald-300">{Number(solution.optimalValue.toFixed(3))}</span>
                          </div>
                          <div className="text-lg">
                            {solution.optimalSolution.map((val, idx) => (
                              <div key={idx}>
                                <span className="text-white/60">x<sub>{idx+1}</sub> = </span>
                                <span className="font-bold text-indigo-300">{Number(val.toFixed(3))}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                        <div className="flex flex-col sm:flex-row gap-3">
                          <button
                            onClick={analyzeSensitivity}
                            disabled={sensitivityLoading}
                            className="w-full sm:w-auto px-6 py-3 rounded-2xl bg-indigo-500 text-white font-semibold hover:bg-indigo-400 transition-all disabled:opacity-40"
                          >
                            {sensitivityLoading ? 'Analizando...' : 'Analizar Sensibilidad'}
                          </button>
                          <span className="text-sm text-slate-300">Ejecute análisis adicional sobre los coeficientes objetivo y restricciones.</span>
                        </div>
                        {sensitivity && (
                          <div className="mt-6">
                            <SensitivityReport analysis={sensitivity} loading={false} />
                          </div>
                        )}
                        {sensitivityError && (
                          <div className="mt-4 text-sm text-red-300">{sensitivityError}</div>
                        )}
                      </div>
                    ) : (
                      <p className="text-red-400">El problema es No Acotado.</p>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl h-[500px] flex items-center justify-center relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/5 to-emerald-500/5 z-0"></div>
                <div className="text-center z-10">
                  <p className="text-2xl text-white/40 font-light mb-4">Visualización del Tablero</p>
                  <p className="text-white/20">Defina su problema y haga clic en resolver para ver el método Simplex paso a paso.</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
