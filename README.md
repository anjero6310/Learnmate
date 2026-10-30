# Learnmate

An accessible learning app for blind and deaf learners, built with Kotlin and Jetpack Compose.
Subjects included: **English** and **Filipino**. More can be added without touching any code.

## Open and run

1. Install Android Studio (Koala 2024.1 or newer).
2. **File > Open** and choose this `LearnTogether` folder.
3. Let Gradle sync (Android Studio will download Gradle and the Android SDK 34 if needed).
4. Run on an emulator or a phone (Android 8.0 / API 26 or newer).

## Screens

| Screen | What it does |
|---|---|
| Welcome | Intro and "Get Started" (shown on first launch only) |
| Home | One colored card per subject, plus a progress summary |
| Lessons | Every lesson from every subject, with **All / Blind / Deaf** filters |
| Subject | The lessons of one subject |
| Lesson | Large-text cards with Previous / Next, **Play Audio**, and "Mark Lesson Complete" |
| Progress | Lessons done, badges (1 per 3 lessons), day streak, overall %, progress per subject |
| Settings | Screen Reader Support (auto read-aloud), High Contrast, Text Size, Audio Speed, name, reset |

## How it helps each audience

**Blind learners**
- Text-to-speech reads each card aloud (Play Audio button, or automatically with Screen Reader Support on).
- Lessons include letter shapes and Braille dot patterns.
- Full TalkBack support: headings, labels, large 56-60dp touch targets, whole-row switches.

**Deaf learners**
- Every lesson is complete as on-screen text, nothing depends on audio.
- Large text, high contrast mode, and a vibration when a lesson is completed.

## Add a new subject (for example Math)

1. Create `app/src/main/assets/subjects/math.json`.
2. Copy this template and edit it:

```json
{
  "id": "math",
  "order": 3,
  "title": "Math",
  "description": "Numbers and counting",
  "icon": "calculator",
  "color": "#2E7D32",
  "speechLanguage": "en-US",
  "lessons": [
    {
      "id": "counting",
      "title": "Counting to Five",
      "summary": "Count from one to five.",
      "audience": ["blind", "deaf"],
      "cards": [
        { "heading": "One and two", "text": "One, two." },
        { "heading": "Three to five", "text": "Three, four, five.", "translation": "Optional English explanation" }
      ]
    }
  ]
}
```

3. Run the app. The subject appears on Home and in Lessons automatically.

Notes:
- `icon` can be `book`, `translate`, `calculator`, `chat` or `bulb`. Add more in `ui/Components.kt` (`iconFor`).
- `color` should be dark enough for white text/icons on top (high-contrast mode shows it as a solid card).
- `speechLanguage` is the voice language, e.g. `en-US`, `fil-PH`, `es-ES`.
- `audience` is `["blind"]`, `["deaf"]` or both. It controls the Blind / Deaf filters and tags.
- `translation` is read aloud in English after the main text, so a Filipino card can have an English explanation.
- File names starting with `_` are ignored, so you can keep drafts like `_math-draft.json`.
- Lesson ids only need to be unique inside their own subject.

## Voice commands

A large **voice bar** sits at the bottom of every screen. Tap it (you feel a vibration and hear a beep),
then say a command. The app answers out loud. The first time, Android asks for microphone permission.

| Say | What happens |
|---|---|
| "Open English" / "Open greetings" | Opens that subject or lesson (lessons start reading automatically) |
| "Continue" or "Next" (outside a lesson) | Starts your next unfinished lesson |
| "Next", "Previous", "Repeat" | Moves between cards / reads the card again |
| "Complete" | Finishes the lesson (on the last card) |
| "Faster", "Slower", "Bigger text", "Smaller text", "High contrast on / off" | Changes settings |
| "Home", "Lessons", "Progress", "Settings", "Back" | Moves around the app |
| "Where am I", "What is left", "List lessons" | Spoken information |
| "Stop", "Help" | Stops the voice / lists the commands |

Filipino words work too: *susunod, nakaraan, ulitin, tapos na, bilisan, bagalan, tigil, tulong*.
Set the language you speak in under **Settings > Voice Commands** (English or Filipino).
Subjects and lessons you add as JSON files can be opened by voice automatically, using their titles.

Notes:
- Voice recognition uses the phone's Google speech service. It is offline only if an offline speech pack
  is installed (Android Settings > System > Languages > Speech). Otherwise it needs internet.
- Code: `voice/VoiceListener.kt` (microphone), `voice/VoiceCommand.kt` (understands phrases),
  `voice/VoiceRouter.kt` (does the action), `ui/VoiceBar.kt` (the button).

### Motion shortcut

Under **Settings > Voice Commands > Motion shortcut** a learner can start a voice command by moving the phone
instead of tapping the voice bar. It works on every screen except Welcome, and only while the app is open.

| Option | Movement |
|---|---|
| Shake | Shake the phone firmly, three quick times |
| Twist | Twist your wrist so the phone turns about its long side, then turn it back |

**How firm** (Gentle / Normal / Firm) sets how strong the movement must be. It is off by default.
You feel a vibration and hear a beep when listening starts. The first time, tap the voice bar once so
Android can ask for the microphone permission. Code: `motion/MotionTrigger.kt`.

### Voice troubleshooting

When voice recognition fails, the app says the problem and an **error code**:

| Code | Meaning | Try |
|---|---|---|
| 1, 2, 4 | Network / server problem, or no speech service | Check internet; update the Google app; make sure Google is the phone's speech service |
| 3 | Microphone could not be used | Close other apps that use the microphone |
| 5 | Client error | Close and reopen the app |
| 8 | Recognizer busy | Wait a moment, or restart the phone |
| 9 | Microphone permission missing | Allow Microphone in app settings |
| 11 | Speech service disconnected | Update or restart the Google app |
| 12, 13 | Language not supported / not downloaded | Settings > Voice Commands > Open speech settings, download Filipino; or switch language |
| 7, 6 | Nothing heard | Speak after the beep, closer to the microphone |

Phones without Google services (some Huawei models) have no speech recognition service for apps to use.

### Filipino voice commands

The app tries Filipino recognition in this order, and only reports an error if all fail:
`fil-PH` online, then `tl-PH` online, then `fil-PH` offline. To make Filipino work well:

1. Update the **Google** app and **Speech Services by Google** in the Play Store.
2. Android Settings > System > Languages > Voice input (or Google app > Settings > Voice > Languages):
   add **Filipino (Pilipinas)**, and download its offline pack if you want it to work without internet.
3. In this app: Settings > Voice Commands > **Filipino**. Spoken replies switch to Filipino too.

## Filipino voice (text-to-speech)

Filipino text-to-speech needs a voice installed on the phone. If it's missing, the Lesson screen shows a message.
Install it in **Android Settings > System > Languages > Text-to-speech output** (Google Text-to-speech engine > Install voice data > Filipino).

## Project layout

```
app/src/main/
  assets/subjects/        english.json, filipino.json  (lesson content)
  java/com/learntogether/app/
    MainActivity.kt
    data/                 Models, ContentRepository (JSON loader), AppViewModel (progress + settings)
    speech/Speaker.kt     Text-to-speech wrapper
    ui/                   App.kt (navigation), Components.kt, theme/, screens/
```

## Ideas for later

- Sign language videos for deaf learners (add a `video` field to lesson cards and play it with Media3).
- More subjects: Math, Life Skills, Science.
- Move UI text to `strings.xml` to translate the app itself into Filipino.
- Sound-free celebrations (confetti, vibration patterns) for completed lessons.
