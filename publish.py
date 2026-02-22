import os
import subprocess
import sys

def run_cmd(cmd, check=True):
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if check and result.returncode != 0:
        print(f"Error running command: {result.stderr}")
        sys.exit(1)
    return result.stdout.strip()

def main():
    print("=== Hytale Modding Middleware Publisher ===")
    
    # Check for uncommitted changes
    status = run_cmd(["git", "status", "--porcelain"])
    
    commit_msg = "Manual push/tag"
    if status:
        # Add all changes
        print("Staging all changes...")
        run_cmd(["git", "add", "."])

        # Generate a smart commit message based on diff
        print("Analyzing diff for smart commit message...")
        diff = run_cmd(["git", "diff", "--cached", "--stat"])
        
        print("\nModifications:")
        print(diff)
        print("\n")
        
        commit_msg_input = input("Enter a summary of these changes for the commit message: ")
        if commit_msg_input:
            commit_msg = commit_msg_input

        print("Committing changes...")
        run_cmd(["git", "commit", "-m", commit_msg])
    else:
        print("No uncommitted changes found. Proceeding with tagging and pushing existing commits.")

    version = input("Enter the new version number (e.g., v1.1.0) or press Enter to skip tagging: ")
    if version:
        print(f"Tagging release {version}...")
        tag_result = subprocess.run(["git", "tag", version], capture_output=True, text=True)
        if tag_result.returncode != 0:
            print(f"Warning: Could not create tag '{version}'. It may already exist. Error: {tag_result.stderr.strip()}")
        
        # Compile Installer
        import os
        import re
        iscc_path = r"C:\Users\kasey\AppData\Local\Programs\Inno Setup 6\ISCC.exe"
        installer_file = f"Hytale_Middleware_Setup_{version}.exe"
        
        if os.path.exists(iscc_path):
            print(f"Compiling Installer with Inno Setup for version {version}...")
            # Dynamically update the version in the ISS file
            with open("installer.iss", "r") as f:
                iss_content = f.read()
            
            clean_version = version.lstrip('v')
            iss_content = re.sub(r"AppVersion=.*", f"AppVersion={clean_version}", iss_content)
            iss_content = re.sub(r"OutputBaseFilename=.*", f"OutputBaseFilename=Hytale_Middleware_Setup_{version}", iss_content)
            
            with open("installer.iss", "w") as f:
                f.write(iss_content)
                
            run_cmd([iscc_path, r"installer.iss"])
        else:
            print(f"Warning: Inno Setup command line compiler (ISCC) not found at {iscc_path}. Skipping installer build.")
            installer_file = "middleware-agent.jar" # Fallback

        push_confirm = input("Push to GitHub? (y/n): ")
        if push_confirm.lower() == 'y':
            print("Pushing tags and commits to GitHub...")
            run_cmd(["git", "push", "origin", "HEAD", "--tags"], check=False)
            
            # Optionally create a GH release if GitHub CLI is installed
            publish_release = input("Create GitHub Release using GitHub CLI (gh)? (y/n): ")
            if publish_release.lower() == 'y':
                print(f"Creating release {version}...")
                try:
                    import shutil
                    gh_path = shutil.which("gh")
                    if gh_path is None and os.path.exists(r"C:\Program Files\GitHub CLI\gh.exe"):
                        gh_path = r"C:\Program Files\GitHub CLI\gh.exe"
                        
                    if gh_path is None:
                        print("Error: GitHub CLI (gh) is not installed or not in PATH. Please install it to create releases automatically.")
                    else:
                        run_cmd([gh_path, "release", "create", version, "--title", version, "--notes", commit_msg, installer_file], check=False)
                except Exception as e:
                    print(f"Warning: Could not create GitHub Release. Ensure 'gh' CLI is authenticated. Error: {e}")

    print("Publish process complete!")

if __name__ == "__main__":
    main()
