# Individuals-api

Сервис для централизованной аутентифицкации пользователей, использующий Keycloak, для предоставления пользователям 
следующего функционала:

    - Регистрация пользователя
    - Логин
    - Обновление токена
    - Получение информации о текущем пользователе

## Регистрация пользователя
 
![Регистрация пользователя](../diagram/flowpictures/registration.png)

## Логин пользователя

![Логин пользователя](../diagram/flowpictures/login.png)

## Обновление токена

![Логин пользователя](../diagram/flowpictures/refresh-token.png)

## Получение информации о текущем пользователе

![Логин пользователя](../diagram/flowpictures/user-info.png)

Для запуска проекта необходимо:
1. Установить [Docker Desktop](https://docs.docker.com/get-started/introduction/get-docker-desktop/)
2. Установить плагин 
```docker plugin install grafana/loki-docker-driver --alias loki --grant-all-permissions``` 
для возможности отправки логов в Loki 
3. Выполнить команду 
```docker compose up -d```