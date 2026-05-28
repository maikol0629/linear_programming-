import { useState } from 'react'
import LPGraphicSolver from './components/LPGraphicSolver'
import SimplexSolver from './components/SimplexSolver'
import './App.css'

function App() {
  const [activeTab, setActiveTab] = useState<'graphical' | 'simplex'>('graphical')

  return (
    <div className="min-h-screen bg-slate-900 font-sans">
      <nav className="bg-slate-900/50 backdrop-blur-md border-b border-white/10 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-8 py-4 flex gap-4">
          <button
            onClick={() => setActiveTab('graphical')}
            className={`px-6 py-2 rounded-full font-semibold transition-all ${
              activeTab === 'graphical'
                ? 'bg-pink-500 text-white shadow-lg shadow-pink-500/25'
                : 'text-white/60 hover:text-white hover:bg-white/5'
            }`}
          >
            Método Gráfico (2D)
          </button>
          <button
            onClick={() => setActiveTab('simplex')}
            className={`px-6 py-2 rounded-full font-semibold transition-all ${
              activeTab === 'simplex'
                ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/25'
                : 'text-white/60 hover:text-white hover:bg-white/5'
            }`}
          >
            Método Simplex (nD)
          </button>
        </div>
      </nav>
      <main>
        {activeTab === 'graphical' ? <LPGraphicSolver /> : <SimplexSolver />}
      </main>
    </div>
  )
}

export default App
