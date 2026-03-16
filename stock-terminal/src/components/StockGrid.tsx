import React from 'react';
// 1. 引入 UseQueryResult 类型
import { useQueries, UseQueryResult } from '@tanstack/react-query';
import { fetchKLine, fetchRealtime, Stock, KLineData, RealtimeQuote } from '../services/api';
import { KLineChart } from './KLineChart';
import clsx from 'clsx';

interface StockGridProps {
    stocks: Stock[];
    onSelect: (code: string) => void;
}

export const StockGrid: React.FC<StockGridProps> = ({ stocks, onSelect }) => {
    // 2. 这里的 as ... 是解决 "未解析变量 data" 的关键
    const klineQueries = useQueries({
        queries: stocks.map(stock => ({
            queryKey: ['kline', stock.stockCode],
            queryFn: () => fetchKLine(stock.stockCode),
            staleTime: 60000,
        }))
    }) as UseQueryResult<KLineData[], Error>[];

    // 3. 这里的 as ... 也是同理
    const quoteQueries = useQueries({
        queries: stocks.map(stock => ({
            queryKey: ['realtime', stock.stockCode],
            queryFn: () => fetchRealtime(stock.stockCode),
            refetchInterval: 2000,
        }))
    }) as UseQueryResult<RealtimeQuote, Error>[];

    if (!stocks || stocks.length === 0) return <div className="p-6 text-slate-500">暂无股票数据</div>;

    return (
        <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 h-full overflow-y-auto pb-20">
            {stocks.map((stock, index) => {
                // 现在 TypeScript 知道 data 存在了，报错会消失
                const klineData = klineQueries[index]?.data || [];
                const quote = quoteQueries[index]?.data;
                const isLoading = quoteQueries[index]?.isLoading;

                // ... (后续代码保持不变) ...
                const current = quote?.currentPrice || 0;
                const close = quote?.closePrice || 1;
                const changePercent = ((current - close) / close) * 100;
                const isUp = changePercent >= 0;

                return (
                    <div
                        key={stock.stockCode}
                        onClick={() => onSelect(stock.stockCode)}
                        className="bg-terminal-card border border-slate-700 rounded-xl p-4 hover:border-accent cursor-pointer transition-all hover:shadow-lg hover:shadow-blue-900/20 h-72 flex flex-col group"
                    >
                        <div className="flex justify-between items-start mb-4">
                            <div>
                                <h3 className="text-lg font-bold text-slate-100 group-hover:text-accent transition-colors">
                                    {stock.stockName}
                                </h3>
                                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded font-mono">
                    {stock.stockCode}
                  </span>
                                    <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
                                </div>
                            </div>

                            <div className="text-right">
                                <div className={clsx(
                                    "text-2xl font-mono font-bold transition-colors duration-300",
                                    isUp ? "text-stock-up" : "text-stock-down",
                                    isLoading ? "opacity-50" : "opacity-100"
                                )}>
                                    {current > 0 ? current.toFixed(2) : '--'}
                                </div>
                                <div className={clsx(
                                    "text-sm font-medium px-2 py-0.5 rounded ml-auto w-fit mt-1",
                                    isUp ? "bg-red-900/20 text-stock-up" : "bg-green-900/20 text-stock-down"
                                )}>
                                    {changePercent > 0 ? '+' : ''}{current > 0 ? changePercent.toFixed(2) : '0.00'}%
                                </div>
                            </div>
                        </div>

                        <div className="flex-1 w-full relative border-t border-slate-800 pt-2">
                            {klineData.length > 0 ? (
                                <KLineChart data={klineData} stockName={stock.stockName} isMini={true} />
                            ) : (
                                <div className="flex items-center justify-center h-full text-xs text-slate-600 animate-pulse">
                                    Waiting for data...
                                </div>
                            )}
                        </div>
                    </div>
                );
            })}
        </div>
    );
};