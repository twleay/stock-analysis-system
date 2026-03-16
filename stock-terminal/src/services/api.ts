import axios from 'axios';

// 配置后端地址
const API_BASE_URL = 'http://localhost:8080/api';

// 1. 股票基础信息
export interface Stock {
    stockCode: string;
    stockName: string;
    market: string;
    sector?: string;
}

// 2. 实时行情
export interface RealtimeQuote {
    stockCode: string;
    timestamp: number;
    currentPrice: number;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    closePrice: number; // 昨收
    volume: number;
    amount: number;
}

// 3. K线数据
export interface KLineData {
    stockCode: string;
    tradeDate: string;
    openPrice: number;
    highPrice: number;
    lowPrice: number;
    closePrice: number;
    volume: number;
}

// 4. 技术指标
export interface Indicator {
    stockCode: string;
    tradeDate: string;
    // MACD
    macd?: number;       // DIF
    macdSignal?: number; // DEA
    macdHist?: number;   // MACD柱
    // KDJ
    kdjK?: number;
    kdjD?: number;
    kdjJ?: number;       // J值
    // 均线
    ma5?: number;
    ma10?: number;
    ma20?: number;
    ma60?: number;
}

// 5. 异常记录
export interface AnomalyRecord {
    stockCode: string;
    anomalyType: string;
    anomalyTime: string;
    severity: 'low' | 'medium' | 'high';
    description: string;
}

const api = axios.create({ baseURL: API_BASE_URL });

export const fetchStocks = async () => (await api.get<Stock[]>('/stocks')).data;
export const fetchRealtime = async (code: string) => (await api.get<RealtimeQuote>(`/realtime/${code}`)).data;

// --- 关键修正：改回原来的名字 fetchAllRealtime 以匹配 Sidebar.tsx ---
export const fetchAllRealtime = async () => (await api.get<RealtimeQuote[]>('/realtime')).data;

export const fetchKLine = async (code: string) => (await api.get<KLineData[]>(`/kline/${code}?limit=100`)).data;
export const fetchIndicators = async (code: string) => (await api.get<Indicator>(`/indicators/${code}/latest`)).data;
export const fetchAnomalies = async (code: string) => (await api.get<AnomalyRecord[]>(`/anomalies?stockCode=${code}`)).data;