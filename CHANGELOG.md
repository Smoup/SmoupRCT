## 1.0.5

**Much wider version support: 1.20.1 through 26.1, with no gaps.** The mod now covers 1.20.1,
1.20.2, 1.20.3–1.20.4, 1.20.5–1.20.6, 1.21–1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5, 1.21.6–1.21.8,
1.21.9–1.21.10, 1.21.11 and 26.1.

Item lore and scoreboard reading are now version-aware: on 1.20.5+ they use the component API,
below that the old NBT and scoreboard APIs. Nothing changed in how the mod behaves in game.

Required Fabric Loader version is taken from what each branch's Fabric API actually needs — on
1.20.2, for instance, it is as low as 0.14.22.

---

## 1.0.4

**Game modes are no longer hardcoded.** Previously the list of modes and categories lived in
the mod's code, so every new mode on the server required a mod update. Now the mod learns the
route to a server from your own clicks in `/menu` and walks it on its own — a new mode works
right away, no update needed.

- The current mode is detected from the sidebar line instead of a name baked into the code.
- Categories (Solo, Duo, Trio, Clan and any others) are recognised automatically.
- If the route is unknown, the mod asks you to join manually once and remembers the path.

**Automatic timing tuning.** The mod measures how long the hub transfer, menu opening and
server list loading take on your connection, and adapts its delays. On errors the delays grow
by themselves — no more editing `menuSettleTicks` by hand. Can be turned off with
`autoTuneTimings: false` in the config.

**More reliable navigation:**

- Hub arrival is detected by the world change instead of a fixed delay.
- A menu counts as ready once its contents stop changing — previously the mod could search a
  half-loaded server list and miss the server on the first try.
- A screen flicker between menus no longer discards the progress made so far.

**Supported versions:** 1.20.6, 1.21–1.21.1, 1.21.4, 1.21.6–1.21.8, 1.21.11, 26.1. The required
Fabric Loader version was lowered to the minimum each branch actually needs.

The old mod config is not migrated: on first launch the mod starts learning from scratch.

---

### По-русски

## 1.0.5

**Поддержка версий расширена: от 1.20.1 до 26.1, без пробелов.** Теперь мод собирается под
1.20.1, 1.20.2, 1.20.3–1.20.4, 1.20.5–1.20.6, 1.21–1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5,
1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11 и 26.1.

Чтение лора предметов и строк скорборда стало версионным: на 1.20.5+ через компонентный API,
ниже — через старый NBT и прежний скорборд. В поведении мода в игре ничего не изменилось.

Требуемая версия Fabric Loader берётся из того, что реально нужно Fabric API конкретной
ветки, — например, на 1.20.2 это всего лишь 0.14.22.

---

## 1.0.4

**Режимы больше не зашиты в мод.** Раньше список режимов и разделов лежал в коде, и новый
режим на сервере требовал обновления мода. Теперь мод запоминает дорогу до сервера по вашим
кликам в `/menu` и дальше ходит по ней сам — новый режим работает сразу, обновляться не нужно.

- Текущий режим определяется по строке в сайдбаре, а не по названию из кода.
- Разделы (Соло, Дуо, Трио, Клан и любые другие) распознаются сами.
- Если дорога неизвестна — мод честно попросит зайти вручную один раз и запомнит путь.

**Автоподбор таймингов.** Мод замеряет, сколько на вашем подключении занимает переход в хаб,
открытие меню и прогрузка списка серверов, и подстраивает задержки под себя. При ошибке
задержки увеличиваются автоматически — крутить `menuSettleTicks` руками больше не нужно.
Отключается через `autoTuneTimings: false` в конфиге.

**Надёжность переходов:**

- Прибытие в хаб определяется по смене мира, а не по фиксированной задержке.
- Меню считается готовым, когда его содержимое перестало меняться, — раньше мод мог искать
  сервер в ещё не прогруженном списке и не находить его с первого раза.
- Мигание окна при смене экрана больше не сбрасывает пройденный путь.

**Совместимость:** 1.20.6, 1.21–1.21.1, 1.21.4, 1.21.6–1.21.8, 1.21.11, 26.1. Требования
к версии Fabric Loader снижены до минимально необходимых для каждой ветки.

Старый конфиг мода не переносится: при первом запуске мод начнёт учиться заново.
