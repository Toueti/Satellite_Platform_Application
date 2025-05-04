import os
import statistics
from dotenv import load_dotenv
load_dotenv()

from flask import Flask
from flask import Flask
from flask_cors import CORS
# from flask_caching import Cache  #  Flask-Caching is not used
from gee_app.utils.logging import configure_logging
from gee_app.utils.auth import initialize_ee
# from gee_app.routes.api_routes_2 import register_blueprints
from gee_app.routes.api_routes import register_blueprints
# Use cache_utils for initialization now
# from gee_app.utils.cache_utils import initialize_db
from asgiref.wsgi import WsgiToAsgi
import uvicorn
import socket
# Removed BackgroundScheduler, statistics, mongo_db imports

os.environ['PYTHONIOENCODING'] = 'UTF-8'

# Removed unused cache import


# Removed update_cache function

def create_app():
    print("Inside create_app()")
    app = Flask(__name__)
    CORS(app)

    app.config['LOG_LEVEL'] = 'DEBUG'
    app.config['DEBUG'] = True
    configure_logging(app)
    register_blueprints(app)
    initialize_ee()
    # This now calls the Redis initialization/connection test
    # initialize_db()

    # Removed scheduler setup

    return app

asgi_app = WsgiToAsgi(create_app())

def check_port(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        try:
            s.bind((host, port))
            return True
        except OSError:
            return False

if __name__ == '__main__':
    host = '127.0.0.1'
    port = 5000
    if not check_port(host, port):
        print(f"Port {port} is in use. Please free it or change the port in the code.")
        exit(1)
    uvicorn.run(
        "app:asgi_app",
        host=host,
        port=port,
        log_level="debug",
        reload=True
    )
