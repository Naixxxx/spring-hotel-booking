# Reservation Service

Содержит `users`, `bookings`, выдачу JWT и сагу бронирования. Порт `8081`.
Swagger: `/swagger-ui/index.html`.

Публичные эндпойнты: `/api/auth/{register,login}`, `/api/bookings`,
`/api/bookings/{id}`, `/api/admin/users` и `/api/admin/users/{id}`.
Отфильтрованная история владельца отдается страницами (по 20 записей;
максимум 100 за страницу).

## Порядок действий

1. `ReservationSteps.start()` коммитит бронь со статусом `PENDING` и уникальным `requestId`.
2. `BookingCoordinator` подбирает комнату и вызывает `CatalogPort.hold()` с `bookingId`.
3. Успех: отдельный локальный `ReservationSteps.confirm()` → `CONFIRMED`.
4. Конфликт: следующий свободный номер для `autoSelect`, иначе `CANCELLED`.
5. Тайм-аут / 5xx: `CANCELLED`, компенсация через `release(compensation=true)`.
   При неудаче освобождения флаг `compensationPending` остаётся `true`; планировщик
   повторяет release каждые 15 секунд. При пользовательской отмене release сохраняет
   счётчик прошлых бронирований.

`application/` не делает HTTP внутри транзакции БД: локальные шаги и
межсервисный оркестратор живут отдельно. `integration/CatalogPort` позволяет
подменять HTTP-клиент в тестах. REST-адаптер имеет тайм-ауты, retry и Circuit Breaker.

Данные H2: `users(id,username,password,role,created_at)` и
`bookings(id,request_id,request_fingerprint,user_id,room_id,start_date,end_date,
status,auto_select,compensation_pending,failure_reason,created_at,version)`.

Тесты: `mvn -pl reservations -am test`.
