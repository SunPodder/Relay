#!/usr/bin/env python3
"""
Automated test for the Relay server conn/ack protocol.
"""
import socket
import json
import uuid
import time
import struct

def create_pong_message(ping_id=None):
    pong_msg = {
        "type": "pong",
        "timestamp": int(time.time()),
        "payload": {"device": "Test-Client"}
    }
    pong_msg["id"] = ping_id or str(uuid.uuid4())
    return pong_msg

def create_conn_message():
    return {
        "type": "conn",
        "id": str(uuid.uuid4()),
        "timestamp": int(time.time()),
        "payload": {
            "device_name": "Test-Client",
            "platform": "linux",
            "version": "1.0.0",
            "supports": ["sms", "ping"],
            "auth_token": "test-token-123"
        }
    }

def send_message(sock, message_dict):
    message_json = json.dumps(message_dict)
    message_bytes = message_json.encode('utf-8')
    length_prefix = struct.pack('>I', len(message_bytes))
    sock.send(length_prefix + message_bytes)

def read_message(sock):
    try:
        length_data = b""
        while len(length_data) < 4:
            chunk = sock.recv(4 - len(length_data))
            if not chunk:
                return None
            length_data += chunk
        message_length = struct.unpack('>I', length_data)[0]
        message_data = b""
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
        send_message(sock, create_conn_message())
        response = read_message(sock)
        if not response or response.get("type") != "ack":
            print("FAIL: No ack or wrong type")
            return False
        payload = response.get("payload", {})
        if payload.get("status") != "ok":
            print("FAIL: Ack status not ok")
            return False
        print("PASS: Connection established")
        # Listen for ping and respond with pong
        start = time.time()
        print("Listening for ping...")
        while time.time() - start < 60:
            sock.settimeout(2)
            msg = read_message(sock)
            if msg and msg.get("type") == "ping":
                send_message(sock, create_pong_message(msg.get("id")))
                print("PASS: Responded to ping")
                return True
        print("FAIL: No ping received")
        return False
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
