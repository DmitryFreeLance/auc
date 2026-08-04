# «Гараж» — аукцион автомобилей в MAX

Рабочий MVP Mini App и MAX-бота на Java 21 / Spring Boot. Бот отвечает на запуск единственным сообщением с кнопкой **«Открыть аукцион»**. Mini App проверяет подпись `WebAppData`, связывает ставки с MAX ID и хранит пользователей, лоты, медиа и ставки в SQLite.

## Возможности

- регистрация по имени и телефону после идентификации через MAX;
- живой лот с медиаслайдером фото/видео, таймером и кнопкой ставки;
- realtime-обновление через SSE и уведомление предыдущего лидера в MAX при перебитии ставки;
- атомарные ставки: лот блокируется в БД на время транзакции;
- ссылка на отчёт «Автотека» для каждого автомобиля;
- роли `USER`, `ADMIN`, `SUPER_ADMIN` по MAX ID;
- админ-статистика, все ставки и участники, изменение шага в процессе торгов;
- создание лота с несколькими фото/видео и запуском по таймеру;
- рассылка зарегистрированным участникам через MAX Bot API;
- SQLite в WAL-режиме: отдельный сервер базы данных не требуется.

## Быстрый запуск

```bash
mvn spring-boot:run
```

Откройте `http://localhost:8080`. В demo-режиме автоматически используется супер-администратор `1000001`; режим участника: `http://localhost:8080/?as=user`.

Docker:

```bash
cp .env.example .env
# заполните токен, PUBLIC_URL и реальные MAX ID
docker compose up --build -d
```

Или одним контейнером после `docker build -t max-auto-auction:latest .`:

```bash
docker volume create auc-data
docker run -d --name auc-app --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -e SQLITE_PATH=/app/data/auction.db \
  -e UPLOAD_DIR=/app/data/uploads \
  -e PUBLIC_URL=https://auc.profishina.moscow \
  -e MAX_BOT_TOKEN=replace-me \
  -e MAX_BOT_USERNAME=replace-me \
  -e MAX_WEBHOOK_SECRET=replace-me \
  -e ADMIN_MAX_IDS=123456789 \
  -e SUPER_ADMIN_MAX_IDS=123456789 \
  -e DEMO_AUTH=false -e COOKIE_SECURE=true -e COOKIE_SAME_SITE=none \
  -v auc-data:/app/data max-auto-auction:latest
```

Production должен работать по HTTPS. Установите `DEMO_AUTH=false`, `COOKIE_SECURE=true` и надёжный `MAX_WEBHOOK_SECRET`. SQLite хранится в Docker volume по пути `/app/data/auction.db`.

## Настройка MAX

1. Создайте бота и Mini App на платформе MAX для партнёров, укажите `PUBLIC_URL` как URL мини-приложения.
2. Зарегистрируйте HTTPS webhook `https://your-domain/api/max/webhook` для событий `bot_started` и `message_created`, передав такой же secret, как `MAX_WEBHOOK_SECRET`.
3. Укажите MAX ID администраторов в `ADMIN_MAX_IDS` и супер-администраторов в `SUPER_ADMIN_MAX_IDS` через запятую.
4. Перезапустите контейнер. При событии `bot_started` бот пришлёт кнопку типа `open_app`.

Backend валидирует подписанные MAX параметры по HMAC-SHA256 (`WebAppData`) и не доверяет произвольному ID из URL. `start_param` остаётся доступен как контекст запуска, но права определяются только по проверенному MAX ID.

## API

- `POST /api/auth/max`, `POST /api/auth/register`, `GET /api/auth/me`
- `GET /api/lots/current`, `POST /api/lots/{id}/bids`
- `GET /api/admin/stats`, `PATCH /api/admin/lots/{id}/step`
- `POST /api/admin/lots` (multipart), `POST /api/admin/broadcast`
- `POST /api/max/webhook`

Загруженные медиа сохраняются в volume `/app/data/uploads`. Для промышленной эксплуатации их можно вынести в S3-совместимое хранилище; модель URL уже к этому готова.
