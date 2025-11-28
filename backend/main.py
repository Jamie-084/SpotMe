from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import asyncio
import json
from typing import List

app = FastAPI(title="SpotMe Backend")

# Development: allow all origins. Lock this down for production.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
        self.lock = asyncio.Lock()

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        async with self.lock:
            self.active_connections.append(websocket)

    async def disconnect(self, websocket: WebSocket):
        async with self.lock:
            if websocket in self.active_connections:
                self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        # send to all connected websockets
        async with self.lock:
            websockets = list(self.active_connections)
        if not websockets:
            return
        data_text = json.dumps(message)
        for ws in websockets:
            try:
                await ws.send_text(data_text)
            except Exception:
                # ignore send errors per client; cleanup happens on disconnect
                pass

manager = ConnectionManager()

@app.post("/api/data")
async def ingest_data(request: Request):
    """
    Accept JSON from external apps (like a C++ client).
    Example client should POST application/json to /api/data
    The JSON will be broadcast to all connected WebSocket clients.
    """
    try:
        payload = await request.json()
    except Exception:
        return JSONResponse({"error": "invalid JSON"}, status_code=400)

    # Basic validation: ensure payload is a dict
    if not isinstance(payload, dict):
        return JSONResponse({"error": "expected JSON object"}, status_code=400)

    # Broadcast to websocket clients (fire-and-forget)
    asyncio.create_task(manager.broadcast(payload))
    return {"status": "ok", "received": payload}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """
    Browser clients should connect here to receive live JSON messages.
    Each message is a JSON string.
    """
    await manager.connect(websocket)
    try:
        while True:
            # Keep the connection alive; if client sends messages we ignore for now
            data = await websocket.receive_text()
            # Echo or ignore. Here we echo back the last message for debugging.
            try:
                obj = json.loads(data)
                await websocket.send_text(json.dumps({"echo": obj}))
            except Exception:
                await websocket.send_text(json.dumps({"info": "connected"}))
    except WebSocketDisconnect:
        await manager.disconnect(websocket)
    except Exception:
        await manager.disconnect(websocket)

@app.get("/api/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    # For local development
    uvicorn.run("backend.main:app", host="0.0.0.0", port=8000, reload=True)
