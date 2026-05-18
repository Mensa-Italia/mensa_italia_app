# Firebase Setup per Mensa Android

## Prerequisiti

1. Accedi a [Firebase Console](https://console.firebase.google.com)
2. Seleziona o crea il progetto Firebase per Mensa Italia

## Passaggi

### 1. Scarica google-services.json

- Firebase Console → Project Settings → "Your apps" → Android
- App package: `it.mensa.app`
- Scarica `google-services.json` e mettilo in `androidApp/google-services.json`

### 2. Abilita il plugin Google Services

In `androidApp/build.gradle.kts`, decommenta:
```kotlin
plugins {
    // ...
    alias(libs.plugins.google.services)  // <-- rimuovi il commento
}
```

### 3. Abilita le dipendenze Firebase

In `androidApp/build.gradle.kts`, decommenta:
```kotlin
val firebaseBom = platform(libs.firebase.bom)
implementation(firebaseBom)
implementation(libs.firebase.messaging.ktx)
implementation(libs.firebase.analytics.ktx)
```

### 4. Abilita MensaMessagingService

In `androidApp/src/main/kotlin/it/mensa/app/services/push/MensaMessagingService.kt`:
- Decommenta `import com.google.firebase.messaging.FirebaseMessagingService`
- Decommenta `import com.google.firebase.messaging.RemoteMessage`
- Cambia la classe in `class MensaMessagingService : FirebaseMessagingService()`
- Decommenta il metodo `onMessageReceived`

### 5. Verifica build

```bash
./gradlew :androidApp:compileDebugSources
```

## Note

- Il manifest già include `<service>` per MensaMessagingService e i meta-data per il canale di notifica default
- Il canale default è `mensa_default` (strings.xml)
- L'icona di notifica default è `@drawable/ic_notification`
