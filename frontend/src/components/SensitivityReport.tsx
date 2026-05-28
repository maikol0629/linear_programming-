import type { SensitivityAnalysis } from '../types/sensitivity';

interface SensitivityReportProps {
  analysis: SensitivityAnalysis | null;
  loading: boolean;
  error?: string;
}

export default function SensitivityReport({ analysis, loading, error }: SensitivityReportProps) {
  if (loading) {
    return (
      <div className="p-6 bg-white/5 rounded-3xl border border-white/10 shadow-lg">
        <p className="text-indigo-200">Calculando análisis de sensibilidad...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 bg-red-500/10 rounded-3xl border border-red-500/20 shadow-lg">
        <p className="text-red-200">{error}</p>
      </div>
    );
  }

  if (!analysis) {
    return (
      <div className="p-6 bg-white/5 rounded-3xl border border-white/10 shadow-lg">
        <p className="text-indigo-200">Ejecute una solución simplex para ver el análisis de sensibilidad.</p>
      </div>
    );
  }

  return (
    <div className="p-6 bg-white/5 rounded-3xl border border-white/10 shadow-lg space-y-6">
      <div className="space-y-2">
        <h2 className="text-2xl font-semibold text-emerald-300">Análisis de Sensibilidad</h2>
        <p className="text-sm text-slate-300 whitespace-pre-wrap">{analysis.analysisNotes}</p>
      </div>

      {analysis.degeneracyWarning?.degenerateSolution && (
        <div className="p-4 bg-yellow-500/10 border border-yellow-500/20 rounded-2xl">
          <p className="text-yellow-200 font-semibold">Advertencia: Solución degenerada detectada.</p>
          <p className="text-sm text-yellow-100">{analysis.degeneracyWarning.recommendation}</p>
          <p className="text-sm text-slate-300">Zero valued variables: {analysis.degeneracyWarning.zeroValuedBasicVariables.join(', ')}</p>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="p-4 bg-slate-950/60 rounded-3xl border border-slate-700/60">
          <h3 className="text-xl font-semibold text-indigo-200 mb-3">Rangos de Coeficientes Objetivo</h3>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm text-slate-200 border-separate border-spacing-y-2">
              <thead>
                <tr className="text-slate-400 uppercase text-xs">
                  <th className="pb-3">Variable</th>
                  <th className="pb-3 text-right">Actual</th>
                  <th className="pb-3 text-right">Mín</th>
                  <th className="pb-3 text-right">Máx</th>
                </tr>
              </thead>
              <tbody>
                {analysis.objectiveCoefficientsRanges.map((range) => (
                  <tr key={range.variableName} className="bg-slate-900/80 rounded-xl">
                    <td className="px-3 py-3 font-medium text-slate-100">{range.variableName}</td>
                    <td className="px-3 py-3 text-right">{isFinite(range.currentValue) ? range.currentValue.toFixed(3) : '∞'}</td>
                    <td className="px-3 py-3 text-right">{isFinite(range.minRange) ? range.minRange.toFixed(3) : '−∞'}</td>
                    <td className="px-3 py-3 text-right">{isFinite(range.maxRange) ? range.maxRange.toFixed(3) : '∞'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <section className="p-4 bg-slate-950/60 rounded-3xl border border-slate-700/60">
          <h3 className="text-xl font-semibold text-indigo-200 mb-3">Rangos RHS y Precios Sombra</h3>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm text-slate-200 border-separate border-spacing-y-2">
              <thead>
                <tr className="text-slate-400 uppercase text-xs">
                  <th className="pb-3">Restricción</th>
                  <th className="pb-3 text-right">RHS</th>
                  <th className="pb-3 text-right">Mín</th>
                  <th className="pb-3 text-right">Máx</th>
                  <th className="pb-3 text-right">Sombra</th>
                </tr>
              </thead>
              <tbody>
                {analysis.rhsRanges.map((rhs) => (
                  <tr key={rhs.constraintIndex} className="bg-slate-900/80 rounded-xl">
                    <td className="px-3 py-3 font-medium text-slate-100">{rhs.constraintName}</td>
                    <td className="px-3 py-3 text-right">{rhs.currentValue.toFixed(3)}</td>
                    <td className="px-3 py-3 text-right">{isFinite(rhs.minRange) ? rhs.minRange.toFixed(3) : '−∞'}</td>
                    <td className="px-3 py-3 text-right">{isFinite(rhs.maxRange) ? rhs.maxRange.toFixed(3) : '∞'}</td>
                    <td className="px-3 py-3 text-right text-emerald-300">{rhs.shadowPrice.toFixed(3)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}
