# Catalog Service

Отвечает за CRUD гостиниц/номеров, выдачу свободных комнат, рекомендации и
журнал выделенных периодов. Порт `8082`; Swagger: `/swagger-ui/index.html`.

Таблицы: `hotels(id,name,address,city)`, `rooms(id,hotel_id,number,available,times_booked,capacity,version)`,
`room_slots(booking_id,room_id,start_date,end_date,state,counted,created_at)`.
Ограничение `(hotel_id,number)` уникально; `booking_id` — PK слотов. Индексы
помогают сортировать номера и искать пересечения дат.

`RoomDirectory` возвращает только `available=true` и без пересечений с активным
интервалом; `SlotLedger` берёт `PESSIMISTIC_WRITE` на запись `Room` до проверки.
Операции `hold` и `release` повторяемы без побочных эффектов. Компенсирующее
освобождение уменьшает `timesBooked`, пользовательская отмена сохраняет
исторический счётчик. Старые слоты сохраняются, поэтому номер с историей
лучше отключить через `PATCH available=false`, а не удалять.

Настройки H2, JWT и внутреннего ключа: `src/main/resources/application.yml`.
Тесты: `mvn -pl catalog -am test`.
