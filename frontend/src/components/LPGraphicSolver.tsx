import { useState } from 'react';
import PlotlyPlot from 'react-plotly.js';

const Plot = (PlotlyPlot as any).default || PlotlyPlot;

interface Constraint {
  id: string;
  x: number;
  y: number;
  operator: '<=' | '>=' | '=';
  value: number;
}

interface Point {
  x: number;
  y: number;
  z: number;
}

interface SolutionResponse {
  status: string;
  optimalPoint: Point | null;
  optimalValue: number;
  feasibleVertices: Point[];
  steps: string[];
}

export default function LPGraphicSolver() {
  const [objectiveType, setObjectiveType] = useState<'MAX' | 'MIN'>('MAX');
  const [objX, setObjX] = useState<number>(3);
  const [objY, setObjY] = useState<number>(2);
  const [constraints, setConstraints] = useState<Constraint[]>([
    { id: '1', x: 2, y: 1, operator: '<=', value: 18 },
    { id: '2', x: 2, y: 3, operator: '<=', value: 42 },
    { id: '3', x: 3, y: 1, operator: '<=', value: 24 }
  ]);
  const [solution, setSolution] = useState<SolutionResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const addConstraint = () => {
    setConstraints([
      ...constraints,
      { id: Date.now().toString(), x: 1, y: 1, operator: '<=', value: 10 }
    ]);
  };

  const removeConstraint = (id: string) => {
    setConstraints(constraints.filter(c => c.id !== id));
  };

  const solveProblem = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/solve/graphical', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          objectiveType,
          objX,
          objY,
          constraints: constraints.map(({ x, y, operator, value }) => ({ x, y, operator, value }))
        }),
      });
      const data: SolutionResponse = await response.json();
      setSolution(data);
    } catch (error) {
      console.error('Error solving problem:', error);
      alert('Failed to connect to the solver backend. Make sure it is running on port 8080.');
    } finally {
      setLoading(false);
    }
  };

  // Helper function to solve and generate Plotly data
  const generatePlotData = () => {
    const data: any[] = [];
    const xMax = solution && solution.feasibleVertices.length > 0 
      ? Math.max(...solution.feasibleVertices.map(v => v.x)) * 1.5 + 5 
      : 20;
    const xValues = [0, xMax]; 

    constraints.forEach((c, idx) => {
      if (c.y !== 0) {
        const yValues = xValues.map(x => (c.value - c.x * x) / c.y);
        data.push({
          x: xValues,
          y: yValues,
          type: 'scatter',
          mode: 'lines',
          name: `C${idx + 1}: ${c.x}x + ${c.y}y ${c.operator} ${c.value}`,
          line: { width: 2 }
        });
      } else if (c.x !== 0) {
        // vertical line
        const xVal = c.value / c.x;
        data.push({
          x: [xVal, xVal],
          y: [0, xMax], // approx y range
          type: 'scatter',
          mode: 'lines',
          name: `C${idx + 1}: ${c.x}x ${c.operator} ${c.value}`,
          line: { width: 2 }
        });
      }
    });

    // Plot feasible vertices
    if (solution && solution.feasibleVertices && solution.feasibleVertices.length > 0) {
      // sort vertices for polygon drawing
      const cx = solution.feasibleVertices.reduce((sum, p) => sum + p.x, 0) / solution.feasibleVertices.length;
      const cy = solution.feasibleVertices.reduce((sum, p) => sum + p.y, 0) / solution.feasibleVertices.length;
      
      const sortedVertices = [...solution.feasibleVertices].sort((a, b) => {
        return Math.atan2(a.y - cy, a.x - cx) - Math.atan2(b.y - cy, b.x - cx);
      });
      // Close the polygon
      sortedVertices.push(sortedVertices[0]);

      data.push({
        x: sortedVertices.map(v => v.x),
        y: sortedVertices.map(v => v.y),
        fill: 'toself',
        fillcolor: 'rgba(236, 72, 153, 0.2)', // Pink transparent
        line: { color: 'rgba(236, 72, 153, 0.5)' },
        name: 'Feasible Region',
        type: 'scatter',
        mode: 'lines+markers'
      });

      // Plot Optimal Point
      if (solution.optimalPoint) {
        data.push({
          x: [solution.optimalPoint.x],
          y: [solution.optimalPoint.y],
          mode: 'markers',
          type: 'scatter',
          name: `Optimal: (${solution.optimalPoint.x.toFixed(2)}, ${solution.optimalPoint.y.toFixed(2)}) Z=${solution.optimalValue.toFixed(2)}`,
          marker: { size: 12, color: '#10b981', symbol: 'star' } // Emerald green star
        });
      }
    }

    return data;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white font-sans p-8">
      <div className="max-w-6xl mx-auto space-y-8">
        <header className="text-center space-y-4">
          <h1 className="text-5xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-pink-400 to-indigo-400">
            OptiSolve
          </h1>
          <p className="text-xl text-purple-200/80 font-light">
            Interactive Linear Programming Solver
          </p>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Input Panel */}
          <div className="lg:col-span-5 space-y-6 bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl">
            <h2 className="text-2xl font-semibold mb-4 text-pink-300">Problem Definition</h2>
            
            {/* Objective Function */}
            <div className="space-y-4 bg-black/20 p-5 rounded-2xl border border-white/5">
              <h3 className="text-sm uppercase tracking-wider text-purple-300 font-semibold">Objective Function</h3>
              <div className="flex items-center gap-4">
                <select 
                  className="bg-purple-900/50 border border-purple-500/30 text-white rounded-xl px-4 py-2 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                  value={objectiveType}
                  onChange={(e) => setObjectiveType(e.target.value as 'MAX' | 'MIN')}
                >
                  <option value="MAX">Maximize</option>
                  <option value="MIN">Minimize</option>
                </select>
                <span className="text-xl font-bold">Z =</span>
                <input 
                  type="number" 
                  className="w-20 bg-purple-900/50 border border-purple-500/30 text-center text-white rounded-xl px-2 py-2 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                  value={objX}
                  onChange={(e) => setObjX(Number(e.target.value))}
                />
                <span className="text-xl">x +</span>
                <input 
                  type="number" 
                  className="w-20 bg-purple-900/50 border border-purple-500/30 text-center text-white rounded-xl px-2 py-2 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                  value={objY}
                  onChange={(e) => setObjY(Number(e.target.value))}
                />
                <span className="text-xl">y</span>
              </div>
            </div>

            {/* Constraints */}
            <div className="space-y-4 bg-black/20 p-5 rounded-2xl border border-white/5">
              <div className="flex justify-between items-center">
                <h3 className="text-sm uppercase tracking-wider text-purple-300 font-semibold">Constraints</h3>
                <button 
                  onClick={addConstraint}
                  className="bg-indigo-500/20 hover:bg-indigo-500/40 text-indigo-300 border border-indigo-500/30 rounded-full px-3 py-1 text-sm transition-all shadow-lg hover:shadow-indigo-500/20"
                >
                  + Add
                </button>
              </div>
              
              <div className="space-y-3">
                {constraints.map((c, index) => (
                  <div key={c.id} className="flex items-center gap-3 group">
                    <span className="text-white/50 w-6">#{index + 1}</span>
                    <input 
                      type="number" 
                      className="w-16 bg-purple-900/50 border border-purple-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                      value={c.x}
                      onChange={(e) => {
                        const newC = [...constraints];
                        newC[index].x = Number(e.target.value);
                        setConstraints(newC);
                      }}
                    />
                    <span>x +</span>
                    <input 
                      type="number" 
                      className="w-16 bg-purple-900/50 border border-purple-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                      value={c.y}
                      onChange={(e) => {
                        const newC = [...constraints];
                        newC[index].y = Number(e.target.value);
                        setConstraints(newC);
                      }}
                    />
                    <span>y</span>
                    <select 
                      className="bg-purple-900/50 border border-purple-500/30 text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
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
                      className="w-20 bg-purple-900/50 border border-purple-500/30 text-center text-white rounded-lg px-2 py-1.5 focus:ring-2 focus:ring-pink-500 outline-none transition-all"
                      value={c.value}
                      onChange={(e) => {
                        const newC = [...constraints];
                        newC[index].value = Number(e.target.value);
                        setConstraints(newC);
                      }}
                    />
                    <button 
                      onClick={() => removeConstraint(c.id)}
                      className="text-red-400 opacity-0 group-hover:opacity-100 transition-opacity hover:text-red-300 ml-2"
                    >
                      ✕
                    </button>
                  </div>
                ))}
                <div className="text-white/60 text-sm mt-4 pl-9">
                  x, y &ge; 0 (Implicit)
                </div>
              </div>
            </div>

            <button 
              onClick={solveProblem}
              disabled={loading}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-pink-500 to-indigo-500 text-white font-bold text-lg hover:from-pink-400 hover:to-indigo-400 transition-all shadow-[0_0_30px_-5px_rgba(236,72,153,0.5)] hover:shadow-[0_0_40px_0px_rgba(236,72,153,0.6)] disabled:opacity-50"
            >
              {loading ? 'Solving...' : 'Solve Problem ✨'}
            </button>
          </div>

          {/* Visualization Panel */}
          <div className="lg:col-span-7 space-y-6">
            <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl h-[500px] flex items-center justify-center relative overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 to-pink-500/10 z-0"></div>
              <div className="relative z-10 w-full h-full bg-slate-900/50 rounded-2xl border border-white/5 flex items-center justify-center overflow-hidden">
                <Plot
                  data={generatePlotData()}
                  layout={{
                    autosize: true,
                    margin: { l: 40, r: 20, t: 20, b: 40 },
                    paper_bgcolor: 'rgba(0,0,0,0)',
                    plot_bgcolor: 'rgba(0,0,0,0)',
                    xaxis: { 
                      title: 'x', 
                      gridcolor: 'rgba(255,255,255,0.1)',
                      zerolinecolor: 'rgba(255,255,255,0.3)',
                      color: '#fff',
                      range: [0, Math.max(10, solution ? Math.max(...solution.feasibleVertices.map(v => v.x)) * 1.2 : 20)]
                    },
                    yaxis: { 
                      title: 'y', 
                      gridcolor: 'rgba(255,255,255,0.1)',
                      zerolinecolor: 'rgba(255,255,255,0.3)',
                      color: '#fff',
                      range: [0, Math.max(10, solution ? Math.max(...solution.feasibleVertices.map(v => v.y)) * 1.2 : 20)]
                    },
                    showlegend: true,
                    legend: { font: { color: '#fff' } }
                  }}
                  useResizeHandler={true}
                  style={{ width: '100%', height: '100%' }}
                  config={{ responsive: true, displayModeBar: false }}
                />
              </div>
            </div>

            {/* Explanation Steps */}
            {solution && (
              <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl animate-fade-in-up">
                <div className="flex justify-between items-center mb-4">
                  <h2 className="text-xl font-semibold text-indigo-300">Resolution Status: <span className={solution.status === 'OPTIMAL' ? 'text-emerald-400' : 'text-red-400'}>{solution.status}</span></h2>
                  {solution.optimalPoint && (
                    <div className="bg-emerald-500/20 border border-emerald-500/50 px-4 py-2 rounded-xl text-emerald-300 font-bold">
                      Optimal Z: {solution.optimalValue.toFixed(2)}
                    </div>
                  )}
                </div>
                <div className="space-y-4 max-h-[300px] overflow-y-auto custom-scrollbar pr-2">
                  {solution.steps.map((step, idx) => (
                    <div key={idx} className={`bg-black/20 p-4 rounded-xl border-l-4 relative overflow-hidden group ${idx % 2 === 0 ? 'border-pink-500' : 'border-indigo-500'}`}>
                      <div className={`absolute inset-0 bg-gradient-to-r opacity-0 group-hover:opacity-100 transition-opacity ${idx % 2 === 0 ? 'from-pink-500/10' : 'from-indigo-500/10'} to-transparent`}></div>
                      <h4 className={`font-bold mb-1 ${idx % 2 === 0 ? 'text-pink-300' : 'text-indigo-300'}`}>Step {idx + 1}</h4>
                      <p className="text-white/80 text-sm">{step}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

