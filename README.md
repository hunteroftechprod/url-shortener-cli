# URL Shortener CLI

Инструмент для сокращения ссылок с лимитом переходов, временем жизни (TTL), UUID‑идентификацией владельца и дружелюбным CLI.

## Быстрый старт
```
mvn -q -DskipTests=false clean package
java -jar target/url-shortener-cli-1.0.0-shaded.jar help
```

## Примеры
```
# 1) Инициализация пользователя и получение UUID
java -jar target/url-shortener-cli-1.0.0-shaded.jar init

# 2) Создание ссылки (лимит 5 кликов, TTL = 24 часа)
java -jar target/url-shortener-cli-1.0.0-shaded.jar shorten --url https://example.com --limit 5 --ttl 24

# 3) Переход (учитывает TTL и лимит; открывается браузер)
java -jar target/url-shortener-cli-1.0.0-shaded.jar open abC1234

# 4) Список ваших ссылок
java -jar target/url-shortener-cli-1.0.0-shaded.jar list

# 5) Редактирование параметров (только владелец)
java -jar target/url-shortener-cli-1.0.0-shaded.jar edit abC1234 --limit 10 --ttl 48

# 6) Удаление (только владелец)
java -jar target/url-shortener-cli-1.0.0-shaded.jar delete abC1234
```

## Конфигурация
Все параметры в `src/main/resources/application.properties`:
- `default.ttl.hours` — TTL по умолчанию для новых ссылок
- `short.host` — отображаемый «хост» для коротких кодов (косметика в CLI)
- `data.dir` — каталог хранения данных (`links.json`, `users.json`)
- `cleanup.interval.seconds` — период фоновой очистки «протухших» ссылок

## Архитектура
- `core` — доменные модели и сервис `ShortenerService` (логика TTL, лимиты, счётчик, контроль владельца)
- `infra` — файловые репозитории JSON, консольный нотификатор
- `ui` — `Main` и парсер CLI аргументов

## Тестирование
Запуск: `mvn -q test`. Покрыты сценарии: лимиты переходов, истечение TTL и блокировка.

## Автоматизация
GitHub Actions (`.github/workflows/ci.yml`): сборка, тесты. В репозитории есть `.editorconfig`.

## Соответствие ТЗ
- Уникальные короткие коды с проверкой коллизий, разные пользователи получают разные коды на один и тот же URL.
- Переход по ссылке через `Desktop.getDesktop().browse(...)`.
- Лимит переходов и TTL с автоочисткой, уведомления в консоль с префиксом `[notify <UUID>]`.
- Право редактирования/удаления — только у владельца (UUID сохраняется локально).
