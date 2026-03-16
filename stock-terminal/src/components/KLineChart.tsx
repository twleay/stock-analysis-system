import React, { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import { KLineData } from '../services/api';

interface KLineChartProps {
    data: KLineData[];
    stockName: string;
    isMini?: boolean; // 新增：是否为迷你模式
}

export const KLineChart: React.FC<KLineChartProps> = ({ data, stockName, isMini = false }) => {
    const option = useMemo(() => {
        if (!data || data.length === 0) return {};

        const dates = data.map(i => i.tradeDate);
        const values = data.map(i => [i.openPrice, i.closePrice, i.lowPrice, i.highPrice]);

        // 迷你模式下简化配置
        const gridConfig = isMini
            ? { left: 5, right: 5, top: 20, bottom: 5 }
            : [
                { left: '3%', right: '1%', height: '60%' },
                { left: '3%', right: '1%', top: '75%', height: '15%' }
            ];

        const xAxisConfig = isMini
            ? { type: 'category', data: dates, show: false } // 隐藏X轴
            : [
                { type: 'category', data: dates, scale: true, boundaryGap: false, axisLine: { onZero: false }, splitLine: { show: false } },
                { type: 'category', gridIndex: 1, data: dates, axisLabel: { show: false } }
            ];

        const yAxisConfig = isMini
            ? { scale: true, show: false, splitLine: { show: false } } // 隐藏Y轴
            : [
                { scale: true, splitArea: { show: true, areaStyle: { color: ['rgba(30,41,59,0.3)', 'rgba(15,23,42,0.3)'] } } },
                { scale: true, gridIndex: 1, splitNumber: 2, axisLabel: { show: false }, axisLine: { show: false }, splitLine: { show: false } }
            ];

        // 迷你模式不显示 DataZoom 和 Volume
        const dataZoomConfig = isMini ? [] : [
            { type: 'inside', xAxisIndex: [0, 1], start: 50, end: 100 },
            { show: true, xAxisIndex: [0, 1], type: 'slider', bottom: '2%', start: 50, end: 100, borderColor: '#334155' }
        ];

        const seriesConfig = [
            {
                name: stockName,
                type: 'candlestick',
                data: values,
                itemStyle: {
                    color: '#ef4444',
                    color0: '#22c55e',
                    borderColor: '#ef4444',
                    borderColor0: '#22c55e'
                }
            }
        ];

        if (!isMini) {
            // 完整模式下添加均线和成交量
            const volumes = data.map((i, index) => [index, i.volume, i.closePrice > i.openPrice ? 1 : -1]);

            const calculateMA = (dayCount: number) => {
                const result = [];
                for (let i = 0, len = values.length; i < len; i++) {
                    if (i < dayCount) { result.push('-'); continue; }
                    let sum = 0;
                    for (let j = 0; j < dayCount; j++) { sum += values[i - j][1]; }
                    result.push((sum / dayCount).toFixed(2));
                }
                return result;
            };

            seriesConfig.push(
                { name: 'MA5', type: 'line', data: calculateMA(5), smooth: true, lineStyle: { opacity: 0.5, width: 1 } } as any,
                { name: 'MA10', type: 'line', data: calculateMA(10), smooth: true, lineStyle: { opacity: 0.5, width: 1 } } as any,
                {
                    name: 'Volume', type: 'bar', xAxisIndex: 1, yAxisIndex: 1, data: volumes,
                    itemStyle: { color: (params: any) => params.value[2] > 0 ? '#ef4444' : '#22c55e' }
                } as any
            );
        }

        return {
            backgroundColor: 'transparent',
            animation: false,
            tooltip: isMini ? { show: false } : {
                trigger: 'axis',
                axisPointer: { type: 'cross' },
                backgroundColor: 'rgba(30, 41, 59, 0.9)',
                borderColor: '#334155',
                textStyle: { color: '#e2e8f0' }
            },
            grid: gridConfig,
            xAxis: xAxisConfig,
            yAxis: yAxisConfig,
            dataZoom: dataZoomConfig,
            series: seriesConfig
        };
    }, [data, stockName, isMini]);

    return <ReactECharts option={option} style={{ height: '100%', width: '100%' }} />;
};