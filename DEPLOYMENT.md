# Deployment guide

Copy `.env.example` to `.env`, replace every placeholder, then run:

```powershell
docker compose up --build -d
docker compose ps
```

Use a random `JWT_SECRET` of at least 32 bytes and a strong administrator password. Never
commit `.env`. For a public deployment, use managed PostgreSQL, HTTPS, a platform secret
manager, backups, and `/actuator/health` as the health check. Run `mvn verify` before release.

The Docker image is suitable for Render, Railway, Azure, AWS, Google Cloud or a VPS.
Publishing requires the chosen provider account and credentials.
