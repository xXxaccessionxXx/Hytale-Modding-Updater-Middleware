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
    if not status:
        print("No changes to commit. Exiting.")
        return

    # Add all changes
    print("Staging all changes...")
    run_cmd(["git", "add", "."])

    # Generate a smart commit message based on diff
    print("Analyzing diff for smart commit message...")
    diff = run_cmd(["git", "diff", "--cached", "--stat"])
    
    print("\nModifications:")
    print(diff)
    print("\n")
    
    commit_msg = input("Enter a summary of these changes for the commit message: ")
    if not commit_msg:
        commit_msg = "Update middleware agent"

    print("Committing changes...")
    run_cmd(["git", "commit", "-m", commit_msg])

    version = input("Enter the new version number (e.g., v1.1.0) or press Enter to skip tagging: ")
    if version:
        print(f"Tagging release {version}...")
        run_cmd(["git", "tag", version])
        
        push_confirm = input("Push to GitHub? (y/n): ")
        if push_confirm.lower() == 'y':
            print("Pushing tags and commits to GitHub...")
            run_cmd(["git", "push", "origin", "main", "--tags"], check=False)
            
            # Optionally create a GH release if GitHub CLI is installed
            publish_release = input("Create GitHub Release using GitHub CLI (gh)? (y/n): ")
            if publish_release.lower() == 'y':
                print(f"Creating release {version}...")
                try:
                    import shutil
                    if shutil.which("gh") is None:
                        print("Error: GitHub CLI (gh) is not installed or not in PATH. Please install it to create releases automatically, or skip this step by doing it manually on github.com.")
                    else:
                        run_cmd(["gh", "release", "create", version, "--title", version, "--notes", commit_msg, "middleware-agent.jar"], check=False)
                except Exception as e:
                    print(f"Warning: Could not create GitHub Release. Ensure 'gh' CLI is installed. Error: {e}")

    print("Publish process complete!")

if __name__ == "__main__":
    main()
