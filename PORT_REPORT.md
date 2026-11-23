# Отчёт о порте Northstar -> Create 6.0.8

Дата: 2026-11-23. Исходник: `lightning2401/northstar`, ветка `1.20.1` (0.1cb, собран под Create 0.5.1.f / Flywheel 0.6).
Цель: запуск рядом с Create 6.0.8 (Forge 1.20.1). Старый jar падал: `NoClassDefFoundError ...MovementBehaviour` и `MixinTransformerError` на `ConfigBase$ConfigInt`.

## Результат на сейчас
- Компиляция против Create 6.0.8: **765 ошибок -> 0**, `gradlew build` проходит, jar собирается.
- Create 6 с модом загружается; найденные при запуске падения (стресс, display-источники) исправлены.
- Версия `0.1cb-1.20.1-create6.2`: сборка и 21 серверный GameTest с Immersive Engineering прошли. Клиентский стенд рендера проверялся на create6.1; новую сцену полёта не удалось осмотреть из-за тайм-аута инструмента игрового окна. Полный пользовательский модпак не предоставлен.

## Продолжение порта (Codex, 2026-11-23)

База: `1a29f2d`. Изменения в ветке `fix/create6-runtime`:

- В dev-runtime добавлен MixinExtras Forge 0.4.1. Версия в `mods.toml` теперь подставляется из `mod_version`, `gradlew` сделан исполняемым. Maven-репозитории ограничены группами зависимостей.
- Исправлена сигнатура активного `DimensionSpecialEffectsMixin`: обработчик `@Inject` возвращает `void` и задаёт результат через `CallbackInfoReturnable`. Добавлена проверка отсутствующего клиентского мира.
- `WindmillBearingBlockEntityMixin` применяет множитель планеты к свежему результату Create. Сохранённый `lastGeneratedSpeed` больше не умножается повторно.
- Восемь старых Instance заменены на `NorthstarMachineVisual` (Flywheel 1.0). Общие трансформации `NorthstarMachineModels` используются и обычным рендерером: поршни, головка/лазер гравёра, вентиляторы, температурные детали, рычаг контролов. Есть обновление света, crumbling и удаление экземпляров.
- Удалён ошибочный код рендера коробки передач из шести рендереров машин: он читал несуществующее у этих блоков свойство `AXIS`. Валы теперь выбираются через `IRotate.hasShaftTowards`. Электролизёр сохраняет обычный рендер жидкостей при активном Flywheel.
- Железная шестерня получила Visual с собственной моделью. Контролы в контрапшене используют `useLevelLight`; обычный BlockEntity-рендер контролов отключён в пользу рендера actor, чтобы не дублировать рычаг.
- Исправлен порядок source/flowing-фабрик в вспомогательном `NorthstarRegistrate`. Текущие игровые жидкости регистрируются через стандартный `CreateRegistrate`; они проверяются отдельно.
- Подсказка combustion engine показывает максимум 128 RPM по формуле `getGeneratedSpeed`, вместо догадки 32 RPM. Реальная генерация не изменена.
- Исправлены тег алмазных руд и три таблицы добычи листвы (корневая структура и условие `minecraft:any_of` для 1.20.1). Удалён пустой файл рецепта, убраны комментарии из четырёх JSON биомов. Неиспользуемые `methane_NOT.json` перенесены в `disabled-resources/`: заглавные буквы в путях ресурсов Minecraft недопустимы.
- Добавлены семь серверных GameTest-проверок в отдельном source set: загрузка данных, жидкости, регистрации, legacy-ID ракеты, бак с кислородом, кэш скорости мельницы и стенд из десяти BlockEntity. Опциональный `-PcompatMods` добавляет Immersive Engineering 10.2.0-183 только в runtime. Тестовый сервер использует отдельный каталог `run-gametest/`.

### Ранее проверено в create6.1

- JDK 17, Forge 47.3.0, Create 6.0.8-291, Ponder 1.0.91, Flywheel 1.0.5, JEI 15.20.0.135, GeckoLib 4.4.6.
- `./gradlew build runGameTestServer`: сборка выполнена, **7/7 обязательных тестов прошли** в базовом наборе зависимостей. Проверены фактическая загрузка таблиц листвы и тега алмазной руды, варианты пяти виртуальных жидкостей, регистрации стресса/display-источников, legacy-ID ракеты, заполнение бака кислородом, сохранённая скорость мельницы и создание/тики десяти машин.
- Ранее `./gradlew runGameTestServer -PcompatMods`: **6/6 тестов прошли** с Immersive Engineering 10.2.0-183 (до добавления проверки исправленных данных).
- `./gradlew runClient -PcompatMods -PgameTests`: открыт мир, `/test run machineblockentitiesload` завершился успешно. Вручную проверено отображение десяти блоков при `flywheel:indirect` и `flywheel:off`, без падений рендера. После осмотра клиент остановлен вручную; ненулевой код завершения Gradle в клиентском логе относится к этой остановке.
- Все **1941 JSON-файла исходных ресурсов** синтаксически корректны. В JAR дополнительно проверены JSON/refmap, версия `mods.toml` и отсутствие тестовых классов и отключённых ресурсов.

Окружение использовало Mesa llvmpipe без доступного аудиоустройства. Были нефатальные сообщения проверки обновлений/авторизации и низкая частота кадров; это не проверка производительности на игровом ПК. Dev-refmap отсутствует в каталогах запуска (Mixin сообщает предупреждение); в собранном JAR `northstar.refmap.json` присутствует.

## Ракеты create6.2

- Сохраняются тяга, отсчёт запуска, таймер посадки, параметры конструкции, стоимость полёта, вычислительная скидка, билет и владелец. UUID владельца переживает повторные сохранения без игрока в мире; измерения сохраняются полными ID, включая орбиту.
- При загрузке старой конструкции восстанавливаются двигатели, контролы, масса, теплозащита и запас топлива. Старое хранилище сундука преобразуется из Create FALLBACK в CHEST, иначе Create 6 пропускал возврат предметов при посадке. Обычные SIMPLE-хранилища также преобразуются.
- Расходуется только подходящее топливо, включая несколько баков и смешанные уровни. Проверка запаса выполняется до списания; равный запас разрешён. Скидка не меняет базовую стоимость, присутствие владельца не требуется; творческие баки сохраняют бесконечный запас.
- Устранены двойной вызов тиков actor и обращение к ещё не созданному звуку при первом выходе на высоту посадки. После разборки больше не выполняется дополнительное движение.
- Перенос между измерениями использует Forge ITeleporter и штатный Entity/ServerPlayer.changeDimension. Восстанавливаются сиденья и пилот; локальное управление подключается отдельным пакетом. Неверная цель не уничтожает исходную ракету.
- Ракеты регистрируются через события входа/выхода сущностей. Временные chunk tickets обновляются во время полёта и удаляются при выходе сущности; это позволяет продолжать полёт без игрока в принимающем измерении. Учтена асинхронная регистрация сущностей после перехода.

### Проверка create6.2

`./gradlew build runGameTestServer -PcompatMods` (JDK 17, Forge 47.3.0, Create 6.0.8-291, Immersive Engineering 10.2.0-183): **все 21 обязательный тест прошли**. В облаке использовались `--offline -x downloadAssets`, поскольку зависимости и проверенные ассеты уже были в кэше.

Новые сценарии: миграция структуры и грузов Create 0.5; параметры конструкции после сохранения; тяга и отсчёт после перезагрузки; раздельные баки воды и топлива; атомарное списание, точный запас, смешанные топлива и несколько баков; UUID владельца и орбита после двух сохранений; запуск по отсчёту; мягкая посадка с возвратом сундука и жидкости; перенос пассажира и пилота на Луну с восстановлением сиденья и управления; автоматический переход загруженной ракеты; недоступное измерение.

Установочный JAR проверен как ZIP: корректная версия mods.toml, JSON, northstar.refmap.json; тестовые классы и структуры исключены. Отдельная установка этого JAR в полном пользовательском модпаке не выполнялась.

Для ручной сцены движущихся контролов и двигателя: `./gradlew runClient -PcompatMods -PgameTests -PvisualRocket`, затем `/test run visualrocketflight` в отдельном творческом тестовом мире. Сцена сажает игрока на ракету, запускает короткий подъём и посадку. Новый визуальный осмотр остался непроведённым: инструмент рабочего стола завершился тайм-аутом. Предыдущий стенд десяти машин при Flywheel indirect/off описан выше.

## Что изменено
### Сборка
- `build.gradle` / `gradle.properties`: Create `6.0.8-291`, Ponder `1.0.91` (внутри Catnip), Flywheel `1.0.5` (api compileOnly + runtimeOnly), JEI `15.20.0.135`; репозиторий `https://maven.createmod.net`; `-Xmaxerrs 5000`.
- `mods.toml`: зависимость create `[6.0.0,)`. Текущий `mod_version=0.1cb-1.20.1-create6.2`.

### Переезд классов (массовая замена импортов)
- `foundation.utility.*` -> Catnip: `Lang`->`CreateLang` (`foundation.utility.CreateLang`), `LangBuilder`, `VecHelper`, `Couple`, `Pair`, `Iterate`, `AngleHelper`, `NBTHelper`, `Color`, `IntAttached`, `WorldAttached`, `AnimationTickHolder`, `LerpedFloat(+Chaser)` -> `net.createmod.catnip.*`.
- `SuperByteBuffer`, `SpriteShiftEntry`, `CachedBufferer`->`CachedBuffers`, `WrappedWorld`->`WrappedLevel`, `RegisteredObjects`->`CatnipServices.REGISTRIES`, `ConfigBase`.
- `MovementBehaviour`, `MovingInteractionBehaviour`, `IHaveGoggleInformation`, `ContraptionType` -> `com.simibubi.create.api.*`.
- Flywheel: `com.jozufozu.flywheel.*` -> `dev.engine_room.flywheel.*` (`PartialModel.of(...)`, `TransformStack.of(...)`).
- `Components.literal/translatable` -> `Component.*`.

### Смена API
- **Стресс**: `BlockStressDefaults`/`CStress` работают только для блоков `create:`. Свой класс `block/NorthstarStress` регистрирует через `BlockStressValues.IMPACTS/CAPACITIES`. Генераторы: `BlockStressValues.setGeneratorSpeed(int[, boolean])` (solar: 8; combustion: 128, mayGenerateLess=true, по фактическому коду генераторов).
- **Display-источники** Rocket Station: регистрируются в `Northstar.commonSetup` через `DisplaySource.BY_BLOCK` (в момент регистрации блока реестр Create ещё пуст).
- **Контрапшены**: `NorthstarContraptionTypes` на `DeferredRegister` (`CreateRegistries.CONTRAPTION_TYPE`) + `AllContraptionTypes.BY_LEGACY_NAME` для `northstar:rocket`. Удалён `makeLighter()` (`ContraptionLighter` больше нет).
- **Поворот**: `TransformStack.rotateX/Y/Z` теперь в радианах -> везде заменено на `rotate*Degrees`. `centre/unCentre` -> `center/uncenter`.
- **Рендер**: `SuperByteBuffer.light(Matrix4f,int)` -> `light(int)`; `rotateCentered` заменён на `center().rotateY().uncenter()`; `FluidRenderer.renderFluidBox` -> обёртка `client/NorthstarFluidRenderer` над `FluidRenderHelper`.
- **Registrate**: `NorthstarRegistrate` — новые конструкторы `VirtualFluidBuilder` (две фабрики), регистрация тултипов через `addRegisterCallback` + `TooltipModifier.REGISTRY.register`.
- **JEI**: слоты жидкостей через `addFluidSlot`, `addRichTooltipCallback`.
- `RecipeApplier.applyRecipeOn(..., false)`: в Create 6 последний аргумент — `returnProcessingRemainder`; `false` отключает возврат остатка ингредиента. Полный игровой цикл всех рецептов пока не проверен.

## История отключения Instance в исходной версии порта
В исходном коммите Flywheel 0.6 `*Instance` были удалены из `src/` и оставлены в `disabled-instances/`; их `.instance(...)` убраны из `NorthstarBlockEntityTypes`. Тогда блоки использовали обычные BE-рендереры:
`CombustionEngineInstance`, `ElectrolysisMachineInstance`, `EngraverInstance`, `OxygenConcentratorInstance`, `OxygenGeneratorInstance`, `RocketControlsInstance`, `SolarPanelInstance`, `TemperatureRegulatorInstance`.
Также убраны `.visual(BracketedKineticBlockEntityVisual)` для `BRACKETED_KINETIC` (тип BE не совпал с фабрикой).
Из Renderer'ов удалены проверки `Backend.canUseInstancing(...)`.

## TODO / риски (по приоритету)
1. Проверить установочный JAR в полной пользовательской сборке. Её состав, кроме Immersive Engineering, не предоставлен. Подтверждённые dev-запуски перечислены выше.
2. Активные миксины применились в dev-клиенте/сервере; бак и кэш скорости мельницы проверены тестами. Нужна дальнейшая проверка игровой логики в разных измерениях. `FluidTankBlockEntityMixin` и `LiquidBlockMixin` отсутствуют в `northstar.mixins.json` и не активны.
3. `JetEngineMovementBehaviour`: новый API по умолчанию сохраняет BlockEntity-рендер (`disableBlockEntityRendering() == false`). Статический блок проверен; визуал и частицы в полёте контрапшена ещё не проверены.
4. Ракетные контролы переведены на схему света текущего `ControlsRenderer` Create 6. Статический рычаг проверен; освещение/управление при полёте остаются ручным тестом.
5. Visuals для восьми блоков и обычный рендер проверены на стенде. Нужны длительные тесты рабочих машин, заполненных жидкостями, и анимаций под нагрузкой.
6. Source/flowing варианты виртуальных жидкостей проверены; нужен полный цикл производства и использования жидкостей.
7. MixinExtras в dev-runtime работает; изменения упаковки в отдельный установочный мод не требуются, поскольку полный Create включает эту библиотеку.
8. Старый формат блоков и грузов Create 0.5 проверен NBT-стендом: загрузка, повторное сохранение, запуск, перенос на Луну и посадка. Реальный пользовательский старый мир не предоставлен.

## Как воспроизвести краши
Скидывать `crash-reports/*.txt` + `logs/latest.log`. Известные уже исправленные: `Non-Create blocks cannot be added to Create's config` (стресс), `Registry entry not present: create:station_summary` (display-источники).

## Рабочие заметки
- Gradle-кэш и загрузки автор держит вне репозитория; `gradlew-d.cmd` (локальный враппер с путями `D:\`) в репозиторий не входит.
- Над портом работают Claude Code и Codex; перед правками смотрите этот отчёт и `git log`.
