import requests
HOST = "http://localhost:4502"
AUTH = ("admin", "admin")
FOLDER = "/content/dam/multisite/press-releases"

s = requests.Session()
s.auth = AUTH

# folder ke andar ke sab children
r = s.get(f"{HOST}{FOLDER}.1.json")
for name, val in r.json().items():
    if isinstance(val, dict) and val.get("jcr:primaryType") == "dam:Asset":
        path = f"{FOLDER}/{name}"
        s.post(f"{HOST}/bin/replicate.json", data={"cmd": "deactivate", "path": path})
        d = s.delete(f"{HOST}/api/assets{path[len('/content/dam'):]}")
        print(name, "-> deleted", d.status_code)