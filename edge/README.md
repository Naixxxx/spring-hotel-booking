# Edge Router

Публичная точка входа на `8080`, маршруты `lb://reservation-service` и
`lb://catalog-service` через Eureka. Проверяет JWT до проксирования,
пропускает запросы регистрации/входа и требует `ROLE_ADMIN` на мутации каталога
и `/api/admin/**`. Backend всё равно проверяет JWT независимо.

Фильтр выпускает/передаёт `X-Correlation-Id` и удаляет потенциально поддельный
`X-Internal-Key` от внешнего клиента. `/internal/**` не маршрутизируется.

Запуск после Registry: `mvn -pl edge spring-boot:run`. Проверка: `mvn -pl edge test`.
