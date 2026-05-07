import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid
} from 'recharts';

export interface RawSprintData {
  category: string;
  storyPoints: number;
}

interface SprintAnalyticsChartProps {
  data: RawSprintData[] | null | undefined;
}

export default function SprintAnalyticsChart({ data }: SprintAnalyticsChartProps) {

  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 bg-gray-900/50 border border-dashed border-white/20 rounded-xl">
        <p className="text-gray-500 font-medium italic">
          No data pooled for this sprint yet
        </p>
      </div>
    );
  }

  const chartData = data.map((item) => ({
    label: item.category,
    value: item.storyPoints,
  }));

  return (
    <div className="bg-gray-900 border border-white/10 p-6 rounded-xl w-full h-80 flex flex-col">
      <h3 className="text-base font-semibold text-white mb-6">
        Sprint Story Point Metrics
      </h3>
      
      <div className="flex-1 w-full min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart 
            data={chartData} 
            margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#ffffff1a" />
            
            <XAxis 
              dataKey="label" 
              tick={{ fontSize: 12, fill: '#9ca3af' }} 
              axisLine={false} 
              tickLine={false} 
            />
            <YAxis 
              tick={{ fontSize: 12, fill: '#9ca3af' }} 
              axisLine={false} 
              tickLine={false} 
            />
            
            <Tooltip 
              cursor={{ fill: '#ffffff0a' }}
              contentStyle={{ 
                backgroundColor: '#111827', 
                borderColor: '#ffffff1a',   
                borderRadius: '8px', 
                color: '#fff',
                boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.5)' 
              }}
              itemStyle={{ color: '#60a5fa' }} 
            />
            
            <Bar 
              dataKey="value" 
              fill="#3b82f6" 
              radius={[4, 4, 0, 0]} 
              barSize={40}
              name="Story Points"
            />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}