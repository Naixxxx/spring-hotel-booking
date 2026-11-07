# Registry (Eureka)

Обеспечивает discovery по именам `catalog-service` и `reservation-service`;
`edge-router` тоже регистрируется. Запуск `mvn -pl registry spring-boot:run`.
Порт `8761`, панель: http://localhost:8761.

Eureka не хранит данные бронирований. Другим модулям нужно задать
`EUREKA_URL=http://localhost:8761/eureka/` (при Docker — `http://registry:8761/eureka/`).
