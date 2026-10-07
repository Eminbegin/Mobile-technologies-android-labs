# Список контактов

## Обязательно должен

- Показывать список имён контактов при наличии разрешения
  - Показывать строчку с количеством контактов "Найдено Х контактов"
- Запрашивать разрешение при отсутствии
  - Показывать, что нет разрешения, а не контактов
- По клику на контакт запускать системную звонилку с введённым номером

## Не должен

- Падать
- Иметь пакет `com.example.…`

## Нежелательно

За пункты отсюда можно получить штраф до 40%; по 5% за каждый. Тем не менее, домашка считается выполненной.

- Строки не в ресурсах
  - `stringResource(…)`
  - `ctx.getString(…)`
- Повторная загрузка списка
- Контейнер, хранящий всех детей сразу
  - `LazyList`
  - `RecyclerView`
- Наползание интерфейса под системный
  - `Scaffold { pvs -> Box(Modifier.padding(pvs)) { … } }`
  - `android:fitsSystemWindows="true"`

## Код для выдёргивания данных

Неэффективно, но очень просто.

`android.content.Context` в `@Composeable` можно достать из `LocalContext.current`.

```kotlin
import android.content.Context
import android.provider.ContactsContract

data class Contact(val name: String?, val phoneNumber: String?, val email: String?)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")
    contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        .use { cursor: Cursor? ->
            if (cursor == null) return emptyList()
            return buildList {
                while (cursor.moveToNext()) {
                    val name =
                        cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val email =
                        cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))

                    add(Contact(name, phoneNumber, email))
                }
            }
        }
}
```

## Автотесты

Отсутствие контактов в английской локали должно выглядеть как строка `No contacts found`.

Для работы тестилки нужно дополнительно оъявить в манифесте разрешение на запись контактов (запрашивать его не нужно).
