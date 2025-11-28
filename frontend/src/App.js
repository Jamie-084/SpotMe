import React, { useEffect, useRef, useState } from "react";

function App() {
  const [messages, setMessages] = useState([]);
  const wsRef = useRef(null);

  useEffect(() => {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const wsUrl = `${protocol}://localhost:8000/ws`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
    };
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        setMessages((m) => [data, ...m].slice(0, 50));
      } catch (e) {
        console.warn("invalid message", event.data);
      }
    };
    ws.onclose = () => {
      console.log("WebSocket closed");
    };
    return () => {
      ws.close();
    };
  }, []);

  const sendTest = async () => {
    const payload = {
      from: "react-test",
      time: new Date().toISOString(),
      note: "test POST from frontend"
    };
    try {
      const res = await fetch("http://localhost:8000/api/data", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify(payload)
      });
      console.log("POST response", await res.json());
    } catch (err) {
      console.error("POST failed", err);
    }
  };

  return (
    <div style={{padding:20}}>
      <h2>SpotMe — Live Messages</h2>
      <button onClick={sendTest}>Send test POST to backend</button>
      <ul>
        {messages.map((m, idx) => (
          <li key={idx}>
            <pre style={{whiteSpace:"pre-wrap"}}>{JSON.stringify(m, null, 2)}</pre>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default App;
