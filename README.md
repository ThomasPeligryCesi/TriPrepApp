# TriPrep - Application de Planification Triathlon

Application Android pour planifier et suivre votre préparation au triathlon.

## Fonctionnalités

### Calculateurs d'allure

#### 🏃 Course à Pied
- Test Semi-Cooper (6 minutes)
- Calcul automatique de la VMA (Vitesse Maximale Aérobie)
- Calcul du VO2 Max

#### 🏊 Natation
- Test 400m
- Calcul des allures d'entraînement (V2, V3, V4)
- Temps calculés pour 50m, 100m et 200m

#### 🚴 Vélo
- Calcul des zones d'entraînement basées sur le FTP
- 6 zones de puissance (Récupération, Endurance, Tempo, Seuil, VO2max, Anaérobie)

### 📅 Planificateur d'Entraînements
- Calendrier interactif
- Ajout manuel d'entraînements
- Notifications programmables
- Vue par date des entraînements planifiés

## Installation

### Depuis GitHub Releases
1. Allez dans l'onglet "Releases" de ce repository
2. Téléchargez le dernier fichier APK
3. Installez sur votre appareil Android

### Depuis la branche release
Le fichier APK le plus récent est disponible sur la branche `release` dans le dossier `releases/`.

## Compilation

### Prérequis
- JDK 17 ou supérieur
- Android SDK
- Gradle 8.2

### Build
```bash
./gradlew assembleRelease
```

L'APK sera généré dans `app/build/outputs/apk/release/`

## CI/CD

Le projet utilise GitHub Actions pour automatiser la compilation :
- Build automatique sur chaque push
- Création d'une release avec l'APK
- Mise à jour de la branche `release`

## Technologies

- **Langage**: Kotlin
- **UI**: Material Design Components
- **Architecture**: MVVM
- **Notifications**: AlarmManager + BroadcastReceiver

## Permissions

L'application requiert les permissions suivantes :
- `POST_NOTIFICATIONS` : Pour envoyer des rappels d'entraînement
- `SCHEDULE_EXACT_ALARM` : Pour programmer les notifications aux heures exactes

## License

MIT License

## Auteur

Développé pour les triathlètes passionnés 🏊🚴🏃
