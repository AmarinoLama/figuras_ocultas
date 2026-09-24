from fastapi import Depends, FastAPI, HTTPException, Query, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from .config import get_settings
from .db import get_db
from .dependencies import get_current_user
from .models import Carta, CartaUsuario, CursoAlumno, HistorialTransaccion, Insignia, RolUsuario, TarjetaAlumno, Usuario, WebConfig
from .schemas import AwardExperience, BadgeCreate, CardCreate, CardSummary, CardUpdate, DashboardResponse, HistoryItem, LoginRequest, ProfileUpdate, StudentCreate, StudentUpdate, TokenResponse, UserSummary, WebConfigUpdate
from .security import create_access_token

settings = get_settings()
app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    docs_url="/api/docs",
    openapi_url="/api/openapi.json",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def require_admin(user: Usuario) -> Usuario:
    if user.rol != RolUsuario.ADMIN:
        raise HTTPException(status_code=403, detail="Solo los administradores pueden realizar esta acción")
    return user


def decode_image(value: str | None) -> bytes | None:
    if not value:
        return None
    import base64
    try:
        return base64.b64decode(value.split(",")[-1], validate=True)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Imagen base64 inválida") from exc


def user_summary(user: Usuario) -> UserSummary:
    card = user.tarjeta
    return UserSummary(
        id=user.id,
        nombre=user.nombre,
        email=user.email,
        rol=user.rol.value if hasattr(user.rol, "value") else str(user.rol),
        curso=user.curso.value if user.curso and hasattr(user.curso, "value") else (str(user.curso) if user.curso else None),
        nivel=card.nivel if card else None,
        exp=card.exp if card else None,
        electronios=card.electronios if card else None,
    )


@app.get("/api/health")
def health(db: Session = Depends(get_db)) -> dict[str, str]:
    db.execute(select(1))
    return {"status": "ok", "service": "figuras-ocultas-api"}


@app.post("/api/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    user = db.scalar(select(Usuario).options(joinedload(Usuario.tarjeta)).where(Usuario.email == payload.email))
    if user is None or user.password != payload.password:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Correo o contraseña incorrectos")
    return TokenResponse(access_token=create_access_token(user.id), user=user_summary(user))


@app.get("/api/auth/me", response_model=UserSummary)
def me(user: Usuario = Depends(get_current_user)) -> UserSummary:
    return user_summary(user)


@app.patch("/api/profile", response_model=UserSummary)
def update_profile(payload: ProfileUpdate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> UserSummary:
    duplicate = db.scalar(select(Usuario).where(Usuario.email == payload.email, Usuario.id != user.id))
    if duplicate:
        raise HTTPException(status_code=409, detail="Ese correo ya está en uso")
    user.nombre = payload.nombre
    user.email = payload.email
    if payload.password:
        user.password = payload.password
    db.commit()
    db.refresh(user)
    return user_summary(user)


@app.get("/api/dashboard", response_model=DashboardResponse)
def dashboard(user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> DashboardResponse:
    web_name = db.scalar(select(WebConfig.nombre).order_by(WebConfig.id).limit(1)) or "Figuras Ocultas"
    cards_available = db.scalar(select(func.count(Carta.id)).where(Carta.activa.is_(True))) or 0
    students_count = db.scalar(select(func.count(Usuario.id)).where(Usuario.rol == RolUsuario.ALUMNO)) or 0
    owned_cards = db.scalar(select(func.count(CartaUsuario.id)).where(CartaUsuario.alumno_id == user.id)) or 0
    history = list(db.scalars(select(HistorialTransaccion).where(HistorialTransaccion.alumno_id == user.id).order_by(HistorialTransaccion.fecha.desc()).limit(5)))
    return DashboardResponse(
        user=user_summary(user),
        web_name=web_name,
        cards_available=cards_available,
        students_count=students_count,
        owned_cards=owned_cards,
        recent_history=history,
    )


@app.get("/api/cards", response_model=list[CardSummary])
def cards(
    page: int = Query(1, ge=1),
    size: int = Query(12, ge=1, le=50),
    user: Usuario = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[CardSummary]:
    query = select(Carta)
    if user.rol != RolUsuario.ADMIN:
        query = query.where(Carta.activa.is_(True))
    query = query.order_by(Carta.id.desc()).offset((page - 1) * size).limit(size)
    items = list(db.scalars(query))
    quantities: dict[int, int] = {}
    if user.rol != RolUsuario.ADMIN:
        owned = db.execute(select(CartaUsuario.carta_id, func.count(CartaUsuario.id)).where(CartaUsuario.alumno_id == user.id).group_by(CartaUsuario.carta_id)).all()
        quantities = dict(owned)
    return [CardSummary.model_validate(item).model_copy(update={"image_url": f"/api/cards/{item.id}/image", "quantity": quantities.get(item.id, 0)}) for item in items]


@app.post("/api/cards", response_model=CardSummary)
def create_card(payload: CardCreate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> CardSummary:
    require_admin(user)
    card = Carta(titulo=payload.titulo, descripcion=payload.descripcion, precio=payload.precio, activa=payload.activa, imagen=decode_image(payload.imagen_base64))
    db.add(card)
    db.commit()
    db.refresh(card)
    return CardSummary.model_validate(card).model_copy(update={"image_url": f"/api/cards/{card.id}/image"})


@app.patch("/api/cards/{card_id}", response_model=CardSummary)
def update_card(card_id: int, payload: CardUpdate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> CardSummary:
    require_admin(user)
    card = db.get(Carta, card_id)
    if card is None:
        raise HTTPException(status_code=404, detail="Carta no encontrada")
    for field in ("titulo", "descripcion", "precio", "activa"):
        value = getattr(payload, field)
        if value is not None:
            setattr(card, field, value)
    if payload.imagen_base64 is not None:
        card.imagen = decode_image(payload.imagen_base64)
    db.commit()
    db.refresh(card)
    return CardSummary.model_validate(card).model_copy(update={"image_url": f"/api/cards/{card.id}/image"})


@app.delete("/api/cards/{card_id}", status_code=204)
def delete_card(card_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> Response:
    require_admin(user)
    card = db.get(Carta, card_id)
    if card is None:
        raise HTTPException(status_code=404, detail="Carta no encontrada")
    db.delete(card)
    db.commit()
    return Response(status_code=204)


@app.get("/api/cards/{card_id}/image")
def card_image(card_id: int, db: Session = Depends(get_db)) -> Response:
    image = db.scalar(select(Carta.imagen).where(Carta.id == card_id))
    if not image:
        raise HTTPException(status_code=404, detail="Imagen no encontrada")
    return Response(content=image, media_type="image/jpeg", headers={"Cache-Control": "public, max-age=86400"})


@app.get("/api/history", response_model=list[HistoryItem])
def history(user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> list[HistoryItem]:
    rows = db.scalars(select(HistorialTransaccion).where(HistorialTransaccion.alumno_id == user.id).order_by(HistorialTransaccion.fecha.desc()).limit(100))
    return list(rows)


@app.get("/api/students", response_model=list[UserSummary])
def students(user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> list[UserSummary]:
    require_admin(user)
    rows = db.scalars(select(Usuario).options(joinedload(Usuario.tarjeta)).where(Usuario.rol == RolUsuario.ALUMNO).order_by(Usuario.nombre))
    return [user_summary(row) for row in rows]


@app.post("/api/students", response_model=UserSummary)
def create_student(payload: StudentCreate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> UserSummary:
    require_admin(user)
    if db.scalar(select(Usuario).where(Usuario.email == payload.email)):
        raise HTTPException(status_code=409, detail="Ese correo ya está en uso")
    curso = CursoAlumno[payload.curso] if payload.curso in CursoAlumno.__members__ else None
    student = Usuario(nombre=payload.nombre, email=payload.email, password=payload.password, rol=RolUsuario.ALUMNO, curso=curso)
    db.add(student)
    db.flush()
    db.add(TarjetaAlumno(usuario_id=student.id, nivel=0, exp=0, electronios=0))
    db.commit()
    db.refresh(student)
    return user_summary(student)


@app.patch("/api/students/{student_id}", response_model=UserSummary)
def update_student(student_id: int, payload: StudentUpdate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> UserSummary:
    require_admin(user)
    student = db.scalar(select(Usuario).options(joinedload(Usuario.tarjeta)).where(Usuario.id == student_id, Usuario.rol == RolUsuario.ALUMNO))
    if student is None:
        raise HTTPException(status_code=404, detail="Alumno no encontrado")
    for field in ("nombre", "email", "password", "curso"):
        value = getattr(payload, field)
        if value is not None:
            setattr(student, field, CursoAlumno[value] if field == "curso" and value in CursoAlumno.__members__ else value)
    if student.tarjeta is None:
        student.tarjeta = TarjetaAlumno(usuario_id=student.id)
    if payload.exp is not None:
        student.tarjeta.exp = payload.exp
    if payload.electronios is not None:
        student.tarjeta.electronios = payload.electronios
    db.commit()
    return user_summary(student)


@app.delete("/api/students/{student_id}", status_code=204)
def delete_student(student_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> Response:
    require_admin(user)
    student = db.get(Usuario, student_id)
    if student is None or student.rol != RolUsuario.ALUMNO:
        raise HTTPException(status_code=404, detail="Alumno no encontrado")
    db.delete(student)
    db.commit()
    return Response(status_code=204)


@app.post("/api/students/award-experience")
def award_experience(payload: AwardExperience, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, int]:
    require_admin(user)
    query = select(Usuario).options(joinedload(Usuario.tarjeta)).where(Usuario.rol == RolUsuario.ALUMNO)
    if payload.curso:
        query = query.where(Usuario.curso == payload.curso)
    elif payload.student_ids:
        query = query.where(Usuario.id.in_(payload.student_ids))
    students_to_update = list(db.scalars(query))
    for student in students_to_update:
        if student.tarjeta is None:
            student.tarjeta = TarjetaAlumno(usuario_id=student.id)
        student.tarjeta.exp += payload.amount
    db.commit()
    return {"updated": len(students_to_update)}


@app.get("/api/config")
def config(db: Session = Depends(get_db)) -> dict[str, str]:
    return {"name": db.scalar(select(WebConfig.nombre).order_by(WebConfig.id).limit(1)) or "Figuras Ocultas"}


@app.patch("/api/config")
def update_config(payload: WebConfigUpdate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    require_admin(user)
    config_row = db.scalar(select(WebConfig).order_by(WebConfig.id).limit(1))
    if config_row is None:
        config_row = WebConfig(nombre=payload.nombre)
        db.add(config_row)
    else:
        config_row.nombre = payload.nombre
    db.commit()
    return {"name": payload.nombre}


@app.post("/api/cards/{card_id}/buy")
def buy_card(card_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    if user.rol == RolUsuario.ADMIN or user.tarjeta is None:
        raise HTTPException(status_code=403, detail="Solo los alumnos pueden comprar cartas")
    card = db.get(Carta, card_id)
    if card is None or not card.activa:
        raise HTTPException(status_code=404, detail="Carta no disponible")
    if user.tarjeta.electronios < card.precio:
        raise HTTPException(status_code=400, detail="No tienes suficientes electronios")
    user.tarjeta.electronios -= card.precio
    db.add(CartaUsuario(carta_id=card.id, alumno_id=user.id, usada=False))
    db.commit()
    return {"message": "Carta comprada correctamente"}


@app.post("/api/inventory/{card_id}/use")
def use_card(card_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, str]:
    if user.rol == RolUsuario.ADMIN:
        raise HTTPException(status_code=403, detail="Solo los alumnos pueden usar cartas")
    owned = db.scalar(select(CartaUsuario).where(CartaUsuario.carta_id == card_id, CartaUsuario.alumno_id == user.id, CartaUsuario.usada.is_(False)).limit(1))
    if owned is None:
        raise HTTPException(status_code=400, detail="No tienes esta carta disponible")
    owned.usada = True
    owned.fecha_usada = func.now()
    db.commit()
    return {"message": "Carta usada correctamente"}


@app.get("/api/inventory", response_model=list[CardSummary])
def inventory(user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> list[CardSummary]:
    if user.rol == RolUsuario.ADMIN:
        raise HTTPException(status_code=403, detail="Solo los alumnos tienen inventario")
    rows = db.execute(select(Carta, func.count(CartaUsuario.id)).join(CartaUsuario, CartaUsuario.carta_id == Carta.id).where(CartaUsuario.alumno_id == user.id, CartaUsuario.usada.is_(False)).group_by(Carta.id)).all()
    return [CardSummary.model_validate(card).model_copy(update={"image_url": f"/api/cards/{card.id}/image", "quantity": quantity}) for card, quantity in rows]


@app.post("/api/students/{student_id}/badges")
def create_badge(student_id: int, payload: BadgeCreate, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> dict[str, object]:
    require_admin(user)
    if db.get(Usuario, student_id) is None:
        raise HTTPException(status_code=404, detail="Alumno no encontrado")
    badge = Insignia(nombre=payload.nombre, imagen=decode_image(payload.imagen_base64), alumno_id=student_id)
    db.add(badge)
    db.commit()
    db.refresh(badge)
    return {"id": badge.id, "nombre": badge.nombre, "image_url": f"/api/badges/{badge.id}/image"}


@app.delete("/api/badges/{badge_id}", status_code=204)
def delete_badge(badge_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> Response:
    require_admin(user)
    badge = db.get(Insignia, badge_id)
    if badge is None:
        raise HTTPException(status_code=404, detail="Insignia no encontrada")
    db.delete(badge)
    db.commit()
    return Response(status_code=204)


@app.get("/api/badges/{student_id}")
def badges(student_id: int, user: Usuario = Depends(get_current_user), db: Session = Depends(get_db)) -> list[dict[str, object]]:
    if user.rol != RolUsuario.ADMIN and user.id != student_id:
        raise HTTPException(status_code=403, detail="No tienes permiso para ver estas insignias")
    rows = db.scalars(select(Insignia).where(Insignia.alumno_id == student_id).order_by(Insignia.id.desc()))
    return [{"id": row.id, "nombre": row.nombre, "image_url": f"/api/badges/{row.id}/image"} for row in rows]


@app.get("/api/badges/{badge_id}/image")
def badge_image(badge_id: int, db: Session = Depends(get_db)) -> Response:
    image = db.scalar(select(Insignia.imagen).where(Insignia.id == badge_id))
    if not image:
        raise HTTPException(status_code=404, detail="Imagen no encontrada")
    return Response(content=image, media_type="image/jpeg", headers={"Cache-Control": "public, max-age=86400"})
