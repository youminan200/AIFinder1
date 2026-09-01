from flask import Flask, request, jsonify, render_template_string
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

@app.route('/admin', methods=['GET'])
def admin_dashboard():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('SELECT id, username, password_hash, email FROM users')
    users = cursor.fetchall()
    conn.close()
    
    html = '''
    <!DOCTYPE html>
    <html>
    <head>
        <title>AIfinder DB Admin</title>
        <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #e2e8f0; margin: 0; padding: 40px; }
            h1 { color: #38bdf8; text-align: center; margin-bottom: 30px; font-weight: 600; }
            table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5); }
            th, td { padding: 16px 20px; text-align: left; }
            th { background: #0ea5e9; color: white; text-transform: uppercase; font-size: 14px; letter-spacing: 0.05em; }
            tr:nth-child(even) { background: #0f172a; }
            tr:hover { background: #334155; }
            td { border-bottom: 1px solid #334155; }
            .badge { background: #10b981; color: white; padding: 4px 8px; border-radius: 9999px; font-size: 12px; font-weight: bold; }
        </style>
    </head>
    <body>
        <h1>🛡️ AIfinder Database Viewer 🛡️</h1>
        <table>
            <thead>
                <tr>
                    <th>DB 고유 ID</th>
                    <th>가입된 아이디 (Username)</th>
                    <th>비밀번호 (Password)</th>
                    <th>상태</th>
                </tr>
            </thead>
            <tbody>
                {% for user in users %}
                <tr>
                    <td style="color: #94a3b8; font-family: monospace;">{{ user.id }}</td>
                    <td style="color: #38bdf8; font-weight: bold;">{{ user.username }}</td>
                    <td style="color: #f472b6;">{{ user.password_hash }}</td>
                    <td><span class="badge">가입 완료</span></td>
                </tr>
                {% else %}
                <tr>
                    <td colspan="4" style="text-align: center; color: #94a3b8;">아직 가입된 유저가 없습니다. 앱에서 회원가입을 진행해주세요!</td>
                </tr>
                {% endfor %}
            </tbody>
        </table>
    </body>
    </html>
    '''
    return render_template_string(html, users=users)

if __name__ == '__main__':
    init_db()
    print("Flask Server running on http://0.0.0.0:5000")
    app.run(host='0.0.0.0', port=5000)
