import React, { useState, useRef } from 'react';
import { Gift, MessageSquare, User, Settings, Sparkles } from 'lucide-react';
import './App.css';

const PRIZES = [
  { id: 201, name: 'iPhone 15', icon: '📱', pos: 0 },
  { id: 202, name: '100元红包', icon: '🧧', pos: 1 },
  { id: 203, name: '华为平板', icon: '💻', pos: 2 },
  { id: 204, name: '小米手环', icon: '⌚', pos: 5 },
  { id: 205, name: '50元话费', icon: '📞', pos: 8 },
  { id: 206, name: '积分+50', icon: '💎', pos: 7 },
  { id: 207, name: '谢谢惠顾', icon: '☕', pos: 6 },
  { id: 208, name: '精美礼品', icon: '🎁', pos: 3 },
];

const indexToGridPos = [0, 1, 2, 5, 8, 7, 6, 3];

export default function App() {
  const [activeTab, setActiveTab] = useState('raffle');
  const [isDrawing, setIsDrawing] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [showModal, setShowModal] = useState(false);
  const [result, setResult] = useState<any>(null);
  
  const timerRef = useRef<any>(null);
  const targetIdRef = useRef<number | null>(null);

  const startAnimation = () => {
    let currentStep = 0;
    let speed = 40; // 初始极速

    const run = () => {
      setActiveIndex(indexToGridPos[currentStep % 8]);
      currentStep++;

      const targetId = targetIdRef.current;

      // 停止策略：
      // 1. 必须已经拿到了后端结果 (targetId !== null)
      // 2. 为了视觉效果，最少也要转够 8 步（一整圈）
      if (targetId !== null && currentStep >= 8) {
        const currentAward = PRIZES.find(p => p.pos === indexToGridPos[(currentStep - 1) % 8]);
        
        // 匹配逻辑：ID 匹配，或者 101L 兜底到谢谢惠顾 (id: 207)
        const isMatch = currentAward?.id === targetId || (targetId === 101 && currentAward?.id === 207);

        if (isMatch) {
          clearTimeout(timerRef.current);
          setTimeout(() => {
            setIsDrawing(false);
            setShowModal(true);
          }, 150); // 停顿一下弹出，体验更好
          return;
        }
        speed = 80; // 接近目标时稍微慢一点点，显得更真实
      }

      timerRef.current = setTimeout(run, speed);
    };

    run();
  };

  const handleDraw = async () => {
    if (isDrawing) return;
    
    setIsDrawing(true);
    setResult(null);
    setShowModal(false);
    targetIdRef.current = null;

    // 1. 立即启动视觉动画
    startAnimation();

    // 2. 并行请求后端
    try {
      const response = await fetch('/api/raffle/draw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: 200001, strategyId: 1001 })
      });
      
      const data = await response.json();
      setResult(data);
      targetIdRef.current = data.awardId; // 瞬间标记目标 ID
    } catch (error) {
      targetIdRef.current = 207; // 报错自动停在谢谢惠顾
      setResult({ success: false, awardName: '系统繁忙' });
    }
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="nav-item-logo"><Sparkles size={32} color="#8b5cf6" /></div>
        <div className={`nav-item ${activeTab === 'raffle' ? 'active' : ''}`} onClick={() => setActiveTab('raffle')}>
          <Gift size={24} />
        </div>
        <div className={`nav-item ${activeTab === 'ai' ? 'active' : ''}`} onClick={() => setActiveTab('ai')}>
          <MessageSquare size={24} />
        </div>
        <div style={{ marginTop: 'auto' }} className="nav-item"><Settings size={24} /></div>
        <div className="nav-item"><User size={24} /></div>
      </aside>

      <main className="main-view">
        <header className="header">
          <h1>幸运抽奖</h1>
          <p>Real-time Rewards System</p>
        </header>

        {activeTab === 'raffle' ? (
          <div className="raffle-container">
            <div className="grid">
              {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((i) => {
                if (i === 4) return (
                  <button key={i} className="draw-btn" onClick={handleDraw} disabled={isDrawing}>
                    {isDrawing ? '...' : 'GO'}
                  </button>
                );
                const prize = PRIZES.find(p => p.pos === i);
                return (
                  <div key={i} className={`cell ${activeIndex === i ? 'active' : ''}`}>
                    <span className="prize-icon">{prize?.icon}</span>
                    <span className="prize-name">{prize?.name}</span>
                  </div>
                );
              })}
            </div>
          </div>
        ) : (
          <div className="ai-placeholder"><h2>AI Chat (Coming Soon)</h2></div>
        )}

        {showModal && result && (
          <div className="overlay">
            <div className="modal">
              <h2>{result.success ? '🎉 恭喜中奖' : '☕ 谢谢参与'}</h2>
              <p>{result.awardName}</p>
              <button onClick={() => setShowModal(false)}>知道了</button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
