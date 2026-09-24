from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Figuras Ocultas API"
    environment: str = "production"
    secret_key: str = "change-me-in-production"
    access_token_expire_minutes: int = 480
    database_url: str = "mysql+pymysql://root:root@mysql:3306/figuras_ocultas"
    cors_origins: str = "http://localhost:4200,http://localhost:8080"

    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False, extra="ignore")

    @property
    def cors_origin_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
