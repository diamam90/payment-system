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
1. docker compose up -d nexus grafana keycloak
2. Зайти в nexus http://localhost:8800 под учеткой администратора (admin,admin) 
и принять соглашение EULA 
3. gradle :person-service:clean :person-service:build
4. gradle :person-service:publish
5. docker compose up -d individuals-api