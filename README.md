# ![app icon](https://raw.githubusercontent.com/wimvdputten/Pixeldrain_android/master/app/src/main/res/mipmap-hdpi/ic_launcher_round.png) Pixeldrain Android

Pixeldrain is a file sharing website built for speed and ease of use.
Pixeldrain is built by [Fornax](https://twitter.com/Fornax96)
This is an Android application that uses the API of Pixeldrain.
Pixeldrain does not cost any money, though donations are appreciated.
If you want to support the Android app, support Pixeldrain.
Check out [Pixeldrain](https://pixeldrain.com/) for more information

| Download | Reddit |
|-------|-------|
| [![stable release](https://img.shields.io/github/downloads-pre/wimvdputten/Pixeldrain_android/latest/total)](https://github.com/wimvdputten/Pixeldrain_android/releases) | [![reddit](https://img.shields.io/reddit/subreddit-subscribers/pixeldrain?style=flat)](http://reddit.com/r/pixeldrain)  |

**Upload files and view them from within the app**
![screenshots of app](https://raw.githubusercontent.com/wimvdputten/Pixeldrain_android/master/.github/app_screenshot.png)

## Features

Features include:
* Upload files anonymously
* Upload files from external sources like google drive and google photos
* View your uploaded files from within the app
* View your uploaded files by logging in to your pixeldrain account
* Delete your uploaded files
* Search your uploaded files
* Share your uploaded files easily

## Download

Get the app from our [releases page](https://github.com/wimvdputten/Pixeldrain_android/releases).

## Permision
* Internet permision - Used for making api calls, uploading files and retrieving files.

## Dependencies

* [Fuel](https://github.com/kittinunf/fuel) - Used for making api calls
* [Room](https://developer.android.com/jetpack/androidx/releases/room) - Used for a local database to save anonymously uploaded files
* [ExoPlayer](https://github.com/google/ExoPlayer) - Used for playing video and audio files
* [Glide](https://github.com/bumptech/glide) - Used for loading images by url

## Setting Up for Development

This project requires JDK 17 to build properly when using Android Studio. If you have JDK 21 installed, you may encounter compatibility issues with the build tools.

### Building with JDK 17

1. Download and install JDK 17 from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [OpenJDK](https://jdk.java.net/archive/)

2. Either:
   - Run the `use_jdk17.bat` script before building from command line, or
   - Configure Android Studio to use JDK 17 for this project:
     - File > Settings > Build, Execution, Deployment > Build Tools > Gradle
     - Set Gradle JVM to JDK 17

3. Build the project:
   ```
   ./gradlew clean build
   ```

### Troubleshooting

If you encounter "Unsupported class file major version 65" errors, make sure you're using JDK 17 to build the project, not JDK 21.

## Credit

* **Wim van der Putten** - *Creator of Pixeldrain Android*
* **Fornax** - *Creator of Pixeldrain* [Github](https://github.com/Fornax96)

