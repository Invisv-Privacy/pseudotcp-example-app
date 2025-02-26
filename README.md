# Pseudotcp Example Apps

This is an example android app which uses [Android's VPN APIs](https://developer.android.com/develop/connectivity/vpn) and gomobile bindings in order to demonstrate the integration with and utility of Invisv's pseudotcp library.


## Build
The go code is built with [gomobile](https://pkg.go.dev/golang.org/x/mobile/cmd/gomobile). You can use the [build.sh](./app/src/go/build.sh) script for convenience:
```
$ cd app/src/go
app/src/go $ build.sh
Running gomobile bind...
Moved .aar to Android libs. REMEMBER to sync project with gradle files in Android!
```
At that point, like it says, use the "sync project with gradle files" command in android studio in order for it to reload the newly built .aar library.

## Test
This repo includes a full end2end test for exercising the entire network stack, from android VPN service code -> gomobile bindings -> pseudotcp -> masque client.

In order to run the test you must first start the dockerized h2o server and the echo server:

```sh
$ docker-compose up -d
```

The [docker-compose file](./docker-compose.yml) includes a custom network with a subnet of `172.25.0.0/24` assuming that range is unlikely to be used elsewhere. If that clashes with your network environment, you'll need to update the addresses in the docker-compose file as well as in the [end2end test](./app/src/androidTest/java/com/invisv/pseudotcpexampleapp/End2EndTest.java).

After starting the docker services, you can then run the test. You'll need an actual device to run it on, whether that's qemu emulated or an actual physical device. [Android studio makes creating qemu emulated devices quite simple](https://developer.android.com/studio/run/managing-avds).

Once you have an android device running, you can run the test through the Android Studio IDE or from the command line:

```sh
$ ./gradlew connectedAndroidTest
Starting a Gradle Daemon (subsequent builds will be faster)

> Configure project :app

> Task :app:connectedDebugAndroidTest
Starting 1 tests on Pixel_5_API_33(AVD) - 13

Pixel_5_API_33(AVD) - 13 Tests 0/1 completed. (0 skipped) (0 failed)
Finished 1 tests on Pixel_5_API_33(AVD) - 13

BUILD SUCCESSFUL in 37s
61 actionable tasks: 11 executed, 50 up-to-date
```

An html report will be then placed in `app/build/reports/androidTests/connected/debug/com.invisv.pseudotcpexampleapp.End2EndTest.html`


