import requests
import json

url = "http://localhost:5000/gee/extract"
headers = {"Content-Type": "application/json"}
with open("request.json", "r") as f:
    data = json.load(f)

response = requests.post(url, headers=headers, json=data)
print(response.text)
