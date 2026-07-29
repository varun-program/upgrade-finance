import React, { useState, useRef, useEffect } from 'react';
import { dataService } from '../utils/api';
import { Send, Sparkles, MessageSquare, User, Bot } from 'lucide-react';

export default function Chat() {
  const [messages, setMessages] = useState([
    {
      sender: 'bot',
      text: "Hello! I am your Antigravity AI Assistant. Ask me anything about your finances, such as 'How much did I spend this month?' or 'What is my biggest expense?'"
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const chatEndRef = useRef(null);

  const quickQuestions = [
    "How much did I spend on food this month?",
    "Show my biggest expense.",
    "What is my current net balance?",
  ];

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleSendMessage = async (textToSend) => {
    if (!textToSend.trim()) return;

    // Add user message
    setMessages(prev => [...prev, { sender: 'user', text: textToSend }]);
    setInput('');
    setLoading(true);

    // Call service
    const responseText = await dataService.askAi(textToSend);
    
    setMessages(prev => [...prev, { sender: 'bot', text: responseText }]);
    setLoading(false);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight flex items-center space-x-2">
          <Sparkles className="h-8 w-8 text-primary" />
          <span>AI Assistant</span>
        </h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">Get instant answers about your expenses and budget metrics.</p>
      </div>

      <div className="glass rounded-2xl border shadow-lg flex flex-col h-[550px] overflow-hidden">
        
        {/* Messages list */}
        <div className="flex-grow p-6 overflow-y-auto space-y-4 no-scrollbar">
          {messages.map((m, idx) => {
            const isBot = m.sender === 'bot';
            return (
              <div key={idx} className={`flex space-x-3 max-w-[85%] ${isBot ? 'mr-auto' : 'ml-auto flex-row-reverse space-x-reverse'}`}>
                <div className={`p-2 rounded-xl h-fit ${isBot ? 'bg-primary/10 text-primary' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'}`}>
                  {isBot ? <Bot className="h-4 w-4" /> : <User className="h-4 w-4" />}
                </div>
                <div className={`p-3.5 rounded-2xl text-sm leading-relaxed ${
                  isBot 
                    ? 'bg-slate-50 dark:bg-slate-900/60 border border-slate-100 dark:border-slate-800/40 text-slate-800 dark:text-slate-100' 
                    : 'bg-primary text-white font-semibold'
                }`}>
                  {m.text.split('\n').map((line, lidx) => <p key={lidx}>{line}</p>)}
                </div>
              </div>
            );
          })}
          
          {loading && (
            <div className="flex space-x-3 mr-auto max-w-[85%] animate-pulse">
              <div className="p-2 rounded-xl bg-primary/10 text-primary h-fit">
                <Bot className="h-4 w-4" />
              </div>
              <div className="p-3.5 bg-slate-50 dark:bg-slate-900/60 border rounded-2xl text-sm text-slate-400">
                Thinking...
              </div>
            </div>
          )}
          
          <div ref={chatEndRef} />
        </div>

        {/* Suggestion tags */}
        <div className="px-6 py-2 bg-slate-50/50 dark:bg-slate-900/20 border-t border-slate-100 dark:border-slate-800/50 flex space-x-2 overflow-x-auto no-scrollbar">
          {quickQuestions.map((q) => (
            <button
              key={q}
              onClick={() => handleSendMessage(q)}
              className="flex-shrink-0 px-3 py-1 rounded-full border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40 text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 hover:border-slate-400 transition-colors"
            >
              {q}
            </button>
          ))}
        </div>

        {/* Input Bar */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800/50 flex space-x-3 bg-white/40 dark:bg-slate-950/20">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSendMessage(input)}
            className="flex-grow pl-4 pr-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
            placeholder="Type your question..."
          />
          <button
            onClick={() => handleSendMessage(input)}
            className="p-3 bg-primary hover:bg-primary-hover text-white rounded-xl shadow-md transition-colors"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
