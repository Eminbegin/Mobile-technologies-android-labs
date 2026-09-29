# Калькулятор

Простой калькулятор с максимально простой логикой.

## Что должно быть

- Считает в `double`
- Клавиатура (3х5, например)
- Кнопка сброса ("C")

## Чего не должно быть

- Потери значений при поворотах
- Блокировки поворота
- Падений
- Открытие стандартной клавиатуры
- Пакет или applicationId содержащий `com.example`

## Автотест

Проверялка ожидает, что элементы можно найти.

Для этого в корневом composable:

```kotlin
Scaffold(
    …
    modifier = Modifier
        .semantics {
            testTagsAsResourceId = true
        }
) { … }
```

и на поле с результатами вычисления:

```kotlin
Text(result, Modifier.testTag("result"))
```

Для XML View:

```xml
<TextView
    android:id="@+id/result"
    …
  />
```
