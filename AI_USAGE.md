# AI Usage Notes

## Tools & Models Used
* **AI Collaborator:** Gemini (conversational engineering assistant for architecture planning, Gradle/JDK troubleshooting, Node.js + TypeScript API scaffolding, Jetpack Compose UI generation, and documentation).
* **IDE & Runtime:** Android Studio (Kotlin/Compose, Gradle Groovy DSL, JDK 11) and Node.js v20 LTS with `tsx`.

## Approach & Plan
1. Align Android Gradle, Kotlin (`1.8.10`), Compose (`1.4.3`), and JDK 11 compatibility so builds run cleanly.
2. Stand up an interface-driven Node.js + TypeScript Express backend (`ProductRepository`, `EmailProvider`) proxying Fake Store API on port `3000`.
3. Build a `StateFlow`-driven `CatalogueViewModel` and Jetpack Compose UI covering Home, Categories, Search, Product Details, and Enquiry flows, using `10.0.2.2:3000` and Coil for images.
4. Add unit tests for ranking/filtering logic and document decisions and AI review catches.

## Key Prompts & Moments That Shaped the Solution
1. **Resolving Gradle & Metadata Mismatches:** Aligned `compileOptions` to `JavaVersion.VERSION_11` and excluded duplicate `META-INF` files in `app/build.gradle`.
2. **Enforcing Clean Backend Separation:** Initially prototyped an embedded Ktor server while setting up Node.js on Windows, then migrated to a standalone Node.js + TypeScript backend in `/backend` and removed `MockServer.kt` to satisfy all technical constraints cleanly.
3. **Rapid Compose UI & Coil Integration:** Scaffolded `CatalogueViewModel` and `CatalogueScreen` with `AsyncImage` cards across browse and detail views.

## Where the AI Got Something Wrong & How I Caught It
1. **Java 17 Target Mismatch:** Early Gradle snippet used `JavaVersion.VERSION_17`, causing `invalid source release: 16` under AGP 7.2.0. Caught during deployment and aligned both `build.gradle` and Gradle JDK to Java 11.
2. **URL Encoding for Category Names:** Categories with spaces and apostrophes (`"men's clothing"`, `"women's clothing"`) initially failed until `.encodeURLPathPart()` was added on the client request.
3. **Unit Test Assertion Order:** A sample test expected the 4th-highest rated item instead of the 5th (`Beta Shirt` at 3.8); verified the rank order and corrected the test expectation.

## Time Spent (~2 Hours 45 Minutes Total)
* **Planning & Environment Setup:** ~55 mins (project setup, Git branch, Node.js installation, Gradle/JDK alignment)
* **Building Backend & Compose UI:** ~65 mins (Node.js/TypeScript server, `CatalogueViewModel`, Compose screens, Coil images)
* **Testing & Bug Fixes:** ~25 mins (end-to-end emulator testing, category URL encoding fix, unit tests)
* **Reviewing & Documentation:** ~20 mins (`README.md`, `DECISIONS.md`, `AI_USAGE.md`, and Pull Request preparation)