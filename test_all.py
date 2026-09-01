import urllib.request
import json
import time

api_key = "AIzaSyCCelm_IRKih0EYUl9G66kjXHy1-TX88NU"
model_list = ['models/gemini-2.5-flash', 'models/gemini-2.0-flash', 'models/gemini-2.0-flash-001', 'models/gemini-flash-latest', 'models/gemini-3-flash-preview', 'models/gemini-3.5-flash']
data = {"contents": [{"parts": [{"text": "Hello"}]}]}

for m in model_list:
    url = f"https://generativelanguage.googleapis.com/v1beta/{m}:generateContent?key={api_key}"
    req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            print(f"SUCCESS: {m}")
    except urllib.error.HTTPError as e:
        print(f"FAILED {m}: {e.code} - {e.read().decode('utf-8')[:100]}")
    except Exception as e:
        print(f"FAILED {m}: {e}")
    time.sleep(1)
