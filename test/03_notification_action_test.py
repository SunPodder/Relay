#!/usr/bin/env python3
"""
Automated test for notification action and reply protocol.
"""
import socket
import json
import struct
import time
import uuid

def send_message(sock, message):
    message_bytes = json.dumps(message).encode('utf-8')
    length_prefix = struct.pack('>I', len(message_bytes))
    sock.send(length_prefix + message_bytes)

def read_message(sock):
    try:
        length_data = b''
        while len(length_data) < 4:
            chunk = sock.recv(4 - len(length_data))
            if not chunk:
                return None
            length_data += chunk
        message_length = struct.unpack('>I', length_data)[0]
        message_data = b''
        while len(message_data) < message_length:
            chunk = sock.recv(message_length - len(message_data))
            if not chunk:
                return None
            message_data += chunk
        return json.loads(message_data.decode('utf-8'))
    except Exception:
        return None

def run_test():
    server_host = "192.168.1.100"
    server_port = 9999
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((server_host, server_port))
        conn_msg = {
            "type": "conn",
            "id": str(uuid.uuid4()),
            "timestamp": int(time.time()),
            "payload": {
                "device_name": "Notification-Action-Tester",
                "platform": "linux",
                "version": "1.0.0",
                "supports": ["notification"],
                "auth_token": "test-token-123"
            }
        }
        send_message(sock, conn_msg)
        response = read_message(sock)
        if not response or response.get("type") != "ack" or response.get("payload", {}).get("status") != "ok":
            print("FAIL: No ack or wrong ack")
            return False
        # Wait for notification
        start = time.time()
        notification = None
        while time.time() - start < 10:
            msg = read_message(sock)
            if msg and msg.get("type") == "notification":
                notification = msg
                break
        if not notification:
            print("FAIL: No notification received")
            return False
        notif_id = notification["payload"]["id"]
        actions = notification["payload"].get("actions", [])
        if not actions:
            print("FAIL: No actions in notification")
            return False
        # Try triggering the first action
        action_key = actions[0]["key"]
        action_type = actions[0]["type"]
        action_msg = {
            "type": "notification_action",
            "payload": {
                "id": notif_id,
                "key": action_key,
                "type": action_type
            }
        }
        send_message(sock, action_msg)
        print("PASS")
        return True
    except Exception as e:
        print(f"FAIL: Exception {e}")
        return False
    finally:
        try:
            sock.close()
        except:
            pass

if __name__ == "__main__":
    result = run_test()
    exit(0 if result else 1)
