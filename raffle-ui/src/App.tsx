import React, { useState, useRef, useEffect } from 'react';
import { Gift, MessageSquare, User, Settings, Sparkles } from 'lucide-react';
import './App.css';

interface ActivityOption {
  activityId: number;
  activityName: string;
  strategyId: number;
}

interface Prize {
  id: number;
  name: string;
  pos: number;
}

const indexToGridPos = [0, 1, 2, 5, 8, 7, 6, 3];

export default function App() {
  const [activeTab, setActiveTab] = useState('raffle');
  const [isDrawing, setIsDrawing] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [showModal, setShowModal] = useState(false);
  const [result, setResult] = useState<any>(null);

  const [activities, setActivities] = useState<ActivityOption[]>([]);
  const [selectedActivityId, setSelectedActivityId] = useState<number | null>(null);
  const [currentStrategyId, setCurrentStrategyId] = useState<number | null>(null);
  const [prizes, setPrizes] = useState<Prize[]>([]);
  
  const timerRef = useRef<any>(null);
  const targetIdRef = useRef<number | null>(null);

  useEffect(() => {
    const fetchActivities = async () => {
      try {
        const response = await fetch('/api/raffle/activities');
        const data = await response.json();
        setActivities(data);
        if (data.length > 0) handleActivityChange(data[0].activityId);
      } catch (err) { console.error(err); }
    };
    fetchActivities();
  }, []);

  const handleActivityChange = async (activityId: number) => {
    setSelectedActivityId(activityId);
    try {
      const response = await fetch(`/api/raffle/activities/${activityId}`);
      const data = await response.json();
      setCurrentStrategyId(data.strategyId);

      const backendPrizes = (data.prizes || []).filter((p: any) => !p.awardName.includes('谢谢惠顾'));
      const newPrizes: Prize[] = new Array(8);

      let prizeCursor = 0;
      for (let i = 0; i < 8; i++) {
        if (i % 2 === 0 && prizeCursor < backendPrizes.length) {
          const bp = backendPrizes[prizeCursor++];
          newPrizes[i] = { id: bp.awardId, name: bp.awardName, pos: indexToGridPos[i] };
        } else {
          newPrizes[i] = { id: 101, name: '谢谢惠顾', pos: indexToGridPos[i] }; // 统一 ID 为 101
        }
      }
      setPrizes(newPrizes);
    } catch (err) { console.error(err); }
  };

  const startAnimation = () => {
    let currentStep = 0;
    let speed = 40;

    const run = () => {
      setActiveIndex(indexToGridPos[currentStep % 8]);
      currentStep++;
      const targetId = targetIdRef.current;

      if (targetId !== null && currentStep >= 8) {
        const currentAward = prizes.find(p => p.pos === indexToGridPos[(currentStep - 1) % 8]);
        
        // 核心修复：只要 ID 匹配（包括我们统一设定的 101），就停止
        if (currentAward?.id === targetId) {
          clearTimeout(timerRef.current);
          setTimeout(() => {
            setIsDrawing(false);
            setShowModal(true);
          }, 150);
          return;
        }
        speed = 80;
      }
      timerRef.current = setTimeout(run, speed);
    };
    run();
  };

  const handleDraw = async () => {
    if (isDrawing || !currentStrategyId) return;
    setIsDrawing(true);
    setResult(null);
    setShowModal(false);
    targetIdRef.current = null;
    startAnimation();

    try {
      const response = await fetch('/api/raffle/draw', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: 200001, strategyId: currentStrategyId })
      });
      const data = await response.json();
      setResult(data);
      targetIdRef.current = data.awardId; // 此时 targetId 可能为 101
    } catch (error) {
      targetIdRef.current = 101; 
      setResult({ success: false, awardName: '抽奖失败' });
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
          <div><h1>幸运抽奖</h1><p style={{ color: 'var(--text-secondary)' }}>Premium Rewards System</p></div>
          <div className="activity-selector">
            <label>活动切换</label>
            <select className="custom-select" value={selectedActivityId || ''} onChange={(e) => handleActivityChange(Number(e.target.value))}>
              {activities.map(act => (<option key={act.activityId} value={act.activityId}>{act.activityName}</option>))}
            </select>
          </div>
        </header>

        {activeTab === 'raffle' ? (
          <div className="raffle-container">
            <div className="grid">
              {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((i) => {
                if (i === 4) return (<button key={i} className="draw-btn" onClick={handleDraw} disabled={isDrawing}>{isDrawing ? '...' : 'GO'}</button>);
                const prize = prizes.find(p => p.pos === i);
                return (<div key={i} className={`cell ${activeIndex === i ? 'active' : ''}`}><span className="prize-name">{prize?.name}</span></div>);
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
