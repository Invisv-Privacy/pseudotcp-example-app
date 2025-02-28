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

The testing architecture looks like this:
```
┌──────────────────────────────┐           ┌──────────────────────────────────┐
│                              │           │  Docker containers               │
│ Android Emulator (qemu)      │           │   ┌─────────────────────────┐    │
│                              │           │   │ Echo server             │    │
│  ┌───────────────────────┐   │           │   │                         │    │
│  │  PseudotcpExampleApp  │   │           │   │                         │    │
│  │                       │   │           │   │                         │    │
│  └─────────┬─────────────┘   │           │   │   172.25.0.4            │    │
│            │                 │           │   │                         │    │
│  ┌─────────▼─────────────┐   │           │   │                         │    │
│  │  gomobile bindings    │   │           │   │                         │    │
│  │                       │   │           │   └───────────▲─────────────┘    │
│  └─────────┬─────────────┘   │           │               │                  │
│            │                 │           │               │                  │
│  ┌─────────▼─────────────┐   │           │   ┌───────────┼─────────────┐    │
│  │     pseudotcp         │   │           │   │ h2o MASQUE Proxy        │    │
│  │                       │   │           │   │                         │    │
│  └─────────┬─────────────┘   │           │   │                         │    │
│            │                 │           │   │                         │    │
│  ┌─────────▼─────────────┐   │           │   │    172.25.0.3           │    │
│  │    masque client      │   │           │   │                         │    │
│  │                       ┼───┼───────────┼──►│                         │    │
│  └───────────────────────┘   │           │   └─────────────────────────┘    │
└──────────────────────────────┘           └──────────────────────────────────┘
```

The emulator uses qemu and a base android image to create a virtualized android device. We then use the [uiautomator](https://developer.android.com/training/testing/other-components/ui-automator) tool to perform actions on the device, replicating actual user usage.

We use docker to run an [h2o](https://github.com/h2o/h2o) MASQUE proxy and another very simple echo server. The echo server responds to HTTP requests with information about the HTTP request, including the request IP.

Inside the automated test we can then start, check our initial reported IP, enable our sample app service, check our reported IP, and assert that the new reported IP is that of the proxy, prving that packets from the android host device are now passing through the MASQUE proxy as expected.

### Running
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

`stdout` will not be outputted on the command line. In order to get logging and `stdout` from the automated test, you can use the [same command we use for CI](./.github/workflows/end2endtest.yml#92):

```sh
$ adb logcat "System.out:D End2EndTest:D *:S" & LOGCAT_PID=$! ; ./gradlew connectedAndroidTest ; test_ret=$? ; if [ -n "$LOGCAT_PID" ] ; then kill $LOGCAT_PID; fi; exit $test_ret
```
