#!/usr/bin/python3

import socket
import json
import time
import uuid
import struct

def read_message(sock):
    """Read a complete message from the socket using length prefix"""
    try:
        # Read the 4-byte length prefix (big-endian unsigned int)
        length_data = b""
        while len(length_data) < 4:
            chunk = sock.recv(4 - len(length_data))
            if not chunk:
                return None
            length_data += chunk
        
        # Unpack the length (big-endian)
        message_length = struct.unpack('>I', length_data)[0]
        
        # Read the JSON message of specified length
        message_data = b""
        while len(message_data) < message_length:
            chunk = sock.recv(message_length - len(message_data))
            if not chunk:
                return None
            message_data += chunk
        
        return message_data.decode('utf-8')
    except Exception as e:
        print(f"Error reading message: {e}")
        return None

def send_message(sock, message):
    """Send a message with length prefix"""
    try:
        # Encode the JSON message
        message_bytes = message.encode('utf-8')
        message_length = len(message_bytes)
        
        # Create 4-byte big-endian length prefix
        length_prefix = struct.pack('>I', message_length)
        
        # Send length prefix followed by message
        sock.send(length_prefix + message_bytes)
    except Exception as e:
        print(f"Error sending message: {e}")

def listen_for_notifications():
    """Connect to server and listen for notification messages"""
    
    # Connect to server
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect(('192.168.1.103', 9999))
        print("Connected to Relay Server!")
        
        # Send conn message
        conn_msg = {
            "type": "conn",
            "id": str(uuid.uuid4()),
            "timestamp": int(time.time()),
            "payload": {
                "device_name": "Notification-Listener",
                "platform": "linux",
                "version": "1.0.0",
                "supports": ["notification"],
                "auth_token": "test-token-123"
            }
        }
        
        print("Sending connection request...")
        send_message(sock, json.dumps(conn_msg))
        
        # Wait for ack
        response = read_message(sock)
        if response:
            ack = json.loads(response)
            if ack.get("type") == "ack" and ack.get("payload", {}).get("status") == "ok":
                print("✅ Connected! Listening for notifications...")
                print("Trigger some notifications on your Android device to see them here.")
                print("Press Ctrl+C to exit.")
                print("")
            else:
                print("❌ Connection failed:", ack)
                return
        
        # Listen for messages
        while True:
            try:
                message = read_message(sock)
                if message:
                    data = json.loads(message)
                    msg_type = data.get("type")
                    
                    if msg_type == "notification":
                        payload = data.get("payload", {})
                        print(f"🔔 NOTIFICATION:")
                        print(f"   App: {payload.get('app', 'Unknown')}")
                        print(f"   Title: {payload.get('title', '')}")
                        print(f"   Body: {payload.get('body', '')}")
                        print(f"   Package: {payload.get('package', '')}")
                        print(f"   Can Reply: {payload.get('can_reply', False)}")
                        if payload.get('actions'):
                            print(f"   Actions: {len(payload['actions'])} available")
                        print("")
                    
                    elif msg_type == "ping":
                        # Respond to ping
                        pong_msg = {
                            "type": "pong",
                            "id": data.get("id"),
                            "timestamp": int(time.time()),
                            "payload": {"device": "Notification-Listener"}
                        }
                        send_message(sock, json.dumps(pong_msg))
                        print("📤 Responded to ping")
                    
                    else:
                        print(f"📨 Received {msg_type} message")
                        
            except KeyboardInterrupt:
                print("\n👋 Disconnecting...")
                break
            except Exception as e:
                print(f"❌ Error: {e}")
                break
                
    except Exception as e:
        print(f"❌ Connection error: {e}")
    finally:
        sock.close()

if __name__ == "__main__":
    listen_for_notifications()
