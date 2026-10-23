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

## The game

Pulpies extract smoothie on 15 floors, in 5 fruit zones from strawberry to dragon fruit.
The elevator brings it up, and the Smoothie Bar sells it for coins.
Coins buy upgrades, new floors behind each zone's barrier, and managers that run a station on their own.

## Assets

<div align="center">

<img src="docs/readme/pulpies.png" width="520" alt="The five Pulpies">

<img src="docs/readme/managers.png" width="520" alt="The five managers, Common to Mythic">

<img src="docs/readme/walk.gif" width="120" alt="A Pulpie walking">

<sub>Generated with ChatGPT, then cleaned and packed into sprite sheets by <code>tools/sprites.py</code>.</sub>

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
- **Tested class by class:** 358 JUnit tests, 98% coverage measured by JaCoCo.
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

<div align="center">

| Leo Bellard | Mathis Monnin | Pornraksa Suksawaeng |
| :---: | :---: | :---: |
| [LinkedIn](https://www.linkedin.com/in/leo-bellard/) | [LinkedIn](https://www.linkedin.com/in/mathis-monnin/) | [LinkedIn](https://www.linkedin.com/in/pornraksa-suksawaeng) |

</div>
