import React from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Database, Server, Activity, Clock } from 'lucide-react';
import { format } from 'date-fns';

// 简单的 API 健康检查
const checkHealth = async () => (await axios.get('http://localhost:8080/api/health')).data;

export const SystemStatus: React.FC<{ stockCount: number; anomalyCount: number }> = ({ stockCount, anomalyCount }) => {
    const { data: health } = useQuery({
        queryKey: ['health'],
        queryFn: checkHealth,
        refetchInterval: 5000
    });

    const isHealthy = health?.status === 'ok';

    return (
        <div className="flex flex-col h-full bg-terminal-card border border-slate-700 rounded-xl overflow-hidden">
            <div className="p-3 border-b border-slate-700 flex items-center gap-2 bg-slate-800/50">
                <Activity size={16} className="text-accent" />
                <h3 className="font-bold text-slate-200 text-sm">SYSTEM MONITOR</h3>
            </div>

            <div className="p-4 grid grid-cols-2 gap-4 flex-1">
                {/* 服务状态 */}
                <div className="bg-slate-800/50 p-3 rounded border border-slate-700/50 flex flex-col justify-between">
                    <div className="flex items-center gap-2 text-slate-400 text-xs uppercase mb-1">
                        <Server size={12} /> API Service
                    </div>
                    <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full ${isHealthy ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
                        <span className={`font-mono font-bold ${isHealthy ? 'text-green-400' : 'text-red-400'}`}>
              {isHealthy ? 'ONLINE' : 'OFFLINE'}
            </span>
                    </div>
                </div>

                {/* 数据库状态 (模拟) */}
                <div className="bg-slate-800/50 p-3 rounded border border-slate-700/50 flex flex-col justify-between">
                    <div className="flex items-center gap-2 text-slate-400 text-xs uppercase mb-1">
                        <Database size={12} /> MySQL/Redis
                    </div>
                    <div className="font-mono font-bold text-blue-400">CONNECTED</div>
                    <div className="text-[10px] text-slate-500">Pool: 8/10 Active</div>
                </div>

                {/* 监控指标 */}
                <div className="bg-slate-800/50 p-3 rounded border border-slate-700/50 flex flex-col justify-between">
                    <div className="text-slate-400 text-xs uppercase">Stocks</div>
                    <div className="text-2xl font-mono text-white">{stockCount}</div>
                </div>

                <div className="bg-slate-800/50 p-3 rounded border border-slate-700/50 flex flex-col justify-between">
                    <div className="text-slate-400 text-xs uppercase">Anomalies</div>
                    <div className="text-2xl font-mono text-yellow-500">{anomalyCount}</div>
                </div>
            </div>

            {/* 底部时间戳 */}
            <div className="px-3 py-1 bg-slate-900 border-t border-slate-800 text-[10px] text-slate-500 flex justify-between font-mono">
                <span className="flex items-center gap-1"><Clock size={10}/> UPTIME: 24H 12M</span>
                <span>LAST PING: {format(new Date(), 'HH:mm:ss')}</span>
            </div>
        </div>
    );
};