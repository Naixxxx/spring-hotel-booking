# Contracts

Модуль-библиотека, который подключают `reservations` и `catalog`. Не запускается отдельно.
Здесь лежат минимальные DTO для межсервисных запросов и единые элементы веб-платформы:
`TokenSupport`, `ApiExceptionHandler`, `CorrelationServletFilter`, `DomainError`.

## Внутренний протокол

- `GET /internal/rooms/recommend?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` — JSON-массив
  `RoomCard`, отсортированный по `timesBooked, id`.
- `POST /internal/rooms/{id}/hold`, JSON: `{"bookingId":"UUID","startDate":"2027-04-12","endDate":"2027-04-15"}`.
- `POST /internal/rooms/{id}/release`, JSON: `{"bookingId":"UUID","compensation":true}`.

Только сервисная сеть. Заголовок `X-Internal-Key` должен соответствовать
`SERVICE_SHARED_KEY`, а `X-Correlation-Id` помогает находить связанные логи.
Повторный hold возвращает `HELD`, повторный release — `RELEASED`. Отпечаток запроса
хранится отдельно в Booking Service, чтобы один `requestId` не мог менять параметры.

Проверка: `mvn -pl contracts -am test` из корня.
