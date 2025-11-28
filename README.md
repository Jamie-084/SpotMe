# SpotMe — Starter: FastAPI + React + C++ JSON client

This repo skeleton demonstrates a local development setup where:

- Backend: FastAPI (Python) serving:
  - POST /api/data — receive JSON data from a local C++ app
  - WebSocket /ws — browser clients connect to receive broadcasts
- Frontend: React (local dev) connects to WebSocket to display live updates
- C++ sample: uses libcurl to POST JSON to the backend

Quick start (local development)

1. Backend
   - Create a virtualenv and install deps:
     python -m venv .venv
     source .venv/bin/activate
     pip install -r backend/requirements.txt
   - Run:
     python backend/main.py
   - Backend listens on http://localhost:8000

2. Frontend
   - cd frontend
   - npm install
   - npm start
   - Open http://localhost:3000 (React dev server) — it connects to ws://localhost:8000/ws

3. C++ sender (requires libcurl)
   - Build and run the sample in cpp/send_json.cpp. It POSTs JSON to http://localhost:8000/api/data

Notes
- This is a minimal starter. For production, secure WebSockets, restrict CORS origins, add authentication, persistent storage, and proper error handling.
- If you want, I can add these files to the repository and open a PR — tell me the branch name or let me pick one.
