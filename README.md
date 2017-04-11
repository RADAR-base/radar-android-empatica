# Empatica plugin for RADAR-pRMT

Application to be run on an Android 4.4 (or later) device with Bluetooth Low Energy (Bluetooth 4.0 or later), to interact with the Empatica.

The plugin application only runs on an ARM architecture and because of its Bluetooth Low Energy requirement, it also requires coarse location permissions. This plugin does not collect location information.

## Installation

First, request an Empatica API key for your Empatica Connect account from their [Developer Area][1]. Also download the Empatica Android SDK there.

In your application, edit the `app/src/main/res/xml/remote_config_defaults.xml` file (see the template `src/main/res/xml/remote_config_defaults_template.xml` in RADAR-Common-Android):

- Download the Empatica Android SDK from the [Empatica Developer Area][1]. Copy the `empalink-2.1.aar` from the Empatica Android SDK package to the `libs` directory of your application. Then the project can be edited with Android Studio. Add the following to your `build.gradle`:
        
     ```gradle
     repositories {
         flatDir { dirs 'libs' }
         maven { url  'http://dl.bintray.com/radar-cns/org.radarcns' }
     }
     
     dependencies {
         compile 'org.radarcns:radar-android-empatica:0.1-alpha.2'
     }
     ```
    

- Set your Empatica API key in the `empatica_api_key` xml element.
- Set the `kafka_rest_proxy_url` and the `schema_registry_url`. If the app should not upload any data, leave them blank.
- Set the `device_group_id` string to a suitable user ID.

[1]: https://www.empatica.com/connect/developer.php

## Contributing

To build this repository, download the Empatica Android SDK from the [Empatica Developer Area][1]. Copy the `empalink-2.1.aar` from the Empatica Android SDK package to the `libs` directory.

Code should be formatted using the [Google Java Code Style Guide](https://google.github.io/styleguide/javaguide.html), except using 4 spaces as indentation. Make a pull request once the code is working.
