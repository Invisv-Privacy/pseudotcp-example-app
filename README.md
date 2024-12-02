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
