# Empatica plugin for RADAR-pRMT

Application to be run on an Android 5.0 (or later) device with Bluetooth Low Energy (Bluetooth 4.0 or later), to interact with the Empatica.

The plugin application only runs on an ARM architecture and because of its Bluetooth Low Energy requirement, it also requires coarse location permissions. This plugin does not collect location information.

## Installation

First, request an Empatica Connect developer account from [Empatica's Developer Area][1]. Download the Empatica Android SDK there.

- Download the Empatica Android SDK from the [Empatica Developer Area][1]. Copy the `empalink-2.2.aar` from the Empatica Android SDK package to the `libs` directory of your application. Then the project can be edited with Android Studio. Add the following to your `build.gradle`:
        
     ```gradle
     repositories {
         flatDir { dirs 'libs' }
         maven { url  'http://dl.bintray.com/radar-cns/org.radarcns' }
     }
     
     dependencies {
         compile 'org.radarcns:radar-android-empatica:0.2.0'
     }
     ```
    


[1]: https://www.empatica.com/connect/developer.php

### Configuration

Request an Empatica API key for your Empatica Connect account. Set your Empatica API key in the `empatica_api_key` Firebase parameter. The plugin can now be used with devices linked to your account.

## Contributing

To build this repository, download the Empatica Android SDK from the [Empatica Developer Area][1]. Copy the `empalink-2.2.aar` from the Empatica Android SDK package to the `libs` directory.

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html), except using 4 spaces as indentation. Make a pull request once the code is working.

[1](https://www.empatica.com/connect/developer.php)
