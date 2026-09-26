# Autoservice — REST API для автосервиса

Spring Boot приложение для управления заказами автосервиса: создание услуг, оформление заказов,
смена статусов и автоматическая очистка истории заказов по расписанию.

---

## Технологический стек

| Технология | Назначение |
|---|---|
| Java 26 | Язык |
| Spring Boot 4.1.1 | Основной фреймворк |
| Spring Data JPA | Работа с БД через репозитории |
| PostgreSQL | База данных |
| Liquibase | Миграции схемы БД |
| Lombok | Сокращение шаблонного кода |
| Maven | Сборка |

---

## Структура проекта

```
autoservice/
├── src/main/java/com/pashinin/autoservice/
│   ├── AutoserviceApplication.java      # Точка входа, @EnableScheduling
│   ├── controller/
│   │   └── AutoserviceController.java   # REST-эндпоинты (HTTP-слой)
│   ├── service/
│   │   ├── AutoserviceService.java      # Бизнес-логика услуг
│   │   └── OrdersService.java           # Бизнес-логика заказов + шедулер
│   ├── repositories/
│   │   ├── AutoServicesRepository.java  # Доступ к таблице auto_services
│   │   └── OrdersRepository.java        # Доступ к таблице orders
│   ├── entities/
│   │   ├── AutoServices.java            # JPA-сущность услуги
│   │   └── Orders.java                  # JPA-сущность заказа
│   ├── dto/                             # Объекты передачи данных + валидация
│   ├── enums/Status.java                # Статусы заказа
│   └── exceptions/                      # Кастомные исключения + @RestControllerAdvice
└── src/main/resources/
    ├── application.yaml                 # Конфигурация (БД, cron)
    └── db/changelog/                    # Liquibase-миграции
```

Проект построен по классической **трёхслойной архитектуре**:

```
HTTP-запрос
    │
    ▼
Controller  ──принимает запрос, валидирует DTO, возвращает ответ──┐
    │                                                             │
    ▼                                                             │
Service  ──бизнес-логика, транзакции, преобразует DTO → Entity────┤
    │                                                             │
    ▼                                                             │
Repository  ──интерфейс Spring Data JPA, генерирует SQL───────────┘
    │
    ▼
PostgreSQL
```

---

## Как запустить

### 1. Требования

- JDK 26+
- Maven 3.9+ (или используйте `./mvnw` — Maven Wrapper уже в проекте)
- PostgreSQL 14+ запущен на `localhost:5432`

### 2. Подготовка базы данных

```bash
# Создайте базу (имя должно совпадать с application.yaml)
sudo -u postgres psql -c "CREATE DATABASE autoservice;"
```

При первом запуске **Liquibase автоматически** применит миграцию
`db/changelog/scripts/001_ddl_create_table.sql` и создаст таблицы `auto_services` и `orders`.
Вручную ничего создавать не нужно.

### 3. Настройка подключения

Параметры БД в `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/autoservice
    username: postgres
    password: root        # замените на свой пароль
```

### 4. Запуск

```bash
# Сборка и запуск
./mvnw spring-boot:run

# Или: собрать jar и запустить
./mvnw clean package
java -jar target/autoservice-0.0.1-SNAPSHOT.jar
```

Приложение поднимется на порту **8080**.

### 5. Проверка

```bash
curl -X POST http://localhost:8080/api/autoservice   -H "Content-Type: application/json"   -d '{
    "name": "Замена масла",
    "price": 2500,
    "clientName": "Иван Петров",
    "orderDate": "26.09.2026",
    "status": "GAVE"
  }'
```

---

## Как работает взаимодействие репозиториев с БД

### JPA-сущности

Сущности — это Java-классы, отображённые на таблицы. Например, `Orders`:

```java
@Entity
@Table(name = "orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // SERIAL в PostgreSQL
    private Long id;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)   // храним "GAVE", а не порядковый номер 0/1/2
    @Column(name = "status")
    private Status status;
}
```

Hibernate по этим аннотациям знает, какие таблицы и колонки использовать.

### Spring Data репозитории

Репозиторий — это **интерфейс**, реализацию которого Spring создаёт автоматически на старте приложения:

```java
public interface OrdersRepository extends JpaRepository<Orders, Long> {
```

`JpaRepository<Orders, Long>` уже содержит готовые методы: `save()`, `findById()`,
`findAll()`, `deleteById()`, `deleteAllById()` и другие. Вручную SQL писать не нужно —
Spring Data анализирует имя метода или `@Query` и генерирует SQL сам.

#### Пример 1: стандартный метод

```java
ordersRepository.findById(id)
```
→ транслируется в `SELECT * FROM orders WHERE id = ?`

#### Пример 2: метод с JPQL-запросом

В `OrdersRepository` объявлен кастомный запрос:

```java
@Query("""
    SELECT o.id
    FROM Orders o
    WHERE (YEAR(o.orderDate) + :deadlineByYear) <= :currentYear
""")
List<Long> getIdListByOrderDate(@Param("deadlineByYear") Integer deadlineByYear,
                                @Param("currentYear") Integer currentYear);
```

- Строка в `@Query` — это **JPQL** (Java Persistence Query Language): запрос пишется
  по именам **классов и полей** (`Orders o`, `o.orderDate`), а не по именам таблиц и колонок.
  Hibernate в рантайме преобразует его в обычный SQL.
- `:deadlineByYear` и `:currentYear` — именованные параметры, их значения подставляются
  из аргументов метода через `@Param`. Это защита от SQL-инъекций: значения передаются
  как bind-параметры, а не конкатенируются в строку.

Вызов `getIdListByDate(5, 2026)` вернёт ID всех заказов, которым исполнилось 5+ лет —
именно их и удаляет шедулер очистки истории.

### Жизненный цикл одного запроса

Разберём на примере создания заказа:

1. **Controller** принимает JSON, Spring автоматически десериализует его в DTO
   и проверяет аннотации валидации (`@NotBlank`, `@NotNull`, `@Min`).
2. **Controller** вызывает `ordersService.createOrder(dto)`.
3. **Service** (метод помечен `@Transactional`) преобразует DTO в Entity через builder
   и вызывает `ordersRepository.save(order)`.
4. Внутри транзакции **Hibernate** видит новую сущность (нет ID) и выполняет
   `INSERT INTO orders (...) VALUES (...)`. БД через `SERIAL` присваивает id.
5. Транзакция **коммитится** — данные фиксируются в PostgreSQL.
6. **Service** возвращает Entity с заполненным id, **Controller** собирает из неё
   ответный DTO, Spring сериализует его обратно в JSON.

Если на любом шаге бросится `RuntimeException` (например, `NotFoundException`),
транзакция **откатывается** — никакие изменения в БД не сохранятся.

### Миграции Liquibase

Схема БД версионируется через Liquibase. Главный файл `db.changelog.yaml` включает
SQL-скрипты по порядку:

```yaml
databaseChangeLog:
  - include:
      file: scripts/001_ddl_create_table.sql
      relativeToChangelogFile: true
```

При старте Liquibase сравнивает список применённых изменений (таблица
`databasechangelog` в БД) с файлами ченджлога и применяет только новые.
Нумерация `001_...` задаёт порядок; чтобы изменить схему, добавляется `002_...sql`
— уже применённый `001` переписывать нельзя.

---

## Функционал проекта

### 1. Создание услуги и заказа — `POST /api/autoservice`

Один запрос создаёт **обе** сущности: услугу и связанный с ней заказ.

Тело запроса (`AutoServiceOrderDTO`):

```json
{
  "name": "Замена тормозных колодок",
  "price": 8000,
  "clientName": "Мария Сидорова",
  "orderDate": "26.09.2026",
  "status": "GAVE"
}
```

Поток выполнения:

1. Данные валидируются (`name` не пустой, `price >= 0`, `clientName`, `orderDate`,
   `status` — обязательны). При ошибке — `400` со списком проблемных полей.
2. Создаётся услуга (`auto_services`), получает сгенерированный `id`.
3. Создаётся заказ (`orders`), причём в него подставляется **реальный `serviceId`**
   из п. 2, а не значение из запроса.
4. Возвращается объединённый ответ со статусом `201 Created`.

Возможные статусы заказа (enum `Status`):

| Статус | Значение |
|---|---|
| `GAVE` | Автомобиль сдан в работу |
| `IN_PROGRESS` | В работе |
| `DONE` | Готов / выдан |

### 2. Смена статуса заказа — `PATCH /api/autoservice/status/{orderId}?status=DONE`

- Находит заказ по `id`; если его нет — `404` («Заказ с id N не найден»).
- Парсит строку в enum `Status`. Если передать неизвестный статус —
  `400` («Некорректное значение параметра»).
- Сохраняет изменение и возвращает полный DTO заказа с данными услуги.

### 3. Автоочистка истории — `@Scheduled`

Метод `OrdersService.cleanOrderByDate()` запускается по cron из `application.yaml`:

```yaml
spring:
  schedule:
    clean-service-history:
      cron: "0 0 0 ? * Sun"   # каждое воскресенье в 00:00
      deadline-by-year: 5     # хранить историю 5 лет
```

Разбор cron-выражения `0 0 0 ? * Sun`:

| Поле | Значение | Смысл |
|---|---|---|
| секунды | 0 | 0 секунд |
| минуты | 0 | 0 минут |
| часы | 0 | полночь |
| день месяца | ? | не задан (обязателен, когда задан день недели) |
| месяц | * | каждый месяц |
| день недели | Sun | воскресенье |

Логика: JPQL-запрос выбирает ID заказов, у которых `год заказа + 5 <= текущий год`,
и удаляет их пакетно (`deleteAllById`). Количество удалённых записей логируется.

### 4. Обработка ошибок — `GlobalExceptionHandler`

`@RestControllerAdvice` перехватывает исключения из всех контроллеров и возвращает
единый формат ошибки (`ErrorResponse` с полем `message`):

| Исключение | HTTP-код | Когда |
|---|---|---|
| `NotFoundException` | 404 | Сущность не найдена |
| `MethodArgumentNotValidException` | 400 | Ошибки валидации тела запроса |
| `IllegalArgumentException` | 400 | Некорректное значение параметра (например, неизвестный статус) |

### 5. Валидация

DTO аннотированы Bean Validation-аннотациями:

```java
@NotBlank(message = "Имя клиента не может быть пустым")
private String clientName;

@NotNull(message = "Цена не может быть пустой")
@Min(value = 0, message = "Цена не может быть отрицательной")
private Integer price;
```

Аннотация `@Valid` на аргументе контроллера включает проверку; при нарушениях
бросается `MethodArgumentNotValidException`, который обрабатывается глобальным хендлером.

---

## Целостность данных в БД

Миграция `001_ddl_create_table.sql` накладывает ограничения:

```sql
CREATE TABLE auto_services (
    id SERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    price INT NOT NULL CHECK (price >= 0)          -- цена неотрицательная
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    service_id INTEGER NOT NULL REFERENCES auto_services(id) ON DELETE CASCADE,
                                                   -- заказ нельзя создать без существующей услуги;
                                                   -- при удалении услуги удаляются и её заказы
    client_name VARCHAR(20) NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(15) NOT NULL CHECK (status IN ('GAVE', 'IN_PROGRESS', 'DONE'))
                                                   -- только допустимые статусы
);
```

Благодаря `ON DELETE CASCADE` связанная логика удаления реализована на уровне БД,
а шедулер может безопасно удалять старые заказы, не нарушая ссылочную целостность.
