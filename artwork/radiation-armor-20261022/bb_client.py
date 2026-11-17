"""Small client for the configured local Blockbench MCP, with task-local receipts."""
import base64
import json
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RPC_URL = 'http://127.0.0.1:3000/bb-mcp'


def rpc(method, params=None, session=None):
    payload = {'jsonrpc': '2.0', 'id': 1, 'method': method}
    if params is not None:
        payload['params'] = params
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}
    if session:
        headers['mcp-session-id'] = session
    req = urllib.request.Request(RPC_URL, data=json.dumps(payload).encode(), headers=headers)
    with urllib.request.urlopen(req, timeout=50) as response:
        body = response.read().decode()
        if body.startswith(('event:', 'data:')):
            body = next(line[5:].strip() for line in body.splitlines() if line.startswith('data:'))
        return json.loads(body), response.headers.get('mcp-session-id')


def init():
    response, session = rpc('initialize', {
        'protocolVersion': '2024-11-05', 'capabilities': {},
        'clientInfo': {'name': 'codex-radiation-armor', 'version': '1.0'}
    })
    (ROOT / 'mcp-session.json').write_text(json.dumps({'session': session, 'server': response}), encoding='utf-8')
    return session


def session_id():
    return json.loads((ROOT / 'mcp-session.json').read_text(encoding='utf-8'))['session']


def call(name, args=None):
    response, _ = rpc('tools/call', {'name': name, 'arguments': args or {}}, session_id())
    (ROOT / ('receipt-' + name + '.json')).write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding='utf-8')
    if 'error' in response:
        raise RuntimeError(response['error'])
    result = response['result']
    if result.get('isError'):
        raise RuntimeError(result)
    return result


def text_result(result):
    return '\n'.join(item.get('text', '') for item in result.get('content', []) if item['type'] == 'text')


if __name__ == '__main__':
    if sys.argv[1] == 'discover':
        sid = init()
        response, _ = rpc('tools/list', {}, sid)
        (ROOT / 'mcp-tools.json').write_text(json.dumps(response, ensure_ascii=False, indent=2), encoding='utf-8')
        names = set(sys.argv[2:])
        for tool in response['result']['tools']:
            if tool['name'] in names:
                print(json.dumps(tool, ensure_ascii=False))
    else:
        args = json.loads(Path(sys.argv[2]).read_text(encoding='utf-8-sig')) if len(sys.argv) > 2 else {}
        result = call(sys.argv[1], args)
        for item in result.get('content', []):
            if item['type'] == 'image':
                target = ROOT / ('capture-' + sys.argv[1] + '.png')
                target.write_bytes(base64.b64decode(item['data']))
                print('IMAGE', target)
            elif item['type'] == 'text':
                print(item['text'][:11000])
