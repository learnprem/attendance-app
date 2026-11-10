ATTENDANCE APP - version 2 (signed app, updates, PDF saving)

You do these steps ONCE. After that, every update is just "upload the changed files".

A. UPLOAD THE FILES
1. Open your attendance-app repository on github.com.
2. Add file -> Upload files. Open this unzipped folder, select ALL files and folders inside
   (including the folder named .github) and drag them in. Click Commit changes.
   (Files with the same name are replaced. Nothing needs to be deleted.)

B. MAKE THE REPOSITORY PUBLIC (needed so that members can download updates)
3. Settings -> General -> scroll to the bottom (Danger Zone) -> Change visibility -> Make public.
   The repository holds only the app's code. Your signing key is stored encrypted and its
   password stays secret in step 5.

C. ALLOW THE BUILD TO PUBLISH
4. Settings -> Actions -> General -> Workflow permissions -> choose "Read and write permissions" -> Save.

D. CREATE THE PERMANENT SIGNING KEY (the app's identity)
5. Settings -> Secrets and variables -> Actions -> New repository secret.
   Name: KEY_PASS      Value: a LONG random password (at least 24 letters and numbers).
   WRITE THIS PASSWORD ON PAPER AND KEEP IT SAFE. Without it you can never update the app.
6. Actions tab -> "Create signing key (run once)" -> Run workflow. Wait for the green tick.
   A file release.keystore.enc now appears in the repository. Download a copy and keep it
   with your password (if it is ever lost, everybody must reinstall the app).

E. BUILD THE APP
7. Actions tab -> "Build APK" -> Run workflow. Wait for the green tick (about 5 minutes).
8. On the repository's main page, look at the right side: Releases -> "Version N".
   Open it and download AttendanceApp.apk. Open that same page on the phone to install it.
   (Allow "install unknown apps" for your browser when the phone asks.)
9. FIRST TIME ONLY: uninstall the old test app first (it had a different key).

F. WHEN YOU CHANGE THE APP LATER
10. Upload the changed files. A new build starts by itself and makes a new Release (Version N+1).
11. To tell the members: in the Google Sheet, Config tab, add or change these rows:
       AppLatest      the new version number N      (they see an "Update" message)
       AppMinVersion  a version number N            (older apps must update to continue; leave 0 normally)
       AppNotes       one line about what is new    (optional)
    Members tap Update, open the downloaded file, and it installs over the old one.

G. IF A RUN FAILS
12. Open the failed run, copy the red error text, and send it to the developer.

WHAT IS NEW IN THIS VERSION
- Permanent signing key: updates install over the old app, nothing is lost.
- "New version available" message, optional or required.
- PDF and Excel files made inside the pages can be saved and shared (WhatsApp etc.).
- The grey "created by another user" bar is hidden (if Google changes the page, it may reappear).
- "No internet connection" screen with a Retry button.
- A proper app icon.
