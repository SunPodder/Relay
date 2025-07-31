#!/usr/bin/env python3
"""
Test client for the Relay server conn/ack protocol.
This script connects to the Android Relay server and sends a conn message.
"""

import socket
import json
import uuid
import time
import struct

def create_pong_message(ping_id=None):
    """Create a pong message in response to a ping."""
    pong_msg = {
        "type": "pong",
        "timestamp": int(time.time()),
        "payload": {
            "device": "Test-Client"
        }
    }
    if ping_id:
        pong_msg["id"] = ping_id
    else:
        pong_msg["id"] = str(uuid.uuid4())
    return pong_msg

def create_conn_message():
    """Create a conn message according to the protocol."""
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
    """Send a JSON message with 4-byte length prefix."""
    message_json = json.dumps(message_dict)
    message_bytes = message_json.encode('utf-8')
    message_length = len(message_bytes)
    
    # Create 4-byte big-endian length prefix
    length_prefix = struct.pack('>I', message_length)
    
    # Send length prefix followed by message
    sock.send(length_prefix + message_bytes)
    print(f"Sent: {message_json}")

def read_message(sock):
    """Read a JSON message from socket using 4-byte length prefix."""
    try:
        # First, read the 4-byte length prefix
        length_data = b""
        while len(length_data) < 4:
            chunk = sock.recv(4 - len(length_data))
            if not chunk:
                return None
            length_data += chunk
        
        # Unpack the length (big-endian unsigned int)
        message_length = struct.unpack('>I', length_data)[0]
        
        # Read the message data
        message_data = b""
        while len(message_data) < message_length:
            chunk = sock.recv(message_length - len(message_data))
            if not chunk:
                return None
            message_data += chunk
        
        # Decode and parse JSON
        message_json = message_data.decode('utf-8')
        return json.loads(message_json)
        
    except Exception as e:
        print(f"Error reading message: {e}")
        return None

def test_conn_protocol():
    """Test the conn/ack protocol with the Relay server."""
    print("=== Relay Server Conn/Ack Protocol Test ===")
    
    # Connect to the server
    server_host = "192.168.1.102"  # Change if running on different device
    server_port = 9999
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)  # 10 second timeout
        
        print(f"Connecting to {server_host}:{server_port}...")
        sock.connect((server_host, server_port))
        print("Connected!")
        
        # Send conn message
        conn_message = create_conn_message()
        send_message(sock, conn_message)
        
        # Wait for ack response
        print("Waiting for ack response...")
        response = read_message(sock)
        
        if response:
            print(f"Received: {json.dumps(response, indent=2)}")
            
            if response.get("type") == "ack":
                payload = response.get("payload", {})
                status = payload.get("status")
                ref_id = payload.get("ref_id")
                
                if status == "ok":
                    print("✅ Connection accepted!")
                    print(f"   Reference ID: {ref_id}")
                    
                    # Keep connection alive and handle ping messages
                    print("Listening for ping messages (will respond with pong)...")
                    start_time = time.time()
                    max_duration = 120  # Listen for 2 minutes
                    
                    while time.time() - start_time < max_duration:
                        try:
                            sock.settimeout(5)  # 5 second timeout for each read
                            message = read_message(sock)
                            
                            if message:
                                msg_type = message.get("type")
                                if msg_type == "ping":
                                    # Respond to ping with pong
                                    ping_id = message.get("id")
                                    pong_message = create_pong_message(ping_id)
                                    send_message(sock, pong_message)
                                    print(f"📤 Responded to ping (ID: {ping_id}) with pong")
                                else:
                                    print(f"📨 Received {msg_type} message: {json.dumps(message, indent=2)}")
                            
                        except socket.timeout:
                            # No message received, continue listening
                            print(".", end="", flush=True)
                            continue
                        except Exception as e:
                            print(f"\n❌ Error reading message: {e}")
                            break
                    
                    print(f"\n✅ Test completed after {max_duration} seconds")
                    
                elif status == "error":
                    reason = payload.get("reason", "Unknown error")
                    print(f"❌ Connection rejected: {reason}")
                else:
                    print(f"⚠️  Unknown status: {status}")
            else:
                print(f"⚠️  Expected 'ack' message, got: {response.get('type')}")
        else:
            print("❌ No response received")
            
    except socket.timeout:
        print("❌ Connection timed out")
    except ConnectionRefusedError:
        print(f"❌ Connection refused. Is the server running on {server_host}:{server_port}?")
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        sock.close()
        print("Connection closed.")

if __name__ == "__main__":
    test_conn_protocol()
