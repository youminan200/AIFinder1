import urllib.request
import json
import time

api_key = "AIzaSyCCelm_IRKih0EYUl9G66kjXHy1-TX88NU"
models_url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
req = urllib.request.Request(models_url)

try:
    models_res = urllib.request.urlopen(req).read().decode('utf-8')
    model_list = [m['name'] for m in json.loads(models_res)['models'] if 'flash' in m['name'] and 'lite' not in m['name'] and 'tts' not in m['name'] and 'image' not in m['name']]
except Exception as e:
    print(f"Error getting models: {e}")
    model_list = []

print("Flash models:", model_list)

data = {"contents": [{"parts": [{"text": "Hello"}]}]}
for m in model_list:
    url = f"https://generativelanguage.googleapis.com/v1beta/{m}:generateContent?key={api_key}"
    req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            print(f"SUCCESS: {m}")
            break
    except urllib.error.HTTPError as e:
        print(f"FAILED {m}: {e.code}")
    except Exception as e:
        print(f"FAILED {m}: {e}")
    time.sleep(1)
