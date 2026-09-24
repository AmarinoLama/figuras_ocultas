from datetime import datetime
from enum import Enum

from sqlalchemy import Boolean, DateTime, Enum as SqlEnum, ForeignKey, Integer, LargeBinary, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .db import Base


class RolUsuario(str, Enum):
    ADMIN = "ADMIN"
    ALUMNO = "ALUMNO"


class CursoAlumno(str, Enum):
    PRIMERO_ESO_A = "PRIMERO_ESO_A"
    PRIMERO_ESO_B = "PRIMERO_ESO_B"
    PRIMERO_ESO_C = "PRIMERO_ESO_C"
    SEGUNDO_ESO_A = "SEGUNDO_ESO_A"
    SEGUNDO_ESO_B = "SEGUNDO_ESO_B"
    SEGUNDO_ESO_C = "SEGUNDO_ESO_C"
    TERCERO_ESO_A = "TERCERO_ESO_A"
    TERCERO_ESO_B = "TERCERO_ESO_B"
    TERCERO_ESO_C = "TERCERO_ESO_C"
    CUARTO_ESO_A = "CUARTO_ESO_A"
    CUARTO_ESO_B = "CUARTO_ESO_B"
    CUARTO_ESO_C = "CUARTO_ESO_C"


class Usuario(Base):
    __tablename__ = "usuarios"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(255))
    email: Mapped[str] = mapped_column(String(255), index=True)
    password: Mapped[str] = mapped_column(String(255))
    curso: Mapped[CursoAlumno | None] = mapped_column(SqlEnum(CursoAlumno, values_callable=lambda enum: [item.name for item in enum]), nullable=True)
    rol: Mapped[RolUsuario] = mapped_column(SqlEnum(RolUsuario, values_callable=lambda enum: [item.name for item in enum]))
    tarjeta: Mapped["TarjetaAlumno | None"] = relationship(back_populates="usuario", uselist=False)
    insignias: Mapped[list["Insignia"]] = relationship(back_populates="alumno")


class TarjetaAlumno(Base):
    __tablename__ = "tarjeta_alumno"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nivel: Mapped[int] = mapped_column(Integer, default=0)
    exp: Mapped[int] = mapped_column(Integer, default=0)
    electronios: Mapped[int] = mapped_column(Integer, default=0)
    usuario_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"), unique=True)
    usuario: Mapped[Usuario] = relationship(back_populates="tarjeta")


class Carta(Base):
    __tablename__ = "cartas"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    imagen: Mapped[bytes | None] = mapped_column(LargeBinary)
    precio: Mapped[int] = mapped_column(Integer, default=0)
    titulo: Mapped[str] = mapped_column(String(50))
    descripcion: Mapped[str | None] = mapped_column(Text)
    activa: Mapped[bool] = mapped_column(Boolean, default=True)


class Insignia(Base):
    __tablename__ = "insignias"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(50))
    imagen: Mapped[bytes | None] = mapped_column(LargeBinary)
    alumno_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"))
    alumno: Mapped[Usuario] = relationship(back_populates="insignias")


class WebConfig(Base):
    __tablename__ = "web_config"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    nombre: Mapped[str] = mapped_column(String(100))


class CartaUsuario(Base):
    __tablename__ = "cartas_usuario"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    carta_id: Mapped[int] = mapped_column(ForeignKey("cartas.id"))
    alumno_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"))
    fecha_adquisicion: Mapped[datetime] = mapped_column(DateTime, nullable=False, server_default="CURRENT_TIMESTAMP")
    usada: Mapped[bool] = mapped_column(Boolean, default=False, nullable=True)
    fecha_usada: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class HistorialTransaccion(Base):
    __tablename__ = "historial_transacciones"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    alumno_id: Mapped[int] = mapped_column(ForeignKey("usuarios.id"))
    tipo: Mapped[str | None] = mapped_column(String(50))
    descripcion: Mapped[str | None] = mapped_column(Text)
    electronios_en_momento: Mapped[int] = mapped_column(Integer)
    fecha: Mapped[datetime] = mapped_column(DateTime, nullable=False, server_default="CURRENT_TIMESTAMP")
