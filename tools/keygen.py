#!/usr/bin/env python3
"""Генератор кодов продления Camera Blocker.

Использование: python keygen.py 2027-08-01
Выдаёт код, который товарищ вводит в приложении (Кнопка «Код продления»).
Код действует до указанной даты (включительно).
"""
import hmac
import hashlib
import sys
from datetime import date

SECRET = b"uvz-cb-2026-slushar-biz"  # должен совпадать с SECRET в License.kt


def main():
    if len(sys.argv) != 2:
        print("Формат: python keygen.py ГГГГ-ММ-ДД  (дата, до которой работает приложение)")
        sys.exit(1)
    target = date.fromisoformat(sys.argv[1])
    msg = f"extend:{target.isoformat()}".encode()
    code = hmac.new(SECRET, msg, hashlib.sha256).hexdigest()[:8].upper()
    print(code)


if __name__ == "__main__":
    main()
