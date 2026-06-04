from flask import Flask, request, jsonify
import sqlite3
import os

app = Flask(__name__)
DB_FILE = os.environ.get('DATABASE_FILE', 'database.db')
if os.path.exists('/data'):
    DB_FILE = '/data/database.db'


def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()
    # Users table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            email TEXT
        )
    ''')
    # Items table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS items (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            name TEXT NOT NULL,
            area_id INTEGER,
            area_name TEXT,
            timestamp INTEGER,
            photo_uri TEXT,
            is_favorite INTEGER DEFAULT 0,
            FOREIGN KEY (user_id) REFERENCES users (id)
        )
    ''')
    conn.commit()
    conn.close()

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': 'Missing body'}), 400
        
    user_id = data.get('id')
    username = data.get('username')
    password_hash = data.get('passwordHash')
    email = data.get('email')
    
    if not username or not password_hash:
        return jsonify({'success': False, 'message': 'Missing data'}), 400
        
    conn = get_db()
    cursor = conn.cursor()
    try:
        cursor.execute(
            'INSERT INTO users (id, username, password_hash, email) VALUES (?, ?, ?, ?)',
            (user_id, username, password_hash, email)
        )
        conn.commit()
        return jsonify({'success': True, 'user': {'id': user_id, 'username': username, 'email': email}})
    except sqlite3.IntegrityError:
        return jsonify({'success': False, 'message': 'Username already exists'}), 400
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': 'Missing body'}), 400
        
    username = data.get('username')
    password_hash = data.get('passwordHash')
    
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM users WHERE username = ? AND password_hash = ?', (username, password_hash))
    user = cursor.fetchone()
    conn.close()
    
    if user:
        return jsonify({
            'success': True,
            'user': {
                'id': user['id'],
                'username': user['username'],
                'email': user['email']
            }
        })
    else:
        return jsonify({'success': False, 'message': 'Invalid credentials'}), 401

@app.route('/items', methods=['GET'])
def get_items():
    user_id = request.args.get('userId')
    if not user_id:
        return jsonify({'success': False, 'message': 'Missing userId'}), 400
        
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT * FROM items WHERE user_id = ?', (user_id,))
    rows = cursor.fetchall()
    conn.close()
    
    items = []
    for r in rows:
        items.append({
            'id': r['id'],
            'userId': r['user_id'],
            'name': r['name'],
            'areaId': r['area_id'],
            'areaName': r['area_name'],
            'timestamp': r['timestamp'],
            'photoUri': r['photo_uri'],
            'isFavorite': r['is_favorite'] == 1
        })
    return jsonify(items)

@app.route('/items/sync', methods=['POST'])
def sync_items():
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': 'Missing body'}), 400
        
    user_id = data.get('userId')
    item_list = data.get('items', [])
    
    if not user_id:
        return jsonify({'success': False, 'message': 'Missing userId'}), 400
        
    conn = get_db()
    cursor = conn.cursor()
    try:
        cursor.execute('DELETE FROM items WHERE user_id = ?', (user_id,))
        for item in item_list:
            cursor.execute(
                '''INSERT INTO items (id, user_id, name, area_id, area_name, timestamp, photo_uri, is_favorite) 
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)''',
                (
                    item.get('id'),
                    user_id,
                    item.get('name'),
                    item.get('areaId'),
                    item.get('areaName'),
                    item.get('timestamp'),
                    item.get('photoUri'),
                    1 if item.get('isFavorite') else 0
                )
            )
        conn.commit()
        return jsonify({'success': True})
    except Exception as e:
        conn.rollback()
        return jsonify({'success': False, 'message': str(e)}), 500
    finally:
        conn.close()

if __name__ == '__main__':
    init_db()
    print("Flask Server running on http://0.0.0.0:5000")
    app.run(host='0.0.0.0', port=5000)
