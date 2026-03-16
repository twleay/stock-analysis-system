import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchRealtime, fetchKLine, fetchAnomalies, fetchIndicators, Stock } from '../services/api';
import { KLineChart } from './KLineChart';
import { SystemStatus } from './SystemStatus';
import { Zap } from 'lucide-react';
import clsx from 'clsx';
import { format } from 'date-fns';

interface DashboardProps {
    stock: Stock | undefined;
}

export const Dashboard: React.FC<DashboardProps> = ({ stock }) => {
    if (!stock) return <div className="p-10 text-slate-500">请选择一只股票</div>;
    const stockCode = stock.stockCode;

    // 1. 实时数据 (2秒刷新 - 高频)
    const { data: quote, isFetching } = useQuery({
        queryKey: ['realtime', stockCode],
        queryFn: () => fetchRealtime(stockCode),
        refetchInterval: 2000,
    });

    // 2. K线数据
    const { data: kline } = useQuery({
        queryKey: ['kline', stockCode],
        queryFn: () => fetchKLine(stockCode)
    });

    // 3. 异常检测 (明确泛型，防止 length 报错)
    const { data: anomalies } = useQuery({
        queryKey: ['anomalies', stockCode],
        queryFn: () => fetchAnomalies(stockCode),
        refetchInterval: 5000
    });

    // 4. 技术指标
    const { data: indicators } = useQuery({
        queryKey: ['indicators', stockCode],
        queryFn: () => fetchIndicators(stockCode),
        refetchInterval: 5000
    });

    const isUp = quote && (quote.currentPrice >= quote.closePrice);
    const change = quote ? quote.currentPrice - quote.closePrice : 0;
    const percent = quote ? (change / quote.closePrice) * 100 : 0;

    return (
        <div className="flex-1 h-screen overflow-hidden flex flex-col bg-terminal-bg text-slate-200">
            {/* 顶部 Header */}
            <header className="h-20 border-b border-slate-700 bg-terminal-card px-6 flex items-center justify-between shadow-lg z-10 shrink-0">
                <div>
                    <h2 className="text-2xl font-bold text-white flex items-center gap-3">
                        {stock.stockName}
                        <span className="text-sm bg-slate-700 px-2 py-0.5 rounded text-slate-300 font-mono tracking-wide">{stockCode}</span>
                    </h2>
                    <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
                        <span className={`w-2 h-2 rounded-full ${quote ? 'bg-green-500 animate-pulse' : 'bg-slate-600'}`}></span>
                        <span>{quote ? 'Realtime Connection' : 'Connecting...'}</span>
                        <span className="text-slate-600">|</span>
                        <span>{stock.market.toUpperCase()}</span>
                    </div>
                </div>

                {quote && (
                    <div className="flex items-center gap-8">
                        {/* 价格与增幅 */}
                        <div className="text-right">
                            <div className={clsx("text-4xl font-mono font-bold tracking-tight transition-colors", isUp ? "text-stock-up" : "text-stock-down")}>
                                {quote.currentPrice.toFixed(2)}
                            </div>
                            <div className={clsx("text-sm font-bold flex items-center justify-end gap-2", isUp ? "text-stock-up" : "text-stock-down")}>
                <span className={clsx("px-1.5 rounded text-xs", isUp ? "bg-red-900/30" : "bg-green-900/30")}>
                   {change > 0 ? '+' : ''}{percent.toFixed(2)}%
                </span>
                                <span>{change > 0 ? '+' : ''}{change.toFixed(2)}</span>
                            </div>
                        </div>

                        {/* 交易量 */}
                        <div className="text-right border-l border-slate-700 pl-6 hidden md:block">
                            <div className="text-xs text-slate-400 uppercase">Volume</div>
                            <div className="text-lg font-mono text-slate-200">{(quote.volume / 10000).toFixed(0)}<span className="text-xs ml-1 text-slate-500">万</span></div>
                            <div className="text-xs text-slate-500 mt-1 font-mono">{format(quote.timestamp, 'HH:mm:ss')}</div>
                        </div>
                    </div>
                )}
            </header>

            {/* 主布局 */}
            <div className="flex-1 p-4 grid grid-cols-4 grid-rows-6 gap-4 overflow-hidden pb-14 relative"> {/* pb-14 是为了给左下角的悬浮按钮留位置 */}

                {/* 左上 - K线图 (保持 row-span-4 不变) */}
                <div className="col-span-3 row-span-4 bg-terminal-card rounded-xl border border-slate-700 p-1 relative flex flex-col">
                    {isFetching && <div className="absolute top-2 right-2 w-2 h-2 bg-accent rounded-full animate-ping"></div>}
                    {kline && <KLineChart data={kline} stockName={stock.stockName} />}
                </div>

                {/* --- 修改此处：右侧栏 - 上半部分：系统状态 --- */}
                {/* 将 row-span-2 改为 row-span-3，并添加 h-full */}
                <div className="col-span-1 row-span-3 h-full">
                    <SystemStatus stockCount={5} anomalyCount={anomalies?.length || 0} />
                </div>
                {/* ------------------------------------------- */}

                {/* --- 修改此处：右侧栏 - 下半部分：异常检测 --- */}
                {/* 将 row-span-4 改为 row-span-3，并确保有 h-full */}
                <div className="col-span-1 row-span-3 bg-terminal-card rounded-xl border border-slate-700 flex flex-col overflow-hidden h-full">
                    <div className="p-3 border-b border-slate-700 flex items-center justify-between bg-red-900/5 shrink-0">
                        <div className="flex items-center gap-2">
                            <Zap className="text-stock-up" size={16} />
                            <h3 className="font-bold text-slate-200 text-sm">ALERTS</h3>
                        </div>
                        <span className="text-[10px] bg-slate-800 px-1.5 py-0.5 rounded text-slate-400">Live</span>
                    </div>

                    <div className="flex-1 overflow-y-auto p-2 space-y-2 custom-scrollbar">
                        {/* ... 异常列表内容保持不变 ... */}
                        {anomalies && anomalies.length > 0 ? (
                            // ... existing map code ...
                            anomalies.map((anomaly, idx) => (
                                <div key={idx} className="bg-slate-800/40 p-2.5 rounded border-l-2 border-l-stock-up hover:bg-slate-800 transition-colors">
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="text-xs font-bold text-slate-200">{anomaly.anomalyType}</span>
                                        <span className="text-[10px] text-slate-500 font-mono">{format(new Date(anomaly.anomalyTime), 'HH:mm:ss')}</span>
                                    </div>
                                    <div className="text-[11px] text-slate-400 leading-tight">{anomaly.description}</div>
                                </div>
                            ))
                        ) : (
                            <div className="text-center text-slate-600 mt-10 text-xs">No active alerts</div>
                        )}
                    </div>
                </div>
                {/* ------------------------------------------- */}

                {/* 左下 - 指标卡片 (保持 row-span-2 不变) */}
                <div className="col-span-3 row-span-2 grid grid-cols-3 gap-4">
                    {/* ... 指标卡片内容保持不变 ... */}
                    {/* MACD 卡片 */}
                    <div className="bg-terminal-card rounded-xl border border-slate-700 p-4 flex flex-col justify-center">
                        <div className="text-slate-500 text-[10px] font-bold uppercase tracking-wider mb-2">MACD Trend</div>
                        {indicators ? (
                            <div className="flex justify-between items-baseline">
                                <div className="text-2xl font-mono text-white">{indicators.macd?.toFixed(3)}</div>
                                <div className={clsx("text-sm font-bold", (indicators.macdHist || 0) > 0 ? "text-stock-up" : "text-stock-down")}>
                                    Hist: {indicators.macdHist?.toFixed(3) || '--'}
                                </div>
                            </div>
                        ) : <div className="h-8 bg-slate-800/50 rounded animate-pulse"></div>}
                    </div>

                    {/* KDJ 卡片 */}
                    <div className="bg-terminal-card rounded-xl border border-slate-700 p-4 flex flex-col justify-center">
                        <div className="text-slate-500 text-[10px] font-bold uppercase tracking-wider mb-2">KDJ Oscillator</div>
                        {indicators ? (
                            <div className="flex gap-4">
                                <div><span className="text-[10px] text-slate-400 block">K</span><span className="text-lg font-mono text-accent">{indicators.kdjK?.toFixed(1) || '-'}</span></div>
                                <div><span className="text-[10px] text-slate-400 block">D</span><span className="text-lg font-mono text-yellow-500">{indicators.kdjD?.toFixed(1) || '-'}</span></div>
                                <div><span className="text-[10px] text-slate-400 block">J</span><span className="text-lg font-mono text-purple-500">{indicators.kdjJ?.toFixed(1) || '-'}</span></div>
                            </div>
                        ) : <div className="h-8 bg-slate-800/50 rounded animate-pulse"></div>}
                    </div>

                    {/* RSI 卡片 */}
                    <div className="bg-terminal-card rounded-xl border border-slate-700 p-4 flex flex-col justify-center relative overflow-hidden">
                        <div className="text-slate-500 text-[10px] font-bold uppercase tracking-wider mb-2">RSI Strength</div>
                        {indicators ? (() => {
                            const rsiValue = indicators.rsi12 ?? 50;
                            const label = rsiValue > 70 ? 'OVERBOUGHT' : rsiValue < 30 ? 'OVERSOLD' : 'NEUTRAL';
                            const barColor = rsiValue > 70 ? 'bg-red-500' : rsiValue < 30 ? 'bg-green-500' : 'bg-blue-500';
                            return (
                                <>
                                    <div className="text-2xl font-mono text-white">
                                        {rsiValue.toFixed(1)}
                                        <span className="text-xs text-slate-500 ml-1">{label}</span>
                                    </div>
                                    <div className="w-full bg-slate-800 h-1.5 mt-3 rounded-full overflow-hidden">
                                        <div className={`${barColor} h-full transition-all duration-500`} style={{ width: `${Math.min(rsiValue, 100)}%` }}></div>
                                    </div>
                                </>
                            );
                        })() : <div className="h-8 bg-slate-800/50 rounded animate-pulse"></div>}
                    </div>
                </div>
            </div>
        </div>
    );
};