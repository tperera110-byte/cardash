# CarDash - Car Launcher for Android head units (Nissan Magnite ready)

One landscape screen: animated engine start-up, speedometer, GPS map/heading, reverse camera,
music player, auto day/night theme with car lights, clock, weather and climate panel.
Settings (gear icon, or tap the car name/plate): car model (Nissan Magnite / Sedan / Hatchback),
car name, number plate, car colour, units, clock, camera source.

## Get the APK (no coding, about 5 minutes)
1. Create a free account at github.com and make a new empty repository.
2. Upload ALL files in this folder (drag and drop in the browser, keep the folder structure,
   including the hidden `.github` folder).
3. Open the **Actions** tab -> **Build CarDash APK** -> wait for the green tick (about 4 min).
4. Open the finished run -> **Artifacts** -> download **CarDash-apk** -> unzip -> `app-debug.apk`.
5. Copy the APK to the head unit (USB / file manager) and install. Allow "unknown sources".
6. Press the Home button and choose **CarDash** -> **Always** to use it as your launcher.

## Notes
- Reverse camera: the app shows a camera feed with parking guide lines. Many head units wire the
  reverse camera through their own camera app. In Settings enter that app's package name, then use
  "Open camera app", or pick the camera source if the unit exposes it as a normal camera.
- Climate buttons control this screen only; real A/C control needs the car's CAN integration.
- GPS needs the head unit's GPS receiver or a phone/USB GPS. Test without GPS: Settings -> Demo drive.
- Music plays songs stored on the device (Music folder). Tap Library -> Rescan after copying songs.
