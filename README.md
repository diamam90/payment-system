# Payment-system

Система для централизованной аутентифицкации пользователей, использующий Keycloak, для предоставления пользователям
следующего функционала:

- Регистрация пользователя
- Логин
- Обновление токена
- Получение информации о текущем пользователе
- Поиск пользователя по идентификатору
- Поиск пользователей по электронной почте
- Обновление пользователя
- Удаление пользователя

---

## Регистрация пользователя

![registration.png](diagram/registration.png)

## Логин

![login.png](diagram/login.png)

## Обновление токена

![refresh-token.png](diagram/refresh-token.png)

## Получение информации о текущем пользователе

![user-info.png](diagram/user-info.png)

## Поиск пользователя по идентификатору

![get-user-by-individual-id.png](diagram/get-user-by-individual-id.png)

## Поиск пользователей по электронной почте

![get-user-by-email.png](diagram/get-user-by-email.png)

## Обновление пользователя

![update-user.png](diagram/update-user.png)

## Удаление пользователя

![delete-user.png](diagram/delete-user.png)

---

## Для запуска проекта необходимо:

### C помощью Makefile:

- Выполнить команду``make all``
- Для просмотра логов выполнить команду ```make infra-logs```
- Для остановки сервисов выполнить команду ```make down```

### Альтернативный вариант:

- Запустить Nexus, выполнив команду ```docker-compose up -d nexus```
- Зайти в Nexus http://localhost:8800 под учеткой администратора (admin,admin) и принять соглашение
  EULA(для более новых версий nexus)
- Выполнить билд person-service командой ```docker-compose build person-service --no-cache```
- Запустить все оставшиеся сервисы ```docker-compose up -d```
- Для просмотра логов выполнить команду ```docker-compose logs -f --tail=200```
- Для остановки сервисов выполнить команду ```docker-compose down -v```

---

## Порты сервисов

| Сервис            | Порт  | Описание                                        |
|-------------------|-------|-------------------------------------------------|
| Nexus             | 8800  | Maven Репозиторий                               |
| Keycloak          | 8080  | Сервис аутентификации                           |
| Keycloak-Postgres | 5433  | БД для Keycloak                                 |
| Alloy             | 12345 | Сбор метрик и логов                             |
| Prometheus        | 9090  | Хранилище метрик                                |
| Loki              | 3100  | Хранилище логов                                 |
| Tempo             | 3200  | Хранилище трассировок                           |
| Grafana           | 3000  | Визуализация метрик трейсов и логов             |
| Person-service    | 8082  | Сервис пользователей                            |
| Person-db         | 5434  | БД для Person-service                           |
| Individuals-api   | 8081  | Сервис взаимодействия с системой Payment System |