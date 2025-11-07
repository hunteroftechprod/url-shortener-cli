![Java](https://img.shields.io/badge/Java-17-blue) ![Build](https://img.shields.io/github/actions/workflow/status/hunteroftechprod/url-shortener-cli/ci.yml?label=build) ![Tests](https://img.shields.io/badge/tests-passing-brightgreen) ![Version](https://img.shields.io/github/v/release/hunteroftechprod/url-shortener-cli?color=blue)

![CI](https://github.com/hunteroftechprod/url-shortener-cli/actions/workflows/maven.yml/badge.svg)
# URL Shortener CLI

CLI-сервис сокращения ссылок с лимитами переходов, TTL и правами владельца (UUID).
Стабильный редирект из консоли, мультипользовательский режим, уведомления.

## Установка и запуск
mvn clean package
java -jar target/url-shortener-cli-1.0.0.jar help

## Команды
init
shorten --url <URL> [--limit N] [--ttl H]
open <shortCode>
list
edit <shortCode> [--limit N] [--ttl H]
delete <shortCode>
help

## Быстрый сценарий проверки
java -jar target/url-shortener-cli-1.0.0.jar init
java -jar target/url-shortener-cli-1.0.0.jar shorten --url https://example.com --limit 1 --ttl 24
java -jar target/url-shortener-cli-1.0.0.jar list

## Конфигурация
src/main/resources/application.properties:
shortener.host=cli.lk
shortener.default-ttl=24
shortener.cleanup-interval=60
shortener.data-path=build

## Тесты
mvn test

## Архитектура
core/ — доменная логика
infra/ — хранение и конфигурация
ui/ — CLI

## Зависимости
Maven: commons-validator, jackson, junit-jupiter


