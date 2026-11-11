# TwsPopup

Experimental **1.5-dev** adds independent RFCOMM acquisition with `settings put global twspopup_battery_source enco`.
It runs in the Bluetooth host without HeyMelody, using a 20-second capability/subscription session.
Actual Enco Air2i transport and initial notification remain unverified. Follow [the independent-source test](docs/ENCO_TEST.md) with HeyMelody stopped. [Protocol evidence and limitations](docs/ENCO_PROTOCOL.md).

Experimental **1.4-dev** observes separate battery events inside HeyMelody 117.6.6.
Enable the additional `com.heytap.headset` module scope and follow [the device checklist](docs/HEYMELODY_TEST.md).
Select with `settings put global twspopup_battery_source heymelody`; HeyMelody must run and receive reports.
Device compatibility is not yet verified.

## Development / Разработка

Reproducible builds: Java 17 + Android SDK 34 + the pinned Gradle wrapper.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
```

APK: `artifacts/AnyBuds-OriginOS-1.5-dev-debug.apk` with SHA256. GitHub Actions artifacts:
`AnyBuds-OriginOS-<commit SHA>` (APK), `android-reports-<commit SHA>` (reports/logs).
See [build setup](docs/BUILD.md), [technical audit](docs/TECHNICAL_AUDIT.md),
[progress](PROGRESS.md), [battery research](docs/BATTERY_RESEARCH.md), and
[manual device tests](docs/MANUAL_TESTS.md). The optional Android metadata battery reader
is documented below. The current popup uses the legacy vivo renderer. Custom themes remain planned.

### Раздельные данные батареи через Android metadata

В версии `1.1-dev` добавлен необязательный источник раздельных значений L/R/кейс.
Включите его самостоятельно в терминале телефона с необходимыми правами, после настройки
`twspopup_auto` и области `com.android.bluetooth` по инструкции ниже.

```sh
settings put global twspopup_battery_source metadata
```

При следующем подключении модуль читает кэш Android Bluetooth metadata. Отсутствующие
значения отображаются как `--`. В журнале `TwsPopup` записываются источник, время чтения,
общий уровень и маска известных состояний зарядки. Доступность кэша зависит от прошивки
и приложения, которое его заполняет. Время измерения наушниками неизвестно. Обновления
читаются при подключении и существующих событиях общего заряда в течение 20 секунд.
Положение наушников в кейсе этот источник не определяет.

Возврат к прежнему источнику общего заряда выполняется командой

```sh
settings delete global twspopup_battery_source
```

Подробности и ограничения приведены в [исследовании батареи](docs/BATTERY_RESEARCH.md).

Модуль Xposed (Vector / LSPosed) для OriginOS. Показывает системное окно наушников vivo / iQOO
(карточка сопряжения и карточка с зарядом, как у AirPods) для любых наушников и без наушников vivo.

Проверено по коду приложения vivo TWS 9.2.41.2 (`com.android.vivo.tws.vivotws`).

## Установка

1. Собери APK: GitHub → Actions → последний успешный запуск → Artifacts → `AnyBuds-OriginOS-<commit SHA>`.
2. Установи, включи модуль в Vector, область: `com.android.vivo.tws.vivotws`.
3. Перезапусти приложение vivo, чтобы оно подхватило модуль:

```
am force-stop com.android.vivo.tws.vivotws
```

Этот шаг нужен после каждого обновления модуля. Больше ничего «будить» не надо.

## Команды (Termux, под `su`)

Все команды идут в один адрес, меняется только действие после `-a`.

Показать карточку с зарядом (закрывается сама через 5 секунд):

```
am broadcast --include-stopped-packages --receiver-foreground -n com.android.vivo.tws.vivotws/com.vivo.service.dongle.DongleBroadcastReceiver -a io.github.mitchelded.twspopup.SHOW --ei model 32 --ei left 60 --ei right 90 --ei box 40
```

Показать карточку «новые наушники» с кнопкой «Подключить» (по нажатию играет подключение, потом заряд):

```
am broadcast --include-stopped-packages --receiver-foreground -n com.android.vivo.tws.vivotws/com.vivo.service.dongle.DongleBroadcastReceiver -a io.github.mitchelded.twspopup.SHOW --ei model 32 --es state connect
```

Закрыть окно:

```
am broadcast --include-stopped-packages -n com.android.vivo.tws.vivotws/com.vivo.service.dongle.DongleBroadcastReceiver -a io.github.mitchelded.twspopup.DISMISS
```

Список всех моделей и того, какие у них картинки:

```
am broadcast --include-stopped-packages -n com.android.vivo.tws.vivotws/com.vivo.service.dongle.DongleBroadcastReceiver -a io.github.mitchelded.twspopup.LIST
logcat -d -s TwsPopup
```

### Параметры SHOW

Числа можно передавать и как `--ei`, и как `--es`.

| Параметр   | По умолчанию | Значение |
|------------|--------------|----------|
| `model`    | 32           | id модели из списка LIST |
| `state`    | `connected`  | `connected` (заряд), `connect` (кнопка «Подключить»), `connecting`, либо число: 1, 20, 30, 40, 50, 60, 80 |
| `left`, `right`, `box` | 80, 80, 100 | заряд в процентах, `-1` показывает «--» |
| `charging` | 0            | кто заряжается: 1 левый + 2 правый + 4 кейс |
| `incase`   | 3            | кто лежит в кейсе: 1 левый + 2 правый |
| `title`    | имя модели   | заголовок окна, например `--es title "OPPO Enco Air2i"` |
| `mac`      | заглушка     | адрес Bluetooth-устройства |
| `timeout`  | 0            | закрыть через столько секунд (0: как решит само окно) |
| `first`    | false        | `--ez first true` показывает окно как первое подключение |

Модель по умолчанию можно поменять: `settings put global twspopup_model 68`.

### Модели

- С полной анимацией (наушники выезжают из кейса): 0, 1, 16, 17, 28, 29, 32, 33.
- Со своими картинками без анимации: например 68 / 69 (iQOO TWS Air), 84 / 85 (iQOO TWS Air Pro), 96 / 97 (iQOO TWS 1).
- У остальных приложение рисует общие картинки наушников.

## Автоматический показ при подключении наушников (по желанию)

1. В Vector добавь в область модуля `com.android.bluetooth` и перезагрузи телефон.
2. Укажи наушники: часть имени из настроек Bluetooth или MAC-адрес.

```
settings put global twspopup_auto Enco
```

Когда такие наушники подключаются, появляется карточка с зарядом. Android знает один уровень заряда на
оба наушника, поэтому левый и правый показывают одно число, а у кейса стоит «--».

Выключить: `settings delete global twspopup_auto`.

## Если что-то не работает

```
logcat -d -s TwsPopup
```

Модуль пишет каждый шаг: `ready`, `shown`, `updated`, `state -> 60`, `closed` или причину отказа.

Подробный лог самого приложения vivo (до перезагрузки телефона):

```
setprop debug.twspopup.applog 1
am force-stop com.android.vivo.tws.vivotws
logcat -d -s TwsPopup TwsApp
```

## Как это устроено

- `Hook` выбирает, что запускать в каком процессе.
- `PopupController` (главный процесс vivo TWS) принимает команду через манифестный приёмник приложения,
  открывает, обновляет и закрывает окно через `TwsFastPairViewServerManager`, и сам обрабатывает нажатия в окне.
- `DeviceBean`, `ModelCatalog`, `PopupRequest` собирают данные окна из `assets/tws_config.json` приложения.
- `VivoApi` содержит все обращения к внутренним классам приложения.
- `BtWatcher` (процесс Bluetooth) нужен только для автоматического показа.
- `AppLogMirror` нужен только для отладки.
