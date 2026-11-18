# Update your BUDDY GitHub repository from Windows 11

1. Download and extract `BUDDY-AI-MIRA-Android-Project.zip` on Windows.
2. Install Git for Windows and sign into your own GitHub account.
3. Open PowerShell and run:

```powershell
git clone https://github.com/kathiresh7831/BUDDY.git
cd BUDDY
```

4. Copy the **contents** of extracted `BUDDY-AI-MIRA` folder into this `BUDDY` folder (replace duplicate files). Important: preserve the hidden `.github` folder.
5. In PowerShell, run:

```powershell
git add .
git commit -m "Add BUDDY AI MIRA Android app and APK build"
git push origin main
```

6. Go to the repository **Actions** tab and open **Build BUDDY AI MIRA APK**. If no run started, choose **Run workflow**.
7. Open the finished run, download the `BUDDY-AI-MIRA-Android-APK` artifact, extract the ZIP and install `app-debug.apk` on your HONOR Pad 10.

Existing root-level files (`MainActivity.kt`, `AndroidManifest.xml`, `android.yml`) are legacy leftovers and aren't referenced by the corrected Gradle build. Do not confuse `.GitHub` with the lowercase `.github` directory. Delete or archive old CI workflow `.github/workflows/blank.yml` if you want to avoid "Hello, world!" workflows.

If the compiler fails, the new workflow will display a real **failed build** rather than incorrectly claiming success. You can inspect the compiler error in the job logs.
