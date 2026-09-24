from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class UserSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    nombre: str
    email: str
    rol: str
    curso: str | None = None
    nivel: int | None = None
    exp: int | None = None
    electronios: int | None = None


class LoginRequest(BaseModel):
    email: str = Field(min_length=3)
    password: str = Field(min_length=1)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserSummary


class ProfileUpdate(BaseModel):
    nombre: str = Field(min_length=1, max_length=255)
    email: str = Field(min_length=3, max_length=255)
    password: str | None = Field(default=None, min_length=1)


class CardSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    titulo: str
    descripcion: str | None = None
    precio: int
    activa: bool
    image_url: str | None = None
    quantity: int = 0


class CardCreate(BaseModel):
    titulo: str = Field(min_length=1, max_length=50)
    descripcion: str | None = None
    precio: int = Field(ge=0)
    activa: bool = True
    imagen_base64: str | None = None


class CardUpdate(BaseModel):
    titulo: str | None = Field(default=None, min_length=1, max_length=50)
    descripcion: str | None = None
    precio: int | None = Field(default=None, ge=0)
    activa: bool | None = None
    imagen_base64: str | None = None


class StudentCreate(BaseModel):
    nombre: str = Field(min_length=1, max_length=255)
    email: str = Field(min_length=3, max_length=255)
    password: str = Field(min_length=1)
    curso: str | None = None


class StudentUpdate(BaseModel):
    nombre: str | None = Field(default=None, min_length=1, max_length=255)
    email: str | None = Field(default=None, min_length=3, max_length=255)
    password: str | None = Field(default=None, min_length=1)
    curso: str | None = None
    exp: int | None = Field(default=None, ge=0)
    electronios: int | None = Field(default=None, ge=0)


class AwardExperience(BaseModel):
    amount: int = Field(gt=0)
    curso: str | None = None
    student_ids: list[int] = []


class BadgeCreate(BaseModel):
    nombre: str = Field(min_length=1, max_length=50)
    imagen_base64: str | None = None


class HistoryItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    tipo: str | None
    descripcion: str | None
    electronios_en_momento: int
    fecha: datetime


class DashboardResponse(BaseModel):
    user: UserSummary
    web_name: str
    cards_available: int
    students_count: int
    owned_cards: int
    recent_history: list[HistoryItem]


class WebConfigUpdate(BaseModel):
    nombre: str = Field(min_length=1, max_length=100)
