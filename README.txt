ATTENDANCE APP (test version) - how to get the APK

1. Create a free account at github.com (if you do not have one).
2. Click + (top right) -> New repository. Name it attendance-app. Choose PRIVATE. Click Create.
3. Click "uploading an existing file". Open this unzipped folder, select ALL files and folders
   inside it (including the hidden-looking folder named .github) and drag them into the page.
   Click Commit changes.
4. If the .github folder did not upload: click Add file -> Create new file, type
   .github/workflows/build.yml as the file name, paste the contents of that file, and commit.
5. Click the Actions tab. A run called "Build APK" starts. Wait about 5 minutes for a green tick.
6. Open the run, scroll to Artifacts, and download attendance-app-apk (a zip). Unzip it.
   Inside is app-debug.apk. Copy it to your phone (USB cable, Google Drive or email) and install it.
7. If a run fails, open it, copy the red error text and send it to the developer.
