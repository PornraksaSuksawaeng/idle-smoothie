<div align="center">

<img src="docs/readme/logo.png" width="420" alt="Idle Smoothie">

**Fruit creatures run a smoothie mine. You run the business.**

![Java 21](https://img.shields.io/badge/Java-21-E8323C?style=flat-square)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-4A5BD9?style=flat-square)
![Gradle 9.8](https://img.shields.io/badge/Gradle-9.8-6CC644?style=flat-square)
![Coverage 98%](https://img.shields.io/badge/coverage-98%25-FF8A1F?style=flat-square)

<img src="docs/readme/loading.png" width="260" alt="Loading screen">
&nbsp;
<img src="docs/readme/screenshot.png" width="260" alt="In-game screenshot">

</div>

## Demo

https://github.com/user-attachments/assets/f9089463-8741-4447-ad47-e72194a86cee

<sub>A run from the first floor: tapping the stations, upgrading, unlocking floors and hiring managers.</sub>

## The game

Pulpies extract smoothie on 15 floors, in 5 fruit zones from strawberry to dragon fruit.
The elevator brings it up, and the Smoothie Bar sells it for coins.
Coins buy upgrades, new floors behind each zone's barrier, and managers that run a station on their own.

## The mine

<details>
<summary>Open the cross-section, from the surface down to dragon fruit</summary>
<br>
<div align="center">
<img src="docs/readme/mine.png" width="320" alt="The mine as a cross-section: the surface, then one room per fruit zone">
</div>
</details>

## Fruit zones

<div align="center">

| Strawberry | Blueberry | Kiwi | Mango | Dragon fruit |
| :---: | :---: | :---: | :---: | :---: |
| <img src="docs/readme/zone_strawberry_idle.gif" width="80" alt="Strawberry Pulpie"> | <img src="docs/readme/zone_blueberry_idle.gif" width="80" alt="Blueberry Pulpie"> | <img src="docs/readme/zone_kiwi_idle.gif" width="80" alt="Kiwi Pulpie"> | <img src="docs/readme/zone_mango_idle.gif" width="80" alt="Mango Pulpie"> | <img src="docs/readme/zone_dragon_fruit_idle.gif" width="80" alt="Dragon fruit Pulpie"> |
| <img src="docs/readme/zone_strawberry_crate.png" width="80" alt="Strawberry crate"> | <img src="docs/readme/zone_blueberry_crate.png" width="80" alt="Blueberry crate"> | <img src="docs/readme/zone_kiwi_crate.png" width="80" alt="Kiwi crate"> | <img src="docs/readme/zone_mango_crate.png" width="80" alt="Mango crate"> | <img src="docs/readme/zone_dragon_fruit_crate.png" width="80" alt="Dragon fruit crate"> |
| <img src="docs/readme/zone_strawberry_soil.png" width="80" alt="Strawberry soil"> | <img src="docs/readme/zone_blueberry_soil.png" width="80" alt="Blueberry soil"> | <img src="docs/readme/zone_kiwi_soil.png" width="80" alt="Kiwi soil"> | <img src="docs/readme/zone_mango_soil.png" width="80" alt="Mango soil"> | <img src="docs/readme/zone_dragon_fruit_soil.png" width="80" alt="Dragon fruit soil"> |

<img src="docs/readme/crate_empty.png" width="80" alt="Empty crate"><br>
<sub>Empty crate</sub>

</div>

## Managers

<div align="center">

| <img src="docs/readme/manager_common.png" width="96" alt="Common manager"> | <img src="docs/readme/manager_rare.png" width="96" alt="Rare manager"> | <img src="docs/readme/manager_epic.png" width="96" alt="Epic manager"> | <img src="docs/readme/manager_legendary.png" width="96" alt="Legendary manager"> | <img src="docs/readme/manager_mythic.png" width="96" alt="Mythic manager"> |
| :---: | :---: | :---: | :---: | :---: |
| Common | Rare | Epic | Legendary | Mythic |

<sub>Pulpies and managers: generated with ChatGPT, then cleaned and packed into sprite sheets by <code>tools/sprites.py</code>.</sub>

</div>

## Stations

<div align="center">

<img src="docs/readme/stations.png" width="576" alt="The headframe, the elevator cabin, the bridge and the Smoothie Bar">

<img src="docs/readme/icons.png" width="116" alt="The coin, speed, settings and smoothie icons">

</div>

## How we built it

- **Design first:** a game design document and UML diagrams before the code.
- **Design patterns:**
  - Facade: `Game`
  - State: `PulpieState`
  - Template Method: `Station`
  - Strategy: `ManagerAbility`
  - Factory: `ManagerFactory`
  - Decorator: `PriceDecorator`
  - Memento: `GameSnapshot` and `SaveManager`
- **Model and view apart:** `model` and `core` never import JavaFX.
- **Tested class by class:** 361 JUnit tests, 98% coverage measured by JaCoCo.
- **Git flow:** one branch per feature, pull requests into `dev`, `dev` into `main`, Conventional Commits.
- **Build order:**
  1. A grey-box prototype of the floor, elevator and bar chain.
  2. The Pulpies.
  3. Upgrades.
  4. Managers.
  5. The save.
  6. The art, last.

## Play it

Install **JDK 21** and **Git**. The Gradle wrapper is included.

```sh
git clone git@github.com:EpitechBachelorPromo2028/B-OOP-301-MPL-3-1-2dgame-4.git
cd B-OOP-301-MPL-3-1-2dgame-4
make run      # or ./gradlew run
make test     # or ./gradlew test
```

On Windows: `gradlew.bat run` and `gradlew.bat test`.

## Team

<a href="https://www.linkedin.com/in/leo-bellard/"><img src="docs/readme/linkedin.svg" width="16" alt="LinkedIn"></a>&nbsp; Leo Bellard<br>
<a href="https://www.linkedin.com/in/mathis-monnin/"><img src="docs/readme/linkedin.svg" width="16" alt="LinkedIn"></a>&nbsp; Mathis Monnin<br>
<a href="https://www.linkedin.com/in/pornraksa-suksawaeng"><img src="docs/readme/linkedin.svg" width="16" alt="LinkedIn"></a>&nbsp; Pornraksa Suksawaeng
