import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchAllRealtime, fetchStocks } from '../services/api';
import { ArrowUp, ArrowDown, Activity } from 'lucide-react';
import clsx from 'clsx';

interface SidebarProps {
    onSelect: (code: string) => void;
    selectedCode: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ onSelect, selectedCode }) => {
    // 轮询获取所有股票的实时行情
    const { data: quotes } = useQuery({
        queryKey: ['allRealtime'],
        queryFn: fetchAllRealtime,
        refetchInterval: 3000, // 每3秒刷新
    });

    const { data: stocks } = useQuery({ queryKey: ['stocks'], queryFn: fetchStocks });

    return (
        <div className="w-80 border-r border-slate-700 bg-terminal-card flex flex-col h-screen">
            <div className="p-4 border-b border-slate-700 flex items-center gap-2">
                <Activity className="text-accent" />
                <h1 className="text-xl font-bold text-white tracking-wider">SCALA QUANT</h1>
            </div>

            <div className="flex-1 overflow-y-auto">
                {stocks?.map(stock => {
                    const quote = quotes?.find(q => q.stockCode === stock.stockCode);
                    const percent = quote ? ((quote.currentPrice - quote.closePrice) / quote.closePrice) * 100 : 0;
                    const isUp = percent >= 0;

                    return (
                        <div
                            key={stock.stockCode}
                            onClick={() => onSelect(stock.stockCode)}
                            className={clsx(
                                "p-4 cursor-pointer border-b border-slate-800 hover:bg-slate-700 transition-colors flex justify-between items-center",
                                selectedCode === stock.stockCode ? "bg-slate-700 border-l-4 border-l-accent" : ""
                            )}
                        >
                            <div>
                                <div className="font-bold text-white">{stock.stockName}</div>
                                <div className="text-xs text-slate-400">{stock.stockCode}</div>
                            </div>
                            <div className="text-right">
                                <div className={clsx("font-mono font-bold", isUp ? "text-stock-up" : "text-stock-down")}>
                                    {quote?.currentPrice.toFixed(2)}
                                </div>
                                <div className={clsx("text-xs flex items-center justify-end", isUp ? "text-stock-up" : "text-stock-down")}>
                                    {isUp ? <ArrowUp size={12} /> : <ArrowDown size={12} />}
                                    {Math.abs(percent).toFixed(2)}%
                                </div>
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};