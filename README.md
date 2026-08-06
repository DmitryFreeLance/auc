# «Гараж» — аукцион автомобилей в MAX

Рабочий MVP Mini App и MAX-бота на Java 21 / Spring Boot. При первом запуске бот подтверждает привязанный к MAX телефон, затем присылает кнопку **«Открыть аукцион»**. Mini App проверяет подпись `WebAppData`, связывает ставки с MAX ID и хранит пользователей, лоты, медиа и ставки в SQLite.

## Возможности

- регистрация через бота: имя берётся из профиля MAX, телефон подтверждается подписанной кнопкой `request_contact`;
- живой лот с медиаслайдером фото/видео, таймером и кнопкой ставки;
- realtime-обновление через SSE и уведомление предыдущего лидера в MAX при перебитии ставки;
- последовательная запись ставок через единственное соединение с SQLite;
- ссылка на отчёт «Автотека» для каждого автомобиля;
- роли `USER`, `ADMIN`, `SUPER_ADMIN` по MAX ID;
- админ-статистика, все ставки и участники, изменение шага в процессе торгов;
- создание нескольких лотов, отдельная загрузка фото/видео и последующее редактирование;
- перезапуск завершённого лота с начальными параметрами без потери истории прошлых торгов;
- фото лота, имя и полный телефон лидера/победителя в админ-панели;
- бан и разбан участников с немедленным закрытием доступа к Mini App;
- адаптивный интерфейс для мобильного MAX и веб-версии;
- доступ к админ-панели по ролям `ADMIN` и `SUPER_ADMIN` без дополнительного пароля;
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
docker volume inspect auc-data >/dev/null 2>&1 || docker volume create auc-data
docker run -d --name max-auto-auction --restart unless-stopped \
  -p 127.0.0.1:8083:8080 \
  -e TZ='Asia/Novosibirsk' \
  -e SQLITE_PATH='/app/data/auction.db' \
  -e UPLOAD_DIR='/app/data/uploads' \
  -e PUBLIC_URL='https://auc.profishina.moscow' \
  -e MAX_BOT_TOKEN='replace-me' \
  -e MAX_BOT_USERNAME='replace-me' \
  -e ADMIN_MAX_IDS='' \
  -e SUPER_ADMIN_MAX_IDS='123456789' \
  -e DEMO_AUTH='false' \
  -e COOKIE_SECURE='true' \
  -e COOKIE_SAME_SITE='none' \
  -e COOKIE_PARTITIONED='true' \
  -v auc-data:/app/data max-auto-auction:latest
```

Production должен работать по HTTPS. Установите `DEMO_AUTH=false` и `COOKIE_SECURE=true`. SQLite хранится в Docker volume по пути `/app/data/auction.db`.

## Настройка MAX

1. Создайте бота и Mini App на платформе MAX для партнёров, укажите `PUBLIC_URL` как URL мини-приложения.
2. Удалите активные webhook-подписки бота: при наличии webhook MAX не выдаёт события через Long Polling.
3. Укажите MAX ID администраторов в `ADMIN_MAX_IDS` и супер-администраторов в `SUPER_ADMIN_MAX_IDS` через запятую.
4. Перезапустите контейнер. При первом запуске бота пользователь подтверждает привязанный к MAX номер через кнопку `request_contact`; после проверки подписи бот присылает кнопку `open_app`.

Backend валидирует подписанные MAX параметры по HMAC-SHA256 (`WebAppData`) и не доверяет произвольному ID из URL. `start_param` остаётся доступен как контекст запуска, но права определяются только по проверенному MAX ID.

## API

- `POST /api/auth/max`, `GET /api/auth/me`
- `GET /api/lots/current`, `POST /api/lots/{id}/bids`
- `GET /api/admin/stats`, `GET /api/admin/users`, `PATCH /api/admin/users/{id}/make-admin`
- `PATCH /api/admin/users/{id}/ban`
- `POST /api/admin/lots` (JSON), `PUT /api/admin/lots/{id}`
- `POST /api/admin/lots/{id}/media`, `DELETE /api/admin/lots/{id}/media/{mediaId}`
- `POST /api/admin/lots/{id}/restart`
- `POST /api/admin/broadcast`

Загруженные медиа сохраняются в volume `/app/data/uploads`. Для промышленной эксплуатации их можно вынести в S3-совместимое хранилище; модель URL уже к этому готова.
