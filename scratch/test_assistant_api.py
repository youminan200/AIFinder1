import urllib.request
import json

api_key = "AIzaSyCCelm_IRKih0EYUl9G66kjXHy1-TX88NU"
model = "gemini-flash-latest"

url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"

itemsText = "- 이름: 지갑, 위치: 현관 (ID: 4), 등록일시: 2026-06-11 15:05"
areasText = "1(침실), 2(거실), 3(주방), 4(현관)"
question = "지갑 어딨어?"

prompt = f"""
사용자는 보관 중인 물건들의 위치를 찾기 위해 질문하고 있습니다.

[보관된 물건 목록]
{itemsText}

[방/구역 목록]
{areasText}

[사용자 질문]
{question}

[답변 지침]
1. 사용자의 질문에 맞춰 물건의 위치(구역 이름)와 필요한 경우 등록 일시를 친근하고 명확한 한국어 존댓말로 답변해 주십시오.
2. 물건 목록에 없는 물건을 물어볼 경우, 목록에 존재하지 않는다고 정중히 설명하고 방 구역 목록을 알려주며 다른 물건을 물어보도록 유도해 주십시오.
3. 답변은 3줄 이내로 간결하고 핵심만 작성해 주십시오.
"""

data = {
    "contents": [{
        "parts": [{"text": prompt}]
    }]
}

req = urllib.request.Request(
    url, 
    data=json.dumps(data).encode('utf-8'), 
    headers={'Content-Type': 'application/json'}
)

try:
    with urllib.request.urlopen(req) as response:
        print("Success:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print(f"HTTPError: {e.code} - {e.reason}")
    print(e.read().decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")
