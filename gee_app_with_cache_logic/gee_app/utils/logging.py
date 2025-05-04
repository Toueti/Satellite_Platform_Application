import logging
import os
from flask import Flask
from colorama import Fore, Style, init

init(autoreset=True)

logger = logging.getLogger('gee_app')

class ColoredFormatter(logging.Formatter):
    LOG_COLORS = {
        'INFO': Fore.GREEN,
        'DEBUG': Fore.CYAN,
        'WARNING': Fore.YELLOW,
        'ERROR': Fore.RED,
        'CRITICAL': Fore.LIGHTRED_EX,
    }
    LOG_EMOJIS = {
        'INFO': '✅',
        'DEBUG': '🔍',
        'WARNING': '⚠️',
        'ERROR': '❌',
        'CRITICAL': '🔥',
    }

    def format(self, record):
        log_level = record.levelname
        color = self.LOG_COLORS.get(log_level, '')
        emoji = self.LOG_EMOJIS.get(log_level, '  ')
        record.levelname = f"{emoji} {log_level}"
        if color:
            return f"{color}{super().format(record)}{Style.RESET_ALL}"
        else:
            return super().format(record)

def configure_logging(app: Flask) -> None:
    log_level = app.config.get('LOG_LEVEL', 'INFO')
    log_file = os.path.join(app.root_path, '..', 'app.log')

    # Clear existing handlers to prevent duplicates
    logger.handlers.clear()

    file_handler = logging.FileHandler(log_file, encoding='utf-8')
    file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(ColoredFormatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))

    logger.setLevel(getattr(logging, log_level.upper()))
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    if os.environ.get('PYTHONIOENCODING') != 'utf-8':
        logger.warning("⚠️ PYTHONIOENCODING environment variable is not set to UTF-8. Emojis may not display correctly in the console.")
    else:
        logger.debug("🔍 PYTHONIOENCODING is set to UTF-8, emojis should display correctly.")