import base64
import json
from bb_client import call, text_result, ROOT

VIEWS = {
    'front-three-quarter': [48, 35, -65],
    'front': [0, 22, -70],
    'back-three-quarter': [-48, 35, 65],
    'back': [0, 22, 70],
    'left': [70, 22, 0],
}


def snapshot(name, position):
    call('set_camera_angle', {'position': position, 'target': [0, 16.5, 0], 'projection': 'orthographic'})
    call('risky_eval', {'code': 'Preview.selected.camera.zoom=0.34;Preview.selected.camera.updateProjectionMatrix();Canvas.updateAll();"camera fitted"'})
    result = call('capture_screenshot')
    for item in result.get('content', []):
        if item['type'] == 'image':
            path = ROOT / ('preview-' + name + '.png')
            path.write_bytes(base64.b64decode(item['data']))
            print('SAVED', path.name, flush=True)


if __name__ == '__main__':
    for name, pos in VIEWS.items():
        snapshot(name, pos)
    print(text_result(call('get_project_info')),flush=True)
