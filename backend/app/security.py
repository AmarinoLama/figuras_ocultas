from datetime import datetime, timedelta, timezone

import jwt
from fastapi import HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from passlib.context import CryptContext

from .config import get_settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
bearer_scheme = HTTPBearer(auto_error=False)


def create_access_token(user_id: int) -> str:
    settings = get_settings()
    expires = datetime.now(timezone.utc) + timedelta(minutes=settings.access_token_expire_minutes)
    return jwt.encode({"sub": str(user_id), "exp": expires}, settings.secret_key, algorithm="HS256")


def decode_access_token(token: str) -> int:
    try:
        payload = jwt.decode(token, get_settings().secret_key, algorithms=["HS256"])
        return int(payload["sub"])
    except (jwt.PyJWTError, KeyError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido") from exc
