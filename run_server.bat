@echo off
cd /d "E:\AIfinder"
set PATH=E:\AIfinder\node;%PATH%
start "" /b python server\server.py
start "" /b node\npx.cmd --yes localtunnel --port 5000 --subdomain yuminan2011
