[![Maven metadata URL](https://img.shields.io/maven-metadata/v?color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)
[![License Apache 2.0](https://img.shields.io/github/license/raamcosta/compose-destinations.svg?style=for-the-badge&color=orange)](https://opensource.org/licenses/Apache-2.0)
[![Android API](https://img.shields.io/badge/api-21%2B-brightgreen.svg?style=for-the-badge)](https://android-arsenal.com/api?level=21)
[![kotlin](https://img.shields.io/github/languages/top/raamcosta/compose-destinations.svg?style=for-the-badge&color=blueviolet)](https://kotlinlang.org/)

<p align="center"> 
   <img height="250" src="https://user-images.githubusercontent.com/80427734/147891822-5cd34c80-8dca-4d34-8278-2aa3bf36913f.png"/> 
</p>

<h1 align="center"> 
   <a href="https://composedestinations.rafaelcosta.xyz">Compose Destinations</a>
</h1>

A KSP library that processes annotations and generates code that uses Official Jetpack Compose Navigation under the hood. It hides the complex, non-type-safe and boilerplate code you would have to write otherwise. </br>
No need to learn a whole new framework to navigate - most APIs are either the same as with the Jetpack Components or inspired by them.


## Main features
- Typesafe navigation arguments
- Simple but configurable navigation graphs setup
- Navigating back with a result in a simple and type-safe way
- Getting the navigation arguments from the `SavedStateHandle` (useful in ViewModels) and `NavBackStackEntry` in a type-safe way.
- Navigation animations
- Destination wrappers to allow reusing Compose logic on multiple screens
- Bottom sheet screens through integration with [Accompanist Navigation-Material](https://github.com/google/accompanist/tree/main/navigation-material)
- Easy deep linking to screens
- Wear OS support (since versions 1.x.30!)
- All you can do with Official Jetpack Compose Navigation but in a simpler safer way!

For a deeper look into all the features, check our [documentation website](https://composedestinations.rafaelcosta.xyz).

## Materials

- Alex Styl's quick introduction videos [_Navigate using the Compose Destinations library_](https://www.composables.co/courses/destination-compose)
- Philipp Lackner's Youtube video [_Compose Navigation Just Got SO MUCH EASIER_ 😱](https://www.youtube.com/watch?v=Q3iZyW2etm4)
- Rafael Costa's blog post [_Compose Destinations: simpler and safer navigation in Compose with no compromises_](https://proandroiddev.com/compose-destinations-simpler-and-safer-navigation-in-compose-with-no-compromises-74a59c6b727d)
- Yanneck Reiß's blog post [_Type Safe Navigation With Jetpack Compose Destinations_](https://medium.com/codex/type-save-navigation-with-jetpack-compose-destinations-610514e85370)
- Google Dev Expert Kenji Abe's blog post [_Navigation Composeを便利にしてくれるライブラリ_](https://star-zero.medium.com/navigation-compose%E3%82%92%E4%BE%BF%E5%88%A9%E3%81%AB%E3%81%97%E3%81%A6%E3%81%8F%E3%82%8C%E3%82%8B%E3%83%A9%E3%82%A4%E3%83%96%E3%83%A9%E3%83%AA-c2d0133b3e84)
- aseem wangoo's blog post (and Youtube video inside): [_Using compose destinations_](https://flatteredwithflutter.com/using-compose-destinations%ef%bf%bc/)
- Vincent Tsen post in Android Kotlin Weekly [_How to convert your Jetpack Compose navigation app to use Compose Destinations Library to get rid of boilerplate code?_](https://vtsen.hashnode.dev/compose-destinations-navigation-library#heading-build-navigation-graph)

## Basic Usage

### 1. Annotate your screen Composables with `@Destination`:

```kotlin
@Destination
@Composable
fun ProfileScreen() { /*...*/ }
```

### 2. Add navigation arguments to the function declaration:

```kotlin
@Destination
@Composable
fun ProfileScreen(
   id: Int, // <-- required navigation argument
   groupName: String?, // <-- optional navigation argument
   isOwnUser: Boolean = false // <-- optional navigation argument
) { /*...*/ }
```

`Parcelable`, `Serializable`, `Enum` and classes annotated with [`@kotlinx.serialization.Serializable`](https://github.com/Kotlin/kotlinx.serialization) (as well as `Array`s and `ArrayList`s of these) work out of the box!
You can also make any other type a navigation argument type. Read about it [here](https://composedestinations.rafaelcosta.xyz/destination-arguments/navigation-arguments#custom-navigation-argument-types)

> There is an alternative way to define the destination arguments in case you don't need to use them
inside the Composable (as is likely the case when using ViewModel). Read more [here](https://composedestinations.rafaelcosta.xyz/destination-arguments/navigation-arguments#navigation-arguments-class-delegate).

### 3. Build the project
   Or run ksp task (example: `./gradlew kspDebugKotlin`), to generate all the Destinations. With the above annotated composable, a `ProfileScreenDestination` file would be generated (that we'll use in step 4).

### 4. Use the generated `[ComposableName]Destination`'s invoke method to navigate to it.
   It will have the correct typed arguments.

```kotlin
@RootNavGraph(start = true) // sets this as the start destination of the default nav graph
@Destination
@Composable
fun HomeScreen(
   navigator: DestinationsNavigator
   // OR navigator: NavController if you prefer, in which case there's a navigate extension function
) {
   /*...*/
   navigator.navigate(ProfileScreenDestination(id = 7, groupName = "Kotlin programmers"))
}
```
> DestinationsNavigator is a wrapper interface to NavController that if declared as a parameter, will be provided for free by the library. NavController can also be provided in the exact same way, but it ties your composables to a specific implementation which will make it harder to test and preview. Read more [here](https://composedestinations.rafaelcosta.xyz/navigation/basics#destinationsnavigator-vs-navcontroller)

### 5. Finally, add the NavHost call:

```kotlin
DestinationsNavHost(navGraph = NavGraphs.root)
```
> `NavGraphs` is a generated file that describes your navigation graphs and their destinations. By default all destinations will belong to "root" (@RootNavGraph), but you can create your own nav graphs annotations to have certain screens in other navigation graphs.

This call adds all annotated Composable functions as destinations of the Navigation Host.

That's it! No need to worry about routes, `NavType`, bundles and strings. All that redundant and
error-prone code gets generated for you.

## Setup

Compose destinations is available via maven central.

#### 1. Add the KSP plugin:

> **Note**: The version you chose for the KSP plugin depends on the Kotlin version your project uses. </br>
You can check https://github.com/google/ksp/releases for the list of KSP versions, then pick the last release that matches your Kotlin version.
Example:
If you're using `1.8.20` Kotlin version, then the last KSP version is `1.8.20-1.0.10`.

<details open>
  <summary>groovy - build.gradle(:module-name)</summary>

```gradle
plugins {
    //...
    id 'com.google.devtools.ksp' version '1.8.20-1.0.10' // Depends on your kotlin version
}
```
</details>

<details>
  <summary>kotlin - build.gradle.kts(:module-name)</summary>  

```gradle
plugins {
    //...
    id("com.google.devtools.ksp") version "1.8.20-1.0.10" // Depends on your kotlin version
}
```
</details>

#### 2. Add the dependencies:

Compose Destinations has multiple active versions. 
The higher one uses the latest versions for Compose and Accompanist, while the others use only stable versions.
Choose the one that matches your Compose version, considering this table:

<table>
 <tr>
  <td>Compose 1.1 (1.1.x)</td><td><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?versionPrefix=1.5&color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)"></td>
 </tr>
 <tr>
  <td>Compose 1.2 (1.2.x)</td><td><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?versionPrefix=1.6&color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)"></td>
 </tr>
 <tr>
  <td>Compose 1.3 (1.3.x)</td><td><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?versionPrefix=1.7&color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)"></td>
 </tr>
 <tr>
  <td>Compose 1.4 (1.4.x)</td><td><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?versionPrefix=1.8&color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)"></td>
 </tr>
 <tr>
  <td>Compose 1.5 (1.5.x)</td><td><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?versionPrefix=1.9&color=blue&metadataUrl=https://s01.oss.sonatype.org/service/local/repo_groups/public/content/io/github/raamcosta/compose-destinations/core/maven-metadata.xml&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/io.github.raamcosta.compose-destinations/core)"></td>
 </tr>
</table>

> **Warning**: If you choose a version that uses a higher version of Compose than the one you're setting for your app, gradle will upgrade your Compose version via transitive dependency.

<details open>
  <summary>groovy - build.gradle(:module-name)</summary>

```gradle
ksp 'io.github.raamcosta.compose-destinations:ksp:<version>'
implementation 'io.github.raamcosta.compose-destinations:core:<version>'

// if you want to use bottom sheet destinations:
implementation 'io.github.raamcosta.compose-destinations:bottom-sheet:<version>'
```
</details>

<details>
  <summary>kotlin - build.gradle.kts(:module-name)</summary>  

```gradle
ksp("io.github.raamcosta.compose-destinations:ksp:<version>")
implementation("io.github.raamcosta.compose-destinations:core:<version>")

// if you want to use bottom sheet destinations:
implementation("io.github.raamcosta.compose-destinations:bottom-sheet:<version>")
```
</details>

> **Note**: `bottom-sheet` dependency uses [Accompanist Navigation-Material](https://github.com/google/accompanist/tree/main/navigation-material) internally. </br>
> Read more about the next steps to configure these features [here](https://composedestinations.rafaelcosta.xyz/styles-and-animations)

> **Note**: If you want to use Compose Destinations in a **Wear OS** app, replace above core dependency with: </br>
`implementation 'io.github.raamcosta.compose-destinations:wear-core:<version>'` </br>
> this will use [Wear Compose Navigation](https://developer.android.com/training/wearables/compose/navigation) internally. </br>
> Read more about the next steps to configure these features [here](https://composedestinations.rafaelcosta.xyz/wear-os)

**If you like the library, consider starring and sharing it with your colleagues.**
