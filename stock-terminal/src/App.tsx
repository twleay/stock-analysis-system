import { useState, useEffect } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Sidebar } from './components/Sidebar';
import { Dashboard } from './components/Dashboard';
import { StockGrid } from './components/StockGrid';
import { fetchStocks, Stock } from './services/api';
import { LayoutGrid, Maximize2 } from 'lucide-react';

const queryClient = new QueryClient();

function AppContent() {
    const [selectedCode, setSelectedCode] = useState<string>('sh600519');
    const [stocks, setStocks] = useState<Stock[]>([]);
    const [viewMode, setViewMode] = useState<'single' | 'grid'>('single');

    useEffect(() => {
        fetchStocks().then(res => {
            setStocks(res);
            if (!selectedCode && res.length > 0) setSelectedCode(res[0].stockCode);
        }).catch(console.error);
    }, []);

    const selectedStock = stocks.find(s => s.stockCode === selectedCode);

    const handleSelectStock = (code: string) => {
        setSelectedCode(code);
        setViewMode('single');
    };

    return (
        <div className="flex h-screen w-screen bg-terminal-bg text-white overflow-hidden">
            <Sidebar
                onSelect={handleSelectStock}
                selectedCode={selectedCode}
            />

            <main className="flex-1 h-full flex flex-col relative">
                {/* --- 修改此处：将 top-4 right-6 改为 bottom-4 left-6 --- */}
                <div className="absolute bottom-4 left-6 z-50 flex bg-terminal-card border border-slate-700 rounded-lg p-1 shadow-xl">
                    <button
                        onClick={() => setViewMode('single')}
                        className={`p-2 rounded transition-colors ${viewMode === 'single' ? 'bg-accent text-white' : 'text-slate-400 hover:text-white hover:bg-slate-700'}`}
                        title="单股详情"
                    >
                        <Maximize2 size={18} />
                    </button>
                    <button
                        onClick={() => setViewMode('grid')}
                        className={`p-2 rounded transition-colors ${viewMode === 'grid' ? 'bg-accent text-white' : 'text-slate-400 hover:text-white hover:bg-slate-700'}`}
                        title="多股概览"
                    >
                        <LayoutGrid size={18} />
                    </button>
                </div>
                {/* -------------------------------------------------- */}

                {viewMode === 'single' ? (
                    <Dashboard stock={selectedStock || stocks[0]} />
                ) : (
                    <StockGrid stocks={stocks} onSelect={handleSelectStock} />
                )}
            </main>
        </div>
    );
}

function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <AppContent />
        </QueryClientProvider>
    );
}

export default App;