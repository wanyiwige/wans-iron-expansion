"""Local Blockbench MCP client. All model mutations go through the editor's MCP tools."""
from pathlib import Path
import urllib.request,json,sys,base64
OUT=Path(__file__).resolve().parents[1]/'development-checks/toxic-audit'
ENDPOINT='http://127.0.0.1:3000/bb-mcp'
def call(name,args):
    sid=json.loads((OUT/'blockbench-session.json').read_text())['session']
    request={'jsonrpc':'2.0','id':100,'method':'tools/call','params':{'name':name,'arguments':args}}
    r=urllib.request.urlopen(urllib.request.Request(ENDPOINT,data=json.dumps(request).encode(),headers={'Content-Type':'application/json','Accept':'application/json, text/event-stream','mcp-session-id':sid}),timeout=45)
    text=r.read().decode()
    if text.startswith('event:') or text.startswith('data:'):
        text=next(l[5:].strip() for l in text.splitlines() if l.startswith('data:'))
    data=json.loads(text)
    (OUT/('bb-'+name+'-last.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
    return data
if __name__=='__main__':
    name=sys.argv[1];args=json.loads(Path(sys.argv[2]).read_text(encoding='utf-8')) if len(sys.argv)>2 else {}
    data=call(name,args)
    print(json.dumps(data,ensure_ascii=False)[:6000])
