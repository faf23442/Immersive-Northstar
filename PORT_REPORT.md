# Отчёт о порте Northstar -> Create 6.0.8

Дата: 2026-11-23. Исходник: `lightning2401/northstar`, ветка `1.20.1` (0.1cb, собран под Create 0.5.1.f / Flywheel 0.6).
Цель: запуск рядом с Create 6.0.8 (Forge 1.20.1). Старый jar падал: `NoClassDefFoundError ...MovementBehaviour` и `MixinTransformerError` на `ConfigBase$ConfigInt`.

## Результат на сейчас
- Компиляция против Create 6.0.8: **765 ошибок -> 0**, `gradlew build` проходит, jar собирается.
- Create 6 с модом загружается; найденные при запуске падения (стресс, display-источники) исправлены.
- **В игре ещё НЕ проверено**: последний собранный jar не подтверждён запуском. Миксины и рантайм-логика не тестировались.

## Что изменено
### Сборка
- `build.gradle` / `gradle.properties`: Create `6.0.8-291`, Ponder `1.0.91` (внутри Catnip), Flywheel `1.0.5` (api compileOnly + runtimeOnly), JEI `15.20.0.135`; репозиторий `https://maven.createmod.net`; `-Xmaxerrs 5000`.
- `mods.toml`: зависимость create `[6.0.0,)`. `mod_version=0.1cb-1.20.1-create6`.

### Переезд классов (массовая замена импортов)
- `foundation.utility.*` -> Catnip: `Lang`->`CreateLang` (`foundation.utility.CreateLang`), `LangBuilder`, `VecHelper`, `Couple`, `Pair`, `Iterate`, `AngleHelper`, `NBTHelper`, `Color`, `IntAttached`, `WorldAttached`, `AnimationTickHolder`, `LerpedFloat(+Chaser)` -> `net.createmod.catnip.*`.
- `SuperByteBuffer`, `SpriteShiftEntry`, `CachedBufferer`->`CachedBuffers`, `WrappedWorld`->`WrappedLevel`, `RegisteredObjects`->`CatnipServices.REGISTRIES`, `ConfigBase`.
- `MovementBehaviour`, `MovingInteractionBehaviour`, `IHaveGoggleInformation`, `ContraptionType` -> `com.simibubi.create.api.*`.
- Flywheel: `com.jozufozu.flywheel.*` -> `dev.engine_room.flywheel.*` (`PartialModel.of(...)`, `TransformStack.of(...)`).
- `Components.literal/translatable` -> `Component.*`.

### Смена API
- **Стресс**: `BlockStressDefaults`/`CStress` работают только для блоков `create:`. Свой класс `block/NorthstarStress` регистрирует через `BlockStressValues.IMPACTS/CAPACITIES`. Генераторы: `BlockStressValues.setGeneratorSpeed(int[, boolean])` (solar: 8; combustion: 32, mayGenerateLess=true — *значения выбраны по догадке из старых Couple(16,32)*).
- **Display-источники** Rocket Station: регистрируются в `Northstar.commonSetup` через `DisplaySource.BY_BLOCK` (в момент регистрации блока реестр Create ещё пуст).
- **Контрапшены**: `NorthstarContraptionTypes` на `DeferredRegister` (`CreateRegistries.CONTRAPTION_TYPE`) + `AllContraptionTypes.BY_LEGACY_NAME` для `northstar:rocket`. Удалён `makeLighter()` (`ContraptionLighter` больше нет).
- **Поворот**: `TransformStack.rotateX/Y/Z` теперь в радианах -> везде заменено на `rotate*Degrees`. `centre/unCentre` -> `center/uncenter`.
- **Рендер**: `SuperByteBuffer.light(Matrix4f,int)` -> `light(int)`; `rotateCentered` заменён на `center().rotateY().uncenter()`; `FluidRenderer.renderFluidBox` -> обёртка `client/NorthstarFluidRenderer` над `FluidRenderHelper`.
- **Registrate**: `NorthstarRegistrate` — новые конструкторы `VirtualFluidBuilder` (две фабрики), регистрация тултипов через `addRegisterCallback` + `TooltipModifier.REGISTRY.register`.
- **JEI**: слоты жидкостей через `addFluidSlot`, `addRichTooltipCallback`.
- `RecipeApplier.applyRecipeOn(..., false)` — последний аргумент выбран по догадке.

## ОТКЛЮЧЕНО (нужно портировать)
Flywheel 0.6 `*Instance` (instancing) удалены из `src/` и лежат в `disabled-instances/`; их `.instance(...)` убраны из `NorthstarBlockEntityTypes`. Блоки рисуются обычными BE-рендерами:
`CombustionEngineInstance`, `ElectrolysisMachineInstance`, `EngraverInstance`, `OxygenConcentratorInstance`, `OxygenGeneratorInstance`, `RocketControlsInstance`, `SolarPanelInstance`, `TemperatureRegulatorInstance`.
Также убраны `.visual(BracketedKineticBlockEntityVisual)` для `BRACKETED_KINETIC` (тип BE не совпал с фабрикой).
Из Renderer'ов удалены проверки `Backend.canUseInstancing(...)`.

## TODO / риски (по приоритету)
1. Подтвердить запуск клиента и сервера с Create 6.0.8 (+ Immersive Engineering и т.д. из сборки).
2. Проверить миксины: `WindmillBearingBlockEntityMixin` (`getAngleSpeedDirection2`, `AllConfigs.server().kinetics...`), `FluidTankBlockMixin`, `FluidTankBlockEntityMixin`, `ContraptionTestMixin`, `LiquidBlockMixin` и др. — в dev-окружении не прогонялись.
3. `JetEngineMovementBehaviour`: `renderAsNormalBlockEntity()` удалён (убран override) — проверить визуал в контрапшене.
4. Ракетные контролы: освещение `LevelRenderer.getLightColor(renderWorld, context.localPos)` вместо `getContraptionWorldLight` — проверить.
5. Порт Instance -> Flywheel 1.0 Visuals (`SimpleBlockEntityVisualizer`, `AbstractBlockEntityVisual`, `SimpleDynamicVisual`) для 8 блоков.
6. Проверить виртуальные жидкости (порядок аргументов flowing/source фабрик).
7. Dev-запуск (`runServer`) требует MixinExtras в зависимостях (Create slim не включает его в jarJar) — добавить `io.github.llamalad7:mixinextras-forge`.
8. Тесты старых миров (legacy name `northstar:rocket`).

## Как воспроизвести краши
Скидывать `crash-reports/*.txt` + `logs/latest.log`. Известные уже исправленные: `Non-Create blocks cannot be added to Create's config` (стресс), `Registry entry not present: create:station_summary` (display-источники).

## Рабочие заметки
- Gradle-кэш и загрузки автор держит вне репозитория; `gradlew-d.cmd` (локальный враппер с путями `D:\`) в репозиторий не входит.
- Над портом работают Claude Code и Codex; перед правками смотрите этот отчёт и `git log`.
