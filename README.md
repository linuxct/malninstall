# malninstall (a.k.a. FluBot Malware Remover) [![Latest Version](https://img.shields.io/github/v/release/linuxct/malninstall)](https://github.com/linuxct/malninstall/releases/latest) [![Compatibility](https://img.shields.io/badge/compat-API%2023%2B-brightgreen)]

A tool to remove FluBot from any Android 6.0+ device.

<img src="https://raw.githubusercontent.com/linuxct/malninstall/main/app/src/main/ic_launcher-playstore.png" alt="icon" width="150"/>

## Usage

This tool relies on setting itself as your default application launcher in a temporary way. It was discovered that whenever FluBot detects that the user is attempting to uninstall the malware, it will always go to the home page, thus having a controlled application as your launcher allows the user to tap on the OK button of the uninstall prompt without any issues. This has been tested on several devices, including Sony Xperia XZ Premium, OnePlus 8, and more. Follow the steps below in order to use it.

1. Please, download the latest version from [here](https://github.com/linuxct/malninstall/releases/latest).
2. Install the application on the infected device.
3. (Optional, but recommended) Disconnect the Wi-Fi and Mobile data connection on the device.
4. Follow the in-screen steps to set up the tool as the default launcher. Depending on the device manufacturer, this step may have to be done manually by accessing the system settings.
5. Uninstall the malware. If prompted to choose the default launcher again, you must tap on "Always". Then, you will be able to hit "OK" on the uninstall prompt.
6. Undo the default launcher choice by following the in-screen steps. 
7. You may now uninstall the tool.

## Build

You can use Android Studio to build the application, or you can build it by using the CLI.  

Navigate to the folder where the source code is located:  
```cd /path/where/you/downloaded/malninstall/```  

Then, check that Gradle runs properly by executing:  
For Linux/MacOS: `./gradlew tasks`  
For Windows: `gradlew tasks`  

You can now build the application in release or debug flavor:   
`./gradlew assemble`  

You will now need to sign the resulting APK by using apksigner, or jarsigner. Here's an example:  
apksigner sign --ks /path/to/example.keystore --ks-pass pass:"EXAMPLEPASSWORD" --v1-signing-enabled true --v2-signing-enabled true *.apk  

