import urllib.request
import json
import base64

api_key = "AIzaSyCCelm_IRKih0EYUl9G66kjXHy1-TX88NU"

# 1x1 transparent GIF image base64
image_base64 = "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
image_bytes = base64.b64decode(image_base64)

models_to_test = [
    "gemini-2.0-flash",
    "gemini-flash-latest",
    "gemini-2.5-flash",
    "gemini-3.5-flash"
]

for model in models_to_test:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    data = {
        "contents": [{
            "parts": [
                {
                    "inlineData": {
                        "mimeType": "image/gif",
                        "data": base64.b64encode(image_bytes).decode('utf-8')
                    }
                },
                {
                    "text": "이 이미지에 무엇이 있나요?"
                }
            ]
        }]
    }
    
    req = urllib.request.Request(
        url, 
        data=json.dumps(data).encode('utf-8'), 
        headers={'Content-Type': 'application/json'}
    )
    
    print(f"Testing model: {model} ...")
    try:
        with urllib.request.urlopen(req) as response:
            res_json = json.loads(response.read().decode('utf-8'))
            text = res_json['candidates'][0]['content']['parts'][0]['text']
            print(f"SUCCESS {model}: {text.strip()}")
    except urllib.error.HTTPError as e:
        print(f"FAILED {model} (HTTPError): {e.code} - {e.reason}")
        try:
            print(e.read().decode('utf-8'))
        except:
            pass
    except Exception as e:
        print(f"FAILED {model}: {e}")
    print("-" * 40)
